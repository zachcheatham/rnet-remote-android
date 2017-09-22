package zachcheatham.me.rnetremote.rnet.packet;

public class PacketC2SZonePower extends RNetPacket
{
    public PacketC2SZonePower(int controllerId, int zoneId, boolean power)
    {
        super();
        buffer.put((byte) controllerId);
        buffer.put((byte) zoneId);
        buffer.put((byte) (power ? 0x01 : 0x00));
    }

    @Override
    byte getPacketID()
    {
        return 0x08;
    }

    @Override
    void parseData() {}
}
