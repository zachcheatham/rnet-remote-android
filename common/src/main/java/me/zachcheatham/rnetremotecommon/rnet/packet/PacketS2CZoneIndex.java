package me.zachcheatham.rnetremotecommon.rnet.packet;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class PacketS2CZoneIndex extends RNetPacket
{
    public static final byte ID = 0x03;

    private List<int[]> zones;

    public PacketS2CZoneIndex(ByteBuffer buffer)
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
        zones = new ArrayList<>();

        while (buffer.position() < buffer.limit())
        {
            int[] zone = new int[2];
            zone[0] = readUnsignedByte();
            zone[1] = readUnsignedByte();
            zones.add(zone);
        }
    }

    public int[][] getIndex()
    {
        int[][] zs = new int[zones.size()][2];
        return zones.toArray(zs);
    }
}
