package zachcheatham.me.rnetremote.rnet.packet;

import java.nio.ByteBuffer;

public class PacketS2CZoneDeleted extends RNetPacket
{
    public static final byte ID = 0x05;

    private int controllerId;
    private int zoneId;

    public PacketS2CZoneDeleted(ByteBuffer buffer)
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
        controllerId = buffer.get();
        zoneId = buffer.get();
    }

    public int getControllerId()
    {
        return controllerId;
    }

    public int getZoneId()
    {
        return zoneId;
    }
}
