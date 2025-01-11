package com.shajikhan.ladspa.amprack;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Objects;

public class NAMDownloader extends AppCompatActivity {
    String TAG = getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_namdownloader);
        ProgressBar progressBar = findViewById(R.id.progress);
        getSupportActionBar().hide();

        progressBar.setIndeterminate(true);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        WebView webView = findViewById(R.id.webview);
        webView.loadUrl("https://tonehunt.org");

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                Log.d(TAG, "onDownloadStart: " + url);
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
                Log.d(TAG, "onLoadResource: " + url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
//                view.loadUrl(request.getUrl().toString());
                Log.i(TAG, "shouldOverrideUrlLoading: " + request.toString());
                Log.d(TAG, "shouldOverrideUrlLoading: " + request.getUrl());
                progressBar.setVisibility(View.VISIBLE);
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                Log.d(TAG, "onPageFinished: " + url);
//                String js = "(function () {return JSON.parse (document.forms [1].children[2].value)[0].downloadUrl})()";
//                webView.evaluateJavascript(js, new ValueCallback<String>() {
//                    @Override
//                    public void onReceiveValue(String value) {
//                        Log.d(TAG, "onReceiveValue: " + value);
//                    }
//                });
            }
        });

    }
}