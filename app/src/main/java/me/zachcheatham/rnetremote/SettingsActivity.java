package me.zachcheatham.rnetremote;

import android.Manifest;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextThemeWrapper;

public class SettingsActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
                            .replace(android.R.id.content, new SettingsFragment()).commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults)
    {
        if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED)
        {
            new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AppTheme_DialogOverlay))
                    .setMessage(R.string.permission_error_phone)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
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
            implements SharedPreferences.OnSharedPreferenceChangeListener
    {
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesName("rnet_remote");
            addPreferencesFromResource(R.xml.settings);

            if (!getActivity().getPackageManager()
                              .hasSystemFeature(PackageManager.FEATURE_TELEPHONY))
            {
                getPreferenceScreen().removePreference(findPreference("mute_on_call"));
                getPreferenceScreen().removePreference(findPreference("mute_on_ring"));
            }
        }

        @Override
        public void onPause()
        {
            getPreferenceManager().getSharedPreferences()
                                  .unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

        @Override
        public void onResume()
        {
            super.onResume();
            getPreferenceManager().getSharedPreferences()
                                  .registerOnSharedPreferenceChangeListener(this);
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
                                .setMessage(R.string.explanation_phone)
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
    }
}
