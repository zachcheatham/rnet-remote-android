package me.zachcheatham.rnetremote.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.UnknownHostException;

import me.zachcheatham.rnetremote.R;
import me.zachcheatham.rnetremotecommon.rnet.RNetServer;
import me.zachcheatham.rnetremotecommon.rnet.packet.PacketC2SAllPower;
import me.zachcheatham.rnetremotecommon.rnet.packet.PacketC2SMute;
import me.zachcheatham.rnetremotecommon.rnet.packet.RNetPacket;

public class ActionService extends IntentService implements RNetServer.ConnectivityListener
{
    public static final String EXTRA_SILENT = "silent";
    public static final String EXTRA_MUTED = "mute";
    public static final String EXTRA_MUTE_TIME = "mute_time";
    public static final String EXTRA_FOREGROUND = "foreground";

    private static final String PREFS = "rnet_remote";
    private static final String NOTIFICATION_CHANNEL_ACTIVITY = "rnet_background_activity";
    private static final int NOTIFICATION_ID = 90;

    private RNetServer server;
    private Handler handler;
    private RNetPacket packet;
    private boolean silent;
    private boolean foreground = false;
    private int completeMessage;

    public ActionService()
    {
        super("RNetAction");
        handler = new Handler();
    }

    @Override
    protected void onHandleIntent(@Nullable final Intent intent)
    {
        assert intent != null;
//        if (intent.getBooleanExtra(EXTRA_FOREGROUND, false) &&
//            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
//        {
//            foreground = true;
//
//            NotificationManager notificationManager = (NotificationManager) getSystemService(
//                    Context.NOTIFICATION_SERVICE);
//            assert notificationManager != null;
//            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ACTIVITY,
//                    getString(R.string.notification_channel_activity),
//                    NotificationManager.IMPORTANCE_LOW);
//            channel.setDescription(getString(R.string.notification_channel_activity_desc));
//            notificationManager.createNotificationChannel(channel);
//
//            Notification notification = new NotificationCompat.Builder(this,
//                    NOTIFICATION_CHANNEL_ACTIVITY)
//                    .setSmallIcon(R.drawable.ic_app_24)
//                    .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
//                    .setContentTitle(getString(R.string.notification_sending_command))
//                    .build();
//
//            startForeground(NOTIFICATION_ID, notification);
//        }

        server = new RNetServer(RNetServer.INTENT_ACTION);

        SharedPreferences settings = getSharedPreferences(PREFS, 0);
        String addressString = settings.getString("server_address", "");
        if (!addressString.isEmpty())
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
                if (foreground)
                    stopForeground(true);
                return;
            }
        }
        else
        {
            sendToast(R.string.toast_connect_error);
            if (foreground)
                stopForeground(true);
            return;
        }

        String action = intent.getAction();
        assert action != null;
        switch (action)
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
            short fadeTime = intent.getShortExtra(EXTRA_MUTE_TIME, (short) 0);
            packet = new PacketC2SMute(PacketC2SMute.MUTE_TOGGLE, fadeTime);
            completeMessage = R.string.toast_system_mute_toggled;
        }
        }

        silent = intent.getBooleanExtra(EXTRA_SILENT, false);

        server.addConnectivityListener(this);
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
        server.removeConnectivityListener(this);
    }

    @Override
    public void disconnected(boolean unexpected)
    {
        if (foreground)
            stopForeground(true);
    }

    private void sendToast(final int messageId)
    {
        handler.post(new Runnable()
        {
            @Override
            public void run()
            {
                Toast.makeText(ActionService.this, messageId, Toast.LENGTH_LONG).show();
            }
        });
    }
}
