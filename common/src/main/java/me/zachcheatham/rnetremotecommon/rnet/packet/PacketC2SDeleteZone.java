package me.zachcheatham.rnetremotecommon.rnet.packet;

public class PacketC2SDeleteZone extends RNetPacket
{
    private static final byte ID = 0x05;

    public PacketC2SDeleteZone(int controllerId, int zoneId)
    {
        super();
        writeUnsignedByte(controllerId);
        writeUnsignedByte(zoneId);
    }

    @Override
    byte getPacketID()
    {
        return ID;
    }

    @Override
    void parseData() {}
}
