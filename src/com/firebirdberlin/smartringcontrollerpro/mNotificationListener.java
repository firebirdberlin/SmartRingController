package com.firebirdberlin.smartringcontrollerpro;

import android.app.Notification;
import android.app.NotificationChannel;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;

import com.firebirdberlin.smartringcontrollerpro.receivers.BluetoothScoReceiver;
import com.firebirdberlin.smartringcontrollerpro.receivers.HeadsetPlugReceiver;
import com.firebirdberlin.smartringcontrollerpro.receivers.IncomingCallReceiver;
import com.firebirdberlin.smartringcontrollerpro.receivers.RingerModeStateChangeReceiver;

import java.text.SimpleDateFormat;
import java.util.Date;

public class mNotificationListener extends NotificationListenerService {

    private String TAG = this.getClass().getSimpleName();
    private long last_notification_posted = 0;
    private final int min_notification_interval = 3000; // ms to be silent between notifications
    private SharedPreferences settings;
    private RingerModeStateChangeReceiver ringerModeStateChangeReceiver;
    private BluetoothScoReceiver bluetoothScoReceiver;
    private HeadsetPlugReceiver headsetPlugReceiver;
    public static boolean isRunning = false;

    String lastText;
    Runnable dropLastMessage = new Runnable() {
        @Override
        public void run() {
            new Handler().removeCallbacks(dropLastMessage);
            lastText = null;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        settings = getSharedPreferences(SmartRingController.PREFS_KEY, 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    @Override
    public void onListenerConnected() {
        isRunning = true;
        ringerModeStateChangeReceiver = new RingerModeStateChangeReceiver();
        registerReceiver(ringerModeStateChangeReceiver, new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION));

        bluetoothScoReceiver = new BluetoothScoReceiver();
        registerReceiver(ringerModeStateChangeReceiver, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));

        headsetPlugReceiver = new HeadsetPlugReceiver();
        registerReceiver(headsetPlugReceiver, new IntentFilter(AudioManager.ACTION_HEADSET_PLUG));
    }

    @Override
    public void onListenerDisconnected() {
        isRunning = false;
        unregisterReceiver(bluetoothScoReceiver);
        unregisterReceiver(headsetPlugReceiver);
        unregisterReceiver(ringerModeStateChangeReceiver);
        bluetoothScoReceiver = null;
        headsetPlugReceiver = null;
        ringerModeStateChangeReceiver = null;
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {
        if (receiver != null) {
            try {
                super.unregisterReceiver(receiver);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

        if (!settings.getBoolean("enabled", true)) return;
        Notification n = sbn.getNotification();
        Uri notificationSound = getNotificationSound(sbn);
        String text = getText(n, this);
        if (! "com.firebirdberlin.smartringcontrollerpro".equals(sbn.getPackageName())) {
            Logger.i(TAG, "**********  onNotificationPosted  ************************************");
            Logger.i(TAG, "**********   package " + sbn.getPackageName());
            Logger.i(TAG, "**********       key " + sbn.getKey());
            Logger.i(TAG, "********** group_key " + sbn.getGroupKey());
            if (Build.VERSION.SDK_INT >= 24) {
                Logger.i(TAG, "********** override group key " + sbn.getOverrideGroupKey());
            }
            Logger.i(TAG, "**********      text " + text);
            Logger.i(TAG, "**********     sound " + ((notificationSound != null) ? notificationSound.toString() : ""));
            Logger.i(TAG, "********** clearable " + sbn.isClearable());
            Logger.i(TAG, "**********   ongoing " + sbn.isOngoing());

        }

        int importance = getImportance(sbn);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            RankingMap rankingMap = getCurrentRanking();
            Logger.i(TAG, "rankingMap");
            for (String key : rankingMap.getOrderedKeys()) {
                Ranking ranking = new Ranking();
                rankingMap.getRanking(key, ranking);
                Logger.i(TAG, String.format(">>> %s | %d", key, ranking.getImportance()));
            }
        }

        // phone call is handled elsewhere
        if (sbn.getPackageName().equals("com.android.phone")) return;

        // Notifications sometimes appear twice. We identify successive notifications be equality
        // of their keys and ignore them.
        if (lastText != null && lastText.equals(text)) {
            Logger.i(TAG, "********  Duplicate notification !");
            return;
        }

        if (sbn.getPackageName().equals("com.google.android.dialer") && !sbn.isClearable()
                && TelephonyManager.EXTRA_STATE_RINGING.equals(IncomingCallReceiver.currentCallState)) {
            String number = getTitle(n);
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (!am.isBluetoothA2dpOn()) {
                String from = Utility.getContactNameFromNumber(this, number, this.getContentResolver());
                String msg = String.format("%s %s.", getString(R.string.TTS_AnnounceCall), from);
                queueMessage(msg, this);
            }
            new Handler().postDelayed(dropLastMessage, 5000);
            return;
        }

        if (!sbn.isClearable() || importance <= 2 || sbn.isOngoing()) {
            return;
        }

        queueMessage(n, this);
        if (!settings.getBoolean("handleNotification", false)) return;
        if ((System.currentTimeMillis() - last_notification_posted) < min_notification_interval) {
            // if the last notification was within the last 3s
            // just queue the message but play no sound
            return;
        }
        lastText = text;
        last_notification_posted = System.currentTimeMillis();

        int delayMillis = ("com.firebirdberlin.smartringcontrollerpro".equals(sbn.getPackageName())) ? 2000 : 60000;
        new Handler().postDelayed(dropLastMessage, delayMillis);

        if ((n.defaults & Notification.DEFAULT_SOUND) == Notification.DEFAULT_SOUND) {
            // do something--it was set
            // this is a notification with default sound
            Logger.i(TAG, "Default notification sound detected");
        } else if (notificationSound == null) {
            Logger.i(TAG, "Notification sound is null");
            // determine music volume
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (am.isMusicActive()) {
                int vol = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                Logger.d(TAG, String.format("media volume: %d", vol));
                if (vol > 0) {
                    SharedPreferences.Editor prefEditor = settings.edit();
                    prefEditor.putInt("lastMusicVolume", vol);
                    prefEditor.apply();
                }
            }
            return;
        }

        Intent i2 = new Intent(this, SetRingerService.class);
        // potentially add data to the intent
        i2.putExtra("PHONE_STATE", "Notification");
        if (notificationSound != null) {
            i2.putExtra("Sound", notificationSound.toString());
        }

        int interruptionFilter = getCurrentInterruptionFilter();
        Logger.d(TAG, "interruptionFilter = " + interruptionFilter);
        if (interruptionFilter == NotificationListenerService.INTERRUPTION_FILTER_ALL) {
            Logger.d(TAG, "starting SetRingerService");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(i2);
            } else {
                startService(i2);
            }
        }
    }

    Ranking getRanking(StatusBarNotification sbn) {
        String notificationKey = sbn.getKey();
        RankingMap rankingMap = getCurrentRanking();
        Ranking ranking = new Ranking();
        rankingMap.getRanking(notificationKey, ranking);
        return ranking;
    }

    int getImportance(StatusBarNotification sbn) {
        Ranking ranking = getRanking(sbn);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return ranking.getImportance();
        } else {
            return 2;
        }
    }

    Uri getNotificationSound(StatusBarNotification sbn) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Ranking ranking = getRanking(sbn);
            NotificationChannel channel = ranking.getChannel();
            if (channel != null) {
                return channel.getSound();
            }
        }
        Notification n = sbn.getNotification();
        return n.sound;
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
    }

    public static void queueMessage(String msg, Context context) {
        TTSService.queueMessage(msg, context);
    }

    public static void queueMessage(Notification n, Context context) {
        String text = getText(n, context);
        if (text == null) return;
        text = text.replace('#', ' '); // replace hashtags
        text = text.replaceAll("\\(?\\b(http://|www[.])[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]", " "); // remove urls
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

    private static String getTitle(Notification notification) {
        Bundle extras = notification.extras;
        CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
        return (title == null) ? "" : title.toString();
    }

    public static String getText(Notification notification, Context context) {
        String title = getTitle(notification);
        Bundle extras = notification.extras;
        CharSequence sequence = extras.getCharSequence(Notification.EXTRA_TEXT);
        String text = (sequence != null) ? sequence.toString() : null;
        if (text == null && notification.tickerText != null) {
            text = notification.tickerText.toString();
        }

        String result = title + " ";
        if (text != null) result += text + " ";
        if (result.isEmpty()) {
            return null;
        }
        String time = format_time(notification.when, context);
        result += time;
        return result;
    }

    private static String format_time(long value, Context context) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm a");
        if (DateFormat.is24HourFormat(context)) {
            dateFormat = new SimpleDateFormat("H:mm");
        }

        return dateFormat.format(new Date(value));
    }
}
