package com.onekosmos.blockidsample.ui;

import static com.onekosmos.blockid.sdk.documentScanner.DocumentScannerActivity.K_UID;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.view.WindowCompat;

import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.ui.driverLicense.DriverLicenseScanActivity;
import com.onekosmos.blockidsample.ui.nationalID.NationalIDScanActivity;
import com.onekosmos.blockidsample.ui.passport.PassportScanningActivity;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2025 1Kosmos. All rights reserved.
 */
public class DocumentScannerWithUIDActivity extends AppCompatActivity {
    private AppCompatImageView mImgBack;
    private AppCompatTextView mTxtTitle;
    private AppCompatEditText mEdTxtEnterUID;
    private AppCompatButton mBtnVerification, mBtnVerificationWithUID;
    private String mDocType;
    private DocumentScannerTypeForUID mDocumentScannerType;
    public static final String K_DOCUMENT_TYPE = "document_type";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 15+
            WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        }

        setContentView(R.layout.activity_document_scanner_with_uid);
        mDocType = getIntent().getStringExtra(K_DOCUMENT_TYPE);
        mDocumentScannerType = DocumentScannerTypeForUID.valueOf(mDocType);
        initView();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    /**
     * Initialize UI Object
     *
     * @noinspection DataFlowIssue
     */
    private void initView() {
        mImgBack = findViewById(R.id.img_back_uid);
        mImgBack.setOnClickListener(v -> onBackPressed());

        mTxtTitle = findViewById(R.id.txt_title_uid);
        mEdTxtEnterUID = findViewById(R.id.edt_uid);
        mBtnVerification = findViewById(R.id.btn_verification);
        mBtnVerificationWithUID = findViewById(R.id.btn_verification_with_uid);

        if (mDocumentScannerType == DocumentScannerTypeForUID.DL1) {
            mTxtTitle.setText(R.string.label_driver_license_1);
            mBtnVerification.setText(R.string.label_start_dl_verification);
            mBtnVerificationWithUID.setText(R.string.label_start_dl_verification_with_UID);
        } else if (mDocumentScannerType == DocumentScannerTypeForUID.PP1) {
            mTxtTitle.setText(R.string.label_passport1);
            mBtnVerification.setText(R.string.label_start_pp_verification);
            mBtnVerificationWithUID.setText(R.string.label_start_pp_verification_with_UID);
        } else if (mDocumentScannerType == DocumentScannerTypeForUID.PP2) {
            mTxtTitle.setText(R.string.label_passport2);
            mBtnVerification.setText(R.string.label_start_pp_verification);
            mBtnVerificationWithUID.setText(R.string.label_start_pp_verification_with_UID);
        } else if (mDocumentScannerType == DocumentScannerTypeForUID.NID1) {
            mTxtTitle.setText(R.string.label_national_id_1);
            mBtnVerification.setText(R.string.label_start_nid_verification);
            mBtnVerificationWithUID.setText(R.string.label_start_nid_verification_with_UID);
        }

        mEdTxtEnterUID.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // not used
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String uid = s.toString().trim();
                mBtnVerificationWithUID.setEnabled(!TextUtils.isEmpty(uid));
            }

            @Override
            public void afterTextChanged(Editable s) {
                // not used
            }
        });

        mBtnVerification.setOnClickListener(v -> {
            if (mDocumentScannerType == DocumentScannerTypeForUID.DL1) {
                Intent intent = new Intent(this, DriverLicenseScanActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
            } else if (mDocumentScannerType == DocumentScannerTypeForUID.PP1 ||
                    mDocumentScannerType == DocumentScannerTypeForUID.PP2) {
                Intent intent = new Intent(this, PassportScanningActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
            } else if (mDocumentScannerType == DocumentScannerTypeForUID.NID1) {
                Intent intent = new Intent(this, NationalIDScanActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
            }
        });

        mBtnVerificationWithUID.setOnClickListener(v -> {
            if (mDocumentScannerType == DocumentScannerTypeForUID.DL1) {
                Intent intent = new Intent(this, DriverLicenseScanActivity.class);
                intent.putExtra(K_UID, mEdTxtEnterUID.getText().toString());
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
            } else if (mDocumentScannerType == DocumentScannerTypeForUID.PP1 ||
                    mDocumentScannerType == DocumentScannerTypeForUID.PP2) {
                Intent intent = new Intent(this, PassportScanningActivity.class);
                intent.putExtra(K_UID, mEdTxtEnterUID.getText().toString());
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
            } else if (mDocumentScannerType == DocumentScannerTypeForUID.NID1) {
                Intent intent = new Intent(this, NationalIDScanActivity.class);
                intent.putExtra(K_UID, mEdTxtEnterUID.getText().toString());
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
            }
        });
    }

    public enum DocumentScannerTypeForUID {
        DL1("DL1"),
        PP1("PP1"),
        PP2("PP2"),
        NID1("NID1");

        private final String docType;

        private DocumentScannerTypeForUID(String docType) {
            this.docType = docType;
        }

        public String getValue() {
            return this.docType;
        }
    }
}
