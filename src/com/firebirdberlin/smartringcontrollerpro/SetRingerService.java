package com.firebirdberlin.smartringcontrollerpro;

import android.Manifest;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.IOException;
import java.util.List;


public class SetRingerService extends Service implements SensorEventListener {
    private static String TAG = SmartRingController.TAG + ".SetRingerService";
    private final Handler handler = new Handler();
    private Settings settings = null;
    private SoundMeter soundmeter = null;
    private mAudioManager audiomanager = null;

    private BatteryStats battery;

    private SensorManager sensorManager = null;
    private Vibrator vibrator = null;
    private Sensor accelerometerSensor = null;
    private Sensor proximitySensor = null;
    private Sensor lightSensor = null;
    private TelephonyManager telephone;


    private boolean running = false;
    private boolean error_on_microphone = false;
    private boolean DeviceIsCovered = false;
    private int targetVolume = 1;
    private float ambientLight = 0;//SensorManager.LIGHT_SUNLIGHT_MAX;
    private String phoneState;
    private int initialPhoneState = TelephonyManager.CALL_STATE_IDLE;
    private long vibrationEndTime = 0;
    private Uri soundUri = null;

    PowerManager.WakeLock wakelock;

    private static int NOT_ON_TABLE = 0;
    private static int DISPLAY_FACE_UP   = 1;
    private static int DISPLAY_FACE_DOWN = 2;
    private int isOnTable = NOT_ON_TABLE;

    private int count_proximity_sensor = 0;
    private int count_acceleration_sensor = 0;
    private int count_light_sensor = 0;

    private static int waitMillis = 3000; // ms to wait before measuring ambient noise
    private static int measurementMillis = 800; // 800 ms is the minimum needed for Android 4.4.4
    private static int SENSOR_DELAY = 50000; // us = 50 ms
    private static float MAX_POCKET_BRIGHTNESS = 10.f; // proximity sensor fix
    private static final int INCREASING_RINGER_VOLUME_STEP_DELAY = 3000; // ms

    /**
     * The listener that listens to events connected to incoming calls
     */
    private final SensorEventListener inCallActions =
            new SensorEventListener() {
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }

