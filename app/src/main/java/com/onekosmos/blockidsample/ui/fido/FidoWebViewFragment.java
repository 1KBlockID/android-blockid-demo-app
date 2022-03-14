package com.onekosmos.blockidsample.ui.fido;

import static android.webkit.WebSettings.LOAD_NO_CACHE;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.util.SharedPreferenceUtil;

import static com.onekosmos.blockidsample.util.SharedPreferenceUtil.K_PREF_FIDO2_USERNAME;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2022 1Kosmos. All rights reserved.
 */
public class FidoWebViewFragment extends Fragment {
    private WebView mWebView;
    private ProgressBar mProgressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_fido_web_view, container, false);

        initView(view);
        return view;
    }

    /**
     * Initialising view for FidoWebViewFragment class
     * @param view root view of FidoWebViewFragment
     */
    private void initView(View view) {
        mProgressBar = view.findViewById(R.id.progress_fido);
        mWebView = view.findViewById(R.id.web_view_fido);
        mWebView.getSettings().setAppCacheEnabled(false);
        mWebView.getSettings().setCacheMode(LOAD_NO_CACHE);
        mWebView.getSettings().setJavaScriptEnabled(true);

        WebAppInterface webAppInterface = new WebAppInterface(getContext());
        mWebView.addJavascriptInterface(webAppInterface, "Android");
        WebViewClient webViewClient = new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                mProgressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mProgressBar.setVisibility(View.GONE);
                webAppInterface.getUserName();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                Toast.makeText(getContext(), error.getDescription(), Toast.LENGTH_SHORT).show();
            }
        };

        mWebView.setWebViewClient(webViewClient);
        mWebView.loadUrl("file:///android_asset/fido.html");
    }

    private class WebAppInterface {
        private Context mContext;

        public WebAppInterface(Context context) {
            mContext = context;
        }

        @JavascriptInterface
        public void registerKey() {
            Toast.makeText(mContext, "Register: " +
                            SharedPreferenceUtil.getInstance().getString(K_PREF_FIDO2_USERNAME),
                    Toast.LENGTH_SHORT).show();
        }

        @JavascriptInterface
        public void authenticate() {
            Toast.makeText(mContext, "Authenticate: " +
                            SharedPreferenceUtil.getInstance().getString(K_PREF_FIDO2_USERNAME),
                    Toast.LENGTH_SHORT).show();
        }

        @JavascriptInterface
        public void manageKeys() {
            Toast.makeText(mContext, "Manage: " +
                            SharedPreferenceUtil.getInstance().getString(K_PREF_FIDO2_USERNAME),
                    Toast.LENGTH_SHORT).show();
        }

        @JavascriptInterface
        public String getUserName() {
            return SharedPreferenceUtil.getInstance().getString(K_PREF_FIDO2_USERNAME);
        }
    }
}