package me.zachcheatham.rnetremote.rnet;

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

import me.zachcheatham.rnetremote.rnet.packet.PacketC2SDeleteSource;
import me.zachcheatham.rnetremote.rnet.packet.PacketC2SDeleteZone;
import me.zachcheatham.rnetremote.rnet.packet.PacketC2SIntent;
import me.zachcheatham.rnetremote.rnet.packet.PacketC2SZoneName;
import me.zachcheatham.rnetremote.rnet.packet.PacketS2CRNetStatus;
import me.zachcheatham.rnetremote.rnet.packet.PacketS2CSourceDeleted;
import me.zachcheatham.rnetremote.rnet.packet.PacketS2CSourceName;
import me.zachcheatham.rnetremote.rnet.packet.PacketS2CZoneDeleted;
import me.zachcheatham.rnetremote.rnet.packet.PacketS2CZoneIndex;
import me.zachcheatham.rnetremote.rnet.packet.PacketS2CZoneMaxVolume;
import me.zachcheatham.rnetremote.rnet.packet.PacketS2CZoneName;
import me.zachcheatham.rnetremote.rnet.packet.PacketS2CZoneParameter;
import me.zachcheatham.rnetremote.rnet.packet.PacketS2CZonePower;
import me.zachcheatham.rnetremote.rnet.packet.PacketS2CZoneSource;
import me.zachcheatham.rnetremote.rnet.packet.PacketS2CZoneVolume;
import me.zachcheatham.rnetremote.rnet.packet.RNetPacket;

public class RNetServer
{
    public static final int INTENT_ACTION = 0x01;
    public static final int INTENT_SUBSCRIBE = 0x02;

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
    private boolean serialConnected = false;
    private SparseArray<Source> sources = new SparseArray<>();
    private SparseArray<SparseArray<Zone>> zones = new SparseArray<>();

    private List<StateListener> stateListeners = new ArrayList<>();
    private List<ZonesListener> zonesListeners = new ArrayList<>();

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

    public void createSource(int sourceId, String sourceName)
    {
        Source source = new Source(sourceId, this);
        source.setName(sourceName, false);
        sources.put(sourceId, source);
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

    public void addStateListener(StateListener listener)
    {
        stateListeners.add(listener);
    }

    public void removeStateListener(StateListener listener)
    {
        stateListeners.remove(listener);
    }

    public void addZoneListener(ZonesListener listener)
    {
        zonesListeners.add(listener);
    }

    List<ZonesListener> getZonesListeners()
    {
        return zonesListeners;
    }

    public void removeZoneListener(ZonesListener listener)
    {
        zonesListeners.remove(listener);
    }

    void run()
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

        for (StateListener listener : stateListeners)
            listener.connectionInitiated();

        try
        {
            channel = SocketChannel.open();
            channel.connect(new InetSocketAddress(address, port));

            Log.d(LOG_TAG, "Server socket opened.");

            sendPacket(new PacketC2SIntent(intent));
            if (intent == INTENT_ACTION)
                for (StateListener listener : stateListeners)
                    listener.ready();

            readChannel();

            try
            {
                channel.close();
            }
            catch (IOException ignored) {}

            if (isReady())
                for (StateListener listener : stateListeners)
                    listener.disconnected(run);
            else if (run) // Don't notify an error if we wanted to disconnect
                for (StateListener listener : stateListeners)
                    listener.connectError();
        }
        catch (IOException e)
        {
            for (StateListener listener : stateListeners)
                listener.connectError();
        }

        receivedIndex = false;
        serialConnected = false;
        channel = null;
        run = false;

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
            case PacketS2CRNetStatus.ID:
            {
                PacketS2CRNetStatus packet = new PacketS2CRNetStatus(buffer);
                serialConnected = packet.getRNetConnected();
                for (StateListener listener : stateListeners)
                    listener.serialStateChanged(packet.getRNetConnected());
                break;
            }
            case PacketS2CSourceDeleted.ID:
            {
                PacketS2CSourceDeleted packet = new PacketS2CSourceDeleted(buffer);
                sources.remove(packet.getSourceId());

                Log.i(LOG_TAG, String.format("Source #%d deleted.", packet.getSourceId()));

                for (ZonesListener listener : zonesListeners)
                    listener.sourcesChanged();
                break;
            }
            case PacketS2CSourceName.ID:
            {
                PacketS2CSourceName packet = new PacketS2CSourceName(buffer);
                Source source = sources.get(packet.getSourceId());
                if (source == null)
                {
                    source = new Source(packet.getSourceId(), this);
                    sources.put(packet.getSourceId(), source);

                    Log.i(LOG_TAG, String.format("Source #%d created", packet.getSourceId()));
                }
                source.setName(packet.getSourceName(), true);
                Log.i(LOG_TAG, String.format("Source #%d renamed to %s", packet.getSourceId(),
                        packet.getSourceName()));
                for (ZonesListener listener : zonesListeners)
                    listener.sourcesChanged();
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
                for (StateListener listener : stateListeners)
                    listener.ready();

            receivedIndex = true;
        }
    }

    void sendPacket(RNetPacket packet)
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
        serialConnected = false;
        receivedIndex = false;

        for (ZonesListener listener : zonesListeners)
            listener.cleared();
    }

    public enum ZoneChangeType
    {
        NAME, POWER, VOLUME, SOURCE, MAX_VOLUME, PARAMETER
    }

    public interface StateListener
    {
        void connectionInitiated();

        void connectError();

        void ready();

        void serialStateChanged(boolean connected);

        void disconnected(boolean unexpected);
    }

    public interface ZonesListener
    {
        void indexReceived();

        void sourcesChanged();

        void zoneAdded(Zone zone);

        void zoneChanged(Zone zone, boolean setRemotely, ZoneChangeType type);

        void zoneRemoved(int controllerId, int zoneId);

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
