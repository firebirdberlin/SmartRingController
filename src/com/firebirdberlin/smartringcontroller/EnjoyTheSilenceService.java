package com.firebirdberlin.smartringcontrollerpro;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.telephony.TelephonyManager;

import java.util.List;
import java.lang.Math;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

public class EnjoyTheSilenceService extends Service implements SensorEventListener {
    private static String TAG = SmartRingController.TAG + ".EnjoyTheSilenceService";
    private Handler handler;
    private boolean running = false;

    private SensorManager sensorManager;
    private WifiManager wifiManager;
    private Sensor accelerometerSensor = null;

    PowerManager.WakeLock wakelock;
    private PowerManager pm;

    private mAudioManager audiomanager;

    private boolean debug = false;
    private boolean systemMobileDataSetting = false;
    private boolean systemWifiOn = false;
    private int systemRingerMode = 0;


    private boolean accelerometerPresent = false;
    private static int NOT_ON_TABLE = 0;
    private static int DISPLAY_FACE_UP   = 1;
    private static int DISPLAY_FACE_DOWN = 2;
    private int isOnTable = NOT_ON_TABLE;

    private static int SENSOR_DELAY          = 5000000;     // us = 5000 ms
    private boolean WAIT_UNTIL_FLIPPED = false;

    @Override
    public void onCreate(){
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,TAG);
        wakelock.acquire();

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        wifiManager   = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if(sensorList.size() > 0){
            accelerometerPresent = true;
            accelerometerSensor = sensorList.get(0);
        }else{
            accelerometerPresent = false;
        }

        audiomanager = new mAudioManager(this);

        handler      = new Handler();

//        SharedPreferences settings = getSharedPreferences(
//                    NightDreamSettingsActivity.PREFS_KEY, 0);
//        int sensitivity   = settings.getInt("NoiseSensitivity", 1);

        if (debug){
            Log.d(TAG,"onCreate() called.");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (debug){
            Log.d(TAG,"onStartCommand() called.");
        }

        Bundle extras = intent.getExtras();
        if (extras != null) {
            String action = intent.getStringExtra("action");
            if (action.equals("unmute")){
                systemWifiOn            =  intent.getBooleanExtra("systemWifiOn", false);
                systemMobileDataSetting =  intent.getBooleanExtra("systemMobileDataOn", false);
                systemRingerMode        =  intent.getIntExtra("systemRingerMode", AudioManager.RINGER_MODE_NORMAL);
                restoreSystemSettings();
                stopSelf();
                return START_NOT_STICKY;
            }
            WAIT_UNTIL_FLIPPED = intent.getBooleanExtra("WAIT_UNTIL_FLIPPED", false);
        }


        if (running==true){
            Log.d(TAG," ... already running.");
            return Service.START_STICKY;
        }
        running = true;

        getSystemSettings();

        audiomanager.setRingerModeSilent(); // silence
        if (systemMobileDataSetting) setMobileDataEnabled(false);
        disableWifi();

//        enableMobileDataConnection(false);

        Intent IntentUnmute = new Intent(this, EnjoyTheSilenceService.class);
        IntentUnmute.putExtra("action", "unmute");
        IntentUnmute.putExtra("systemWifiOn", systemWifiOn);
        IntentUnmute.putExtra("systemRingerMode", systemRingerMode);
        IntentUnmute.putExtra("systemMobileDataOn", systemMobileDataSetting);
        PendingIntent pIntentUnmute = PendingIntent.getService(this, 0, IntentUnmute, 0);

//        Intent i=new Intent(this, SmartRingController.class);
//        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
//                 Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        PendingIntent pi=PendingIntent.getActivity(this, 0, i, 0);

        // build notification using Notification Builder
        Notification note  = new Notification.Builder(this)
            .setContentTitle(this.getString(R.string.titleSilenceMode))
            .setContentText(this.getString(R.string.msgRestoreRingerMode))
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pIntentUnmute)
            .setAutoCancel(true).build();
//        .addAction(R.drawable.ic_launcher, "unmute", pIntentUnmute).build();
////        .addAction(R.drawable.icon, "More", pIntent)
////        .addAction(R.drawable.icon, "And more", pIntent).build();
        note.flags|=Notification.FLAG_NO_CLEAR;

        if (WAIT_UNTIL_FLIPPED){
            note.setLatestEventInfo(this, "Smart Ring Controller",
                                this.getString(R.string.msgRestoreRingerMode),
                                pIntentUnmute);
            note.flags|=Notification.FLAG_FOREGROUND_SERVICE;
            startForeground(1337, note);
            handler.postDelayed(registerListener,5000);
        } else {
            NotificationManager notificationManager =
              (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            notificationManager.notify(1, note);
            stopSelf();
        }
        return Service.START_STICKY;
        //return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        //TODO for communication return IBinder implementation
        return null;
    }

