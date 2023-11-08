package com.onekosmos.blockidsample.ui.passport;

import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_PP_RFID_TIMEOUT;
import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_SOMETHING_WENT_WRONG;
import static com.onekosmos.blockid.sdk.document.BIDDocumentProvider.RegisterDocCategory.identity_document;
import static com.onekosmos.blockid.sdk.document.RegisterDocType.PPT;

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

import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.rfidScanner.IRFIDScanResponseListener;
import com.onekosmos.blockid.sdk.rfidScanner.RFIDScannerHelper;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.document.DocumentHolder;
import com.onekosmos.blockidsample.ui.liveID.ActiveLiveIDScanningActivity;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.ProgressDialog;

import java.util.Arrays;
import java.util.LinkedHashMap;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class EPassportChipActivity extends AppCompatActivity implements View.OnClickListener, IRFIDScanResponseListener {
    private static int K_PASSPORT_EXPIRY_GRACE_DAYS = 90;
    private AppCompatTextView mTxtSkip;
    private AppCompatButton mBtnScan, mBtnCancel;
    private ConstraintLayout mLayoutNFC, mLayoutScanRFId;
    private RFIDScannerHelper mRFIDScannerHelper;
    private LinkedHashMap<String, Object> mPassportMap;
    private boolean mIsRegInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_e_passport_chip_scan);
        mRFIDScannerHelper = new RFIDScannerHelper(this, K_PASSPORT_EXPIRY_GRACE_DAYS,
                this);
        mPassportMap = DocumentHolder.getData();
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
                mRFIDScannerHelper.startRFIDScanning(mPassportMap);
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
                mRFIDScannerHelper.onNewIntent(tag);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (!mIsRegInProgress)
            super.onBackPressed();
    }

    @Override
    public void onRFIDResponse(LinkedHashMap<String, Object> passportMap,
                               ErrorManager.ErrorResponse error) {
        mRFIDScannerHelper.stopRFIDScanning();
        if (passportMap != null) {
            mPassportMap = passportMap;
            registerPassport();
            return;
        }

        if (error == null)
            error = new ErrorManager.ErrorResponse(K_SOMETHING_WENT_WRONG.getCode(),
                    K_SOMETHING_WENT_WRONG.getMessage());

        ErrorDialog errorDialog = new ErrorDialog(this);
        if (error.getCode() == K_PP_RFID_TIMEOUT.getCode()) {
            errorDialog.showWithOneButton(null,
                    getString(R.string.label_timeout),
                    error.getMessage() + "\n(" + getString(R.string.label_error_code) +
                            error.getCode() + ")",
                    getString(R.string.label_scan_again),
                    dialog -> {
                        errorDialog.dismiss();
                        mRFIDScannerHelper.startRFIDScanning(mPassportMap);
                    });
            return;
        }
        errorDialog.show(null,
                getString(R.string.label_error),
                error.getMessage() + "\n(" + getString(R.string.label_error_code)
                        + error.getCode() + ")",
                dialog -> {
                    errorDialog.dismiss();
                    finish();
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRFIDScannerHelper.stopRFIDScanning();
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

    private void registerPassport() {
        mIsRegInProgress = true;
        mRFIDScannerHelper.stopRFIDScanning();
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();
        if (mPassportMap != null) {
            mPassportMap.put("category", identity_document.name());
            mPassportMap.put("type", PPT.getValue());
            mPassportMap.put("id", mPassportMap.get("id"));
            BlockIDSDK.getInstance().registerDocument(this, mPassportMap,
                    null, (status, error) -> {
                        progressDialog.dismiss();
                        if (status) {
                            DocumentHolder.clearData();
                            Toast.makeText(this, R.string.label_passport_enrolled_successfully, Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }

                        if (error == null)
                            error = new ErrorManager.ErrorResponse(K_SOMETHING_WENT_WRONG.getCode(), K_SOMETHING_WENT_WRONG.getMessage());

                        if (error.getCode() == ErrorManager.CustomErrors.K_LIVEID_IS_MANDATORY.getCode()) {
                            DocumentHolder.setData(mPassportMap, null);
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
    }
}