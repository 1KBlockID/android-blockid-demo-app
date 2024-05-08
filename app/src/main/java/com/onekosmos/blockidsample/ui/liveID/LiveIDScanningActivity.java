package com.onekosmos.blockidsample.ui.liveID;

import static android.Manifest.permission.CAMERA;
import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_SOMETHING_WENT_WRONG;
import static com.onekosmos.blockid.sdk.document.BIDDocumentProvider.RegisterDocCategory.identity_document;

import android.content.DialogInterface.OnDismissListener;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.ErrorResponse;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.cameramodule.BIDScannerView;
import com.onekosmos.blockid.sdk.cameramodule.camera.liveIDModule.ILiveIDResponseListener;
import com.onekosmos.blockid.sdk.cameramodule.liveID.LiveIDScannerHelper;
import com.onekosmos.blockidsample.AppConstant;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.document.DocumentHolder;
import com.onekosmos.blockidsample.util.AppPermissionUtils;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.ProgressDialog;

import java.util.LinkedHashMap;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
@SuppressWarnings("FieldCanBeLocal")
public class LiveIDScanningActivity extends AppCompatActivity implements ILiveIDResponseListener {
    public static String IS_FROM_AUTHENTICATE = "IS_FROM_AUTHENTICATE";
    public static String LIVEID_WITH_DOCUMENT = "LIVEID_WITH_DOCUMENT";
    private static final int K_LIVEID_PERMISSION_REQUEST_CODE = 1009;
    private static final int mScannerOverlayMargin = 30;
    private final String[] K_CAMERA_PERMISSION = new String[]{CAMERA};
    private AppCompatImageView mImgBack, mScannerOverlay;
    private AppCompatTextView mTxtBack, mTxtMessage, mTxtTitle;
    private AppCompatButton mBtnCancel;
    private BIDScannerView mBIDScannerView;
    private LiveIDScannerHelper mLiveIDScannerHelper;
    private ProgressDialog mProgressDialog;
    private boolean mIsFromAuthentication; // Is LiveID scanning started for authentication purpose

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_liveid_scan);
        mIsFromAuthentication = getIntent().hasExtra(IS_FROM_AUTHENTICATE) &&
                getIntent().getBooleanExtra(IS_FROM_AUTHENTICATE, false);

        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        if (mLiveIDScannerHelper != null)
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
                    getString(R.string.label_liveid_camera_permission_alert),
                    dialog -> finish());
        }
    }

    /**
     * Initialize UI Objects
     */
    private void initViews() {
        mProgressDialog = new ProgressDialog(this, getString(R.string.label_verify_liveid));
        mBIDScannerView = findViewById(R.id.scanner_view_liveid_active);
        mScannerOverlay = findViewById(R.id.view_overlay_liveid_active);
        mBIDScannerView.setScannerWidthMargin(mScannerOverlayMargin, mScannerOverlay);
        mTxtTitle = findViewById(R.id.txt_title_liveid_active);

        if (mIsFromAuthentication)
            mTxtTitle.setText(R.string.label_verify_liveid);

        mTxtMessage = findViewById(R.id.txt_message_liveid_active);
        mImgBack = findViewById(R.id.img_back_liveid_active);
        mImgBack.setOnClickListener(view -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        mTxtBack = findViewById(R.id.txt_back_liveid_active);
        mTxtBack.setOnClickListener(view -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        mBtnCancel = findViewById(R.id.btn_cancel_liveid_active);
        mBtnCancel.setOnClickListener(view -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }

    /**
     * Start LiveID scanning
     * Implement ILiveIDResponseListener interface for the Activity
     */
    private void startLiveIDScan() {
        mBIDScannerView.setVisibility(View.VISIBLE);
        mScannerOverlay.setVisibility(View.VISIBLE);
        mLiveIDScannerHelper = new LiveIDScannerHelper(this, mBIDScannerView,
                mScannerOverlay, this);
        mLiveIDScannerHelper.startLiveIDScanning(AppConstant.dvcId);
    }

    // LiveID scanning response
    @Override
    public void onLiveIDCaptured(Bitmap liveIDBitmap, String signatureToken, String livenessResult,
                                 ErrorResponse error) {
        // Stop LiveID scanning
        mLiveIDScannerHelper.stopLiveIDScanning();

        mTxtMessage.setVisibility(View.GONE);
        mBtnCancel.setClickable(false);
        mImgBack.setClickable(false);
        mTxtBack.setClickable(false);
        mBIDScannerView.setVisibility(View.GONE);
        mScannerOverlay.setVisibility(View.GONE);
        mBtnCancel.setVisibility(View.GONE);

        if (mProgressDialog.isShowing())
            mProgressDialog.dismiss();

        ErrorDialog errorDialog = new ErrorDialog(this);
        OnDismissListener onDismissListener = dialogInterface -> {
            errorDialog.dismiss();
            finish();
        };

        if (liveIDBitmap == null) {
            // Liveness check API call failed because of device is offline, show offline error
            if (error.getCode() == ErrorManager.CustomErrors.K_CONNECTION_ERROR.getCode()) {
                errorDialog.showNoInternetDialog(onDismissListener);
                return;
            }

            // LiveID liveness check failed, Get error object and show error
            String stringError = (error.getObject() != null) ? error.getObject() : "";
            errorDialog.show(null, getString(R.string.label_error),
                    "(" + error.getCode() + ") " + error.getMessage() + "\n" + stringError,
                    onDismissListener);
            return;
        }

        // LiveID scanned successful

        // Activity started for authentication purpose, call verify LiveID
        if (mIsFromAuthentication) {
            verifyLiveID(liveIDBitmap, signatureToken, livenessResult);
            return;
        }

        // Activity stared after document scanning, register LiveID with Document Data
        if (getIntent().hasExtra(LIVEID_WITH_DOCUMENT) &&
                getIntent().getBooleanExtra(LIVEID_WITH_DOCUMENT, false)) {
            registerLiveIDWithDocument(liveIDBitmap);
            return;
        }

        // Activity stared for LiveID registration, register LiveID
        registerLiveID(liveIDBitmap, signatureToken, livenessResult);
    }

    @Override
    public void onFaceFocusChanged(boolean isFocused, String message) {
        // Show face message and focused view
        showFaceFocusedViews();
        if (message != null) {
            mTxtMessage.setVisibility(View.VISIBLE);
            mTxtMessage.setText(message);
        } else {
            mTxtMessage.setVisibility(View.GONE);
        }
    }

    // LiveID Liveness check is in progress
    // Hide scanner view
    @Override
    public void onLivenessCheckStarted() {
        if (!isFinishing()) {
            mProgressDialog.show();
        }
    }

    /**
     * Update the overlay image and color
     */
    private void showFaceFocusedViews() {
        mScannerOverlay.setImageResource(R.drawable.group_3);
    }

    /**
     * Register LiveID
     *
     * @param livIdBitmap LiveID image received from LiveID scanner
     */
    private void registerLiveID(Bitmap livIdBitmap, String signatureToken, String livenessResult) {
        mProgressDialog = new ProgressDialog(this, getString(R.string.label_please_wait));
        mProgressDialog.show();
        BlockIDSDK.getInstance().setLiveID(livIdBitmap, null, signatureToken,
                livenessResult, (status, message, error) -> {
                    mProgressDialog.dismiss();

                    // Register LiveID failed
                    if (!status) {
                        // show error
                        showError(error);
                        return;
                    }

                    // LiveID registered successfully
                    Toast.makeText(this,
                            getString(R.string.label_liveid_enrolled_successfully),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    /**
     * Register LiveID with document
     *
     * @param livIdBitmap LiveID image received from LiveID scanner
     */
    private void registerLiveIDWithDocument(Bitmap livIdBitmap) {
        mProgressDialog = new ProgressDialog(this, getString(R.string.label_please_wait));
        mProgressDialog.show();
        LinkedHashMap<String, Object> documentMap = DocumentHolder.getData();
        documentMap.put("category", identity_document.name());
        documentMap.put("type", documentMap.get("type"));
        documentMap.put("id", documentMap.get("id"));

        BlockIDSDK.getInstance().registerDocument(this, documentMap,
                livIdBitmap, null, null, null, (status, error) -> {
                    mProgressDialog.dismiss();
                    DocumentHolder.clearData();

                    // Register LiveID with document failed
                    if (!status) {
                        // show error
                        showError(error);
                        return;
                    }

                    // LiveID with document registered successfully
                    Toast.makeText(this,
                            getString(R.string.label_document_enrolled_successfully),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    /**
     * Verify LiveID
     *
     * @param bitmap LiveID image received from LiveID scanner
     */
    private void verifyLiveID(Bitmap bitmap, String signatureToken, String livenessResult) {
        mProgressDialog = new ProgressDialog(this, getString(R.string.label_verify_liveid));
        mProgressDialog.show();
        BlockIDSDK.getInstance().verifyLiveID(this, bitmap, signatureToken, livenessResult,
                (status, error) -> {
                    mProgressDialog.dismiss();
                    // LiveID verification failed
                    if (!status) {
                        // show error
                        showError(error);
                        return;
                    }

                    // LiveID verified successfully
                    setResult(RESULT_OK);
                    finish();
                });
    }

    /**
     * Show error dialog
     *
     * @param error {@link ErrorResponse}
     */
    private void showError(ErrorResponse error) {
        if (error == null)
            error = new ErrorResponse(K_SOMETHING_WENT_WRONG.getCode(),
                    K_SOMETHING_WENT_WRONG.getMessage());

        ErrorDialog errorDialog = new ErrorDialog(this);
        OnDismissListener onDismissListener = dialogInterface -> {
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