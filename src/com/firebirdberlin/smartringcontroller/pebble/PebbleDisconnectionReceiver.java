package com.firebirdberlin.smartringcontrollerpro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.os.Bundle;
import de.greenrobot.event.EventBus;

public class PebbleDisconnectionReceiver extends BroadcastReceiver {
    private final static String TAG = SmartRingController.TAG + ".PebbleDisonnectionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.i(TAG, "Pebble disconnected !");
        PebbleActions.unmute(context);
    }
}
