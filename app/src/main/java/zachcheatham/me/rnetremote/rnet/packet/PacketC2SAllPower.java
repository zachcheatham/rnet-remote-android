package zachcheatham.me.rnetremote.rnet.packet;

public class PacketC2SAllPower extends RNetPacket
{
    public PacketC2SAllPower(boolean power)
    {
        super();
        buffer.put((byte) (power ? 0x01 : 0x00));
    }

    @Override
    byte getPacketID()
    {
        return 0x0C;
    }

    @Override
    void parseData() {}
}
