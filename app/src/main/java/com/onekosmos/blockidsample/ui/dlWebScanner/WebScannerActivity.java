package com.onekosmos.blockidsample.ui.dlWebScanner;

import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_CONNECTION_ERROR;
import static com.onekosmos.blockid.sdk.document.BIDDocumentProvider.RegisterDocCategory.identity_document;
import static com.onekosmos.blockid.sdk.document.RegisterDocType.DL;

import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.utils.BIDUtil;
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
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                mProgressBar.setVisibility(View.VISIBLE);
                mTxtPlsWait.setVisibility(View.VISIBLE);
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, final String url) {
                mProgressBar.setVisibility(View.GONE);
                mTxtPlsWait.setVisibility(View.GONE);
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {
            // Grant permissions for cam
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                WebScannerActivity.this.runOnUiThread(new Runnable() {
                    @TargetApi(Build.VERSION_CODES.M)
                    @Override
                    public void run() {
                        request.grant(request.getResources());
                    }
                });
            }
        });

        mWebView.loadUrl(webUrl);
        verifySessionStatus(sessionId);
    }

    private void verifySessionStatus(String sessionId) {
        mDisableBackPress = true;
        SessionApi.getInstance().checkSessionStatus(sessionId, this, (status, response, error) -> {
            if (!status) {
                if (error != null && error.getCode() == K_CONNECTION_ERROR.getCode()) {
                    return;
                }
                return;
            } else {
                LinkedHashMap<String, Object> driverLicenseMap = new LinkedHashMap<>();
                driverLicenseMap.put("category", response.getDl_object().getCategory());
                driverLicenseMap.put("type", response.getDl_object().getType());
                driverLicenseMap.put("id", response.getDl_object().getId());

                driverLicenseMap.put("proofedBy", response.getDl_object().getProofedBy());
                driverLicenseMap.put("documentId", response.getDl_object().getDocumentId());
                driverLicenseMap.put("documentType", response.getDl_object().getDocumentType());
                driverLicenseMap.put("firstName", response.getDl_object().getFirstName());
                driverLicenseMap.put("lastName", response.getDl_object().getLastName());
                driverLicenseMap.put("familyName", response.getDl_object().getFamilyName());
                driverLicenseMap.put("middleName", response.getDl_object().getMiddleName());
                driverLicenseMap.put("givenName", response.getDl_object().getGivenName());
                driverLicenseMap.put("fullName", response.getDl_object().getFullName());
                driverLicenseMap.put("dob", response.getDl_object().getDob());
                driverLicenseMap.put("doe", response.getDl_object().getDoe());
                driverLicenseMap.put("doi", response.getDl_object().getDoi());
                driverLicenseMap.put("face", response.getDl_object().getFace());
                driverLicenseMap.put("image", response.getDl_object().getImage());
                driverLicenseMap.put("imageBack", response.getDl_object().getImageBack());
                driverLicenseMap.put("gender", response.getDl_object().getGender());
                driverLicenseMap.put("height", response.getDl_object().getHeight());
                driverLicenseMap.put("street", response.getDl_object().getStreet());
                driverLicenseMap.put("city", response.getDl_object().getCity());
                driverLicenseMap.put("restrictionCode", response.getDl_object().getRestrictionCode());
                driverLicenseMap.put("residenceCity", response.getDl_object().getResidenceCity());
                driverLicenseMap.put("state", response.getDl_object().getState());
                driverLicenseMap.put("country", response.getDl_object().getCountry());
                driverLicenseMap.put("zipCode", response.getDl_object().getZipCode());
                driverLicenseMap.put("residenceZipCode", response.getDl_object().getResidenceZipCode());
                driverLicenseMap.put("classificationCode", response.getDl_object().getClassificationCode());
                driverLicenseMap.put("complianceType", response.getDl_object().getComplianceType());
                driverLicenseMap.put("placeOfBirth", response.getDl_object().getPlaceOfBirth());

                BlockIDSDK.getInstance().registerDocument(this, driverLicenseMap,
                        null, (registerStatus, err) -> {
//                            progressDialog.dismiss();
//                            isRegistrationInProgress = false;
                            if (registerStatus) {
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
