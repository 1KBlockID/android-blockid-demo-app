package com.onekosmos.blockidsample.ui.liveID;

import static android.Manifest.permission.CAMERA;

import android.content.DialogInterface.OnDismissListener;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.ErrorResponse;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.cameramodule.camera.liveIDModule.ILiveIDResponseListener;
import com.onekosmos.blockid.sdk.cameramodule.liveID.LiveIDScannerHelper;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.util.AppPermissionUtils;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.ProgressDialog;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2023 1Kosmos. All rights reserved.
 */
public class PassiveLiveIDScanningActivity extends AppCompatActivity
        implements ILiveIDResponseListener {
    private final String[] K_CAMERA_PERMISSION = new String[]{CAMERA};
    private static final int K_LIVEID_PERMISSION_REQUEST_CODE = 1009;
    private LiveIDScannerHelper mLiveIDScannerHelper;
    private ProgressDialog mProgressDialog;
    private AppCompatImageView mImgBack;
    private AppCompatTextView mTxtBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passive_live_idscanning);
        initView();
        // Check for camera permission
        if (!AppPermissionUtils.isPermissionGiven(K_CAMERA_PERMISSION, this)) {
            AppPermissionUtils.requestPermission(this, K_LIVEID_PERMISSION_REQUEST_CODE,
                    K_CAMERA_PERMISSION);
        } else {
            // Camera permission is granted start LiveID scanning
            startLiveIDScan();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // Stop LiveID scanning
        mLiveIDScannerHelper.stopLiveIDScanning();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Dismiss the progress dialog
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.cancel();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (AppPermissionUtils.isGrantedPermission(this, requestCode, grantResults,
                K_CAMERA_PERMISSION)) {
            // Camera permission is granted start LiveID scanning
            startLiveIDScan();
        } else {
            // Show error camera permission is not granted
            ErrorDialog errorDialog = new ErrorDialog(this);
            errorDialog.show(null, null,
                    getString(R.string.label_liveid_camera_permission_alert), dialog -> finish());
        }
    }

    /**
     * Initialize UI Objects
     */
    private void initView() {
        mProgressDialog = new ProgressDialog(this, getString(R.string.label_verify_liveid));
        mImgBack = findViewById(R.id.img_back_liveid_passive);
        mImgBack.setOnClickListener(view -> onBackPressed());

        mTxtBack = findViewById(R.id.txt_back_liveid_passive);
        mTxtBack.setOnClickListener(view -> onBackPressed());
    }

    /**
     * Start LiveID scanning
     * Implement ILiveIDResponseListener interface for the Activity
     */
    private void startLiveIDScan() {
        mLiveIDScannerHelper = new LiveIDScannerHelper(this, this);
        mLiveIDScannerHelper.startLiveIDScanning();
    }

    // LiveID scanning response
    @Override
    public void onLiveIDCaptured(Bitmap liveIDBitmap, String signatureToken, ErrorResponse error) {
        // Stop LiveID scanning
        mLiveIDScannerHelper.stopLiveIDScanning();

        mImgBack.setClickable(false);
        mTxtBack.setClickable(false);
        if (mProgressDialog.isShowing())
            mProgressDialog.dismiss();

        ErrorDialog errorDialog = new ErrorDialog(this);
        OnDismissListener onDismissListener = dialogInterface -> {
            errorDialog.dismiss();
            finish();
        };

        // LiveID scanning failed
        if (liveIDBitmap == null) {
            // User canceled the LiveID scanning process
            if (error.getCode() == ErrorManager.CustomErrors.K_SCAN_CANCELLED.getCode()) {
                finish();
            }

            // Liveness check API call failed because of device is offline
            if (error.getCode() == ErrorManager.CustomErrors.K_CONNECTION_ERROR.getCode()) {
                errorDialog.showNoInternetDialog(onDismissListener);
                return;
            }

            // LiveID liveness check failed, Get error message and code and show it
            String stringError = (error.getObject() != null) ? error.getObject() : "";
            errorDialog.show(null, getString(R.string.label_error), "(" + error.getCode() + ") " +
                    error.getMessage() + "\n" + stringError, onDismissListener);
            return;
        }

        // LiveID scanned successful and register LiveID
        registerLiveID(liveIDBitmap);
    }

    // LiveID Liveness check is in progress
    @Override
    public void onLivenessCheckStarted() {
        if (!isFinishing()) {
            mProgressDialog.show();
        }
    }

    /**
     * Register LiveID
     *
     * @param livIdBitmap LiveID image received from LiveID scanner
     */
    private void registerLiveID(Bitmap livIdBitmap) {
        mProgressDialog = new ProgressDialog(this, getString(R.string.label_please_wait));
        mProgressDialog.show();
        BlockIDSDK.getInstance().setLiveID(livIdBitmap, null, null,
                (status, message, error) -> {
                    mProgressDialog.dismiss();

                    // Register LiveID failed
                    if (!status) {
                        ErrorDialog errorDialog = new ErrorDialog(this);
                        OnDismissListener onDismissListener = dialogInterface -> {
                            errorDialog.dismiss();
                            finish();
                        };

                        // show offline error
                        if (error.getCode() == ErrorManager.CustomErrors.K_CONNECTION_ERROR.getCode()) {
                            errorDialog.showNoInternetDialog(onDismissListener);
                            return;
                        }
                        // show error
                        errorDialog.show(null, getString(R.string.label_error), error.getMessage(),
                                onDismissListener);
                        return;
                    }

                    // LiveID registered successfully
                    Toast.makeText(this,
                            getString(R.string.label_liveid_enrolled_successfully),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
    }
}