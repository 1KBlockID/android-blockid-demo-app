package com.onekosmos.blockidsample.ui.nationalID;

import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_SOMETHING_WENT_WRONG;
import static com.onekosmos.blockid.sdk.document.BIDDocumentProvider.RegisterDocCategory.identity_document;
import static com.onekosmos.blockid.sdk.document.RegisterDocType.NATIONAL_ID;
import static com.onekosmos.blockid.sdk.documentScanner.DocumentScannerActivity.K_DOCUMENT_SCAN_ERROR;
import static com.onekosmos.blockid.sdk.documentScanner.DocumentScannerActivity.K_DOCUMENT_SCAN_TYPE;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.documentScanner.DocumentScannerActivity;
import com.onekosmos.blockid.sdk.documentScanner.DocumentScannerType;
import com.onekosmos.blockid.sdk.utils.BIDUtil;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.document.DocumentHolder;
import com.onekosmos.blockidsample.ui.liveID.ActiveLiveIDScanningActivity;
import com.onekosmos.blockidsample.util.AppPermissionUtils;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.ProgressDialog;

import java.util.LinkedHashMap;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class NationalIDScanActivity extends AppCompatActivity {
    private static final int K_CAMERA_PERMISSION_REQUEST_CODE = 1011;
    private final String[] K_CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private AppCompatImageView mImgBack;
    private AppCompatTextView mTxtBack;
    private LinkedHashMap<String, Object> mNationalIDMap;
    private boolean isRegistrationInProgress;

    private final ActivityResultLauncher<Intent> documentSessionResult =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_CANCELED) {
                            ErrorManager.ErrorResponse error;
                            if (result.getData() != null) {
                                error = BIDUtil.JSONStringToObject(
                                        result.getData().getStringExtra(K_DOCUMENT_SCAN_ERROR),
                                        ErrorManager.ErrorResponse.class);
                                if (error != null) {
                                    showError(error);
                                } else {
                                    error = new ErrorManager.ErrorResponse(K_SOMETHING_WENT_WRONG.getCode(),
                                            K_SOMETHING_WENT_WRONG.getMessage());
                                    showError(error);
                                }
                            } else {
                                finish();
                            }
                            return;
                        }
                        //Process document data and Register Document
                        // Call registerNationalID()
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nationalid_scanning);
        initView();
        if (!AppPermissionUtils.isPermissionGiven(K_CAMERA_PERMISSION, this))
            AppPermissionUtils.requestPermission(this, K_CAMERA_PERMISSION_REQUEST_CODE, K_CAMERA_PERMISSION);
        else {
            startScan();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (AppPermissionUtils.isGrantedPermission(this, requestCode, grantResults, K_CAMERA_PERMISSION)) {
            startScan();
        } else {
            ErrorDialog errorDialog = new ErrorDialog(this);
            errorDialog.show(null,
                    "",
                    getString(R.string.label_camera_permission_alert), dialog -> {
                        errorDialog.dismiss();
                        setResult(RESULT_CANCELED);
                        finish();
                    });
        }
    }

    @Override
    public void onBackPressed() {
        if (!isRegistrationInProgress)
            super.onBackPressed();
    }

    private void initView() {
        mImgBack = findViewById(R.id.img_back);
        mImgBack.setOnClickListener(v -> onBackPressed());

        mTxtBack = findViewById(R.id.txt_back);
        mTxtBack.setOnClickListener(v -> onBackPressed());
    }

    private void registerNationalID() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();
        isRegistrationInProgress = true;
        mImgBack.setClickable(false);
        mTxtBack.setClickable(false);
        mNationalIDMap.put("category", identity_document.name());
        mNationalIDMap.put("type", NATIONAL_ID.getValue());
        mNationalIDMap.put("id", mNationalIDMap.get("id"));
        BlockIDSDK.getInstance().registerDocument(this, mNationalIDMap, null,
                (status, error) -> {
                    progressDialog.dismiss();
                    isRegistrationInProgress = false;
                    if (status) {
                        Toast.makeText(this, R.string.label_nid_enrolled_successfully, Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    if (error.getCode() == ErrorManager.CustomErrors.K_LIVEID_IS_MANDATORY.getCode()) {
                        DocumentHolder.setData(mNationalIDMap, null);
                        Intent intent = new Intent(this, ActiveLiveIDScanningActivity.class);
                        intent.putExtra(ActiveLiveIDScanningActivity.LIVEID_WITH_DOCUMENT, true);
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

    private void startScan() {
        Intent intent = new Intent(this, DocumentScannerActivity.class);
        intent.putExtra(K_DOCUMENT_SCAN_TYPE, DocumentScannerType.ID.getValue());
        documentSessionResult.launch(intent);
    }

    /**
     * Show Error Dialog
     * @param errorResponse = {@link ErrorManager.ErrorResponse}
     */
    private void showError(ErrorManager.ErrorResponse errorResponse) {
        ErrorDialog errorDialog = new ErrorDialog(this);
        if (errorResponse.getCode() == 0) {
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
                    errorResponse.getMessage(), dialog -> {
                        errorDialog.dismiss();
                        finish();
                    });
        }
    }
}
