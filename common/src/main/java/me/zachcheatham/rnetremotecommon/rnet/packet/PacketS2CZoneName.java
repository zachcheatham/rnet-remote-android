package me.zachcheatham.rnetremotecommon.rnet.packet;

import java.nio.ByteBuffer;

public class PacketS2CZoneName extends RNetPacket
{
    public static final byte ID = 0x04;

    private int controllerId;
    private int zoneId;
    private String zoneName;

    public PacketS2CZoneName(ByteBuffer buffer)
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
        zoneName = readNTString();
    }

    public int getControllerId()
    {
        return controllerId;
    }

    public int getZoneId()
    {
        return zoneId;
    }

    public String getZoneName()
    {
        return zoneName;
    }
}
