package com.firebirdberlin.smartringcontrollerpro;

import android.app.ActionBar; // >= api level 11
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;
import java.text.DateFormat;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

public class SmartRingController extends Activity {
    public static final String TAG = "SmartRingController";
    public static final String PREFS_KEY = "SmartRingController preferences";

    private Fragment mSettingsFragment;
    private Fragment mTTSFragment;
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

        ActionBar.Tab TabSettings = actionbar.newTab().setText(getString(R.string.tabSettings));
        ActionBar.Tab TabTTS = actionbar.newTab().setText(getString(R.string.tabTTS));
        ActionBar.Tab TabTest = actionbar.newTab().setText(getString(R.string.tabTest));

        //create the two fragments we want to use for display content
        mSettingsFragment = new SettingsFragment();
        mTTSFragment = new TTSFragment();
        mTestFragment = new TestFragment();

        //set the Tab listener. Now we can listen for clicks.
        TabSettings.setTabListener(new TabsListener(mSettingsFragment));
        TabTTS.setTabListener(new TabsListener(mTTSFragment));
        TabTest.setTabListener(new TabsListener(mTestFragment));

        //add the two tabs to the actionbar
        actionbar.addTab(TabSettings);
        actionbar.addTab(TabTTS);
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
        //menu.findItem(R.id.action_search_view).getActionView();
        // Get widget's instance
        //switchEnabled = (Switch)menu.findItem(R.id.sw_enabled).getActionView();
        //swtService.setOnCheckedChangeListener(this);
        final SharedPreferences settings = getSharedPreferences(SmartRingController.PREFS_KEY, 0);
        boolean enabled = settings.getBoolean("enabled", false);
        final     CompoundButton switchEnabled     = (CompoundButton) menu.findItem(R.id.sw_enabled).getActionView();
        switchEnabled.setChecked(enabled);

        switchEnabled.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView,
                    final boolean isChecked) {
                //tabs.setEnabled(isChecked);

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

    if(v.getId() == R.id.btnClearNotify){
        Intent i= new Intent(this, SetRingerService.class);
        // potentially add data to the intent
        //i.putExtra("KEY1", "Value to be used by the service");

        //TTSService.queueMessage("Dies ist ein Test.", this);

        i.putExtra("PHONE_STATE", "TestService");
//        i.putExtra("PHONE_STATE", "RINGING");
        startService(i);
        //Intent i = new Intent("com.firebirdberlin.smartringcontrollerpro.NOTIFICATION_LISTENER_SERVICE_EXAMPLE");
        //i.putExtra("command","clearall");
        //sendBroadcast(i);
    } else if(v.getId() == R.id.buttonAccessibilitySettings){
        if (Build.VERSION.SDK_INT < 18)    {
            Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivityForResult(intent, 0);
        } else {
            Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            startActivityForResult(intent, 0);
        }
    } else if (v.getId() == R.id.btnTestNotify){
        // prepare intent which is triggered if the
        // notification is selected

        Intent intent = new Intent(this, SmartRingController.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

        // build notification
        // the addAction re-use the same intent to keep the example short
        Notification n  = new Notification.Builder(this)
        .setContentTitle("Smart Ring Controller")
        .setContentText(getString(R.string.msgTestNotification))
        .setSmallIcon(R.drawable.ic_launcher_gray)
        .setContentIntent(pIntent)
        .setAutoCancel(true)
        //.addAction(R.drawable.icon, "Call", pIntent)
        //.addAction(R.drawable.icon, "More", pIntent)
        //.addAction(R.drawable.icon, "And more", pIntent)
        .build();

        n.defaults |= Notification.DEFAULT_SOUND;
        //n.defaults |= Notification.DEFAULT_ALL;
        NotificationManager notificationManager =
            (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        notificationManager.notify(0, n);
    }
    }

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();
        final SharedPreferences settings = getSharedPreferences(SmartRingController.PREFS_KEY, 0);
        SharedPreferences.Editor prefEditor = settings.edit();
        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio_TTS_headphones:
                if (checked){
                    prefEditor.putInt("TTS.mode", TTSFragment.TTS_MODE_HEADPHONES);
                }
                break;
            case R.id.radio_TTS_always:
                if (checked){
                    prefEditor.putInt("TTS.mode", TTSFragment.TTS_MODE_ALWAYS);
                }
                break;
        }
        prefEditor.commit();
    }

}

////Use following code to open Notification Access setting screen
//Intent intent=new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
//startActivity(intent);