                public void onSensorChanged(SensorEvent event) {
                    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                        float x_value = event.values[0];
                        float z_value = event.values[2];
                        // if acceleration in x and y direction is too strong, device is moving
                        if (settings.ShakeAction == true) {
                            if (x_value < -12.) shake_left++;
                            if (x_value > 12.) shake_right++;

                            // shake to silence
                            if ((shake_left >= 1 && shake_right >= 2) ||
                                    (shake_left >= 2 && shake_right >= 1)) {
                                handler.removeCallbacks(handleIncreasingRingerVolume);
                                audiomanager.setRingerVolume(settings.minRingerVolume); // lowest volume possible
                                vibrator.cancel();
                                shake_left = shake_right = 0;
                            }
                        }

                        if (settings.FlipAction) {
                            if (z_value > -10.3 && z_value < -9.3) { // display face down
                                handler.removeCallbacks(handleIncreasingRingerVolume);
                                audiomanager.mute();
                                vibrator.cancel();
                            }
                        }

                    } else if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                        DeviceUnCovered = (event.values[0] > 0.f);

                    } else if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
                        if ((settings.PullOutAction) && isCovered()) {
                            // Attention do not choose the value to low,
                            // noise produces values up to 12 lux on my GNex
                            if (event.values[0] >= 15.f) { // uncovered
                                handler.removeCallbacks(handleIncreasingRingerVolume);
                                audiomanager.setRingerVolume(settings.minRingerVolume); // lowest volume possible
                                vibrator.cancel();
                            }
                        }
                    }
                }
            };

    @Override
    public void onCreate(){
        callStartForeground();
//        Logger.setDebugging( true );
        Logger.setDebugging( Utility.isDebuggable(this) );

        IntentFilter filter = new IntentFilter();
        //filter.addAction("android.provider.Telephony.SMS_RECEIVED");
        filter.addAction(android.telephony.TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        registerReceiver(PhoneStateReceiver, filter);

        battery = new BatteryStats(this);
        soundmeter = new SoundMeter();
        settings = new Settings(this);
        audiomanager = new mAudioManager(this);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if( sensorList.size() > 0 ){
            accelerometerSensor = sensorList.get(0);
        }

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,TAG);
        //wakelock = pm.newWakeLock(32,TAG); // Proximity Wakelock
        wakelock.acquire();

        telephone = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    }

    void callStartForeground() {
        Utility.createNotificationChannels(getApplicationContext());
        Notification note = buildNotification(getString(R.string.notificationChannelNameRingerService));
        startForeground(SmartRingController.NOTIFICATION_ID_TTS, note);
    }

    private Notification buildNotification(String message) {
        NotificationCompat.Builder noteBuilder =
                Utility.buildNotification(this, SmartRingController.NOTIFICATION_CHANNEL_ID_RINGER_SERVICE)
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(message)
                        .setSmallIcon(R.drawable.ic_phone_30dp)
                        .setPriority(NotificationCompat.PRIORITY_MIN);

        Notification note = noteBuilder.build();

        note.flags |= Notification.FLAG_NO_CLEAR;
        note.flags |= Notification.FLAG_FOREGROUND_SERVICE;

        return note;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy(){

        handler.removeCallbacks(handleIncreasingRingerVolume);
        unregisterReceiver(PhoneStateReceiver);
        sensorManager.unregisterListener(this);
        sensorManager.unregisterListener(inCallActions);

        releaseSoundmeter();

        if (wakelock != null && wakelock.isHeld()){
            wakelock.release();
        }
        Logger.d(TAG,"onDestroy()");
    }

    private void releaseSoundmeter() {
        if (soundmeter != null){
            soundmeter.release();
            soundmeter = null;
        }
    }

    private void registerListenerForSensor(Sensor sensor) {
        if (sensor != null) {
            sensorManager.registerListener(this, sensor, SENSOR_DELAY, SENSOR_DELAY/2);
        }
    }

    private Runnable startListening = new Runnable() {
        @Override
        public void run() {
            if (! Utility.hasPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO)
                || !settings.controlRingerVolume) {
                handler.postDelayed(stopListening, measurementMillis/2);
                return;
            }

            boolean success;
            try {
                success = soundmeter.start();
            } catch (Exception e){
                success = false;
            }

            error_on_microphone = !success;
            handler.postDelayed(stopListening, measurementMillis);
        }
    };

    private Runnable stopListening = new Runnable() {
        @Override
        public void run() {

            if (error_on_microphone || settings.controlRingerVolume == false) {
                setVolume(0.);
            } else {
                if ( soundmeter != null ) {
                    setVolume(soundmeter.getAmplitude());
                    soundmeter.stop();
                }
            }

            releaseSoundmeter();
            System.gc();
        }
    };


    private Runnable stopService = new Runnable() {
        @Override
        public void run() {
            // don't stop when ringing => vibration may be handled
            if (telephone.getCallState() != TelephonyManager.CALL_STATE_RINGING){
                vibrator.cancel();
                // to be sure we unmute the streams
                audiomanager.unmute();
                if (settings.controlRingerVolume) {
                    // and restore a silent setting, so that bursts are not too loud
                    audiomanager.setRingerVolume(settings.minRingerVolume);
                }
                stopSelf();
            }
        }
    };

    private Runnable handleIncreasingRingerVolume = new Runnable() {
        @Override
        public void run() {
            int currentVolume = audiomanager.getRingerVolume(); // current value
            if (currentVolume < targetVolume) {
                audiomanager.setRingerVolume(currentVolume + 1);
                handler.postDelayed(handleIncreasingRingerVolume, INCREASING_RINGER_VOLUME_STEP_DELAY);
            }
            currentVolume = audiomanager.getRingerVolume(); // current value
            Logger.i(TAG, "current ringer volume: " + String.valueOf(currentVolume) +
                          " / " + String.valueOf(targetVolume));
        }
    };

    private boolean isScreenOn() {
        if (Build.VERSION.SDK_INT >= 20) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            return pm.isInteractive();
        }
        return deprecated_isScreenOn();
    }

    @SuppressWarnings("deprecation")
    private boolean deprecated_isScreenOn() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        return pm.isScreenOn();
    }

    private boolean isCovered(){
        if ( settings.brokenProximitySensor ) {
            return ( DeviceIsCovered || (ambientLight < MAX_POCKET_BRIGHTNESS));
        }

        return DeviceIsCovered;
    }

    private boolean shouldVibrate(){
        return ((settings.handleVibration == true)
                && isCovered()
                && (! isScreenOn() )
                && (isOnTable == NOT_ON_TABLE)
                && (! battery.isCharging() )
                && (telephone.getCallState() != TelephonyManager.CALL_STATE_OFFHOOK));
    }

    private boolean shouldRing(){
        Log.i(TAG, "shouldRing()");
        Log.i(TAG, " settings.FlipAction = " + String.valueOf(settings.FlipAction));
        Log.i(TAG, " isOnTable = " + String.valueOf(isOnTable));
        Log.i(TAG, " ambientLight = " + String.valueOf(ambientLight));
        Log.i(TAG, " DeviceIsCovered = " + String.valueOf(DeviceIsCovered));
        return (! (settings.FlipAction == true
                   && isOnTable == DISPLAY_FACE_DOWN
                   && isCovered()));
                   //&& ambientLight < MAX_POCKET_BRIGHTNESS));
    }

    private void vibrateForNotification(){
        long data[] = {100, 100, 100, 100};
        vibrator.vibrate(data, -1);
        vibrationEndTime = System.currentTimeMillis() + 600;
    }

    private boolean handleVibration(){
        if ( shouldVibrate() ) {
            long data[] = {100, 600, 100, 100, 100, 100, 100, 600, 600};
            if (telephone.getCallState() == TelephonyManager.CALL_STATE_RINGING) { // phone call
                vibrator.vibrate(data, 0);
            } else { // notification or test
                vibrateForNotification();
            }
            return true;
        }
        return false;
    }

    private void setVolume(double currentAmbientNoiseAmplitude) {

        sensorManager.unregisterListener(this);
        ambientLight /= (float) count_light_sensor; // mean value

        //audiomanager.restoreRingerMode();
        // unmute the audiostream
        audiomanager.unmute();
        if (! shouldRing() ) {
            Log.i(TAG, "Hush ... silence.");
            // but mute the device by another service
            EnjoyTheSilenceService.start(this, settings.disconnectWhenFaceDown);
        }

        if ( phoneState.equals("RINGING") ) { // expecting that a call is runnning
            int callState = telephone.getCallState();
            // call has stopped, while we were waiting for measurements
            if (callState != TelephonyManager.CALL_STATE_RINGING) {
                handler.post(stopService);
                return;
            }
        }

        boolean vibratorON = handleVibration();

        targetVolume = audiomanager.getRingerVolume(); // current value
        if (currentAmbientNoiseAmplitude > 0.) {
            targetVolume = settings.getRingerVolume(currentAmbientNoiseAmplitude,
                                                    isCovered(),
                                                    audiomanager.isWiredHeadsetOn());
        }

        if ( phoneState.equals("Notification") ) {
            audiomanager.setRingerVolume(targetVolume);
            if ( shouldRing() && ! TTSService.shouldRead(false, this) ) {
                // the service is stopped on NotificationCompleted
                playNotification(soundUri);
            } else {
                // otherwise stop service after 600ms (wait for vibrator)
                handler.postDelayed(stopService, 600);
                return;
            }

        } else if ( phoneState.equals("RINGING") ){
            if ( settings.increasingRingerVolume ) {
                handler.postDelayed(handleIncreasingRingerVolume, INCREASING_RINGER_VOLUME_STEP_DELAY);
            } else {
                audiomanager.setRingerVolume(targetVolume);
            }
            registerInCallSensorListeners();
            // The service will be stopped on change of the in-call state
        } else {
            audiomanager.setRingerVolume(targetVolume);
            // could be a test of the service
            if (vibratorON){
                handler.postDelayed(stopService, 600);
            } else {
                handler.post(stopService);
            }
        }
    }

    private void registerInCallSensorListeners(){
        sensorManager.registerListener(inCallActions, proximitySensor    , SENSOR_DELAY);
        sensorManager.registerListener(inCallActions, accelerometerSensor, SENSOR_DELAY);
        sensorManager.registerListener(inCallActions, lightSensor        , SENSOR_DELAY);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // called when sensor value have changed
    @Override
    public void onSensorChanged(SensorEvent event) {
        // The Proximity sensor returns a single value either 0 or 5 (also 1 depends on Sensor manufacturer).
        // 0 for near and 5 for far
        if(event.sensor.getType() == Sensor.TYPE_PROXIMITY){
            count_proximity_sensor++;
            DeviceIsCovered = (event.values[0] == 0);
        } else if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            count_light_sensor++;
            ambientLight += event.values[0]; // simply log the value
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            count_acceleration_sensor++;
            isOnTable = NOT_ON_TABLE;
            // if acceleration in x and y direction is too strong, device is moving
            if (Math.abs(event.values[0]) > 1.) return;
            if (Math.abs(event.values[1]) > 1.) return;

            float z_value = event.values[2];
            // acceleration in z direction must be g
            if (z_value > -10.3 && z_value < -9.3 ){
                isOnTable = DISPLAY_FACE_DOWN;
            } else
            if (z_value > 9.3 && z_value < 10.3 ){
                isOnTable = DISPLAY_FACE_UP;
            } else{
                isOnTable = NOT_ON_TABLE;
            }
            }
    }

    private boolean DeviceUnCovered = false;
    private int shake_left = 0;
    private int shake_right = 0;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() ( running = " + String.valueOf(running) + " )");
        callStartForeground();
        // no action needed
        if (audiomanager.isSilent() || audiomanager.isVibration() || (settings.enabled == false)) {
            stopSelf();
            return Service.START_NOT_STICKY;
        }

        // force to stop the service after 3 minutes
        handler.postDelayed(stopService, 180000);

        if (running) {
            // Don't bother the service while already running.
            // The volume is beeing set right now.
            Logger.i(TAG, "Declined ! Service already running");
            return Service.START_NOT_STICKY;
        }
        running = true;

        // store the initial call state for later use
        initialPhoneState = telephone.getCallState();
        DeviceIsCovered = false;
        phoneState = "None";
        targetVolume = settings.minRingerVolume;

        Bundle extras = intent.getExtras();
        if (extras != null) {
            phoneState = intent.getStringExtra("PHONE_STATE"); // Ringing or notification
            if (phoneState.equals("Notification") && intent.hasExtra("Sound")) {
                // store the sound URI
                String sound = intent.getStringExtra("Sound");
                soundUri = Uri.parse(sound);
                Logger.i(TAG, "The notification ships this sound uri " + soundUri);
            }
        }

        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        registerListenerForSensor(proximitySensor);
        registerListenerForSensor(lightSensor);
        registerListenerForSensor(accelerometerSensor);

        audiomanager.mute();
        if (phoneState.equals("Notification")) {
            handler.postDelayed(startListening, waitMillis);
        } else { // phone call
            handler.post(startListening);
        }

        return Service.START_NOT_STICKY;
    }

    private final BroadcastReceiver PhoneStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                // was idle but now is ringing (probably service is started for notification)
                if (initialPhoneState == TelephonyManager.CALL_STATE_IDLE) {
                    handleVibration();
                }
            } else { // OFFHOOK or IDLE
                vibrator.cancel();
                handler.postDelayed(stopService, 200);
            }
        }
    };

    /**
     * Plays a notification sound. If a wired headset is present the
     * sound if played on the music stream in order to save the
     * environment from sounds.
     *
     * @param uri: Uri of the sound to be played
     */
    private void playNotification(Uri uri){

        if (uri == null || uri.equals(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI) ) {
            Logger.w(TAG, "default sound uri detected ! Falling back to own setting");
            uri = Uri.parse(settings.defaultNotificationUri);
        }

        Logger.i(TAG, "Playing notification " + uri.toString());

        audiomanager.setMode(AudioManager.MODE_NORMAL);
        MediaPlayer mp = new MediaPlayer();
        try {
            mp.setDataSource(this, uri);
        } catch (SecurityException| IOException e){
            try {
                mp.setDataSource(this, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);
            } catch (IOException ignored) {
                return;
            }
        }

       try {
            if (audiomanager.isWiredHeadsetOn() || audiomanager.isBluetoothA2dpOn()) {
                mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            } else {
                mp.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
            }
            mp.prepare();
            mp.start();
            mp.setOnCompletionListener(NotificationCompleted);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private final OnCompletionListener NotificationCompleted = new OnCompletionListener() {
        public void onCompletion(MediaPlayer mp) {
            Logger.i(TAG,"notification sound completed");
            long now = System.currentTimeMillis();
            if (now >= vibrationEndTime){
                handler.post(stopService);
            } else {
                handler.postDelayed(stopService, vibrationEndTime - now);
            }
        }
    };
}
