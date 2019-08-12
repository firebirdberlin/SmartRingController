package com.firebirdberlin.smartringcontrollerpro;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {
    public static final String PREFS_KEY = SmartRingController.PREFS_KEY;

    Context mContext;
    SharedPreferences settings;

    public boolean FlipAction = false;
    public boolean PullOutAction = false;
    public boolean ShakeAction = false;
    public boolean TTSenabled = false;
    public boolean brokenProximitySensor = false;
    public boolean controlRingerVolume = true; // set the ringer volume based on ambient noise
    public boolean disconnectWhenFaceDown = false;
    public boolean enabled = true;
    public boolean handleNotification = false;
    public boolean handleVibration = false;
    public boolean increasingRingerVolume = false;
    public boolean showDnDNotification = false;
    public double maxAmplitude = 10000.;
    public double minAmplitude = 1000.;
    public int addPocketVolume = 0; // increase in pocket
    public int maxRingerVolume = 7;
    public int minRingerVolume = 1; // 0 means vibration
    public String defaultNotificationUri = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI.toString();
    private mAudioManager audiomanager = null;

    public Settings(Context context){
        this.mContext = context;
        settings = context.getSharedPreferences(PREFS_KEY, 0);
        audiomanager = new mAudioManager(context);
        reload();
    }

    public void reload() {
        FlipAction = settings.getBoolean("FlipAction", false);
        PullOutAction = settings.getBoolean("PullOutAction", false);
        ShakeAction = settings.getBoolean("ShakeAction", false);
        TTSenabled = settings.getBoolean("TTS.enabled", false);
        addPocketVolume = settings.getInt("Ctrl.PocketVolume", 0);
        brokenProximitySensor = settings.getBoolean("Ctrl.BrokenProximitySensor", true);
        controlRingerVolume = settings.getBoolean("Ctrl.RingerVolume", true);
        disconnectWhenFaceDown = settings.getBoolean("disconnectWhenFaceDown", false);
        enabled = settings.getBoolean("enabled", true);
        handleNotification = settings.getBoolean("handleNotification", true);
        handleVibration = settings.getBoolean("handle_vibration", false);
        increasingRingerVolume = settings.getBoolean("increasingRingerVolume", false);
        maxAmplitude = (double) settings.getInt("maxAmplitude", 10000);
        maxRingerVolume = audiomanager.getMaxRingerVolume();
        minAmplitude = (double) settings.getInt("minAmplitude", 500);
        minRingerVolume = settings.getInt("minRingerVolume", 1);
        showDnDNotification = settings.getBoolean("showDnDNotification", false);
        defaultNotificationUri = settings.getString("defaultNotificationUri",
                                                    android.provider.Settings.System.DEFAULT_NOTIFICATION_URI.toString());
    }

    public int getRingerVolume(double currentAmplitude, boolean deviceIsCovered,
                               boolean wiredHeadsetIsOn) {
        double min = this.minAmplitude;
        double max = this.maxAmplitude;
        float diffRingerVolume = maxRingerVolume - minRingerVolume;
        int volume = minRingerVolume
                     + (int) ( diffRingerVolume * (currentAmplitude - min)/ (max - min) );

        if (deviceIsCovered) volume += addPocketVolume;
        if (volume > maxRingerVolume) volume = maxRingerVolume;
        if (volume < minRingerVolume) volume = minRingerVolume;

        if ( wiredHeadsetIsOn ) {
            // limit the maximum ringer volume
            if (volume > maxRingerVolume/2) volume = maxRingerVolume/2;
        }

        return volume;
    }
}
