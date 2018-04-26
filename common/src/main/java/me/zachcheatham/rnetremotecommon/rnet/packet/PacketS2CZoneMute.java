package me.zachcheatham.rnetremotecommon.rnet.packet;

import java.nio.ByteBuffer;

public class PacketS2CZoneMute extends RNetPacket
{
    public static final byte ID = 0x65;

    private int controllerId;
    private int zoneId;
    private boolean mute;

    public PacketS2CZoneMute(ByteBuffer buffer)
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
        mute = readUnsignedByte() == 0x01;
    }

    public int getControllerId()
    {
        return controllerId;
    }

    public int getZoneId()
    {
        return zoneId;
    }

    public boolean getMute()
    {
        return mute;
    }
}
