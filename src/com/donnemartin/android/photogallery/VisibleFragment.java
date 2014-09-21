package com.donnemartin.android.photogallery;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;
import android.util.Log;

public abstract class VisibleFragment extends Fragment {
    // Dynamic broadcast receiver.
    // Generic fragment that hides foreground notifications

    public static final String TAG = "VisibleFragment";

    // The receiver is typically defined as an inner instance like a
    // button click listener
    // We need mOnShowNotification for onResume and onPause, so save it
    private BroadcastReceiver mOnShowNotification = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // if we receive this, we're visible, so cancel
            // the notification
            Log.i(TAG, "canceling notification");
            setResultCode(Activity.RESULT_CANCELED);
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        // Can also create this in XML as an <intent-filter>
        IntentFilter filter =
            new IntentFilter(PollService.ACTION_SHOW_NOTIFICATION);
        getActivity().registerReceiver(mOnShowNotification,
                                       filter,
                                       PollService.PERM_PRIVATE,
                                       null);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mOnShowNotification);
    }
}
