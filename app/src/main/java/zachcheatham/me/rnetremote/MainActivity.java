package zachcheatham.me.rnetremote;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
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

import zachcheatham.me.rnetremote.server.RNetServer;

public class MainActivity extends AppCompatActivity implements SelectServerDialogFragment.SelectServerListener,
        RNetServer.Listener
{
    private static final String PREFS = "rnet_remote";

    private String serverName;
    private InetAddress serverAddress;
    private int serverPort;
    
    private RNetServer rNetServer;

    private View connectingPlaceholder;
    private TextView connectingPlaceholderText;
    private Button connectingPlaceholderButton;

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

        SharedPreferences settings = getSharedPreferences(PREFS, 0);

        String serverName = settings.getString("server_name", "");
        if (serverName.length() > 0)
        {
            String addressName = settings.getString("server_address", "");
            int port = settings.getInt("server_port", 0);

            try
            {
                InetAddress address = InetAddress.getByName(addressName);
                connectToServer(serverName, address, port);
            }
            catch (UnknownHostException e)
            {
                promptSelectServer(false);
            }
        }
        else
        {
            promptSelectServer(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_remote, menu);
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

        connectingPlaceholderButton.setVisibility(View.GONE);
        connectingPlaceholderText.setVisibility(View.GONE);
    }

    private void setConnectingError()
    {
        connectingPlaceholderButton.setVisibility(View.VISIBLE);
        connectingPlaceholderText.setVisibility(View.VISIBLE);
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

        connectToServer(name, address, port);

        this.serverName = name;
        this.serverAddress = address;
        this.serverPort = port;
    }

    @Override
    public void connectError()
    {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                setConnectingError();
            }
        });
    }

    @Override
    public void connected()
    {
        setConnectingVisible(false);
    }

    @Override
    public void packetReceived()
    {
        Log.d("MainActivity", "Packet received.");
    }

    @Override
    public void disconnected(boolean unexpected)
    {
        Log.i("MainActivity", String.format("Disconnect. %s", unexpected ? "Unexpected!" : ""));
        rNetServer = null;
    }

    private class ServerConnectionTaskParameters
    {
        final InetAddress address;
        final int port;
        final RNetServer.Listener listener;

        ServerConnectionTaskParameters(InetAddress address, int port, RNetServer.Listener listener)
        {
            this.address = address;
            this.port = port;
            this.listener = listener;
        }
    }

    private class ServerConnectionTask extends AsyncTask<ServerConnectionTaskParameters, Void, Void>
    {
        @Override
        protected Void doInBackground(ServerConnectionTaskParameters... params)
        {
            rNetServer = new RNetServer(params[0].address, params[0].port, params[0].listener);
            rNetServer.run();

            return null;
        }
    }
}
