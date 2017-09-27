package me.zachcheatham.rnetremote.rnet.packet;

import java.nio.ByteBuffer;

public class PacketS2CSourceName extends RNetPacket
{
    public static final byte ID = 0x06;

    private int sourceID;
    private String sourceName;

    public PacketS2CSourceName(ByteBuffer buffer)
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
        sourceID = readUnsignedByte();
        sourceName = readNTString();
    }

    public int getSourceId()
    {
        return sourceID;
    }

    public String getSourceName()
    {
        return sourceName;
    }
}
