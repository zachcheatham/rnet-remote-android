package me.zachcheatham.rnetremotecommon.rnet.packet;

public class PacketC2SRequestSourceProperties extends RNetPacket
{
    public PacketC2SRequestSourceProperties(int sourceId)
    {
        super();
        writeUnsignedByte(sourceId);
    }

    @Override
    byte getPacketID()
    {
        return 0x33;
    }

    @Override
    void parseData() {}
}
