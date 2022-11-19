package com.ctg.coptok.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ctg.coptok.R;
import com.ctg.coptok.utils.LocaleUtil;

public class PaymentActivity extends AppCompatActivity {

    private static final String TAG = "PaymentActivity";

    private WebView mWebView;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleUtil.wrap(base));
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        findViewById(R.id.header_back).setOnClickListener(v -> finish());
        TextView title = findViewById(R.id.header_title);
        title.setText(R.string.payment_label);
        findViewById(R.id.header_more).setVisibility(View.GONE);
        mWebView = findViewById(R.id.webview);
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.v(TAG, "Page " + url + " has finished loading.");
                String path = Uri.parse(url).getPath();
                if (path.contains("wallet/response")) {
                    finish();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView webview, String url) {
                if (url.startsWith("intent://") || url.startsWith("upi://")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                    return true;
                }
                return false;
            }
        });
        mWebView.loadUrl(getIntent().getDataString());
    }
}
