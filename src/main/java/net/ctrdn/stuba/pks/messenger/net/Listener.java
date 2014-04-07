package net.ctrdn.stuba.pks.messenger.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import net.ctrdn.stuba.pks.messenger.Helpers;
import net.ctrdn.stuba.pks.messenger.exception.ListenerException;
import net.ctrdn.stuba.pks.messenger.exception.MessageException;

public class Listener implements Runnable {

    private final ListenerMode mode;
    private final PeerIdentity localIdentity;
    private final List<ListenerEventCallback> eventCallbackList = new ArrayList<>();
    private boolean running = false;
    private DatagramSocket socket;
    private Date lastIdentityBroadcast = null;

    private final List<Message> messageList = new ArrayList<>();

    public Listener(PeerIdentity localIdentity, ListenerMode mode) throws ListenerException {
        try {
            this.localIdentity = localIdentity;
            this.mode = mode;
            this.socket = new DatagramSocket(this.getLocalIdentity().getPort());
        } catch (SocketException ex) {
            ListenerException finalEx = new ListenerException("Failed to start UDP listener: " + ex.getMessage());
            finalEx.addSuppressed(ex);
            throw finalEx;
        }
    }

    public void addEventCallback(ListenerEventCallback callback) {
        this.eventCallbackList.add(callback);
    }

