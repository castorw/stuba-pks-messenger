package net.ctrdn.stuba.pks.messenger.net;

import java.net.InetAddress;
import net.ctrdn.stuba.pks.messenger.net.listener.ListenerMode;

public class DefaultPeerIdentity implements PeerIdentity {

    private final long identifier;
    private final ListenerMode listenerMode;
    private final InetAddress inetAddress;
    private final int port;
    private final PeerStatus peerStatus;
    private final String peerName;

    public DefaultPeerIdentity(long identifier, ListenerMode listenerMode, PeerStatus peerStatus, InetAddress inetAddress, int port, String peerName) {
        this.identifier = identifier;
        this.listenerMode = listenerMode;
        this.peerStatus = peerStatus;
        this.inetAddress = inetAddress;
        this.port = port;
        this.peerName = peerName;
    }

    @Override
    public long getIdentifier() {
        return this.identifier;
    }

    @Override
    public ListenerMode getListenerMode() {
        return this.listenerMode;
    }

    @Override
    public InetAddress getInetAddress() {
        return this.inetAddress;
    }

    @Override
    public int getPort() {
        return this.port;
    }

    @Override
    public PeerStatus getPeerStatus() {
        return this.peerStatus;
    }

    @Override
    public String getPeerName() {
        return this.peerName;
    }

    @Override
    public String toString() {
        return "(" + ((this.listenerMode == ListenerMode.TRANSMITTER) ? "T" : "R") + ") " + this.getPeerName() + " (" + this.getInetAddress().getHostAddress() + ":" + this.getPort() + ")";
    }
}
