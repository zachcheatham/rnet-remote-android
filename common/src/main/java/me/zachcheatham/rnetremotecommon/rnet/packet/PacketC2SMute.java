package me.zachcheatham.rnetremotecommon.rnet.packet;

public class PacketC2SMute extends RNetPacket
{
    public static final int MUTE_OFF = 0x00;
    public static final int MUTE_ON = 0x01;
    public static final int MUTE_TOGGLE = 0x02;
    private static final byte ID = 0x0D;

    public PacketC2SMute(int state, short fadeTime)
    {
        super();
        writeUnsignedByte(state);
        writeUnsignedShort(fadeTime);
    }

    @Override
    byte getPacketID()
    {
        return ID;
    }

    @Override
    void parseData() {}
}
