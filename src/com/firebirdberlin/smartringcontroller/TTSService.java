package com.firebirdberlin.smartringcontrollerpro;

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
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.telephony.TelephonyManager;
import java.util.HashMap;
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

  private static int SENSOR_DELAY          = 50000;     // us = 50 ms
  private SensorManager sensorManager;
  private Sensor accelerometerSensor = null;
  private boolean accelerometerPresent;

  private final LocalBinder binder = new LocalBinder();
  private Queue<String> messageQueue = new LinkedList<String>();
  private TimerTask bluetoothTimerTask;

  private TextToSpeech tts;
  private SharedPreferences settings;

  private AudioManager audioManager;
  private int systemVolume;

  // settings
  public static final int READING_AUDIO_STREAM = AudioManager.STREAM_VOICE_CALL;
  //public static final int READING_AUDIO_STREAM = AudioManager.STREAM_MUSIC;
  private boolean preferSco = false;
  private int desiredVolume = -1;

  public class LocalBinder extends Binder {
      TTSService getService() {
          return TTSService.this;
      }
  }

  @Override
  public IBinder onBind(Intent arg0) {
    return binder;
  }

  @Override
  public void onCreate() {
    audioManager  = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
    sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

    List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
    if(sensorList.size() > 0){
        accelerometerPresent = true;
        accelerometerSensor = sensorList.get(0);
    }else{
        accelerometerPresent = false;
    }
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent == null) {
      // Service restarted after suspension. Nothing to do.
      stopSelf();
      return START_NOT_STICKY;
    }

    settings = getSharedPreferences(SmartRingController.PREFS_KEY, 0);

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
        if (stop == true) {
          // This will trigger onUtteranceCompleted, so we don't have to worry about cleaning up.
          tts.stop();
        }

        messageQueue.clear();

        // We still want to stick around long enough for onUtteranceCompleted to get called, so let
        // it call stopSelf();
        // If no TTS service was running, we stop here
