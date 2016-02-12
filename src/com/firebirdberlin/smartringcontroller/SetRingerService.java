package com.firebirdberlin.smartringcontrollerpro;

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
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import android.telephony.TelephonyManager;
import java.lang.Math;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
    private String PhoneState;
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

    @Override
    public void onCreate(){
//        Logger.setDebugging( true );
//        Logger.setDebugging( Utility.isDebuggable(this) );

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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // no action needed
        if (audiomanager.isSilent() || audiomanager.isVibration() || (settings.enabled == false)){
            stopSelf();
            return Service.START_NOT_STICKY;
        }


        if (running == true) {
            // Don't bother the service while already running.
            // The volume is beeing set right now.
            Logger.i(TAG,"Declined ! Service already running");
            return Service.START_NOT_STICKY;
        }
        running = true;

        // store the initial call state for later use
        initialPhoneState = telephone.getCallState();
        DeviceIsCovered = false;
        PhoneState = "None";
        targetVolume = settings.minRingerVolume;

        Bundle extras = intent.getExtras();
        if (extras != null) {
            PhoneState = intent.getStringExtra("PHONE_STATE"); // Ringing or notification
            if (PhoneState.equals("Notification")) {
                if (intent.hasExtra("Sound")) {
                    // store te sound URI
                    String sound = intent.getStringExtra("Sound");
                    soundUri = Uri.parse(sound);
                }
            }
        }

        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        registerListenerForSensor(proximitySensor);
        registerListenerForSensor(lightSensor);
        registerListenerForSensor(accelerometerSensor);

        if ( PhoneState.equals("Notification") ){
            audiomanager.mute();
            handler.postDelayed(startListening, waitMillis);
        } else { // phone call
            audiomanager.mute();
            handler.post(startListening);
        }

        return Service.START_NOT_STICKY;
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

        if (soundmeter != null){
            soundmeter.release();
            soundmeter = null;
        }

        if (wakelock != null && wakelock.isHeld()){
            wakelock.release();
        }
        Logger.d(TAG,"onDestroy()");
    }

    private void registerListenerForSensor(Sensor sensor) {
        if (sensor != null) {
            if (Build.VERSION.SDK_INT < 19) {
                sensorManager.registerListener(this, sensor, SENSOR_DELAY);
            } else {
                sensorManager.registerListener(this, sensor, SENSOR_DELAY, SENSOR_DELAY/2);
            }
        }
    }

    private Runnable startListening = new Runnable() {
        @Override
        public void run() {
            if (settings.controlRingerVolume == false) {
                handler.postDelayed(stopListening, measurementMillis/2);
                return;
            }

            boolean success = false;
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
                setVolume(soundmeter.getAmplitude());
                if ( soundmeter != null ) soundmeter.stop();
            }

            soundmeter.release();
            soundmeter = null;
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
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        //if (Build.VERSION.SDK_INT >= 20) {
            //return pm.isInteractive();
        //}
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
        return (! (settings.FlipAction == true
                   && isOnTable == DISPLAY_FACE_DOWN
                   && ambientLight < MAX_POCKET_BRIGHTNESS));
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

    @SuppressWarnings("deprecation")
    private void setVolume(double currentAmbientNoiseAmplitude) {

        sensorManager.unregisterListener(this);
        ambientLight /= (float) count_light_sensor; // mean value

        //audiomanager.restoreRingerMode();
        if ( shouldRing() ){// otherwise pass
            audiomanager.unmute();
        } else {
            // unmute the audiostream
            audiomanager.unmute();
            // but therefore mute the phone
            EnjoyTheSilenceService.start(this, settings.disconnectWhenFaceDown);
        }

        if ( PhoneState.equals("RINGING") ) { // expecting that a call is runnning
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

        if ( PhoneState.equals("Notification") ) {
            audiomanager.setRingerVolume(targetVolume);
            if ( shouldRing() && ! TTSService.shouldRead(false, this) ) {
                // the service is stopped on NotificationCompleted
                playNotification(this, soundUri);
            } else {
                // otherwise stop service after 600ms (wait for vibrator)
                handler.postDelayed(stopService, 600);
                return;
            }

        } else if ( PhoneState.equals("RINGING") ){
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

    /**
     * The listener that listens to events connected to incoming calls
     */
    private final SensorEventListener inCallActions =
    new SensorEventListener()  {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                float x_value = event.values[0];
                float z_value = event.values[2];
                // if acceleration in x and y direction is too strong, device is moving
                if (settings.ShakeAction == true) {
                    if (x_value < -12.) shake_left++;
                    if (x_value > 12.) shake_right++;

                    // shake to silence
                    if ( (shake_left >= 1 && shake_right >= 2) ||
                         (shake_left >= 2 && shake_right >= 1) ) {
                        handler.removeCallbacks(handleIncreasingRingerVolume);
                        audiomanager.setRingerVolume(settings.minRingerVolume); // lowest volume possible
                        vibrator.cancel();
                        shake_left = shake_right = 0;
                    }
                }

                if (settings.FlipAction) {
                    if (z_value > -10.3 && z_value < -9.3 ) { // display face down
                        handler.removeCallbacks(handleIncreasingRingerVolume);
                        audiomanager.mute();
                        vibrator.cancel();
                    }
                }

            } else if(event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                DeviceUnCovered = (event.values[0] > 0.f);

            } else if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
                if ((settings.PullOutAction) && isCovered()) {
                    // Attention do not choose the value to low,
                    // noise produces values up to 12 lux on my GNex
                    if(event.values[0] >= 15.f) { // uncovered
                        handler.removeCallbacks(handleIncreasingRingerVolume);
                        audiomanager.setRingerVolume(settings.minRingerVolume); // lowest volume possible
                        vibrator.cancel();
                    };
                }
            }
        }
    };

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
     * Note: isWiredHeadsetOn() is deprecated without a proper alternative. Use only to check if
     *       a wired headset is on.
     *
     * @param context: The aaplication context
     * @param uri: Uri of the sound to be played
     */
    @SuppressWarnings("deprecation")
    private void playNotification(Context context, Uri uri){

        if (uri == null) {
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        am.setMode(AudioManager.MODE_NORMAL);
        MediaPlayer mp = new MediaPlayer();
        try {
            mp.setDataSource(context, uri);
            if (am.isWiredHeadsetOn() || am.isBluetoothA2dpOn()) {
                mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            } else {
                mp.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
            }
            mp.prepare();
            mp.start();
            mp.setOnCompletionListener(NotificationCompleted);
        } catch(Exception e) {
            //exception caught in the end zone
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
