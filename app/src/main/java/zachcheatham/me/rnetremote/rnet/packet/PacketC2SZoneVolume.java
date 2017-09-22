package zachcheatham.me.rnetremote.rnet.packet;

public class PacketC2SZoneVolume extends RNetPacket
{
    public PacketC2SZoneVolume(int controllerId, int zoneId, int volume)
    {
        super();
        buffer.put((byte) controllerId);
        buffer.put((byte) zoneId);
        buffer.put((byte) volume);
    }

    @Override
    byte getPacketID()
    {
        return 0x09;
    }

    @Override
    void parseData() {}
}
