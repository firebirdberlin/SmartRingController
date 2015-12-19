package com.firebirdberlin.smartringcontrollerpro;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import de.greenrobot.event.EventBus;
import java.lang.reflect.Field;
import java.lang.reflect.Method;


public class EnjoyTheSilenceService extends Service {
    private static String TAG = SmartRingController.TAG + ".EnjoyTheSilenceService";
    private boolean running = false;

    private WifiManager wifiManager;
    private PowerManager pm;
    PowerManager.WakeLock wakelock;
    private mAudioManager audiomanager;

    private boolean debug = false;
    private boolean toggleWifiState = false;
    private boolean toggleMobileDataState = false;
    private boolean systemMobileDataSetting = false;
    private boolean systemWifiOn = false;
    private int systemRingerMode = 0;

    @Override
    public void onCreate(){
        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakelock.acquire();

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        audiomanager = new mAudioManager(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.hasExtra("action")) {
            String action = intent.getStringExtra("action");
            boolean disconnectDataConnections =
                intent.getBooleanExtra("disconnectDataConnections", false);

            if ( action.equals("unmute") ) {
                parseIntent(intent);
                restoreSystemSettings();
            } else if ( action.equals("mute") ) {
                toggleMobileDataState = disconnectDataConnections;
                toggleWifiState = disconnectDataConnections;
                getSystemSettings();
                activateSilentMode();
            }
        }

        stopSelf();
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy(){
        if (debug){
            Log.d(TAG,"onDestroy() called.");
        }
        audiomanager = null;

        if (wakelock.isHeld()){
            wakelock.release();
        }
    }

    private void parseIntent(Intent intent) {
        EventBus bus = EventBus.getDefault();
        OnSilentModeActivated event = bus.getStickyEvent(OnSilentModeActivated.class);
        if (event != null) {
            toggleWifiState = event.toggleWifiState;
            toggleMobileDataState = event.toggleMobileDataState;
            systemMobileDataSetting = event.previousMobileDataStateOn;
            systemWifiOn = event.previousWifiStateOn;
            bus.removeStickyEvent(event);
            Log.d(TAG, "W: " + String.valueOf(systemWifiOn) + " | D:" + String.valueOf(systemMobileDataSetting));
        }
        systemRingerMode = intent.getIntExtra("systemRingerMode", AudioManager.RINGER_MODE_NORMAL);
    }

    public static void start(Context context, boolean disconnectDataConnections) {
        Intent i = new Intent(context, EnjoyTheSilenceService.class);
        i.putExtra("action", "mute");
        i.putExtra("disconnectDataConnections", disconnectDataConnections);
        context.startService(i);
    }

    private void getSystemSettings() {
        systemMobileDataSetting = isMobileDataEnabled();
        systemWifiOn = wifiManager.isWifiEnabled();
        systemRingerMode  = audiomanager.getRingerMode();
    }

    private void activateSilentMode() {
        audiomanager.setRingerModeSilent();

        if (toggleMobileDataState)  {
            setMobileDataEnabled(false);
        }
        if (toggleWifiState) {
            disableWifi();
        }

        OnSilentModeActivated event = new OnSilentModeActivated(systemRingerMode);
        event.previousWifiStateOn = systemWifiOn;
        event.previousMobileDataStateOn = systemMobileDataSetting;
        event.toggleMobileDataState = toggleMobileDataState;
        event.toggleWifiState = toggleWifiState;
        EventBus.getDefault().postSticky(event);
    }

    private void restoreSystemSettings() {
        if (audiomanager != null ){
            audiomanager.setRingerMode(systemRingerMode);
        }

        if (wifiManager != null && toggleWifiState){
            wifiManager.setWifiEnabled(systemWifiOn);
        }

        if (toggleMobileDataState) {
            setMobileDataEnabled(systemMobileDataSetting);
        }
    }

    private void setMobileDataEnabled(boolean enabled) {
        try{
           final ConnectivityManager conman = (ConnectivityManager)  getSystemService(Context.CONNECTIVITY_SERVICE);
           final Class conmanClass = Class.forName(conman.getClass().getName());
           final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
           iConnectivityManagerField.setAccessible(true);
           final Object iConnectivityManager = iConnectivityManagerField.get(conman);
           final Class iConnectivityManagerClass =  Class.forName(iConnectivityManager.getClass().getName());
           final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
           setMobileDataEnabledMethod.setAccessible(true);

           setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
        } catch (Exception e){
        }
    }

    private boolean isMobileDataEnabled() {
        boolean enabled = false;
        try{
            final ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            Class cmClass = Class.forName(cm.getClass().getName());
            Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
            method.setAccessible(true); // Make the method callable
            enabled = (Boolean) method.invoke(cm);
        } catch (Exception e){

        }
        return enabled;
    }

    private void disableWifi(){
        systemWifiOn = wifiManager.isWifiEnabled();
        if ( systemWifiOn ) {
            wifiManager.setWifiEnabled(false);
        }
    }

    private void restoreWifi(){
        if ( systemWifiOn ) {
            wifiManager.setWifiEnabled(true);
        }
    }
}
