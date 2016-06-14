package com.firebirdberlin.smartringcontrollerpro;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Build;


public class RingerModeStateChangeReceiver extends BroadcastReceiver {
    private final static String TAG = SmartRingController.TAG + ".RingerModeStateChangeReceiver";
    private final static int NOTIFICATION_ID = 627;
    private Context mContext;
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        mContext = context;
        Bundle bundle = intent.getExtras();
        int newRingerMode = bundle.getInt(AudioManager.EXTRA_RINGER_MODE, -1);
        switch (newRingerMode) {
            case AudioManager.RINGER_MODE_NORMAL:
                cancelNotification();
                break;
            case AudioManager.RINGER_MODE_SILENT:
                postNotification();
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                postNotification();
                break;
        }
    }

    private void cancelNotification() {
        NotificationManager notificationManager =
            (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private void postNotification() {
        Intent IntentUnmute = new Intent(mContext, EnjoyTheSilenceService.class);
        IntentUnmute.putExtra("action", "unmute");
        IntentUnmute.putExtra("systemRingerMode", AudioManager.RINGER_MODE_NORMAL);
        PendingIntent pIntentUnmute = PendingIntent.getService(mContext, 0, IntentUnmute, 0);

        Notification note  = new Notification.Builder(mContext)
            .setContentTitle(mContext.getString(R.string.titleSilenceMode))
            .setContentText(mContext.getString(R.string.msgRestoreRingerMode))
            .setSmallIcon(R.drawable.ic_logo_bw)
            .setColor(Color.RED)
            .setContentIntent(pIntentUnmute)
            .setAutoCancel(true)
            //.addAction(R.drawable.ic_launcher, "unmute", pIntentUnmute)
            //.addAction(R.drawable.ic_launcher_gray, "More", pIntentUnmute)
            .build();
        note.flags |= Notification.FLAG_NO_CLEAR;
        note.flags |= Notification.FLAG_ONGOING_EVENT;

        if (Build.VERSION.SDK_INT >= 16) {
            note.priority = Notification.PRIORITY_HIGH;
        }

        NotificationManager notificationManager =
            (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(NOTIFICATION_ID, note);
    }

}
