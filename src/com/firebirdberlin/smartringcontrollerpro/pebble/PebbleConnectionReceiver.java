package com.firebirdberlin.smartringcontrollerpro.pebble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.firebirdberlin.smartringcontrollerpro.Logger;

public class PebbleConnectionReceiver extends BroadcastReceiver {
    private final static String TAG = "PebbleConnectionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.i(TAG, "Pebble connected !");
        PebbleActions.mute(context);
    }
}
