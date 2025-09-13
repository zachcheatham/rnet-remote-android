package me.zachcheatham.rnetremotecommon.rnet;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;

import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import me.zachcheatham.rnetremotecommon.rnet.packet.PacketC2SAllPower;
import me.zachcheatham.rnetremotecommon.rnet.packet.PacketC2SMute;
import me.zachcheatham.rnetremotecommon.rnet.packet.RNetPacket;

public class RNetServerWorker extends ListenableWorker {

    public static final String KEY_MUTED = "mute";
    public static final String KEY_MUTE_TIME = "mute_time";
    public static final String KEY_ACTION = "action";
    public static final String KEY_TARGET_NETWORK = "target_network_id";
    public static final String KEY_HOST = "server_host";
    public static final String KEY_PORT = "server_port";

    public static final String ACTION_ALL_ON = "all_on";
    public static final String ACTION_ALL_OFF = "all_off";
    public static final String ACTION_MUTE = "mute";

    private final Executor backgroundExecutor = Executors.newSingleThreadExecutor();

    public RNetServerWorker(@NotNull Context context, @NotNull WorkerParameters workerParams) {
        super(context, workerParams);
    }


    @NonNull
    @Override
    public ListenableFuture<ListenableWorker.Result> startWork() {

        return CallbackToFutureAdapter.getFuture(completer -> {

            backgroundExecutor.execute(() -> {

                Data inputData = getInputData();

                int targetNetworkId = inputData.getInt(KEY_TARGET_NETWORK, -1);

                if (!onNetwork(getApplicationContext(), targetNetworkId)) {
                    completer.set(Result.success());
                }

                String action = inputData.getString(KEY_ACTION);
                String serverHost = inputData.getString(KEY_HOST);
                int serverPort = inputData.getInt(KEY_PORT, 0);

                try {
                    assert action != null;
                    assert serverHost != null;

                    RNetPacket packet = null;
                    RNetServer server = new RNetServer(RNetServer.INTENT_ACTION);
                    InetAddress serverAddress = InetAddress.getByName(serverHost);
                    server.setConnectionInfo(serverAddress, serverPort);

                    switch (action) {
                        case ACTION_ALL_ON:
                            packet = new PacketC2SAllPower(true);
                            break;
                        case ACTION_ALL_OFF:
                            packet = new PacketC2SAllPower(false);
                            break;
                        case ACTION_MUTE:
                            boolean muted = inputData.getBoolean(KEY_MUTED, false);
                            int fadeTime = inputData.getInt(KEY_MUTE_TIME, 0);
                            packet = new PacketC2SMute(muted ? 0x01 : 0x00, (short) fadeTime);
                            break;
                    }

                    RNetPacket finalPacket = packet;
                    server.addConnectivityListener(new RNetServer.ConnectivityListener() {
                        @Override
                        public void connectionInitiated() {
                        }

                        @Override
                        public void connectError() {
                            completer.set(Result.failure());
                            server.removeConnectivityListener(this);
                        }

                        @Override
                        public void ready() {
                            server.sendPacket(finalPacket);
                            server.disconnect();
                            server.removeConnectivityListener(this);
                            completer.set(Result.success());
                        }

                        @Override
                        public void disconnected(boolean unexpected) {
                        }
                    });

                    server.run();
                } catch (Exception e) {
                    completer.setException(e);
                }
            });

            return "RNetServer work.";
        });
    }

    private boolean onNetwork(Context context, int targetNetworkId) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

            if (networkInfo != null &&
                    networkInfo.isConnected() && (
                            networkInfo.getType() == ConnectivityManager.TYPE_WIFI ||
                            networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET
                    )) {

                if (networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET ||
                    targetNetworkId == -1) {
                    return true;
                }
                else {
                    WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                            .getSystemService(Context.WIFI_SERVICE);

                    if (wifiManager != null) {
                        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                        return (wifiInfo != null && targetNetworkId == wifiInfo.getNetworkId());
                    }

                }
            }
        }

        return false;
    }
}
