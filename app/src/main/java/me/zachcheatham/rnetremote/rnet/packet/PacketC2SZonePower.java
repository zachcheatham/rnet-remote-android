package me.zachcheatham.rnetremote.rnet.packet;

public class PacketC2SZonePower extends RNetPacket
{
    public PacketC2SZonePower(int controllerId, int zoneId, boolean power)
    {
        super();
        writeUnsignedByte(controllerId);
        writeUnsignedByte(zoneId);
        writeUnsignedByte(power ? 0x01 : 0x00);
    }

    @Override
    byte getPacketID()
    {
        return 0x08;
    }

    @Override
    void parseData() {}
}
