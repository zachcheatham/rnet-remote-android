package me.zachcheatham.rnetremotecommon.rnet.packet;

public class PacketC2SIntent extends RNetPacket
{
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
