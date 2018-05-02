package me.zachcheatham.rnetremotecommon.rnet.packet;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import me.zachcheatham.rnetremotecommon.rnet.Source;

public class PacketS2CSourceProperty extends RNetPacket
{
    public static final byte ID = 0x34;

    private int sourceID;
    private int propertyID;
    private Object value;

    public PacketS2CSourceProperty(ByteBuffer buffer)
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
        sourceID = readUnsignedByte();
        propertyID = readUnsignedByte();
        switch (propertyID)
        {
        case Source.PROPERTY_AUTO_OFF:
        case Source.PROPERTY_OVERRIDE_NAME:
            value = (readUnsignedByte() == 0x01);
            break;
        case Source.PROPERTY_AUTO_ON_ZONES:
            ArrayList<int[]> v = new ArrayList<>();
            while (buffer.hasRemaining())
                v.add(new int[]{readUnsignedByte(), readUnsignedByte()});
            value = v.toArray(new int[v.size()][2]);
            break;
        default:
            value = null;
        }
    }

    public int getSourceId()
    {
        return sourceID;
    }

    public int getPropertyID()
    {
        return propertyID;
    }

    public Object getPropertyValue()
    {
        return value;
    }
}
