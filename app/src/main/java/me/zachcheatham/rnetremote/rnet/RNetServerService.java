package me.zachcheatham.rnetremote.rnet;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;

import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class RNetServerService extends Service implements RNetServer.StateListener
{
    private static final String PREFS = "rnet_remote";
    private static final String LOG_TAG = "RNetServerService";

    private final IBinder binder = new LocalBinder();
    private int serviceClients = 0;

    private String name;
    private InetAddress address = null;
    private int port;
    private RNetServer server;
    private Thread connectingThread;

    @Override
    public void onCreate()
    {
        super.onCreate();
        server = new RNetServer(getBluetoothName());
        server.addStateListener(this);

        SharedPreferences settings = getSharedPreferences(PREFS, 0);
        String name = settings.getString("server_name", "");
        if (name.length() > 0)
        {
            this.name = name;
            this.port = settings.getInt("server_port", 0);
            try
            {
                this.address = InetAddress.getByName(settings.getString("server_address", ""));
            }
            catch (UnknownHostException e)
            {
                this.name = null;
                this.port = -1;
                this.address = null;
            }
        }

        Log.d(LOG_TAG, "Service created.");
    }

    @Override
    public void onDestroy()
    {
        if (server.isRunning())
            server.disconnect();

        Log.d(LOG_TAG, "Service destroyed.");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        serviceClients++;
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        serviceClients--;
        return super.onUnbind(intent);
    }

    @Override
    public void connectionInitiated() {}

    @Override
    public void connectError()
    {
        Log.d(LOG_TAG, "Server connect error.");

        if (connectingThread == null)
        {
            Log.d(LOG_TAG, "Will restart in 5 seconds.");
            connectingThread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        Thread.sleep(5000);
                        if (serviceClients > 0 && !server.isRunning())
                            new Thread(server.new ServerRunnable()).start();
                    }
                    catch (InterruptedException ignored) {}
                    connectingThread = null;
                }
            });
            connectingThread.start();
        }
    }

    @Override
    public void connected() {}

    @Override
    public void serialStateChanged(boolean connected) {}

    @Override
    public void disconnected(boolean unexpected)
    {
        Log.d(LOG_TAG, "Server disconnected.");
        if (unexpected && connectingThread == null)
        {
            Log.d(LOG_TAG, "Will restart in 5 seconds.");
            connectingThread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        Thread.sleep(5000);
                        if (serviceClients > 0 && !server.isRunning())
                            new Thread(server.new ServerRunnable()).start();
                    }
                    catch (InterruptedException ignored) {}
                    connectingThread = null;
                }
            });
            connectingThread.start();
        }
    }

    public String getServerName()
    {
        return name;
    }

    public class LocalBinder extends Binder
    {
        public RNetServerService getService()
        {
            return RNetServerService.this;
        }
    }

    public boolean hasServerInfo()
    {
        return name != null;
    }

    public void setConnectionInfo(String name, InetAddress address, int port)
    {
        this.name = name;
        this.address = address;
        this.port = port;

        SharedPreferences settings = getSharedPreferences(PREFS, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("server_name", name);
        editor.putString("server_address", address.getHostAddress());
        editor.putInt("server_port", port);
        editor.apply();
    }

    public void startServerConnection()
    {
        server.setConnectionInfo(address, port);
        new Thread(server.new ServerRunnable()).start();
    }

    public void stopServerConnection()
    {
        if (server.isConnected())
            server.disconnect();
    }

    public RNetServer getServer()
    {
        return server;
    }

    private static String getBluetoothName()
    {
        return BluetoothAdapter.getDefaultAdapter().getName();
    }
}
