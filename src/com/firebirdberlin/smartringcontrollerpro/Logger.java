package com.firebirdberlin.smartringcontrollerpro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;

public class Logger {

    private static boolean debug = true;

    public static void setDebugging(boolean on){
        debug = on;
    }

    public static void d(String TAG, String msg){
        if (debug) {
            Log.d(TAG, msg);
        }
    }

    public static void e(String TAG, String msg){
        if (debug) {
            Log.e(TAG, msg);
        }
    }

    public static void i(String TAG, String msg){
        if (debug) {
            Log.i(TAG, msg);
        }
    }

    public static void w(String TAG, String msg){
        if (debug) {
            Log.w(TAG, msg);
        }
    }

}
