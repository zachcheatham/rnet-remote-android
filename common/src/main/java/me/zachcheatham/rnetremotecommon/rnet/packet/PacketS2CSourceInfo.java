package me.zachcheatham.rnetremotecommon.rnet.packet;

import java.nio.ByteBuffer;

public class PacketS2CSourceInfo extends RNetPacket
{
    public static final byte ID = 0x06;

    private int sourceID;
    private String sourceName;
    private int type;

    public PacketS2CSourceInfo(ByteBuffer buffer)
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
        type = readUnsignedByte();
    }

    public int getSourceId()
    {
        return sourceID;
    }

    public String getSourceName()
    {
        return sourceName;
    }

    public int getType()
    {
        return type;
    }
}
