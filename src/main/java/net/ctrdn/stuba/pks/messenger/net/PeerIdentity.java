package net.ctrdn.stuba.pks.messenger.net;

import java.net.InetAddress;

public interface PeerIdentity {

    public long getIdentifier();

    public ListenerMode getListenerMode();

    public InetAddress getInetAddress();

    public int getPort();

    public PeerStatus getPeerStatus();

    public String getPeerName();
}
