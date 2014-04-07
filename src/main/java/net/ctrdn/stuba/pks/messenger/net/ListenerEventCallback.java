package net.ctrdn.stuba.pks.messenger.net;

import java.net.InetAddress;

public interface ListenerEventCallback {

    public void onListenerStarted(ListenerMode mode);

    public void onListenerStopped();

    public void onListenerLogEvent(String message);

    public void onListenerTick();

    public void onIdentityBroadcastReceived(PeerIdentity peerIdentity);

    public void onMessageReceived(Message message);
}
