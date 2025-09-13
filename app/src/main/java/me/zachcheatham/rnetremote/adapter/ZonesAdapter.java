package me.zachcheatham.rnetremote.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

import android.util.Log;
import android.util.SparseArray;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.*;
import com.nostra13.universalimageloader.core.ImageLoader;
import me.zachcheatham.rnetremote.R;
import me.zachcheatham.rnetremote.ZoneActivity;
import me.zachcheatham.rnetremote.ZoneSettingsActivity;
import me.zachcheatham.rnetremote.ui.BackgroundImageViewAware;
import me.zachcheatham.rnetremotecommon.rnet.RNetServer;
import me.zachcheatham.rnetremotecommon.rnet.Source;
import me.zachcheatham.rnetremotecommon.rnet.Zone;

import java.util.ArrayList;
import java.util.List;

public class ZonesAdapter extends RecyclerView.Adapter<ZonesAdapter.ViewHolder> implements RNetServer.ZonesListener, RNetServer.SourcesListener
{
    @SuppressWarnings("unused")
    private static final String LOG_TAG = "ZonesAdapter";
    private static final String PREFS = "rnet_remote_zone_order";

    private final Activity activity;
    private final ArrayList<int[]> zoneIndex = new ArrayList<>();
    private final ItemTouchHelper itemTouchHelper;
    private RNetServer server;
    private final SourcesAdapter sourcesAdapter;
    private RecyclerView recyclerView;
    private boolean showArtwork = true;
    private boolean gridLayout = false;

