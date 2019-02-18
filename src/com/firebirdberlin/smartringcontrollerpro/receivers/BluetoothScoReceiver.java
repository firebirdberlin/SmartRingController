package com.firebirdberlin.smartringcontrollerpro.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;

import com.firebirdberlin.smartringcontrollerpro.Logger;
import com.firebirdberlin.smartringcontrollerpro.TTSService;

public class BluetoothScoReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = BluetoothScoReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            Logger.w(LOG_TAG, "Bundle is null. Doing nothing.");
            return;
        }

        if (intent.getAction().equals(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)) {
            receivedScoStateUpdate(bundle, context);
        } else {
            Logger.w(LOG_TAG, "Received unrecognized action broadcast: " + intent.getAction());
        }
    }

    private void receivedScoStateUpdate(Bundle bundle, Context context) {
        int previousState = bundle.getInt(AudioManager.EXTRA_SCO_AUDIO_PREVIOUS_STATE);
        int state = bundle.getInt(AudioManager.EXTRA_SCO_AUDIO_STATE);
        Logger.i(LOG_TAG, "sco state = " + previousState + " " + state);

        if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED ||
                state == AudioManager.SCO_AUDIO_STATE_ERROR) {
            AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
            audioManager.stopBluetoothSco();

            if (TTSService.shouldRead(false, context)) {
                // No SCO, but fall back to other methods if available.
                TTSService.startReading(context);
            } else if (previousState != AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
                // unless we are already disconnected (stopped when reading queue empties), stop reading
                TTSService.stopReading(context);
            }
        } else if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
            // Sleep a little to give the connection time to settle so that speech doesn't get cut off.
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Logger.w(LOG_TAG, e.toString());
            }
            TTSService.startReading(context);
        }
    }
}
