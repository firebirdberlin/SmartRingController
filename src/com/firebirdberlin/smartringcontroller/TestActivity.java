package com.firebirdberlin.smartringcontrollerpro;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;

public class TestActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new TestFragment())
                .commit();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

     public static void open(Context context) {
        Intent myIntent = new Intent(context, TestActivity.class);
        context.startActivity(myIntent);
    }

    public void buttonClicked(View v){
        if(v.getId() == R.id.btnClearNotify) {
            Intent i = new Intent(this, SetRingerService.class);
            i.putExtra("PHONE_STATE", "TestService");
            startService(i);
        } else if (v.getId() == R.id.btnTestNotify) {
            Intent intent = new Intent(this, SmartRingController.class);
            PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

            // build notification
            Notification n = new Notification.Builder(this)
                .setContentTitle("Smart Ring Controller")
                .setContentText(getString(R.string.msgTestNotification))
                .setSmallIcon(R.drawable.ic_launcher_gray)
                .setContentIntent(pIntent)
                .setAutoCancel(true)
                .build();

            n.defaults |= Notification.DEFAULT_SOUND;
            //n.defaults |= Notification.DEFAULT_ALL;
            NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            notificationManager.notify(0, n);
        }
    }
}
