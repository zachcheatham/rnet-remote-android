package me.zachcheatham.rnetremotecommon.rnet.packet;

import java.nio.ByteBuffer;

public class PacketS2CMediaMetadata extends RNetPacket
{
    public static final byte ID = 0x36;

    private int sourceId;
    private String title;
    private String artist;
    private String artworkUrl;

    public PacketS2CMediaMetadata(ByteBuffer buffer)
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
        title = readNTString();
        artist = readNTString();
        artworkUrl = readNTString();
    }

    public int getSourceId()
    {
        return sourceId;
    }

    public String getTitle()
    {
        return title;
    }

    public String getArtist()
    {
        return artist;
    }

    public String getArtworkUrl()
    {
        return artworkUrl;
    }
}
