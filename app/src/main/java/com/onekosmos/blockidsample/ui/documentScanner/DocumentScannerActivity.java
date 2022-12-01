package com.onekosmos.blockidsample.ui.documentScanner;

import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_SOMETHING_WENT_WRONG;

import android.Manifest;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;

import com.idmetrics.dc.utils.DSError;
import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.util.AppPermissionUtils;
import com.onekosmos.blockidsample.util.AppUtil;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.ProgressDialog;
import com.onekosmos.blockidsample.util.VerifyDocument;
import com.onekosmos.blockidsample.util.scannerHelpers.DocumentScannerHelper;
import com.onekosmos.blockidsample.util.scannerHelpers.SelfieScannerHelper;

import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2022 1Kosmos. All rights reserved.
 */
public class DocumentScannerActivity extends AppCompatActivity {
    private final String[] K_CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private static final int K_CAMERA_PERMISSION_REQUEST_CODE = 1009;
    private static final String K_TYPE = "type";
    private static final String K_ID = "id";
    private LinkedHashMap<String, Object> mSelfieMap, mDocumentMap;
    private ProgressDialog mProgressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_scanner);
        initView();
        if (!AppPermissionUtils.isPermissionGiven(K_CAMERA_PERMISSION, this))
            AppPermissionUtils.requestPermission(this, K_CAMERA_PERMISSION_REQUEST_CODE,
                    K_CAMERA_PERMISSION);
        else {
            startDocumentScan();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
            startDocumentScan();
        } else {
            ErrorDialog errorDialog = new ErrorDialog(this);
            errorDialog.show(null, null,
                    getString(R.string.label_liveid_camera_permission_alert), dialog -> finish());
        }
    }

    /**
     * Initialise UI Objects
     */
    private void initView() {
        AppCompatImageView mImgBack = findViewById(R.id.img_back_document);
        mImgBack.setOnClickListener(v -> onBackPressed());
        mProgressDialog = new ProgressDialog(this);
    }

    /**
     * Start Document Scan
     */
    private void startDocumentScan() {
        DocumentScannerHelper documentScannerHelper = new DocumentScannerHelper(this);
        documentScannerHelper.startDocumentScan(DocumentScannerHelper.DocumentScannerType.DL,
                new DocumentScannerHelper.DocumentScanCallback() {
                    @Override
                    public void handleScan(LinkedHashMap<String, Object> documentMap) {
                        startSelfieScan(documentMap);
                    }

                    @Override
                    public void scanWasCancelled() {
                        finish();
                    }

                    @Override
                    public void captureError(DSError dsError) {
                        ErrorDialog errorDialog = new ErrorDialog(
                                DocumentScannerActivity.this);
                        errorDialog.show(null, getString(R.string.label_error),
                                dsError.message,
                                dialog -> {
                                    errorDialog.dismiss();
                                    finish();
                                });
                    }
                });
    }

    /**
     * Start authentic id Selfie scan
     *
     * @param documentMap object from authenticId scanner
     */
    private void startSelfieScan(LinkedHashMap<String, Object> documentMap) {
        SelfieScannerHelper selfieScannerHelper = new SelfieScannerHelper(this);
        selfieScannerHelper.scanSelfie(new SelfieScannerHelper.SelfieScanCallback() {
            @Override
            public void onFinishSelfieScan(LinkedHashMap<String, Object> selfieScanData) {
                mSelfieMap = selfieScanData;
                verifyDocument(documentMap);
            }

            @Override
            public void onCancelSelfieScan() {
                finish();
            }

            @Override
            public void onErrorSelfieScan(int errorCode, String errorMessage) {
                showError(new ErrorManager.ErrorResponse(errorCode, errorMessage));
            }
        });
    }

    /**
     * Call verify document api to get DL document data
     *
     * @param documentMap DL Object with front_image, front_image_flash and back_image,
     */
    private void verifyDocument(LinkedHashMap<String, Object> documentMap) {
        mProgressDialog.show(getString(R.string.label_extracting_identity_data));
        documentMap.put(K_TYPE, "dl");
        documentMap.put(K_ID, BlockIDSDK.getInstance().getDID() + ".dl");
        VerifyDocument.getInstance().verifyDL(documentMap, (response, documentData, error) -> {
            if (!response) {
                mProgressDialog.dismiss();
                showError(error);
                return;
            }
            mDocumentMap = documentData;
            compareFace();
        });
    }

    /**
     * compare driver license face and selfie
     */
    private void compareFace() {
        mProgressDialog.show(getString(R.string.label_matching_selfie));
        String liveIdBase64 = Objects.requireNonNull(mSelfieMap.get("liveId")).toString();
        String documentFaceBase64 = Objects.requireNonNull(
                mDocumentMap.get("face")).toString();
        VerifyDocument.getInstance().compareFace(liveIdBase64, documentFaceBase64,
                (status, errorResponse) -> {
                    if (status) {
                        registerDocument(mDocumentMap);
                    } else {
                        mProgressDialog.dismiss();
                        showError(errorResponse);
                    }
                });
    }

    /**
     * Register Document
     *
     * @param documentMap {@link LinkedHashMap}
     */
    private void registerDocument(LinkedHashMap<String, Object> documentMap) {
        mProgressDialog.show(getString(R.string.label_completing_your_registration));
        if (documentMap != null) {
            BlockIDSDK.getInstance().registerDocument(this, documentMap,
                    null, (enroll_status, errorResponse) -> {
                        mProgressDialog.dismiss();
                        if (enroll_status) {
                            Toast.makeText(this, R.string.label_dl_enrolled_successfully,
                                    Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }
                        showError(errorResponse);
                    });
        }
    }

    /**
     * register document with liveid, when liveid is not enrolled
     */
    private void registerDocumentWithLiveID() {
        mProgressDialog.show(getString(R.string.label_completing_your_registration));
        Bitmap liveIdBitmap = AppUtil.imageBase64ToBitmap(
                Objects.requireNonNull(mSelfieMap.get("liveId")).toString());
        BlockIDSDK.getInstance().registerDocument(this, mDocumentMap, liveIdBitmap,
                "blockid", null, null, (status, error) -> {
                    mProgressDialog.dismiss();
                    if (status) {
                        Toast.makeText(this, R.string.label_dl_enrolled_successfully,
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    showError(error);
                });
    }

    /**
     * Show Error Dialog
     *
     * @param error {@link ErrorManager.ErrorResponse}
     */
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