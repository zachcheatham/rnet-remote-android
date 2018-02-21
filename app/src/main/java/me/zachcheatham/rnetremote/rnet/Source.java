package me.zachcheatham.rnetremote.rnet;

import me.zachcheatham.rnetremote.rnet.packet.PacketC2SSourceName;

public class Source
{
    private final int sourceId;
    private final RNetServer server;
    private String name;

    public Source(int sourceId, RNetServer server)
    {
        this.sourceId = sourceId;
        this.server = server;
    }

    public void setName(String name, boolean setRemotely)
    {
        this.name = name;

        for (RNetServer.ZonesListener listener : server.getZonesListeners())
            listener.sourcesChanged();

        if (!setRemotely)
            new RNetServer.SendPacketTask(server).execute(
                    new PacketC2SSourceName(sourceId, name));
    }

    public String getName()
    {
        return name;
    }
}
