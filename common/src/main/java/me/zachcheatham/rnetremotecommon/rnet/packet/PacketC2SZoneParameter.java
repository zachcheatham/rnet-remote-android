package me.zachcheatham.rnetremotecommon.rnet.packet;

import me.zachcheatham.rnetremotecommon.rnet.Zone;

public class PacketC2SZoneParameter extends RNetPacket
{
    public PacketC2SZoneParameter(int controllerId, int zoneId, int parameterId, Object value)
    {
        super();
        writeUnsignedByte(controllerId);
        writeUnsignedByte(zoneId);
        writeUnsignedByte(parameterId);

        switch (parameterId)
        {
        case Zone.PARAMETER_BALANCE:
        case Zone.PARAMETER_BASS:
        case Zone.PARAMETER_TREBLE:
            buffer.put((byte) (int) value);
            break;
        case Zone.PARAMETER_DO_NOT_DISTURB:
        case Zone.PARAMETER_FRONT_AV_ENABLE:
        case Zone.PARAMETER_LOUDNESS:
            writeUnsignedByte((boolean) value ? 1 : 0);
            break;
        default:
            writeUnsignedByte((int) value);
            break;
        }
    }

    @Override
    byte getPacketID()
    {
        return 0x0B;
    }

    @Override
    void parseData() {}
}
