package me.zachcheatham.rnetremote.rnet.packet;

import java.nio.ByteBuffer;

public class PacketS2CRNetStatus extends RNetPacket
{
    public static final byte ID = 0x02;

    private boolean connected;

    public PacketS2CRNetStatus(ByteBuffer buffer)
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
        connected = buffer.get() == 0x01;
    }

    public boolean getRNetConnected()
    {
        return connected;
    }
}