    @Override
    public void run() {
        try {
            this.getSocket().setSoTimeout(1000);
            this.getSocket().setBroadcast(true);
            this.running = true;
            for (ListenerEventCallback callback : this.eventCallbackList) {
                callback.onListenerStarted(this.getMode());
            }
            while (this.running) {
                try {
                    for (ListenerEventCallback callback : this.eventCallbackList) {
                        callback.onListenerTick();
                    }
                    if (this.lastIdentityBroadcast == null || new Date().getTime() - this.lastIdentityBroadcast.getTime() > 5000) {
                        this.broadcastIdentity(running);
                    }

                    Message removeMessage = null;
                    for (Message message : this.messageList) {
                        try {
                            if (new Date().getTime() - message.getLastFragmentDate().getTime() > 15000) {
                                this.logMessage("Message SEQ_ID " + message.getSequenceIdentifier() + " did not receive data for over 15 seconds - deleting");
                                removeMessage = message;
                                break;
                            }
                        } catch (MessageException ex) {
                            this.logMessage("Time check error; " + ex.getMessage());
                        }
                    }
                    if (removeMessage != null) {
                        this.messageList.remove(removeMessage);
                        for (ListenerEventCallback callback : this.eventCallbackList) {
                            callback.onMessageReceived(removeMessage);
                        }
                    }

                    byte[] recvData = new byte[4096];
                    DatagramPacket recvPacket = new DatagramPacket(recvData, recvData.length);
                    this.getSocket().receive(recvPacket);
                    if (recvPacket.getLength() < 3) {
                        this.logMessage("Invalid message from " + recvPacket.getAddress().getHostAddress() + ":" + recvPacket.getPort() + " (MESSAGE_TOO_SHORT)");
                    } else {
                        switch (recvPacket.getData()[0]) {
                            case ProtocolConstants.MSG_TYPE_IDENTITY: {
                                ByteBuffer wrapped = ByteBuffer.wrap(recvPacket.getData(), 1, Long.SIZE / 8 + 3);
                                final long peerIdentifier = wrapped.getLong();
                                final ListenerMode peerListenerMode = (wrapped.get() == ProtocolConstants.IDENTITY_CLIENT) ? ListenerMode.CLIENT : ListenerMode.SERVER;
                                final PeerStatus peerStatus = (wrapped.get() == ProtocolConstants.IDENTITY_LEAVING) ? PeerStatus.LEAVING : PeerStatus.ACTIVE;
                                int nameLen = wrapped.get();
                                StringBuilder peerNameSB = new StringBuilder();
                                for (int i = 0; i < nameLen; i++) {
                                    peerNameSB.append((char) recvPacket.getData()[1 + Long.SIZE / 8 + 3 + i]);
                                }
                                final String peerName = peerNameSB.toString();
                                final InetAddress peerInetAddress = recvPacket.getAddress();
                                final int peerPort = recvPacket.getPort();

                                final PeerIdentity remoteIdentity = new PeerIdentity() {

                                    @Override
                                    public InetAddress getInetAddress() {
                                        return peerInetAddress;
                                    }

                                    @Override
                                    public int getPort() {
                                        return peerPort;
                                    }

                                    @Override
                                    public PeerStatus getPeerStatus() {
                                        return peerStatus;
                                    }

                                    @Override
                                    public String getPeerName() {
                                        return peerName;
                                    }

                                    @Override
                                    public ListenerMode getListenerMode() {
                                        return peerListenerMode;
                                    }

                                    @Override
                                    public long getIdentifier() {
                                        return peerIdentifier;
                                    }
                                };
                                for (ListenerEventCallback callback : this.eventCallbackList) {
                                    callback.onIdentityBroadcastReceived(remoteIdentity);
                                }
                                break;
                            }
                            case ProtocolConstants.MSG_TYPE_MESSAGE: {
                                try {
                                    ByteBuffer wrappedSequenceId = ByteBuffer.wrap(recvPacket.getData(), 1, Long.SIZE / 8);
                                    long sequenceId = wrappedSequenceId.getLong();
                                    Message message = null;
                                    for (Message msg : this.messageList) {
                                        if (msg.getSequenceIdentifier() == sequenceId) {
                                            message = msg;
                                            break;
                                        }
                                    }
                                    if (message == null) {
                                        message = new MessageImpl(recvPacket.getAddress(), recvPacket.getPort());
                                        this.messageList.add(message);
                                        this.logMessage("Receiving new message SEQ_ID " + sequenceId);
                                    }
                                    message.addFragment(recvPacket.getData(), 1, recvPacket.getData().length - 1);
                                    this.logMessage("Added frame " + message.getReceivedFrameCount() + " of " + message.getTotalFragmentCount() + " to message SEQ_ID " + sequenceId);
                                    if (message.getReceivedFrameCount() == message.getTotalFragmentCount()) {
                                        this.logMessage("Message SEQ_ID " + sequenceId + " received complete (fragments=" + message.getTotalFragmentCount() + ")");
                                        for (ListenerEventCallback callback : this.eventCallbackList) {
                                            callback.onMessageReceived(message);
                                        }
                                        this.messageList.remove(message);
                                    }
                                } catch (MessageException ex) {
                                    this.logMessage("Failed to parse incoming message: (MessageException): " + ex.getMessage());
                                }
                                break;
                            }
                            default: {
                                this.logMessage("Invalid packet from " + recvPacket.getAddress().getHostAddress() + ":" + recvPacket.getPort() + " (UNKNOWN_PAYLOAD_TYPE)");
                                break;
                            }
                        }
                    }
                } catch (SocketTimeoutException ex) {
                } catch (IOException ex) {
                    Helpers.showExceptionMessage(ex);
                }
            }
        } catch (SocketException ex) {
            Helpers.showExceptionMessage(ex);
        } finally {
            try {
                if (this.getSocket() != null && this.getSocket().isBound()) {
                    this.broadcastIdentity(false);
                    this.getSocket().close();
                }
            } catch (IOException ex) {
                Helpers.showExceptionMessage(ex);
            }
            for (ListenerEventCallback callback : this.eventCallbackList) {
                callback.onListenerStopped();
            }
        }
    }

