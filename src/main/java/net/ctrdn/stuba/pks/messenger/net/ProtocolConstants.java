package net.ctrdn.stuba.pks.messenger.net;

public interface ProtocolConstants {

    // message types
    public static final byte MSG_TYPE_IDENTITY = (byte) 0x0a;
    public static final byte MSG_TYPE_MESSAGE = (byte) 0x0b;

    // payloads
    public static final byte IDENTITY_ACTIVE = (byte) 0x01;
    public static final byte IDENTITY_LEAVING = (byte) 0x02;
    public static final byte IDENTITY_CLIENT = (byte) 0x20;
    public static final byte IDENTITY_SERVER = (byte) 0x21;
}
