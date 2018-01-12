package me.zachcheatham.rnetremote;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import me.zachcheatham.rnetremote.rnet.RNetServer;
import me.zachcheatham.rnetremote.rnet.RNetServerService;
import me.zachcheatham.rnetremote.rnet.Source;
import me.zachcheatham.rnetremote.rnet.Zone;
import me.zachcheatham.rnetremote.ui.SimpleDividerItemDecoration;

public class ManageSourcesActivity extends AppCompatActivity implements RNetServer.StateListener,
        RNetServer.ZonesListener, AddSourceDialogFragment.AddSourceListener
{
    private RNetServer server;
    private final SourcesAdapter sourcesAdapter = new SourcesAdapter();
    private boolean ignoreNextUpdate = false; // HACK I hate this

    private RNetServerService serverService;
    private ServiceConnection serviceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder)
        {
            RNetServerService.LocalBinder binder = (RNetServerService.LocalBinder) iBinder;

            serverService = binder.getService();
            server = serverService.getServer();

            server.addStateListener(ManageSourcesActivity.this);
            server.addZoneListener(ManageSourcesActivity.this);

            if (server.isReady())
                sourcesAdapter.notifyDataSetChanged();
            else
                finish();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName)
        {
            server.removeStateListener(ManageSourcesActivity.this);
            server.removeZoneListener(ManageSourcesActivity.this);

            serverService = null;
            server = null;
        }
    };

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_zones);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        RecyclerView sourceList = (RecyclerView) findViewById(R.id.sources);
        sourceList.setLayoutManager(new LinearLayoutManager(this));
        sourceList.addItemDecoration(new SimpleDividerItemDecoration(this));
        sourceList.setAdapter(sourcesAdapter);
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
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.activity_manage_sources, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
        case R.id.action_add:
            AddSourceDialogFragment dialog = new AddSourceDialogFragment();
            dialog.show(getSupportFragmentManager(), "AddSourceDialogFragment");
            return true;

        }

        return super.onOptionsItemSelected(item);
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
    public void ready() {}

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
    public void indexReceived() {}

    @Override
    public void sourcesChanged()
    {
        if (!ignoreNextUpdate)
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    sourcesAdapter.notifyDataSetChanged();
                }
            });
        }
        else
        {
            ignoreNextUpdate = false;
        }
    }

    @Override
    public void zoneAdded(Zone zone) {}

    @Override
    public void zoneChanged(Zone zone, boolean setRemotely, RNetServer.ZoneChangeType type) {}

    @Override
    public void zoneRemoved(int controllerId, int zoneId) {}

    @Override
    public void cleared() {}

    @Override
    public void addSource(String sourceName, int sourceId)
    {
        server.createSource(sourceId, sourceName);
    }

    @Override
    public boolean sourceExists(int sourceId)
    {
        return server.getSources().get(sourceId) != null;
    }

    private class SourcesAdapter extends RecyclerView.Adapter<SourceViewHolder>
    {
        @Override
        public SourceViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View view = LayoutInflater.from(parent.getContext())
                                      .inflate(R.layout.item_manage_source, parent, false);
            return new SourceViewHolder(view);
        }

        @Override
        public void onBindViewHolder(SourceViewHolder holder, int position)
        {
            int sourceId = server.getSources().keyAt(position);
            Source source = server.getSources().get(sourceId);

            holder.sourceId.setText(String.format("%02d", sourceId + 1));
            holder.name.setText(source.getName());
        }

        @Override
        public int getItemCount()
        {
            if (server == null)
                return 0;
            else
                return server.getSources().size();
        }
    }

    private class SourceViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener
    {
        TextView sourceId;
        TextView name;
        ImageButton deleteButton;

        SourceViewHolder(View itemView)
        {
            super(itemView);

            sourceId = itemView.findViewById(R.id.text_id);
            name = itemView.findViewById(R.id.text_name);
            deleteButton = itemView.findViewById(R.id.button_delete);
            deleteButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v)
        {
            ignoreNextUpdate = true;
            server.deleteSource(server.getSources().keyAt(getAdapterPosition()));
            sourcesAdapter.notifyItemRemoved(getAdapterPosition());
        }
    }
}