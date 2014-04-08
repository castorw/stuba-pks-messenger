package net.ctrdn.stuba.pks.messenger.net.listener;

import net.ctrdn.stuba.pks.messenger.net.message.Message;
import net.ctrdn.stuba.pks.messenger.net.PeerIdentity;

public interface ListenerCallback {

    public void onListenerStarted(Listener listener);

    public void onListenerStopped(Listener listener);

    public void onListenerLogEvent(Listener listener, String message);

    public void onListenerTick(Listener listener);

    public void onIdentityBroadcastReceived(Listener listener, PeerIdentity peerIdentity);

    public void onMessageReceived(Listener listener, Message message);
}
