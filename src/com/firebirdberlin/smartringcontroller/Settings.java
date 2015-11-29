package com.firebirdberlin.smartringcontrollerpro;

import android.os.Build;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

public class Settings {
    public static final String PREFS_KEY = SmartRingController.PREFS_KEY;

    Context mContext;
    SharedPreferences settings;

    public boolean brokenProximitySensor = false;
    public boolean controlRingerVolume = true; // set the ringer volume based on ambient noise
    public boolean enabled = false;
    public boolean FlipAction = false;
    public boolean handleNotification = false;
    public boolean handleVibration = false;
    public boolean PullOutAction = false;
    public boolean ShakeAction = false;
    public boolean TTSenabled = false;
    public double maxAmplitude = 10000.;
    public double minAmplitude = 1000.;
    public int addPocketVolume = 0; // increase in pocket
    public int maxRingerVolume = 7;
    public int minRingerVolume = 1; // 0 means vibration
    private mAudioManager audiomanager = null;

    public Settings(Context context){
        this.mContext = context;
        settings = context.getSharedPreferences(PREFS_KEY, 0);
        audiomanager = new mAudioManager(context);
        reload();
    }

    public void reload() {
        addPocketVolume = settings.getInt("Ctrl.PocketVolume", 0);
        brokenProximitySensor = settings.getBoolean("Ctrl.BrokenProximitySensor", true);
        controlRingerVolume = settings.getBoolean("Ctrl.RingerVolume", true);
        enabled = settings.getBoolean("enabled", false);
        FlipAction = settings.getBoolean("FlipAction", false);
        handleNotification = settings.getBoolean("handle_notification", true);
        handleVibration = settings.getBoolean("handle_vibration", false);
        maxAmplitude = (double) settings.getInt("maxAmplitude", 10000);
        minAmplitude = (double) settings.getInt("minAmplitude", 500);
        maxRingerVolume = audiomanager.getMaxRingerVolume();
        minRingerVolume = settings.getInt("minRingerVolume", 1);
        PullOutAction = settings.getBoolean("PullOutAction", false);
        ShakeAction = settings.getBoolean("ShakeAction", false);
        TTSenabled = settings.getBoolean("TTS.enabled", false);
    }

    public int getRingerVolume(double currentAmplitude, boolean deviceIsCovered) {
        double min = this.minAmplitude;
        double max = this.maxAmplitude;
        float diffRingerVolume = maxRingerVolume - minRingerVolume;
        int volume = minRingerVolume
                     + (int) ( diffRingerVolume * (currentAmplitude - min)/ (max - min) );

        if (deviceIsCovered) volume += addPocketVolume;
        if (volume > maxRingerVolume) volume = maxRingerVolume;
        if (volume < minRingerVolume) volume = minRingerVolume;

        return volume;
    }
}
