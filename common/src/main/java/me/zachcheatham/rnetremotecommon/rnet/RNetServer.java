package me.zachcheatham.rnetremotecommon.rnet;

import android.os.AsyncTask;
import android.util.Log;
import android.util.SparseArray;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import me.zachcheatham.rnetremotecommon.rnet.packet.*;

public class RNetServer
{
    public static final int INTENT_ACTION = 0x01;
    public static final int INTENT_SUBSCRIBE = 0x02;

    public static final int PROPERTY_NAME = 1;
    public static final int PROPERTY_VERSION = 2;
    public static final int PROPERTY_SERIAL_CONNECTED = 3;
    public static final int PROPERTY_WEB_SERVER_ENABLED = 4;

    private static final String LOG_TAG = "RNetServer";
    private final ByteBuffer pendingBuffer = ByteBuffer.allocate(255);
    private SocketChannel channel;
    private InetAddress address;
    private int port;
    private int intent;
    private int pendingPacketType = 0;
    private int pendingRemainingBytes = -1;

    private boolean run;
    private boolean receivedIndex = false;
    private SparseArray<Source> sources = new SparseArray<>();
    private SparseArray<SparseArray<Zone>> zones = new SparseArray<>();
    private String name = "<unknown>";
    private String version = "<unknown>";
    private String newVersion = null;
    //private boolean serialConnected = false;

    private List<ConnectivityListener> connectivityListeners = new ArrayList<>();
    private List<ControllerListener> controllerListeners = new ArrayList<>();
    List<ZonesListener> zonesListeners = new ArrayList<>();
    List<SourcesListener> sourcesListeners = new ArrayList<>();

    public RNetServer(int intent)
    {
        pendingBuffer.order(ByteOrder.LITTLE_ENDIAN);
        this.intent = intent;
    }

    public void setConnectionInfo(InetAddress address, int port)
    {
        this.address = address;
        this.port = port;
    }

