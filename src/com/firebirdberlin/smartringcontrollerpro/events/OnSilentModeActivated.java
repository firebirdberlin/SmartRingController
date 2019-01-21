package com.firebirdberlin.smartringcontrollerpro.events;

public class OnSilentModeActivated {

    public int previousRingerMode;
    public boolean previousWifiStateOn;
    public boolean previousMobileDataStateOn;
    public boolean toggleWifiState = false;
    public boolean toggleMobileDataState = false;

    public OnSilentModeActivated(int previousRingerMode, boolean previousWifiStateOn,
            boolean previousMobileDataStateOn) {
        this.previousMobileDataStateOn = previousMobileDataStateOn;
        this.previousRingerMode = previousRingerMode;
        this.previousWifiStateOn = previousWifiStateOn;
        this.toggleMobileDataState = true;
        this.toggleWifiState = true;
    }

    public OnSilentModeActivated(int previousRingerMode, boolean previousWifiStateOn) {
        this.previousRingerMode = previousRingerMode;
        this.previousWifiStateOn = previousWifiStateOn;
        this.toggleWifiState = true;
    }

    public OnSilentModeActivated(int previousRingerMode) {
        this.previousRingerMode = previousRingerMode;
    }
}
