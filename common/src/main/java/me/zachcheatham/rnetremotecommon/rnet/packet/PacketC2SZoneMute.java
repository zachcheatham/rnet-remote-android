package me.zachcheatham.rnetremotecommon.rnet.packet;

public class PacketC2SZoneMute extends RNetPacket
{
    public PacketC2SZoneMute(int controllerId, int zoneId, boolean mute)
    {
        super();
        writeUnsignedByte(controllerId);
        writeUnsignedByte(zoneId);
        writeUnsignedByte(mute ? 0x01 : 0x00);
    }

    @Override
    byte getPacketID()
    {
        return 0x65;
    }

    @Override
    void parseData() {}
}
