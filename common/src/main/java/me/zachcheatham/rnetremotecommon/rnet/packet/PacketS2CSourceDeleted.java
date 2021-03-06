package me.zachcheatham.rnetremotecommon.rnet.packet;

import java.nio.ByteBuffer;

public class PacketS2CSourceDeleted extends RNetPacket
{
    public static final byte ID = 0x07;

    private int sourceId;

    public PacketS2CSourceDeleted(ByteBuffer buffer)
    {
        super(buffer);
    }

    @Override
    byte getPacketID()
    {
        return ID;
    }

    @Override
    void parseData()
    {
        sourceId = readUnsignedByte();
    }

    public int getSourceId()
    {
        return sourceId;
    }
}
