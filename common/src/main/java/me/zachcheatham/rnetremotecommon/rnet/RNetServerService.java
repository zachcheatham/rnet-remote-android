package me.zachcheatham.rnetremotecommon.rnet;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class RNetServerService extends Service implements RNetServer.ConnectivityListener
{
    private static final String PREFS = "rnet_remote";
    private static final String LOG_TAG = "RNetServerService";

    private final Handler handler = new Handler();
    private final IBinder binder = new LocalBinder();

    private boolean started = false;
    private String name;
    private InetAddress address = null;
    private int port;
    private RNetServer server;

    private final Runnable delayedShutdown = new Runnable()
    {
        @Override
        public void run()
        {
            RNetServerService.this.stopSelf();
        }
    };

    private final Runnable delayedReconnect = new Runnable()
    {
        @Override
        public void run()
        {
            if (!server.isRunning())
                new Thread(server.new ServerRunnable()).start();
        }
    };

    @Override
    public void onCreate()
    {
        super.onCreate();
        server = new RNetServer(RNetServer.INTENT_SUBSCRIBE);
        server.addConnectivityListener(this);

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
        started = false;
        server.disconnect();
        handler.removeCallbacks(delayedReconnect);
        Log.d(LOG_TAG, "Service destroyed.");
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        if (!started)
        {
            started = true;
            startService(new Intent(this, RNetServerService.class));
        }

        cancelShutdown();
        return binder;
    }

    @Override
    public void onRebind(Intent intent)
    {
        super.onRebind(intent);
        cancelShutdown();
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        handler.postDelayed(delayedShutdown, 1000);
        return true;
    }

    @Override
    public void connectionInitiated() {}

    @Override
    public void connectError()
    {
        Log.d(LOG_TAG, "Server connect error.");

        handler.removeCallbacks(delayedReconnect);
        Log.d(LOG_TAG, "Will restart in 5 seconds.");

        handler.postDelayed(delayedReconnect, 5000);
    }

    @Override
    public void ready() {}

    @Override
    public void disconnected(boolean unexpected)
    {
        Log.d(LOG_TAG, "Server disconnected.");
        if (unexpected)
        {
            handler.removeCallbacks(delayedReconnect);
            Log.d(LOG_TAG, "Will restart in 5 seconds.");
            handler.postDelayed(delayedReconnect, 5000);
        }
    }

    public String getSavedServerName()
    {
        return name;
    }

    public void setSavedServerName(String name)
    {
        this.name = name;
    }

    public boolean hasServerInfo()
    {
        return name != null;
    }

    public boolean serverInfoEquals(String hostname, Integer port)
    {
        return (hostname.equals(address.getHostAddress()) && port == this.port);
    }

    public void setConnectionInfo(String name, InetAddress address, int port)
    {
        this.name = name;
        this.address = address;
        this.port = port;
    }

    public void startServerConnection()
    {
        handler.removeCallbacks(delayedReconnect);
        server.setConnectionInfo(address, port);
        new Thread(server.new ServerRunnable()).start();
    }

    public void stopServerConnection()
    {
        server.disconnect();
    }

    public RNetServer getServer()
    {
        return server;
    }

    private void cancelShutdown()
    {
        handler.removeCallbacks(delayedShutdown);
    }

    public class LocalBinder extends Binder
    {
        public RNetServerService getService()
        {
            return RNetServerService.this;
        }
    }
}
