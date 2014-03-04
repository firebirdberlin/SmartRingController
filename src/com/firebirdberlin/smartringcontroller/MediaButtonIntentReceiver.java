package com.firebirdberlin.smartringcontrollerpro;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import android.util.Log;

public class MediaButtonIntentReceiver extends BroadcastReceiver {
    private static String TAG = "MediaButtonIntentReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "onReceive()");
        String intentAction = intent.getAction(); // I never get to this point in debugger (breakpoint set here)
		KeyEvent key = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
		Log.d(TAG, "key code = " + key.toString());
        // Code to actually handle button press ...

    }
}

