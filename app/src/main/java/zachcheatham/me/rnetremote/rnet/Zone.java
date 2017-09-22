package zachcheatham.me.rnetremote.rnet;

import android.util.Log;

public class Zone
{
    private static final String LOG_TAG = "Zone";

    private final int controllerId;
    private final int zoneId;
    private final RNetServer server;

    private String name = "Unknown";
    private boolean power;
    private int volume;
    private int sourceId;

    public Zone(int controllerId, int zoneId, RNetServer server)
    {
        this.controllerId = controllerId;
        this.zoneId = zoneId;
        this.server = server;
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
        this.name = name;

        for (RNetServer.ZonesListener listener : server.getZoneListeners())
            listener.zoneChanged(this);

        Log.i(LOG_TAG, String.format("Zone #%d-%d renamed to %s", controllerId, zoneId, name));

        if (!setRemotely)
        {
            // TODO Send rename packet
        }
    }

    public String getName()
    {
        return name;
    }

    public void setPower(boolean power, boolean setRemotely)
    {
        this.power = power;

        Log.i(LOG_TAG, String.format("Zone #%d-%d power set %s", controllerId, zoneId, power ? "on" : "off"));

        for (RNetServer.ZonesListener listener : server.getZoneListeners())
            listener.zoneChanged(this);

        if (!setRemotely)
        {
            // TODO Send power packet
        }
    }

    public boolean getPowered()
    {
        return power;
    }

    public void setVolume(int volume, boolean setRemotely)
    {
        this.volume = volume;

        Log.i(LOG_TAG, String.format("Zone #%d-%d volume set to %d", controllerId, zoneId, volume));

        for (RNetServer.ZonesListener listener : server.getZoneListeners())
            listener.zoneChanged(this);

        if (!setRemotely)
        {
            // TODO Send volume packet
        }
    }

    public int getVolume()
    {
        return volume;
    }

    public void setSourceId(int sourceId, boolean setRemotely)
    {
        this.sourceId = sourceId;

        Log.i(LOG_TAG, String.format("Zone #%d-%d source set to #%d", controllerId, zoneId, sourceId));

        for (RNetServer.ZonesListener listener : server.getZoneListeners())
            listener.zoneChanged(this);

        if (!setRemotely)
        {
            // TODO send source packet
        }
    }

    public int getSourceId()
    {
        return sourceId;
    }
}
