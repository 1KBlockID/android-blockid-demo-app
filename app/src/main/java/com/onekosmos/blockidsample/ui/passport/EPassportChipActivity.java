package com.onekosmos.blockidsample.ui.passport;

import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.view.View;
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
import com.example.blockidsdkdemo.R;
import com.onekosmos.blockidsample.ui.liveID.LiveIDScanningActivity;
import com.onekosmos.blockidsample.util.DocumentHolder;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.ProgressDialog;

import java.util.Arrays;

import static com.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_PP_RFID_TIMEOUT;
import static com.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_SOMETHING_WENT_WRONG;

/**
 * Created by Pankti Mistry on 03-05-2021.
 * Copyright © 2020 1Kosmos. All rights reserved.
 */
public class EPassportChipActivity extends AppCompatActivity implements View.OnClickListener, IPassportResponseListener {
    private static int K_PASSPORT_EXPIRY_GRACE_DAYS = 90;
    private AppCompatTextView mTxtSkip;
    private AppCompatButton mBtnScan, mBtnCancel;
    private ConstraintLayout mLayoutNFC, mLayoutScanRFId;
    private PassportScannerHelper mPassportScannerHelper;
    private BIDPassport mPassportData;
    private String mSigToken;
    private boolean mIsRegInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_e_passport_chip_scan);
        mPassportScannerHelper = new PassportScannerHelper(this, K_PASSPORT_EXPIRY_GRACE_DAYS, this);
        mPassportData = PassportDataHolder.getData();
        mSigToken = PassportDataHolder.getToken();
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
        if (!mIsRegInProgress)
            super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPassportScannerHelper.stopRFIDScanning();
    }

    private void initView() {
        mTxtSkip = findViewById(R.id.txt_skip);
        mTxtSkip.setOnClickListener(this);
        mBtnScan = findViewById(R.id.btn_scan);
        mBtnScan.setOnClickListener(this);
        mBtnCancel = findViewById(R.id.btn_cancel);
        mBtnCancel.setOnClickListener(this::onClick);
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
        if (bidPassport != null) {
            mPassportData = bidPassport;
            registerPassport();
            return;
        }

        if (error == null)
            error = new ErrorManager.ErrorResponse(K_SOMETHING_WENT_WRONG.getCode(), K_SOMETHING_WENT_WRONG.getMessage());

        ErrorDialog errorDialog = new ErrorDialog(this);
        if (error.getCode() == K_PP_RFID_TIMEOUT.getCode()) {
            errorDialog.showWithOneButton(null,
                    getString(R.string.label_timeout),
                    error.getMessage() + "\n(" + getString(R.string.label_error_code) + error.getCode() + ")",
                    getString(R.string.label_scan_again),
                    dialog -> {
                        errorDialog.dismiss();
                        mPassportScannerHelper.startRFIDScanning(mPassportData, mSigToken);
                    });
            return;
        }
        errorDialog.show(null,
                getString(R.string.label_error),
                error.getMessage() + "\n(" + getString(R.string.label_error_code) + error.getCode() + ")",
                dialog -> {
                    errorDialog.dismiss();
                    finish();
                });
    }

    private void registerPassport() {
        mIsRegInProgress = true;
        mPassportScannerHelper.stopRFIDScanning();
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();
        if (mPassportData != null) {
            BlockIDSDK.getInstance().registerDocument(this, mPassportData, BIDDocumentProvider.BIDDocumentType.passport,
                    "", (status, error) -> {
                        progressDialog.dismiss();
                        if (status) {
                            PassportDataHolder.clearData();
                            Toast.makeText(this, R.string.label_passport_enrolled_successfully, Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }

                        if (error == null)
                            error = new ErrorManager.ErrorResponse(K_SOMETHING_WENT_WRONG.getCode(), K_SOMETHING_WENT_WRONG.getMessage());

                        if (error.getCode() == ErrorManager.CustomErrors.K_LIVEID_IS_MANDATORY.getCode()) {
                            DocumentHolder.setData(mPassportData, BIDDocumentProvider.BIDDocumentType.passport, "");
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
    }
}