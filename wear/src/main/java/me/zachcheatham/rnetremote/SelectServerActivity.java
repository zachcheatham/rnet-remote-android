package me.zachcheatham.rnetremote;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.wear.widget.WearableRecyclerView;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;

public class SelectServerActivity extends WearableActivity implements ServersAdapter.ItemClickListener
{
    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;

    private ServersAdapter adapter;
    private View searchingIndicator;
    private boolean cancelable = true;

    private ConnectivityManager connectivityManager;
    private boolean connectedToNetwork = false;

    private ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback()
    {
        @Override
        public void onAvailable(Network network)
        {
            if (connectivityManager.bindProcessToNetwork(network))
            {
                connectedToNetwork = true;
                nsdManager.discoverServices("_rnet._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener);
            }

            Log.d("SelectServer", "NETWORK AVAILABLE");
        }

        @Override
        public void onUnavailable()
        {
            super.onUnavailable();
            Log.d("SelectServer", "NETWORK UNAVAILABLE");
        }

        @Override
        public void onLost(Network network)
        {
            super.onLost(network);
            connectedToNetwork = false;
            nsdManager.stopServiceDiscovery(discoveryListener);
            Log.d("SelectServer", "NETWORK LOST");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);


        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        Bundle extras = getIntent().getExtras();
        if (extras != null)
            cancelable = getIntent().getExtras().getBoolean("cancelable", true);

        nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        createNSDListener();
        //noinspection ResultOfMethodCallIgnored
        createResolveListener();

        setContentView(R.layout.activity_select_server);

        adapter = new ServersAdapter(getString(R.string.dialog_change_server), this);

        WearableRecyclerView recyclerView = findViewById(R.id.list_servers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        searchingIndicator = findViewById(R.id.searching);
    }

    @Override
    public void onStart()
    {
        super.onStart();

        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build();
        connectivityManager.requestNetwork(request, networkCallback);
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        connectivityManager.bindProcessToNetwork(null);
        connectivityManager.unregisterNetworkCallback(networkCallback);

        nsdManager.stopServiceDiscovery(discoveryListener);

        adapter.clearServers();
        searchingIndicator.setVisibility(View.VISIBLE);
    }

    private void createNSDListener()
    {
        discoveryListener = new NsdManager.DiscoveryListener()
        {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {}

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {}

            @Override
            public void onDiscoveryStarted(String serviceType) {}

            @Override
            public void onDiscoveryStopped(String serviceType)
            {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run()
                    {
                        adapter.clearServers();
                        searchingIndicator.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo)
            {
                nsdManager.resolveService(serviceInfo, createResolveListener());
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo)
            {
                adapter.removeServer(serviceInfo.getHost(), serviceInfo.getPort());
                if (adapter.getItemCount() < 3)
                {
                    searchingIndicator.setVisibility(View.VISIBLE);
                }
            }
        };
    }

    private NsdManager.ResolveListener createResolveListener()
    {
        return new NsdManager.ResolveListener()
        {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {}

            @Override
            public void onServiceResolved(final NsdServiceInfo serviceInfo)
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        adapter.addServer(
                                serviceInfo.getServiceName(),
                                serviceInfo.getHost(),
                                serviceInfo.getPort()
                        );

                        searchingIndicator.setVisibility(View.GONE);
                    }
                });
            }
        };
    }

    @Override
    public void onItemClick(int position)
    {
        ServersAdapter.RNetServer server = adapter.getServer(position);
        if (cancelable)
        {
            Intent intent = new Intent();
            intent.putExtra("host", server.host);
            intent.putExtra("port", server.port);
            intent.putExtra("name", server.name);
            setResult(1, intent);

            Log.d("SSA", "FINISH WITH RESULT");
        }
        else
        {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("server_name", server.name);
            intent.putExtra("server_host", server.host);
            intent.putExtra("server_port", server.port);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            Log.d("SSA", "FINISH START ACT");
        }

        finish();
    }
}
