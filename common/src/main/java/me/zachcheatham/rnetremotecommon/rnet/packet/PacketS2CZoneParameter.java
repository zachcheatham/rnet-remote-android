package me.zachcheatham.rnetremotecommon.rnet.packet;

import java.nio.ByteBuffer;

import me.zachcheatham.rnetremotecommon.rnet.Zone;

public class PacketS2CZoneParameter extends RNetPacket
{
    public static final byte ID = 0x0B;

    private int controllerId;
    private int zoneId;
    private int parameterId;
    private Object parameterValue;

    public PacketS2CZoneParameter(ByteBuffer buffer)
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
        parameterId = readUnsignedByte();

        switch (parameterId)
        {
        case Zone.PARAMETER_BALANCE:
        case Zone.PARAMETER_BASS:
        case Zone.PARAMETER_TREBLE:
            parameterValue = (int) buffer.get();
            break;
        case Zone.PARAMETER_DO_NOT_DISTURB:
        case Zone.PARAMETER_FRONT_AV_ENABLE:
        case Zone.PARAMETER_LOUDNESS:
            parameterValue = (readUnsignedByte() == 1);
            break;
        default:
            parameterValue = readUnsignedByte();
        }
    }

    public int getControllerId()
    {
        return controllerId;
    }

    public int getZoneId()
    {
        return zoneId;
    }

    public int getParameterId()
    {
        return parameterId;
    }

    public Object getParameterValue()
    {
        return parameterValue;
    }
}
