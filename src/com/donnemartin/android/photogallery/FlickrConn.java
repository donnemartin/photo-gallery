package com.donnemartin.android.photogallery;

import android.net.Uri;
import android.util.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownServiceException;
import java.util.ArrayList;

public class FlickrConn {

    public static final String TAG = "FlickerConn";

    public static final String PREF_SEARCH_QUERY ="searchQuery";

    private static final String ENDPOINT =
        "https://api.flickr.com/services/rest/";
    private static final String METHOD_GET_RECENT = "flickr.photos.getRecent";
    private static final String METHOD_SEARCH = "flickr.photos.search";
    private static final String PARAM_EXTRAS = "extras";
    private static final String PARAM_TEXT = "text";

    // Specifies the name of the photo XML element used by XmlPullParser to
    // identify each photo in the XML
    private static final String XML_PHOTO = "photo";
    private static final String EXTRA_SMALL_URL = "url_s";

    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        // Cast to HttpURLConnection to give us HTTP-specific interfaces for
        // working with request methods, response codes, streaming methods, etc
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        ByteArrayOutputStream out = null;
        byte[] outBytes = null;

        try {

            // Connect to the endpoint (GET), use getOutputStream for POST
            InputStream in = connection.getInputStream();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                out = new ByteArrayOutputStream();
                int bytesRead = 0;
                byte[] buffer = new byte[1024];

                // Call read repeatedly until the connection runs out of data
                // InputStream will yield bytes as they are available
                while ((bytesRead = in.read(buffer)) > 0) {
                    out.write(buffer, 0, bytesRead);
                }
                out.close();
                outBytes = out.toByteArray();
            }
        } catch (SocketTimeoutException ste) {
            Log.e(TAG, "Socket timeout trying to read bytes", ste);
        } catch (UnknownServiceException use) {
            Log.e(TAG, "Unknown service exception trying to read bytes", use);
        } catch (IOException ioe) {
            Log.e(TAG, "IO Exception trying read bytes", ioe);
        } finally {
            connection.disconnect();
        }

        return outBytes;
    }

    String getUrl(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    public ArrayList<GalleryItem> downloadGalleryItems(String  url) {
        ArrayList<GalleryItem> items = new ArrayList<GalleryItem>();

        try {
            String xmlString = getUrl(url);
            Log.i(TAG, "Received xml: " + xmlString);

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new StringReader(xmlString));

            parseItems(items, parser);
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch items", ioe);
        } catch (XmlPullParserException xppe) {
            Log.e(TAG, "Failed to parse items", xppe);
        }
        return items;
    }

    public ArrayList<GalleryItem> fetchItems() {
        // Build the complete URL for the API request with Uri.Builder,
        // a convenience class for creating properly escaped parameterized
        // URLs
        String url = Uri.parse(ENDPOINT).buildUpon()
                .appendQueryParameter("method", METHOD_GET_RECENT)
                .appendQueryParameter("api_key", Creds.API_KEY)
                .appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
                .build().toString();
        return downloadGalleryItems(url);
    }

    public ArrayList<GalleryItem> search(String query) {
        String url = Uri.parse(ENDPOINT).buildUpon()
                .appendQueryParameter("method", METHOD_SEARCH)
                .appendQueryParameter("api_key", Creds.API_KEY)
                .appendQueryParameter(PARAM_EXTRAS, EXTRA_SMALL_URL)
                .appendQueryParameter(PARAM_TEXT, query)
                .build().toString();
        return downloadGalleryItems(url);
    }

    void parseItems(ArrayList<GalleryItem> items, XmlPullParser parser)
        throws XmlPullParserException, IOException {
        // XmlPullParser is used internally by Android to inflate layout files
        // It can also be used to parse GalleryItem objects
        //
        // Sample XML:
        // <photo id="14960257130" owner="76958130@N08" secret="204ab79ea2"
        // server="3861" farm="4" title="#tgif#s5" ispublic="1" isfriend="0"
        // isfamily="0" url_s="https://farm4.staticflickr.com/3861/14960257130_
        // 204ab79ea2_m.jpg" height_s="240" width_s="240" />
        int eventType = parser.next();

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG &&
                XML_PHOTO.equals(parser.getName())) {
                String id = parser.getAttributeValue(null, "id");
                String caption = parser.getAttributeValue(null, "title");
                String smallUrl = parser.getAttributeValue(null,
                                                           EXTRA_SMALL_URL);
                String owner = parser.getAttributeValue(null, "owner");

                GalleryItem item = new GalleryItem();
                item.setId(id);
                item.setCaption(caption);
                item.setUrl(smallUrl);
                item.setOwner(owner);
                items.add(item);
            }

            eventType = parser.next();
        }
    }
}