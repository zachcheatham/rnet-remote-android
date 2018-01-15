package me.zachcheatham.rnetremote.rnet.packet;

public class PacketC2SZoneMaxVolume extends RNetPacket
{
    public PacketC2SZoneMaxVolume(int controllerId, int zoneId, int maxVolume)
    {
        super();
        writeUnsignedByte(controllerId);
        writeUnsignedByte(zoneId);
        writeUnsignedByte(maxVolume);
    }

    @Override
    byte getPacketID()
    {
        return 0x64;
    }

    @Override
    void parseData() {}
}
