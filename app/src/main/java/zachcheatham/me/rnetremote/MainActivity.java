package zachcheatham.me.rnetremote;

import android.bluetooth.BluetoothAdapter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.net.InetAddress;
import java.net.UnknownHostException;

import zachcheatham.me.rnetremote.rnet.RNetServer;

public class MainActivity extends AppCompatActivity implements SelectServerDialogFragment.SelectServerListener,

        RNetServer.StateListener, View.OnClickListener
{
    private static final String PREFS = "rnet_remote";

    private String serverName = null;
    private InetAddress serverAddress = null;
    private int serverPort;
    
    private RNetServer rNetServer;
    private boolean needServer = true;

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

        connectingPlaceholder = findViewById(R.id.connecting_placeholder);
        connectingPlaceholderText = (TextView) findViewById(R.id.text_view_connecting_placeholder_notice);
        connectingPlaceholderButton = (Button) findViewById(R.id.button_connecting_placeholder_connect);
        connectingPlaceholderButton.setOnClickListener(this);

        SharedPreferences settings = getSharedPreferences(PREFS, 0);

        serverName = settings.getString("server_name", "");
        if (serverName.length() > 0)
        {
            String addressName = settings.getString("server_address", "");
            serverPort = settings.getInt("server_port", 0);

            try
            {
                serverAddress = InetAddress.getByName(addressName);
                connectToServer(serverName, serverAddress, serverPort);
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
        if (rNetServer != null)
        {
            rNetServer.disconnect();
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        needServer = true;
        if (rNetServer == null && serverName != null)
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
        boolean connected = rNetServer != null && rNetServer.isConnected();

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
        if (id == R.id.action_change_server)
        {
            promptSelectServer(true);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setConnectingVisible(boolean visible)
    {
        if (visible)
        {
            connectingPlaceholder.setVisibility(View.VISIBLE);
        }
        else
        {
            connectingPlaceholder.setVisibility(View.GONE);
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
        getSupportActionBar().setTitle(name);

        setConnectingVisible(true);
        new ServerConnectionTask().execute(new ServerConnectionTaskParameters(address, port, this));
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

        if (rNetServer != null)
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

        rNetServer = null;

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
    public void disconnected(boolean unexpected)
    {
        rNetServer = null;
        invalidateOptionsMenu();

        if (unexpected && needServer)
        {
            runOnUiThread(new Runnable() {
                @Override
                public void run()
                {
                    setConnectingVisible(true);
                    setConnectingError(true);

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            connectToServer(serverName, serverAddress, serverPort);
                        }
                    }, 5000);
                }
            });
        }

        if (serialConnectionSnackbar != null && serialConnectionSnackbar.isShown())
        {
            serialConnectionSnackbar.dismiss();
            serialConnectionSnackbar = null;
        }
    }

    private class ServerConnectionTaskParameters
    {
        final InetAddress address;
        final int port;
        final RNetServer.StateListener stateListener;

        ServerConnectionTaskParameters(InetAddress address, int port, RNetServer.StateListener
                stateListener)
        {
            this.address = address;
            this.port = port;
            this.stateListener = stateListener;
        }
    }

    private class ServerConnectionTask extends AsyncTask<ServerConnectionTaskParameters, Void, Void>
    {
        @Override
        protected Void doInBackground(ServerConnectionTaskParameters... params)
        {
            rNetServer = new RNetServer(getBluetoothName(), params[0].address, params[0].port, params[0].stateListener);
            rNetServer.run();

            return null;
        }
    }

    private static String getBluetoothName()
    {
        return BluetoothAdapter.getDefaultAdapter().getName();
    }
}
