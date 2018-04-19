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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.SnapHelper;
import android.support.wear.widget.WearableRecyclerView;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.TextView;

import java.net.InetAddress;

import me.zachcheatham.rnetremotecommon.rnet.RNetServer;
import me.zachcheatham.rnetremotecommon.rnet.RNetServerService;

public class MainActivity extends WearableActivity implements RNetServer.ConnectivityListener
{
    private static final String PREFS = "rnet_remote";

    private final Handler handler = new Handler();

    private View connectingPlaceholder;
    private TextView connectingPlaceholderText;
    private View wifiNotice;
    private WearableRecyclerView zoneList;
    private ZonesAdapter zoneAdapter;

    private ConnectivityManager connectivityManager;
    private boolean networkConnected = false;
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

            server.addConnectivityListener(MainActivity.this);

            zoneAdapter.setServer(server);

            Bundle extras = getIntent().getExtras();
            if (extras != null)
            {
                String serverName = extras.getString("server_name", null);
                if (serverName != null)
                {
                    InetAddress address = (InetAddress) extras.getSerializable("server_host");
                    int port = extras.getInt("server_port");

                    assert address != null;
                    serverSelected(serverName, address, port);

                    return;
                }
            }

            if (serverService.hasServerInfo())
            {
                if (!server.isRunning())
                {
                    if (networkConnected)
                        serverService.startServerConnection();
                }
                else
                {
                    setConnectingVisible(!server.isReady());
                }
            }
            else
            {
                promptSelectServer(false);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            server.removeConnectivityListener(MainActivity.this);
            zoneAdapter.setServer(null);

            boundToServerService = false;
            serverService = null;
            server = null;
        }
    };

    private ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback()
    {
        @Override
        public void onAvailable(Network network)
        {
            if (connectivityManager.bindProcessToNetwork(network))
            {
                networkConnected = true;
                if (boundToServerService && serverService.hasServerInfo() && !server.isRunning())
                {
                    serverService.startServerConnection();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run()
                    {
                        wifiNotice.setVisibility(View.GONE);
                    }
                });
            }
        }

        @Override
        public void onLost(Network network)
        {
            super.onLost(network);
            networkConnected = false;
        }
    };
    private Runnable checkNetworkConnected = new Runnable()
    {
        @Override
        public void run()
        {
            if (!networkConnected)
                runOnUiThread(new Runnable() {
                    @Override
                    public void run()
                    {
                        wifiNotice.setVisibility(View.VISIBLE);
                        setConnectingVisible(false);
                    }
                });

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        zoneAdapter = new ZonesAdapter(this);

        setContentView(R.layout.activity_main);

        connectingPlaceholder = findViewById(R.id.connecting_placeholder);
        connectingPlaceholderText = findViewById(R.id.text_view_connecting_placeholder_notice);
        wifiNotice = findViewById(R.id.text_view_wifi_notice);
        zoneList = findViewById(R.id.list_zones);
        zoneList.setLayoutManager(new LinearLayoutManager(this));
        zoneList.setAdapter(zoneAdapter);
        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(zoneList);

        // Enables Always-on
        //setAmbientEnabled();
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();

        connectivityManager.requestNetwork(request, networkCallback);
        handler.postDelayed(checkNetworkConnected, 5000);

        Intent intent = new Intent(this, RNetServerService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        unbindService(serviceConnection);

        if (server != null)
            server.removeConnectivityListener(this);
        zoneAdapter.setServer(null);

        connectivityManager.bindProcessToNetwork(null);
        connectivityManager.unregisterNetworkCallback(networkCallback);
        handler.removeCallbacks(checkNetworkConnected);
        networkConnected = false;
    }

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

    private void serverSelected(String name, InetAddress address, int port)
    {
        setConnectingError(false);

        SharedPreferences settings = getSharedPreferences(PREFS, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("server_name", name);
        editor.putString("server_address", address.getHostAddress());
        editor.putInt("server_port", port);

        editor.clear();
        editor.apply();

        serverService.stopServerConnection();
        while (!serverService.getServer().canStartConnection())
            try {Thread.sleep(500);}
            catch (InterruptedException e) {break;}

        serverService.setConnectionInfo(name, address, port);
        serverService.startServerConnection();
    }

    private void setConnectingVisible(boolean visible)
    {
        if (visible)
        {
            connectingPlaceholder.setVisibility(View.VISIBLE);
            zoneList.setVisibility(View.GONE);
        }
        else
        {
            connectingPlaceholder.setVisibility(View.GONE);
            zoneList.setVisibility(View.VISIBLE);
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
}