package me.zachcheatham.rnetremotecommon.rnet.packet;

import java.nio.ByteBuffer;

public class PacketS2CMediaPlayState extends RNetPacket
{
    public static final byte ID = 0x37;

    private int sourceId;
    private boolean playing;

    public PacketS2CMediaPlayState(ByteBuffer buffer)
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
        playing = readUnsignedByte() == 0x01;
    }

    public int getSourceId()
    {
        return sourceId;
    }

    public boolean getPlaying()
    {
        return playing;
    }
}
