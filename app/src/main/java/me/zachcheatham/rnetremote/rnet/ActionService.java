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
import me.zachcheatham.rnetremote.rnet.packet.PacketC2SMute;
import me.zachcheatham.rnetremote.rnet.packet.RNetPacket;

public class ActionService extends IntentService implements RNetServer.StateListener
{
    public static final String EXTRA_SILENT = "silent";
    public static final String EXTRA_MUTED = "mute";
    public static final String EXTRA_MUTE_TIME = "mute_time";

    private static final String PREFS = "rnet_remote";

    private RNetServer server;
    private Handler handler;
    private RNetPacket packet;
    private boolean silent;
    private int completeMessage;

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
        switch (intent.getAction())
        {
        case "me.zachcheatham.rnetremote.action.ALL_ON":
            packet = new PacketC2SAllPower(true);
            completeMessage = R.string.toast_all_zones_on;
            break;
        case "me.zachcheatham.rnetremote.action.ALL_OFF":
            packet = new PacketC2SAllPower(false);
            completeMessage = R.string.toast_all_zones_off;
            break;
        case "me.zachcheatham.rnetremote.action.MUTE":
        {
            boolean muted = intent.getBooleanExtra(EXTRA_MUTED, false);
            short fadeTime = intent.getShortExtra(EXTRA_MUTE_TIME, (short) 0);

            packet = new PacketC2SMute(muted ? 0x01 : 0x00, fadeTime);

            if (muted)
                completeMessage = R.string.toast_system_muted;
            else
                completeMessage = R.string.toast_system_unmuted;

            break;
        }
        case "me.zachcheatham.rnetremote.action.TOGGLE_MUTE":
        {
            short fadeTime = intent.getShortExtra(EXTRA_MUTE_TIME, (short)0);
            packet = new PacketC2SMute(PacketC2SMute.MUTE_TOGGLE, fadeTime);
            completeMessage = R.string.toast_system_mute_toggled;
        }
        }

        silent = intent.getBooleanExtra(EXTRA_SILENT, false);

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
        server.sendPacket(packet);
        if (!silent)
            sendToast(completeMessage);
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
