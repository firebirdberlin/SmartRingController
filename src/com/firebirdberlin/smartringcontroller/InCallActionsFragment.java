package com.firebirdberlin.smartringcontrollerpro;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Settings.System;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;


public class InCallActionsFragment extends Fragment {
    SharedPreferences settings;

    private CompoundButton switch_handle_notification;
    private CompoundButton switch_handle_vibration;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.incallactionsfragment, container, false);

        settings = getActivity().getSharedPreferences(SmartRingController.PREFS_KEY, 0);
        boolean enabled = settings.getBoolean("enabled", false);

        boolean FlipAction = settings.getBoolean("FlipAction", false);
        boolean ShakeAction = settings.getBoolean("ShakeAction", false);
        boolean PullOutAction = settings.getBoolean("PullOutAction", false);

        final CompoundButton switchFlipAction = (CompoundButton) view.findViewById(R.id.switchFlipAction);
        switchFlipAction.setChecked(FlipAction);
        switchFlipAction.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView,
                final boolean isChecked) {
                SharedPreferences.Editor prefEditor = settings.edit();
                prefEditor.putBoolean("FlipAction", isChecked);
                prefEditor.commit();
            }
        });

        final CheckBox cbAutoReactivateRingerMode = (CheckBox) view.findViewById(R.id.cbAutoReactivateRingerMode);

        cbAutoReactivateRingerMode.setChecked(settings.getBoolean("Ctrl.AutoReactivateRingerMode",
                                                                  false));
        cbAutoReactivateRingerMode.setOnCheckedChangeListener(new OnCheckedChangeListener()    {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor prefEditor = settings.edit();
                prefEditor.putBoolean("Ctrl.AutoReactivateRingerMode", isChecked);
                prefEditor.commit();
            }
        });

        final CompoundButton switchShakeAction = (CompoundButton) view.findViewById(R.id.switchShakeAction);
        switchShakeAction.setChecked(ShakeAction);
        switchShakeAction.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView,
                final boolean isChecked) {
                SharedPreferences.Editor prefEditor = settings.edit();
                prefEditor.putBoolean("ShakeAction", isChecked);
                prefEditor.commit();
            }
        });

        final CompoundButton switchPullOutAction     = (CompoundButton) view.findViewById(R.id.switchPullOutAction);
        switchPullOutAction.setChecked(PullOutAction);
        switchPullOutAction.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView,
                final boolean isChecked) {
                SharedPreferences.Editor prefEditor = settings.edit();
                prefEditor.putBoolean("PullOutAction", isChecked);
                prefEditor.commit();
            }
        });

        boolean handleVibration = settings.getBoolean("handle_vibration", false);
        boolean handleNotification = settings.getBoolean("handle_notification", false);

        switch_handle_vibration = (CompoundButton) view.findViewById(R.id.SwitchHandleVibration);

        switch_handle_vibration.setChecked(handleVibration);
        switch_handle_vibration.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView,
                final boolean isChecked) {
                SharedPreferences.Editor prefEditor = settings.edit();
                prefEditor.putBoolean("handle_vibration", isChecked);
                prefEditor.commit();

                if (isChecked){
                    Settings.System.putInt(
                        getActivity().getContentResolver(),
                        "vibrate_when_ringing", false ? 1 : 0);
                }
            }
        });

        switch_handle_notification = (CompoundButton) view.findViewById(R.id.SwitchHandleNotification);
        switch_handle_notification.setChecked(handleNotification);
        switch_handle_notification.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton buttonView,
                final boolean isChecked) {
                SharedPreferences.Editor prefEditor = settings.edit();
                prefEditor.putBoolean("handle_notification", isChecked);
                prefEditor.commit();
            }
        });

        final CheckBox cbBrokenProximitySensor = (CheckBox) view.findViewById(R.id.cbBrokenProximitySensor);
        cbBrokenProximitySensor.setChecked(settings.getBoolean("Ctrl.BrokenProximitySensor", true));
        cbBrokenProximitySensor.setOnCheckedChangeListener(new OnCheckedChangeListener()    {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor prefEditor = settings.edit();
                prefEditor.putBoolean("Ctrl.BrokenProximitySensor", isChecked);
                prefEditor.commit();
            }

        });

        return view;
    }

}
