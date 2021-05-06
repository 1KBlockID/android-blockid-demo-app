package com.onekosmos.blockidsdkdemo.ui.liveID;

import android.Manifest;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.blockid.sdk.BlockIDSDK;
import com.blockid.sdk.cameramodule.BIDScannerView;
import com.blockid.sdk.cameramodule.ScanningMode;
import com.blockid.sdk.cameramodule.camera.liveIDModule.ILiveIDResponseListener;
import com.blockid.sdk.cameramodule.liveID.LiveIDScannerHelper;
import com.example.blockidsdkdemo.R;
import com.onekosmos.blockidsdkdemo.AppVault;
import com.onekosmos.blockidsdkdemo.util.AppPermissionUtils;
import com.onekosmos.blockidsdkdemo.util.ErrorDialog;
import com.onekosmos.blockidsdkdemo.util.ImageProcessingUtil;
import com.onekosmos.blockidsdkdemo.util.ProgressDialog;

/**
 * Created by Pankti Mistry on 30-04-2021.
 * Copyright © 2020 1Kosmos. All rights reserved.
 */
public class LiveIDScanningActivity extends AppCompatActivity implements View.OnClickListener, ILiveIDResponseListener {
    private final String[] K_CAMERA_PERMISSION = new String[]{
            Manifest.permission.CAMERA};
    private static final int K_LIVEID_PERMISSION_REQUEST_CODE = 1009;
    private AppCompatImageView mImgBack, mIvSuccess;
    private AppCompatTextView mTxtBack, mTxtPlsWait, mTxtMessage;
    private AppCompatButton mBtnCancel;
    private BIDScannerView mBIDScannerView;
    private LiveIDScannerHelper mLiveIDScannerHelper;
    private AppCompatImageView mScannerOverlay;
    private int mScannerOverlayMargin = 30;
    private ProgressBar mProgressBar;
    private LinearLayout mLayoutMessage;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_liveid_scan);

        initViews();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!AppPermissionUtils.isPermissionGiven(K_CAMERA_PERMISSION, this))
            AppPermissionUtils.requestPermission(this, K_LIVEID_PERMISSION_REQUEST_CODE, K_CAMERA_PERMISSION);
        else {
            mBIDScannerView.setVisibility(View.VISIBLE);
            mScannerOverlay.setVisibility(View.VISIBLE);

            mLiveIDScannerHelper = new LiveIDScannerHelper(this, ScanningMode.SCAN_LIVE, this, mBIDScannerView, mScannerOverlay);
            mLiveIDScannerHelper.startLiveIDScanning();
        }
    }

    private void initViews() {
        mBIDScannerView = findViewById(R.id.view_bid_scanner);
        mScannerOverlay = findViewById(R.id.view_overlay);
        mBIDScannerView.setScannerWidthMargin(mScannerOverlayMargin, mScannerOverlay);

        if (AppPermissionUtils.isPermissionGiven(K_CAMERA_PERMISSION, this)) {
            mBIDScannerView.setVisibility(View.VISIBLE);
        }

        mTxtMessage = findViewById(R.id.txt_msg);
        mLayoutMessage = findViewById(R.id.layout_msg);
        mIvSuccess = findViewById(R.id.iv_success);
        mProgressBar = findViewById(R.id.progress_bar);
        mTxtPlsWait = findViewById(R.id.txt_please_wait);

        mImgBack = findViewById(R.id.img_back);
        mImgBack.setOnClickListener(this);

        mTxtBack = findViewById(R.id.txt_back);
        mTxtBack.setOnClickListener(this);

        mBtnCancel = findViewById(R.id.btn_cancel);
        mBtnCancel.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.img_back:
            case R.id.txt_back:
            case R.id.btn_cancel:
                finish();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (AppPermissionUtils.isGrantedPermission(this, requestCode, grantResults, K_CAMERA_PERMISSION)) {
            mBIDScannerView.setVisibility(View.VISIBLE);
            mScannerOverlay.setVisibility(View.VISIBLE);
            mLiveIDScannerHelper = new LiveIDScannerHelper(this, ScanningMode.SCAN_LIVE, this, mBIDScannerView, mScannerOverlay);
            mLiveIDScannerHelper.startLiveIDScanning();
        } else {
            ErrorDialog errorDialog = new ErrorDialog(this);
            errorDialog.show(null,
                    "",
                    getString(R.string.label_liveid_camera_permission_alert), dialog -> {
                        finish();
                    });
        }
    }

    @Override
    public void onFaceFocusChanged(boolean isFocused, String expression) {
        if (isFocused) {
            showFaceFocusedViews();
            mLayoutMessage.setVisibility(View.VISIBLE);
            mTxtMessage.setVisibility(View.VISIBLE);
            mTxtMessage.setText(getMessgaeForExpression(expression));
            mIvSuccess.setVisibility(View.GONE);
        } else
            showFaceNotFocusedViews();
    }

    @Override
    public void onLiveIDCaptured(Bitmap bitmap, String signatureToken, ErrorManager.ErrorResponse error) {
        mTxtMessage.setVisibility(View.GONE);
        mLayoutMessage.setVisibility(View.GONE);
        mLiveIDScannerHelper.stopLiveIDScanning();
        mProgressBar.setVisibility(View.VISIBLE);
        mTxtPlsWait.setVisibility(View.VISIBLE);
        mBtnCancel.setClickable(false);
        mImgBack.setClickable(false);
        mTxtBack.setClickable(false);
        mBIDScannerView.setVisibility(View.GONE);
        mScannerOverlay.setVisibility(View.GONE);
        mBtnCancel.setVisibility(View.GONE);
        // call enrollLiveID func here

        ErrorDialog errorDialog = new ErrorDialog(this);
        if (bitmap == null) {
            errorDialog.show(null,
                    getString(R.string.label_error),
                    error.getMessage(), dialog -> {
                        errorDialog.dismiss();
                        finish();
                    });
            return;
        }
        registerLiveID(bitmap);
    }

    private void showFaceNotFocusedViews() {
        mScannerOverlay.setImageResource(R.drawable.group_3);
        mScannerOverlay.setColorFilter(getResources().getColor(R.color.misc2));
    }

    private void showFaceFocusedViews() {
        mScannerOverlay.setImageResource(R.drawable.group_3);
        mScannerOverlay.setColorFilter(getResources().getColor(R.color.misc1));
    }

    public void onStop() {
        super.onStop();
    }

    private String getMessgaeForExpression(String expression) {
        switch (expression) {
            case "Blink":
                return getResources().getString(R.string.label_liveid_please_blink_your_eyes);
            case "Smile":
                return getResources().getString(R.string.label_liveid_please_smile);
        }
        return "";
    }

    private void registerLiveID(Bitmap bitmap) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();
        BlockIDSDK.getInstance().setLiveID(bitmap, "", (enroll_status, msg, errorResponse) -> {
            progressDialog.dismiss();
            if (!enroll_status) {
                if (errorResponse != null) {
                    ErrorDialog errorDialog = new ErrorDialog(this);
                    errorDialog.show(null,
                            getString(R.string.label_error),
                            errorResponse.getMessage(), dialog -> {
                                dialog.dismiss();
                                finish();
                            });
                }
            } else {
                AppVault.getInstance().setLiveID(ImageProcessingUtil.getBitMapToBase64(bitmap));
                Toast.makeText(this, getString(R.string.label_liveid_enrolled_successfully), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }
}