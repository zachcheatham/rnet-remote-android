package me.zachcheatham.rnetremotecommon.rnet.packet;

import java.nio.ByteBuffer;

public class PacketS2CUpdateAvailable extends RNetPacket
{
    public static final byte ID = 0x7D;

    private String version;

    public PacketS2CUpdateAvailable(ByteBuffer buffer)
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

    public String getNewVersion()
    {
        return version;
    }
}
