package com.firebirdberlin.smartringcontrollerpro;

import android.app.ActionBar; // >= api level 11
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SmartRingController extends Activity {
    public static final String TAG = "SmartRingController";
    public static final String PREFS_KEY = "SmartRingController preferences";
	public static final String TTS_MODE_HEADPHONES 	= "headphones";
	public static final String TTS_MODE_ALWAYS 		= "always";

    private Fragment mPreferencesFragment;
    private Fragment mTestFragment;
    private LinearLayout tabs;
    private ActionBar actionbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tabs);

        tabs = (LinearLayout) findViewById(R.id.fragment_container);

        final SharedPreferences settings = getSharedPreferences(SmartRingController.PREFS_KEY, 0);
        boolean enabled = settings.getBoolean("enabled", false);

        actionbar = getActionBar();
        if (enabled){
            actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        } else {
            actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        }
        //actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionbar.setDisplayShowTitleEnabled(true);
        actionbar.setDisplayShowHomeEnabled(true);
        //actionBar.setTitle("");

        ActionBar.Tab TabActions = actionbar.newTab().setText(getString(R.string.tabSettings));
        ActionBar.Tab TabTest = actionbar.newTab().setText(getString(R.string.tabTest));

        //create the two fragments we want to use for display content
        mPreferencesFragment = new PreferencesFragment();
        mTestFragment = new TestFragment();

        //set the Tab listener. Now we can listen for clicks.
        TabActions.setTabListener(new TabsListener(mPreferencesFragment));
        TabTest.setTabListener(new TabsListener(mTestFragment));

        //add the two tabs to the actionbar
        actionbar.addTab(TabActions);
        actionbar.addTab(TabTest);

        if (enabled){
            setContentView(R.layout.tabs);
        } else {
            setupWelcomePage();
        }
    }

    // Activity bar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actions, menu);

        final SharedPreferences settings = getSharedPreferences(SmartRingController.PREFS_KEY, 0);
        boolean enabled = settings.getBoolean("enabled", false);
        final     CompoundButton switchEnabled     = (CompoundButton) menu.findItem(R.id.sw_enabled).getActionView();
        switchEnabled.setChecked(enabled);

        switchEnabled.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                SharedPreferences.Editor prefEditor = settings.edit();
                prefEditor.putBoolean("enabled", isChecked);
                prefEditor.commit();

                if (isChecked){
                    setContentView(R.layout.tabs);
                    actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
                } else {
                    actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
                    setupWelcomePage();
                }
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void setupWelcomePage(){
        setContentView(R.layout.welcome);
        try{
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            TextView tv = (TextView) findViewById(R.id.tvWelcome);
            tv.setText(packageInfo.applicationInfo.loadLabel(getPackageManager()).toString()
                    + " " + packageInfo.versionName
                    + "\n\n" + tv.getText() );
        } catch (NameNotFoundException e) {
            //should never happen
            return;
        }
    }

    public void buttonClicked(View v){
        if(v.getId() == R.id.btnClearNotify) {
            Intent i = new Intent(this, SetRingerService.class);
            i.putExtra("PHONE_STATE", "TestService");
            startService(i);
        } else if (v.getId() == R.id.btnTestNotify) {
            Intent intent = new Intent(this, SmartRingController.class);
            PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

            // build notification
            Notification n = new Notification.Builder(this)
                .setContentTitle("Smart Ring Controller")
                .setContentText(getString(R.string.msgTestNotification))
                .setSmallIcon(R.drawable.ic_launcher_gray)
                .setContentIntent(pIntent)
                .setAutoCancel(true)
                .build();

            n.defaults |= Notification.DEFAULT_SOUND;
            //n.defaults |= Notification.DEFAULT_ALL;
            NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            notificationManager.notify(0, n);
        }
    }
}

////Use following code to open Notification Access setting screen
//Intent intent=new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
//startActivity(intent);
