package me.zachcheatham.rnetremotecommon.rnet.packet;

import me.zachcheatham.rnetremotecommon.rnet.Source;

public class PacketC2SSourceProperty extends RNetPacket
{
    private static final byte ID = 0x34;

    public PacketC2SSourceProperty(int sourceID, int propertyID, Object propertyValue)
    {
        super();
        writeUnsignedByte(sourceID);
        writeUnsignedByte(propertyID);
        switch (propertyID)
        {
        case Source.PROPERTY_AUTO_OFF:
        case Source.PROPERTY_OVERRIDE_NAME:
        {
            writeUnsignedByte(((Boolean) propertyValue) ? 0x01 : 0x00);
            break;
        }
        case Source.PROPERTY_AUTO_ON_ZONES:
        {
            int[][] zones = (int[][]) propertyValue;
            for (int[] z : zones)
            {
                writeUnsignedByte(z[0]);
                writeUnsignedByte(z[1]);
            }
            break;
        }
        }
    }

    @Override
    byte getPacketID()
    {
        return ID;
    }

    @Override
    void parseData() {}
}
