package com.onekosmos.blockidsample.ui.dlWebScanner;

import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_CONNECTION_ERROR;
import static com.onekosmos.blockid.sdk.document.BIDDocumentProvider.RegisterDocCategory.identity_document;
import static com.onekosmos.blockid.sdk.document.RegisterDocType.DL;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.document.DocumentHolder;
import com.onekosmos.blockidsample.ui.liveID.LiveIDScanningActivity;
import com.onekosmos.blockidsample.util.ErrorDialog;

import java.util.LinkedHashMap;

public class WebScannerActivity extends AppCompatActivity {
    private WebView mWebView;
    private ProgressBar mProgressBar;
    private AppCompatTextView mTxtPlsWait;
    private boolean mDisableBackPress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_web_scanner);
        initView();
    }

    private void initView() {
        mWebView = findViewById(R.id.web_view);
        mProgressBar = findViewById(R.id.progress_bar);
        mTxtPlsWait = findViewById(R.id.txt_please_wait);
        mProgressBar.setVisibility(View.VISIBLE);
        mTxtPlsWait.setVisibility(View.VISIBLE);

        SessionApi.getInstance().createSession(this, (status, response, error) -> {
            if (!status) {
                if (error.getCode() == K_CONNECTION_ERROR.getCode()) {
                    return;
                }
                return;
            } else {
                String webUrl = response.getUrl();
                loadWebView(webUrl, response.getSessionId());
            }
        });
    }

    private void loadWebView(String webUrl, String sessionId) {
        mWebView.loadUrl(webUrl);
        verifySessionStatus(sessionId);

//        mWebView.setWebViewClient(new WebViewClient() {
//            @Override
//            public void onPageFinished(WebView view, String url) {
//                super.onPageFinished(view, url);
//                mWebView.setVisibility(View.VISIBLE);
//                Toast.makeText(WebScannerActivity.this, "hi", Toast.LENGTH_SHORT).show();
//                //  verifySessionStatus();
//            }
////            @Override
////            public boolean shouldOverrideUrlLoading(WebView view, String url) {
////
////                return true;
////            }
//        });
//      //  mWebView.getSettings().setLoadsImagesAutomatically(true);
//        mWebView.getSettings().setJavaScriptEnabled(true);
//      //  mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
//        mWebView.loadUrl(webUrl);

    }

    private void verifySessionStatus(String sessionId) {
        mDisableBackPress = true;
        SessionApi.getInstance().checkSessionStatus(sessionId, this, (status, response, error) -> {
            if (!status) {
//                if (error.getCode() == K_CONNECTION_ERROR.getCode()) {
//                    return;
//                }
                return;
            } else {
                Toast.makeText(WebScannerActivity.this, "done", Toast.LENGTH_SHORT).show();
                LinkedHashMap<String, Object> driverLicenseMap = new LinkedHashMap<>();
                driverLicenseMap.put("category", identity_document.name());
                driverLicenseMap.put("type", DL.getValue());
                driverLicenseMap.put("id", driverLicenseMap.get("id"));
                BlockIDSDK.getInstance().registerDocument(this, driverLicenseMap,
                        null, (registerStatus, err) -> {
//                            progressDialog.dismiss();
//                            isRegistrationInProgress = false;
                            if (status) {
                                mDisableBackPress = false;
                                Toast.makeText(this, R.string.label_dl_enrolled_successfully,
                                        Toast.LENGTH_LONG).show();
                                finish();
                                return;
                            }

                            if (error.getCode() == ErrorManager.CustomErrors.K_LIVEID_IS_MANDATORY.getCode()) {
                                DocumentHolder.setData(driverLicenseMap, null);
                                Intent intent = new Intent(this, LiveIDScanningActivity.class);
                                intent.putExtra(LiveIDScanningActivity.LIVEID_WITH_DOCUMENT, true);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                startActivity(intent);
                                finish();
                                return;
                            }

                            ErrorDialog errorDialog = new ErrorDialog(this);
                            DialogInterface.OnDismissListener onDismissListener = dialogInterface -> {
                                errorDialog.dismiss();
                                finish();
                            };
                            if (error.getCode() == ErrorManager.CustomErrors.K_CONNECTION_ERROR.getCode()) {
                                errorDialog.showNoInternetDialog(onDismissListener);
                                return;
                            }
                            errorDialog.show(null, getString(R.string.label_error), error.getMessage(), onDismissListener);
                        });
            }
        });
    }

    public void onBackPressed() {
        if (mDisableBackPress = false)
            super.onBackPressed();
    }

}
