package com.firebirdberlin.smartringcontrollerpro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.os.Bundle;
import de.greenrobot.event.EventBus;

public class PebbleConnectionReceiver extends BroadcastReceiver {
    private final static String TAG = SmartRingController.TAG + ".PebbleConnectionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.i(TAG, "Pebble connected !");
        dumpIntent(intent);
        mAudioManager audiomanager = new mAudioManager(context);
        int ringerMode = audiomanager.getRingerMode();
        audiomanager.setRingerModeSilent();
        EventBus.getDefault().postSticky(new OnRingerModeChanged(ringerMode));
    }


    public static void dumpIntent(Intent i){

        Bundle bundle = i.getExtras();
        if (bundle == null) return;
        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            Logger.d(TAG, String.format("%s %s (%s)", key,
                            value.toString(), value.getClass().getName()));
        }
    }

}
