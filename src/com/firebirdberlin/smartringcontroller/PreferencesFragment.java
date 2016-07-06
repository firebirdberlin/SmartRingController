package com.firebirdberlin.smartringcontrollerpro;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import de.greenrobot.event.EventBus;


public class PreferencesFragment extends PreferenceFragment {
    public static final String TAG = SmartRingController.TAG + ".PreferencesFragment";
    private static final String PREFERENCE_SCREEN_RINGER_VOLUME = "Ctrl.RingerVolumePreferenceScreen";
    private final SoundMeter soundMeter = new SoundMeter();

    private Context context = null;
    private boolean volumePreferencesDisplayed = false;

    private InlineSeekBarPreference seekBarMinAmplitude = null;
    private InlineSeekBarPreference seekBarMaxAmplitude = null;
    private InlineSeekBarPreference seekBarMinRingerVolume = null;
    private InlineSeekBarPreference seekBarAddPocketVolume = null;
    private InlineProgressPreference progressBarRingerVolume = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getActivity().getApplicationContext();
        Settings settings = new Settings(context);

        // Define the settings file to use by this settings fragment
        getPreferenceManager().setSharedPreferencesName(SmartRingController.PREFS_KEY);

        addPreferencesFromResource(R.layout.preferences);

        seekBarMinAmplitude = (InlineSeekBarPreference) findPreference("minAmplitude");
        seekBarMaxAmplitude = (InlineSeekBarPreference) findPreference("maxAmplitude");
        seekBarMinRingerVolume = (InlineSeekBarPreference) findPreference("minRingerVolume");
        seekBarAddPocketVolume = (InlineSeekBarPreference) findPreference("Ctrl.PocketVolume");
        progressBarRingerVolume = (InlineProgressPreference) findPreference("currentRingerVolumeValue");

        seekBarMinRingerVolume.setMax(settings.maxRingerVolume);
        seekBarAddPocketVolume.setMax(settings.maxRingerVolume);

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

        Preference prefSendTestNotification = (Preference) findPreference("sendTestNotification");
        prefSendTestNotification.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                sendTestNotification();
                return true;
            }
        });

        Preference prefSystemSounds = (Preference) findPreference("systemSoundPreferences");
        prefSystemSounds.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                startActivityForResult(new Intent(android.provider.Settings.ACTION_SOUND_SETTINGS), 0);
                return true;
            }
        });

        Preference prefSilentWhilePebbleConnected = (Preference) findPreference("SilentWhilePebbleConnected");
        boolean installed = Utility.isPackageInstalled(getActivity(), "com.getpebble.android") ||
                            Utility.isPackageInstalled(getActivity(), "com.getpebble.android.basalt");

        if ( ! installed ) {
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

    @Override
    public void onResume(){
        super.onResume();
        EventBus.getDefault().register(this);
        handleAmbientNoiseMeasurement();
    }

    @Override
    public void onPause(){
        super.onPause();
        EventBus.getDefault().unregister(this);
        soundMeter.stopMeasurement();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        String key = "";
        volumePreferencesDisplayed = false;
        if( preference != null && preference.getKey() != null ) {
            key = preference.getKey();
            volumePreferencesDisplayed = key.equals(PREFERENCE_SCREEN_RINGER_VOLUME);
        }
        handleAmbientNoiseMeasurement();
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void handleAmbientNoiseMeasurement() {
        if (volumePreferencesDisplayed) {
            new measureAmbientNoiseTask().execute();
        } else {
            soundMeter.stopMeasurement();
        }
    }

    public void onEvent(OnNewAmbientNoiseValue event) {
        seekBarMinAmplitude.setSecondaryProgress((int) event.value);
        seekBarMaxAmplitude.setSecondaryProgress((int) event.value);

        Context context = getActivity().getApplicationContext();
        Settings settings = new Settings(context);
        int volume = settings.getRingerVolume(event.value, false, false);

        progressBarRingerVolume.setMax(settings.maxRingerVolume);
        progressBarRingerVolume.setProgress(volume);
    }

    private class measureAmbientNoiseTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void...params) {
            soundMeter.startMeasurement(500);
            return null;
        }
    }

    private void sendTestNotification() {
        Intent intent = new Intent(context, SmartRingController.class);
        PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent, 0);

        // build notification
        Notification n = new Notification.Builder(context)
            .setContentTitle("SmartRingController")
            .setContentText(getString(R.string.msgTestNotification))
            .setSmallIcon(R.drawable.ic_logo_bw)
            .setContentIntent(pIntent)
            .setAutoCancel(true)
            .build();

        n.defaults |= Notification.DEFAULT_SOUND;
        //n.defaults |= Notification.DEFAULT_ALL;
        NotificationManager notificationManager =
            (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);

        notificationManager.notify(0, n);
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
