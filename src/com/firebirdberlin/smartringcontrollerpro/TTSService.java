package com.firebirdberlin.smartringcontrollerpro;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.telephony.TelephonyManager;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

public class TTSService extends Service {

    private static final String BLUETOOTH_TIMEOUT_EXTRA = "com.firebirdberlin.smartringcontrollerpro.BLUETOOTH_TIMEOUT";
    private static final String QUEUE_MESSAGE_EXTRA = "com.firebirdberlin.smartringcontrollerpro.QUEUE_MESSAGE";
    private static final String START_READING_EXTRA = "com.firebirdberlin.smartringcontrollerpro.START_READING";
    private static final String STOP_READING_EXTRA = "com.firebirdberlin.smartringcontrollerpro.STOP_READING";

    private static final String TAG = SmartRingController.TAG + "." + TTSService.class.getSimpleName();

    private SensorManager sensorManager;
    private Sensor accelerometerSensor = null;
    private boolean accelerometerPresent;

    private final LocalBinder binder = new LocalBinder();
    private final Queue<String> messageQueue = new LinkedList<>();
    private TimerTask bluetoothTimerTask;

    private TextToSpeech tts;

    private AudioManager audioManager;
    private int systemVolume;

    public static final int READING_AUDIO_STREAM = AudioManager.STREAM_VOICE_CALL;

    public class LocalBinder extends Binder {
        TTSService getService() {
            return TTSService.this;
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return binder;
    }

    public static boolean shouldRead(boolean canUseSco, Context context) {
        final SharedPreferences settings = context.getSharedPreferences(SmartRingController.PREFS_KEY, 0);
        boolean enabled = settings.getBoolean("enabled", false);
        boolean tts_enabled = settings.getBoolean("TTS.enabled", false);
        String ttsMode = settings.getString("TTSmode", SmartRingController.TTS_MODE_HEADPHONES);

        Logger.w(TAG, "ttsmode \"" + ttsMode + "\"");

        if (enabled && tts_enabled) {
            if (Utility.getCallState(context) == TelephonyManager.CALL_STATE_OFFHOOK) {
                // don't speak, when in call
                return false;
            }


            if (ttsMode != null && ttsMode.equals(SmartRingController.TTS_MODE_ALWAYS)) {
                return true;
            }

            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

            if (canUseSco && audioManager.isBluetoothScoAvailableOffCall()) {
                return true;
            }

            return audioManager.isBluetoothA2dpOn() || mAudioManager.isWiredHeadsetOn(context);
        }

        return false;
    }

    public static void queueMessage(String message, Context context) {
        Logger.i(TAG, "Trying to queue message: " + message);
        if (!shouldRead(false, context)) return;
        Logger.i(TAG, " > success, message: " + message);
        sendIntent(TTSService.QUEUE_MESSAGE_EXTRA, message, context);
    }

    private static void sendIntent(String extraName, String extraValue, Context context) {
        Intent readIntent = new Intent(context, TTSService.class);
        readIntent.putExtra(extraName, extraValue);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(readIntent);
        } else {
            context.startService(readIntent);
        }
    }

    @Override
    public void onCreate() {

        Notification note = buildNotification(getString(R.string.notificationChannelNameTTS));
        startForeground(SmartRingController.NOTIFICATION_ID_TTS, note);

        audioManager  = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        if( sensorList.size() > 0 ){
            accelerometerPresent = true;
            accelerometerSensor = sensorList.get(0);
        } else {
            accelerometerPresent = false;
        }
    }

    private Notification buildNotification(String message) {
        NotificationCompat.Builder noteBuilder =
                Utility.buildNotification(this, SmartRingController.NOTIFICATION_CHANNEL_ID_TTS)
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(message)
                        .setSmallIcon(R.drawable.ic_voice)
                        .setPriority(NotificationCompat.PRIORITY_MIN);

        Notification note = noteBuilder.build();

        note.flags |= Notification.FLAG_NO_CLEAR;
        note.flags |= Notification.FLAG_FOREGROUND_SERVICE;

        return note;
    }

