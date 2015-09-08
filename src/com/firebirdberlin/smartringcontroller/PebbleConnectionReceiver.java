package com.firebirdberlin.smartringcontrollerpro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.Bundle;
import android.content.Intent;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class PebbleConnectionReceiver extends BroadcastReceiver {
    private final static String TAG = SmartRingController.TAG + ".PebbleConnectionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.i(TAG, "Pebble connected !");
        dumpIntent(intent);
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
