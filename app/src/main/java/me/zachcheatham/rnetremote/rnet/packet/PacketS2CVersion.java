package me.zachcheatham.rnetremote.rnet.packet;

import java.nio.ByteBuffer;

public class PacketS2CVersion extends RNetPacket
{
    public static final byte ID = 0x7F;

    private String version;

    public PacketS2CVersion(ByteBuffer buffer)
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
        version = readNTString();
    }

    public String getVersion()
    {
        return version;
    }
}
