package zachcheatham.me.rnetremote;

import android.app.Activity;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;

import android.widget.SeekBar;
import android.widget.TextView;

import net.cachapa.expandablelayout.ExpandableLayout;

import java.util.ArrayList;

import zachcheatham.me.rnetremote.rnet.RNetServer;
import zachcheatham.me.rnetremote.rnet.Source;
import zachcheatham.me.rnetremote.rnet.Zone;

public class ZonesAdapter extends RecyclerView.Adapter<ZonesAdapter.ViewHolder>
        implements RNetServer.ZonesListener
{
    private static final String LOG_TAG = "ZonesAdapter";

    private final Activity activity;
    private final RNetServer server;
    private final ArrayList<int[]> zoneIndex = new ArrayList<>();
    private ArrayAdapter<String> sourcesAdapter;
    
    ZonesAdapter(Activity a, RNetServer server)
    {
        this.activity = a;
        this.server = server;
        server.addZoneListener(this);

        sourcesAdapter = new ArrayAdapter<String>(activity, android.R.layout.simple_list_item_activated_1, new String[0]);
    }

    @Override
    public ZonesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext())
                                  .inflate(R.layout.item_zone, parent, false);
        
        return new ZonesAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ZonesAdapter.ViewHolder holder, int position) {
        int[] zoneInfo = zoneIndex.get(position);
        Zone zone = server.getZone(zoneInfo[0], zoneInfo[1]);

        holder.name.setText(zone.getName());
        if (!holder.seekBar.isPressed())
            holder.seekBar.setProgress((int) Math.floor(zone.getVolume() / 2));

        if (zone.getPowered())
            holder.power.setColorFilter(ContextCompat.getColor(activity, R.color.colorAccent));
        else
            holder.power.setColorFilter(ContextCompat.getColor(activity, R.color.colorCardButton));

        holder.sources.setAdapter(sourcesAdapter);

    }

    @Override
    public int getItemCount()
    {
        return zoneIndex.size();
    }

    @Override
    public void dataReset()
    {
        zoneIndex.clear();

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
        sourcesAdapter.clear();
        for (int i = 0; i < server.getSources().size(); i++)
        {
            int key = server.getSources().keyAt(i);
            sourcesAdapter.add(server.getSources().get(key).getName());
        }
        sourcesAdapter.notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements SeekBar.OnSeekBarChangeListener,
            View.OnClickListener
    {
        TextView name;
        ImageButton power;
        ExpandableLayout extraSettings;
        ListView sources;
        View primaryDivider;
        SeekBar seekBar;

        ViewHolder(View itemView)
        {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            extraSettings = itemView.findViewById(R.id.sources_container);
            primaryDivider = itemView.findViewById(R.id.primary_divider);

            seekBar = itemView.findViewById(R.id.volume);
            power = itemView.findViewById(R.id.power);

            sources = itemView.findViewById(R.id.sources);

            View header = itemView.findViewById(R.id.header);
            header.setOnClickListener(this);
            power.setOnClickListener(this);
            seekBar.setOnSeekBarChangeListener(this);
            sources.setAdapter(sourcesAdapter);
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
                    if (extraSettings.isExpanded())
                    {
                        extraSettings.collapse();
                        primaryDivider.setBackground(activity.getDrawable(R.color.colorCardDivider));
                    }
                    else
                    {
                        extraSettings.expand();
                        primaryDivider.setBackground(activity.getDrawable(R.color.colorCardDividerDarker));
                    }
                    return;
                case R.id.power:
                    zone.setPower(!zone.getPowered(), false);
                    return;
            }
        }
    }
}