    public ZonesAdapter(Activity a)
    {
        this.activity = a;
        sourcesAdapter = new SourcesAdapter(a);
        itemTouchHelper = new ItemTouchHelper(new ItemTouchHelperCallback());
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

    public void setServer(RNetServer server)
    {
        if (this.server != null)
        {
            this.server.removeZonesListener(this);
        }

        this.server = server;

        if (server != null)
        {
            server.addZonesListener(this);
            server.addSourcesListener(this);
            if (server.isReady())
            {
                handleIndex();
                activity.runOnUiThread(this::notifyDataSetChanged);
            }
        }

        sourcesAdapter.setServer(server);
    }

    public void setGridLayout(boolean gridLayout) {
        this.gridLayout = gridLayout;
    }

    public void setShowArtwork(boolean show)
    {
        showArtwork = show;
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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        int[] zoneInfo = zoneIndex.get(position);
        Zone zone = server.getZone(zoneInfo[0], zoneInfo[1]);

        if (zone != null)
        {

            holder.name.setText(zone.getName());
            if (!holder.seekBar.isPressed())
                holder.seekBar.setProgress((int) Math.floor(zone.getVolume() / 2d));

            holder.seekBar.setMax((int) Math.floor(zone.getMaxVolume() / 2d));

            if (zone.getPowered())
            {
                holder.power.setColorFilter(ContextCompat.getColor(activity, R.color.colorAccent));
                holder.seekBar.setEnabled(true);
                holder.sourceSelect.setEnabled(true);
                holder.sourceSelect.setAlpha(1.0f);
                holder.mute.setEnabled(true);
                holder.mute.setAlpha(1.0f);
                if (zone.getMute())
                {
                    holder.mute.setImageResource(R.drawable.ic_volume_off_white_24dp);
                    holder.mute.setColorFilter(ContextCompat.getColor(activity, R.color.colorMute));
                }
                else
                {
                    holder.mute.setImageResource(R.drawable.ic_volume_up_white_24dp);
                    holder.mute.setColorFilter(ContextCompat.getColor(activity, R.color.colorCardButton));
                }

                Source source = server.getSource(zone.getSourceId());
                if (source != null)
                {
                    String artworkUrl = showArtwork ? source.getMediaArtworkUrl() : null;
                    if (artworkUrl != null && !artworkUrl.isEmpty())
                    {
                        ImageLoader.getInstance()
                                   .displayImage(artworkUrl, new BackgroundImageViewAware(holder.innerLayout));
                    }
                    else
                    {
                        holder.innerLayout.setBackground(null);
                    }
                }
                else
                {
                    holder.innerLayout.setBackground(null);
                }
            }
            else
            {
                holder.power
                        .setColorFilter(ContextCompat.getColor(activity, R.color.colorCardButton));
                holder.seekBar.setEnabled(false);
                holder.sourceSelect.setEnabled(false);
                holder.mute.setImageResource(R.drawable.ic_volume_up_white_24dp);
                holder.mute.setEnabled(false);
                holder.mute.setAlpha(0.26f);
                holder.mute.setColorFilter(ContextCompat.getColor(activity, R.color.colorCardButton));
                holder.sourceSelect.setAlpha(0.26f);
                holder.innerLayout.setBackground(null);
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

    private int getZoneIndexPosition(int ctrllrID, int zoneID)
    {
        for (int i = 0; i < zoneIndex.size(); i++)
        {
            if (zoneIndex.get(i)[0] == ctrllrID &&
                zoneIndex.get(i)[1] == zoneID)
            {
                return i;
            }
        }

        return -1;
    }

    @Override
    public void sourceAdded(Source source) {}

    @Override
    public void sourceChanged(Source source, boolean setRemotely, RNetServer.SourceChangeType type) {
        // Notify artwork changed
        if (type == RNetServer.SourceChangeType.METADATA && showArtwork) {
            ArrayList<Integer> itemsChanged = new ArrayList<>();
            for (int i = 0; i < zoneIndex.size(); i++) {
                int[] zoneId = zoneIndex.get(i);
                Zone zone = server.getZone(zoneId[0], zoneId[1]);
                if (zone.getSourceId() == source.getId()) {
                    itemsChanged.add(i);
                }
            }

            if (!itemsChanged.isEmpty()) {
                activity.runOnUiThread(() -> {
                    for (int i : itemsChanged) {
                        notifyItemChanged(i);
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
    public void cleared()
    {
        activity.runOnUiThread(this::notifyDataSetChanged);
    }

    @Override
    public void indexReceived()
    {
        activity.runOnUiThread(() -> {
            handleIndex();
            notifyDataSetChanged();
        });
    }

    @Override
    public void zoneAdded(final Zone zone)
    {
        activity.runOnUiThread(() -> {
            zoneIndex
                    .add(zoneIndex.size(), new int[]{zone.getControllerId(), zone.getZoneId()});
            notifyItemInserted(zoneIndex.size() - 1);
        });
    }

    @Override
    public void zoneChanged(final Zone zone, boolean setRemotely,
                            final RNetServer.ZoneChangeType type)
    {
        if (type != RNetServer.ZoneChangeType.PARAMETER)
        {
            activity.runOnUiThread(() -> {
                int i = getZoneIndexPosition(zone.getControllerId(), zone.getZoneId());
                if (type == RNetServer.ZoneChangeType.VOLUME && recyclerView != null)
                {
                    ViewHolder holder = (ViewHolder) recyclerView
                            .findViewHolderForAdapterPosition(i);
                    if (holder != null && holder.seekBar != null &&
                        !holder.seekBar.isPressed())
                        holder.seekBar
                                .setProgress((int) (double) (zone.getVolume() / 2));
                }
                else
                    notifyItemChanged(i);
            });
        }
    }

    @Override
    public void zoneRemoved(final int controllerId, final int zoneId)
    {
        activity.runOnUiThread(() -> {
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
        });
    }

    class ViewHolder extends RecyclerView.ViewHolder implements SeekBar.OnSeekBarChangeListener,
            View.OnClickListener, View.OnLongClickListener {
        View innerLayout;
        TextView name;
        ImageButton power;
        ImageButton sourceSelect;
        ImageButton mute;
        SeekBar seekBar;

        ViewHolder(View itemView)
        {
            super(itemView);

            innerLayout = itemView.findViewById(R.id.inner_layout);
            name = itemView.findViewById(R.id.name);
            power = itemView.findViewById(R.id.power);
            mute = itemView.findViewById(R.id.button_mute);
            seekBar = itemView.findViewById(R.id.volume);
            sourceSelect = itemView.findViewById(R.id.source_select);

            power.setOnClickListener(this);
            sourceSelect.setOnClickListener(this);
            mute.setOnClickListener(this);
            seekBar.setOnSeekBarChangeListener(this);

            View header = itemView.findViewById(R.id.header);
            if (header != null)
            {
                header.setOnClickListener(this);
                header.setOnLongClickListener(this);
            }
            else
            {
                name.setOnClickListener(this);
                name.setOnLongClickListener(this);
            }

            ImageButton tuneSettings = itemView.findViewById(R.id.settings);
            tuneSettings.setOnClickListener(this);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean userSet)
        {
            if (userSet)
            {
                int[] id = zoneIndex.get(getBindingAdapterPosition());
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
            case R.id.name:
            {
                Intent intent = new Intent(activity, ZoneActivity.class);
                intent.putExtra("cid", zone.getControllerId());
                intent.putExtra("zid", zone.getZoneId());
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
                                (dialogInterface, i) -> {
                                    zone.setSourceId(server.getSources().keyAt(i), false);
                                    dialogInterface.dismiss();
                                })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                break;
            case R.id.button_mute:
                zone.setMute(!zone.getMute(), false);
                break;
            }
        }

        @Override
        public boolean onLongClick(View v) {
            itemTouchHelper.startDrag(this);
            return true;
        }
    }

    private class ItemTouchHelperCallback extends ItemTouchHelper.Callback {
        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            if (gridLayout) {
                return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0);
            }
            else {
                return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
            }
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {

            int fromPosition = viewHolder.getAbsoluteAdapterPosition();
            int toPosition = target.getAbsoluteAdapterPosition();

            int[] i = zoneIndex.get(fromPosition);
            zoneIndex.remove(fromPosition);
            zoneIndex.add(toPosition, i);

            notifyItemMoved(fromPosition, toPosition);
            saveIndex();

            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }
    }
}
