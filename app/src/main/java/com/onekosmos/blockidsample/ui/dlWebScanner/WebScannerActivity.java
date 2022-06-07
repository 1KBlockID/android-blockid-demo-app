package com.onekosmos.blockidsample.ui.dlWebScanner;

import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_CONNECTION_ERROR;
import static com.onekosmos.blockidsample.ui.dlWebScanner.SessionApi.K_CHECK_SESSION_STATUS;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import com.androidnetworking.AndroidNetworking;
import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.document.DocumentHolder;
import com.onekosmos.blockidsample.ui.liveID.LiveIDScanningActivity;
import com.onekosmos.blockidsample.util.AppPermissionUtils;
import com.onekosmos.blockidsample.util.ErrorDialog;

import java.util.LinkedHashMap;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2022 1Kosmos. All rights reserved.
 */
public class WebScannerActivity extends AppCompatActivity {
    private WebView mWebView;
    private ProgressBar mProgressBar;
    private AppCompatTextView mTxtPlsWait;
    private boolean mDisableBackPress;
    private final String[] K_CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private static final int K_WEB_SCANNER_PERMISSION_REQUEST_CODE = 1012;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_web_scanner);
        initView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!AppPermissionUtils.isPermissionGiven(K_CAMERA_PERMISSION, this))
            AppPermissionUtils.requestPermission(this, K_WEB_SCANNER_PERMISSION_REQUEST_CODE,
                    K_CAMERA_PERMISSION);
        else
            initWebSDK();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (AppPermissionUtils.isGrantedPermission(this, requestCode, grantResults,
                K_CAMERA_PERMISSION)) {
            initWebSDK();
        } else {
            ErrorDialog errorDialog = new ErrorDialog(this);
            errorDialog.show(null,
                    "",
                    getString(R.string.label_camera_permission_alert), dialog -> {
                        errorDialog.dismiss();
                        finish();
                    });
        }
    }

    private void initView() {
        mWebView = findViewById(R.id.web_view);
        mProgressBar = findViewById(R.id.progress_bar);
        mTxtPlsWait = findViewById(R.id.txt_please_wait);
        mProgressBar.setVisibility(View.VISIBLE);
        mTxtPlsWait.setVisibility(View.VISIBLE);
    }

    private void initWebSDK() {
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

        mWebView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // chromium, enable hardware acceleration
            mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else {
            // older android version, disable hardware acceleration
            mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        mWebView.setVisibility(View.VISIBLE);
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
            // Grant permissions
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
        SessionApi.getInstance().checkSessionStatus(sessionId, this, (status, response, error) -> {
            if (!status) {
                if (error != null && error.getCode() == K_CONNECTION_ERROR.getCode()) {
                    return;
                }
                return;
            } else {
                // mDisableBackPress = true;
                LinkedHashMap<String, Object> driverLicenseMap = createDLData(response);
                BlockIDSDK.getInstance().registerDocument(this, driverLicenseMap,
                        null, (registerStatus, err) -> {
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
        super.onBackPressed();
        AndroidNetworking.cancel(K_CHECK_SESSION_STATUS);
        SessionApi.getInstance().stopPolling();
    }

    private LinkedHashMap<String, Object> createDLData(SessionApi.SessionStatusDecryptedData dlData) {
        LinkedHashMap<String, Object> driverLicenseMap = new LinkedHashMap<>();
        driverLicenseMap.put("category", dlData.getDl_object().getCategory());
        driverLicenseMap.put("type", dlData.getDl_object().getType());
        driverLicenseMap.put("id", dlData.getDl_object().getId());
        driverLicenseMap.put("proofedBy", dlData.getDl_object().getProofedBy());
        driverLicenseMap.put("documentId", dlData.getDl_object().getDocumentId());
        driverLicenseMap.put("documentType", dlData.getDl_object().getDocumentType());
        driverLicenseMap.put("firstName", dlData.getDl_object().getFirstName());
        driverLicenseMap.put("lastName", dlData.getDl_object().getLastName());
        driverLicenseMap.put("familyName", dlData.getDl_object().getFamilyName());
        driverLicenseMap.put("middleName", dlData.getDl_object().getMiddleName());
        driverLicenseMap.put("givenName", dlData.getDl_object().getGivenName());
        driverLicenseMap.put("fullName", dlData.getDl_object().getFullName());
        driverLicenseMap.put("dob", dlData.getDl_object().getDob());
        driverLicenseMap.put("doe", dlData.getDl_object().getDoe());
        driverLicenseMap.put("doi", dlData.getDl_object().getDoi());
        driverLicenseMap.put("face", dlData.getDl_object().getFace());
        driverLicenseMap.put("image", dlData.getDl_object().getImage());
        driverLicenseMap.put("imageBack", dlData.getDl_object().getImageBack());
        driverLicenseMap.put("gender", dlData.getDl_object().getGender());
        driverLicenseMap.put("height", dlData.getDl_object().getHeight());
        driverLicenseMap.put("street", dlData.getDl_object().getStreet());
        driverLicenseMap.put("city", dlData.getDl_object().getCity());
        driverLicenseMap.put("restrictionCode", dlData.getDl_object().getRestrictionCode());
        driverLicenseMap.put("residenceCity", dlData.getDl_object().getResidenceCity());
        driverLicenseMap.put("state", dlData.getDl_object().getState());
        driverLicenseMap.put("country", dlData.getDl_object().getCountry());
        driverLicenseMap.put("zipCode", dlData.getDl_object().getZipCode());
        driverLicenseMap.put("residenceZipCode", dlData.getDl_object().getResidenceZipCode());
        driverLicenseMap.put("classificationCode", dlData.getDl_object().getClassificationCode());
        driverLicenseMap.put("complianceType", dlData.getDl_object().getComplianceType());
        driverLicenseMap.put("placeOfBirth", dlData.getDl_object().getPlaceOfBirth());
        return driverLicenseMap;
    }
}
