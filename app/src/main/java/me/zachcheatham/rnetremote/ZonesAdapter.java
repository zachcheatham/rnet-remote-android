package me.zachcheatham.rnetremote;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.SparseArray;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import net.cachapa.expandablelayout.ExpandableLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.zachcheatham.rnetremote.rnet.RNetServer;
import me.zachcheatham.rnetremote.rnet.Zone;
import me.zachcheatham.rnetremote.ui.ItemTouchHelperAdapter;
import me.zachcheatham.rnetremote.ui.SimpleItemTouchHelperCallback;

class ZonesAdapter extends RecyclerView.Adapter<ZonesAdapter.ViewHolder>
        implements RNetServer.ZonesListener, ItemTouchHelperAdapter
{
    @SuppressWarnings("unused")
    private static final String LOG_TAG = "ZonesAdapter";
    private static final String PREFS = "rnet_remote_zone_order";

    private final Activity activity;
    private final ArrayList<int[]> zoneIndex = new ArrayList<>();
    private ItemTouchHelper itemTouchHelper;
    private RNetServer server;
    private ArrayAdapter<String> sourcesAdapter;
    private RecyclerView recyclerView;

    
    ZonesAdapter(Activity a)
    {
        this.activity = a;
        sourcesAdapter = new ArrayAdapter<>(new ContextThemeWrapper(activity, R.style.AppTheme_SourceListOverlay), android.R.layout.simple_list_item_activated_1, new ArrayList<String>());

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(this);
        itemTouchHelper = new ItemTouchHelper(callback);
    }

    void setServer(RNetServer server)
    {
        if (this.server != null)
            this.server.removeZoneListener(this);

        this.server = server;

        if (server != null)
        {
            server.addZoneListener(this);
            if (server.isReady())
                handleIndex();

            sourcesAdapter.clear();
            for (int i = 0; i < server.getSources().size(); i++)
            {
                int key = server.getSources().keyAt(i);
                sourcesAdapter.add(server.getSources().get(key).getName());
            }

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run()
                {
                    notifyDataSetChanged();
                }
            });
        }
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView)
    {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView)
    {
        this.recyclerView = null;
        itemTouchHelper.attachToRecyclerView(null);
    }

    @Override
    public ZonesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext())
                                  .inflate(R.layout.item_zone, parent, false);
        
        return new ZonesAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ZonesAdapter.ViewHolder holder, int position)
    {
        int[] zoneInfo = zoneIndex.get(position);
        Zone zone = server.getZone(zoneInfo[0], zoneInfo[1]);

        if (zone != null)
        {
            holder.name.setText(zone.getName());
            if (!holder.seekBar.isPressed())
                holder.seekBar.setProgress((int) Math.floor(zone.getVolume() / 2));

            if (zone.getPowered())
            {
                holder.power.setColorFilter(ContextCompat.getColor(activity, R.color.colorAccent));
                holder.seekBar.setEnabled(true);
            } else
            {
                holder.power
                        .setColorFilter(ContextCompat.getColor(activity, R.color.colorCardButton));
                holder.seekBar.setEnabled(false);
                if (holder.sourcesContainer.isExpanded())
                {
                    holder.sourcesContainer.collapse();
                    holder.primaryDivider
                            .setBackground(activity.getDrawable(R.color.colorCardDivider));
                }
            }

            if (holder.sources.getAdapter() == null)
                holder.sources.setAdapter(sourcesAdapter);

            holder.sources.setItemChecked(server.getSources().indexOfKey(zone.getSourceId()), true);
        }
    }

    @Override
    public int getItemCount()
    {
        if (server == null)
            return 0;
        else
            return zoneIndex.size();
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition)
    {
        Collections.swap(zoneIndex, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        saveIndex();
        return true;
    }

    private void handleIndex()
    {
        zoneIndex.clear();

        SharedPreferences settings = activity.getSharedPreferences(PREFS, 0);
        int size = settings.getInt("zones", 0);

        if (size > 0)
        {
            for (int i = 0; i < size; i++)
            {
                int controllerId = settings.getInt("index_" + i + "_controller", -1);
                int zoneId = settings.getInt("index_" + i + "_zone", -1);

                zoneIndex.add(i, new int[]{controllerId, zoneId});
            }

            // Remove removed zones
            {
                List<int[]> remove = new ArrayList<>();
                for (int[] zoneInfo : zoneIndex)
                {
                    if (server.getZone(zoneInfo[0], zoneInfo[1]) == null)
                        remove.add(zoneInfo);
                }
                for (int[] zoneInfo : remove)
                    zoneIndex.remove(zoneInfo);
                remove.clear();
            }

            // Add new zones
            SparseArray<SparseArray<Zone>> zones = server.getZones();
            for (int i = 0; i < zones.size(); i++)
            {
                int ctrllrId = zones.keyAt(i);
                for (int c = 0; c < zones.get(ctrllrId).size(); c++)
                {
                    int zoneId = zones.get(ctrllrId).keyAt(c);
                    if (!zoneIndexContains(zoneIndex, ctrllrId, zoneId))
                        zoneIndex.add(new int[]{ctrllrId, zoneId});
                }
            }
        }
        else
        {
            SparseArray<SparseArray<Zone>> zones = server.getZones();
            for (int i = 0; i < zones.size(); i++)
            {
                int ctrllrId = zones.keyAt(i);
                for (int c = 0; c < zones.get(ctrllrId).size(); c++)
                {
                    int zoneId = zones.get(ctrllrId).keyAt(c);
                    zoneIndex.add(new int[]{ctrllrId, zoneId});
                }
            }
        }

        saveIndex();
    }

    private void saveIndex()
    {
        SharedPreferences settings = activity.getSharedPreferences(PREFS, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        editor.putInt("zones", zoneIndex.size());
        for (int i = 0; i < zoneIndex.size(); i++)
        {
            int[] zoneInfo = zoneIndex.get(i);
            editor.putInt("index_" + i + "_controller", zoneInfo[0]);
            editor.putInt("index_" + i + "_zone", zoneInfo[1]);
        }
        editor.apply();
    }

    @Override
    public void cleared()
    {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                zoneIndex.clear();
                sourcesAdapter.clear();
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public void indexReceived()
    {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
                handleIndex();
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public void zoneAdded(final Zone zone)
    {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                zoneIndex.add(zoneIndex.size(), new int[]{zone.getControllerId(), zone.getZoneId()});
                notifyItemInserted(zoneIndex.size() - 1);
            }
        });
    }

    @Override
    public void zoneChanged(final Zone zone, boolean setRemotely, RNetServer.ZoneChangeType type)
    {
        if (type != RNetServer.ZoneChangeType.PARAMETER && (setRemotely || type != RNetServer.ZoneChangeType.VOLUME))
        {
            activity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    for (int i = 0; i < zoneIndex.size(); i++)
                    {
                        if (zoneIndex.get(i)[0] == zone.getControllerId() &&
                            zoneIndex.get(i)[1] == zone.getZoneId())
                        {
                            notifyItemChanged(i);
                            break;
                        }
                    }
                }
            });
        }
    }

    @Override
    public void zoneRemoved(final int controllerId, final int zoneId)
    {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                for (int i = 0; i < zoneIndex.size(); i++)
                {
                    if (zoneIndex.get(i)[0] == controllerId &&
                        zoneIndex.get(i)[1] == zoneId)
                    {
                        zoneIndex.remove(i);

                        notifyItemRemoved(i);
                        break;
                    }
                }
            }
        });
    }

    @Override
    public void sourcesChanged()
    {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                sourcesAdapter.clear();
                for (int i = 0; i < server.getSources().size(); i++)
                {
                    int key = server.getSources().keyAt(i);
                    sourcesAdapter.add(server.getSources().get(key).getName());
                }
            }
        });
    }

    class ViewHolder extends RecyclerView.ViewHolder implements SeekBar.OnSeekBarChangeListener,
            View.OnClickListener, ExpandableLayout.OnExpansionUpdateListener,
            AdapterView.OnItemClickListener, View.OnLongClickListener
    {
        TextView name;
        ImageButton power;
        ExpandableLayout sourcesContainer;
        ListView sources;
        View primaryDivider;
        SeekBar seekBar;

        ViewHolder(View itemView)
        {
            super(itemView);

            name = itemView.findViewById(R.id.name);
            power = itemView.findViewById(R.id.power);
            primaryDivider = itemView.findViewById(R.id.primary_divider);
            sourcesContainer = itemView.findViewById(R.id.sources_container);
            seekBar = itemView.findViewById(R.id.volume);
            sources = itemView.findViewById(R.id.sources);

            power.setOnClickListener(this);
            seekBar.setOnSeekBarChangeListener(this);
            sources.setAdapter(sourcesAdapter);
            sources.setOnItemClickListener(this);
            sourcesContainer.setOnExpansionUpdateListener(this);

            View header = itemView.findViewById(R.id.header);
            header.setOnClickListener(this);
            header.setOnLongClickListener(this);

            ImageButton tuneSettings = itemView.findViewById(R.id.settings);
            tuneSettings.setOnClickListener(this);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean userSet)
        {
            if (userSet)
            {
                int[] id = zoneIndex.get(getAdapterPosition());
                server.getZone(id[0], id[1]).setVolume(progress * 2, false);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}

        @Override
        public void onClick(View view)
        {
            int[] id = zoneIndex.get(getAdapterPosition());
            Zone zone = server.getZone(id[0], id[1]);

            switch (view.getId())
            {
                case R.id.header:
                    if (sourcesContainer.isExpanded())
                    {
                        sourcesContainer.collapse();
                        primaryDivider.setBackground(activity.getDrawable(R.color.colorCardDivider));
                    }
                    else if (zone.getPowered())
                    {
                        sourcesContainer.expand();
                        primaryDivider.setBackground(activity.getDrawable(R.color.colorCardDividerDarker));
                    }
                    break;
                case R.id.power:
                    zone.setPower(!zone.getPowered(), false);
                    break;
                case R.id.settings:
                    Intent intent = new Intent(activity, ZoneSettingsActivity.class);
                    intent.putExtra("cid", zone.getControllerId());
                    intent.putExtra("zid", zone.getZoneId());
                    activity.startActivity(intent);
                    activity.overridePendingTransition(R.anim.slide_left, R.anim.fade_out);
                    break;
            }
        }

        @Override
        public boolean onLongClick(View view)
        {
            itemTouchHelper.startDrag(this);
            return false;
        }

        @Override
        public void onExpansionUpdate(float expansionFraction, int state)
        {
            recyclerView.scrollToPosition(getAdapterPosition());
        }

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
        {
            int[] id = zoneIndex.get(getAdapterPosition());
            Zone zone = server.getZone(id[0], id[1]);

            int sourceId = server.getSources().keyAt(i);
            zone.setSourceId(sourceId, false);
        }
    }

    private static boolean zoneIndexContains(List<int[]> index, int controllerId, int zoneId)
    {
        for (int[] info : index)
        {
            if (info[0] == controllerId && info[1] == zoneId)
                return true;
        }
        return false;
    }
}
