package com.firebirdberlin.smartringcontrollerpro.pebble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.firebirdberlin.smartringcontrollerpro.Logger;

public class PebbleDisconnectionReceiver extends BroadcastReceiver {
    private final static String TAG = "PebbleDisonnectionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.i(TAG, "Pebble disconnected !");
        PebbleActions.unmute(context);
    }
}
