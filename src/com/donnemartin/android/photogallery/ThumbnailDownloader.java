package com.donnemartin.android.photogallery;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

public class ThumbnailDownloader<Handle> extends HandlerThread {
    // Class to download thumbnails on demand to avoid isses when there is
    // a slow connection or a large number of thumbnails stored in memory
    // AsyncTask is the easiest way to get a background thread, but it is
    // not well suited for repetitive and long-running work.

    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;

    Handler mHandler;
    // Synchronized hash map.  Using Token as a key, store and retrieve the
    // URL associated with a particular Token.
    Map<Handle,String> requestMap =
            Collections.synchronizedMap(new HashMap<Handle,String>());
    Handler mResponseHandler;
    Listener<Handle> mListener;

    public interface Listener<Handle> {
        // Add a Listener interface to communicate the responses with
        void onThumbnailDownloaded(Handle handle, Bitmap thumbnail);
    }

    public void setListener(Listener<Handle> listener) {
        mListener = listener;
    }

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
    }

    // Supress warnings about subclassing Handler.
    // The hander will be kept alive by its Looper.  If your handler is an
    // anonymous inner class, it is easy to leak memory accidentally through an
    // implicit object reference.  However, everything here is tied to the
    // HandlerThread so there is no danger of leaking anything
    @SuppressLint("HandlerLeak")
    @Override
    protected void onLooperPrepared() {
        // onLooperPrepared is called before the Looper checks the queue
        // for the first time, a good place to create a Handler implementation
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    // Necessary because Token is a generic class argument but
                    // Message.obj is an Object.  Type erasure makes it
                    // impossible to make this cast
                    @SuppressWarnings("unchecked")
                    Handle handle = (Handle)msg.obj;
                    Log.i(TAG,
                          "Got a request for url: " + requestMap.get(handle));
                    handleRequest(handle);
                }
            }
        };
    }

    private void handleRequest(final Handle handle) {
        // Use FlickrConn to download bytes from the URL and turn them
        // into a bitmap
        try {
            final String url = requestMap.get(handle);
            if (url != null) {
                byte[] bitmapBytes = new FlickrConn().getUrlBytes(url);

                // Create a BitmapFactory to construct a bitmap with the array
                // of bytes
                final Bitmap bitmap = BitmapFactory
                        .decodeByteArray(bitmapBytes, 0, bitmapBytes.length);

                // Send a custom message back to the main thread
                // When a Message has its callback field set, instead of being
                // rn by its Handler target, the Runnable in callback is run
                // instead.  Because mResponseHandler is associated with the
                // main thread's Looper, this UI update code will be run on the
                // main thread
                mResponseHandler.post(new Runnable() {
                    public void run() {
                        // Double check the requestMap because the GridView
                        // recycles its views to ensure the Token gets the
                        // correct image, even if another request has been
                        // made in the meantime
                        if (requestMap.get(handle) == url) {
                            // Remove this token since we have handled it
                            requestMap.remove(handle);
                            // Set the bitmap on the token
                            mListener.onThumbnailDownloaded(handle, bitmap);
                        }
                    }
                });
            }
        } catch (IOException ioe) {
            Log.e(TAG, "Error downloading image", ioe);
        }
    }

    public void queueThumbnail(Handle handle, String url) {
        requestMap.put(handle, url);

        // Build the message using obtainMessage rather than building it by hand
        // ObtainMessage pulls from a common recycling pool to avoid creating
        // new message objects
        mHandler
                .obtainMessage(MESSAGE_DOWNLOAD, handle)
                .sendToTarget();
    }

    public void clearQueue() {
        // If the user rotates the screen, ThumbnailDownloader might have an
        // invalid ImageView.  Clean all requests out of the queue.
        mHandler.removeMessages(MESSAGE_DOWNLOAD);
        requestMap.clear();
    }
}
