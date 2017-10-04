package me.zachcheatham.rnetremote.rnet.packet;

public class PacketC2SIntent extends RNetPacket
{
    public static final int INTENT_ACTION = 0x01;
    public static final int INTENT_SUBSCRIBE = 0x02;

    private static final byte ID = 0x01;

    public PacketC2SIntent(int intentID)
    {
        super();
        writeUnsignedByte(intentID);
    }

    @Override
    byte getPacketID()
    {
        return ID;
    }

    @Override
    void parseData() {}
}
