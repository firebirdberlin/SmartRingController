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
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;


public class SmartRingController extends Activity {
    public static final String TAG = "SmartRingController";
    public static final String PREFS_KEY = "SmartRingController preferences";
    public static final String TTS_MODE_HEADPHONES = "headphones";
    public static final String TTS_MODE_ALWAYS = "always";

    private ActionBar actionbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final SharedPreferences settings = getSharedPreferences(SmartRingController.PREFS_KEY, 0);
        boolean enabled = settings.getBoolean("enabled", false);

        actionbar = getActionBar();
        actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionbar.setDisplayShowTitleEnabled(true);
        actionbar.setDisplayShowHomeEnabled(true);
        //actionBar.setTitle("");

        if (enabled){
            setupMainPage();
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
        final CompoundButton switchEnabled = (CompoundButton) menu.findItem(R.id.sw_enabled).getActionView();
        switchEnabled.setChecked(enabled);

        switchEnabled.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                SharedPreferences.Editor prefEditor = settings.edit();
                prefEditor.putBoolean("enabled", isChecked);
                prefEditor.commit();

                if (isChecked){
                    setupMainPage();
                } else {
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
        getFragmentManager().beginTransaction()
            .replace(android.R.id.content, new WelcomeFragment())
            .commit();
    }

    private void setupMainPage() {
        getFragmentManager().beginTransaction()
            .replace(android.R.id.content, new PreferencesFragment())
            .commit();
    }
}

////Use following code to open Notification Access setting screen
//Intent intent=new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
//startActivity(intent);
