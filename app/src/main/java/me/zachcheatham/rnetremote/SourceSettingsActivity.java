package me.zachcheatham.rnetremote;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.*;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import me.zachcheatham.rnetremote.ui.MultiZoneSelectListPreference;
import me.zachcheatham.rnetremotecommon.rnet.RNetServer;
import me.zachcheatham.rnetremotecommon.rnet.RNetServerService;
import me.zachcheatham.rnetremotecommon.rnet.Source;

import java.util.*;

public class SourceSettingsActivity extends AppCompatActivity

{
    private ServiceConnection serviceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {}

        @Override
        public void onServiceDisconnected(ComponentName componentName) {}
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Fragment f = new SettingsFragment();
        f.setArguments(getIntent().getExtras());
        getFragmentManager().beginTransaction().replace(android.R.id.content, f).commit();
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
    }

    @Override
    public boolean onSupportNavigateUp()
    {
        finish();
        return true;
    }

    @Override
    public void finish()
    {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_right);
    }

    public static class SettingsFragment extends PreferenceFragment implements RNetServer.ConnectivityListener,
            RNetServer.SourcesListener
    {
        private int sourceId;

        private List<String> sourceTypes;
        private Preference sourceIdPreference;
        private EditTextPreference sourceNamePreference;
        private SwitchPreference overrideNamePreference;
        private ListPreference sourceTypePreference;
        private MultiZoneSelectListPreference autoOnPreference;
        private SwitchPreference autoOffPreference;

        private RNetServer server;
        private ServiceConnection serviceConnection = new ServiceConnection()
        {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder)
            {
                RNetServerService.LocalBinder binder = (RNetServerService.LocalBinder) iBinder;

                server = binder.getService().getServer();
                if (!server.isReady())
                {
                    getActivity().finish();
                }

                server.addConnectivityListener(SettingsFragment.this);
                server.addSourcesListener(SettingsFragment.this);

                Source source = server.getSource(sourceId);
                if (source == null)
                {
                    getActivity().finish();
                }
                else
                {
                    source.requestProperties();

                    ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
                    if (actionBar != null)
                        actionBar.setTitle(getString(R.string.format_object_settings, source.getName()));

                    sourceIdPreference.setSummary(getResources().getString(R.string.format_source_id, sourceId+1));

                    sourceNamePreference.setText(source.getName());
                    sourceNamePreference.setSummary(source.getName());

                    overrideNamePreference.setChecked(source.getOverrideName());

                    sourceTypePreference.setSummary(sourceTypes.get(source.getType()));
                    sourceTypePreference.setValueIndex(source.getType());

                    autoOnPreference.setZones(server.getZones());

                    if (source.getType() == Source.TYPE_GOOGLE_CAST)
                    {
                        if (source.getAutoOnZones().length > 0)
                        {
                            autoOnPreference.setSummary(getString(R.string.active));
                            autoOnPreference.setSelected(source.getAutoOnZones());
                        }
                        else
                            autoOnPreference.setSummary(R.string.disabled);

                        autoOffPreference.setChecked(source.getAutoOff());
                    }
                    else
                    {
                        autoOnPreference.setEnabled(false);
                        autoOnPreference.setSummary(R.string.disabled);
                        autoOffPreference.setEnabled(false);
                    }
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName)
            {
                server.removeConnectivityListener(SettingsFragment.this);
                server.removeSourcesListener(SettingsFragment.this);
                server = null;
            }
        };

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.source_settings);

            sourceId = getArguments().getInt("id", -1);
            if (sourceId == -1)
            {
                getActivity().finish();
                return;
            }

            sourceTypes = Arrays.asList(getResources().getStringArray(R.array.source_type));

            sourceIdPreference = findPreference("source_id");
            sourceNamePreference = (EditTextPreference) findPreference("source_name");
            overrideNamePreference = (SwitchPreference) findPreference("source_name_override");
            sourceTypePreference = (ListPreference) findPreference("source_type");
            autoOffPreference = (SwitchPreference) findPreference("auto_off");
            autoOnPreference = (MultiZoneSelectListPreference) findPreference("auto_on");

            sourceNamePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue)
                {
                    Source source = server.getSource(sourceId);
                    if (source != null)
                        source.setName((String) newValue, false);
                    return true;
                }
            });

            overrideNamePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue)
                {
                    Source source = server.getSource(sourceId);
                    if (source != null)
                        source.setOverrideName((boolean) newValue, false);
                    return true;
                }
            });

            sourceTypePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue)
                {
                    Source source = server.getSource(sourceId);
                    if (source != null)
                        source.setType(sourceTypePreference.findIndexOfValue((String) newValue), false);
                    return true;
                }
            });

            autoOnPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue)
                {
                    Source source = server.getSource(sourceId);
                    if (source != null)
                    {
                        int[][] autoOnZones = (int[][]) newValue;
                        source.setAutoOnZones(autoOnZones, false);
                    }

                    return true;
                }
            });

            autoOffPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
            {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue)
                {
                    Source source = server.getSource(sourceId);
                    if (source != null)
                        source.setAutoOff((Boolean) newValue, false);
                    return true;
                }
            });

            findPreference("delete_source").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    server.deleteSource(sourceId);
                    return true;
                }
            });
        }

        @Override
        public void onStart()
        {
            super.onStart();

            Intent intent = new Intent(getActivity(), RNetServerService.class);
            getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }

        @Override
        public void onStop()
        {
            super.onStop();

            getActivity().unbindService(serviceConnection);
            if (server != null)
            {
                server.removeConnectivityListener(this);
                server.removeSourcesListener(this);
            }
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
            getActivity().finish();
        }

        @Override
        public void sourceAdded(Source source) {}

        @Override
        public void sourceChanged(Source source, boolean setRemotely,
                RNetServer.SourceChangeType changeType)
        {
            switch (changeType)
            {
            case NAME:
                final String name = source.getName();
                getActivity().runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        sourceNamePreference.setText(name);
                        sourceNamePreference.setSummary(name);
                        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
                        if (actionBar != null)
                            actionBar.setTitle(getString(R.string.format_object_settings, name));
                    }
                });
                break;
            case TYPE:
                int type = source.getType();
                sourceTypePreference.setSummary(sourceTypes.get(type));
                sourceTypePreference.setValueIndex(type);
                if (type == Source.TYPE_GOOGLE_CAST)
                {
                    final boolean autoOn = source.getAutoOnZones().length > 0;
                    final boolean autoOff = source.getAutoOff();

                    getActivity().runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            autoOnPreference.setEnabled(true);
                            autoOffPreference.setEnabled(true);
                            if (autoOn)
                                autoOnPreference.setSummary(getString(R.string.active));
                            else
                                autoOnPreference.setSummary(R.string.disabled);
                            autoOffPreference.setChecked(autoOff);
                        }
                    });
                }
                else
                {
                    getActivity().runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            autoOnPreference.setEnabled(false);
                            autoOnPreference.setSummary(R.string.disabled);
                            autoOffPreference.setEnabled(false);
                            autoOffPreference.setChecked(false);
                        }
                    });
                }
                break;
            case AUTO_OFF:
                if (source.getType() == Source.TYPE_GOOGLE_CAST)
                {
                    final boolean autoOff = source.getAutoOff();
                    getActivity().runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            autoOffPreference.setChecked(autoOff);
                        }
                    });
                }
                break;
            case AUTO_ON:
                if (source.getType() == Source.TYPE_GOOGLE_CAST)
                {
                    final int[][] autoOnZones = source.getAutoOnZones();

                    getActivity().runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            autoOnPreference.setSelected(autoOnZones);

                            if (autoOnZones.length > 0)
                                autoOnPreference.setSummary(getString(R.string.active));
                            else
                                autoOnPreference.setSummary(R.string.disabled);
                        }
                    });
                }
                break;
            case OVERRIDE_NAME:
                final boolean override = source.getOverrideName();
                getActivity().runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        overrideNamePreference.setChecked(override);
                    }
                });
                break;
            }
        }

        @Override
        public void descriptiveText(Source source, String text, int length) {}

        @Override
        public void sourceRemoved(int sourceId)
        {
            if (sourceId == this.sourceId)
            {
                getActivity().finish();
            }
        }

        @Override
        public void cleared()
        {

        }
    }
}
