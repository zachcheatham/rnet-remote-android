package me.zachcheatham.rnetremotecommon.rnet.packet;

public class PacketC2SSourceInfo extends RNetPacket
{
    private static final byte ID = 0x06;

    public PacketC2SSourceInfo(int sourceId, String name, int type)
    {
        super();
        writeUnsignedByte(sourceId);
        writeNTString(name);
        writeUnsignedByte(type);
    }

    @Override
    byte getPacketID()
    {
        return ID;
    }

    @Override
    void parseData() {}
}
