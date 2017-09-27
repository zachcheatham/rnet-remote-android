package me.zachcheatham.rnetremote;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
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

import me.zachcheatham.rnetremote.rnet.RNetServer;
import me.zachcheatham.rnetremote.rnet.Zone;

class ZonesAdapter extends RecyclerView.Adapter<ZonesAdapter.ViewHolder>
        implements RNetServer.ZonesListener
{
    private static final String LOG_TAG = "ZonesAdapter";

    private final Activity activity;
    private final ArrayList<int[]> zoneIndex = new ArrayList<>();
    private RNetServer server;
    private ArrayAdapter<String> sourcesAdapter;
    private RecyclerView recyclerView;
    
    ZonesAdapter(Activity a)
    {
        this.activity = a;
        sourcesAdapter = new ArrayAdapter<>(new ContextThemeWrapper(activity, R.style.AppTheme_SourceListOverlay), android.R.layout.simple_list_item_activated_1, new ArrayList<String>());
    }

    void setServer(RNetServer server)
    {
        if (this.server != null)
            this.server.removeZoneListener(this);

        if (server != null)
        {
            server.addZoneListener(this);

            zoneIndex.clear();
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

        this.server = server;
    }

    private void compareZoneIndex()
    {
        ArrayList<int[]> zoneIndex = new ArrayList<>();
        SparseArray<SparseArray<Zone>> zones = server.getZones();
        //for (int i
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView)
    {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView)
    {
        this.recyclerView = null;

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

        holder.name.setText(zone.getName());
        if (!holder.seekBar.isPressed())
            holder.seekBar.setProgress((int) Math.floor(zone.getVolume() / 2));

        if (zone.getPowered())
        {
            holder.power.setColorFilter(ContextCompat.getColor(activity, R.color.colorAccent));
            holder.seekBar.setEnabled(true);
        }
        else
        {
            holder.power.setColorFilter(ContextCompat.getColor(activity, R.color.colorCardButton));
            holder.seekBar.setEnabled(false);
            if (holder.sourcesContainer.isExpanded())
            {
                holder.sourcesContainer.collapse();
                holder.primaryDivider.setBackground(activity.getDrawable(R.color.colorCardDivider));
            }
        }

        if (holder.sources.getAdapter() == null)
            holder.sources.setAdapter(sourcesAdapter);

        holder.sources.setItemChecked(server.getSources().indexOfKey(zone.getSourceId()), true);
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
    public void dataReset()
    {
        zoneIndex.clear();
        sourcesAdapter.clear();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run()
            {
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
                int index = zoneIndex.size();

                for (int i = 0; i < zoneIndex.size(); i++)
                {
                    int[] zoneInfo = zoneIndex.get(i);
                    if (zoneInfo[0] > zone.getControllerId() ||
                        (zoneInfo[0] == zone.getControllerId() && zoneInfo[1] > zone.getZoneId()))
                    {
                        index = i;
                        break;
                    }
                }

                zoneIndex.add(index, new int[]{zone.getControllerId(), zone.getZoneId()});
                notifyItemInserted(index);
            }
        });
    }

    @Override
    public void zoneChanged(final Zone zone, boolean setRemotely, RNetServer.ZoneChangeType type)
    {
        if (setRemotely || type != RNetServer.ZoneChangeType.VOLUME)
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
            }
        }

        @Override
        public boolean onLongClick(View view)
        {
            int[] id = zoneIndex.get(getAdapterPosition());
            Zone zone = server.getZone(id[0], id[1]);

            Intent intent = new Intent(activity, ZoneSettingsActivity.class);
            intent.putExtra("cid", zone.getControllerId());
            intent.putExtra("zid", zone.getZoneId());
            activity.startActivity(intent);
            activity.overridePendingTransition(R.anim.slide_left, R.anim.fade_out);

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
}
