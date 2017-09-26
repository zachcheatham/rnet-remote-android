package me.zachcheatham.rnetremote;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import me.zachcheatham.rnetremote.rnet.RNetServer;
import me.zachcheatham.rnetremote.rnet.RNetServerService;
import me.zachcheatham.rnetremote.rnet.Zone;

public class ZoneSettingsActivity extends AppCompatActivity implements RNetServer.StateListener
{
    @SuppressWarnings("unused")
    private static final String LOG_TAG = "ZoneSettingsActivity";

    private int controllerId;
    private int zoneId;

    private RNetServer server;
    private Zone zone;
    private RNetServerService serverService;
    private ServiceConnection serviceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder)
        {
            RNetServerService.LocalBinder binder = (RNetServerService.LocalBinder) iBinder;
            serverService = binder.getService();
            server = serverService.getServer();

            zone = server.getZone(controllerId, zoneId);

            //noinspection ConstantConditions
            ZoneSettingsActivity.this.getSupportActionBar().setTitle(zone.getName());

            server.addStateListener(ZoneSettingsActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName)
        {
            server.removeStateListener(ZoneSettingsActivity.this);

            serverService = null;
            server = null;
        }
    };

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        controllerId = getIntent().getIntExtra("cid", 0);
        zoneId = getIntent().getIntExtra("zid", 0);
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
    }

    @Override
    public void finish()
    {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_right);
    }

    @Override
    public boolean onSupportNavigateUp()
    {
        finish();
        return true;
    }

    @Override
    public void connectionInitiated() {}

    @Override
    public void connectError() {}

    @Override
    public void connected() {}

    @Override
    public void serialStateChanged(boolean connected) {}

    @Override
    public void disconnected(boolean unexpected)
    {
        finish();
    }
}
