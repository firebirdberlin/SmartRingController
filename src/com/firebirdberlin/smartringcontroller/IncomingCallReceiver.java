package com.firebirdberlin.smartringcontrollerpro;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.TelephonyManager;
import android.widget.Toast;
import android.os.Vibrator;

public class IncomingCallReceiver extends BroadcastReceiver {
	private final static String TAG = SmartRingController.TAG + ".IncomingCallReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        String msg = "Phone state changed to " + state;

        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);  // 5
            msg += ". Incoming number is " + incomingNumber;

			Intent i= new Intent(context, SetRingerService.class);
			i.putExtra("PHONE_STATE", state);
			context.startService(i);

			TTSService.stopReading(context);
			String from = getContactNameFromNumber(incomingNumber, context.getContentResolver());

			String text = context.getString(R.string.TTS_AnnounceCall) +
							" " + from + ".";
			TTSService.queueMessage(text, context);

        } else{ // OFFHOOK or IDLE
			TTSService.stopReading(context);
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
