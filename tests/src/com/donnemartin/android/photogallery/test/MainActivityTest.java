package com.donnemartin.android.photogallery.test;

import com.donnemartin.android.photogallery.PhotoGalleryActivity;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.Button;

public class MainActivityTest extends
    ActivityInstrumentationTestCase2<PhotoGalleryActivity> {

    Activity mainActivity = null;
    Button button = null;

    public MainActivityTest() {
        super(PhotoGalleryActivity.class);
    }
  
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mainActivity = getActivity();
    }
  
    public void testChangeText() {

    }
}
