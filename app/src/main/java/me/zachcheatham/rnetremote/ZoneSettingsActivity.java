package me.zachcheatham.rnetremote;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import me.zachcheatham.rnetremote.rnet.RNetServer;
import me.zachcheatham.rnetremote.rnet.RNetServerService;
import me.zachcheatham.rnetremote.rnet.Zone;

public class ZoneSettingsActivity extends AppCompatActivity implements View.OnClickListener,
        SeekBar.OnSeekBarChangeListener, CompoundButton.OnCheckedChangeListener,
        PopupMenu.OnMenuItemClickListener, RNetServer.ZonesListener, RNetServer.StateListener

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

            server.addStateListener(ZoneSettingsActivity.this);
            server.addZoneListener(ZoneSettingsActivity.this);

            zone = server.getZone(controllerId, zoneId);
            if (zone == null)
            {
                finish();
                return;
            }

            applyZoneParameters();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName)
        {
            server.removeStateListener(ZoneSettingsActivity.this);
            server.removeZoneListener(ZoneSettingsActivity.this);

            serverService = null;
            server = null;
        }
    };

    private TextView zoneNameText;
    private SeekBar balanceSlider;
    private TextView balanceText;
    private SeekBar bassSlider;
    private TextView bassText;
    private SeekBar trebleSlider;
    private TextView trebleText;
    private Switch loudnessSwitch;
    private TextView partyModeText;
    private Switch doNotDisturbSwitch;
    private TextView turnOnVolumeText;
    private SeekBar turnOnVolumeSlider;
    private TextView maxVolumeText;
    private SeekBar maxVolumeSlider;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zone_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        controllerId = getIntent().getIntExtra("cid", 0);
        zoneId = getIntent().getIntExtra("zid", 0);

        TextView zoneIdText = (TextView) findViewById(R.id.text_zone_id);
        View zoneNameItem = findViewById(R.id.item_name);
        this.zoneNameText = (TextView) findViewById(R.id.text_name);
        this.balanceSlider = (SeekBar) findViewById(R.id.seek_bar_balance);
        this.balanceText = (TextView) findViewById(R.id.text_balance);
        this.bassSlider = (SeekBar) findViewById(R.id.seek_bar_bass);
        this.bassText = (TextView) findViewById(R.id.text_bass);
        this.trebleSlider = (SeekBar) findViewById(R.id.seek_bar_treble);
        this.trebleText = (TextView) findViewById(R.id.text_treble);
        this.loudnessSwitch = (Switch) findViewById(R.id.switch_loudness);
        View partyModeItem = findViewById(R.id.item_party_mode);
        this.partyModeText = (TextView) findViewById(R.id.text_party_mode);
        this.doNotDisturbSwitch = (Switch) findViewById(R.id.switch_do_not_disturb);
        View deleteItem = findViewById(R.id.item_delete_zone);
        this.turnOnVolumeText = (TextView) findViewById(R.id.text_turn_on_volume);
        this.turnOnVolumeSlider = (SeekBar) findViewById(R.id.seek_bar_turn_on_volume);
        this.maxVolumeText = (TextView) findViewById(R.id.text_max_volume);
        this.maxVolumeSlider = (SeekBar) findViewById(R.id.seek_bar_max_volume);

        zoneIdText.setText(getResources().getString(R.string.format_zone_id, controllerId + 1, zoneId + 1));
        zoneNameItem.setOnClickListener(this);
        this.balanceSlider.setOnSeekBarChangeListener(this);
        this.bassSlider.setOnSeekBarChangeListener(this);
        this.trebleSlider.setOnSeekBarChangeListener(this);
        this.loudnessSwitch.setOnCheckedChangeListener(this);
        this.doNotDisturbSwitch.setOnCheckedChangeListener(this);
        this.turnOnVolumeSlider.setOnSeekBarChangeListener(this);
        this.maxVolumeSlider.setOnSeekBarChangeListener(this);
        partyModeItem.setOnClickListener(this);
        deleteItem.setOnClickListener(this);
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
        if (server != null)
        {
            server.removeStateListener(this);
            server.removeZoneListener(this);
        }
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

    private void applyZoneParameters()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                //noinspection ConstantConditions
                ZoneSettingsActivity.this.getSupportActionBar().setTitle(zone.getName());

                zoneNameText.setText(zone.getName());
                if (!balanceSlider.isPressed())
                    balanceSlider.setProgress(((Integer) zone.getParameter(Zone.PARAMETER_BALANCE)) + 10);
                if (!bassSlider.isPressed())
                    bassSlider.setProgress(((Integer) zone.getParameter(Zone.PARAMETER_BASS)) + 10);
                if (!trebleSlider.isPressed())
                    trebleSlider.setProgress(((Integer) zone.getParameter(Zone.PARAMETER_TREBLE)) + 10);
                if (!turnOnVolumeSlider.isPressed())
                    turnOnVolumeSlider.setProgress(
                            (int) Math.floor(((Integer) zone.getParameter(Zone.PARAMETER_TURN_ON_VOLUME)) / 2));
                if (!maxVolumeSlider.isPressed())
                    maxVolumeSlider.setProgress(
                            (int) Math.floor((zone.getMaxVolume()) / 2));
                loudnessSwitch.setChecked((Boolean) zone.getParameter(Zone.PARAMETER_LOUDNESS));
                doNotDisturbSwitch.setChecked((Boolean) zone.getParameter(Zone.PARAMETER_DO_NOT_DISTURB));

                switch ((int) zone.getParameter(Zone.PARAMETER_PARTY_MODE))
                {
                case Zone.PARAMETER_PARTY_MODE_OFF:
                    partyModeText.setText(R.string.option_value_off);
                    break;
                case Zone.PARAMETER_PARTY_MODE_ON:
                    partyModeText.setText(R.string.option_value_on);
                    break;
                case Zone.PARAMETER_PARTY_MODE_MASTER:
                    partyModeText.setText(R.string.option_value_master);
                    break;
                }
            }
        });
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
        case R.id.item_name:
            @SuppressLint("InflateParams") View textContainer = LayoutInflater.from(this).inflate(R.layout.dialog_text, null, false);
            final EditText input = textContainer.findViewById(R.id.dialog_text);
            input.setText(zone.getName());
            input.setSelection(zone.getName().length());
            AlertDialog dialog = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AppTheme_DialogOverlay))
                    .setTitle(R.string.label_zone_name)
                    .setView(textContainer)
                    .setPositiveButton(R.string.action_rename, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i)
                        {
                            zone.setName(input.getText().toString(), false);

                            //noinspection ConstantConditions
                            getSupportActionBar().setTitle(zone.getName());
                            zoneNameText.setText(zone.getName());
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            dialog.show();

            input.requestFocus();
            break;
        case R.id.item_party_mode:
            PopupMenu menu = new PopupMenu(new ContextThemeWrapper(this, R.style.AppTheme_PopupOverlay), view);
            menu.setOnMenuItemClickListener(this);
            menu.inflate(R.menu.party_mode_popup);
            menu.show();
            break;
        case R.id.item_delete_zone:
            new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AppTheme_DialogOverlay))
                    .setMessage(R.string.confirm_delete_zone)
                    .setNegativeButton(R.string.action_delete, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i)
                        {
                            server.deleteZone(controllerId, zoneId, false);
                            finish();
                        }
                    })
                    .setPositiveButton(android.R.string.cancel, null)
                    .show();
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onProgressChanged(SeekBar seekBar, int value, boolean user)
    {
        switch (seekBar.getId())
        {
        case R.id.seek_bar_balance:
            balanceText.setText((value > 10 ? "+" : "") + (value - 10));
            if (user)
                zone.setParameter(Zone.PARAMETER_BALANCE, value - 10, false);
            break;
        case R.id.seek_bar_bass:
            bassText.setText((value > 10 ? "+" : "") + (value - 10));
            if (user)
                zone.setParameter(Zone.PARAMETER_BASS, value - 10, false);
            break;
        case R.id.seek_bar_treble:
            trebleText.setText((value > 10 ? "+" : "") + (value - 10));
            if (user)
                zone.setParameter(Zone.PARAMETER_TREBLE, value - 10, false);
            break;
        case R.id.seek_bar_turn_on_volume:
            turnOnVolumeText.setText((value * 2) + "");
            if (user)
                zone.setParameter(Zone.PARAMETER_TURN_ON_VOLUME, value * 2, false);
            break;
        case R.id.seek_bar_max_volume:
            maxVolumeText.setText((value * 2) + "");
            if (user)
                zone.setMaxVolume(value * 2, false);
            break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b)
    {
        if (zone != null)
        {
            switch (compoundButton.getId())
            {
            case R.id.switch_loudness:
                zone.setParameter(Zone.PARAMETER_LOUDNESS, b, false);
                break;
            case R.id.switch_do_not_disturb:
                zone.setParameter(Zone.PARAMETER_DO_NOT_DISTURB, b, false);
                break;
            }
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item)
    {
        switch (item.getItemId())
        {
        case R.id.party_mode_off:
            partyModeText.setText(R.string.option_value_off);
            zone.setParameter(Zone.PARAMETER_PARTY_MODE, Zone.PARAMETER_PARTY_MODE_OFF, false);
            break;
        case R.id.party_mode_on:
            partyModeText.setText(R.string.option_value_on);
            zone.setParameter(Zone.PARAMETER_PARTY_MODE, Zone.PARAMETER_PARTY_MODE_ON, false);
            break;
        case R.id.party_mode_master:
            partyModeText.setText(R.string.option_value_master);
            zone.setParameter(Zone.PARAMETER_PARTY_MODE, Zone.PARAMETER_PARTY_MODE_MASTER, false);
            break;
        }

        return true;
    }

    @Override
    public void connectionInitiated() {}

    @Override
    public void connectError() {}

    @Override
    public void ready() {

    }

    @Override
    public void serialStateChanged(boolean connected) {}

    @Override
    public void disconnected(boolean unexpected)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                finish();
            }
        });
    }

    @Override
    public void cleared()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                finish();
            }
        });
    }

    @Override
    public void zoneAdded(Zone zone) {}

    @Override
    public void zoneChanged(Zone zone, boolean setRemotely, RNetServer.ZoneChangeType type)
    {
        if (setRemotely && zone == this.zone && (type == RNetServer.ZoneChangeType.PARAMETER || type == RNetServer.ZoneChangeType.NAME || type == RNetServer.ZoneChangeType.MAX_VOLUME))
            applyZoneParameters();
    }

    @Override
    public void zoneRemoved(int controllerId, int zoneId)
    {
        if (controllerId == this.controllerId && zoneId == this.zoneId)
        {
            runOnUiThread(new Runnable() {
                @Override
                public void run()
                {
                    finish();
                }
            });
        }
    }

    @Override
    public void indexReceived()
    {

    }

    @Override
    public void sourcesChanged() {}
}
