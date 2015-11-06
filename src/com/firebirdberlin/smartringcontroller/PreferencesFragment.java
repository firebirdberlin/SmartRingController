package com.firebirdberlin.smartringcontrollerpro;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceCategory;
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

        Preference goToDonation = (Preference) findPreference("openDonationPage");
        goToDonation.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                openDonationPage();
                return true;
            }
        });

        Preference prefSilentWhilePebbleConnected = (Preference) findPreference("SilentWhilePebbleConnected");
        if ( Utility.isPackageInstalled(getActivity(), "com.getpebble.android") == false ) {
            PreferenceCategory cat = (PreferenceCategory) findPreference("CategoryMuteActions");
            cat.removePreference(prefSilentWhilePebbleConnected);
            toggleComponentState(PebbleConnectionReceiver.class, false);
            toggleComponentState(PebbleDisconnectionReceiver.class, false);
            toggleComponentState(PebbleMessageReceiver.class, false);
        } else {
            prefSilentWhilePebbleConnected.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object new_value) {
                    boolean on = Boolean.parseBoolean(new_value.toString());
                    toggleComponentState(PebbleConnectionReceiver.class, on);
                    toggleComponentState(PebbleDisconnectionReceiver.class, on);
                    toggleComponentState(PebbleMessageReceiver.class, on);
                    return true;
                }
            });
        }
    }

    private void openDonationPage() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=5PX9XVHHE6XP8"));
        startActivity(browserIntent);
    }

    private void toggleComponentState(Class component, boolean on){
        ComponentName receiver = new ComponentName(getActivity(), component);
        PackageManager pm = getActivity().getPackageManager();
        int new_state = (on) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        pm.setComponentEnabledSetting(receiver, new_state, PackageManager.DONT_KILL_APP);
    }
}
