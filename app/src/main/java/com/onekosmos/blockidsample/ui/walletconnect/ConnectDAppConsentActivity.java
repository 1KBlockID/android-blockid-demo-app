package com.onekosmos.blockidsample.ui.walletconnect;

import static com.onekosmos.blockidsample.ui.walletconnect.WalletConnectActivity.D_APP_URL;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockidsample.R;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2022 1Kosmos. All rights reserved.
 */
public class ConnectDAppConsentActivity extends AppCompatActivity {

    AppCompatTextView mTxtHeader, mTxtSubHeader, mTxtFromAddressValue, mTxtToAddressValue,
            mTxtValueData, mTxtGasPriceValue, mTxtDataValue, mTxtNonceValue, mTxtWalletAddressValue;
    AppCompatButton mBtnReject, mBtnApprove;
    ConstraintLayout mLayoutScr1, mLayoutScr2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_dapp_consent_screen);
        initView();
    }

    private void initView() {

        String dAppUrl = getIntent().hasExtra(D_APP_URL) ? getIntent().getStringExtra(D_APP_URL)
                : "";

        mTxtHeader = findViewById(R.id.txt_title_header);
        mTxtHeader.setText(dAppUrl);
        mTxtSubHeader = findViewById(R.id.txt_sub_heading);
        mTxtWalletAddressValue = findViewById(R.id.txt_wallet_from_address);
        mTxtWalletAddressValue.setText(BlockIDSDK.getInstance().getDID());
        mBtnReject = findViewById(R.id.btn_reject);
        mBtnApprove = findViewById(R.id.btn_approve);

        mBtnReject.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        mBtnApprove.setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });
    }
}
