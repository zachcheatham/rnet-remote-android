package me.zachcheatham.rnetremote;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
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
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import me.zachcheatham.rnetremote.ui.ItemTouchHelperAdapter;
import me.zachcheatham.rnetremote.ui.SimpleItemTouchHelperCallback;
import me.zachcheatham.rnetremotecommon.rnet.RNetServer;
import me.zachcheatham.rnetremotecommon.rnet.Source;
import me.zachcheatham.rnetremotecommon.rnet.Zone;

class ZonesAdapter extends RecyclerView.Adapter<ZonesAdapter.ViewHolder>
        implements RNetServer.ZonesListener, ItemTouchHelperAdapter, RNetServer.SourcesListener
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
        sourcesAdapter = new ArrayAdapter<>(
                new ContextThemeWrapper(activity, R.style.AppTheme_SourceListOverlay),
                android.R.layout.simple_list_item_activated_1, new ArrayList<String>());
        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(this);
        itemTouchHelper = new ItemTouchHelper(callback);
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

    void setServer(RNetServer server)
    {
        if (this.server != null)
        {
            this.server.removeZonesListener(this);
            this.server.removeSourcesListener(this);
        }

        this.server = server;

        if (server != null)
        {
            server.addZonesListener(this);
            server.addSourcesListener(this);
            if (server.isReady())
            {
                sourcesAdapter.clear();
                for (int i = 0; i < server.getSources().size(); i++)
                {
                    int key = server.getSources().keyAt(i);
                    sourcesAdapter.add(server.getSources().get(key).getName());
                }

                handleIndex();

                activity.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        notifyDataSetChanged();
                    }
                });
            }
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView)
    {
        super.onAttachedToRecyclerView(recyclerView);
        itemTouchHelper.attachToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView)
    {
        itemTouchHelper.attachToRecyclerView(null);
        this.recyclerView = null;
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
            if (!holder.seekBar.isPressed())
                holder.seekBar.setProgress((int) Math.floor(zone.getVolume() / 2));

            holder.seekBar.setMax((int) Math.floor(zone.getMaxVolume() / 2));

            if (zone.getPowered())
            {
                holder.power.setColorFilter(ContextCompat.getColor(activity, R.color.colorAccent));
                holder.seekBar.setEnabled(true);
                holder.sourceSelect.setEnabled(true);
                holder.sourceSelect.setAlpha(1.0f);
            }
            else
            {
                holder.power
                        .setColorFilter(ContextCompat.getColor(activity, R.color.colorCardButton));
                holder.seekBar.setEnabled(false);
                holder.sourceSelect.setEnabled(false);
                holder.sourceSelect.setAlpha(0.26f);
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
    public void onItemMove(int fromPosition, int toPosition)
    {
        int[] i = zoneIndex.get(fromPosition);
        zoneIndex.remove(fromPosition);
        zoneIndex.add(toPosition, i);

        notifyItemMoved(fromPosition, toPosition);
        saveIndex();
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
                zoneIndex.removeAll(remove);
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
                            if (type == RNetServer.ZoneChangeType.VOLUME && recyclerView != null)
                            {
                                ViewHolder holder = (ViewHolder) recyclerView
                                        .findViewHolderForAdapterPosition(i);
                                if (holder != null && holder.seekBar != null &&
                                    !holder.seekBar.isPressed())
                                    holder.seekBar
                                            .setProgress((int) Math.floor(zone.getVolume() / 2));
                            }
                            else
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
    public void sourceAdded(Source source)
    {
        final int index = server.getSources().indexOfValue(source);
        sourcesAdapter.insert(source.getName(), index);
    }

    @Override
    public void sourceChanged(Source source, boolean setRemotely,
            RNetServer.SourceChangeType type)
    {
        if (type == RNetServer.SourceChangeType.NAME)
        {
            final int index = server.getSources().indexOfValue(source);
            sourcesAdapter.remove(sourcesAdapter.getItem(index));
            sourcesAdapter.insert(source.getName(), index);
        }
    }

    @Override
    public void descriptiveText(Source source, String text, int length) {}

    @Override
    public void sourceRemoved(int sourceId)
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
            View.OnClickListener, AdapterView.OnItemClickListener, View.OnLongClickListener
    {
        TextView name;
        ImageButton power;
        ImageButton sourceSelect;
        SeekBar seekBar;

        ViewHolder(View itemView)
        {
            super(itemView);

            name = itemView.findViewById(R.id.name);
            power = itemView.findViewById(R.id.power);
            seekBar = itemView.findViewById(R.id.volume);
            sourceSelect = itemView.findViewById(R.id.source_select);

            power.setOnClickListener(this);
            seekBar.setOnSeekBarChangeListener(this);

            View header = itemView.findViewById(R.id.header);
            if (header != null)
            {
                header.setOnClickListener(this);
                header.setOnLongClickListener(this);
            }

            ImageButton tuneSettings = itemView.findViewById(R.id.settings);
            tuneSettings.setOnClickListener(this);

            if (sourceSelect != null)
                sourceSelect.setOnClickListener(this);
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
            final Zone zone = server.getZone(id[0], id[1]);

            switch (view.getId())
            {
            case R.id.header:
            {
                Intent intent = new Intent(activity, ZoneActivity.class);
                activity.startActivity(intent);
                activity.overridePendingTransition(R.anim.slide_left, R.anim.fade_out);
                break;
            }
            case R.id.power:
                zone.setPower(!zone.getPowered(), false);
                break;
            case R.id.settings:
            {
                Intent intent = new Intent(activity, ZoneSettingsActivity.class);
                intent.putExtra("cid", zone.getControllerId());
                intent.putExtra("zid", zone.getZoneId());
                activity.startActivity(intent);
                activity.overridePendingTransition(R.anim.slide_left, R.anim.fade_out);
                break;
            }
            case R.id.source_select:
                new AlertDialog.Builder(
                        new ContextThemeWrapper(activity, R.style.AppTheme_DialogOverlay))
                        .setTitle(activity.getResources()
                                          .getString(R.string.dialog_select_source, zone.getName()))
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
        }

        @Override
        public boolean onLongClick(View view)
        {
            itemTouchHelper.startDrag(this);
            return false;
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
