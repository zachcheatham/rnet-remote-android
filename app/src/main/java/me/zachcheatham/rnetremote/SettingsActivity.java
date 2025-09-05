package me.zachcheatham.rnetremote;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.ContextThemeWrapper;

import me.zachcheatham.rnetremotecommon.rnet.RNetServer;
import me.zachcheatham.rnetremotecommon.rnet.RNetServerService;

public class SettingsActivity extends AppCompatActivity
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

        getFragmentManager().beginTransaction()
                            .replace(android.R.id.content, new SettingsFragment()).commit();
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED)
        {
            new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AppTheme_DialogOverlay))
                    .setMessage(R.string.permission_error_phone)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    @Override
    public void startActivity(Intent intent)
    {
        super.startActivity(intent);
        overridePendingTransition(R.anim.slide_left, R.anim.fade_out);
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

    public static class SettingsFragment extends PreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener, RNetServer.ControllerListener,
            RNetServer.ConnectivityListener
    {
        private RNetServer server;
        private RNetServerService serverService;
        private ServiceConnection serviceConnection = new ServiceConnection()
        {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder)
            {
                RNetServerService.LocalBinder binder = (RNetServerService.LocalBinder) iBinder;

                serverService = binder.getService();
                server = serverService.getServer();

                if (serverService.hasServerInfo() && !server.isRunning())
                {
                    serverService.startServerConnection();
                }

                server.addConnectivityListener(SettingsFragment.this);
                server.addControllerListener(SettingsFragment.this);
                applyControllerSettings();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName)
            {
                server.removeConnectivityListener(SettingsFragment.this);
                server.removeControllerListener(SettingsFragment.this);
                applyControllerSettings();

                serverService = null;
                server = null;
            }
        };

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesName("rnet_remote");
            addPreferencesFromResource(R.xml.settings);

            if (!getActivity().getPackageManager()
                              .hasSystemFeature(PackageManager.FEATURE_TELEPHONY))
            {
                getPreferenceScreen().removePreference(findPreference("phone_calls"));
            }

            findPreference("controller_name").setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener()
                    {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue)
                        {
                            if (server != null && server.isReady())
                                server.setName((String) newValue);

                            return true;
                        }
                    });
            findPreference("application_version").setSummary(BuildConfig.VERSION_NAME);
        }

        @Override
        public void onStart()
        {
            super.onStart();

            Intent intent = new Intent(getActivity(), RNetServerService.class);
            getActivity().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }

        @Override
        public void onResume()
        {
            super.onResume();
            getPreferenceManager().getSharedPreferences()
                                  .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause()
        {
            getPreferenceManager().getSharedPreferences()
                                  .unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

        @Override
        public void onStop()
        {
            super.onStop();

            unbindService();
            applyControllerSettings();
        }

        private void unbindService()
        {
            if (server != null)
            {
                server.removeConnectivityListener(this);
                server.removeControllerListener(this);
                server = null;
            }

            if (serverService != null)
            {
                getActivity().unbindService(serviceConnection);
                serverService = null;
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
        {
            if (key.equals("mute_on_ring") || key.equals("mute_on_call"))
            {
                boolean enabled = sharedPreferences.getBoolean(key, false);
                if (enabled && ContextCompat.checkSelfPermission(this.getActivity(),
                        Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
                {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this.getActivity(),
                            Manifest.permission.READ_PHONE_STATE))
                    {
                        new AlertDialog.Builder(new ContextThemeWrapper(getActivity(),
                                R.style.AppTheme_DialogOverlay))
                                .setMessage(R.string.permission_explanation_phone)
                                .setPositiveButton(android.R.string.ok,
                                        new DialogInterface.OnClickListener()
                                        {
                                            public void onClick(DialogInterface dialog,
                                                    int whichButton)
                                            {
                                                ActivityCompat.requestPermissions(getActivity(),
                                                        new String[]{
                                                                Manifest.permission
                                                                        .READ_PHONE_STATE},
                                                        1);
                                            }
                                        })
                                .show();
                    }
                    else
                    {
                        ActivityCompat.requestPermissions(this.getActivity(),
                                new String[]{Manifest.permission.READ_PHONE_STATE}, 1);
                    }
                }
            }
        }

        private void applyControllerSettings()
        {
            Preference versionPreference = findPreference("controller_version");
            Preference addressPreference = findPreference("controller_address");
            EditTextPreference namePreference = (EditTextPreference) findPreference(
                    "controller_name");
            //Preference webServerPreference = findPreference("controller_web_server");

            if (server != null && server.isReady())
            {
                versionPreference.setSummary(server.getVersion());
                versionPreference.setEnabled(true);
                addressPreference
                        .setSummary(server.getAddress().getHostAddress() + ":" + server.getPort());
                addressPreference.setEnabled(true);
                namePreference.setSummary(server.getName());
                namePreference.setText(server.getName());
                namePreference.setEnabled(true);
                //webServerPreference.setEnabled(true);
                findPreference("manage_sources").setEnabled(true);
            }
            else
            {
                versionPreference.setSummary(getString(R.string.label_preference_disconnected));
                versionPreference.setEnabled(false);
                addressPreference.setSummary(getString(R.string.label_preference_disconnected));
                addressPreference.setEnabled(false);
                namePreference.setSummary(getString(R.string.label_preference_disconnected));
                namePreference.setEnabled(false);
                //webServerPreference.setEnabled(false);
                findPreference("manage_sources").setEnabled(false);
            }
        }

        @Override
        public void connectionInitiated() {}

        @Override
        public void connectError() {}

        @Override
        public void ready()
        {
            getActivity().runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    applyControllerSettings();
                }
            });
        }

        @Override
        public void updateAvailable() {}

        @Override
        public void propertyChanged(int prop, Object value)
        {
            getActivity().runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    applyControllerSettings();
                }
            });
        }

        @Override
        public void disconnected(boolean unexpected)
        {
            getActivity().runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    applyControllerSettings();
                }
            });
        }
    }
}
