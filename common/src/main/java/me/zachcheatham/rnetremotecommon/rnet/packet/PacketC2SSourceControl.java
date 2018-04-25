package me.zachcheatham.rnetremotecommon.rnet.packet;

public class PacketC2SSourceControl extends RNetPacket
{
    private static final byte ID = 0x32;

    public PacketC2SSourceControl(int sourceId, int operation)
    {
        super();
        writeUnsignedByte(sourceId);
        writeUnsignedByte(operation);
    }

    @Override
    byte getPacketID()
    {
        return ID;
    }

    @Override
    void parseData() {}
}
