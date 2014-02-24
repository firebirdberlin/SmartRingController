package com.firebirdberlin.smartringcontrollerpro;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.widget.RemoteViews;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.Locale;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class mNotificationListener extends NotificationListenerService {

	private String TAG = SmartRingController.TAG +"." + this.getClass().getSimpleName();
	private long last_notification_posted = 0;
	private final int min_notification_interval = 3000; // ms to be silent between notifications


	private NLServiceReceiver nlservicereciver;
	@Override
	public void onCreate() {
		super.onCreate();
		nlservicereciver = new NLServiceReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction("com.firebirdberlin.smartringcontroller.NOTIFICATION_LISTENER_SERVICE_EXAMPLE");
		registerReceiver(nlservicereciver,filter);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(nlservicereciver);
	}

	@Override
	public void onNotificationPosted(StatusBarNotification sbn) {

		// phone call is handled elsewhere
		if (sbn.getPackageName().equals("com.android.phone")) return;
		Notification n = sbn.getNotification();


		if (sbn.getPackageName().equals("com.whatsapp")){
			// whatsapp sends no sound ... wtf ?
		} else // no sound, no action
		if ((n.defaults & Notification.DEFAULT_SOUND) == Notification.DEFAULT_SOUND){
			// do something--it was set
			// this is a notification with default sound
		} else {
			if (n.sound == null) return;
		}

		//mAudioManager.muteNotificationSounds(true, this);

        // if the last notification was within the last 10s
        if ((System.currentTimeMillis()-last_notification_posted)
                < min_notification_interval){
            //n.sound = null; // remove the sound, to be more pleasent
            queueMessage(n, this);
            return;
        }

        last_notification_posted = System.currentTimeMillis();

        Logger.i(TAG,"**********  onNotificationPosted");
		Logger.i(TAG,"********** " + sbn.getPackageName());
		Logger.i(TAG,"********** " + sbn.getNotification().tickerText);

		Intent i = new  Intent("com.firebirdberlin.smartringcontroller.NOTIFICATION_LISTENER");
		i.putExtra("notification_event","notification :" + sbn.getPackageName());
		sendBroadcast(i);

		Intent i2= new Intent(this, SetRingerService.class);
		// potentially add data to the intent
		i2.putExtra("PHONE_STATE", "Notification");
		if (n.sound != null){
			i2.putExtra("Sound", n.sound.toString() );
		}
		startService(i2);

		queueMessage(n, this);
	}

	@Override
	public void onNotificationRemoved(StatusBarNotification sbn) {
		//Log.i(TAG,"********** onNOtificationRemoved");
		//Log.i(TAG,"ID :" + sbn.getId() + "\t" + sbn.getNotification().tickerText +"\t" + sbn.getPackageName());
		//Intent i = new  Intent("com.firebirdberlin.smartringcontrollerpro.NOTIFICATION_LISTENER");
		//i.putExtra("notification_event","onNotificationRemoved :" + sbn.getPackageName() + "\n");
		//sendBroadcast(i);
	}

	class NLServiceReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getStringExtra("command").equals("clearall")){
					mNotificationListener.this.cancelAllNotifications();
			}
			else if(intent.getStringExtra("command").equals("list")){
				Intent i1 = new  Intent("com.firebirdberlin.smartringcontroller.NOTIFICATION_LISTENER");
				i1.putExtra("notification_event","=====================");
				sendBroadcast(i1);
				int i=1;
				for (StatusBarNotification sbn : mNotificationListener.this.getActiveNotifications()) {
					Intent i2 = new  Intent("com.firebirdberlin.smartringcontroller.NOTIFICATION_LISTENER");
					i2.putExtra("notification_event",i +" " + sbn.getPackageName() + "n");
					sendBroadcast(i2);
					i++;
				}
				Intent i3 = new  Intent("com.firebirdberlin.smartringcontroller.NOTIFICATION_LISTENER");
				i3.putExtra("notification_event","===== Notification List ====");
				sendBroadcast(i3);

			}

		}
	}

	public static void queueMessage(Notification n, Context context){
		String text = getText(n);
		text = text.replace('#',' '); // replace hashtags
		//text = text.replaceAll("http://.*\\s",""); // remove urls
		text = text.replaceAll("\\(?\\b(http://|www[.])[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]"," "); // remove urls
		text = truncate(text, 1000); // truncate after 1000 chars
		TTSService.queueMessage(text, context);
	}

	public static String truncate(final String content, final int lastIndex) {
		if (lastIndex >  content.length()) return content; // do nothing if short enough
		String result = content.substring(0, lastIndex);
		if (content.charAt(lastIndex) != ' ') { // find last word
			result = result.substring(0, result.lastIndexOf(" "));
		}
		return result;
	}

	public static String getText(Notification notification)
	{
	// We have to extract the information from the view
	RemoteViews        views = notification.bigContentView;
	if (views == null) views = notification.contentView;
	if (views == null) return null;

	// Use reflection to examine the m_actions member of the given RemoteViews object.
	// It's not pretty, but it works.
	//List<String> text = new ArrayList<String>();
	String text = "";
	try
	{
		Field field = views.getClass().getDeclaredField("mActions");
		field.setAccessible(true);

		@SuppressWarnings("unchecked")
		ArrayList<Parcelable> actions = (ArrayList<Parcelable>) field.get(views);

		// Find the setText() and setTime() reflection actions
		for (Parcelable p : actions)
		{
			Parcel parcel = Parcel.obtain();
			p.writeToParcel(parcel, 0);
			parcel.setDataPosition(0);

			// The tag tells which type of action it is (2 is ReflectionAction, from the source)
			int tag = parcel.readInt();
			if (tag != 2) continue;

			// View ID
			parcel.readInt();

			String methodName = parcel.readString();
			if (methodName == null) continue;

			// Save strings
			else if (methodName.equals("setText"))
			{
				// Parameter type (10 = Character Sequence)
				parcel.readInt();

				// Store the actual string
				String t = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel).toString().trim();
				//text.add(t);
				text += " " + t;
			}

			// Save times. Comment this section out if the notification time isn't important
			else if (methodName.equals("setTime"))
			{
				// Parameter type (5 = Long)
				parcel.readInt();
				long val = parcel.readLong();
				String t = new SimpleDateFormat("h:mm a").format(new Date(val));
				if (Build.VERSION.SDK_INT >= 18){
					DateFormat formatter = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
					String pattern  = ((SimpleDateFormat)formatter).toLocalizedPattern();
					t = new SimpleDateFormat(pattern).format(new Date(val));
				}
				//text.add(t);
				text += " " + t;
			}

			parcel.recycle();
		}
	}

	// It's not usually good style to do this, but then again, neither is the use of reflection...
	catch (Exception e)
	{
		Logger.e("NotificationClassifier", e.toString());
	}

	return text;
	}
}