    private void updateNotification(String message) {
        Notification notification = buildNotification(message);

        NotificationManagerCompat mNotificationManager =
                NotificationManagerCompat.from(getApplicationContext());
        mNotificationManager.notify(SmartRingController.NOTIFICATION_ID_TTS, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            // Service restarted after suspension. Nothing to do.
            stopSelf();
            return START_NOT_STICKY;
        }

        synchronized(messageQueue) {
            if (intent.hasExtra(BLUETOOTH_TIMEOUT_EXTRA)) {
                if (bluetoothTimerTask != null) {
                    audioManager.stopBluetoothSco();
                    bluetoothTimerTask.cancel();
                    bluetoothTimerTask = null;

                    if (shouldRead(false, this)) {
                        // If SCO failed but we have another method we can read through, do so.
                        startReading(this);
                    } else {
                        // Otherwise clear the queue.
                        messageQueue.clear();
                    }
                }
            } else if (intent.hasExtra(STOP_READING_EXTRA)) {
                boolean stop = (tts != null);
                if (stop) {
                    // This will trigger onUtteranceCompleted, so we don't have to worry about cleaning up.
                    tts.stop();
                }
                messageQueue.clear();

            } else if (intent.hasExtra(QUEUE_MESSAGE_EXTRA)) {
                String message = intent.getStringExtra(QUEUE_MESSAGE_EXTRA);
                messageQueue.add(message);

                // if the TTS service is already running, just queue the message
                if (tts == null) {
                    //public static final int READING_AUDIO_STREAM = AudioManager.STREAM_MUSIC;
                    boolean preferSco = false;
                    if (!preferSco &&
                            (audioManager.isBluetoothA2dpOn() ||
                                mAudioManager.isWiredHeadsetOn(getApplicationContext()))) {
                        // We prefer to use non-sco if it's available. The logic is that if you have your
                        // headphones on in the car, the whole car shouldn't hear your messages.
                        TTSService.startReading(this);
                    } else if (audioManager.isBluetoothScoAvailableOffCall()) {
                        Logger.i(TAG, "Starting SCO, will wait until it is connected. sco on: " +
                                 audioManager.isBluetoothScoOn());
                        audioManager.startBluetoothSco();
                        bluetoothTimerTask = new TimerTask() {
                            @Override
                            public void run() {
                                TTSService.bluetoothTimeout(TTSService.this);
                            }
                        };
                        Timer timer = new Timer("bluetoothTimeoutTimer");
                        timer.schedule(bluetoothTimerTask, 5000);
                    } else if (shouldRead(false, this)) {
                        // In case we should read anyway (reading is always on)
                        TTSService.startReading(this);
                    }
                }
            } else if (intent.hasExtra(START_READING_EXTRA)) {
                if (bluetoothTimerTask != null) {
                    // Probably triggered by a bluetooth connection. reset;
                    bluetoothTimerTask.cancel();
                    bluetoothTimerTask = null;
                }

                if (tts == null && !messageQueue.isEmpty()) {
                    tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                        @Override
                        public void onInit(int status) {
                            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                                @Override
                                public void onError(String utteranceId) {

                                }

                                @Override
                                public void onStart(String utteranceId) {

                                }

                                @Override
                                public void onDone(String utteranceId) {
                                    updateNotification("...");
                                    restoreAudio();
                                    synchronized (messageQueue) {
                                        messageQueue.poll(); // retrieves and removes head of the queue
                                        if (messageQueue.isEmpty()) { //another message to speak ?
                                            // Sleep a little to give the bluetooth device a bit longer to finish.
                                            try {
                                                Thread.sleep(1000);
                                            } catch (InterruptedException e) {
                                                Logger.w(TAG, e.toString());
                                            }
                                            audioManager.stopBluetoothSco();
                                            tts.shutdown();
                                            tts = null;
                                            Logger.i(TAG, "Nothing else to speak. Shutting down TTS, stopping service.");
                                            TTSService.this.stopSelf();
                                        } else {
                                            Logger.i(TAG, "Speaking next message.");
                                            speak(messageQueue.peek());
                                        }
                                    }
                                }
                            });

                            synchronized (messageQueue) {
                                speak(messageQueue.peek());
                            }
                        }
                    });
                }
            }

            return START_STICKY;
        }
    }

    private void speak(final String text) {
        updateNotification(text);
        // The first message should clear the queue so we can start speaking right away.
        Logger.i(TAG, "speaking \"" + text + "\"");

        prepareAudio();

        if (accelerometerPresent){
            // us = 50 ms
            int SENSOR_DELAY = 50000;
            sensorManager.registerListener(ShakeActions, accelerometerSensor, SENSOR_DELAY, SENSOR_DELAY / 2);
        }

        Bundle ttsParams = new Bundle();
        ttsParams.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, READING_AUDIO_STREAM);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, ttsParams, "SRC1");
    }

    AudioManager.OnAudioFocusChangeListener audioFocusChangeListener =
        new AudioManager.OnAudioFocusChangeListener() {
            public void onAudioFocusChange(int focusChange) {
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    // Pause playback
                } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                    // Lower the volume
                } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                    // Resume playback or raise it back to normal
                } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                    audioManager.abandonAudioFocus(audioFocusChangeListener);
                    // Stop playback
                }
            }
        };

    private final SensorEventListener ShakeActions =
        new SensorEventListener()  {
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

            // called when sensor value have changed
            private int shake_left = 0;
            private int shake_right = 0;
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){

                    // if acceleration in x and y direction is too strong, device is moving

                    if (event.values[0] < -12.) shake_left++;
                    if (event.values[0] > 12.) shake_right++;

                    // shake to silence
                    if ((shake_left >= 1 && shake_right >= 2) ||
                            (shake_left >= 2 && shake_right >= 1)){

                        tts.stop(); // stop reading current message
                        stopSelf();
                        shake_left = shake_right = 0;
                    }

                }

            }
        };

    private void prepareAudio() {
        final SharedPreferences settings = this.getSharedPreferences(SmartRingController.PREFS_KEY, 0);
        final boolean ttsEnabled = settings.getBoolean("TTS.enabled", false);
        final String ttsMode = settings.getString("TTSmode", SmartRingController.TTS_MODE_HEADPHONES);

        //audioManager.setStreamSolo(READING_AUDIO_STREAM, true);
        audioManager.setSpeakerphoneOn(false);

        if (ttsEnabled && SmartRingController.TTS_MODE_ALWAYS.equals(ttsMode)) {
            audioManager.setSpeakerphoneOn(true);
        }

        systemVolume = -1;
        audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        );
    }

    public static void startReading(Context context) {
        Logger.i(TAG, "Starting to read message");
        sendIntent(TTSService.START_READING_EXTRA, null, context);
    }

    public static void stopReading(Context context) {
        Logger.i(TAG, "Stopping reading of messages");
        sendIntent(TTSService.STOP_READING_EXTRA, null, context);
    }

    public static void bluetoothTimeout(Context context) {
        Logger.i(TAG, "Timedout waiting for bluetooth.");
        sendIntent(TTSService.BLUETOOTH_TIMEOUT_EXTRA, null, context);
    }

    private void restoreAudio() {
        final SharedPreferences settings = this.getSharedPreferences(SmartRingController.PREFS_KEY, 0);
        final boolean ttsEnabled = settings.getBoolean("TTS.enabled", false);
        final String ttsMode = settings.getString("TTSmode", SmartRingController.TTS_MODE_HEADPHONES);

        sensorManager.unregisterListener(ShakeActions); // disable shake action

        // restore system setting of the speakerphone
        if (ttsEnabled && SmartRingController.TTS_MODE_ALWAYS.equals(ttsMode)) {
            audioManager.setSpeakerphoneOn(false);
        }

        if (systemVolume != -1) {
            Logger.i(TAG, "Resetting volume to " + systemVolume);
            audioManager.setStreamVolume(READING_AUDIO_STREAM, systemVolume, 0);
        }
        //audioManager.setStreamSolo(READING_AUDIO_STREAM, false);
        audioManager.abandonAudioFocus(audioFocusChangeListener);
    }
}
