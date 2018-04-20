package me.zachcheatham.rnetremote;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
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

import java.util.Locale;

import me.zachcheatham.rnetremote.ui.SimpleDividerItemDecoration;
import me.zachcheatham.rnetremotecommon.rnet.RNetServer;
import me.zachcheatham.rnetremotecommon.rnet.RNetServerService;
import me.zachcheatham.rnetremotecommon.rnet.Source;
import me.zachcheatham.rnetremotecommon.rnet.Zone;

public class ManageSourcesActivity extends AppCompatActivity implements
        RNetServer.ConnectivityListener, AddSourceDialogFragment.AddSourceListener, RNetServer.SourcesListener
{
    private final SourcesAdapter sourcesAdapter = new SourcesAdapter();
    private RNetServer server;
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

            server.addConnectivityListener(ManageSourcesActivity.this);
            server.addSourcesListener(ManageSourcesActivity.this);

            if (server.isReady())
                sourcesAdapter.notifyDataSetChanged();
            else
                finish();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName)
        {
            server.removeConnectivityListener(ManageSourcesActivity.this);
            server.removeSourcesListener(ManageSourcesActivity.this);

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

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        RecyclerView sourceList = findViewById(R.id.sources);
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
            server.removeConnectivityListener(this);
            server.removeSourcesListener(this);
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
    public void sourceAdded(Source source)
    {
        // TODO
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                sourcesAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void sourceChanged(Source source, boolean setRemotely,
            RNetServer.SourceChangeType metadata)
    {
        // TODO
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                sourcesAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void descriptiveText(Source source, String text, int length) {}

    @Override
    public void sourceRemoved(int sourceId)
    {
        // TODO
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                sourcesAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void cleared() {}

    @Override
    public void addSource(String sourceName, int sourceId, int sourceType)
    {
        server.createSource(sourceId, sourceName, sourceType);
    }

    @Override
    public boolean sourceExists(int sourceId)
    {
        return server.getSources().get(sourceId) != null;
    }

    private class SourcesAdapter extends RecyclerView.Adapter<SourceViewHolder>
    {
        @NonNull
        @Override
        public SourceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            View view = LayoutInflater.from(parent.getContext())
                                      .inflate(R.layout.item_manage_source, parent, false);
            return new SourceViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SourceViewHolder holder, int position)
        {
            int sourceId = server.getSources().keyAt(position);
            Source source = server.getSources().get(sourceId);

            holder.sourceId.setText(String.format(Locale.getDefault(), "%02d", sourceId + 1));
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
