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
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Settings.System;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;

import java.text.DateFormat;
public class SettingsFragment extends Fragment {


	private RelativeLayout main_layout;
	SharedPreferences settings;
	private TextView tvMinAmplitude;
	private TextView tvMaxAmplitude;
	private TextView tvMinRingerVolume;
	private TextView tvPocketVolume;

	private int minAmplitude;
	private int maxAmplitude;

	private SeekBar SeekBarMin;
	private SeekBar SeekBarMax;

	private CompoundButton switch_handle_notification;
	private CompoundButton switch_handle_vibration;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.settingsfragment, container, false);

		settings = getActivity().getSharedPreferences(SmartRingController.PREFS_KEY, 0);
		boolean enabled = settings.getBoolean("enabled", false);
		minAmplitude 	 = settings.getInt("minAmplitude", 0);
		maxAmplitude 	 = settings.getInt("maxAmplitude", 7000);
		int minRingerVolume  	= settings.getInt("minRingerVolume", 1);
		int addPocketVolume 	= settings.getInt("Ctrl.PocketVolume", 0);
		boolean FlipAction  	= settings.getBoolean("FlipAction", false);
		boolean ShakeAction 	= settings.getBoolean("ShakeAction", false);
		boolean PullOutAction  = settings.getBoolean("PullOutAction", false);

		main_layout = (RelativeLayout) view.findViewById(R.id.main_layout);

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
				// TODO Auto-generated method stub
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
				// TODO Auto-generated method stub
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				SharedPreferences.Editor prefEditor = settings.edit();
				maxAmplitude = seekBar.getProgress()+SeekBarMin.getProgress();
				prefEditor.putInt("maxAmplitude", maxAmplitude);
				prefEditor.putInt("minAmplitude", minAmplitude);
				prefEditor.commit();
			}


			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
				// TODO Auto-generated method stub
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
				// TODO Auto-generated method stub
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				SharedPreferences.Editor prefEditor = settings.edit();
				prefEditor.putInt("minRingerVolume", seekBar.getProgress()+1);
				prefEditor.commit();
			}


			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
				// TODO Auto-generated method stub
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
				// TODO Auto-generated method stub
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				SharedPreferences.Editor prefEditor = settings.edit();
				prefEditor.putInt("Ctrl.PocketVolume", seekBar.getProgress());
				prefEditor.commit();
			}


			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
				// TODO Auto-generated method stub
				String str = getResources().getString(R.string.tvPocketVolume);
				tvPocketVolume.setText(str + " " + String.valueOf(progress));
			}
		});


		if (LicenseCheck.isLicensed(getActivity()) == true){
			boolean handleVibration    = settings.getBoolean("handle_vibration", false);
			boolean handleNotification = settings.getBoolean("handle_notification", false);

			switch_handle_vibration 	= (CompoundButton) view.findViewById(R.id.SwitchHandleVibration);

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

			switch_handle_notification 	= (CompoundButton) view.findViewById(R.id.SwitchHandleNotification);
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
			cbBrokenProximitySensor.setChecked(
				settings.getBoolean("Ctrl.BrokenProximitySensor", true)
				);
			cbBrokenProximitySensor.setOnCheckedChangeListener(new OnCheckedChangeListener()	{
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
				{
					SharedPreferences.Editor prefEditor = settings.edit();
					prefEditor.putBoolean("Ctrl.BrokenProximitySensor", isChecked);
					prefEditor.commit();
				}

			});


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

			final CompoundButton switchPullOutAction 	= (CompoundButton) view.findViewById(R.id.switchPullOutAction);
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


		} else { // lite version
			View llpro 	= (View) view.findViewById(R.id.llProFeatures);
			llpro.setVisibility(View.GONE);
			//View llv 	= (View) view.findViewById(R.id.llHandleVibration);
			//View lln 	= (View) view.findViewById(R.id.llHandleNotification);
			//llv.setVisibility(View.GONE);
			//lln.setVisibility(View.GONE);
		}

		return view;
    }

}
