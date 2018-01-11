package me.zachcheatham.rnetremote.rnet.packet;

import java.nio.ByteBuffer;

public class PacketS2CZoneMaxVolume extends RNetPacket
{
    public static final byte ID = 0x64;

    private int controllerId;
    private int zoneId;
    private int maxVolume;

    public PacketS2CZoneMaxVolume(ByteBuffer buffer)
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
        controllerId = readUnsignedByte();
        zoneId = readUnsignedByte();
        maxVolume = readUnsignedByte();
    }

    public int getControllerId()
    {
        return controllerId;
    }

    public int getZoneId()
    {
        return zoneId;
    }

    public int getMaxVolume()
    {
        return maxVolume;
    }
}
