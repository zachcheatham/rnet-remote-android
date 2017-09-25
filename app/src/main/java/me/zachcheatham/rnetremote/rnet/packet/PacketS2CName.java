package me.zachcheatham.rnetremote.rnet.packet;

public class PacketS2CName extends RNetPacket
{
    public static final byte ID = 0x01;

    @Override
    byte getPacketID()
    {
        return ID;
    }

    @Override
    void parseData() {}
}
