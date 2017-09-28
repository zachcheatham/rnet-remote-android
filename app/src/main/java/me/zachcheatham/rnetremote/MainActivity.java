package me.zachcheatham.rnetremote;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.net.InetAddress;

import me.zachcheatham.rnetremote.rnet.RNetServer;
import me.zachcheatham.rnetremote.rnet.RNetServerService;
import me.zachcheatham.rnetremote.rnet.packet.PacketC2SAllPower;

public class MainActivity extends AppCompatActivity implements SelectServerDialogFragment.SelectServerListener,
        RNetServer.StateListener, View.OnClickListener, AddZoneDialogFragment.AddZoneListener
{
    @SuppressWarnings("unused")
    private static final String LOG_TAG = "MainActivity";

    private RecyclerView zoneList;
    private ZonesAdapter zoneAdapter;
    private View connectingPlaceholder;
    private TextView connectingPlaceholderText;
    private Button connectingPlaceholderButton;
    private Snackbar serialConnectionSnackbar;

    private boolean boundToServerService = false;
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
                promptSelectServer(false);
            else
            {
                //noinspection ConstantConditions
                getSupportActionBar().setTitle(serverService.getServerName());

                if (!server.isRunning())
                    serverService.startServerConnection();
                else
                    setConnectingVisible(!server.hasSentName());
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

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        zoneAdapter = new ZonesAdapter(this);

        zoneList = (RecyclerView) findViewById(R.id.list_zones);
        zoneList.setLayoutManager(new LinearLayoutManager(this));
        ((SimpleItemAnimator) zoneList.getItemAnimator()).setSupportsChangeAnimations(false);
        zoneList.setAdapter(zoneAdapter);

        connectingPlaceholder = findViewById(R.id.connecting_placeholder);
        connectingPlaceholderText = (TextView) findViewById(R.id.text_view_connecting_placeholder_notice);
        connectingPlaceholderButton = (Button) findViewById(R.id.button_connecting_placeholder_connect);
        connectingPlaceholderButton.setOnClickListener(this);

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
        getMenuInflater().inflate(R.menu.menu_remote, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        boolean connected = boundToServerService && server.hasSentName();

        MenuItem allPower = menu.findItem(R.id.action_power_all);
        allPower.setVisible(connected);

        MenuItem changeController = menu.findItem(R.id.action_change_server);
        changeController.setVisible(connected);

        return true;
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
                server.new SendPacketTask().execute(new PacketC2SAllPower(false));
            }
            else if (server.anyZonesOn())
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AppTheme_DialogOverlay));
                builder.setTitle(R.string.set_all_on_off);
                builder.setNegativeButton(R.string.action_all_off,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i)
                            {
                                server.new SendPacketTask().execute(new PacketC2SAllPower(false));
                            }
                        });
                builder.setPositiveButton(R.string.action_all_on,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i)
                            {
                                server.new SendPacketTask().execute(new PacketC2SAllPower(true));
                            }
                        });

                builder.create().show();
            }
            else
            {
                server.new SendPacketTask().execute(new PacketC2SAllPower(true));
            }
            return true;
        case R.id.action_add_zone:
            AddZoneDialogFragment dialog = new AddZoneDialogFragment();
            dialog.show(getSupportFragmentManager(), "AddZoneDialogFragment");
            return true;
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
        SelectServerDialogFragment dialog = new SelectServerDialogFragment();
        dialog.setCancelable(cancelable);
        dialog.show(getSupportFragmentManager(), "SelectServerDialogFragment");
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
    public void serverSelected(String name, InetAddress address, int port)
    {
        setConnectingError(false);
        serverService.stopServerConnection();
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
        runOnUiThread(new Runnable() {
            @SuppressWarnings("ConstantConditions")
            @Override
            public void run()
            {
                getSupportActionBar().setTitle(serverService.getServerName());
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
    public void connected()
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
    public void serialStateChanged(boolean connected)
    {
        if (!connected)
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

    @Override
    public void disconnected(final boolean unexpected)
    {
        runOnUiThread(new Runnable() {
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
