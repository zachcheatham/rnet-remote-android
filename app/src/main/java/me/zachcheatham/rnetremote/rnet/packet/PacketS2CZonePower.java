package me.zachcheatham.rnetremote.rnet.packet;

import java.nio.ByteBuffer;

public class PacketS2CZonePower extends RNetPacket
{
    public static final byte ID = 0x08;

    private int controllerId;
    private int zoneId;
    private boolean power;

    public PacketS2CZonePower(ByteBuffer buffer)
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
        power = readUnsignedByte() == 1;
    }

    public int getControllerId()
    {
        return controllerId;
    }

    public int getZoneId()
    {
        return zoneId;
    }

    public boolean getPowered()
    {
        return power;
    }
}
