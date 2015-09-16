package com.firebirdberlin.smartringcontrollerpro;

import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class WelcomeFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.welcome, container, false);
        Context context = getActivity();
        TextView tv = (TextView) v.findViewById(R.id.tvWelcome);
        try{
            PackageInfo packageInfo = context.getPackageManager()
                                                   .getPackageInfo(context.getPackageName(), 0);
            tv.setText(packageInfo.applicationInfo.loadLabel(context.getPackageManager()).toString()
                       + " " + packageInfo.versionName + "\n\n" + tv.getText());
        } catch (NameNotFoundException e) {
            //should never happen
            return v;
        }
        return v;
    }
}
