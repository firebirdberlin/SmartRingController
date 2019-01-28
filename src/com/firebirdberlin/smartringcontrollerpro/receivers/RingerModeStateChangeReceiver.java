package com.firebirdberlin.smartringcontrollerpro.receivers;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;

import com.firebirdberlin.smartringcontrollerpro.EnjoyTheSilenceService;
import com.firebirdberlin.smartringcontrollerpro.R;
import com.firebirdberlin.smartringcontrollerpro.SmartRingController;
import com.firebirdberlin.smartringcontrollerpro.Utility;


public class RingerModeStateChangeReceiver extends BroadcastReceiver {
    private final static String TAG = "RingerModeStateChangeReceiver";
    private final static int NOTIFICATION_ID = 627;
    private Context mContext;
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }
        mContext = context;
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

        Notification note  = Utility.buildNotification(mContext, SmartRingController.NOTIFICATION_CHANNEL_ID_STATUS)
            .setContentTitle(mContext.getString(R.string.titleSilenceMode))
            .setContentText(mContext.getString(R.string.msgRestoreRingerMode))
            .setSmallIcon(R.drawable.ic_logo_bw)
            .setColor(Color.RED)
            .setContentIntent(pIntentUnmute)
            .setAutoCancel(true)
            .build();
        note.flags |= Notification.FLAG_NO_CLEAR;
        note.flags |= Notification.FLAG_ONGOING_EVENT;
        note.priority = Notification.PRIORITY_HIGH;

        NotificationManager notificationManager =
            (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(NOTIFICATION_ID, note);
    }

}
