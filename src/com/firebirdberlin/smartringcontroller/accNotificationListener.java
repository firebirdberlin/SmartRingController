package com.firebirdberlin.smartringcontrollerpro;

import android.app.Notification;
import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.lang.String;

public class accNotificationListener extends AccessibilityService {
	private boolean isInit;
	private String TAG = this.getClass().getSimpleName();
    private long last_notification_posted = 0;
    private int min_notification_interval = 10000; // ms to be silent between notifications

	@Override
	protected void onServiceConnected() {
	    if (isInit) {
	        return;
	    }
	    //AccessibilityServiceInfo info = new AccessibilityServiceInfo();
	    //info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
	    //info.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN;
	    //setServiceInfo(info);
	    isInit = true;
	}

	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
	    if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
	        //Do something, eg getting packagename
	    final String packagename = String.valueOf(event.getPackageName());

	        //{
	            //Intent i = new  Intent("com.firebirdberlin.nightdream.NOTIFICATION_LISTENER");
	            //i.putExtra("what","onNotificationPosted :" + event.getPackageName() + "n");
	            //i.putExtra("action","added");
	            //i.putExtra("tickertext",event.getText().toString());
	            //i.putExtra("number",1);
	            //sendBroadcast(i);
	        //}

	         // phone call is handled elsewhere
        if (packagename.equals("com.android.phone")) return;

        if ( ! (event.getParcelableData() instanceof Notification)) return;
        Notification n = (Notification) event.getParcelableData();

        if (packagename.equals("com.whatsapp")){
            // whatsapp sends no sound ... wtf ?
        } else
        if ((n.defaults & Notification.DEFAULT_SOUND) == Notification.DEFAULT_SOUND){
            // do something--it was set
            // this is a notification with default sound
        } else {
			// no sound, no action
            if (n.sound == null) return;
        }


        // if the last notification was within the last 10s
        if ((System.currentTimeMillis()-last_notification_posted)
                < min_notification_interval){
			mNotificationListener.queueMessage(n, this);
            return;
        }
        last_notification_posted = System.currentTimeMillis();

        Logger.i(TAG, packagename);
        //n.tickertext;

        Intent i = new  Intent("com.firebirdberlin.smartringcontroller.NOTIFICATION_LISTENER");
        i.putExtra("notification_event","notification :" + packagename);
        sendBroadcast(i);

        Intent i2= new Intent(this, SetRingerService.class);
		// potentially add data to the intent
		i2.putExtra("PHONE_STATE", "Notification");
		if (n.sound != null){
			i2.putExtra("Sound", n.sound.toString() );
		}
        startService(i2);

		if (Config.PRO == true || LicenseCheck.isLicensed(this) == true){
			mNotificationListener.queueMessage(n, this);
		}

		}
	}

	@Override
	public void onInterrupt() {
	    isInit = false;
	}
}
