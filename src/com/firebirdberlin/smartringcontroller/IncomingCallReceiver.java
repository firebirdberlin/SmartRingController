package com.firebirdberlin.smartringcontrollerpro;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.TelephonyManager;
import android.widget.Toast;
import android.os.Vibrator;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class IncomingCallReceiver extends BroadcastReceiver {
	private final static String TAG = SmartRingController.TAG + ".IncomingCallReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
		SharedPreferences settings = context.getSharedPreferences(SmartRingController.PREFS_KEY, 0);
		if (settings.getBoolean("enabled", false) == false) return;

		AudioManager am=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);

        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        String msg = "Phone state changed to " + state;
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {

			if (am.isMusicActive()){
				SharedPreferences.Editor prefEditor = settings.edit();
				int vol = am.getStreamVolume(AudioManager.STREAM_MUSIC);
				Logger.d(TAG, "media volume " + String.valueOf(vol));

				if (vol > 0){
					prefEditor.putInt("lastMusicVolume", vol);
					prefEditor.commit();
				}
			}

            String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);  // 5
            msg += ". Incoming number is " + incomingNumber;

			Intent i= new Intent(context, SetRingerService.class);
			i.putExtra("PHONE_STATE", state);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startService(i);

			TTSService.stopReading(context);
			String from = getContactNameFromNumber(incomingNumber, context.getContentResolver());

			String text = context.getString(R.string.TTS_AnnounceCall) +
							" " + from + ".";
			TTSService.queueMessage(text, context);

        } else{ // OFFHOOK or IDLE
			TTSService.stopReading(context);
        }

		if (am.isMusicActive()){
		 if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
			// Using a bluetooth headset the media volume reminas muted.
			// It seems to be a bug in android 4.3. The problem appears
			// also when Smart Ring Controller is disabled.
			// So we reset thhe volume manually ...
			int vol	= settings.getInt("lastMusicVolume", 7);
			if ( vol == 0 ) vol = 7;
			am.setStreamVolume(AudioManager.STREAM_MUSIC, vol, AudioManager.FLAG_SHOW_UI);
			Logger.d(TAG, "setting media volume " + String.valueOf(vol));
		 }
		}

		//Toast.makeText(context, msg, Toast.LENGTH_LONG).show();

    }

	private String getContactNameFromNumber(String number, ContentResolver contentResolver) {
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

		Cursor cursor = null;
		try {
		  cursor = contentResolver.query(uri, new String[]{ PhoneLookup.DISPLAY_NAME }, null, null, null);

		  if (cursor.isAfterLast()) {
			// If nothing was found, return the number....
			Logger.w(TAG, "Unable to look up incoming number in contacts");
			return number;
		  }

		  // ...otherwise return the first entry.
		  cursor.moveToFirst();
		  int nameFieldColumnIndex = cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME);
		  String contactName = cursor.getString(nameFieldColumnIndex);
		  return contactName;
		} finally {
		  cursor.close();
		}
	}


}
