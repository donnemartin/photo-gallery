package com.donnemartin.android.photogallery;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String TAG = "NotificationReceiver";

    @Override
    public void onReceive(Context c, Intent i) {
        Log.i(TAG, "received result: " + getResultCode());

        // A foreground activity might have cancelled the broadcast
        if (getResultCode() == Activity.RESULT_OK) {
            int requestCode = i.getIntExtra("REQUEST_CODE", 0);
            Notification notification =
                (Notification) i.getParcelableExtra("NOTIFICATION");

            NotificationManager notificationManager = (NotificationManager)
                c.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(requestCode, notification);
        }
    }
}
