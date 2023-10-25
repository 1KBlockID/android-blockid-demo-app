package com.onekosmos.blockidsample.ui.driverLicense;

import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_SCAN_ERROR;
import static com.onekosmos.blockid.sdk.DocumentScanner.DocumentScannerActivity.K_DOCUMENT_SCAN_ERROR;
import static com.onekosmos.blockid.sdk.DocumentScanner.DocumentScannerActivity.K_DOCUMENT_TYPE;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.ErrorResponse;
import com.onekosmos.blockid.sdk.DocumentScanner.DocumentScannerActivity;
import com.onekosmos.blockid.sdk.DocumentScanner.DocumentType;
import com.onekosmos.blockid.sdk.utils.BIDUtil;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.util.AppPermissionUtils;
import com.onekosmos.blockidsample.util.ErrorDialog;


/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class DriverLicenseScanActivity extends AppCompatActivity {
    private static final int K_DL_PERMISSION_REQUEST_CODE = 1011;
    private final String[] K_CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_license_scan);
        if (!AppPermissionUtils.isPermissionGiven(K_CAMERA_PERMISSION, this))
            AppPermissionUtils.requestPermission(this, K_DL_PERMISSION_REQUEST_CODE,
                    K_CAMERA_PERMISSION);
        else
            startScan();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (AppPermissionUtils.isGrantedPermission(this, requestCode, grantResults,
                K_CAMERA_PERMISSION)) {
            startScan();
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

    /**
     * Start Document Scanning
     */
    private void startScan() {
        Intent intent = new Intent(this, DocumentScannerActivity.class);
        intent.putExtra(K_DOCUMENT_TYPE, DocumentType.DL.getValue());
        documentSessionResult.launch(intent);
    }

    private final ActivityResultLauncher<Intent> documentSessionResult =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_CANCELED) {
                            if (result.getData() != null) {
                                ErrorResponse errorResponse = BIDUtil.JSONStringToObject(
                                        result.getData().getStringExtra(K_DOCUMENT_SCAN_ERROR),
                                        ErrorResponse.class);
                                if (errorResponse != null) {
                                    showError(errorResponse);
                                }
                            } else {
                                finish();
                            }
                            return;
                        }
                        // Show Details screen
                    });

    /**
     * Show Error Dialog
     * @param errorResponse {@link ErrorResponse}
     */
    private void showError(ErrorResponse errorResponse) {
        ErrorDialog errorDialog = new ErrorDialog(this);
        if (errorResponse.getCode() == 0 || errorResponse.getCode() == -6) {
            errorDialog.show(null,
                    getString(R.string.label_your_are_offline),
                    getString(R.string.label_please_check_your_internet_connection),
                    dialog -> {
                        errorDialog.dismiss();
                        finish();
                    });
        } else {
            errorDialog.show(null,
                    getString(R.string.label_error),
                    TextUtils.isEmpty(errorResponse.getMessage()) ? K_SCAN_ERROR.getMessage() :
                            errorResponse.getMessage(),
                    dialog -> {
                        errorDialog.dismiss();
                        finish();
                    });
        }
    }
}