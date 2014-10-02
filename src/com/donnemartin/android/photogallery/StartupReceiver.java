package com.donnemartin.android.photogallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class StartupReceiver extends BroadcastReceiver {
    // When this BroadcastReceiver wakes up, it will call onReceive then die.
    // Because it is short lived, you cannot use async APIs or register any
    // listeners because the receiver will not be alive longer than the length
    // of onReceive.  onReceive runs on the main thread, so no networking or
    // work with permanent storage.
    // BroadcastReceivers are useful for small plumbing code.

    private static final String TAG = "StartupReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Received broadcast intent: " + intent.getAction());

        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(context);
        boolean isOn = prefs.getBoolean(PollService.PREF_IS_ALARM_ON, false);
        PollService.setServiceAlarm(context, isOn);
    }
}
