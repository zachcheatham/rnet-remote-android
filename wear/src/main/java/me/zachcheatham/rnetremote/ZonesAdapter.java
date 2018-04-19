package me.zachcheatham.rnetremote;

import android.app.Activity;

import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

import me.zachcheatham.rnetremotecommon.rnet.RNetServer;
import me.zachcheatham.rnetremotecommon.rnet.Zone;

class ZonesAdapter extends RecyclerView.Adapter<ZonesAdapter.ViewHolder>
        implements RNetServer.ZonesListener
{
    @SuppressWarnings("unused")
    private static final String LOG_TAG = "ZonesAdapter";

    private final Activity activity;
    private final ArrayList<int[]> zoneIndex = new ArrayList<>();
    private RNetServer server;

    ZonesAdapter(Activity a)
    {
        this.activity = a;
    }

    void setServer(RNetServer server)
    {
        if (this.server != null)
        {
            this.server.removeZonesListener(this);
        }

        this.server = server;

        if (server != null)
        {
            server.addZonesListener(this);
            if (server.isReady())
            {
                indexReceived();
            }
        }
    }

    @NonNull
    @Override
    public ZonesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View view = LayoutInflater.from(parent.getContext())
                                  .inflate(R.layout.item_zone, parent, false);

        return new ZonesAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ZonesAdapter.ViewHolder holder, int position)
    {
        int[] zoneInfo = zoneIndex.get(position);
        Zone zone = server.getZone(zoneInfo[0], zoneInfo[1]);

        if (zone != null)
        {
            holder.name.setText(zone.getName());
            holder.volume.setText(String.format(Locale.getDefault(), "Volume: %d%%", zone.getVolume()));

            if (zone.getPowered())
            {
                holder.power.setColorFilter(ContextCompat.getColor(activity, R.color.colorAccent));
                holder.volumeUp.setEnabled(true);
                holder.volumeUp.setAlpha(1.0f);
                holder.volumeDown.setEnabled(true);
                holder.volumeDown.setAlpha(1.0f);
                holder.sourceSelect.setEnabled(true);
                holder.sourceSelect.setAlpha(1.0f);
            }
            else
            {
                holder.power
                        .setColorFilter(ContextCompat.getColor(activity, R.color.colorButton));
                holder.sourceSelect.setEnabled(false);
                holder.sourceSelect.setAlpha(0.26f);
                holder.volumeUp.setEnabled(false);
                holder.volumeUp.setAlpha(0.26f);
                holder.volumeDown.setEnabled(false);
                holder.volumeDown.setAlpha(0.26f);
            }
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
    public void cleared()
    {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public void indexReceived()
    {
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
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
                zoneIndex
                        .add(zoneIndex.size(), new int[]{zone.getControllerId(), zone.getZoneId()});
                notifyItemInserted(zoneIndex.size() - 1);
            }
        });
    }

    @Override
    public void zoneChanged(final Zone zone, boolean setRemotely,
            final RNetServer.ZoneChangeType type)
    {
        if (type != RNetServer.ZoneChangeType.PARAMETER)
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

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        TextView name;
        TextView volume;
        ImageButton power;
        ImageButton sourceSelect;
        ImageButton volumeUp;
        ImageButton volumeDown;

        ViewHolder(View itemView)
        {
            super(itemView);

            name = itemView.findViewById(R.id.name);
            power = itemView.findViewById(R.id.power);
            volume = itemView.findViewById(R.id.volume);
            volumeUp = itemView.findViewById(R.id.volume_up);
            volumeDown = itemView.findViewById(R.id.volume_down);
            sourceSelect = itemView.findViewById(R.id.source_select);

            power.setOnClickListener(this);
            volumeUp.setOnClickListener(this);
            volumeDown.setOnClickListener(this);
            sourceSelect.setOnClickListener(this);
        }

        @Override
        public void onClick(View view)
        {
            int[] id = zoneIndex.get(getAdapterPosition());
            final Zone zone = server.getZone(id[0], id[1]);

            switch (view.getId())
            {
            case R.id.power:
                zone.setPower(!zone.getPowered(), false);
                break;
            case R.id.volume_up:
                zone.volumeUp();
                break;
            case R.id.volume_down:
                zone.volumeDown();
                break;
            }
        }
    }
}
