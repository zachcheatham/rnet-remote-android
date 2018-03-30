package me.zachcheatham.rnetremotecommon.rnet.packet;

public class PacketC2SDeleteSource extends RNetPacket
{
    public PacketC2SDeleteSource(int sourceID)
    {
        super();
        writeUnsignedByte(sourceID);
    }

    @Override
    byte getPacketID()
    {
        return 0x07;
    }

    @Override
    void parseData() {}
}
