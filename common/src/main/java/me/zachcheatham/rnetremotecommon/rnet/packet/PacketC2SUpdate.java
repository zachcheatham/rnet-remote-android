package me.zachcheatham.rnetremotecommon.rnet.packet;

public class PacketC2SUpdate extends RNetPacket
{
    private static final byte ID = 0x7D;

    @Override
    byte getPacketID()
    {
        return ID;
    }

    @Override
    void parseData() {}
}
