package com.donnemartin.android.photogallery;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.util.ArrayList;

public class PhotoGalleryFragment extends Fragment {

    GridView mGridView;
    ArrayList<GalleryItem> mItems;
    ThumbnailDownloader<ImageView> mThumbnailThread;

    private static final String TAG = "PhotoGalleryFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        // Start AsyncTask to fire up a background thread and calls
        // doInBackground() method
        new FetchItemsTask().execute();

        // By default, handler will attach itself to the looper of the current
        // thread, which is the main thread
        mThumbnailThread = new ThumbnailDownloader<ImageView>(new Handler());
        mThumbnailThread.setListener(new ThumbnailDownloader.Listener<ImageView>() {
            public void onThumbnailDownloaded(ImageView imageView, Bitmap thumbnail) {
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

    void setupAdapter() {
        // GridView has no GridFragment class, so we have to build our own
        // adapter management.  We look at the current model state and configure
        // the adapter appropriately

        // Fragments can exist unattached from any activity, which can occur
        // since we are using AyncTask.
        if (getActivity() != null && mGridView != null) {
            if (mItems != null) {
                mGridView.setAdapter(new GalleryItemAdapter(mItems));
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
            imageView.setImageResource(R.drawable.ic_launcher);
            mThumbnailThread.queueThumbnail(imageView, item.getUrl());

            return convertView;
        }
    }
}
