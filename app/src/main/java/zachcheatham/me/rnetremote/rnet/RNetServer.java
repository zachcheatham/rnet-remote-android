package zachcheatham.me.rnetremote.rnet;

import android.os.AsyncTask;
import android.util.Log;
import android.util.SparseArray;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import zachcheatham.me.rnetremote.rnet.packet.PacketC2SName;
import zachcheatham.me.rnetremote.rnet.packet.PacketS2CName;
import zachcheatham.me.rnetremote.rnet.packet.PacketS2CRNetStatus;
import zachcheatham.me.rnetremote.rnet.packet.PacketS2CSourceDeleted;
import zachcheatham.me.rnetremote.rnet.packet.PacketS2CSourceName;
import zachcheatham.me.rnetremote.rnet.packet.PacketS2CZoneDeleted;
import zachcheatham.me.rnetremote.rnet.packet.PacketS2CZoneName;
import zachcheatham.me.rnetremote.rnet.packet.PacketS2CZonePower;
import zachcheatham.me.rnetremote.rnet.packet.PacketS2CZoneSource;
import zachcheatham.me.rnetremote.rnet.packet.PacketS2CZoneVolume;
import zachcheatham.me.rnetremote.rnet.packet.RNetPacket;

public class RNetServer
{
    private static final String LOG_TAG = "RNetServer";

    private final String clientName;
    private final StateListener stateListener;

    private SocketChannel channel;
    private InetAddress address;
    private int port;

    private final ByteBuffer pendingBuffer = ByteBuffer.allocate(255);
    private byte pendingPacketType = 0x00;
    private short pendingRemainingBytes = -1;

    private boolean run;
    private boolean sentName = false;
    private boolean serialConnected = false;
    private SparseArray<Source> sources = new SparseArray<>();
    private SparseArray<SparseArray<Zone>> zones = new SparseArray<>();

    private List<ZonesListener> zonesListeners = new ArrayList<>();

    public RNetServer(String clientName, StateListener stateListener)
    {
        this.clientName = clientName;
        this.stateListener = stateListener;

        pendingBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    public void setConnectionInfo(InetAddress address, int port)
    {
        this.address = address;
        this.port = port;
    }

    private void run()
    {
        if (channel != null)
        {
            throw new IllegalStateException("RNetServer already running.");
        }

        if (port == 0 || address == null)
        {
            throw new IllegalStateException("Connection information hasn't been set yet.");
        }

        cleanUp();

        run = true;

        try
        {
            channel = SocketChannel.open();
            channel.connect(new InetSocketAddress(address, port));

            readChannel();

            try
            {
                channel.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            stateListener.connectError();
        }

        if (sentName)
        {
            stateListener.disconnected(run);
        }

        sentName = false;
        serialConnected = false;
        channel = null;
    }

    private void sendPacket(RNetPacket packet)
    {
        try
        {
            channel.write(ByteBuffer.wrap(packet.getData()));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void disconnect()
    {
        run = false;
        try
        {
            channel.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public boolean isConnected()
    {
        return sentName;
    }

    public boolean hasSentName()
    {
        return sentName;
    }

    public boolean isSerialConnected()
    {
        return serialConnected;
    }

    public Zone getZone(int controllerId, int zoneId)
    {
        if (zones.get(controllerId) != null)
        {
            return zones.get(controllerId).get(zoneId);
        }

        return null;
    }

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

    public void addZoneListener(ZonesListener listener)
    {
        zonesListeners.add(listener);
    }

    List<ZonesListener> getZonesListeners()
    {
        return zonesListeners;
    }

    /*public void removeZoneListener(ZonesListener listener)
    {
        zonesListeners.remove(listener);
    }*/

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
                            pendingPacketType = incomingBuffer.get();
                            pendingRemainingBytes = incomingBuffer.get();
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
                            pendingPacketType = 0x00;
                            pendingBuffer.clear();
                            pendingBuffer.rewind();
                        }
                    }

                    incomingBuffer.clear();
                }
            }
        }
        catch (IOException e)
        {
            //e.printStackTrace();
        }
    }

