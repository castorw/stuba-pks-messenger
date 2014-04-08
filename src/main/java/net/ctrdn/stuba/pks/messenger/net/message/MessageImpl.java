package net.ctrdn.stuba.pks.messenger.net.message;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import net.ctrdn.stuba.pks.messenger.exception.MessageException;

public class MessageImpl implements Message {

    @Override
    public InetAddress getSenderAddress() {
        return this.address;
    }

    @Override
    public int getSenderPort() {
        return this.port;
    }

    private class MessageFragment {

        private final int id;
        private final byte[] data;

        public MessageFragment(int id, byte[] buffer, int offset, int len) {
            this.id = id;
            this.data = ByteBuffer.allocate(len).put(buffer, offset, len).array();
        }

        public int getId() {
            return id;
        }

        public byte[] getData() {
            return data;
        }
    }

    private final InetAddress address;
    private final int port;
    private final List<MessageFragment> messageFragmentList = new ArrayList<>();
    private long sequenceId;
    private int totalFragmentCount;
    private Date lastFragmentDate;

    public MessageImpl(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    @Override
    public long getSequenceIdentifier() throws MessageException {
        if (messageFragmentList.isEmpty()) {
            throw new MessageException("No fragments added to message yet");
        }
        return this.sequenceId;
    }

    @Override
    public int getReceivedFrameCount() throws MessageException {
        return this.messageFragmentList.size();
    }

    @Override
    public int getTotalFragmentCount() throws MessageException {
        if (messageFragmentList.isEmpty()) {
            throw new MessageException("No fragments added to message yet");
        }
        return this.totalFragmentCount;
    }

    @Override
    public void addFragment(byte[] buffer, int offset, int length) throws MessageException {
        ByteBuffer wrappedHeader = ByteBuffer.wrap(buffer, offset, (Long.SIZE / 8) + (3 * (Integer.SIZE / 8)));
        long rxSequenceId = wrappedHeader.getLong();
        int rxLength = wrappedHeader.getInt();
        int rxFragmentId = wrappedHeader.getInt();
        int rxTotalFragmentCount = wrappedHeader.getInt();

        if (this.messageFragmentList.isEmpty()) {
            this.sequenceId = rxSequenceId;
            this.totalFragmentCount = rxTotalFragmentCount;
        } else if (this.sequenceId != rxSequenceId) {
            throw new MessageException("Added packet with no matching sequence id (got=" + this.sequenceId + ", recvd=" + rxSequenceId + ")");
        } else if (this.totalFragmentCount != rxTotalFragmentCount) {
            throw new MessageException("Total frame count changed in transit (got=" + this.totalFragmentCount + ", recvd=" + rxTotalFragmentCount + ")");
        }
        this.messageFragmentList.add(new MessageFragment(rxFragmentId, buffer, offset + (Long.SIZE / 8) + (3 * (Integer.SIZE / 8)), rxLength));
        this.lastFragmentDate = new Date();
    }

    @Override
    public Date getLastFragmentDate() throws MessageException {
        if (messageFragmentList.isEmpty()) {
            throw new MessageException("No fragments added to message yet");
        }
        return this.lastFragmentDate;
    }

    @Override
    public byte[] getMessage() throws MessageException {
        if (messageFragmentList.isEmpty()) {
            throw new MessageException("No fragments added to message yet");
        }
        Collections.sort(this.messageFragmentList, new Comparator<MessageImpl.MessageFragment>() {

            @Override
            public int compare(MessageFragment o1, MessageFragment o2) {
                return o1.getId() < o2.getId() ? -1 : o1.getId() == o2.getId() ? 0 : 1;
            }
        });
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (MessageFragment fragment : this.messageFragmentList) {
            baos.write(fragment.getData(), 0, fragment.getData().length);
        }
        return baos.toByteArray();
    }
}