    @Override
    public void onDestroy(){
        if (debug){
            Log.d(TAG,"onDestroy() called.");
        }
        handler.removeCallbacks(registerListener);
        audiomanager = null;

        if (wakelock.isHeld()){
            wakelock.release();
        }
    }

    public static void start(Context context) {
        Intent i = new Intent(context, EnjoyTheSilenceService.class);
        final SharedPreferences settings = context.getSharedPreferences(SmartRingController.PREFS_KEY, 0);
        boolean keepalive = settings.getBoolean("Ctrl.AutoReactivateRingerMode", false);

        i.putExtra("action", "mute");
        i.putExtra("WAIT_UNTIL_FLIPPED", keepalive);
        context.startService(i);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // called when sensor value have changed
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
            isOnTable = NOT_ON_TABLE;
            // if acceleration in x and y direction is too strong, device is moving
//            if (Math.abs(event.values[0]) > 1.) return;
//            if (Math.abs(event.values[1]) > 1.) return;

            float z_value = event.values[2];
            // acceleration in z direction must be g
            if (z_value > -10.3 && z_value < -9.3 ){
                isOnTable = DISPLAY_FACE_DOWN;
                Log.d(TAG,"DISPLAY_FACE_DOWN");
            }else
            if (z_value > 9.3 && z_value < 10.3 ){
                isOnTable = DISPLAY_FACE_UP;
                Log.d(TAG,"DISPLAY_FACE_UP");
            }
            else{
                isOnTable = NOT_ON_TABLE;
                Log.d(TAG,"NOT_ON_TABLE");
            }


            if (isOnTable != DISPLAY_FACE_DOWN){
                if (audiomanager != null ){
                    audiomanager.restoreRingerMode();
                }
                setMobileDataEnabled(systemMobileDataSetting);
                restoreWifi();
                sensorManager.unregisterListener(this);
                stopSelf();
            }

            sensorManager.unregisterListener(this);
            handler.postDelayed(registerListener, 20000); // 20 s seem to be fairly enough
        }
    }

    private void register(){
        if (accelerometerPresent){
            if (Build.VERSION.SDK_INT < 19)
                sensorManager.registerListener(this, accelerometerSensor, SENSOR_DELAY);
            else
                sensorManager.registerListener(this, accelerometerSensor, SENSOR_DELAY, SENSOR_DELAY/2);
        } else {
            audiomanager.restoreRingerMode();
            setMobileDataEnabled(systemMobileDataSetting);
            restoreWifi();
            stopSelf();
        }
    }

    private Runnable registerListener = new Runnable() {
        @Override
        public void run() {
            register();
        }
    };

    private void unmute(){
        if (audiomanager != null ){
            audiomanager.restoreRingerMode();
        }
        setMobileDataEnabled(systemMobileDataSetting);
        restoreWifi();
    }

    private void getSystemSettings(){
        systemMobileDataSetting = isMobileDataEnabled();
        systemWifiOn            = wifiManager.isWifiEnabled();
        systemRingerMode        = audiomanager.getRingerMode();

    }

    private void restoreSystemSettings(){
        if (audiomanager != null ){
            audiomanager.setRingerMode(systemRingerMode);
        }

        if (wifiManager != null){
            wifiManager.setWifiEnabled(systemWifiOn);
        }

        setMobileDataEnabled(systemMobileDataSetting);
    }

    private void setMobileDataEnabled(boolean enabled) {
        try{
           final ConnectivityManager conman = (ConnectivityManager)  getSystemService(Context.CONNECTIVITY_SERVICE);
           final Class conmanClass = Class.forName(conman.getClass().getName());
           final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
           iConnectivityManagerField.setAccessible(true);
           final Object iConnectivityManager = iConnectivityManagerField.get(conman);
           final Class iConnectivityManagerClass =  Class.forName(iConnectivityManager.getClass().getName());
           final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
           setMobileDataEnabledMethod.setAccessible(true);

           setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
        } catch (Exception e){
        }
    }

    private boolean isMobileDataEnabled() {
        boolean is = false;
        try{
            final ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            Class cmClass = Class.forName(cm.getClass().getName());
            Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
            method.setAccessible(true); // Make the method callable
            // get the setting for "mobile data"
            is = (Boolean) method.invoke(cm);
        } catch (Exception e){
        }
        return is;
    }

    private void disableWifi(){
        systemWifiOn = wifiManager.isWifiEnabled();
        if (systemWifiOn){
            wifiManager.setWifiEnabled(false);
        }
    }

    private void restoreWifi(){
        if (systemWifiOn){
            wifiManager.setWifiEnabled(true);
        }
    }
}
