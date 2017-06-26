package com.firebirdberlin.smartringcontrollerpro;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.format.DateFormat;

import java.text.SimpleDateFormat;
import java.util.Date;

public class mNotificationListener extends NotificationListenerService {

    private String TAG = SmartRingController.TAG +"." + this.getClass().getSimpleName();
    private long last_notification_posted = 0;
    private final int min_notification_interval = 3000; // ms to be silent between notifications
    private SharedPreferences settings;
    private NLServiceReceiver nlservicereciver;


    @Override
    public void onCreate() {
        super.onCreate();
        nlservicereciver = new NLServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.firebirdberlin.smartringcontroller.NOTIFICATION_LISTENER_SERVICE_EXAMPLE");
        registerReceiver(nlservicereciver,filter);
        settings = getSharedPreferences(SmartRingController.PREFS_KEY, 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(nlservicereciver);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

        if (settings.getBoolean("enabled", false) == false) return;
        // phone call is handled elsewhere
        if (sbn.getPackageName().equals("com.android.phone")) return;
        Notification n = sbn.getNotification();


        if (sbn.getPackageName().equals("com.whatsapp")){
            // whatsapp sends no sound ... wtf ?
        } else // no sound, no action
        if ((n.defaults & Notification.DEFAULT_SOUND) == Notification.DEFAULT_SOUND){
            // do something--it was set
            // this is a notification with default sound
        } else {
            if (n.sound == null) {
                // determine music volume
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                if (am.isMusicActive()){
                    SharedPreferences.Editor prefEditor = settings.edit();
                    int vol = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                    Logger.d(TAG, "media volume " + String.valueOf(vol));

                    if (vol > 0){
                        prefEditor.putInt("lastMusicVolume", vol);
                        prefEditor.commit();
                    }
                }
                return;
            }
        }

        // if the last notification was within the last 3s
        // just queue the message but play no sound
        if ((System.currentTimeMillis() - last_notification_posted) < min_notification_interval){
            queueMessage(n, this);
            return;
        }

        last_notification_posted = System.currentTimeMillis();

        Logger.i(TAG,"**********  onNotificationPosted");
        Logger.i(TAG,"********** " + sbn.getPackageName());
        Logger.i(TAG,"********** " + sbn.getNotification().tickerText);

        Intent i = new  Intent("com.firebirdberlin.smartringcontroller.NOTIFICATION_LISTENER");
        i.putExtra("notification_event","notification :" + sbn.getPackageName());
        sendBroadcast(i);

        boolean handleNotification = settings.getBoolean("handle_notification", true);
        if (handleNotification) {
            Intent i2 = new Intent(this, SetRingerService.class);
            // potentially add data to the intent
            i2.putExtra("PHONE_STATE", "Notification");
            if (n.sound != null) {
                i2.putExtra("Sound", n.sound.toString() );
            }

            if (Build.VERSION.SDK_INT >= 21) {
                int interruptionFilter = getCurrentInterruptionFilter();
                Logger.d(TAG, "interruptionFilter = " + String.valueOf(interruptionFilter));
                if (interruptionFilter == NotificationListenerService.INTERRUPTION_FILTER_ALL) {
                    Logger.d(TAG, "starting SetRingerService");
                    startService(i2);
                }
            } else {
                startService(i2);
            }
        }

        queueMessage(n, this);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
    }

    class NLServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getStringExtra("command").equals("clearall")){
                    mNotificationListener.this.cancelAllNotifications();
            }
            else if(intent.getStringExtra("command").equals("list")){
                Intent i1 = new Intent("com.firebirdberlin.smartringcontroller.NOTIFICATION_LISTENER");
                i1.putExtra("notification_event","=====================");
                sendBroadcast(i1);
                int i=1;
                for (StatusBarNotification sbn : mNotificationListener.this.getActiveNotifications()) {
                    Intent i2 = new Intent("com.firebirdberlin.smartringcontroller.NOTIFICATION_LISTENER");
                    i2.putExtra("notification_event",i +" " + sbn.getPackageName() + "n");
                    sendBroadcast(i2);
                    i++;
                }
                Intent i3 = new Intent("com.firebirdberlin.smartringcontroller.NOTIFICATION_LISTENER");
                i3.putExtra("notification_event","===== Notification List ====");
                sendBroadcast(i3);
            }
        }
    }

    public static void queueMessage(Notification n, Context context){
        String text = getText(n, context);
        text = text.replace('#',' '); // replace hashtags
        text = text.replaceAll("\\(?\\b(http://|www[.])[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]"," "); // remove urls
        text = truncate(text, 1000); // truncate after 1000 chars
        TTSService.queueMessage(text, context);
    }

    public static String truncate(final String content, final int lastIndex) {
        if (lastIndex > content.length()) return content; // do nothing if short enough

        String result = content.substring(0, lastIndex);
        if (content.charAt(lastIndex) != ' ') { // find last word
            result = result.substring(0, result.lastIndexOf(" "));
        }
        return result;
    }

    public static String getText(Notification notification, Context context) {
        Bundle extras = notification.extras;
        String title = extras.getString(Notification.EXTRA_TITLE);
        String text = extras.getString(Notification.EXTRA_TEXT);
        String time = format_time(notification.when, context);

        String result = "";
        if (title != null) result += title + " ";
        if (text != null) result += text + " ";
        result += time;
        return result;
    }

    private static String format_time(long value, Context context) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm a");
        if ( DateFormat.is24HourFormat(context) ) {
            dateFormat = new SimpleDateFormat("H:mm");
        }

        return dateFormat.format(new Date(value));
    }
}
