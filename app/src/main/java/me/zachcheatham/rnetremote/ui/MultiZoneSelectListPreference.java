package me.zachcheatham.rnetremote.ui;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.SparseArray;
import me.zachcheatham.rnetremotecommon.rnet.Zone;

import java.util.*;

public class MultiZoneSelectListPreference extends DialogPreference
{
    private CharSequence[] zoneNames = new CharSequence[0];
    private List<ZoneId> zoneIds = new ArrayList<>();
    private Set<ZoneId> selectedZones = new HashSet<>();
    private Set<ZoneId> newSelectedZones = new HashSet<>();
    private boolean mPreferenceChanged;

    public MultiZoneSelectListPreference(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public void setZones(SparseArray<SparseArray<Zone>> zones)
    {
        List<String> zoneNamesList = new ArrayList<>();
        for (int i = 0; i < zones.size(); i++)
        {
            int ctrllrId = zones.keyAt(i);
            for (int c = 0; c < zones.get(ctrllrId).size(); c++)
            {
                int zoneId = zones.get(ctrllrId).keyAt(c);
                Zone zone = zones.get(ctrllrId).get(zoneId);
                zoneNamesList.add(zone.getName());
                zoneIds.add(new ZoneId(ctrllrId, zoneId));
            }
        }

        zoneNames = zoneNamesList.toArray(new CharSequence[zoneNamesList.size()]);
    }

    public void setSelected(int[][] values)
    {
        selectedZones.clear();
        for (int[] zoneId : values)
            selectedZones.add(new ZoneId(zoneId));
    }

    private boolean[] getSelectedItems()
    {
        boolean[] result = new boolean[zoneNames.length];
        for (int i = 0; i < zoneNames.length; i++)
            result[i] = selectedZones.contains(zoneIds.get(i));
        return result;
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder)
    {
        super.onPrepareDialogBuilder(builder);

        builder.setMultiChoiceItems(zoneNames, getSelectedItems(),
            new DialogInterface.OnMultiChoiceClickListener()
            {
                public void onClick(DialogInterface dialog, int which, boolean isChecked)
                {
                    if (isChecked)
                    {
                        mPreferenceChanged |= newSelectedZones.add(zoneIds.get(which));
                    }
                    else
                    {
                        mPreferenceChanged |= newSelectedZones.remove(zoneIds.get(which));
                    }
                }
            });
        newSelectedZones.clear();
        newSelectedZones.addAll(selectedZones);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult)
    {
        super.onDialogClosed(positiveResult);

        if (positiveResult && mPreferenceChanged)
        {
            final Set<ZoneId> values = newSelectedZones;
            int[][] selectedZones = new int[newSelectedZones.size()][2];
            int i = 0;
            for (ZoneId zoneId : values)
            {
                selectedZones[i][0] = zoneId.ctrllrId;
                selectedZones[i++][1] = zoneId.zoneId;
            }
            if (callChangeListener(selectedZones))
            {
                setSelected(selectedZones);
            }
        }
        mPreferenceChanged = false;
    }

    public static class ZoneId
    {
        final int ctrllrId;
        final int zoneId;

        ZoneId(int[] zoneId)
        {
            this(zoneId[0], zoneId[1]);
        }

        ZoneId(int ctrllrId, int zoneId)
        {
            this.ctrllrId = ctrllrId;
            this.zoneId = zoneId;
        }

        @Override
        public boolean equals(Object anObject)
        {
            if (this == anObject)
                return true;

            if (anObject instanceof ZoneId)
            {

                ZoneId anotherZoneId = (ZoneId) anObject;
                return (anotherZoneId.ctrllrId == ctrllrId && anotherZoneId.zoneId == zoneId);
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            return ctrllrId * 31 + zoneId;
        }
    }
}
