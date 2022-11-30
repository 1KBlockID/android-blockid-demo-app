package com.onekosmos.blockidsample.ui.liveID;

import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_SOMETHING_WENT_WRONG;

import android.Manifest;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;

import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.util.AppPermissionUtils;
import com.onekosmos.blockidsample.util.AppUtil;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.ProgressDialog;
import com.onekosmos.blockidsample.util.VerifyDocument;
import com.onekosmos.blockidsample.util.scanner.SelfieScannerHelper;

import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2022 1Kosmos. All rights reserved.
 */
public class SelfieScanningActivity extends AppCompatActivity {
    private final String[] K_CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private static final int K_LIVEID_PERMISSION_REQUEST_CODE = 1009;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selfie_scanning);
        initView();
        if (!AppPermissionUtils.isPermissionGiven(K_CAMERA_PERMISSION, this))
            AppPermissionUtils.requestPermission(this, K_LIVEID_PERMISSION_REQUEST_CODE,
                    K_CAMERA_PERMISSION);
        else {
            startLiveIDScan();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (AppPermissionUtils.isGrantedPermission(this, requestCode, grantResults,
                K_CAMERA_PERMISSION)) {
            startLiveIDScan();
        } else {
            ErrorDialog errorDialog = new ErrorDialog(this);
            errorDialog.show(null, "",
                    getString(R.string.label_liveid_camera_permission_alert), dialog -> finish());
        }
    }

    private void initView() {
        mProgressDialog = new ProgressDialog(this);
        AppCompatImageView mImgBack = findViewById(R.id.img_back_selfie);
        mImgBack.setOnClickListener(v -> onBackPressed());
    }

    private void startLiveIDScan() {
        SelfieScannerHelper selfieScannerHelper = new SelfieScannerHelper(this);
        selfieScannerHelper.scanSelfie(false, new SelfieScannerHelper.SelfieScanCallback() {
            @Override
            public void onCancelSelfieScan() {
                finish();
            }

            @Override
            public void onErrorSelfieScan(int errorCode, String errorMessage) {
                showError(new ErrorManager.ErrorResponse(errorCode, errorMessage));
            }

            @Override
            public void onFinishSelfieScan(LinkedHashMap<String, Object> selfieScanData) {
                checkLiveness(Objects.requireNonNull(selfieScanData.get("liveId")).toString());
            }
        });
    }

    private void checkLiveness(String liveIdBase64) {
        mProgressDialog.show(getString(R.string.label_validating_liveness));
        VerifyDocument.getInstance().checkLiveness(liveIdBase64, (status, errorResponse) -> {
            if (status) {
                registerLiveID(AppUtil.convertBase64ToBitmap(liveIdBase64));
            } else {
                mProgressDialog.dismiss();
                showError(errorResponse);
            }
        });
    }

    private void registerLiveID(Bitmap livIdBitmap) {
        mProgressDialog.show(getString(R.string.label_completing_your_registration));
        BlockIDSDK.getInstance().setLiveID(livIdBitmap, null, null, (status, msg, error) -> {
            mProgressDialog.dismiss();
            if (status) {
                Toast.makeText(this, getString(R.string.label_liveid_enrolled_successfully),
                        Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            if (error == null)
                error = new ErrorManager.ErrorResponse(K_SOMETHING_WENT_WRONG.getCode(),
                        K_SOMETHING_WENT_WRONG.getMessage());

            ErrorDialog errorDialog = new ErrorDialog(this);
            DialogInterface.OnDismissListener onDismissListener = dialogInterface -> {
                errorDialog.dismiss();
                finish();
            };
            if (error.getCode() == ErrorManager.CustomErrors.K_CONNECTION_ERROR.getCode()) {
                errorDialog.showNoInternetDialog(onDismissListener);
                return;
            }
            errorDialog.show(null, getString(R.string.label_error), error.getMessage(),
                    onDismissListener);
        });
    }

    private void showError(ErrorManager.ErrorResponse error) {
        if (error == null)
            error = new ErrorManager.ErrorResponse(K_SOMETHING_WENT_WRONG.getCode(),
                    K_SOMETHING_WENT_WRONG.getMessage());

        ErrorDialog errorDialog = new ErrorDialog(this);
        DialogInterface.OnDismissListener onDismissListener = dialogInterface -> {
            errorDialog.dismiss();
            setResult(RESULT_CANCELED);
            finish();
        };

        if (error.getCode() == ErrorManager.CustomErrors.K_CONNECTION_ERROR.getCode()) {
            errorDialog.showNoInternetDialog(onDismissListener);
            return;
        }
        errorDialog.show(null, getString(R.string.label_error),
                error.getMessage(), onDismissListener);
    }
}