    public void disconnect()
    {
        run = false;
        if (channel != null)
        {
            try
            {
                channel.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void createZone(String zoneName, int controllerId, int zoneId)
    {
        if (zones.get(controllerId) == null || zones.get(controllerId).get(zoneId) == null)
            new SendPacketTask(this).execute(new PacketC2SZoneName(controllerId, zoneId, zoneName));
    }

    public void deleteZone(int controllerId, int zoneId, boolean remotelyTriggered)
    {
        if (zones.get(controllerId) != null)
        {
            zones.get(controllerId).remove(zoneId);
            Log.i(LOG_TAG, String.format("Deleted zone #%d-%d", controllerId, zoneId));

            if (zones.get(controllerId).size() < 1)
            {
                zones.remove(controllerId);
                Log.i(LOG_TAG, String.format("Deleted controller #%d", controllerId));
            }

            for (ZonesListener listener : zonesListeners)
                listener.zoneRemoved(controllerId, zoneId);

            if (!remotelyTriggered)
                new SendPacketTask(this).execute(new PacketC2SDeleteZone(controllerId, zoneId));
        }
    }

    public void createSource(int sourceId, String sourceName, int sourceType)
    {
        Source source = new Source(sourceId, sourceName, sourceType, this);
        sources.put(sourceId, source);

        new SendPacketTask(this).execute(new PacketC2SSourceInfo(sourceId, sourceName, sourceType));
    }

    public void deleteSource(int sourceId)
    {
        sources.remove(sourceId);
        new SendPacketTask(this).execute(new PacketC2SDeleteSource(sourceId));
        // We don't update our listeners here because the server is going to send us a packet
        // back...
    }

    public boolean isRunning()
    {
        return run;
    }

    public boolean isConnected()
    {
        return channel != null && channel.isConnected();
    }

    public boolean canStartConnection()
    {
        return channel == null;
    }

    public boolean isReady()
    {
        return receivedIndex || (intent == INTENT_ACTION && isConnected());
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        if (!name.equals(this.name))
            new SendPacketTask(this).execute(new PacketC2SProperty(PROPERTY_NAME, name));
    }

    public String getVersion()
    {
        return version;
    }

    public boolean updateAvailable()
    {
        return newVersion != null;
    }

    public String getNewVersion()
    {
        return newVersion;
    }

    /*public boolean isSerialConnected()
    {
        return serialConnected;
    }*/

    public boolean anyZonesOn()
    {
        for (int i = 0; i < zones.size(); i++)
        {
            int ctrllrId = zones.keyAt(i);
            for (int c = 0; c < zones.get(ctrllrId).size(); c++)
            {
                int zoneId = zones.get(ctrllrId).keyAt(c);
                if (zones.get(ctrllrId).get(zoneId).getPowered())
                    return true;
            }
        }

        return false;
    }

    public boolean allZonesOn()
    {
        for (int i = 0; i < zones.size(); i++)
        {
            int ctrllrId = zones.keyAt(i);
            for (int c = 0; c < zones.get(ctrllrId).size(); c++)
            {
                int zoneId = zones.get(ctrllrId).keyAt(c);
                if (!zones.get(ctrllrId).get(zoneId).getPowered())
                    return false;
            }
        }

        return true;
    }

    public void volumeDown()
    {
        for (int i = 0; i < zones.size(); i++)
        {
            int ctrllrId = zones.keyAt(i);
            for (int c = 0; c < zones.get(ctrllrId).size(); c++)
            {
                int zoneId = zones.get(ctrllrId).keyAt(c);
                Zone zone = zones.get(ctrllrId).get(zoneId);
                if (zone.getPowered())
                    zone.volumeDown();
            }
        }
    }

    public void volumeUp()
    {
        for (int i = 0; i < zones.size(); i++)
        {
            int ctrllrId = zones.keyAt(i);
            for (int c = 0; c < zones.get(ctrllrId).size(); c++)
            {
                int zoneId = zones.get(ctrllrId).keyAt(c);
                Zone zone = zones.get(ctrllrId).get(zoneId);
                if (zone.getPowered())
                    zone.volumeUp();
            }
        }
    }

    public InetAddress getAddress()
    {
        return address;
    }

    public int getPort()
    {
        return port;
    }

    public SparseArray<SparseArray<Zone>> getZones()
    {
        return zones;
    }

    public Zone getZone(int controllerId, int zoneId)
    {
        if (zones.get(controllerId) != null)
        {
            return zones.get(controllerId).get(zoneId);
        }
        return null;
    }

    public SparseArray<Source> getSources()
    {
        return sources;
    }

    public Source getSource(int sourceId)
    {
        return sources.get(sourceId);
    }

    public void addConnectivityListener(ConnectivityListener listener)
    {
        connectivityListeners.add(listener);
    }

    public void removeConnectivityListener(ConnectivityListener listener)
    {
        connectivityListeners.remove(listener);
    }

    public void addControllerListener(ControllerListener listener)
    {
        controllerListeners.add(listener);
    }

    public void removeControllerListener(ControllerListener listener)
    {
        controllerListeners.remove(listener);
    }

    public void addZonesListener(ZonesListener listener)
    {
        zonesListeners.add(listener);
    }

    public void removeZonesListener(ZonesListener listener)
    {
        zonesListeners.remove(listener);
    }

    public void addSourcesListener(SourcesListener listener)
    {
        sourcesListeners.add(listener);
    }

    public void removeSourcesListener(SourcesListener listener)
    {
        sourcesListeners.remove(listener);
    }

    public void update()
    {
        new SendPacketTask(this).execute(new PacketC2SUpdate());
    }

    public void run()
    {
        if (channel != null)
        {
            throw new IllegalStateException("RNetServer already running.");
        }

        if (port == 0 || address == null)
        {
            throw new IllegalStateException("Connection information hasn't been set yet.");
        }

        Log.d(LOG_TAG, "Server run started.");

        cleanUp();

        run = true;

        for (ConnectivityListener listener : connectivityListeners)
            listener.connectionInitiated();

        try
        {
            channel = SocketChannel.open();
            channel.connect(new InetSocketAddress(address, port));

            Log.d(LOG_TAG, "Server socket opened.");

            sendPacket(new PacketC2SIntent(intent));
            if (intent == INTENT_ACTION)
                for (ConnectivityListener listener : connectivityListeners)
                    listener.ready();

            readChannel();

            try
            {
                channel.close();
            }
            catch (IOException ignored) {}

            if (isReady())
                for (ConnectivityListener listener : connectivityListeners)
                    listener.disconnected(run);
            else if (run) // Don't notify an error if we wanted to disconnect
                for (ConnectivityListener listener : connectivityListeners)
                    listener.connectError();
        }
        catch (IOException e)
        {
            if (run)
                for (ConnectivityListener listener : connectivityListeners)
                    listener.connectError();
        }

        channel = null;
        run = false;
        cleanUp();

        Log.d(LOG_TAG, "Server run ended.");
    }

    private void readChannel()
    {
        int bytesRead;
        ByteBuffer incomingBuffer = ByteBuffer.allocateDirect(1024);
        incomingBuffer.order(ByteOrder.LITTLE_ENDIAN);

        try
        {
            while (run)
            {
                bytesRead = channel.read(incomingBuffer);
                if (bytesRead == -1)
                {
                    channel.close();
                }
                else
                {
                    incomingBuffer.rewind();
                    incomingBuffer.limit(bytesRead);

                    while (incomingBuffer.remaining() > 0)
                    {
                        if (pendingRemainingBytes == -1)
                        {
                            pendingPacketType = incomingBuffer.get() & 0xff;
                            pendingRemainingBytes = incomingBuffer.get() & 0xff;
                            pendingBuffer.limit(pendingRemainingBytes);
                        }
                        else
                        {
                            int bytesToRead;
                            if (pendingRemainingBytes > incomingBuffer.remaining())
                                bytesToRead = incomingBuffer.remaining();
                            else
                                bytesToRead = pendingRemainingBytes;

                            byte[] data = new byte[bytesToRead];
                            incomingBuffer.get(data);
                            pendingBuffer.put(data);
                            pendingRemainingBytes -= bytesToRead;
                        }

                        if (pendingRemainingBytes == 0)
                        {
                            pendingBuffer.flip();
                            pendingBuffer.rewind();
                            constructAndHandlePacket(pendingPacketType, pendingBuffer);
                            pendingBuffer.flip();
                            pendingRemainingBytes = -1;
                            pendingPacketType = 0;
                            pendingBuffer.clear();
                            pendingBuffer.rewind();
                        }
                    }

                    incomingBuffer.clear();
                }
            }
        }
        catch (IOException ignored) {}
    }

    private void constructAndHandlePacket(int packetType, ByteBuffer buffer)
    {
        if (receivedIndex)
        {
            switch (packetType)
            {
            case PacketS2CProperty.ID:
            {
                PacketS2CProperty packet = new PacketS2CProperty(buffer);

                switch (packet.getPropertyID())
                {
                case PROPERTY_NAME:
                    name = (String) packet.getValue();
                    break;
                case PROPERTY_VERSION:
                    version = (String) packet.getValue();
                    break;
                }

                for (ControllerListener listener : controllerListeners)
                    listener.propertyChanged(packet.getPropertyID(), packet.getValue());
                break;
            }
            case PacketS2CSourceDeleted.ID:
            {
                PacketS2CSourceDeleted packet = new PacketS2CSourceDeleted(buffer);
                sources.remove(packet.getSourceId());

                Log.i(LOG_TAG, String.format("Source #%d deleted.", packet.getSourceId()));

                for (SourcesListener listener : sourcesListeners)
                    listener.sourceRemoved(packet.getSourceId());
                break;
            }
            case PacketS2CSourceDescriptiveText.ID:
            {
                PacketS2CSourceDescriptiveText packet = new PacketS2CSourceDescriptiveText(buffer);
                Source source = getSource(packet.getSourceId());
                if (source != null)
                {
                    if (packet.getDisplayTime() == 0)
                        source.setPermanentDescriptiveText(packet.getText());
                    else
                        for (SourcesListener listener : sourcesListeners)
                            listener.descriptiveText(source, packet.getText(), packet.getDisplayTime());
                }
                break;
            }
            case PacketS2CMediaMetadata.ID:
            {
                PacketS2CMediaMetadata packet = new PacketS2CMediaMetadata(buffer);
                Source source = getSource(packet.getSourceId());
                if (source != null)
                {
                    source.setMediaMetadata(packet.getTitle(), packet.getArtist(), packet.getArtworkUrl());
                }
                break;
            }
            case PacketS2CMediaPlayState.ID:
            {
                PacketS2CMediaPlayState packet = new PacketS2CMediaPlayState(buffer);
                Source source = getSource(packet.getSourceId());
                if (source != null)
                {
                    source.setMediaPlayState(packet.getPlaying());
                }
                break;
            }
            case PacketS2CSourceInfo.ID:
            {
                PacketS2CSourceInfo packet = new PacketS2CSourceInfo(buffer);
                Source source = getSource(packet.getSourceId());
                if (source == null)
                {
                    source = new Source(packet.getSourceId(), packet.getSourceName(), packet.getType(), this);
                    sources.put(packet.getSourceId(), source);

                    Log.i(LOG_TAG, String.format("Source #%d created (Type #%d)", packet.getSourceId(), packet.getType()));

                    for (SourcesListener listener : sourcesListeners)
                        listener.sourceAdded(source);
                }
                else
                {
                    source.setName(packet.getSourceName(), true);
                    source.setType(packet.getType(), true);
                }
                break;
            }
            case PacketS2CSourceProperty.ID:
            {
                PacketS2CSourceProperty packet = new PacketS2CSourceProperty(buffer);
                Source source = getSource(packet.getSourceId());
                if (source != null)
                {
                    switch (packet.getPropertyID())
                    {
                    case Source.PROPERTY_AUTO_OFF:
                        source.setAutoOff((Boolean) packet.getPropertyValue(), true);
                        break;
                    case Source.PROPERTY_AUTO_ON_ZONES:
                        source.setAutoOnZones((int[][]) packet.getPropertyValue(), true);
                        break;
                    }
                }
                break;
            }
            case PacketS2CZoneName.ID:
            {
                PacketS2CZoneName packet = new PacketS2CZoneName(buffer);
                if (zones.get(packet.getControllerId()) == null)
                {
                    zones.put(packet.getControllerId(), new SparseArray<Zone>());
                    Log.i(LOG_TAG,
                            String.format("Created controller #%d", packet.getControllerId()));
                }

                Zone zone = zones.get(packet.getControllerId()).get(packet.getZoneId());
                if (zone == null)
                {
                    zone = new Zone(packet.getControllerId(), packet.getZoneId(), this);
                    zones.get(packet.getControllerId()).put(packet.getZoneId(), zone);

                    Log.i(LOG_TAG,
                            String.format("Created zone #%d-%d", packet.getControllerId(),
                                    packet.getZoneId()));

                    for (ZonesListener listener : zonesListeners)
                        listener.zoneAdded(zone);
                }

                zone.setName(packet.getZoneName(), true);
                break;
            }
            case PacketS2CZoneDeleted.ID:
            {
                PacketS2CZoneDeleted packet = new PacketS2CZoneDeleted(buffer);
                deleteZone(packet.getControllerId(), packet.getZoneId(), true);
                break;
            }
            case PacketS2CZonePower.ID:
            {
                PacketS2CZonePower packet = new PacketS2CZonePower(buffer);
                if (zones.get(packet.getControllerId()) != null)
                {
                    Zone zone = zones.get(packet.getControllerId()).get(packet.getZoneId());
                    if (zone != null)
                        zone.setPower(packet.getPowered(), true);
                }
                break;
            }
            case PacketS2CZoneParameter.ID:
            {
                PacketS2CZoneParameter packet = new PacketS2CZoneParameter(buffer);
                if (zones.get(packet.getControllerId()) != null)
                {
                    Zone zone = zones.get(packet.getControllerId()).get(packet.getZoneId());
                    if (zone != null)
                        zone.setParameter(packet.getParameterId(), packet.getParameterValue(),
                                true);
                }
                break;
            }
            case PacketS2CZoneSource.ID:
            {
                PacketS2CZoneSource packet = new PacketS2CZoneSource(buffer);
                if (zones.get(packet.getControllerId()) != null)
                {
                    Zone zone = zones.get(packet.getControllerId()).get(packet.getZoneId());
                    if (zone != null)
                        zone.setSourceId(packet.getSourceId(), true);
                }
                break;
            }
            case PacketS2CZoneVolume.ID:
            {
                PacketS2CZoneVolume packet = new PacketS2CZoneVolume(buffer);
                if (zones.get(packet.getControllerId()) != null)
                {
                    Zone zone = zones.get(packet.getControllerId()).get(packet.getZoneId());
                    if (zone != null)
                        zone.setVolume(packet.getVolume(), true);
                }
                break;
            }
            case PacketS2CZoneMaxVolume.ID:
            {
                PacketS2CZoneMaxVolume packet = new PacketS2CZoneMaxVolume(buffer);
                if (zones.get(packet.getControllerId()) != null)
                {
                    Zone zone = zones.get(packet.getControllerId()).get(packet.getZoneId());
                    if (zone != null)
                    {
                        zone.setMaxVolume(packet.getMaxVolume(), true);
                    }
                }

                break;
            }
            case PacketS2CUpdateAvailable.ID:
            {
                PacketS2CUpdateAvailable packet = new PacketS2CUpdateAvailable(buffer);
                newVersion = packet.getNewVersion();
                for (ControllerListener listener : controllerListeners)
                    listener.updateAvailable();
            }
            default:
                Log.w(LOG_TAG, String.format("Received invalid packet %d", packetType));
            }
        }
        else if (packetType == PacketS2CZoneIndex.ID)
        {
            PacketS2CZoneIndex packet = new PacketS2CZoneIndex(buffer);
            for (int[] zoneInfo : packet.getIndex())
            {
                if (zones.get(zoneInfo[0]) == null)
                {
                    zones.put(zoneInfo[0], new SparseArray<Zone>());
                    Log.i(LOG_TAG,
                            String.format("Created controller #%d", zoneInfo[0]));
                }

                Zone zone = zones.get(zoneInfo[0]).get(zoneInfo[1]);
                if (zone == null)
                {
                    zone = new Zone(zoneInfo[0], zoneInfo[1], this);
                    zones.get(zoneInfo[0]).put(zoneInfo[1], zone);

                    Log.i(LOG_TAG,
                            String.format("Created zone #%d-%d", zoneInfo[0], zoneInfo[1]));
                }
            }

            for (ZonesListener listener : zonesListeners)
                listener.indexReceived();

            if (!receivedIndex && intent == INTENT_SUBSCRIBE)
                for (ConnectivityListener listener : connectivityListeners)
                    listener.ready();

            receivedIndex = true;
        }
    }

    public void sendPacket(RNetPacket packet)
    {
        if (channel != null)
        {
            try
            {
                channel.write(ByteBuffer.wrap(packet.getData()));
            }
            catch (IOException | NotYetConnectedException e)
            {
                e.printStackTrace();
            }
        }
    }

    private void cleanUp()
    {
        for (int i = 0; i < zones.size(); i++)
        {
            int k = zones.keyAt(i);
            zones.get(k).clear();
        }
        zones.clear();
        sources.clear();
        //serialConnected = false;
        receivedIndex = false;
        version = "<unknown>";
        newVersion = null;

        for (ZonesListener listener : zonesListeners)
            listener.cleared();
        for (SourcesListener listener : sourcesListeners)
            listener.cleared();
    }

    public enum ZoneChangeType
    {
        NAME, POWER, VOLUME, SOURCE, MAX_VOLUME, PARAMETER
    }

    public enum SourceChangeType
    {
        NAME, TYPE, METADATA, AUTO_ON, PLAYSTATE, AUTO_OFF
    }

    public interface ConnectivityListener
    {
        void connectionInitiated();
        void connectError();
        void ready();
        void disconnected(boolean unexpected);
    }

    public interface ControllerListener
    {
        void updateAvailable();
        void propertyChanged(int prop, Object value);
    }

    public interface ZonesListener
    {
        void indexReceived();
        void zoneAdded(Zone zone);
        void zoneChanged(Zone zone, boolean setRemotely, ZoneChangeType type);
        void zoneRemoved(int controllerId, int zoneId);
        void cleared();
    }

    public interface SourcesListener
    {
        void sourceAdded(Source source);
        void sourceChanged(Source source, boolean setRemotely,
                SourceChangeType type);
        void descriptiveText(Source source, String text, int length);
        void sourceRemoved(int sourceId);
        void cleared();
    }

    public static class SendPacketTask extends AsyncTask<RNetPacket, Void, Void>
    {
        private final WeakReference<RNetServer> serverReference;

        public SendPacketTask(RNetServer server)
        {
            serverReference = new WeakReference<>(server);
        }

        @Override
        protected Void doInBackground(RNetPacket... rNetPackets)
        {
            RNetServer server = serverReference.get();
            if (server != null)
                for (RNetPacket packet : rNetPackets)
                    server.sendPacket(packet);

            return null;
        }
    }

    public class ServerRunnable implements Runnable
    {
        @Override
        public void run()
        {
            RNetServer.this.run();
        }
    }
}
