package com.onekosmos.blockidsample.ui.qrAuth;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.blockid.sdk.cameramodule.BIDScannerView;
import com.blockid.sdk.cameramodule.QRCodeScanner.QRScannerHelper;
import com.blockid.sdk.cameramodule.ScanningMode;
import com.blockid.sdk.cameramodule.camera.qrCodeModule.IOnQRScanResponseListener;
import com.onekosmos.blockidsample.R;
import com.google.gson.Gson;
import com.onekosmos.blockidsample.util.AppPermissionUtils;
import com.onekosmos.blockidsample.util.ErrorDialog;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class ScanQRCodeActivity extends AppCompatActivity implements IOnQRScanResponseListener {
    private AppCompatTextView mTxtBack, mTxtPleaseWait;
    private ProgressBar mProgressBar;
    private final String[] K_CAMERA_PERMISSION = new String[]{
            Manifest.permission.CAMERA};
    private static final int K_QR_CODE_PERMISSION_REQUEST_CODE = 1008;
    private QRScannerHelper mQRScannerHelper;
    private BIDScannerView mBIDScannerView;
    private RelativeLayout mScannerOverlay;
    private LinearLayout mScannerView;
    private AppCompatImageView mImgBack;
    private int mScannedViewWidthMargin = 50;
    private static final String K_AUTH_REQUEST_MODEL = "authRequestModel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qrcode);
        initView();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!AppPermissionUtils.isPermissionGiven(K_CAMERA_PERMISSION, this))
            AppPermissionUtils.requestPermission(this, K_QR_CODE_PERMISSION_REQUEST_CODE, K_CAMERA_PERMISSION);
        else if (!(mProgressBar.getVisibility() == View.VISIBLE)) {
            mBIDScannerView.setVisibility(View.VISIBLE);
            mScannerOverlay.setVisibility(View.VISIBLE);
            mQRScannerHelper = new QRScannerHelper(this, ScanningMode.SCAN_LIVE, this, mBIDScannerView);
            mQRScannerHelper.startQRScanning();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (AppPermissionUtils.isGrantedPermission(requestCode, grantResults, K_CAMERA_PERMISSION, this)) {
            mQRScannerHelper = new QRScannerHelper(this, ScanningMode.SCAN_LIVE, this, mBIDScannerView);
            mQRScannerHelper.startQRScanning();
            mBIDScannerView.setVisibility(View.VISIBLE);
            mScannerOverlay.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void onQRScanResultResponse(String qrCodeData) {
        if (mQRScannerHelper.isRunning()) {
            mQRScannerHelper.stopQRScanning();
            runOnUiThread(() -> onQRCodeScanResponse(qrCodeData));
        }
    }

    private void initView() {
        mScannerView = findViewById(R.id.scanner_view);
        mBIDScannerView = findViewById(R.id.bid_scanner_view);
        mScannerOverlay = findViewById(R.id.scanner_overlay);
        mBIDScannerView.setScannerWidthMargin(mScannedViewWidthMargin, mScannerOverlay);
        if (AppPermissionUtils.isPermissionGiven(K_CAMERA_PERMISSION, this)) {
            mBIDScannerView.setVisibility(View.VISIBLE);
            mScannerOverlay.setVisibility(View.VISIBLE);
        }
        mTxtBack = findViewById(R.id.txt_back);
        mTxtPleaseWait = findViewById(R.id.txt_please_wait);
        mProgressBar = findViewById(R.id.progress_bar_register);

        mImgBack = findViewById(R.id.img_back);
        mTxtBack.setOnClickListener(view -> onBackPressed());
        mImgBack.setOnClickListener(view -> onBackPressed());
    }

    private void onQRCodeScanResponse(String qrResponseB64String) {
        ErrorDialog errorDialog = new ErrorDialog(ScanQRCodeActivity.this);
        try {
            mBIDScannerView.setVisibility(View.INVISIBLE);
            mScannerOverlay.setVisibility(View.INVISIBLE);
            mScannerView.setVisibility(View.INVISIBLE);
            mProgressBar.setVisibility(View.VISIBLE);
            mTxtPleaseWait.setVisibility(View.VISIBLE);
            String qrResponseString = new String(Base64.decode(qrResponseB64String, Base64.NO_WRAP));
            AuthRequestModel authRequestModel = new Gson().fromJson(qrResponseString, AuthRequestModel.class);
            String mQRScopes = authRequestModel.scopes.toLowerCase();
            mQRScopes = mQRScopes.replace("windows", "scep_creds");
            authRequestModel.scopes = mQRScopes;
            Intent i = new Intent();
            i.putExtra(K_AUTH_REQUEST_MODEL, new Gson().toJson(authRequestModel));
            setResult(RESULT_OK, i);
            this.finish();
        } catch (Exception e) {
            mProgressBar.setVisibility(View.GONE);
            errorDialog.show(null,
                    getString(R.string.label_invalid_code),
                    getString(R.string.label_unsupported_qr_code), dialog -> {
                        setResult(RESULT_CANCELED);
                        finish();
                    });
        }
    }
}