    private void constructAndHandlePacket(byte packetType, ByteBuffer buffer)
    {
        switch (packetType)
        {
            case PacketS2CName.ID:
                sendName();
                break;
            case PacketS2CRNetStatus.ID:
            {
                PacketS2CRNetStatus packet = new PacketS2CRNetStatus(buffer);
                serialConnected = packet.getRNetConnected();
                stateListener.serialStateChanged(packet.getRNetConnected());
                break;
            }
            case PacketS2CSourceDeleted.ID:
            {
                PacketS2CSourceDeleted packet = new PacketS2CSourceDeleted(buffer);
                sources.remove(packet.getSourceId());
                Log.i(LOG_TAG, String.format("Source #%d deleted.", packet.getSourceId()));
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
                Log.i(LOG_TAG, String.format("Source #%d renamed to %s", packet.getSourceId(), packet.getSourceName()));
                break;
            }
            case PacketS2CZoneName.ID:
            {
                PacketS2CZoneName packet = new PacketS2CZoneName(buffer);
                if (zones.get(packet.getControllerId()) == null)
                {
                    zones.put(packet.getControllerId(), new SparseArray<Zone>());
                    Log.i(LOG_TAG, String.format("Created controller #%d", packet.getControllerId()));
                }

                Zone zone = zones.get(packet.getControllerId()).get(packet.getZoneId());
                if (zone == null)
                {
                    zone = new Zone(packet.getControllerId(), packet.getZoneId(), this);
                    zones.get(packet.getControllerId()).put(packet.getZoneId(), zone);

                    Log.i(LOG_TAG, String.format("Created zone #%d-%d", packet.getControllerId(), packet.getZoneId()));

                    for (ZonesListener listener : zonesListeners)
                        listener.zoneAdded(zone);
                }

                zone.setName(packet.getZoneName(), true);
                break;
            }
            case PacketS2CZoneDeleted.ID:
            {
                PacketS2CZoneDeleted packet = new PacketS2CZoneDeleted(buffer);
                if (zones.get(packet.getControllerId()) != null)
                {
                    zones.get(packet.getControllerId()).remove(packet.getZoneId());
                    Log.i(LOG_TAG, String.format("Deleted zone #%d-%d", packet.getControllerId(), packet.getZoneId()));

                    if (zones.get(packet.getControllerId()).size() < 1)
                    {
                        zones.remove(packet.getControllerId());
                        Log.i(LOG_TAG, String.format("Deleted controller #%d", packet.getControllerId()));
                    }

                    for (ZonesListener listener : zonesListeners)
                        listener.zoneRemoved(packet.getControllerId(), packet.getZoneId());
                }
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
            default:
                Log.w(LOG_TAG, String.format("Received invalid packet %d", packetType));
        }
    }

    private void sendName()
    {
        sendPacket(new PacketC2SName(clientName));

        if (!sentName)
        {
            sentName = true;
            stateListener.connected();
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
        sentName = false;

        for (ZonesListener listener : zonesListeners)
            listener.dataReset();
    }

    public class ServerRunnable implements Runnable
    {
        @Override
        public void run()
        {
            RNetServer.this.run();
        }
    }

    public class SendPacketTask extends AsyncTask<RNetPacket, Void, Void>
    {
        @Override
        protected Void doInBackground(RNetPacket... rNetPackets)
        {
            for (RNetPacket packet : rNetPackets)
                sendPacket(packet);

            return null;
        }
    }

    public interface StateListener
    {
        void connectError();
        void connected();
        void serialStateChanged(boolean connected);
        void disconnected(boolean unexpected);
    }

    public interface ZonesListener
    {
        void dataReset();
        void zoneAdded(Zone zone);
        void zoneChanged(Zone zone, boolean setRemotely, ZoneChangeType type);
        void zoneRemoved(int controllerId, int zoneId);
    }

    public enum ZoneChangeType
    {
        NAME, POWER, VOLUME, SOURCE
    }
}
