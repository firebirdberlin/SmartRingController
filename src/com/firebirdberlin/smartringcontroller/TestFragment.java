package com.firebirdberlin.smartringcontrollerpro;

import android.app.Fragment;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Date;
import java.text.DateFormat;

public class TestFragment extends Fragment {

    private TextView txtView;
    private NotificationReceiver nReceiver;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.testfragment, container, false);
        txtView = (TextView) v.findViewById(R.id.textView);
        nReceiver = new NotificationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.firebirdberlin.smartringcontroller.NOTIFICATION_LISTENER");
        getActivity().registerReceiver(nReceiver,filter);
        return v;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // notification receiver
        getActivity().unregisterReceiver(nReceiver);
    }

    class NotificationReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            if (intent.hasExtra("notification_event")){
                String temp = intent.getStringExtra("notification_event") + "\n" + txtView.getText();

                Date date = new Date();
                DateFormat dateFormat = android.text.format.DateFormat.getTimeFormat(getActivity());
                txtView.setText(dateFormat.format(date) + " " +temp);
            }
        }
    }

}
