package me.zachcheatham.rnetremote.rnet.packet;

public class PacketC2SZoneName extends RNetPacket
{
    private static final byte ID = 0x04;

    public PacketC2SZoneName(int controllerId, int zoneId, String name)
    {
        super();
        writeUnsignedByte(controllerId);
        writeUnsignedByte(zoneId);
        writeNTString(name);
    }

    @Override
    byte getPacketID()
    {
        return ID;
    }

    @Override
    void parseData() {}
}
