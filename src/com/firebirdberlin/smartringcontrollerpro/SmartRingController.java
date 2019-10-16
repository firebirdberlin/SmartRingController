package com.firebirdberlin.smartringcontrollerpro;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.firebirdberlin.smartringcontrollerpro.receivers.IncomingCallReceiver;


public class SmartRingController extends BillingHelperActivity
                                implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    public static final String TAG = "SmartRingController";
    public static final String PREFS_KEY = "SmartRingController preferences";
    public static final String TTS_MODE_HEADPHONES = "headphones";
    public static final String TTS_MODE_ALWAYS = "always";
    public static final String NOTIFICATION_CHANNEL_ID_STATUS = "notification channel id status";
    public static final String NOTIFICATION_CHANNEL_ID_TTS = "notification channel id tts";
    public static final String NOTIFICATION_CHANNEL_ID_RINGER_SERVICE = "notification channel id ringer service";

    public static final int NOTIFICATION_ID_STATUS = 10;
    public static final int NOTIFICATION_ID_TTS = 11;
    public static final int NOTIFICATION_ID_RINGER_SERVICE = 12;
    private Context mContext = null;
    private PreferencesFragment preferencesFragment = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        Utility.createNotificationChannels(this);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);


        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayShowTitleEnabled(true);
            actionbar.setDisplayShowHomeEnabled(true);
            actionbar.setHomeButtonEnabled(true);
            actionbar.setDisplayHomeAsUpEnabled(false);
            actionbar.setIcon(R.drawable.ic_launcher);
        }

        final SharedPreferences settings = getSharedPreferences(SmartRingController.PREFS_KEY, 0);
        boolean enabled = settings.getBoolean("enabled", true);

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

        MenuItem btn = menu.findItem(R.id.sw_enabled);
        final SwitchCompat switchEnabled = (SwitchCompat) btn.getActionView();
        if (switchEnabled != null) {
            final SharedPreferences settings = getSharedPreferences(SmartRingController.PREFS_KEY, 0);
            boolean enabled = settings.getBoolean("enabled", true);
            switchEnabled.setChecked(enabled);
            toggleComponents(enabled);

            switchEnabled.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                    SharedPreferences.Editor prefEditor = settings.edit();
                    prefEditor.putBoolean("enabled", isChecked);
                    prefEditor.apply();

                    toggleComponents(isChecked);

                    if (isChecked) {
                        setupMainPage();
                    } else {
                        setupWelcomePage();
                    }
                }
            });

        }
        return super.onCreateOptionsMenu(menu);
    }

    void toggleComponents(boolean on) {
        toggleComponentState(mContext, TTSService.class, on);
        toggleComponentState(mContext, SetRingerService.class, on);
        toggleComponentState(mContext, IncomingCallReceiver.class, on);
        toggleComponentState(mContext, EnjoyTheSilenceService.class, on);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void setupWelcomePage() {
        preferencesFragment = null;
        getSupportFragmentManager().beginTransaction()
            .replace(android.R.id.content, new WelcomeFragment())
            .commit();
    }

    private void setupMainPage() {
        preferencesFragment = new PreferencesFragment();
        getSupportFragmentManager().beginTransaction()
            .replace(android.R.id.content, preferencesFragment)
            .commit();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (preferencesFragment != null) {
            preferencesFragment.onBackPressed();
        }
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        Log.i(TAG, "onPreferenceStartFragment(): " + pref.getKey());
        final Fragment fragment =
                Fragment.instantiate(this, pref.getFragment(), pref.getExtras());
        fragment.setTargetFragment(caller, 0);
        Bundle args = new Bundle();
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref.getKey());
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, fragment)
                .addToBackStack(null)
                .commit();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(pref.getTitle());
        }

        return true;
    }

    @Override
    protected void onPurchasesInitialized() {
        super.onPurchasesInitialized();

        if (preferencesFragment != null) {
            if (! isPurchased(ITEM_PRO) &&
                    ! isPurchased(ITEM_DONATION)) {
                // disable pro features
                SharedPreferences settings = getSharedPreferences(SmartRingController.PREFS_KEY, 0);
                SharedPreferences.Editor prefEditor = settings.edit();
                prefEditor.putBoolean("handleNotification", false);
                prefEditor.putBoolean("handle_vibration", false);
                prefEditor.putBoolean("ShakeAction", false);
                prefEditor.putBoolean("FlipAction", false);
                prefEditor.putBoolean("PullOutAction", false);
                prefEditor.putBoolean("SilentWhilePebbleConnected", false);
                prefEditor.apply();
                setupMainPage();
            }
            preferencesFragment.initPurchases();
        }
    }

    @Override
    protected void onItemPurchased(String sku) {
        super.onItemPurchased(sku);
        preferencesFragment.initPurchases();
    }

    public void toggleComponentState(Context context, Class component, boolean on){
        ComponentName receiver = new ComponentName(context, component);
        PackageManager pm = context.getPackageManager();
        int new_state = (on) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        pm.setComponentEnabledSetting(receiver, new_state, PackageManager.DONT_KILL_APP);
    }
}
