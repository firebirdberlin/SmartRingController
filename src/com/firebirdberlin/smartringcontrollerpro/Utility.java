package com.firebirdberlin.smartringcontrollerpro;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class Utility {

    private static final String TAG = "Utility";

    public static void playNotification(Context context){
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(context, notification);
            r.play();
            //Thread.sleep(300);
            //r.stop();
        } catch (Exception e) {}
    }

    public void PlayAlarmSound(Context context) {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            Ringtone r = RingtoneManager.getRingtone(context, notification);
            r.play();
        } catch (Exception e) {}
    }

    public static boolean isDebuggable(Context context){
        return ( 0 != ( context.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE ) );
    }

    public static boolean isPackageInstalled(Context context, String targetPackage){
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(targetPackage, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }

    public static int getCallState(Context context) {
        TelephonyManager telephone = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return telephone.getCallState();
    }

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager notificationManager =
                context.getSystemService(NotificationManager.class);

        NotificationChannel channelTTS = prepareNotificationChannel(
                context,
                SmartRingController.NOTIFICATION_CHANNEL_ID_TTS,
                R.string.notificationChannelNameTTS,
                R.string.notificationChannelDescTTS,
                NotificationManager.IMPORTANCE_DEFAULT
        );
        notificationManager.createNotificationChannel(channelTTS);
        NotificationChannel channelRingerService = prepareNotificationChannel(
                context,
                SmartRingController.NOTIFICATION_CHANNEL_ID_RINGER_SERVICE,
                R.string.notificationChannelNameRingerService,
                R.string.notificationChannelDescRingerService,
                NotificationManager.IMPORTANCE_MIN
        );
        notificationManager.createNotificationChannel(channelRingerService);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static NotificationChannel prepareNotificationChannel(
            Context context, String channelName, int idName, int idDesc, int importance) {
        String name = context.getString(idName);
        String description = context.getString(idDesc);
        NotificationChannel mChannel = new NotificationChannel(channelName, name, importance);
        mChannel.setDescription(description);
        mChannel.enableLights(false);
        mChannel.enableVibration(false);
        mChannel.setSound(null, null);
        mChannel.setShowBadge(false);
        return mChannel;
    }

    public static NotificationCompat.Builder buildNotification(Context context, String channel_id) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return new NotificationCompat.Builder(context);
        } else {
            return new NotificationCompat.Builder(context, channel_id);
        }
    }

    static boolean hasPermission(Context context, String permission) {
        return (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED);
    }

    static String getContactNameFromNumber(Context context, String number, ContentResolver contentResolver) {
        if (!Utility.hasPermission(context, Manifest.permission.READ_CONTACTS)) {
            return number;
        }
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

        Cursor cursor = null;
        try {
            cursor = contentResolver.query(uri, new String[]{ ContactsContract.PhoneLookup.DISPLAY_NAME }, null, null, null);

            if (cursor.isAfterLast()) {
                // If nothing was found, return the number....
                Logger.w(TAG, "Unable to look up incoming number in contacts");
                return number;
            }

            // ...otherwise return the first entry.
            cursor.moveToFirst();
            int nameFieldColumnIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
            String contactName = cursor.getString(nameFieldColumnIndex);
            return contactName;
        } finally {
            cursor.close();
        }
    }
}

