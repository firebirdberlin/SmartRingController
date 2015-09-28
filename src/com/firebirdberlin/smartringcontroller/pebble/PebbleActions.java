package com.firebirdberlin.smartringcontrollerpro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.os.Bundle;

public class PebbleActions {

    public static void mute(Context context) {
        mAudioManager audiomanager = new mAudioManager(context);

        // if there is still an event then we have missed the disconnection event
        SharedPreferences settings = context.getSharedPreferences(SmartRingController.PREFS_KEY, 0);
        int ringerMode = settings.getInt("previousRingerMode", audiomanager.getRingerMode());

        audiomanager.setRingerModeSilent();

        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putInt("previousRingerMode", ringerMode);
        prefEditor.commit();
    }

    public static void unmute(Context context) {
        SharedPreferences settings = context.getSharedPreferences(SmartRingController.PREFS_KEY, 0);
        int ringerMode = settings.getInt("previousRingerMode", AudioManager.RINGER_MODE_NORMAL);

        mAudioManager audiomanager = new mAudioManager(context);
        audiomanager.restoreRingerModeTo(ringerMode);

        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.remove("previousRingerMode");
        prefEditor.apply();

    }

}
