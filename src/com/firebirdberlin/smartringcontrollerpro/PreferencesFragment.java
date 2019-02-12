package com.firebirdberlin.smartringcontrollerpro;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.firebirdberlin.smartringcontrollerpro.events.OnNewAmbientNoiseValue;
import com.firebirdberlin.smartringcontrollerpro.pebble.PebbleConnectionReceiver;
import com.firebirdberlin.smartringcontrollerpro.pebble.PebbleDisconnectionReceiver;
import com.firebirdberlin.smartringcontrollerpro.pebble.PebbleMessageReceiver;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import de.firebirdberlin.preference.InlineProgressPreference;
import de.firebirdberlin.preference.InlineSeekBarPreference;


public class PreferencesFragment extends PreferenceFragment implements BillingHelperActivity.ItemPurchaseListener {
    private static int PERMISSION_REQUEST_RECORD_AUDIO = 1;

    public static final String TAG = SmartRingController.TAG + ".PreferencesFragment";
    private static final String PREFERENCE_SCREEN_RINGER_VOLUME = "Ctrl.RingerVolumePreferenceScreen";
    private final SoundMeter soundMeter = new SoundMeter();

    private Context context = null;
    private boolean volumePreferencesDisplayed = false;

    private InlineSeekBarPreference seekBarMinAmplitude = null;
    private InlineSeekBarPreference seekBarMaxAmplitude = null;
    private InlineProgressPreference progressBarRingerVolume = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getActivity().getApplicationContext();
        Settings settings = new Settings(context);

        // Define the settings file to use by this settings fragment
        getPreferenceManager().setSharedPreferencesName(SmartRingController.PREFS_KEY);

        addPreferencesFromResource(R.xml.preferences);

