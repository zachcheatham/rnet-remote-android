package me.zachcheatham.rnetremotecommon.rnet;

import me.zachcheatham.rnetremotecommon.rnet.packet.PacketC2SSourceInfo;

public class Source
{
    private final byte TYPE_GENERIC = 0;
    private final byte TYPE_AIRPLAY = 1;
    private final byte TYPE_BLURAY = 2;
    private final byte TYPE_CABLE = 3;
    private final byte TYPE_CD = 4;
    private final byte TYPE_COMPUTER = 5;
    private final byte TYPE_DVD = 6;
    private final byte TYPE_GOOGLE_CAST = 7;
    private final byte TYPE_INTERNET_RADIO = 8;
    private final byte TYPE_IPOD = 9;
    private final byte TYPE_MEDIA_SERVER = 10;
    private final byte TYPE_MP3 = 11;
    private final byte TYPE_OTA = 12;
    private final byte TYPE_PHONO = 13;
    private final byte TYPE_RADIO = 14;
    private final byte TYPE_SATELITE_TV = 15;
    private final byte TYPE_SATELITE_RADIO = 16;
    private final byte TYPE_SONOS = 17;
    private final byte TYPE_CASSETTE = 18;
    private final byte TYPE_VCR = 19;

    private final int sourceId;
    private final RNetServer server;
    private String name;
    private byte type;

    Source(int sourceId, RNetServer server)
    {
        this.sourceId = sourceId;
        this.server = server;
    }

    void setName(String name, boolean setRemotely)
    {
        this.name = name;

        for (RNetServer.ZonesListener listener : server.getZonesListeners())
            listener.sourcesChanged();

        if (!setRemotely)
            new RNetServer.SendPacketTask(server).execute(
                    new PacketC2SSourceInfo(sourceId, name, type));
    }

    void setType(byte type, boolean setRemotely)
    {
        this.type = type;

        for (RNetServer.ZonesListener listener : server.getZonesListeners())
            listener.sourcesChanged();

        if (!setRemotely)
            new RNetServer.SendPacketTask(server).execute(
                    new PacketC2SSourceInfo(sourceId, name, type));
    }

    public String getName()
    {
        return name;
    }

    public byte getType()
    {
        return type;
    }
}
