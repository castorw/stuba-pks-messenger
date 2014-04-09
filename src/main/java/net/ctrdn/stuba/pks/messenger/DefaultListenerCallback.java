package net.ctrdn.stuba.pks.messenger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.DefaultListModel;
import net.ctrdn.stuba.pks.messenger.exception.MessageException;
import net.ctrdn.stuba.pks.messenger.net.listener.ListenerCallback;
import net.ctrdn.stuba.pks.messenger.net.listener.ListenerMode;
import net.ctrdn.stuba.pks.messenger.net.message.Message;
import net.ctrdn.stuba.pks.messenger.net.PeerIdentity;
import net.ctrdn.stuba.pks.messenger.net.PeerStatus;
import net.ctrdn.stuba.pks.messenger.net.listener.Listener;

public class DefaultListenerCallback implements ListenerCallback {

    private final MainFrame mainFrame;
    private final List<PeerIdentity> peerIdentityList = new ArrayList<>();
    private final Map<PeerIdentity, Date> peerIdentityLastIdentMap = new ConcurrentHashMap<>();

    public DefaultListenerCallback(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    @Override
    public void onListenerStarted(Listener listener) {
        mainFrame.getComboBoxMode().setEnabled(false);
        mainFrame.getFieldPort().setEnabled(false);
        mainFrame.getFieldIdentity().setEnabled(false);
        mainFrame.getButtonAddPeer().setEnabled(true);
        mainFrame.getFieldIpAddress().setEnabled(true);
        mainFrame.getButtonControl().setText("Stop");
        mainFrame.getLabelStatus().setText("Running (" + listener.getLocalIdentity().getListenerMode().toString() + ")");
        mainFrame.getLabelAddress().setText("0.0.0.0:" + mainFrame.getFieldPort().getText());
        mainFrame.logMessage("[Listener] Listener has started in mode " + listener.getLocalIdentity().getListenerMode().toString() + " on 0.0.0.0:" + listener.getLocalIdentity().getPort() + " with identifier " + listener.getLocalIdentity().getIdentifier());

        if (listener.getLocalIdentity().getListenerMode() == ListenerMode.CLIENT) {
            mainFrame.getFieldMessage().setEnabled(true);
            mainFrame.getFieldMtu().setEnabled(true);
            mainFrame.getButtonSend().setEnabled(true);
        }
    }

    @Override
    public void onListenerStopped(Listener listener) {
        mainFrame.getComboBoxMode().setEnabled(true);
        mainFrame.getFieldPort().setEnabled(true);
        mainFrame.getFieldIdentity().setEnabled(true);
        mainFrame.getButtonControl().setText("Start");
        mainFrame.getLabelStatus().setText("Stopped");
        mainFrame.getLabelAddress().setText("-");
        mainFrame.logMessage("[Listener] Listener has been stopped");
        mainFrame.removeListener();
        mainFrame.getListNeighbors().setModel(new DefaultListModel());
        mainFrame.getButtonAddPeer().setEnabled(false);
        mainFrame.getFieldIpAddress().setEnabled(false);
        mainFrame.getFieldMessage().setEnabled(false);
        mainFrame.getFieldMtu().setEnabled(false);
        mainFrame.getButtonSend().setEnabled(false);
    }

    @Override
    public void onListenerTick(Listener listener) {
        boolean updateNeeded = false;
        Date currentDate = new Date();
        for (Map.Entry<PeerIdentity, Date> entry : this.peerIdentityLastIdentMap.entrySet()) {
            if (currentDate.getTime() - entry.getValue().getTime() > 30000) {
                mainFrame.logMessage("[Listener] Peer " + entry.getKey().getInetAddress().getHostAddress() + ":" + entry.getKey().getPort() + " did not send identity packet in 30 seconds - removing");
                this.peerIdentityList.remove(entry.getKey());
                this.peerIdentityLastIdentMap.remove(entry.getKey());
                updateNeeded = true;
            }
        }
        if (updateNeeded) {
            this.reloadReceiverList();
        }
    }

    @Override
    public void onIdentityBroadcastReceived(Listener listener, PeerIdentity peerIdentity) {
        mainFrame.logMessage("[Listener] Identity packet received (address=" + peerIdentity.getInetAddress().getHostAddress() + ", port=" + peerIdentity.getPort() + ", ident=" + peerIdentity.getIdentifier() + ", name=" + peerIdentity.getPeerName() + ", mode=" + peerIdentity.getListenerMode().toString() + " status=" + peerIdentity.getPeerStatus().toString() + ")");
        if (peerIdentity.getIdentifier() == listener.getLocalIdentity().getIdentifier()) {
            mainFrame.logMessage("[Listener] Identity from self - ignored");
            return;
        }
        boolean updateNeeded = false;
        PeerIdentity foundPeerIdentity = null;
        for (PeerIdentity currentId : this.peerIdentityList) {
            if (currentId.getIdentifier() == peerIdentity.getIdentifier()) {
                foundPeerIdentity = currentId;
                break;
            }
        }
        if (foundPeerIdentity != null && peerIdentity.getPeerStatus() == PeerStatus.LEAVING) {
            this.peerIdentityList.remove(foundPeerIdentity);
            this.peerIdentityLastIdentMap.remove(foundPeerIdentity);
            updateNeeded = true;
        } else if (foundPeerIdentity == null) {
            this.peerIdentityList.add(peerIdentity);
            this.peerIdentityLastIdentMap.put(peerIdentity, new Date());
            updateNeeded = true;
        } else {
            this.peerIdentityLastIdentMap.remove(foundPeerIdentity);
            this.peerIdentityLastIdentMap.put(foundPeerIdentity, new Date());
        }
        if (updateNeeded) {
            this.reloadReceiverList();
        }
    }

    @Override
    public void onMessageReceived(Listener listener, Message message) {
        try {
            PeerIdentity foundId = null;
            for (PeerIdentity currentIdentity : this.peerIdentityList) {
                if (currentIdentity.getInetAddress().equals(message.getSenderAddress()) && currentIdentity.getPort() == message.getSenderPort()) {
                    foundId = currentIdentity;
                    break;
                }
            }
            String appendLines = "[Received message from ";
            appendLines += ((foundId == null) ? "_unknown_ (" : foundId.getPeerName() + " (");
            appendLines += message.getSenderAddress().getHostAddress() + ":" + message.getSenderPort() + ", ";
            appendLines += ((message.getReceivedFrameCount() == message.getTotalFragmentCount()) ? "C" : "I");
            appendLines += ", " + message.getReceivedFrameCount() + " of " + message.getTotalFragmentCount() + " fragments" + "]\n";
            appendLines += new String(message.getMessage()) + "\n\n";

            mainFrame.getTaMessageLog().append(appendLines);
        } catch (MessageException ex) {
            mainFrame.handleException(ex);
        }
    }

    @Override
    public void onListenerLogEvent(Listener listener, String message) {
        mainFrame.logMessage("[Listener] " + message);
    }

    private void reloadReceiverList() {
        String selectedItem = (String) mainFrame.getListNeighbors().getSelectedValue();
        boolean selectedItemFound = false;
        DefaultListModel cmodel = new DefaultListModel();
        for (PeerIdentity listingId : this.peerIdentityList) {
            cmodel.addElement(listingId);
            if (listingId.equals(selectedItem)) {
                selectedItemFound = true;
            }
        }
        mainFrame.getListNeighbors().setModel(cmodel);
        if (selectedItemFound) {
            mainFrame.getListNeighbors().setSelectedValue(selectedItem, true);
        }
    }

    public List<PeerIdentity> getPeerIdentityList() {
        return this.peerIdentityList;
    }

    public void addStaticPeerIdentity(PeerIdentity peerIdentity) {
        this.peerIdentityList.add(peerIdentity);
        this.reloadReceiverList();
    }
}
