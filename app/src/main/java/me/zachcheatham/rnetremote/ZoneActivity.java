package me.zachcheatham.rnetremote;

import android.content.*;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.nostra13.universalimageloader.core.ImageLoader;
import me.zachcheatham.rnetremote.adapter.SourcesAdapter;
import me.zachcheatham.rnetremotecommon.rnet.RNetServer;
import me.zachcheatham.rnetremotecommon.rnet.RNetServerService;
import me.zachcheatham.rnetremotecommon.rnet.Source;
import me.zachcheatham.rnetremotecommon.rnet.Zone;

public class ZoneActivity extends AppCompatActivity
        implements RNetServer.SourcesListener, RNetServer.ConnectivityListener,
        RNetServer.ZonesListener, SeekBar.OnSeekBarChangeListener, View.OnClickListener
{
    private SourcesAdapter sourcesAdapter;
    private Menu actionMenu;
    private View metadataContainerView;
    private View controlsView;
    private TextView sourceDescriptionTextView;
    private TextView titleTextView;
    private TextView artistTextView;
    private ImageView artworkImageView;
    private FloatingActionButton playPauseButton;
    private SeekBar volumeSeekBar;

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

            server.addConnectivityListener(ZoneActivity.this);
            server.addZonesListener(ZoneActivity.this);
            server.addSourcesListener(ZoneActivity.this);

            zone = server.getZone(controllerId, zoneId);
            if (zone == null)
            {
                finish();
                return;
            }

            sourcesAdapter.setServer(server);
            applyState();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName)
        {
            server.removeConnectivityListener(ZoneActivity.this);
            server.removeZonesListener(ZoneActivity.this);
            server.removeSourcesListener(ZoneActivity.this);
            sourcesAdapter.setServer(null);

            server = null;
            serverService = null;
        }
    };

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zone);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        sourcesAdapter = new SourcesAdapter(this);

        controllerId = getIntent().getIntExtra("cid", 0);
        zoneId = getIntent().getIntExtra("zid", 0);

        controlsView = findViewById(R.id.controls_container);
        metadataContainerView = findViewById(R.id.metadata_container);
        sourceDescriptionTextView = findViewById(R.id.text_source_description);
        titleTextView = findViewById(R.id.text_media_title);
        artistTextView = findViewById(R.id.text_media_artist);
        artworkImageView = findViewById(R.id.image_artwork);
        playPauseButton = findViewById(R.id.button_play_pause);
        volumeSeekBar = findViewById(R.id.seek_bar_volume);

        ImageButton prevButton = findViewById(R.id.button_prev);
        ImageButton nextButton = findViewById(R.id.button_next);

        prevButton.setOnClickListener(this);
        nextButton.setOnClickListener(this);
        playPauseButton.setOnClickListener(this);
        volumeSeekBar.setOnSeekBarChangeListener(this);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        Intent intent = new Intent(this, RNetServerService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.activity_zone, menu);

        actionMenu = menu;
        if (zone != null && zone.getPowered())
            actionMenu.findItem(R.id.action_power).getIcon()
                      .setColorFilter(getResources().getColor(R.color.colorAccent), PorterDuff.Mode.SRC_IN);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
        case R.id.action_power:
            zone.setPower(!zone.getPowered(), false);
            break;
        case R.id.action_settings:
            Intent intent = new Intent(this, ZoneSettingsActivity.class);
            intent.putExtra("cid", zone.getControllerId());
            intent.putExtra("zid", zone.getZoneId());
            startActivity(intent);
            overridePendingTransition(R.anim.slide_left, R.anim.fade_out);
            break;
        case R.id.action_select_source:
            new AlertDialog.Builder(
                    new ContextThemeWrapper(this, R.style.AppTheme_DialogOverlay))
                    .setTitle(getResources().getString(R.string.dialog_select_source, zone.getName()))
                    .setSingleChoiceItems(sourcesAdapter,
                            server.getSources().indexOfKey(zone.getSourceId()),
                            new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i)
                                {
                                    zone.setSourceId(server.getSources().keyAt(i), false);
                                    dialogInterface.dismiss();
                                }
                            })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop()
    {
        unbindService();
        super.onStop();
    }

    @Override
    public void finish()
    {
        unbindService();
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_right);
    }

    @Override
    public boolean onSupportNavigateUp()
    {
        finish();
        return true;
    }

    private void applyState()
    {
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle(zone.getName());

        if (actionMenu != null)
        {
            MenuItem selectSource = actionMenu.findItem(R.id.action_select_source);

            if (zone.getPowered())
            {
                actionMenu.findItem(R.id.action_power).getIcon()
                          .setColorFilter(getResources().getColor(R.color.colorAccent), PorterDuff.Mode.SRC_IN);
                selectSource.setEnabled(true);
                selectSource.getIcon().setAlpha(255);
            }
            else
            {
                actionMenu.findItem(R.id.action_power).getIcon()
                          .setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_IN);
                selectSource.setEnabled(false);
                selectSource.getIcon().setAlpha(66);
            }
        }

        if (zone.getPowered())
        {
            artworkImageView.setVisibility(View.VISIBLE);
            metadataContainerView.setVisibility(View.VISIBLE);
            controlsView.setVisibility(View.VISIBLE);
            applySource();
        }
        else
        {
            artworkImageView.setVisibility(View.GONE);
            metadataContainerView.setVisibility(View.GONE);
            controlsView.setVisibility(View.GONE);
        }

        volumeSeekBar.setEnabled(zone.getPowered());
        volumeSeekBar.setProgress((int) Math.floor(zone.getVolume() / 2));
        volumeSeekBar.setMax((int) Math.floor(zone.getMaxVolume() / 2));
    }

    private void applySource()
    {
        if (server.getSource(zone.getSourceId()) != null)
        {
            Source source = server.getSource(zone.getSourceId());
            String descriptiveText = source.getPermanentDescriptiveText();
            if (descriptiveText != null && descriptiveText.length() > 0)
                sourceDescriptionTextView.setText(descriptiveText);
            else
                sourceDescriptionTextView.setText(source.getName());

            if (source.getMediaPlayState())
                playPauseButton.setImageResource(R.drawable.ic_pause_black_36dp);
            else
                playPauseButton.setImageResource(R.drawable.ic_play_arrow_black_36dp);

            String mediaTitle = source.getMediaTitle();

            if (mediaTitle != null && mediaTitle.length() > 0)
            {
                String mediaArtist = source.getMediaArtist();
                String mediaArtwork = source.getMediaArtworkUrl();

                titleTextView.setVisibility(View.VISIBLE);
                titleTextView.setText(mediaTitle);
                if (mediaArtist != null && mediaArtist.length() > 0)
                {
                    artistTextView.setVisibility(View.VISIBLE);
                    artistTextView.setText(mediaArtist);
                }
                else
                {
                    artistTextView.setVisibility(View.GONE);
                }

                if (mediaArtwork != null && mediaArtwork.length() > 0)
                {
                    artworkImageView.setPadding(0,0,0,0);
                    artworkImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    artworkImageView.clearColorFilter();
                    ImageLoader.getInstance().displayImage(mediaArtwork, artworkImageView);
                    metadataContainerView
                            .setBackgroundColor(getResources().getColor(R.color.colorMetadataBackground));
                }
                else
                {
                    artworkImageView.setImageResource(source.getTypeDrawable());
                    artworkImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    artworkImageView.getDrawable().setColorFilter(getResources().getColor(R.color.colorPrimary),
                            PorterDuff.Mode.SRC_IN);
                    int padding = (int) getResources().getDimension(R.dimen.activity_horizontal_margin);
                    artworkImageView.setPadding(padding, padding, padding, padding);
                    metadataContainerView.setBackgroundColor(0);
                }
            }
            else
            {
                artistTextView.setVisibility(View.GONE);
                titleTextView.setVisibility(View.GONE);
                artworkImageView.setImageResource(source.getTypeDrawable());
                artworkImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                artworkImageView.setColorFilter(getResources().getColor(R.color.colorPrimary),
                        PorterDuff.Mode.SRC_IN);
                int padding = (int) getResources().getDimension(R.dimen.activity_horizontal_margin) * 2;
                artworkImageView.setPadding(padding, padding, padding, padding);
                metadataContainerView.setBackgroundColor(0);
            }
        }
    }

    private void unbindService()
    {
        if (server != null)
        {
            server.removeConnectivityListener(this);
            server.removeZonesListener(this);
            server.removeSourcesListener(this);
            server = null;

            sourcesAdapter.setServer(null);
        }

        if (serverService != null)
        {
            unbindService(serviceConnection);
            serverService = null;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
    {
        if (fromUser && zone != null)
            zone.setVolume(progress * 2, false);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}

    @Override
    public void connectionInitiated() {}

    @Override
    public void connectError() {}

    @Override
    public void ready() {}

    @Override
    public void disconnected(boolean unexpected)
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                finish();
            }
        });
    }

    @Override
    public void indexReceived()
    {
    }

    @Override
    public void zoneAdded(Zone zone)
    {
    }

    @Override
    public void zoneChanged(Zone z, boolean setRemotely, final RNetServer.ZoneChangeType type)
    {
        if (z == this.zone)
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    switch (type)
                    {
                    case POWER:
                        applyState();
                        break;
                    case VOLUME:
                        if (!volumeSeekBar.isPressed())
                            volumeSeekBar.setProgress((int) Math.floor(zone.getVolume() / 2));
                        break;
                    case MAX_VOLUME:
                        volumeSeekBar.setProgress((int) Math.floor(zone.getVolume() / 2));
                        volumeSeekBar.setMax((int) Math.floor(zone.getMaxVolume() / 2));
                        break;
                    case SOURCE:
                        applySource();
                        break;
                    }
                }
            });
        }
    }

    @Override
    public void zoneRemoved(int controllerId, int zoneId)
    {
        if (controllerId == this.controllerId && zoneId == this.zoneId)
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    finish();
                }
            });
        }
    }

    @Override
    public void cleared()
    {
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                finish();
            }
        });
    }

    @Override
    public void sourceAdded(Source source)
    {
    }

    @Override
    public void sourceChanged(final Source source, boolean setRemotely, RNetServer.SourceChangeType type)
    {
        if (zone.getPowered() && source.getId() == zone.getSourceId())
        {
            if (type == RNetServer.SourceChangeType.METADATA)
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        applySource();
                    }
                });
            }
            else if (type == RNetServer.SourceChangeType.PLAYSTATE)
            {
                final boolean playing = source.getMediaPlayState();
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (playing)
                            playPauseButton.setImageResource(R.drawable.ic_pause_black_36dp);
                        else
                            playPauseButton.setImageResource(R.drawable.ic_play_arrow_black_36dp);
                    }
                });
            }
        }
    }

    @Override
    public void descriptiveText(Source source, String text, int length) {}

    @Override
    public void sourceRemoved(int sourceId) {}

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
        case R.id.button_play_pause:
        {
            Source source = server.getSource(zone.getSourceId());
            if (source != null)
            {
                if (source.getMediaPlayState())
                    source.control(Source.CONTROL_PAUSE);
                else
                    source.control(Source.CONTROL_PLAY);
            }
            break;
        }
        case R.id.button_prev:
        {
            Source source = server.getSource(zone.getSourceId());
            if (source != null)
                source.control(Source.CONTROL_PREV);
            break;
        }
        case R.id.button_next:
        {
            Source source = server.getSource(zone.getSourceId());
            if (source != null)
                source.control(Source.CONTROL_NEXT);
            break;
        }
        }
    }
}
