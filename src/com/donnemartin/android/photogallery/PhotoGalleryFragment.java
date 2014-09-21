package com.donnemartin.android.photogallery;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.*;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SearchView;

import java.util.ArrayList;

public class PhotoGalleryFragment extends VisibleFragment {

    GridView mGridView;
    ArrayList<GalleryItem> mItems;
    ThumbnailDownloader mThumbnailThread;

    private static final String TAG = "PhotoGalleryFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
        setHasOptionsMenu(true);

        updateItems();

        // By default, handler will attach itself to the looper of the current
        // thread, which is the main thread
        mThumbnailThread = new ThumbnailDownloader(new Handler());
        mThumbnailThread
            .setListener(new ThumbnailDownloader.Listener<ImageView>() {
            public void onThumbnailDownloaded(ImageView imageView,
                                              Bitmap thumbnail) {
                // isVisibile ensures we are not setting the image on a stale
                // ImageView
                if (isVisible()) {
                    imageView.setImageBitmap(thumbnail);
                }
            }
        });
        mThumbnailThread.start();

        // Ensure the thread's guts are ready before proceeding
        mThumbnailThread.getLooper();
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_gallery,
                                     container,
                                     false);

        mGridView = (GridView)view.findViewById(R.id.gridView);

        // Ensure we configure the appropriate adapter when the new GridView
        // is created, such as on a rotation.
        setupAdapter();

        mGridView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> gridView,
                                    View view,
                                    int pos,
                                    long id) {
                GalleryItem item = mItems.get(pos);

                Uri photoPageUri = Uri.parse(item.getPhotoPageUrl());
                Intent i = new Intent(Intent.ACTION_VIEW, photoPageUri);

                startActivity(i);
            }
        });

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // It's critical to quit the HandlerThread otherwise it will never die
        mThumbnailThread.quit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailThread.clearQueue();
    }

    @Override
    @TargetApi(11)
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Pull out the SearchView
            MenuItem searchItem = menu.findItem(R.id.menu_item_search);
            SearchView searchView = (SearchView)searchItem.getActionView();

            // Get the data from our searchable.xml as a SearchableInfo
            // SearchManager is a system serice that is responsible for all
            // things search related
            SearchManager searchManager = (SearchManager)getActivity()
                .getSystemService(Context.SEARCH_SERVICE);
            ComponentName name = getActivity().getComponentName();
            // Root around in the manifest, package up the relevant info
            SearchableInfo searchInfo = searchManager.getSearchableInfo(name);

            searchView.setSearchableInfo(searchInfo);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Note, SearchView does not generate any events here
        // These callbacks are only for older devices
        boolean selectionHandled = false;

        switch (item.getItemId()) {
            case R.id.menu_item_search:
                getActivity().onSearchRequested();
                selectionHandled = true;
                break;
            case R.id.menu_item_clear:
                PreferenceManager.getDefaultSharedPreferences(getActivity())
                        .edit()
                        .putString(FlickrConn.PREF_SEARCH_QUERY, null)
                        .commit();
                updateItems();
                selectionHandled = true;
                break;
            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm =
                    !PollService.isServiceAlarmOn(getActivity());
                PollService.setServiceAlarm(getActivity(), shouldStartAlarm);

                // On 3.0 and later devices, the action bar does not
                // automatically update itself
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    getActivity().invalidateOptionsMenu();
                }

                selectionHandled = true;
                break;
            default:
                selectionHandled = super.onOptionsItemSelected(item);
        }

        return selectionHandled;
    }

    @Override
    // Called whenever the menu needs to be configured
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if (PollService.isServiceAlarmOn(getActivity())) {
            toggleItem.setTitle(R.string.stop_polling);
        } else {
            toggleItem.setTitle(R.string.start_polling);
        }
    }

    public void updateItems() {
        // Start AsyncTask to fire up a background thread and calls
        // doInBackground() method
        new FetchItemsTask().execute();
    }

    void setupAdapter() {
        // GridView has no GridFragment class, so we have to build our own
        // adapter management.  We look at the current model state and configure
        // the adapter appropriately

        // Fragments can exist unattached from any activity, which can occur
        // since we are using AyncTask.
        if (getActivity() != null && mGridView != null) {
            if (mItems != null) {
                mGridView.setAdapter(new GalleryItemAdapter(mItems));
            } else {
                mGridView.setAdapter(null);
            }
        }
    }

    private class FetchItemsTask
        extends AsyncTask<Void,Void,ArrayList<GalleryItem>> {

        @Override
        protected ArrayList<GalleryItem> doInBackground(Void... params) {
            ArrayList<GalleryItem> list;
            Activity activity = getActivity();

            if (activity == null) {
                list = new ArrayList<GalleryItem>();
            } else {
                String query = PreferenceManager
                    .getDefaultSharedPreferences(activity)
                    .getString(FlickrConn.PREF_SEARCH_QUERY, null);

                if (query != null) {
                    list = new FlickrConn().search(query);
                }
                else {
                    list = new FlickrConn().fetchItems();
                }
            }

            return list;
        }

        @Override
        protected void onPostExecute(ArrayList<GalleryItem> items) {
            // onPostExecute is run after doInBackground completes and is run
            // on the main thread, not the background thread so it is safe
            // to update the UI within it
            mItems = items;

            if (items.size() > 0) {
                String resultId = items.get(0).getId();
                PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .edit()
                    .putString(FlickrConn.PREF_LAST_RESULT_ID, resultId)
                    .commit();
            }

            // Configure the appropriate adapter when we update our items
            setupAdapter();
        }
    }

    private class GalleryItemAdapter extends ArrayAdapter<GalleryItem> {
        public GalleryItemAdapter(ArrayList<GalleryItem> items) {
            super(getActivity(), 0, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater()
                        .inflate(R.layout.gallery_item, parent, false);
            }

            GalleryItem item = getItem(position);
            ImageView imageView = (ImageView)convertView
                .findViewById(R.id.gallery_item_imageView);
            imageView.setImageResource(R.drawable.black);
            mThumbnailThread.queueThumbnail(imageView, item.getUrl());

            return convertView;
        }
    }
}
