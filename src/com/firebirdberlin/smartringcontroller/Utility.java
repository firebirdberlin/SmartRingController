package com.firebirdberlin.smartringcontrollerpro;
import android.os.Build;
import android.content.Context;
import android.provider.AlarmClock;
import android.content.Intent;
import android.view.WindowManager;
import android.view.Display;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;

import android.util.DisplayMetrics;
import java.lang.Exception;
import java.io.IOException;
import java.lang.Thread;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.content.pm.ApplicationInfo;

import android.provider.Settings;
import android.provider.Settings.System;

public class Utility{

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

	public int getStatusBarHeight(Context context) {
      int result = 0;
      int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
      if (resourceId > 0) {
          result = context.getResources().getDimensionPixelSize(resourceId);
      }
      return result;
	}

	public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and keeps both
			// height and width larger than the requested height and width.
			while ((halfHeight / inSampleSize) > reqHeight
					&& (halfWidth / inSampleSize) > reqWidth) {
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
	}

}

