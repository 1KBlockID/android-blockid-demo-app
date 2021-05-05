package com.onekosmos.blockidsdkdemo.ui.passport;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.blockid.sdk.BlockIDSDK;
import com.blockid.sdk.cameramodule.camera.passportModule.IPassportResponseListener;
import com.blockid.sdk.cameramodule.passport.PassportScannerHelper;
import com.blockid.sdk.datamodel.BIDPassport;
import com.blockid.sdk.document.BIDDocumentProvider;
import com.blockid.sdk.utils.BIDUtil;
import com.example.blockidsdkdemo.R;
import com.onekosmos.blockidsdkdemo.AppVault;
import com.onekosmos.blockidsdkdemo.ui.utils.ErrorDialog;
import com.onekosmos.blockidsdkdemo.ui.utils.ProgressDialog;

import java.util.Arrays;

import static com.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_PP_RFID_TIMEOUT;

/**
 * Created by Pankti Mistry on 03-05-2021.
 * Copyright © 2020 1Kosmos. All rights reserved.
 */
public class EPassportChipActivity extends AppCompatActivity implements View.OnClickListener, IPassportResponseListener {
    private AppCompatTextView mTxtSkip;
    private AppCompatButton mBtnScan, mBtnCancle;
    private ConstraintLayout mLayoutNFC, mLayoutScanRFId;
    private ProgressBar mProgressBar;
    private static int K_PASSPORT_EXPIRY_GRACE_DAYS = 90;
    private PassportScannerHelper mPassportScannerHelper;
    private BIDPassport mPassportData;
    private String mSigToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_e_passport_chip_scan);
        mPassportScannerHelper = new PassportScannerHelper(this, K_PASSPORT_EXPIRY_GRACE_DAYS, this);
        mPassportData = BIDUtil.JSONStringToObject(AppVault.getInstance().getPPData(), BIDPassport.class);
        mSigToken = getIntent().getStringExtra("S_TOKEN");
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_scan:
                mLayoutNFC.setVisibility(View.GONE);
                mLayoutScanRFId.setVisibility(View.VISIBLE);
                mPassportScannerHelper.startRFIDScanning(mPassportData, mSigToken);
                break;
            case R.id.txt_skip:
            case R.id.btn_cancel:
                showWarningDialog();
                break;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
            Tag tag = intent.getExtras().getParcelable(NfcAdapter.EXTRA_TAG);
            if (Arrays.asList(tag.getTechList()).contains("android.nfc.tech.IsoDep")) {
                mPassportScannerHelper.onNewIntent(tag);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mProgressBar.getVisibility() != View.VISIBLE)
            super.onBackPressed();
    }

    private void initView() {
        mTxtSkip = findViewById(R.id.txt_skip);
        mTxtSkip.setOnClickListener(this);
        mBtnScan = findViewById(R.id.btn_scan);
        mBtnScan.setOnClickListener(this);
        mBtnCancle = findViewById(R.id.btn_cancel);
        mBtnCancle.setOnClickListener(this::onClick);
        mProgressBar = findViewById(R.id.progress_bar);
        mLayoutNFC = findViewById(R.id.layout_nfc);
        mLayoutScanRFId = findViewById(R.id.layout_scan_rfid);
    }

    private void showWarningDialog() {
        ErrorDialog errorDialog = new ErrorDialog(this);
        errorDialog.showWithTwoButton(null
                , getString(R.string.label_warning)
                , getString(R.string.label_do_you_want_to_cancel_rfid)
                , getString(R.string.label_yes)
                , getString(R.string.label_no)
                , (dialogInterface, i) -> {
                    errorDialog.dismiss();
                },
                dialog -> {
                    errorDialog.dismiss();
                    registerPassport();
                });
    }

    @Override
    public void onPassportResponse(BIDPassport bidPassport, String s, ErrorManager.ErrorResponse error) {
        mPassportScannerHelper.stopRFIDScanning();
        ErrorDialog errorDialog = new ErrorDialog(this);
        if (error != null && error.getCode() == K_PP_RFID_TIMEOUT.getCode()) {
            errorDialog.showWithOneButton(null,
                    getString(R.string.label_timeout),
                    error.getMessage() + "\n(" + getString(R.string.label_error_code) + error.getCode() + ")",
                    getString(R.string.label_scan_again),
                    dialog -> {
                        errorDialog.dismiss();
                        mPassportScannerHelper.startRFIDScanning(mPassportData, mSigToken);
                    });
            return;
        } else if (error != null) {
            errorDialog.show(null,
                    getString(R.string.label_error),
                    error.getMessage() + "\n(" + getString(R.string.label_error_code) + error.getCode() + ")",
                    dialog -> {
                        errorDialog.dismiss();
                        finish();
                    });
            return;
        }
        if (bidPassport == null) {
            errorDialog.show(null,
                    getString(R.string.label_error),
                    getString(R.string.label_passport_fail_to_scan), dialog -> {
                        errorDialog.dismiss();
                        finish();
                    });
            return;
        }
        mPassportData = bidPassport;
        registerPassport();
    }

    private void registerPassport() {
        mPassportScannerHelper.stopRFIDScanning();
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();
        if (mPassportData != null) {
            BlockIDSDK.getInstance().registerDocument(this, mPassportData, BIDDocumentProvider.BIDDocumentType.passport,
                    "", (status, errorResponse) -> {
                        progressDialog.dismiss();
                        if (!status) {
                            ErrorDialog errorDialog = new ErrorDialog(this);
                            errorDialog.show(null,
                                    getString(R.string.label_error),
                                    errorResponse.getMessage(), dialog -> {
                                        errorDialog.dismiss();
                                        finish();
                                    });
                            return;
                        }
                        AppVault.getInstance().setPPData(BIDUtil.objectToJSONString(mPassportData, true));
                        Toast.makeText(this, "Passport register successfully", Toast.LENGTH_LONG).show();
                        finish();
                    });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPassportScannerHelper.stopRFIDScanning();
    }
}
