package com.onekosmos.blockidsample.ui.nationalID;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.cameramodule.BIDScannerView;
import com.onekosmos.blockid.sdk.cameramodule.ScanningMode;
import com.onekosmos.blockid.sdk.cameramodule.camera.nationalID.INationalIDResponseListener;
import com.onekosmos.blockid.sdk.cameramodule.nationalID.NationalIDScanOrder;
import com.onekosmos.blockid.sdk.cameramodule.nationalID.NationalIDScannerHelper;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.document.DocumentHolder;
import com.onekosmos.blockidsample.ui.liveID.LiveIDScanningActivity;
import com.onekosmos.blockidsample.util.AppPermissionUtils;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.ProgressDialog;

import java.util.LinkedHashMap;

import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_SOMETHING_WENT_WRONG;
import static com.onekosmos.blockid.sdk.cameramodule.nationalID.NationalIDScanOrder.FIRST_BACK_THEN_FRONT;
import static com.onekosmos.blockid.sdk.document.BIDDocumentProvider.RegisterDocCategory.identity_document;
import static com.onekosmos.blockid.sdk.document.RegisterDocType.NATIONAL_ID;
import static com.onekosmos.blockidsample.ui.nationalID.NationalIDLiveScanActivity.ScanOrder.BACK_SIDE;
import static com.onekosmos.blockidsample.ui.nationalID.NationalIDLiveScanActivity.ScanOrder.FRONT_SIDE;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class NationalIDLiveScanActivity extends AppCompatActivity implements View.OnClickListener, INationalIDResponseListener {
    private static final int K_CAMERA_PERMISSION_REQUEST_CODE = 1011;
    private static int K_NATIONAL_ID_EXPIRY_GRACE_DAYS = 90;
    private final String[] K_CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private AppCompatImageView mImgBack, mScannerOverlay, mImgSuccess;
    private AppCompatTextView mTxtBack, mTxtMessage, mTxtScanMsg;
    private BIDScannerView mBIDScannerView;
    private LinearLayout mLayoutMessage;
    private NationalIDScannerHelper mNationalIdScannerHelper;
    private LinkedHashMap<String, Object> mNationalIDMap, mNationalIDFirstSideData;
    private String mSigToken, mScanSide;
    private NationalIDScanOrder mNationalIDScanOrder = FIRST_BACK_THEN_FRONT;
    private boolean isRegistrationInProgress;
    private String K_NO_FACE_FOUND = "BlockIDFaceDetectionNotification";
    private String K_FACE_COUNT = "numberOfFaces";

    private BroadcastReceiver mPPScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int count = intent.getIntExtra(K_FACE_COUNT, 0);

            runOnUiThread(() -> {
                if (count > 1)
                    mTxtScanMsg.setText(R.string.label_many_faces);
                else
                    mTxtScanMsg.setText(mScanSide);
            });
        }
    };

    enum ScanOrder {
        FRONT_SIDE,
        BACK_SIDE
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nationalid_live_scanning);
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mPPScanReceiver,
                new IntentFilter(K_NO_FACE_FOUND));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!AppPermissionUtils.isPermissionGiven(K_CAMERA_PERMISSION, this))
            AppPermissionUtils.requestPermission(this, K_CAMERA_PERMISSION_REQUEST_CODE, K_CAMERA_PERMISSION);
        else {
            if (NationalIDTempData.getInstance().getNationalIDFirstSideData() == null) {
                startFirstSideScan();
                if (mNationalIDScanOrder == FIRST_BACK_THEN_FRONT)
                    updateScanOrderText(BACK_SIDE);
                else
                    updateScanOrderText(FRONT_SIDE);
            }

            if (NationalIDTempData.getInstance().getNationalIDFirstSideData() != null && NationalIDTempData.getInstance().getmSignatureToken() != null) {
                mNationalIDFirstSideData = NationalIDTempData.getInstance().getNationalIDFirstSideData();
                mSigToken = NationalIDTempData.getInstance().getmSignatureToken();
                NationalIDTempData.getInstance().clearNationalIDData();
                mNationalIDMap = mNationalIDFirstSideData;
                mNationalIdScannerHelper = new NationalIDScannerHelper(this, ScanningMode.SCAN_LIVE,
                        mNationalIDMap, mSigToken,
                        mBIDScannerView, mScannerOverlay, K_NATIONAL_ID_EXPIRY_GRACE_DAYS, this);
                mNationalIdScannerHelper.startNationalIDScanning();
                if (mNationalIDScanOrder != FIRST_BACK_THEN_FRONT)
                    updateScanOrderText(BACK_SIDE);
                else
                    updateScanOrderText(FRONT_SIDE);
            }
        }
        mLayoutMessage.setVisibility(View.VISIBLE);
        mTxtMessage.setVisibility(View.VISIBLE);
        mTxtMessage.setText(R.string.label_scanning);
    }

    private void updateScanOrderText(ScanOrder order) {
        if (order == BACK_SIDE) {
            mTxtScanMsg.setText(R.string.label_scan_back);
            mScanSide = getString(R.string.label_scan_back);
        } else {
            mTxtScanMsg.setText(R.string.label_scan_front);
            mScanSide = getString(R.string.label_scan_front);
        }
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
                    getString(R.string.label_camera_permission_alert), dialog -> {
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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mPPScanReceiver);
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
        Intent intent = new Intent(this, NationalIDLiveScanActivity.class);
        startActivity(intent);
        getIntent().setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        overridePendingTransition(0, 0);
    }

    private void initView() {
        mBIDScannerView = findViewById(R.id.bid_scanner_view);
        mScannerOverlay = findViewById(R.id.view_overlay);

        if (AppPermissionUtils.isPermissionGiven(K_CAMERA_PERMISSION, this)) {
            mBIDScannerView.setVisibility(View.VISIBLE);
        }

        mImgBack = findViewById(R.id.img_back);
        mImgBack.setOnClickListener(this);

        mTxtBack = findViewById(R.id.txt_back);
        mTxtBack.setOnClickListener(this);

        mTxtScanMsg = findViewById(R.id.txt_scan_msg);
        mTxtScanMsg.setOnClickListener(this);

        mImgSuccess = findViewById(R.id.img_success);
        mTxtMessage = findViewById(R.id.txt_message);
        mLayoutMessage = findViewById(R.id.layout_message);
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
        mNationalIdScannerHelper = new NationalIDScannerHelper(this, ScanningMode.SCAN_LIVE, mNationalIDScanOrder,
                mBIDScannerView, mScannerOverlay, K_NATIONAL_ID_EXPIRY_GRACE_DAYS, this);
        mNationalIdScannerHelper.startNationalIDScanning();
    }

    private void stopScan() {
        mTxtScanMsg.setVisibility(View.GONE);
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