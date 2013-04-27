package com.github.matt.williams.argolf;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class AboutActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        ((WebView)findViewById(R.id.webview)).loadUrl("file:///android_asset/about.html");
    }
}
