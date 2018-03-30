package me.zachcheatham.rnetremotecommon.rnet.packet;

import java.nio.ByteBuffer;

public class PacketS2CZoneVolume extends RNetPacket
{
    public static final byte ID = 0x09;

    private int controllerId;
    private int zoneId;
    private int volume;

    public PacketS2CZoneVolume(ByteBuffer buffer)
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
        volume = readUnsignedByte();
    }

    public int getControllerId()
    {
        return controllerId;
    }

    public int getZoneId()
    {
        return zoneId;
    }

    public int getVolume()
    {
        return volume;
    }
}
