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

        return view;
    }

}
