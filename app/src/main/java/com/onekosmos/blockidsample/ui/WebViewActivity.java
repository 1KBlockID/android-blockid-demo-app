package com.onekosmos.blockidsample.ui;

import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_CONNECTION_ERROR;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.ErrorResponse;
import com.onekosmos.blockidsample.R;

/*
 * @author Sarthak Mishra.Created on 17/10/2023.
 * Copyright © 2020 1Kosmos. All rights reserved.
 */
@Keep
public class WebViewActivity extends AppCompatActivity {
    private WebView mDocumentScannerWebView;
    private ConstraintLayout mWebViewLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        initView();
    }


    /**
     * Initialize UI Objects
     */
    private void initView() {
        mWebViewLayout = findViewById(R.id.layout_document_scanner_webview);
        mWebViewLayout.setVisibility(View.GONE);

        AppCompatTextView mTxtCancel = findViewById(R.id.txt_document_scanner_cancel);
        mTxtCancel.setText(getString(R.string.label_cancel));
        mTxtCancel.setOnClickListener(v -> onBackPressed());

        mDocumentScannerWebView = findViewById(R.id.webview_document_scanner);

        initializeAndLoadWebView("https://1k-dev.1kosmos.net/idproofing/capture-demo");
    }

    /**
     * Initializes and load WebView with session URL
     *
     * @param url - Session URL
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void initializeAndLoadWebView(String url) {
        mDocumentScannerWebView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        mDocumentScannerWebView.getSettings().setJavaScriptEnabled(true);
        mDocumentScannerWebView.getSettings().setDomStorageEnabled(true);

        mDocumentScannerWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                request.grant(request.getResources());
            }

            @Override
            public void onPermissionRequestCanceled(PermissionRequest request) {
                super.onPermissionRequestCanceled(request);
            }
        });

        mDocumentScannerWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mWebViewLayout.setVisibility(View.VISIBLE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (error.getErrorCode() == -6) {
                    ErrorResponse errorResponse = new ErrorResponse(K_CONNECTION_ERROR.getCode(),
                            K_CONNECTION_ERROR.getMessage());
                    Toast.makeText(WebViewActivity.this, errorResponse.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                ErrorResponse errorResponse = new ErrorResponse(error.getErrorCode(),
                        String.valueOf(error.getDescription()));

                Toast.makeText(WebViewActivity.this, errorResponse.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
        mDocumentScannerWebView.loadUrl(url);
    }

}