//        if (stop == false) { // au backe .. das geht doch nicht
//            TTSService.this.stopSelf();
//        }
      } else if (intent.hasExtra(QUEUE_MESSAGE_EXTRA)) {
        String message = intent.getStringExtra(QUEUE_MESSAGE_EXTRA);
        messageQueue.add(message);

        // if the TTS service is already running, just queue the message
        if (tts == null) {
          if (! preferSco &&
              (audioManager.isBluetoothA2dpOn() ||
               audioManager.isWiredHeadsetOn())) {
            // We prefer to use non-sco if it's available. The logic is that if you have your
            // headphones on in the car, the whole car shouldn't hear your messages.
            TTSService.startReading(this);
          } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO &&
              audioManager.isBluetoothScoAvailableOffCall()) {
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
              tts.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
                @Override
                public void onUtteranceCompleted(String utteranceId) { // ready reading
                  restoreAudio();
                  synchronized (messageQueue) {
                    messageQueue.poll(); // retrieves and removes head of the queue
                    if (messageQueue.isEmpty()) { //another message to speak ?
                      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
                        // Sleep a little to give the bluetooth device a bit longer to finish.
                        try {
                          Thread.sleep(1000);
                        } catch (InterruptedException e) {
                          Logger.w(TAG, e.toString());
                        }
                        audioManager.stopBluetoothSco();
                      }
                      tts.shutdown();
                      tts = null;
                      TTSService.this.stopSelf();
                      Logger.i(TAG, "Nothing else to speak. Shutting down TTS, stopping service.");
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

    public static boolean shouldRead(boolean canUseSco, Context context){
        final SharedPreferences settings = context.getSharedPreferences(SmartRingController.PREFS_KEY, 0);
        boolean enabled = settings.getBoolean("enabled", false);
        boolean tts_enabled = settings.getBoolean("TTS.enabled", false);
        String ttsMode = settings.getString("TTSmode", SmartRingController.TTS_MODE_HEADPHONES);

        if ( enabled && tts_enabled) {
            if (Utility.getCallState(context) == TelephonyManager.CALL_STATE_OFFHOOK){
                // don't speak, when in call
                return false;
            }


            if ( ttsMode == SmartRingController.TTS_MODE_ALWAYS){
                return true;
            }

            AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
            return ((canUseSco && Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO &&
                     audioManager.isBluetoothScoAvailableOffCall())
                    || audioManager.isBluetoothA2dpOn()
                    || audioManager.isWiredHeadsetOn());
        }

        return false;
    }

  private void speak(final String text) {
    // The first message should clear the queue so we can start speaking right away.
    Logger.i(TAG, "speaking \"" + text + "\"");
    final HashMap<String, String> params = new HashMap<String, String>();
    params.put(TextToSpeech.Engine.KEY_PARAM_STREAM,
               String.valueOf(READING_AUDIO_STREAM));

    params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "valueNotUsed");
    prepareAudio();

    if (accelerometerPresent){
        if (Build.VERSION.SDK_INT < 19)
            sensorManager.registerListener(ShakeActions, accelerometerSensor, SENSOR_DELAY);
        else
            sensorManager.registerListener(ShakeActions, accelerometerSensor, SENSOR_DELAY, SENSOR_DELAY/2);
    }
    tts.speak(text, TextToSpeech.QUEUE_FLUSH, params);
  }

  private void prepareAudio() {
    final SharedPreferences settings = this.getSharedPreferences(SmartRingController.PREFS_KEY, 0);

    audioManager.setStreamSolo(READING_AUDIO_STREAM, true);
    audioManager.setSpeakerphoneOn(false);
    if ( settings.getBoolean("TTS.enabled", false)
         && (settings.getString("TTSmode", SmartRingController.TTS_MODE_HEADPHONES)
             == SmartRingController.TTS_MODE_ALWAYS) ) {
        audioManager.setSpeakerphoneOn(true);
    }

    //int desiredVolume = SettingsUtil.getVolume(this);

    // -1 means use the system volume.
    if (desiredVolume != -1) {
      systemVolume = audioManager.getStreamVolume(READING_AUDIO_STREAM);
      int boudedDesiredVolume =
          Math.min(desiredVolume, audioManager.getStreamMaxVolume(READING_AUDIO_STREAM));
      Logger.i(TAG, "Temporarily setting volume to " + boudedDesiredVolume);
      audioManager.setStreamVolume(READING_AUDIO_STREAM, boudedDesiredVolume, 0);
    } else {
      systemVolume = -1;
    }
  }

  private void restoreAudio() {
     sensorManager.unregisterListener(ShakeActions); // disable shake action

    // restore system setting of the speakerphone
    if (         settings.getBoolean("TTS.enabled", false)
            && (settings.getString("TTSmode", SmartRingController.TTS_MODE_HEADPHONES)
                == SmartRingController.TTS_MODE_ALWAYS) ) {
        audioManager.setSpeakerphoneOn(false);
    }

    if (systemVolume != -1) {
      Logger.i(TAG, "Resetting volume to " + systemVolume);
      audioManager.setStreamVolume(READING_AUDIO_STREAM, systemVolume, 0);
    }
    audioManager.setStreamSolo(READING_AUDIO_STREAM, false);
  }

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
                if ((shake_left >= 1 && shake_right >= 2)
                        || (shake_left >= 2 && shake_right >= 1)){

                    tts.stop(); // stop reading current message
                    shake_left = shake_right = 0;
                }

            }

        }
    };


  public static void queueMessage(String message, Context context) {
    if (shouldRead(false, context) == false) return;
    Logger.i(TAG, "Queueing message: " + message);
    sendIntent(TTSService.QUEUE_MESSAGE_EXTRA, message, context);
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

  private static void sendIntent(String extraName, String extraValue, Context context) {
    Intent readSmsIntent = new Intent(context, TTSService.class);
    readSmsIntent.putExtra(extraName, extraValue);
    context.startService(readSmsIntent);
  }
}
