package zachcheatham.me.rnetremote.rnet.packet;

public class PacketC2SName extends RNetPacket
{
    private static final byte ID = 0x01;

    public PacketC2SName(String name)
    {
        super();
        writeNTString(name);
    }

    @Override
    byte getPacketID()
    {
        return ID;
    }

    @Override
    void parseData() {}
}
