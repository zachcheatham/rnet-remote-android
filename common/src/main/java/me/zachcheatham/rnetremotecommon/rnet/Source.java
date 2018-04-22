package me.zachcheatham.rnetremotecommon.rnet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.zachcheatham.rnetremotecommon.rnet.packet.PacketC2SRequestSourceProperties;
import me.zachcheatham.rnetremotecommon.rnet.packet.PacketC2SSourceInfo;
import me.zachcheatham.rnetremotecommon.rnet.packet.PacketC2SSourceProperty;

public class Source
{
    public static final byte TYPE_GENERIC = 0;
    public static final byte TYPE_AIRPLAY = 1;
    public static final byte TYPE_BLURAY = 2;
    public static final byte TYPE_CABLE = 3;
    public static final byte TYPE_CD = 4;
    public static final byte TYPE_COMPUTER = 5;
    public static final byte TYPE_DVD = 6;
    public static final byte TYPE_GOOGLE_CAST = 7;
    public static final byte TYPE_INTERNET_RADIO = 8;
    public static final byte TYPE_IPOD = 9;
    public static final byte TYPE_MEDIA_SERVER = 10;
    public static final byte TYPE_MP3 = 11;
    public static final byte TYPE_OTA = 12;
    public static final byte TYPE_PHONO = 13;
    public static final byte TYPE_RADIO = 14;
    public static final byte TYPE_SATELLITE_TV = 15;
    public static final byte TYPE_SATELLITE_RADIO = 16;
    public static final byte TYPE_SONOS = 17;
    public static final byte TYPE_CASSETTE = 18;
    public static final byte TYPE_VCR = 19;

    public static final byte PROPERTY_AUTO_ON_ZONES = 1;
    public static final byte PROPERTY_AUTO_OFF = 2;

    private final int sourceId;
    private final RNetServer server;
    private String name = "";
    private int type = 0;
    private String descriptiveText = null;
    private String title = null;
    private String artist = null;
    private String artworkUrl = null;
    private boolean autoOff = false;
    private List<int[]> autoOnZones = new ArrayList<>();

    Source(int id, String name, int type, RNetServer server)
    {
        this.sourceId = id;
        this.name = name;
        this.type = type;
        this.server = server;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name, boolean setRemotely)
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

    public void setType(int type, boolean setRemotely)
    {
        this.type = type;

        for (RNetServer.SourcesListener listener : server.sourcesListeners)
            listener.sourceChanged(this, setRemotely, RNetServer.SourceChangeType.TYPE);

        if (!setRemotely)
            new RNetServer.SendPacketTask(server).execute(
                    new PacketC2SSourceInfo(sourceId, name, type));
    }

    public String getPermanentDescriptiveText()
    {
        return this.descriptiveText;
    }

    public void setPermanentDescriptiveText(String text)
    {
        this.descriptiveText = text;

        for (RNetServer.SourcesListener listener : server.sourcesListeners)
            listener.descriptiveText(this, text, 0);
    }

    public String getMediaTitle()
    {
        return title;
    }

    public String getMediaArtist()
    {
        return artist;
    }

    public String getMediaArtworkUrl()
    {
        return artworkUrl;
    }

    public void setMediaMetadata(String title, String artist, String artworkUrl)
    {
        this.title = title;
        this.artist = artist;
        this.artworkUrl = artworkUrl;

        for (RNetServer.SourcesListener listener : server.sourcesListeners)
            listener.sourceChanged(this, false, RNetServer.SourceChangeType.METADATA);
    }

    public void requestProperties()
    {
        new RNetServer.SendPacketTask(server).execute(
                new PacketC2SRequestSourceProperties(sourceId));
    }

    public boolean getAutoOff()
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
                    new PacketC2SSourceProperty(sourceId, PROPERTY_AUTO_OFF, autoOff));
    }

    public int[][] getAutoOnZones()
    {
        return autoOnZones.toArray(new int[autoOnZones.size()][2]);
    }

    public void setAutoOnZones(int[][] autoOnZones, boolean setRemotely)
    {
        this.autoOnZones.clear();
        this.autoOnZones.addAll(Arrays.asList(autoOnZones));

        for (RNetServer.SourcesListener listener : server.sourcesListeners)
            listener.sourceChanged(this, setRemotely, RNetServer.SourceChangeType.AUTO_ON);

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
