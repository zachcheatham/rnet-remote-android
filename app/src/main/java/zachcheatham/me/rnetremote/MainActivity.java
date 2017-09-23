package zachcheatham.me.rnetremote;

import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.net.InetAddress;
import java.net.UnknownHostException;

import zachcheatham.me.rnetremote.rnet.RNetServer;
import zachcheatham.me.rnetremote.rnet.packet.PacketC2SAllPower;

public class MainActivity extends AppCompatActivity implements SelectServerDialogFragment.SelectServerListener,
        RNetServer.StateListener, View.OnClickListener
{
    private static final String PREFS = "rnet_remote";
    private static final String LOG_TAG = "MainActivity";

    private String serverName = null;
    private InetAddress serverAddress = null;
    private int serverPort;
    
    private RNetServer rNetServer;
    private boolean needServer = true;

    private RecyclerView zoneList;
    private View connectingPlaceholder;
    private TextView connectingPlaceholderText;
    private Button connectingPlaceholderButton;
    private Snackbar serialConnectionSnackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        zoneList = (RecyclerView) findViewById(R.id.list_zones);
        zoneList.setLayoutManager(new LinearLayoutManager(this));
        ((SimpleItemAnimator) zoneList.getItemAnimator()).setSupportsChangeAnimations(false);

        connectingPlaceholder = findViewById(R.id.connecting_placeholder);
        connectingPlaceholderText = (TextView) findViewById(R.id.text_view_connecting_placeholder_notice);
        connectingPlaceholderButton = (Button) findViewById(R.id.button_connecting_placeholder_connect);
        connectingPlaceholderButton.setOnClickListener(this);

        rNetServer = new RNetServer(getBluetoothName(), this);
        zoneList.setAdapter(new ZonesAdapter(MainActivity.this, rNetServer));

        SharedPreferences settings = getSharedPreferences(PREFS, 0);

        serverName = settings.getString("server_name", "");
        if (serverName.length() > 0)
        {
            String addressName = settings.getString("server_address", "");
            serverPort = settings.getInt("server_port", 0);

            try
            {
                serverAddress = InetAddress.getByName(addressName);
            }
            catch (UnknownHostException e)
            {
                promptSelectServer(false);
            }
        }
        else
        {
            serverName = null;
            promptSelectServer(false);
        }
    }

    @Override
    protected void onStop()
    {
        super.onStop();

        needServer = false;
        if (rNetServer.isConnected())
            rNetServer.disconnect();
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        needServer = true;
        if (!rNetServer.isConnected() && serverName != null)
        {
            connectToServer(serverName, serverAddress, serverPort);
        }
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
        boolean connected = rNetServer.hasSentName();

        MenuItem allPower = menu.findItem(R.id.action_power_all);
        allPower.setVisible(connected);

        MenuItem changeController = menu.findItem(R.id.action_change_server);
        changeController.setVisible(connected);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        switch (item.getItemId())
        {
        case R.id.action_change_server:
            promptSelectServer(true);
            return true;
        case R.id.action_power_all:
            if (rNetServer.allZonesOn())
            {
                rNetServer.new SendPacketTask().execute(new PacketC2SAllPower(false));
            }
            else if (rNetServer.anyZonesOn())
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AppTheme_Dialog));
                builder.setTitle(R.string.set_all_on_off);
                builder.setNegativeButton(R.string.action_all_off,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i)
                            {
                                rNetServer.new SendPacketTask().execute(new PacketC2SAllPower(false));
                            }
                        });
                builder.setPositiveButton(R.string.action_all_on,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i)
                            {
                                rNetServer.new SendPacketTask().execute(new PacketC2SAllPower(true));
                            }
                        });

                builder.create().show();
            }
            else
            {
                rNetServer.new SendPacketTask().execute(new PacketC2SAllPower(true));
            }

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

    private void connectToServer(String name, InetAddress address, int port)
    {
        //noinspection ConstantConditions
        getSupportActionBar().setTitle(name);

        setConnectingVisible(true);

        rNetServer.setConnectionInfo(address, port);
        new Thread(rNetServer.new ServerRunnable()).start();
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
        SharedPreferences settings = getSharedPreferences(PREFS, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("server_name", name);
        editor.putString("server_address", address.getHostAddress());
        editor.putInt("server_port", port);

        editor.apply();

        if (rNetServer.isConnected())
            rNetServer.disconnect();

        setConnectingError(false);
        connectToServer(name, address, port);

        this.serverName = name;
        this.serverAddress = address;
        this.serverPort = port;
    }

    @Override
    public void connectError()
    {
        Log.i("MainActivity", "Unable to connect.");

        if (needServer)
        {
            this.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    setConnectingError(true);

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            if (needServer)
                                connectToServer(serverName, serverAddress, serverPort);
                        }
                    }, 5000);
                }
            });
        }
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

        if (!needServer)
            rNetServer.disconnect();
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
        invalidateOptionsMenu();

        runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                if (unexpected && needServer)
                {
                    setConnectingVisible(true);
                    setConnectingError(true);

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            if (!rNetServer.isRunning())
                            {
                                connectToServer(serverName, serverAddress, serverPort);
                            }
                        }
                    }, 5000);
                }
            }
        });

        if (serialConnectionSnackbar != null && serialConnectionSnackbar.isShown())
        {
            serialConnectionSnackbar.dismiss();
            serialConnectionSnackbar = null;
        }
    }

    private static String getBluetoothName()
    {
        return BluetoothAdapter.getDefaultAdapter().getName();
    }
}
