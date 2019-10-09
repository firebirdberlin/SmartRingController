package com.firebirdberlin.smartringcontrollerpro;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.text.Html;
import android.text.Spanned;
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
        TextView tv = v.findViewById(R.id.tvWelcome);
        String htmlAsString = getString(R.string.tvWelcome);      // used by WebView
        Spanned htmlAsSpanned = Html.fromHtml(htmlAsString);
        tv.setText(htmlAsSpanned);

        try{
            PackageInfo packageInfo = context.getPackageManager()
                                                   .getPackageInfo(context.getPackageName(), 0);
            tv.setText(String.format("%s %s\n\n%s",
                    packageInfo.applicationInfo.loadLabel(context.getPackageManager()).toString(),
                    packageInfo.versionName,
                    tv.getText()));
        } catch (NameNotFoundException e) {
            //should never happen
            return v;
        }
        return v;
    }
}
