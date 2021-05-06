package com.onekosmos.blockidsdkdemo.ui.passport;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
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
import com.blockid.sdk.cameramodule.camera.passportModule.IPassportResponseListener;
import com.blockid.sdk.cameramodule.passport.PassportScannerHelper;
import com.blockid.sdk.datamodel.BIDPassport;
import com.blockid.sdk.document.BIDDocumentProvider;
import com.blockid.sdk.utils.BIDUtil;
import com.example.blockidsdkdemo.R;
import com.onekosmos.blockidsdkdemo.AppVault;
import com.onekosmos.blockidsdkdemo.util.ErrorDialog;
import com.onekosmos.blockidsdkdemo.util.ProgressDialog;
import com.onekosmos.blockidsdkdemo.util.AppPermissionUtils;

import static com.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_PP_ABOUT_TO_EXPIRE;
import static com.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_PP_ALREADY_EXPIRED;

/**
 * Created by Pankti Mistry on 03-05-2021.
 * Copyright © 2020 1Kosmos. All rights reserved.
 */
public class PassportScanningActivity extends AppCompatActivity implements View.OnClickListener, IPassportResponseListener {
    private static final int K_PASSPORT_PERMISSION_REQUEST_CODE = 1011;
    private static int K_PASSPORT_EXPIRY_GRACE_DAYS = 90;
    private final String[] K_CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private AppCompatImageView mImgBack, mScannerOverlay, mImgSuccess;
    private AppCompatTextView mTxtBack, mTxtMessage;
    private BIDScannerView mBIDScannerView;
    private LinearLayout mLayoutMessage;
    private PassportScannerHelper mPassportScannerHelper;
    private int mScannerOverlayMargin = 30;
    private BIDPassport mBIDPassport;
    private String mSigToken;
    private boolean isDeviceHasNfc, isRegistrationInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passport_scanning);
        isDeviceHasNfc = isDeviceHasNFC();
        initView();
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

        mImgSuccess = findViewById(R.id.iv_success);
        mTxtMessage = findViewById(R.id.txt_msg);
        mLayoutMessage = findViewById(R.id.layout_msg);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!AppPermissionUtils.isPermissionGiven(K_CAMERA_PERMISSION, this))
            AppPermissionUtils.requestPermission(this, K_PASSPORT_PERMISSION_REQUEST_CODE, K_CAMERA_PERMISSION);
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
                    getString(R.string.label_passport_camera_permission_alert), dialog -> {
                        errorDialog.dismiss();
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
    public void onPassportResponse(BIDPassport bidPassport, String signatureToken, ErrorManager.ErrorResponse error) {
        stopScan();
        mBIDPassport = bidPassport;
        mSigToken = signatureToken;

        ErrorDialog errorDialog = new ErrorDialog(PassportScanningActivity.this);
        if (error != null && error.getCode() == K_PP_ALREADY_EXPIRED.getCode()) {
            errorDialog.show(null, getString(R.string.label_error), error.getMessage()
                            + "\n(" + getString(R.string.label_error_code) + error.getCode() + ")"
                    , dialog -> {
                        errorDialog.dismiss();
                        finish();
                    });
            return;
        } else if (error != null && error.getCode() == K_PP_ABOUT_TO_EXPIRE.getCode()) {
            showAboutToExpireDialog(error.getCode());
            return;
        } else if (error != null) {
            errorDialog.show(null, getString(R.string.label_error), error.getMessage()
                            + "\n(" + getString(R.string.label_error_code) + error.getCode() + ")"
                    , dialog -> {
                        errorDialog.dismiss();
                        finish();
                    });
            return;
        }

        if (bidPassport == null) {
            errorDialog.show(null, getString(R.string.label_error), getString(R.string.label_passport_fail_to_scan), dialog -> {
                errorDialog.dismiss();
                finish();
            });
            return;
        }
        if (isDeviceHasNfc) {
            openEPassportChipActivity();
            return;
        }
        registerPassport();
    }

    private void openEPassportChipActivity() {
        AppVault.getInstance().setPPData(BIDUtil.objectToJSONString(mBIDPassport, true));
        Intent intent = new Intent(this, EPassportChipActivity.class);
        intent.putExtra("S_TOKEN", mSigToken);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        finish();
    }

    private void registerPassport() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();
        isRegistrationInProgress = true;
        if (mBIDPassport != null) {
            BlockIDSDK.getInstance().registerDocument(this, mBIDPassport, BIDDocumentProvider.BIDDocumentType.passport,
                    "", (status, errorResponse) -> {
                        progressDialog.dismiss();
                        isRegistrationInProgress = false;
                        if (!status) {
                            ErrorDialog errorDialog = new ErrorDialog(this);
                            errorDialog.show(null,
                                    getString(R.string.label_error),
                                    errorResponse.getMessage(), dialog -> {
                                        errorDialog.dismiss();
                                    });
                            return;
                        }
                        AppVault.getInstance().setPPData(BIDUtil.objectToJSONString(mBIDPassport, true));
                        Toast.makeText(this,R.string.label_passport_enrolled_successfully , Toast.LENGTH_LONG).show();
                        finish();
                    });
        }
    }

    public void onStop() {
        super.onStop();
        mPassportScannerHelper.stopScanning();
    }

    private void showAboutToExpireDialog(int errorCode) {
        ErrorDialog errorDialog = new ErrorDialog(this);
        errorDialog.showWithTwoButton(null, "",
                getString(R.string.label_doc_about_to_expire_1) + " " + K_PASSPORT_EXPIRY_GRACE_DAYS + " "
                        + getString(R.string.label_doc_about_to_expire_2)
                        + "\n(" + getString(R.string.label_error_code) + errorCode + ")",
                getString(R.string.label_yes),
                getString(R.string.label_no),
                (dialogInterface, i) -> {
                    errorDialog.dismiss();
                    finish();
                },
                dialog -> {
                    if (isDeviceHasNfc) {
                        openEPassportChipActivity();
                        return;
                    }
                    registerPassport();
                });
    }

    private void startScan() {
        mBIDScannerView.setVisibility(View.VISIBLE);
        mScannerOverlay.setVisibility(View.VISIBLE);
        mPassportScannerHelper = new PassportScannerHelper(this, ScanningMode.SCAN_LIVE, mBIDScannerView, mScannerOverlay, K_PASSPORT_EXPIRY_GRACE_DAYS, this);
        mPassportScannerHelper.startPassportScanning();
        mLayoutMessage.setVisibility(View.VISIBLE);
        mTxtMessage.setVisibility(View.VISIBLE);
        mTxtMessage.setText(R.string.label_scanning);
    }

    private void stopScan() {
        mLayoutMessage.setVisibility(View.VISIBLE);
        mTxtMessage.setVisibility(View.VISIBLE);
        mTxtMessage.setText(R.string.label_scan_complete);
        mImgSuccess.setVisibility(View.VISIBLE);
        mPassportScannerHelper.stopScanning();
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
                    mPassportScannerHelper.stopScanning();
                    errorDialog.dismiss();
                    finish();
                });
    }

    private boolean isDeviceHasNFC() {
        NfcManager manager = (NfcManager) getSystemService(Context.NFC_SERVICE);
        NfcAdapter adapter = manager.getDefaultAdapter();
        return adapter != null;
    }
}
