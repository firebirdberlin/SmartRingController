package com.firebirdberlin.smartringcontrollerpro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.telephony.TelephonyManager;

import com.firebirdberlin.smartringcontrollerpro.R;

public class IncomingCallReceiver extends BroadcastReceiver {
    private final static String TAG = "IncomingCallReceiver";
    public static String currentCallState = "None";
    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.d(TAG, "onReceive");

        SharedPreferences settings = context.getSharedPreferences(SmartRingController.PREFS_KEY, 0);
        if (settings.getBoolean("enabled", false) == false) return;

        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        currentCallState = state;
        String msg = "Phone state changed to " + state;
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {

            String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);  // 5
            msg += ". Incoming number is " + incomingNumber;
            Logger.d(TAG, msg);
            Intent i =  new Intent(context, SetRingerService.class);
            i.putExtra("PHONE_STATE", state);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startService(i);

            // activate tts
            /*
            if (am.isBluetoothA2dpOn() == false && incomingNumber != null){
                TTSService.stopReading(context);
                String from = Utility.getContactNameFromNumber(context, incomingNumber, context.getContentResolver());

                String text = String.format(
                        "%s %s.", context.getString(R.string.TTS_AnnounceCall), from);
                TTSService.queueMessage(text, context);
            }
            */

        } else { // OFFHOOK or IDLE
            TTSService.stopReading(context);
        }


        if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            // Using a bluetooth headset the media volume remains muted.
            // It seems to be a bug in android 4.3. The problem appears
            // also when Smart Ring Controller is disabled.
            // So we reset the volume manually ...
            int vol = settings.getInt("lastMusicVolume", 7);
            am.setStreamVolume(AudioManager.STREAM_MUSIC, vol, AudioManager.FLAG_SHOW_UI);
            Logger.d(TAG, "setting media volume " + String.valueOf(vol));
        }
    }
}
