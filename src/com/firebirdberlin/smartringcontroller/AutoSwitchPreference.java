
package com.firebirdberlin.smartringcontrollerpro;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.preference.SwitchPreference;

public class AutoSwitchPreference extends SwitchPreference {
    private Context mContext = null;

    public AutoSwitchPreference(Context context) {
        super(context);
        mContext = context;
    }
    public AutoSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }
    public AutoSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    @Override
    protected void onClick() {
        Intent mIntent = getIntent();
        if (mIntent == null) {
            return;
        }
        mContext.startActivity(mIntent);
        ((Activity) mContext).overridePendingTransition(0, 0);
    }
}
