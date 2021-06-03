package com.onekosmos.blockidsample.ui.nationalID;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.blockid.sdk.BlockIDSDK;
import com.blockid.sdk.cameramodule.BIDScannerView;
import com.blockid.sdk.cameramodule.ScanningMode;
import com.blockid.sdk.cameramodule.camera.nationalID.INationalIDResponseListener;
import com.blockid.sdk.cameramodule.nationalID.NationalIDScanOrder;
import com.blockid.sdk.cameramodule.nationalID.NationalIDScannerHelper;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.document.DocumentHolder;
import com.onekosmos.blockidsample.ui.liveID.LiveIDScanningActivity;
import com.onekosmos.blockidsample.util.AppPermissionUtils;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.ProgressDialog;

import java.util.LinkedHashMap;

import static com.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_SOMETHING_WENT_WRONG;
import static com.blockid.sdk.document.BIDDocumentProvider.RegisterDocCategory.identity_document;
import static com.blockid.sdk.document.RegisterDocType.NATIONAL_ID;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class NationalIDScanActivity extends AppCompatActivity implements View.OnClickListener, INationalIDResponseListener {
    private static final int K_CAMERA_PERMISSION_REQUEST_CODE = 1011;
    private static int K_NATIONAL_ID_EXPIRY_GRACE_DAYS = 90;
    private final String[] K_CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private AppCompatImageView mImgBack, mScannerOverlay, mImgSuccess;
    private AppCompatTextView mTxtBack, mTxtMessage, mTxtScanMsg;
    private BIDScannerView mBIDScannerView;
    private LinearLayout mLayoutMessage;
    private NationalIDScannerHelper mNationalIdScannerHelper;
    private int mScannerOverlayMargin = 30;
    private LinkedHashMap<String, Object> mNationalIDMap, mNationalIDFirstSideData;
    private String mSigToken;
    private NationalIDScanOrder mNationalIDScanOrder;
    private boolean isRegistrationInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nationalid_scanning);
        initView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!AppPermissionUtils.isPermissionGiven(K_CAMERA_PERMISSION, this))
            AppPermissionUtils.requestPermission(this, K_CAMERA_PERMISSION_REQUEST_CODE, K_CAMERA_PERMISSION);
        else {
            if (NationalIDTempData.getInstance().getNationalIDFirstSideData() == null)
                startFirstSideScan();

            if (NationalIDTempData.getInstance().getNationalIDFirstSideData() != null && NationalIDTempData.getInstance().getmSignatureToken() != null) {
                mNationalIDFirstSideData = NationalIDTempData.getInstance().getNationalIDFirstSideData();
                mSigToken = NationalIDTempData.getInstance().getmSignatureToken();
                NationalIDTempData.getInstance().clearNationalIDData();
                mNationalIDMap = mNationalIDFirstSideData;
                mNationalIdScannerHelper = new NationalIDScannerHelper(this, ScanningMode.SCAN_LIVE,
                        mNationalIDMap, mSigToken,
                        mBIDScannerView, mScannerOverlay, K_NATIONAL_ID_EXPIRY_GRACE_DAYS, this);
                mNationalIdScannerHelper.startNationalIDScanning();
            }
        }

        mLayoutMessage.setVisibility(View.VISIBLE);
        mTxtMessage.setVisibility(View.VISIBLE);
        mTxtMessage.setText(R.string.label_scanning);

        if (mNationalIDScanOrder == NationalIDScanOrder.FIRST_BACK_THEN_FRONT)
            mTxtScanMsg.setText("Scan Back");
        else mTxtScanMsg.setText("Scan Front");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (AppPermissionUtils.isGrantedPermission(this, requestCode, grantResults, K_CAMERA_PERMISSION)) {
            startFirstSideScan();
        } else {
            ErrorDialog errorDialog = new ErrorDialog(this);
            errorDialog.show(null,
                    "",
                    getString(R.string.label_passport_camera_permission_alert), dialog -> {
                        errorDialog.dismiss();
                        setResult(RESULT_CANCELED);
                        finish();
                    });
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.img_back:
            case R.id.txt_back:
                onCancelEnrollment();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (!isRegistrationInProgress)
            onCancelEnrollment();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mNationalIdScannerHelper != null)
            mNationalIdScannerHelper.stopScanning();
    }

    @Override
    public void onNationalIDScanResponse(LinkedHashMap<String, Object> nationalIdMap, String signatureToken, ErrorManager.ErrorResponse error) {
        stopScan();

        if (nationalIdMap != null) {
            mNationalIDMap = nationalIdMap;
            mSigToken = signatureToken;
            registerNationalID();
            return;
        }

        if (error == null)
            error = new ErrorManager.ErrorResponse(K_SOMETHING_WENT_WRONG.getCode(), K_SOMETHING_WENT_WRONG.getMessage());

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
    }

    @Override
    public void onFirstSideScanResponse(LinkedHashMap<String, Object> nationalIdMap, String signatureToken, ErrorManager.ErrorResponse error) {
        NationalIDTempData.getInstance().setNationalIDFirstSideData(nationalIdMap);
        NationalIDTempData.getInstance().setmSignatureToken(signatureToken);
        Intent intent = new Intent(this, NationalIDScanActivity.class);
        startActivity(intent);
        getIntent().setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        overridePendingTransition(0, 0);
    }

    private void initView() {
        mBIDScannerView = findViewById(R.id.view_bid_scanner);
        mScannerOverlay = findViewById(R.id.view_overlay);
        mBIDScannerView.setScannerWidthMargin(mScannerOverlayMargin, null);

        if (AppPermissionUtils.isPermissionGiven(K_CAMERA_PERMISSION, this)) {
            mBIDScannerView.setVisibility(View.VISIBLE);
        }

        mImgBack = findViewById(R.id.img_back);
        mImgBack.setOnClickListener(this);

        mTxtBack = findViewById(R.id.txt_back);
        mTxtBack.setOnClickListener(this);

        mTxtScanMsg = findViewById(R.id.txt_scan_msg);
        mTxtScanMsg.setOnClickListener(this);

        mImgSuccess = findViewById(R.id.iv_success);
        mTxtMessage = findViewById(R.id.txt_msg);
        mLayoutMessage = findViewById(R.id.layout_msg);
    }

    private void registerNationalID() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();
        isRegistrationInProgress = true;
        mBIDScannerView.setVisibility(View.GONE);
        mLayoutMessage.setVisibility(View.GONE);
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

    private void startFirstSideScan() {
        mBIDScannerView.setVisibility(View.VISIBLE);
        mScannerOverlay.setVisibility(View.VISIBLE);
        mNationalIDScanOrder = NationalIDScanOrder.FIRST_BACK_THEN_FRONT;
        mNationalIdScannerHelper = new NationalIDScannerHelper(this, ScanningMode.SCAN_LIVE, NationalIDScanOrder.FIRST_BACK_THEN_FRONT,
                mBIDScannerView, mScannerOverlay, K_NATIONAL_ID_EXPIRY_GRACE_DAYS, this);
        mNationalIdScannerHelper.startNationalIDScanning();
    }

    private void stopScan() {
        mLayoutMessage.setVisibility(View.VISIBLE);
        mTxtMessage.setVisibility(View.VISIBLE);
        mTxtMessage.setText(R.string.label_scan_complete);
        mImgSuccess.setVisibility(View.VISIBLE);
        mNationalIdScannerHelper.stopScanning();
    }

    private void onCancelEnrollment() {
        ErrorDialog errorDialog = new ErrorDialog(this);
        errorDialog.showWithTwoButton(null,
                getString(R.string.cancellation_warning),
                getString(R.string.label_do_you_want_to_cancel_the_registration_process),
                getString(R.string.label_yes),
                getString(R.string.label_no),
                (dialogInterface, i) -> {
                    errorDialog.dismiss();
                },
                dialog -> {
                    NationalIDTempData.getInstance().clearNationalIDData();
                    mNationalIdScannerHelper.stopScanning();
                    errorDialog.dismiss();
                    setResult(RESULT_CANCELED);
                    finish();
                });
    }
}