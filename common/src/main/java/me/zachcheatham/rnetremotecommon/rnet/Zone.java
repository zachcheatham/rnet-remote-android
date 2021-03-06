package me.zachcheatham.rnetremotecommon.rnet;

import android.util.Log;

import java.util.Locale;

import me.zachcheatham.rnetremotecommon.rnet.packet.*;

public class Zone
{
    public static final int PARAMETER_BASS = 0;
    public static final int PARAMETER_TREBLE = 1;
    public static final int PARAMETER_LOUDNESS = 2;
    public static final int PARAMETER_BALANCE = 3;
    public static final int PARAMETER_TURN_ON_VOLUME = 4;
    public static final int PARAMETER_BACKGROUND_COLOR = 5;
    public static final int PARAMETER_DO_NOT_DISTURB = 6;
    public static final int PARAMETER_PARTY_MODE = 7;
    public static final int PARAMETER_PARTY_MODE_OFF = 0;
    public static final int PARAMETER_PARTY_MODE_ON = 1;
    public static final int PARAMETER_PARTY_MODE_MASTER = 2;
    public static final int PARAMETER_FRONT_AV_ENABLE = 8;

    private static final String LOG_TAG = "Zone";

    private final int controllerId;
    private final int zoneId;
    private final RNetServer server;

    private final Object[] parameters = new Object[9];
    private String name = "Unknown";
    private boolean power = false;
    private int volume = 0;
    private boolean muted = false;
    private int maxVolume = 100;
    private int sourceId;

    Zone(int controllerId, int zoneId, RNetServer server)
    {
        this.controllerId = controllerId;
        this.zoneId = zoneId;
        this.server = server;

        parameters[0] = 0;
        parameters[1] = 0;
        parameters[2] = false;
        parameters[3] = 0;
        parameters[4] = 0;
        parameters[5] = 0;
        parameters[6] = false;
        parameters[7] = 0;
        parameters[8] = false;
    }

    public int getControllerId()
    {
        return controllerId;
    }

    public int getZoneId()
    {
        return zoneId;
    }

    public void setName(String name, boolean setRemotely)
    {
        if (!name.equals(this.name))
        {
            this.name = name;

            for (RNetServer.ZonesListener listener : server.zonesListeners)
                listener.zoneChanged(this, setRemotely, RNetServer.ZoneChangeType.NAME);

            Log.i(LOG_TAG, String.format("Zone #%d-%d renamed to %s", controllerId, zoneId, name));

            for (RNetServer.ZonesListener listener : server.zonesListeners)
                listener.zoneChanged(this, setRemotely, RNetServer.ZoneChangeType.NAME);

            if (!setRemotely)
                new RNetServer.SendPacketTask(server)
                        .execute(new PacketC2SZoneName(controllerId, zoneId, name));
        }
    }

    public String getName()
    {
        return name;
    }

    public void setPower(boolean power, boolean setRemotely)
    {
        if (power != this.power)
        {
            this.power = power;

            Log.i(LOG_TAG, String.format("Zone #%d-%d power set %s", controllerId, zoneId,
                    power ? "on" : "off"));

            for (RNetServer.ZonesListener listener : server.zonesListeners)
                listener.zoneChanged(this, setRemotely, RNetServer.ZoneChangeType.POWER);

            if (!setRemotely)
                new RNetServer.SendPacketTask(server)
                        .execute(new PacketC2SZonePower(controllerId, zoneId, power));
        }
    }

    public boolean getPowered()
    {
        return power;
    }

    public void setVolume(int volume, boolean setRemotely)
    {
        if (volume != this.volume)
        {
            this.volume = volume;

            Log.i(LOG_TAG,
                    String.format("Zone #%d-%d volume set to %d", controllerId, zoneId, volume));

            for (RNetServer.ZonesListener listener : server.zonesListeners)
                listener.zoneChanged(this, setRemotely, RNetServer.ZoneChangeType.VOLUME);

            if (!setRemotely)
                new RNetServer.SendPacketTask(server)
                        .execute(new PacketC2SZoneVolume(controllerId, zoneId, volume));
        }
    }

