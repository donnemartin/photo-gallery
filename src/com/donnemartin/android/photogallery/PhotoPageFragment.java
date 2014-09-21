package com.donnemartin.android.photogallery;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

public class PhotoPageFragment extends VisibleFragment {

    private String mUrl;
    private WebView mWebView;
    private static final int MAX_PROGRESS = 100;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mUrl = getActivity().getIntent().getData().toString();
    }

    // Flickr requires JavaScript to work if using a WebView
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup parent,
                             Bundle savedInstanceState) {
        View view =
            inflater.inflate(R.layout.fragment_photo_page, parent, false);

        final ProgressBar progressBar =
            (ProgressBar)view.findViewById(R.id.progressBar);
        progressBar.setMax(MAX_PROGRESS);
        final TextView titleTextView =
            (TextView)view.findViewById(R.id.titleTextView);

        mWebView = (WebView)view.findViewById(R.id.webView);

        // Flickr requirement to enable JavaScript
        mWebView.getSettings().setJavaScriptEnabled(true);

        // WebClientView allows you to respond to rendering events like detect
        // when rendering starts or decide whether to submit a POST request
        // to the server.
        mWebView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Tell WebViewClient to go ahead and load the URL, we are not
                // doing anything custom with it.
                return false;
            }
        });
        mWebView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView webView, int progress) {
                if (progress == MAX_PROGRESS) {
                    progressBar.setVisibility(View.INVISIBLE);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(progress);
                }
            }

            public void onReceivedTitle(WebView webView, String title) {
                titleTextView.setText(title);
            }
        });

        // Needs to be done after configuring the WebView
        mWebView.loadUrl(mUrl);

        return view;
    }
}
