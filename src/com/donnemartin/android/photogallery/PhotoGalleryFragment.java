package com.donnemartin.android.photogallery;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;

import java.util.ArrayList;

public class PhotoGalleryFragment extends Fragment {

    GridView mGridView;
    ArrayList<GalleryItem> mItems;

    private static final String TAG = "PhotoGalleryFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        // Start AsyncTask to fire up a background thread and calls
        // doInBackground() method
        new FetchItemsTask().execute();
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

        return view;
    }

    void setupAdapter() {
        // GridView has no GridFragment class, so we have to build our own
        // adapter management.  We look at the current model state and configure
        // the adapter appropriately

        // Fragments can exist unattached from any activity, which can occur
        // since we are using AyncTask.
        if (getActivity() != null && mGridView != null) {
            if (mItems != null) {
                mGridView.setAdapter(new ArrayAdapter<GalleryItem>(
                    getActivity(),
                    android.R.layout.simple_gallery_item, mItems));
            }
            else {
                mGridView.setAdapter(null);
            }
        }
    }

    private class FetchItemsTask
        extends AsyncTask<Void,Void,ArrayList<GalleryItem>> {

        @Override
        protected ArrayList<GalleryItem> doInBackground(Void... params) {
            return new FlickrConn().fetchItems();
        }

        @Override
        protected void onPostExecute(ArrayList<GalleryItem> items) {
            // onPostExecute is run after doInBackground completes and is run
            // on the main thread, not the background thread so it is safe
            // to update the UI within it
            mItems = items;

            // Configure the appropriate adapter when we update our items
            setupAdapter();
        }
    }
}
