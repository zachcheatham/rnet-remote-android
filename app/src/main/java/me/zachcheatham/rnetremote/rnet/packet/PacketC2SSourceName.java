package me.zachcheatham.rnetremote.rnet.packet;

public class PacketC2SSourceName extends RNetPacket
{
    private static final byte ID = 0x06;

    public PacketC2SSourceName(int sourceId, String name)
    {
        super();
        writeUnsignedByte(sourceId);
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
