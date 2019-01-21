package com.firebirdberlin.smartringcontrollerpro.pebble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.firebirdberlin.smartringcontrollerpro.Logger;

import java.util.UUID;
import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

public class PebbleMessageReceiver extends BroadcastReceiver {
    private final static String TAG = "PebbleMessageReceiver";
    private final static UUID PEBBLE_APP_UUID = UUID.fromString("7100dca9-2d97-4ea9-a1a9-f27aae08d144");
    private static int TAG_WATCH_IS_PLUGGED = 4;
    private static int VALUE_WATCH_IS_PLUGGED = 1;
    private static int VALUE_WATCH_IS_UNPLUGGED = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        dumpIntent(intent);

        Bundle bundle = intent.getExtras();
        Object uuid_object = bundle.get("uuid");
        String msg_data = bundle.getString("msg_data", "none");

        if (! PEBBLE_APP_UUID.equals(uuid_object)){
            return;
        }

        int watchIsPlugged = -1;
        try {
            JSONArray json = new JSONArray(msg_data);
            for (int i = 0; i < json.length(); i++) {
                JSONObject jsonObject = json.getJSONObject(i);
                int key = jsonObject.getInt("key");
                int value = jsonObject.getInt("value");
                if (key == TAG_WATCH_IS_PLUGGED) {
                    watchIsPlugged = value;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (watchIsPlugged == VALUE_WATCH_IS_PLUGGED) {
            PebbleActions.unmute(context);
        } else if (watchIsPlugged == VALUE_WATCH_IS_UNPLUGGED) {
            PebbleActions.mute(context);
        }
    }

    public static void dumpIntent(Intent i){
        Bundle bundle = i.getExtras();
        if (bundle == null) return;
        for (String key : bundle.keySet()) {
            Object value = bundle.get(key);
            Logger.d(TAG, String.format("%s %s (%s)", key,
                            value.toString(), value.getClass().getName()));
        }
    }

}
