package me.zachcheatham.rnetremote;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;

import me.zachcheatham.rnetremotecommon.rnet.RNetServer;
import me.zachcheatham.rnetremotecommon.rnet.RNetServerService;

public class MainActivity extends WearableActivity implements RNetServer.StateListener
{
    private static final String PREFS = "rnet_remote";

    private View connectingPlaceholder;
    private TextView connectingPlaceholderText;

    private ConnectivityManager connectivityManager;
    private boolean connectedToNetwork = false;
    private boolean boundToServerService = false;
    private RNetServer server;
    private RNetServerService serverService;
    private ServiceConnection serviceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            RNetServerService.LocalBinder binder = (RNetServerService.LocalBinder) service;
            serverService = binder.getService();
            server = serverService.getServer();
            boundToServerService = true;

            if (serverService.hasServerInfo())
            {
                if (!server.isRunning())
                {
                    if (connectedToNetwork)
                        serverService.startServerConnection();
                }
                else
                {
                    setConnectingVisible(!server.isReady());
                    if (server.isReady())
                        propertyChanged(RNetServer.PROPERTY_NAME, server.getName());
                }
            }
            else
            {
                promptSelectServer(false);
            }

            server.addStateListener(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            server.removeStateListener(MainActivity.this);

            boundToServerService = false;
            serverService = null;
            server = null;
        }
    };

    private void promptSelectServer(boolean cancelable)
    {
        Intent intent = new Intent(this, SelectServerActivity.class);
        if (!cancelable)
        {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("cancelable", false);
            startActivity(intent);
            finish();
        }
        else
        {
            startActivityForResult(intent, 1);
        }
    }

    private ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback()
    {
        @Override
        public void onAvailable(Network network)
        {
            if (connectivityManager.bindProcessToNetwork(network))
            {
                connectedToNetwork = true;
                if (boundToServerService && serverService.hasServerInfo() && !server.isRunning())
                {
                    serverService.startServerConnection();
                }
            }
        }

        @Override
        public void onLost(Network network)
        {
            super.onLost(network);
            connectedToNetwork = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        setContentView(R.layout.activity_main);

        connectingPlaceholder = findViewById(R.id.connecting_placeholder);
        connectingPlaceholderText = findViewById(R.id.text_view_connecting_placeholder_notice);

        // Enables Always-on
        setAmbientEnabled();
    }

    private void setConnectingVisible(boolean visible)
    {
        if (visible)
        {
            connectingPlaceholder.setVisibility(View.VISIBLE);
            //zoneList.setVisibility(View.GONE);
        }
        else
        {
            connectingPlaceholder.setVisibility(View.GONE);
            //zoneList.setVisibility(View.VISIBLE);
        }
    }

    private void setConnectingError(boolean visible)
    {
        if (visible)
        {
            connectingPlaceholderText.setVisibility(View.VISIBLE);
        }
        else
        {
            connectingPlaceholderText.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();

        connectivityManager.requestNetwork(request, networkCallback);

        Intent intent = new Intent(this, RNetServerService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        unbindService(serviceConnection);

        if (server != null)
            server.removeStateListener(this);

        connectivityManager.bindProcessToNetwork(null);
        connectivityManager.unregisterNetworkCallback(networkCallback);
    }

    @Override
    public void connectionInitiated()
    {
        runOnUiThread(new Runnable()
        {
            @SuppressWarnings("ConstantConditions")
            @Override
            public void run()
            {
                setConnectingVisible(true);
            }
        });
    }

    @Override
    public void connectError()
    {
        this.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                setConnectingError(true);
            }
        });
    }

    @Override
    public void ready()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                setConnectingVisible(false);
                setConnectingError(false);
            }
        });
    }

    @Override
    public void updateAvailable() {}

    @Override
    public void propertyChanged(int prop, Object value) {}

    @Override
    public void disconnected(final boolean unexpected)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (unexpected)
                    setConnectingError(true);
                setConnectingVisible(true);
            }
        });
    }

    private static class SetServerInfoTask extends AsyncTask<String, Void, Void>
    {
        private final WeakReference<RNetServerService> serverServiceReference;
        private final WeakReference<SharedPreferences> preferencesReference;

        SetServerInfoTask(RNetServerService service, SharedPreferences preferences)
        {
            serverServiceReference = new WeakReference<>(service);
            preferencesReference = new WeakReference<>(preferences);
        }

        @Override
        protected Void doInBackground(String... serverInfo)
        {
            String name = serverInfo[0];
            InetAddress address;
            try
            {
                address = InetAddress.getByName(serverInfo[1]);
            }
            catch (UnknownHostException e)
            {
                return null;
            }
            int port = Integer.parseInt(serverInfo[2]);

            SharedPreferences settings = preferencesReference.get();
            if (settings != null)
            {
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("server_name", name);
                editor.putString("server_address", address.getHostAddress());
                editor.putInt("server_port", port);
                editor.apply();
            }

            RNetServerService serverService = serverServiceReference.get();
            if (serverService != null)
            {
                serverService.stopServerConnection();
                while (!serverService.getServer().canStartConnection())
                    try {Thread.sleep(500);}
                    catch (InterruptedException e) {break;}

                serverService.setConnectionInfo(name, address, port);
                serverService.startServerConnection();
            }

            return null;
        }
    }
}