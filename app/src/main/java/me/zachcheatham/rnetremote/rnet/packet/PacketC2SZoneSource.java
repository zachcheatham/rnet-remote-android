package me.zachcheatham.rnetremote.rnet.packet;

public class PacketC2SZoneSource extends RNetPacket
{
    public PacketC2SZoneSource(int controllerId, int zoneId, int sourceID)
    {
        super();
        buffer.put((byte) controllerId);
        buffer.put((byte) zoneId);
        buffer.put((byte) sourceID);
    }

    @Override
    byte getPacketID()
    {
        return 0x0A;
    }

    @Override
    void parseData() {}
}
