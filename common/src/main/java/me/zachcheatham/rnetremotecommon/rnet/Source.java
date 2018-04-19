package me.zachcheatham.rnetremotecommon.rnet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.zachcheatham.rnetremotecommon.rnet.packet.PacketC2SSourceInfo;
import me.zachcheatham.rnetremotecommon.rnet.packet.PacketC2SSourceProperty;

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
    public static final byte PROPERTY_AUTO_OFF = 0;
    public static final byte PROPERTY_AUTO_ON_ZONES = 1;

    private final int sourceId;
    private final RNetServer server;
    private String name = "";
    private int type = 0;
    private boolean autoOff = false;
    private List<int[]> autoOnZones = new ArrayList<>();

    Source(int sourceId, RNetServer server)
    {
        this.sourceId = sourceId;
        this.server = server;
    }

    public String getName()
    {
        return name;
    }

    void setName(String name, boolean setRemotely)
    {
        if (!name.equals(this.name))
        {
            this.name = name;

            for (RNetServer.SourcesListener listener : server.sourcesListeners)
                listener.sourceChanged(this, setRemotely, RNetServer.SourceChangeType.NAME);

            if (!setRemotely)
                new RNetServer.SendPacketTask(server).execute(
                        new PacketC2SSourceInfo(sourceId, name, type));
        }
    }

    public int getType()
    {
        return type;
    }

    void setType(int type, boolean setRemotely)
    {
        this.type = type;

        for (RNetServer.SourcesListener listener : server.sourcesListeners)
            listener.sourceChanged(this, setRemotely, RNetServer.SourceChangeType.TYPE);

        if (!setRemotely)
            new RNetServer.SendPacketTask(server).execute(
                    new PacketC2SSourceInfo(sourceId, name, type));
    }

    {

    private boolean getAutoOff()
    {
        return autoOff;
    }

    public void setAutoOff(boolean autoOff, boolean setRemotely)
    {
        this.autoOff = autoOff;

        for (RNetServer.SourcesListener listener : server.sourcesListeners)
            listener.sourceChanged(this, setRemotely, RNetServer.SourceChangeType.AUTO_OFF);

        if (!setRemotely)
            new RNetServer.SendPacketTask(server).execute(
                    new PacketC2SSourceProperty(sourceId, PROPERTY_AUTO_OFF, setRemotely));
    }

    public int[][] getAutoOnZones()
    {
        return autoOnZones.toArray(new int[autoOnZones.size()][2]);
    }

    public void setAutoOnZones(int[][] autoOnZones, boolean setRemotely)
    {
        this.autoOnZones.clear();
        this.autoOnZones.addAll(Arrays.asList(autoOnZones));

        if (!setRemotely)
            new RNetServer.SendPacketTask(server).execute(
                    new PacketC2SSourceProperty(sourceId, PROPERTY_AUTO_ON_ZONES, autoOnZones));
    }

    public void removeAutoOnZone(int ctrllrId, int sourceId)
    {
        for (int i = 0; i < autoOnZones.size(); i++)
        {
            int[] zone = autoOnZones.get(i);
            if (zone[0] == ctrllrId && zone[1] == sourceId)
            {
                autoOnZones.remove(i);
                break;
            }
        }
    }
    }
}
