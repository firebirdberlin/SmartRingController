package com.firebirdberlin.smartringcontrollerpro.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.firebirdberlin.smartringcontrollerpro.Logger;
import com.firebirdberlin.smartringcontrollerpro.TTSService;

public class HeadsetPlugReceiver extends BroadcastReceiver {
    private static final String TAG = HeadsetPlugReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            Logger.w(TAG, "Bundle is null. Doing nothing.");
            return;
        }
        if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
            int state = intent.getIntExtra("state", -1);
            switch (state) {
                case 0:
                    Logger.d(TAG, "Headset unplugged");
                    TTSService.stopReading(context);
                    return;
                case 1:
                    Logger.d(TAG, "Headset plugged");
                    break;
            }
        }
    }
}
