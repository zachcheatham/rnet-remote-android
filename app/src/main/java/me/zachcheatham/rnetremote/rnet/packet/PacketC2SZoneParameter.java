package me.zachcheatham.rnetremote.rnet.packet;

import me.zachcheatham.rnetremote.rnet.Zone;

public class PacketC2SZoneParameter extends RNetPacket
{
    public PacketC2SZoneParameter(int controllerId, int zoneId, int parameterId, Object value)
    {
        super();
        buffer.put((byte) controllerId);
        buffer.put((byte) zoneId);
        buffer.put((byte) parameterId);

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
            buffer.put((byte) ((boolean) value ? 0x01 : 0x00));
            break;
        default:
            buffer.put((byte) (int) value);
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
