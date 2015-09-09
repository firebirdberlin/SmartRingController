package com.firebirdberlin.smartringcontrollerpro;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;

public class PreferencesFragment extends PreferenceFragment {
    public static final String TAG = SmartRingController.TAG + ".PreferencesFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Define the settings file to use by this settings fragment
        getPreferenceManager().setSharedPreferencesName(SmartRingController.PREFS_KEY);

        addPreferencesFromResource(R.layout.preferences);

        Preference goToSettings = (Preference) findPreference("openNotificationListenerSettings");
        goToSettings.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                if (Build.VERSION.SDK_INT < 18) {
                    Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivityForResult(intent, 0);
                } else {
                    Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                    startActivityForResult(intent, 0);
                }
                return true;
            }
        });
    }
}
