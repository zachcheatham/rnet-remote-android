package me.zachcheatham.rnetremote;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.net.InetAddress;

import me.zachcheatham.rnetremote.rnet.RNetServer;
import me.zachcheatham.rnetremote.rnet.RNetServerService;
import me.zachcheatham.rnetremote.rnet.packet.PacketC2SAllPower;
import me.zachcheatham.rnetremote.rnet.packet.PacketC2SMute;
import me.zachcheatham.rnetremote.ui.GridAutofitLayoutManager;

public class MainActivity extends AppCompatActivity implements SelectServerListener,
        RNetServer.StateListener, View.OnClickListener, AddZoneDialogFragment.AddZoneListener,
        PopupMenu.OnMenuItemClickListener
{
    private static final String PREFS = "rnet_remote";
    private static final String PREFS_ORDER = "rnet_remote_zone_order";
    @SuppressWarnings("unused")
    private static final String LOG_TAG = "MainActivity";

    private RecyclerView zoneList;
    private ZonesAdapter zoneAdapter;
    private View connectingPlaceholder;
    private TextView connectingPlaceholderText;
    private Button connectingPlaceholderButton;
    private Snackbar serialConnectionSnackbar;

    private boolean boundToServerService = false;
    private boolean useVolumeKeys = false;
    private boolean serverPromptOpened = false;
    private RNetServer server;
    private RNetServerService serverService;
    private ServiceConnection serviceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder)
        {
            RNetServerService.LocalBinder binder = (RNetServerService.LocalBinder) iBinder;
            serverService = binder.getService();
            server = serverService.getServer();
            boundToServerService = true;

            if (!serverService.hasServerInfo())
            {
                if (!serverPromptOpened)
                    promptSelectServer(false);
            }
            else
            {
                //noinspection ConstantConditions
                getSupportActionBar().setTitle(serverService.getSavedServerName());

                if (!server.isRunning())
                {
                    serverService.startServerConnection();
                }
                else
                {
                    setConnectingVisible(!server.isReady());
                    if (server.isReady())
                        propertyChanged(RNetServer.PROPERTY_NAME, server.getName());
                }
            }

            server.addStateListener(MainActivity.this);
            zoneAdapter.setServer(server);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName)
        {
            server.removeStateListener(MainActivity.this);
            zoneAdapter.setServer(null);

            boundToServerService = false;
            serverService = null;
            server = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Configuration config = getResources().getConfiguration();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        zoneAdapter = new ZonesAdapter(this);

        zoneList = findViewById(R.id.list_zones);
        if (config.smallestScreenWidthDp < 600)
            zoneList.setLayoutManager(new LinearLayoutManager(this));
        else
            zoneList.setLayoutManager(new GridAutofitLayoutManager(this, 350));
        ((SimpleItemAnimator) zoneList.getItemAnimator()).setSupportsChangeAnimations(false);
        zoneList.setAdapter(zoneAdapter);

        connectingPlaceholder = findViewById(R.id.connecting_placeholder);
        connectingPlaceholderText = findViewById(R.id.text_view_connecting_placeholder_notice);
        connectingPlaceholderButton = findViewById(R.id.button_connecting_placeholder_connect);
        connectingPlaceholderButton.setOnClickListener(this);

        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        //noinspection ConstantConditions
        getSupportActionBar().setTitle("");
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        Intent intent = new Intent(this, RNetServerService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        SharedPreferences settings = getSharedPreferences(PREFS, 0);
        useVolumeKeys = settings.getBoolean("use_volume_keys", false);
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        unbindService(serviceConnection);

        if (server != null)
            server.removeStateListener(this);
        zoneAdapter.setServer(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        boolean connected = boundToServerService && server.isReady();

        menu.findItem(R.id.action_update).setVisible(connected & server.updateAvailable());
        menu.findItem(R.id.action_power_all).setVisible(connected);
        menu.findItem(R.id.action_change_server).setVisible(connected);
        menu.findItem(R.id.action_add_zone).setVisible(connected);
        menu.findItem(R.id.action_manage_sources).setVisible(connected);
        menu.findItem(R.id.action_toggle_mute).setVisible(connected);

        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (useVolumeKeys && boundToServerService && server.isReady())
        {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
            {
                server.volumeUp();
                return true;
            }
            else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
            {
                server.volumeDown();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
        case R.id.action_change_server:
            promptSelectServer(true);
            return true;
        case R.id.action_power_all:
            if (server.allZonesOn())
            {
                new RNetServer.SendPacketTask(server).execute(new PacketC2SAllPower(false));
            }
            else if (server.anyZonesOn())
            {
                PopupMenu menu = new PopupMenu(
                        new ContextThemeWrapper(this, R.style.AppTheme_PopupOverlay),
                        findViewById(R.id.action_power_all));
                menu.setOnMenuItemClickListener(this);
                menu.inflate(R.menu.all_on_off_popup);
                menu.show();
            }
            else
            {
                new RNetServer.SendPacketTask(server).execute(new PacketC2SAllPower(true));
            }
            return true;
        case R.id.action_toggle_mute:
            new RNetServer.SendPacketTask(server)
                    .execute(new PacketC2SMute(PacketC2SMute.MUTE_TOGGLE, (short) 0));
            return true;
        case R.id.action_add_zone:
            AddZoneDialogFragment dialog = new AddZoneDialogFragment();
            dialog.show(getSupportFragmentManager(), "AddZoneDialogFragment");
            return true;
        case R.id.action_manage_sources:
        {
            Intent intent = new Intent(this, ManageSourcesActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_left, R.anim.fade_out);
            return true;
        }
        case R.id.settings:
        {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_left, R.anim.fade_out);
            return true;
        }
        case R.id.action_update:
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    new ContextThemeWrapper(this, R.style.AppTheme_DialogOverlay));
            builder.setTitle(R.string.proxy_update_available);
            builder.setMessage(String.format(getString(R.string.proxy_update_available_desc), server.getNewVersion()));
            builder.setPositiveButton(getString(R.string.action_update),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            if (boundToServerService)
                            {
                                server.update();
                                notifyUpdating();
                            }
                        }
                    });
            builder.setNegativeButton(getString(android.R.string.cancel), null);
            builder.create().show();
            return true;
        }
        }

        return super.onOptionsItemSelected(item);
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
            connectingPlaceholderButton.setVisibility(View.VISIBLE);
            connectingPlaceholderText.setVisibility(View.VISIBLE);
        }
        else
        {
            connectingPlaceholderButton.setVisibility(View.GONE);
            connectingPlaceholderText.setVisibility(View.GONE);
        }
    }

    private void promptSelectServer(boolean cancelable)
    {
        DialogFragment dialog;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            dialog = new SelectServerDialogFragment();
        else
            dialog = new EnterServerDialogFragment();

        dialog.setCancelable(cancelable);
        dialog.show(getSupportFragmentManager(), "SelectServerDialogFragment");

        if (!cancelable)
            serverPromptOpened = true;
    }

    private void notifyUpdating()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(
                new ContextThemeWrapper(this, R.style.AppTheme_DialogOverlay));
        builder.setTitle(R.string.proxy_update_started);
        builder.setMessage(R.string.proxy_update_started_desc);
        builder.setPositiveButton(getString(android.R.string.ok), null);
        builder.create().show();
    }

    @Override
    public void onClick(View view)
    {
        if (view.equals(connectingPlaceholderButton))
        {
            promptSelectServer(true);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item)
    {
        switch (item.getItemId())
        {
        case R.id.action_all_on:
            new RNetServer.SendPacketTask(server).execute(new PacketC2SAllPower(true));
            return true;
        case R.id.action_all_off:
            new RNetServer.SendPacketTask(server).execute(new PacketC2SAllPower(false));
            return true;
        }

        return false;
    }

    @Override
    public void serverSelected(String name, InetAddress address, int port)
    {
        setConnectingError(false);

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(
                Context.CONNECTIVITY_SERVICE);
        assert connectivityManager != null;
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        SharedPreferences settings = getSharedPreferences(PREFS, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("server_name", name);
        editor.putString("server_address", address.getHostAddress());

        if (networkInfo.isConnected() && networkInfo.getType() == ConnectivityManager.TYPE_WIFI)
        {
            WifiManager wifiManager = (WifiManager) getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            assert wifiManager != null;
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            editor.putInt("server_network", wifiInfo.getNetworkId());
        }

        editor.putInt("server_port", port);
        editor.apply();

        settings = getSharedPreferences(PREFS_ORDER, 0);
        editor = settings.edit();
        editor.clear();
        editor.commit();

        serverService.stopServerConnection();
        while (!serverService.getServer().canStartConnection())
            try {Thread.sleep(500);}
            catch (InterruptedException e) {break;}

        serverService.setConnectionInfo(name, address, port);
        serverService.startServerConnection();
    }

    @Override
    public void addZone(String zoneName, int controllerId, int zoneId)
    {
        server.createZone(zoneName, controllerId, zoneId);
    }

    @Override
    public boolean zoneExists(int controllerId, int zoneId)
    {
        return server.getZone(controllerId, zoneId) != null;
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
                getSupportActionBar().setTitle(serverService.getSavedServerName());
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
                invalidateOptionsMenu();
            }
        });
    }

    @Override
    public void updateAvailable()
    {
        invalidateOptionsMenu();
    }

    @Override
    public void propertyChanged(int prop, Object value)
    {
        if (prop == RNetServer.PROPERTY_SERIAL_CONNECTED)
        {
            if (!((boolean) value))
            {
                serialConnectionSnackbar = Snackbar.make(findViewById(R.id.main_content),
                        R.string.notice_serial_disconnected, Snackbar.LENGTH_INDEFINITE);
                serialConnectionSnackbar.show();
            }
            else if (serialConnectionSnackbar != null && serialConnectionSnackbar.isShown())
            {
                serialConnectionSnackbar.dismiss();
                serialConnectionSnackbar = null;
            }
        }
        else if (prop == RNetServer.PROPERTY_NAME)
        {
            final String name = (String) value;
            if (!name.equals(serverService.getSavedServerName()))
            {
                serverService.setSavedServerName(name);

                SharedPreferences settings = getSharedPreferences(PREFS, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("server_name", name);
                editor.apply();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run()
                    {
                        //noinspection ConstantConditions
                        getSupportActionBar().setTitle(name);
                    }
                });
            }
        }
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

                invalidateOptionsMenu();
            }
        });

        if (serialConnectionSnackbar != null && serialConnectionSnackbar.isShown())
        {
            serialConnectionSnackbar.dismiss();
            serialConnectionSnackbar = null;
        }
    }
}
