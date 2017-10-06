package me.zachcheatham.rnetremote.rnet;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.UnknownHostException;

import me.zachcheatham.rnetremote.R;
import me.zachcheatham.rnetremote.rnet.packet.PacketC2SAllPower;

public class ActionService extends IntentService implements RNetServer.StateListener
{
    private static final String PREFS = "rnet_remote";

    private RNetServer server;
    private Handler handler;
    private String action;

    public ActionService()
    {
        super("RNetAction");
        handler = new Handler();
    }

    @Override
    protected void onHandleIntent(@Nullable final Intent intent)
    {
        server = new RNetServer(RNetServer.INTENT_ACTION);

        SharedPreferences settings = getSharedPreferences(PREFS, 0);
        String addressString = settings.getString("server_address", "");
        if (addressString.length() > 0)
        {
            int port = settings.getInt("server_port", 0);
            try
            {
                InetAddress address = InetAddress.getByName(addressString);
                server.setConnectionInfo(address, port);
            }
            catch (UnknownHostException e)
            {
                sendToast(R.string.toast_connect_error);
                return;
            }
        }
        else
        {
            sendToast(R.string.toast_connect_error);
            return;
        }

        assert intent != null;
        action = intent.getAction();

        server.addStateListener(this);
        server.run();
    }

    @Override
    public void connectionInitiated() {}

    @Override
    public void connectError()
    {
        sendToast(R.string.toast_connect_error);
    }

    @Override
    public void ready()
    {
        switch (action)
        {
        case "me.zachcheatham.rnetremote.action.ALL_ON":
            server.sendPacket(new PacketC2SAllPower(true));
            sendToast(R.string.toast_all_zones_on);
            break;
        case "me.zachcheatham.rnetremote.action.ALL_OFF":
            server.sendPacket(new PacketC2SAllPower(false));
            sendToast(R.string.toast_all_zones_off);
            break;
        }

        server.disconnect();
        server.removeStateListener(this);
    }

    @Override
    public void serialStateChanged(boolean connected) {}

    @Override
    public void disconnected(boolean unexpected) {}

    private void sendToast(final int messageId)
    {
        handler.post(new Runnable() {
            @Override
            public void run()
            {
                Toast.makeText(ActionService.this, messageId, Toast.LENGTH_LONG).show();
            }
        });
    }
}
