package com.firebirdberlin.smartringcontrollerpro;

import android.app.ActionBar;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.firebirdberlin.smartringcontrollerpro.receivers.RingerModeStateChangeReceiver;


public class SmartRingController extends BillingHelperActivity {
    public static final String TAG = "SmartRingController";
    public static final String PREFS_KEY = "SmartRingController preferences";
    public static final String TTS_MODE_HEADPHONES = "headphones";
    public static final String TTS_MODE_ALWAYS = "always";
    public static final String NOTIFICATION_CHANNEL_ID_STATUS = "notification channel id status";
    public static final String NOTIFICATION_CHANNEL_ID_TTS = "notification channel id tts";

    public static final int NOTIFICATION_ID_STATUS = 10;
    public static final int NOTIFICATION_ID_TTS = 11;
    private ActionBar actionbar;
    private Context mContext = null;
    private PreferencesFragment preferencesFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        Utility.createNotificationChannels(this);

        actionbar = getActionBar();
        actionbar.setDisplayShowTitleEnabled(true);
        actionbar.setDisplayShowHomeEnabled(true);
        //actionBar.setTitle("");

        final SharedPreferences settings = getSharedPreferences(SmartRingController.PREFS_KEY, 0);
        boolean enabled = settings.getBoolean("enabled", false);

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
                prefEditor.apply();

                toggleComponentState(mContext, RingerModeStateChangeReceiver.class, isChecked);
                toggleComponentState(mContext, TTSService.class, isChecked);
                toggleComponentState(mContext, SetRingerService.class, isChecked);
                toggleComponentState(mContext, IncomingCallReceiver.class, isChecked);
                toggleComponentState(mContext, EnjoyTheSilenceService.class, isChecked);

                if (isChecked) {
                    setupMainPage();
                } else {
                    setupWelcomePage();
                }
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void setupWelcomePage() {
        preferencesFragment = null;
        getFragmentManager().beginTransaction()
            .replace(android.R.id.content, new WelcomeFragment())
            .commit();
    }

    private void setupMainPage() {
        preferencesFragment = new PreferencesFragment();
        getFragmentManager().beginTransaction()
            .replace(android.R.id.content, preferencesFragment)
            .commit();
    }

    @Override
    protected void updateAllPurchases() {
        super.updateAllPurchases();
        if (preferencesFragment != null) {
            if (isPurchased("donation")) {
                preferencesFragment.onItemPurchased("donation");
            } else if ( isPurchased("pro") ) {
                preferencesFragment.onItemPurchased("pro");
            }
        }
    }

    public void toggleComponentState(Context context, Class component, boolean on){
        ComponentName receiver = new ComponentName(context, component);
        PackageManager pm = context.getPackageManager();
        int new_state = (on) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        pm.setComponentEnabledSetting(receiver, new_state, PackageManager.DONT_KILL_APP);
    }
}
