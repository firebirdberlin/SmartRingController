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
        Bundle extras = intent.getExtras();
        if (extras != null) {
            PhoneState = intent.getStringExtra("PHONE_STATE"); // Ringing or notification
            if (PhoneState.equals("Notification")){
                // store te sound URI
                if (intent.hasExtra("Sound")){
                    String sound = intent.getStringExtra("Sound");
                    soundUri = Uri.parse(sound);
                }
            }
        }

        //create instance of sensor manager and get system service to interact with Sensor
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        if (proximitySensor != null) {
            //ProximityMaximumRange = proximitySensor.getMaximumRange();
            if (Build.VERSION.SDK_INT < 19) {
                sensorManager.registerListener(this, proximitySensor, SENSOR_DELAY);
            } else {
                sensorManager.registerListener(this, proximitySensor, SENSOR_DELAY, SENSOR_DELAY/2);
            }
        }

        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (lightSensor != null) {
            if (Build.VERSION.SDK_INT < 19) {
                sensorManager.registerListener(this, lightSensor, SENSOR_DELAY);
            } else {
                sensorManager.registerListener(this, lightSensor, SENSOR_DELAY, SENSOR_DELAY/2);
            }
        }

        if (accelerometerSensor != null) {
            if (Build.VERSION.SDK_INT < 19) {
                sensorManager.registerListener(this, accelerometerSensor, SENSOR_DELAY);
            } else {
                sensorManager.registerListener(this, accelerometerSensor, SENSOR_DELAY,
                                               SENSOR_DELAY/2);
            }
        }

        // pleasent setting as initial value
        //audiomanager.setRingerVolume(minRingerVolume);

        if ( PhoneState.equals("Notification") ){
            audiomanager.mute();
            handler.postDelayed(startListening, waitMillis);
        } else { // phone call
            //handler.postDelayed(MuteAndListen,200);
            handler.post(startListening);
        }

        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        //TODO for communication return IBinder implementation
        return null;
    }

    @Override
    public void onDestroy(){

        unregisterReceiver(PhoneStateReceiver);
        sensorManager.unregisterListener(this);
        sensorManager.unregisterListener(inCallActions);

        if (soundmeter != null){
            //soundmeter.stop();
            soundmeter.release();
            soundmeter = null;
        }

        if (wakelock != null && wakelock.isHeld()){
            wakelock.release();
        }
        Logger.d(TAG,"onDestroy()");
    }

    private Runnable MuteAndListen = new Runnable() {
        @Override
        public void run() {
            audiomanager.mute();
            handler.post(startListening);
        }
    };

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
            // on error starting the recording
            if (error_on_microphone) {
                broadcastEvent("Failed to initialise the microphone! ");
            }

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

    private boolean isScreenOn() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        //if (Build.VERSION.SDK_INT >= 20) {
            //return pm.isInteractive();
        //}
        return pm.isScreenOn();
    }

    private boolean isCovered(){
        if ( settings.brokenProximitySensor ) {
            return ((DeviceIsCovered == true) || (ambientLight < MAX_POCKET_BRIGHTNESS));
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

    private void setVolume(double currentAmbientNoiseAmplitude) {

        sensorManager.unregisterListener(this);
        ambientLight /= (float) count_light_sensor; // mean value

        //audiomanager.restoreRingerMode();
        if ( shouldRing() ){// otherwise pass
            audiomanager.unmute();// sound is unmuted onDestroy
        } else {
            // mute phone until it is flipped again
            EnjoyTheSilenceService.start(this);
        }

        if ( PhoneState.equals("RINGING") ){ // expecting that a call is runnning
            int callState = telephone.getCallState();
            // call has stopped, while we were waiting for measurements
            if (callState != TelephonyManager.CALL_STATE_RINGING){
                handler.post(stopService);
                return;
            }
        }

        boolean vibratorON = handleVibration();

        int newRingerVolume = audiomanager.getRingerVolume();
        if (currentAmbientNoiseAmplitude > 0.) { // could not detect ambient noise
            newRingerVolume = settings.getRingerVolume(currentAmbientNoiseAmplitude ,
                                                       isCovered());
            audiomanager.setRingerVolume(newRingerVolume);

        }

        String msg = String.valueOf(currentAmbientNoiseAmplitude) + " => " + String.valueOf(newRingerVolume);
        msg += (DeviceIsCovered) ? " -" : " +";
        msg += (vibratorON) ? " | vibrate" : "";

        if (isOnTable == DISPLAY_FACE_DOWN ) msg += " | face DOWN";
        else if (isOnTable == DISPLAY_FACE_UP ) msg += " | face UP";

        msg += " | " + String.valueOf(ambientLight);
        broadcastEvent(msg);

        if ( PhoneState.equals("Notification") ){

            if ( shouldRing() && ! TTSService.shouldRead(false, this) ){
                // service is stopped on NotificationCompleted
                playNotification(this, soundUri);
            } else {
                // otherwise stop service in 600ms (wait for vibrator)
                handler.postDelayed(stopService, 600);
                return;
            }

        } else if ( PhoneState.equals("RINGING") ){
            // The service will be stopped on change of the in-call state
            registerInCallSensorListeners();
        } else {
            // could be a test of the service
            if (vibratorON){
                handler.postDelayed(stopService, 600);
            }else{
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
                    if ((shake_left >= 1 && shake_right >= 2)
                            || (shake_left >= 2 && shake_right >= 1)){
                        audiomanager.setRingerVolume(1); // lowest volume possible
                        vibrator.cancel();
                        shake_left = shake_right = 0;
                    }
                }

                if (settings.FlipAction == true) {
                    if (z_value > -10.3 && z_value < -9.3 ){ // display face down
                        audiomanager.mute();
                        vibrator.cancel();
                    }
                }
            } else if(event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                DeviceUnCovered = (event.values[0] > 0.f);

            } else if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
                if ((settings.PullOutAction == true) && isCovered()) {
                    // Attention do not choose the value to low,
                    // noise produces values up to 12 lux on my GNex
                    if(event.values[0] >= 15.f){// uncovered
                        audiomanager.setRingerVolume(1); // lowest volume possible
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
     * sound if played on the music stream. In order to save the
     * environment from sounds.
     * @param context: The aaplication context
     * @param uri: Uri of the sound to be played
     */
    private void playNotification(Context context, Uri uri){

        if (uri == null) {
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        AudioManager am=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
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

    private void broadcastEvent(String msg){
        Intent i = new Intent("com.firebirdberlin.smartringcontroller.NOTIFICATION_LISTENER");
        i.putExtra("notification_event", msg);
        sendBroadcast(i);
    }
}