    public int sendMessage(PeerIdentity target, int mtu, String message) throws ListenerException {
        if (this.mode != ListenerMode.CLIENT) {
            throw new ListenerException("Messages can be only sent in client mode");
        }
        try {
            // calculate random sequence number
            Random random = new Random(new Date().getTime());
            long sequenceId = random.nextLong();
            byte[] sequenceIdBytes = ByteBuffer.allocate(Long.SIZE / 8).putLong(sequenceId).array();
            this.logMessage("Sending message SEQ_ID " + sequenceId + " to " + target.getPeerName() + " (" + target.getInetAddress().getHostAddress() + ":" + target.getPort() + ")");

            // calculate fragment count
            int messageLength = message.length();
            int fragmentCount = (int) Math.ceil((float) messageLength / (float) mtu);

            byte[] fragmentCountBytes = ByteBuffer.allocate(Integer.SIZE / 8).putInt(fragmentCount).array();
            ByteArrayInputStream messageInputStream = new ByteArrayInputStream(message.getBytes(Charset.forName("UTF-8")));

            for (int currentFrameIndex = 0; currentFrameIndex < fragmentCount; currentFrameIndex++) {
                this.logMessage("Sending fragment " + (currentFrameIndex + 1) + " of " + fragmentCount + ", message SEQ_ID " + sequenceId);
                byte[] currentFrameIndexBytes = ByteBuffer.allocate(Integer.SIZE / 8).putInt(currentFrameIndex).array();
                byte[] buffer = new byte[mtu];
                int writeLength = messageInputStream.read(buffer, 0, mtu);
                byte[] writeLengthBytes = ByteBuffer.allocate(Integer.SIZE / 8).putInt(writeLength).array();

                // construct packet --- 1B MSG_TYPE + 8B SEQ_ID + 4B MSG_LEN + 4B FRAGMENT_ID + 4B FRAGMENT_COUNT + nB DATA
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                baos.write(ProtocolConstants.MSG_TYPE_MESSAGE);
                baos.write(sequenceIdBytes, 0, sequenceIdBytes.length);
                baos.write(writeLengthBytes, 0, writeLengthBytes.length);
                baos.write(currentFrameIndexBytes, 0, currentFrameIndexBytes.length);
                baos.write(fragmentCountBytes, 0, fragmentCountBytes.length);
                baos.write(buffer, 0, writeLength);

                byte[] txBuffer = baos.toByteArray();
                DatagramPacket txPacket = new DatagramPacket(txBuffer, txBuffer.length, target.getInetAddress(), target.getPort());
                this.socket.send(txPacket);
            }
            return fragmentCount;
        } catch (IOException ex) {
            ListenerException finalEx = new ListenerException("Failed to transmit packet");
            finalEx.addSuppressed(ex);
            throw finalEx;
        }
    }

    private void broadcastIdentity(boolean active) throws IOException {
        this.lastIdentityBroadcast = new Date();
        byte[] identifierBytes = ByteBuffer.allocate(Long.SIZE / 8).putLong(this.localIdentity.getIdentifier()).array();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(ProtocolConstants.MSG_TYPE_IDENTITY);
        baos.write(identifierBytes, 0, identifierBytes.length);
        baos.write((this.mode == ListenerMode.CLIENT) ? ProtocolConstants.IDENTITY_CLIENT : ProtocolConstants.IDENTITY_SERVER);
        baos.write((active) ? ProtocolConstants.IDENTITY_ACTIVE : ProtocolConstants.IDENTITY_LEAVING);
        baos.write((byte) this.getLocalIdentity().getPeerName().length());
        baos.write(this.getLocalIdentity().getPeerName().getBytes("UTF-8"), 0, this.getLocalIdentity().getPeerName().getBytes().length);
        byte[] identityData = baos.toByteArray();

        int count = 0;
        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
            NetworkInterface nic = en.nextElement();
            for (InterfaceAddress ifaddress : nic.getInterfaceAddresses()) {
                InetAddress broadcastAddress = ifaddress.getBroadcast();
                if (broadcastAddress != null && !ifaddress.getAddress().isLoopbackAddress()) {
                    try {
                        DatagramPacket identityPacket = new DatagramPacket(identityData, identityData.length, broadcastAddress, this.localIdentity.getPort());
                        this.getSocket().send(identityPacket);
                        count++;
                    } catch (IOException ex) {
                        if (ex.getMessage().equalsIgnoreCase("host is down") || ex.getMessage().equalsIgnoreCase("no route to host")) {
                            this.logMessage("Failed to broadcast to " + broadcastAddress.getHostAddress() + ": " + ex.getMessage());
                        } else {
                            throw ex;
                        }
                    }
                }
            }
        }
        this.logMessage("Broadcasted identity packet on " + count + " addresses");
    }

    private void logMessage(String message) {
        for (ListenerEventCallback callback : this.eventCallbackList) {
            callback.onListenerLogEvent(message);
        }
    }

    public void stop() {
        this.running = false;
    }

    public PeerIdentity getLocalIdentity() {
        return localIdentity;
    }

    public DatagramSocket getSocket() {
        return socket;
    }

    public ListenerMode getMode() {
        return mode;
    }
}
