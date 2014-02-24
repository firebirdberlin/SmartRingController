package com.firebirdberlin.smartringcontrollerpro;

import android.content.Context;
import android.os.Build;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

public class LicenseCheck{

	 // constructor
    public LicenseCheck(){

    }

	static public boolean isLicensed(Context context){
		//Beta phase allows full features until a certain date ---------
		boolean licensed = true;
		if (Config.PRO == false){ // Lite version
			//Calendar barrier = Calendar.getInstance();
			//barrier.set(Calendar.MONTH, Calendar.JANUARY);
			//barrier.set(Calendar.DAY_OF_MONTH, 1);
			//barrier.set(Calendar.YEAR, 2014);

			long when = getAppFirstInstallTime(context);
			Calendar barrier = Calendar.getInstance();
			barrier.setTimeInMillis(when);
			barrier.add(Calendar.HOUR_OF_DAY,24); // allow for 24 hours to test

			Calendar now = Calendar.getInstance();
			if (now.compareTo(barrier) == 1) licensed = false;
		}
		// -------------------------------------------------------------
		return licensed;
	}


	/**
	 * The time at which the app was first installed. Units are as per currentTimeMillis().
	 * @param context
	 * @return
	 */
	public static long getAppFirstInstallTime(Context context){
	    PackageInfo packageInfo;
	    try {
	    if(Build.VERSION.SDK_INT>8/*Build.VERSION_CODES.FROYO*/ ){
	        packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
	        return packageInfo.firstInstallTime;
	    }else{
	        //firstinstalltime unsupported return last update time not first install time
	        ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
	        String sAppFile = appInfo.sourceDir;
	        return new File(sAppFile).lastModified();
	    }
	    } catch (NameNotFoundException e) {
			//should never happen
			return 0;
	    }
	}

};
