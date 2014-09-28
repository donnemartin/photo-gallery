package com.donnemartin.android.photogallery;

public class GalleryItem {

    private String mCaption;
    private String mId;
    private String mUrl;
    private String mOwner;

    @SuppressWarnings("UnusedDeclaration")
    public String getCaption() {
        return mCaption;
    }
    public void setCaption(String caption) {
        mCaption = caption;
    }
    public String getId() {
        return mId;
    }
    public void setId(String id) {
        mId = id;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getOwner() {
        return mOwner;
    }

    public void setOwner(String owner) {
        mOwner = owner;
    }

    public String getPhotoPageUrl() {
        return "http://www.flickr.com/photos/" + mOwner + "/" + mId;
    }
    @Override
    public String toString() {
        return mCaption;
    }
}
