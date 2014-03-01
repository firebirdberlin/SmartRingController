package com.firebirdberlin.smartringcontrollerpro;

import android.app.Fragment;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.Date;
import java.text.DateFormat;

public class TTSFragment extends Fragment {
	public static final int TTS_MODE_HEADPHONES 	= 0;
	public static final int TTS_MODE_ALWAYS 		= 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.ttsfragment, container, false);

        final SharedPreferences settings = getActivity().getSharedPreferences(SmartRingController.PREFS_KEY, 0);
		//txtView = (TextView) v.findViewById(R.id.textView);

		boolean handleTTS 		= settings.getBoolean("TTS.enabled", false);
		int TTS_mode 			= settings.getInt("TTS.mode", TTS_MODE_HEADPHONES);

		final CompoundButton switch_handle_TTS 	= (CompoundButton) view.findViewById(R.id.switchTTSenabled);

		switch_handle_TTS.setChecked(handleTTS);
		switch_handle_TTS.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(final CompoundButton buttonView,
					final boolean isChecked) {
				SharedPreferences.Editor prefEditor = settings.edit();
				prefEditor.putBoolean("TTS.enabled", isChecked);
				prefEditor.commit();
			}
		});

		final RadioButton radio_TTS_always		= (RadioButton) view.findViewById(R.id.radio_TTS_always);
		final RadioButton radio_TTS_headphones	= (RadioButton) view.findViewById(R.id.radio_TTS_headphones);

		switch (TTS_mode){
			case TTS_MODE_ALWAYS:
					radio_TTS_always.setChecked(true);
					break;
			case TTS_MODE_HEADPHONES:
					radio_TTS_headphones.setChecked(true);
					break;
		}

        return view;
    }

	@Override
	public void onDestroy() {
		super.onDestroy();

	}

}
