package com.donnemartin.android.photogallery;

import android.app.SearchManager;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;

public class PhotoGalleryActivity extends SingleFragmentActivity {

    private static final String TAG = "PhotoGalleryActivity";

    @Override
    public Fragment createFragment() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onNewIntent(Intent intent) {
        // Search is implemented using the singleTop launch mode.  Instead of
        // launching a new activity, the search intent will go to this
        // PhotoGalleryActivity on top of the back stack
        //
        // Note, if you need the new intent value, save it somewhere
        // getIntent() will have the old intent (return the intent that started
        // this activity)
        PhotoGalleryFragment fragment = (PhotoGalleryFragment)
            getSupportFragmentManager()
            .findFragmentById(R.id.fragmentContainer);

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Log.i(TAG, "Received a new search query: " + query);

            // Get the instance of SharedPreferences (with a default name and
            // private permissions) that is shared across the entire app
            // SharedPreferences.Editor stashes values into SharedPreferences
            // and allows us to group sets of changes together in transactions,
            // which is helpful if you have alot of changes.
            PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString(FlickrConn.PREF_SEARCH_QUERY, query)
                .commit();
        }

        // Whenever we receive a new intent, we want to refresh the items
        fragment.updateItems();
    }
}