        SwitchPreference ctrlRingerVolume = (SwitchPreference) findPreference("Ctrl.RingerVolume");
        SwitchPreference ctrlNotification = (SwitchPreference) findPreference("handleNotification");
        SwitchPreference ctrlTTS = (SwitchPreference) findPreference("TTS.enabled");
        InlineSeekBarPreference seekBarMinRingerVolume = (InlineSeekBarPreference) findPreference("minRingerVolume");
        InlineSeekBarPreference seekBarAddPocketVolume = (InlineSeekBarPreference) findPreference("Ctrl.PocketVolume");
        seekBarMinAmplitude = (InlineSeekBarPreference) findPreference("minAmplitude");
        seekBarMaxAmplitude = (InlineSeekBarPreference) findPreference("maxAmplitude");
        progressBarRingerVolume = (InlineProgressPreference) findPreference("currentRingerVolumeValue");
        Preference buyDonation = findPreference("buyDonation");
        final PreferencesFragment listener = this;
        buyDonation.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ((SmartRingController) getActivity()).showPurchaseDialog(listener);
                return true;
            }
        });
        Preference buyPro = findPreference("buyPro");
        buyPro.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ((SmartRingController) getActivity()).showPurchaseDialog(listener);
                return true;
            }
        });

        ctrlRingerVolume.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                boolean ischecked = ((SwitchPreference) preference).isChecked();
                if (!ischecked && !Utility.hasPermission(context, Manifest.permission.RECORD_AUDIO)) {
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{
                                    Manifest.permission.RECORD_AUDIO,
                                    Manifest.permission.READ_PHONE_STATE
                            }, PERMISSION_REQUEST_RECORD_AUDIO);
                }

                return true;
            }
        });

        ctrlNotification.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                boolean ischecked = ((SwitchPreference) preference).isChecked();
                if (!ischecked && !Utility.hasPermission(context, Manifest.permission.RECORD_AUDIO)) {
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            PERMISSION_REQUEST_RECORD_AUDIO);
                }

                if (!ischecked && !isNotificationListenerServiceRunning()) {
                    requestNotificationListenerGrants();
                }

                return true;
            }
        });

        ctrlTTS.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                boolean ischecked = ((SwitchPreference) preference).isChecked();
                if (!ischecked && !isNotificationListenerServiceRunning()) {
                    requestNotificationListenerGrants();
                }

                return true;
            }
        });
        seekBarMinRingerVolume.setMax(settings.maxRingerVolume);
        seekBarAddPocketVolume.setMax(settings.maxRingerVolume);

        Preference prefPermissions = findPreference("prefPermissions");
        prefPermissions.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                openPermssionSettings();
                return true;
            }
        });
        Preference prefSendTestNotification = findPreference("sendTestNotification");
        prefSendTestNotification.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                sendTestNotification();
                return true;
            }
        });
        Preference prefSendTestNotification2 = findPreference("sendTestNotification2");
        prefSendTestNotification2.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                sendTestNotification();
                return true;
            }
        });

        Preference prefSystemSounds = findPreference("systemSoundPreferences");
        prefSystemSounds.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                startActivityForResult(new Intent(android.provider.Settings.ACTION_SOUND_SETTINGS), 0);
                return true;
            }
        });

        Preference prefSilentWhilePebbleConnected = findPreference("SilentWhilePebbleConnected");
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

        ((SmartRingController) getActivity()).updateAllPurchases();
    }

    private void requestNotificationListenerGrants() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.notification_listener) //
                .setMessage(R.string.notification_listener_message) //
                .setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                        dialog.dismiss();
                    }
                }) //
                .setNegativeButton(getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // TODO
                        dialog.dismiss();
                    }
                });
        builder.show();
    }

    private boolean isNotificationListenerServiceRunning() {
        return mNotificationListener.isRunning;
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

    @Subscribe
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
        NotificationCompat.Builder builder = Utility.buildNotification(context, SmartRingController.NOTIFICATION_CHANNEL_ID_TTS)
                .setContentTitle("SmartRingController")
                .setContentText(getString(R.string.msgTestNotification))
                .setSmallIcon(R.drawable.ic_logo_bw)
                .setContentIntent(pIntent)
                .setAutoCancel(true);

        Notification n = builder.build();

        n.defaults |= Notification.DEFAULT_SOUND;
        //n.defaults |= Notification.DEFAULT_ALL;
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
//            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0, n);
    }

    private void toggleComponentState(Class component, boolean on){
        ComponentName receiver = new ComponentName(getActivity(), component);
        PackageManager pm = getActivity().getPackageManager();
        int new_state = (on) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        pm.setComponentEnabledSetting(receiver, new_state, PackageManager.DONT_KILL_APP);
    }

    private void openPermssionSettings() {
        Intent intent = new Intent();
        intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    public void onItemPurchased(String sku) {
        // no matter which item was purchased, we enable everything
        enablePreference("preferenceScreenNotifications");
        enablePreference("preferenceScreenTTS");
        enablePreference("preferenceScreenVibration");
        enablePreference("preferenceScreenActions");
        removePreference("buyPro");
    }

    private void removePreference(String key) {
        Preference preference = findPreference(key);
        removePreference(preference);
    }

    private void removePreference(Preference preference) {
        if (preference == null) {
            return;
        }
        PreferenceGroup parent = getParent(getPreferenceScreen(), preference);
        if ( parent != null ) {
            parent.removePreference(preference);
        }
    }

    private void enablePreference(String key) {
        Preference preference = findPreference(key);

        if (preference != null) {
            preference.setEnabled(true);
        }
    }
    private PreferenceGroup getParent(PreferenceGroup root, Preference preference) {
        for (int i = 0; i < root.getPreferenceCount(); i++) {
            Preference p = root.getPreference(i);
            if (p == preference) {
                return root;
            }
            if (PreferenceGroup.class.isInstance(p)) {
                PreferenceGroup parent = getParent((PreferenceGroup)p, preference);
                if (parent != null) {
                    return parent;
                }
            }
        }
        return null;
    }

}