    public void volumeUp()
    {
        if (volume < maxVolume)
            this.setVolume(volume + 2, false);
    }

    public void volumeDown()
    {
        if (volume > 0)
            this.setVolume(volume - 2, false);
    }

    public int getVolume()
    {
        return volume;
    }

    public void setMute(boolean mute, boolean setRemotely)
    {
        if (mute != muted)
        {
            muted = mute;

            Log.i(LOG_TAG, String.format("Zone #%d-%d mute set to %s", controllerId, zoneId, mute));

            for (RNetServer.ZonesListener listener : server.zonesListeners)
                listener.zoneChanged(this, setRemotely, RNetServer.ZoneChangeType.MUTE);

            if (!setRemotely)
                new RNetServer.SendPacketTask(server).execute(new PacketC2SZoneMute(controllerId, zoneId, mute));
        }
    }

    public boolean getMute()
    {
        return muted;
    }

    public void setMaxVolume(int maxVolume, boolean setRemotely)
    {
        if (maxVolume != this.maxVolume)
        {
            this.maxVolume = maxVolume;

            Log.i(LOG_TAG, String.format("Zone #%d-%d max volume set to %d", controllerId, zoneId,
                    maxVolume));

            for (RNetServer.ZonesListener listener : server.zonesListeners)
                listener.zoneChanged(this, setRemotely, RNetServer.ZoneChangeType.MAX_VOLUME);

            if (!setRemotely)
                new RNetServer.SendPacketTask(server)
                        .execute(new PacketC2SZoneMaxVolume(controllerId, zoneId, maxVolume));
        }
    }

    public int getMaxVolume()
    {
        return maxVolume;
    }

    public void setSourceId(int sourceId, boolean setRemotely)
    {
        if (this.sourceId != sourceId)
        {
            this.sourceId = sourceId;

            Log.i(LOG_TAG,
                    String.format("Zone #%d-%d source set to #%d", controllerId, zoneId, sourceId));

            for (RNetServer.ZonesListener listener : server.zonesListeners)
                listener.zoneChanged(this, setRemotely, RNetServer.ZoneChangeType.SOURCE);

            if (!setRemotely)
                new RNetServer.SendPacketTask(server)
                        .execute(new PacketC2SZoneSource(controllerId, zoneId, sourceId));
        }
    }

    public int getSourceId()
    {
        return sourceId;
    }

    public void setParameter(int parameterId, Object value, boolean setRemotely)
    {
        switch (parameterId)
        {
        case PARAMETER_BASS:
        case PARAMETER_TREBLE:
        case PARAMETER_BALANCE:
        case PARAMETER_TURN_ON_VOLUME:
        case PARAMETER_BACKGROUND_COLOR:
        case PARAMETER_PARTY_MODE:
            if (!(value instanceof Integer))
                throw new IllegalArgumentException(
                        String.format(Locale.getDefault(),
                                "Value must be Integer for %d. Instead got %s", parameterId,
                                value.getClass().toString()));
            break;
        default:
            if (!(value instanceof Boolean))
                throw new IllegalArgumentException("Value must be Boolean");
            break;
        }

        if (!parameters[parameterId].equals(value))
        {
            parameters[parameterId] = value;

            Log.i(LOG_TAG,
                    String.format("Zone #%d-%d parameter #%d set to %s", controllerId, zoneId,
                            parameterId, value));

            for (RNetServer.ZonesListener listener : server.zonesListeners)
                listener.zoneChanged(this, setRemotely, RNetServer.ZoneChangeType.PARAMETER);

            if (!setRemotely)
                new RNetServer.SendPacketTask(server).execute(
                        new PacketC2SZoneParameter(controllerId, zoneId, parameterId, value));
        }
    }

    public Object getParameter(int parameterId)
    {
        return parameters[parameterId];
    }
}
