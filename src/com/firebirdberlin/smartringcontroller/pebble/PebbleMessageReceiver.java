package com.firebirdberlin.smartringcontrollerpro;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.os.Bundle;
import de.greenrobot.event.EventBus;
import java.util.UUID;
import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

public class PebbleMessageReceiver extends BroadcastReceiver {
    private final static String TAG = SmartRingController.TAG + ".PebbleMessageReceiver";
    private final static UUID PEBBLE_APP_UUID = UUID.fromString("7100dca9-2d97-4ea9-a1a9-f27aae08d144");
    private static int TAG_WATCH_IS_PLUGGED = 4;
    private static int VALUE_WATCH_IS_PLUGGED = 1;
    private static int VALUE_WATCH_IS_UNPLUGGED = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.i(TAG, "Pebble message received!");
        dumpIntent(intent);

        Bundle bundle = intent.getExtras();
        Object uuid_object = bundle.get("uuid");
        UUID uuid = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        if (uuid_object != null) {
            uuid = (UUID) uuid_object;
        }

        Logger.i(TAG, uuid.toString());
        Logger.i(TAG, PEBBLE_APP_UUID.toString());
        if (! uuid.equals(PEBBLE_APP_UUID)){
            Logger.w(TAG, "NO");
            return;
        }

        Logger.w(TAG, "YES");
        String msg_data = bundle.getString("msg_data", "none");
        int watchIsPlugged = -1;
        try {
            JSONArray json = new JSONArray(msg_data);
            JSONObject first = json.getJSONObject(0);
            int key = first.getInt("key");
            int value = first.getInt("value");
            if (key == TAG_WATCH_IS_PLUGGED) {
                watchIsPlugged = value;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (watchIsPlugged == VALUE_WATCH_IS_PLUGGED) {
            Logger.w(TAG, "WATCH IS PLUGGED");
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
