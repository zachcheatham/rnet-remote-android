package zachcheatham.me.rnetremote;

import android.app.Activity;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;

import zachcheatham.me.rnetremote.rnet.RNetServer;
import zachcheatham.me.rnetremote.rnet.Zone;

public class ZonesAdapter extends RecyclerView.Adapter<ZonesAdapter.ViewHolder>
        implements RNetServer.ZonesListener
{
    private static final String LOG_TAG = "ZonesAdapter";

    private final Activity activity;
    private final RNetServer server;
    private final ArrayList<int[]> zoneIndex = new ArrayList<>();

    ZonesAdapter(Activity a, RNetServer server)
    {
        this.activity = a;
        this.server = server;
        server.addZoneListener(this);
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
            holder.power.setColorFilter(ContextCompat.getColor(activity, R.color.colorAccent));
        else
            holder.power.setColorFilter(ContextCompat.getColor(activity, R.color.colorCardButton));
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

    class ViewHolder extends RecyclerView.ViewHolder implements SeekBar.OnSeekBarChangeListener,
            View.OnClickListener
    {
        TextView name;
        ImageButton power;
        View extraSettings;
        View primaryDivider;
        View secondaryDivider;
        SeekBar seekBar;


        ViewHolder(View itemView)
        {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            extraSettings = itemView.findViewById(R.id.extra_settings);
            primaryDivider = itemView.findViewById(R.id.primary_divider);
            secondaryDivider = itemView.findViewById(R.id.secondary_divider);

            seekBar = itemView.findViewById(R.id.volume);
            power = itemView.findViewById(R.id.power);

            View header = itemView.findViewById(R.id.header);
            header.setOnClickListener(this);
            power.setOnClickListener(this);
            seekBar.setOnSeekBarChangeListener(this);
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
            switch (view.getId())
            {
            case R.id.header:
                if (extraSettings.getVisibility() == View.VISIBLE)
                {
                    extraSettings.setVisibility(View.GONE);
                    secondaryDivider.setVisibility(View.GONE);
                    primaryDivider.setBackground(activity.getDrawable(R.color.colorCardDivider));
                }
                else
                {
                    extraSettings.setVisibility(View.VISIBLE);
                    secondaryDivider.setVisibility(View.VISIBLE);
                    primaryDivider.setBackground(activity.getDrawable(R.color.colorCardDividerDarker));
                }
                break;
            case R.id.power:
                int[] id = zoneIndex.get(getAdapterPosition());
                Zone zone = server.getZone(id[0], id[1]);
                zone.setPower(!zone.getPowered(), false);
                break;
            }
        }
    }
}
