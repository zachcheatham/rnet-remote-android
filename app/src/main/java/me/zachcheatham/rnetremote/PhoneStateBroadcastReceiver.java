package me.zachcheatham.rnetremote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;

import me.zachcheatham.rnetremote.service.ActionService;

public class PhoneStateBroadcastReceiver extends BroadcastReceiver
{
    private static final String PREFS = "rnet_remote";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String action = intent.getAction();
        if (action != null && action.equals("android.intent.action.PHONE_STATE"))
        {
            Intent serviceIntent = null;

            SharedPreferences settings = context.getSharedPreferences(PREFS, 0);

            int networkId = settings.getInt("server_network", -1);
            boolean muteOnRing = settings.getBoolean("mute_on_ring", false);
            boolean muteOnCall = settings.getBoolean("mute_on_call", false);
            //int muteTime = settings.getInt("mute_fade_time", 0);

            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (state.equals(TelephonyManager.EXTRA_STATE_RINGING))
            {
                if (muteOnRing && onNetwork(context, networkId))
                {
                    serviceIntent = new Intent(context, ActionService.class);
                    serviceIntent.setAction("me.zachcheatham.rnetremote.action.MUTE");
                    serviceIntent.putExtra(ActionService.EXTRA_MUTED, true);
                }
            }
            else if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK))
            {
                if (muteOnCall)
                {
                    if (onNetwork(context, networkId))
                    {
                        serviceIntent = new Intent(context, ActionService.class);
                        serviceIntent.setAction("me.zachcheatham.rnetremote.action.MUTE");
                        serviceIntent.putExtra(ActionService.EXTRA_MUTED, true);
                        serviceIntent.putExtra(ActionService.EXTRA_MUTE_TIME, (short) 1000);
                    }
                }
                else if (muteOnRing && onNetwork(context, networkId))
                {
                    serviceIntent = new Intent(context, ActionService.class);
                    serviceIntent.setAction("me.zachcheatham.rnetremote.action.MUTE");
                    serviceIntent.putExtra(ActionService.EXTRA_MUTED, false);
                    serviceIntent.putExtra(ActionService.EXTRA_MUTE_TIME, (short) 1000);
                }
            }
            else if ((muteOnCall || muteOnRing) && onNetwork(context, networkId))
            {
                serviceIntent = new Intent(context, ActionService.class);
                serviceIntent.setAction("me.zachcheatham.rnetremote.action.MUTE");
                serviceIntent.putExtra(ActionService.EXTRA_MUTED, false);
                serviceIntent.putExtra(ActionService.EXTRA_MUTE_TIME, (short) 1000);
            }

            if (serviceIntent != null)
            {
                //serviceIntent.putExtra(ActionService.EXTRA_MUTE_TIME, muteTime);
                serviceIntent.putExtra(ActionService.EXTRA_SILENT, true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                {
                    serviceIntent.putExtra(ActionService.EXTRA_FOREGROUND, true);
                    context.startForegroundService(serviceIntent);
                }
                else
                    context.startService(serviceIntent);
            }
        }
    }

    private boolean onNetwork(Context context, int targetNetwork)
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null)
        {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

            if (networkInfo.isConnected() && (
                    networkInfo.getType() == ConnectivityManager.TYPE_WIFI ||
                    networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET))
            {
                if (networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET ||
                    targetNetwork == -1)
                {
                    return true;
                }
                else
                {
                    WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                                                                   .getSystemService(
                                                                           Context.WIFI_SERVICE);
                    if (wifiManager != null)
                    {
                        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                        return (wifiInfo != null && targetNetwork == wifiInfo.getNetworkId());
                    }
                }
            }
        }

        return true;
    }
}
