package me.zachcheatham.rnetremotecommon.rnet.packet;

public class PacketC2SZoneSource extends RNetPacket
{
    public PacketC2SZoneSource(int controllerId, int zoneId, int sourceID)
    {
        super();
        writeUnsignedByte(controllerId);
        writeUnsignedByte(zoneId);
        writeUnsignedByte(sourceID);
    }

    @Override
    byte getPacketID()
    {
        return 0x0A;
    }

    @Override
    void parseData() {}
}
