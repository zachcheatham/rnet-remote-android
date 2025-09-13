package me.zachcheatham.rnetremote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import me.zachcheatham.rnetremotecommon.rnet.RNetServerWorker;

public class PhoneStateBroadcastReceiver extends BroadcastReceiver
{
    private static final String PREFS = "rnet_remote";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String action = intent.getAction();

        if (action == null || !action.equals("android.intent.action.PHONE_STATE")) return;

        SharedPreferences settings = context.getSharedPreferences(PREFS, 0);
        boolean muteOnRing = settings.getBoolean("mute_in_ring", false);
        boolean muteOnCall = settings.getBoolean("mute_on_call", false);
        //int muteTime = settings.getInt("mute_fade_time", 0);
        int networkId = settings.getInt("server_network", -1);
        String serverHost = settings.getString("server_address", "");
        int serverPort = settings.getInt("server_port", 0);

        String phoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        Data.Builder workerDataBuilder = null;

        if (phoneState.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
            if (muteOnRing) {
                workerDataBuilder = new Data.Builder();
                workerDataBuilder.putBoolean(RNetServerWorker.KEY_MUTED, true);
            }
        }
        else if (phoneState.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
            if (muteOnCall) {
                workerDataBuilder = new Data.Builder();
                workerDataBuilder.putBoolean(RNetServerWorker.KEY_MUTED, true);
            }
            else if (muteOnRing) {
                workerDataBuilder = new Data.Builder();
                workerDataBuilder.putBoolean(RNetServerWorker.KEY_MUTED, false);
            }
        }
        else if (phoneState.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
            if (muteOnCall || muteOnRing) {
                workerDataBuilder = new Data.Builder();
                workerDataBuilder.putBoolean(RNetServerWorker.KEY_MUTED, false);
            }
        }

        if (workerDataBuilder != null) {
            workerDataBuilder.putString(RNetServerWorker.KEY_ACTION, RNetServerWorker.ACTION_MUTE);
            workerDataBuilder.putInt(RNetServerWorker.KEY_TARGET_NETWORK, networkId);
            workerDataBuilder.putInt(RNetServerWorker.KEY_MUTE_TIME, 1000);
            workerDataBuilder.putString(RNetServerWorker.KEY_HOST, serverHost);
            workerDataBuilder.putInt(RNetServerWorker.KEY_PORT, serverPort);

            OneTimeWorkRequest muteWorkRequest = new OneTimeWorkRequest.Builder(RNetServerWorker.class)
                    .setInputData(workerDataBuilder.build())
                    .build();

            WorkManager.getInstance(context).enqueue(muteWorkRequest);
        }
    }
}
