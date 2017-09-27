package me.zachcheatham.rnetremote.rnet.packet;

public class PacketC2SZoneVolume extends RNetPacket
{
    public PacketC2SZoneVolume(int controllerId, int zoneId, int volume)
    {
        super();
        writeUnsignedByte(controllerId);
        writeUnsignedByte(zoneId);
        writeUnsignedByte(volume);
    }

    @Override
    byte getPacketID()
    {
        return 0x09;
    }

    @Override
    void parseData() {}
}
