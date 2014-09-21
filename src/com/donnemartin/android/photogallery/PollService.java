package com.donnemartin.android.photogallery;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

// A service's intents are called commands, which are serviced off a queue
// When it receives its first command, the IntentService starts up, fires up
// a background thread, and puts the command on a queue.  It then services
// each command in order, calling onHandleIntent on its background thread
// for each command.  The service is destroyed when there are no commands left.
public class PollService extends IntentService {

    private static final String TAG = "PollService";

    private static final int MS_PER_SEC = 1000;
    private static final int SEC_PER_MIN = 1;
    private static final int POLL_INTERVAL = MS_PER_SEC * SEC_PER_MIN * 5;
    public static final String PREF_IS_ALARM_ON = "isAlarmOn";

    // Send the broadcast notification only when the application is closed
    public static final String ACTION_SHOW_NOTIFICATION =
        "com.donnemartin.android.photogallery.SHOW_NOTIFICATION";

    // Ensure we are the only app that can trigger VisibleFragment's receiver
    public static final String PERM_PRIVATE =
        "com.donnemartin.android.photogallery.PRIVATE";

    public PollService() {
        super(TAG);
    }

    @Override
    public void onHandleIntent(Intent intent) {
        // Since Android provides the ability for a user to turn off networking
        // for background apps, we need to verify this is available
        ConnectivityManager cm = (ConnectivityManager)
            getSystemService(Context.CONNECTIVITY_SERVICE);

        @SuppressWarnings("deprecation")
        // Pre ICS, if you did not check for getBackgroundDataSetting, you
        // could still use the network.  Post ICS, the background data setting
        // for users disables networking entirely, which requires you to check
        // getActiveNetworkInfo.  getActiveNetworkInfo requires
        // ACCESS_NETWORK_STATE in the manifest
        boolean isNetworkAvailable = cm.getBackgroundDataSetting() &&
            cm.getActiveNetworkInfo() != null;

        if (isNetworkAvailable) {
            // Pull out the current query and the last result ID from the
            // default SharedPreferences
            SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(this);
            String query = prefs.getString(FlickrConn.PREF_SEARCH_QUERY, null);
            String lastResultId =
                prefs.getString(FlickrConn.PREF_LAST_RESULT_ID, null);

            ArrayList<GalleryItem> items;

            // Fetch the latest result set
            if (query != null) {
                items = new FlickrConn().search(query);
            }
            else {
                items = new FlickrConn().fetchItems();
            }

            if (items.size() > 0) {
                // Grab the first result
                String resultId = items.get(0).getId();

                if (!resultId.equals(lastResultId)) {
                    Log.i(TAG, "Got a new result: " + resultId);

                    Resources res = getResources();
                    PendingIntent pi = PendingIntent
                        .getActivity(
                            this,
                            0,
                            new Intent(this, PhotoGalleryActivity.class),
                            0);

                    Notification notification =
                        new NotificationCompat.Builder(this)
                        .setTicker(res.getString(R.string.new_pictures_title))
                        .setSmallIcon(android.R.drawable.ic_menu_report_image)
                        .setContentTitle(
                            res.getString(R.string.new_pictures_title))
                        .setContentText(
                            res.getString(R.string.new_pictures_text))
                        .setContentIntent(pi)
                        .setAutoCancel(true)
                        .build();

                    showBackgroundNotification(0, notification);
                }

                // Store the first result back in SharedPreferences
                prefs.edit()
                        .putString(FlickrConn.PREF_LAST_RESULT_ID, resultId)
                        .commit();
            }
        }
    }

    // Static method to keep the alarm code with other code in PollService that
    // it is related to and allows other components to invoke it.  Usually
    // you want to turn an alarm on and off from front end code in a fragment
    // or other controller.
    public static void setServiceAlarm(Context context, boolean isOn) {
        // Construct PendingIntent that starts PollService
        // getService packages up an invocation of Context.startService
        Intent intent = new Intent(context, PollService.class);
        PendingIntent pi = PendingIntent.getService(context, 0, intent, 0);

        // AlarmManager will keep firing intents to start PollService over and
        // over even if the app is exited
        AlarmManager alarmManager = (AlarmManager)
            context.getSystemService(Context.ALARM_SERVICE);

        if (isOn) {
            // Set the alarm
            alarmManager.setRepeating(AlarmManager.RTC,
                                      System.currentTimeMillis(),
                                      POLL_INTERVAL,
                                      pi);
        } else {
            // Can only register one alarm for each PendingIntent
            alarmManager.cancel(pi);
            pi.cancel();
        }

        // Set alarm flag that the StartupReceiver can use to turn the alarm
        // on or off at boot
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(PollService.PREF_IS_ALARM_ON, isOn)
            .commit();
    }

    public static boolean isServiceAlarmOn(Context context) {
        Intent intent = new Intent(context, PollService.class);
        // FLAG_NO_CREATE says if the PendingIntent does not already exist,
        // return null instead of creating it
        PendingIntent pi = PendingIntent.getService(
            context, 0, intent, PendingIntent.FLAG_NO_CREATE);
        return pi != null;
    }

    private static BroadcastReceiver sNotificationReceiver =
        new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            Log.i(TAG, "received result: " + getResultCode());
            if (getResultCode() != Activity.RESULT_OK)
                // a foreground activity cancelled the broadcast
                return;

            int requestCode = intent.getIntExtra("REQUEST_CODE", 0);
            Notification notification =
                (Notification)intent.getParcelableExtra("NOTIFICATION");

            NotificationManager notificationManager = (NotificationManager)
                c.getSystemService(NOTIFICATION_SERVICE);

            // requestCode is the identifier for the notification and should
            // unique across the application.  You can implement a progress
            // bar or dynamic visual by passing in a second notification with
            // the same ID
            notificationManager.notify(requestCode, notification);
        }
    };

    void showBackgroundNotification(int requestCode,
                                    Notification notification) {
        Intent intent = new Intent(ACTION_SHOW_NOTIFICATION);
        intent.putExtra("REQUEST_CODE", requestCode);
        intent.putExtra("NOTIFICATION", notification);

        // Regular broadcast intents are received by everyone at the same time.
        // Since onReceive is executed on the main thread, receivers are not
        // executed concurrently and are not executed in a particular oder.
        // Thus, it is difficult for broadcast receivers to communicate with
        // each other or for the sender of the intent to receive info from the
        // receivers
        // Ordered broadcast intents allow two-way communication by allowing a
        // sequence of broadcast receivers to process a broadcast intent in
        // order.  They also allow the sender of a broadcast to receive results
        // from the broadcast's recipients by passing in a special broadcast
        // receiver (result receiver).
        sendOrderedBroadcast(intent,
                             PERM_PRIVATE,
                             sNotificationReceiver,
                             null,
                             Activity.RESULT_OK,
                             null,
                             null);
    }
}
