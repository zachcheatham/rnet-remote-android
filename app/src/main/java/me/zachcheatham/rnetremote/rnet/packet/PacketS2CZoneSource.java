package me.zachcheatham.rnetremote.rnet.packet;

import java.nio.ByteBuffer;

public class PacketS2CZoneSource extends RNetPacket
{
    public static final byte ID = 0x0A;

    private int controllerId;
    private int zoneId;
    private int sourceId;

    public PacketS2CZoneSource(ByteBuffer buffer)
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
        controllerId = buffer.get();
        zoneId = buffer.get();
        sourceId = buffer.get();
    }

    public int getControllerId()
    {
        return controllerId;
    }

    public int getZoneId()
    {
        return zoneId;
    }

    public int getSourceId()
    {
        return sourceId;
    }
}
