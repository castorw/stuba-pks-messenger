package net.ctrdn.stuba.pks.messenger.net.message;

import java.net.InetAddress;
import java.util.Date;
import net.ctrdn.stuba.pks.messenger.exception.MessageException;

public interface Message {

    public InetAddress getSenderAddress();

    public int getSenderPort();

    public long getSequenceIdentifier() throws MessageException;

    public int getReceivedFrameCount() throws MessageException;

    public int getTotalFragmentCount() throws MessageException;

    public void addFragment(byte[] buffer, int offset, int len) throws MessageException;

    public Date getLastFragmentDate() throws MessageException;

    public byte[] getMessage() throws MessageException;
}
