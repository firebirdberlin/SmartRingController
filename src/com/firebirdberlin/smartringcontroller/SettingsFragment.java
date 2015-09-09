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
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;


public class SettingsFragment extends Fragment {
    SharedPreferences settings;
    private TextView tvMinAmplitude;
    private TextView tvMaxAmplitude;
    private TextView tvMinRingerVolume;
    private TextView tvPocketVolume;

    private int minAmplitude;
    private int maxAmplitude;

    private SeekBar SeekBarMin;
    private SeekBar SeekBarMax;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.settingsfragment, container, false);

        settings = getActivity().getSharedPreferences(SmartRingController.PREFS_KEY, 0);
        boolean enabled = settings.getBoolean("enabled", false);
        minAmplitude = settings.getInt("minAmplitude", 0);
        maxAmplitude = settings.getInt("maxAmplitude", 7000);
        int minRingerVolume = settings.getInt("minRingerVolume", 1);
        boolean handleRingerVolume = settings.getBoolean("Ctrl.RingerVolume", true);
        int addPocketVolume = settings.getInt("Ctrl.PocketVolume", 0);

        tvMinAmplitude = (TextView) view.findViewById(R.id.tvMinAmplitude);
        tvMaxAmplitude = (TextView) view.findViewById(R.id.tvMaxAmplitude);
        tvMinRingerVolume = (TextView) view.findViewById(R.id.tvMinRingerVolume);
        tvPocketVolume = (TextView) view.findViewById(R.id.tvPocketVolume);

        String str = getResources().getString(R.string.setting_min_amplitude);
        tvMinAmplitude.setText(str + " : " + String.valueOf(minAmplitude));

        str = getResources().getString(R.string.setting_max_amplitude);
        tvMaxAmplitude.setText(str + " : " + String.valueOf(maxAmplitude));

        str = getResources().getString(R.string.setting_min_ringer_volume);
        tvMinRingerVolume.setText(str + " : " + String.valueOf(minRingerVolume));

        str = getResources().getString(R.string.tvPocketVolume);
        tvPocketVolume.setText(str + " " + String.valueOf(addPocketVolume));

        SeekBarMin =(SeekBar) view.findViewById(R.id.SeekBarMin);
        SeekBarMin.setProgress(minAmplitude);
        SeekBarMin.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                SharedPreferences.Editor prefEditor = settings.edit();
                minAmplitude = seekBar.getProgress();
                prefEditor.putInt("minAmplitude", minAmplitude);
                prefEditor.putInt("maxAmplitude", maxAmplitude);
                prefEditor.commit();
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                int p = 50 * (int) (progress / 50);
                seekBar.setProgress(p);
                minAmplitude = p;
                String str = getResources().getString(R.string.setting_min_amplitude);
                tvMinAmplitude.setText(str + " : " + String.valueOf(p));
                if (maxAmplitude <= minAmplitude){
                    maxAmplitude +=1000;
                    tvMaxAmplitude.setText(str + " : " + String.valueOf(maxAmplitude));
                    SeekBarMax.setProgress(maxAmplitude-minAmplitude-50);
                }

            }
        });

        SeekBarMax =(SeekBar) view.findViewById(R.id.SeekBarMax);
        SeekBarMax.setProgress(maxAmplitude-minAmplitude-50);
        SeekBarMax.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                SharedPreferences.Editor prefEditor = settings.edit();
                maxAmplitude = seekBar.getProgress()+SeekBarMin.getProgress();
                prefEditor.putInt("maxAmplitude", maxAmplitude);
                prefEditor.putInt("minAmplitude", minAmplitude);
                prefEditor.commit();
            }


            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                int p = 50 * (int) (progress / 50);
                seekBar.setProgress(p);
                maxAmplitude = p + minAmplitude + 50;
                String str = getResources().getString(R.string.setting_max_amplitude);
                tvMaxAmplitude.setText(str + " : " + String.valueOf(maxAmplitude));
                if (minAmplitude >= maxAmplitude){
                    minAmplitude = 0;
                    SeekBarMin.setProgress(0);
                }
            }
        });

        final SeekBar SeekBarMinRingerVolume =(SeekBar) view.findViewById(R.id.SeekBarMinRingerVolume);
        SeekBarMinRingerVolume.setProgress(minRingerVolume-1);
        mAudioManager audiomanager = new mAudioManager(getActivity());
        SeekBarMinRingerVolume.setMax(audiomanager.getMaxRingerVolume()-1);

        SeekBarMinRingerVolume.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                SharedPreferences.Editor prefEditor = settings.edit();
                prefEditor.putInt("minRingerVolume", seekBar.getProgress()+1);
                prefEditor.commit();
            }


            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                String str = getResources().getString(R.string.setting_min_ringer_volume);
                tvMinRingerVolume.setText(str + " : " + String.valueOf(progress+1));
            }
        });

        final SeekBar SeekBarPocketVolume =(SeekBar) view.findViewById(R.id.SeekBarPocketVolume);
        SeekBarPocketVolume.setProgress(settings.getInt("Ctrl.PocketVolume", 0));
        SeekBarPocketVolume.setMax(audiomanager.getMaxRingerVolume());
        audiomanager = null;

        SeekBarPocketVolume.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                SharedPreferences.Editor prefEditor = settings.edit();
                prefEditor.putInt("Ctrl.PocketVolume", seekBar.getProgress());
                prefEditor.commit();
            }


            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                String str = getResources().getString(R.string.tvPocketVolume);
                tvPocketVolume.setText(str + " " + String.valueOf(progress));
            }
        });

        return view;
    }

}
