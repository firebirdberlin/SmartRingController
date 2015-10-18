package com.firebirdberlin.smartringcontrollerpro;


import android.media.MediaRecorder;
import android.util.Log;
import java.io.IOException;
import java.lang.RuntimeException;

public class SoundMeter {
        static final private double EMA_FILTER = 0.6;
        static String TAG= SmartRingController.TAG + ".SoundMeter";
        private MediaRecorder mRecorder = null;
        private double mEMA = 0.0;
        private boolean recording = false;


        public SoundMeter(){
            mRecorder = null;
            Logger.d(TAG,"SoundMeter()");
        }

        public boolean start() {
            Logger.d(TAG,"SoundMeter.start()");
            if (recording) return false;
            if (mRecorder == null) {
                mRecorder = new MediaRecorder();
                mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                mRecorder.setOutputFile("/dev/null");
                mRecorder.setOnErrorListener(errorListener);
                mRecorder.setOnInfoListener(infoListener);
            }
            try{
                mRecorder.prepare();
             }catch (IOException e) {
                Logger.e(TAG," > IOEXCEPTION, when preparing SoundMeter: " + e.toString());
                this.release();
                return false;
            } catch (IllegalStateException e) {
                Logger.e(TAG," > IllegalStateException, when preparing SoundMeter: " + e.toString());
                this.release();
                return false;
            }

            try{
                mRecorder.start();
                mRecorder.getMaxAmplitude(); // init
                mEMA = 0.0;
                Logger.d(TAG," > SoundMeter started,");
            } catch (IllegalStateException e) {
                Logger.e(TAG," > IllegalStateException, when starting SoundMeter: " + e.toString());
                this.release();
                return false;
            }
            recording = true;
            return true;
        }

        public void stop() {
            if (mRecorder != null) {
                try{
                    mRecorder.stop();
                } catch (IllegalStateException e) {
                    Logger.e(TAG,"Error, when stopping SoundMeter: " + e.toString());
                } catch (RuntimeException e) {
                    Logger.e(TAG,"RuntimeException when stopping SoundMeter: " + e.toString());
                }

                mRecorder.reset();

                try{
                    mRecorder.release();
                } catch (Exception e) {
                    Logger.e(TAG,"Error, when releasing SoundMeter: " +e.toString());
                }
                mRecorder = null;
                Logger.d(TAG,"SoundMeter stopped,");
            }
            recording = false;
        }

        public void release() {
            if (mRecorder != null) {
                mRecorder.reset();
                mRecorder.release();
                mRecorder = null;
                Logger.i(TAG,"SoundMeter released,");
            }
            recording = false;
        }

        public double getAmplitude() {
            if (mRecorder != null){
                return  (mRecorder.getMaxAmplitude());
            } else {
                return -1.0;
            }
        }

        public double getAmplitudeEMA() {
            double amp = getAmplitude();
            mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA;
            return mEMA;
        }

         private MediaRecorder.OnErrorListener errorListener = new MediaRecorder.OnErrorListener() {
            @Override
            public void onError(MediaRecorder mr, int what, int extra) {
                Logger.e(TAG,"Error: " + what + ", " + extra);
            }
        };

         private MediaRecorder.OnInfoListener infoListener = new MediaRecorder.OnInfoListener() {
             @Override
             public void onInfo(MediaRecorder mr, int what, int extra) {
                 Logger.e(TAG,"Warning: " + what + ", " + extra);
             }
         };
}

