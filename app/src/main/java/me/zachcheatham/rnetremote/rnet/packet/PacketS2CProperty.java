package me.zachcheatham.rnetremote.rnet.packet;

import java.nio.ByteBuffer;

import me.zachcheatham.rnetremote.rnet.RNetServer;

public class PacketS2CProperty extends RNetPacket
{
    public static final byte ID = 0x02;

    private int property;
    private Object value;

    public PacketS2CProperty(ByteBuffer buffer)
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
        property = readUnsignedByte();
        switch (property)
        {
        case RNetServer.PROPERTY_SERIAL_CONNECTED:
        case RNetServer.PROPERTY_WEB_SERVER_ENABLED:
            value = readUnsignedByte() == 1;
            break;
        case RNetServer.PROPERTY_NAME:
        case RNetServer.PROPERTY_VERSION:
            value = readNTString();
            break;
        }
    }

    public int getPropertyID()
    {
        return property;
    }

    public Object getValue()
    {
        return value;
    }
}
