package me.zachcheatham.rnetremotecommon.rnet.packet;

import java.nio.ByteBuffer;

public class PacketS2CSourceDescriptiveText extends RNetPacket
{
    public static final byte ID = 0x35;

    private int sourceID;
    private int displayTime;
    private String text;

    public PacketS2CSourceDescriptiveText(ByteBuffer buffer)
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
        displayTime = readUnsignedShort();
        text = readNTString();
    }

    public int getSourceId()
    {
        return sourceID;
    }

    public int getDisplayTime()
    {
        return displayTime;
    }

    public String getText()
    {
        return text;
    }
}
