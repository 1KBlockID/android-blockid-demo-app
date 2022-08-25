package com.onekosmos.blockidsample.ui.walletconnect;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;

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
        mLayoutScr1 = findViewById(R.id.layout_constraint_scr_1);
        mLayoutScr2 = findViewById(R.id.layout_constraint_scr_2);
        mLayoutScr1.setVisibility(View.VISIBLE);
        mLayoutScr2.setVisibility(View.GONE);
        mTxtHeader = findViewById(R.id.txt_title_header);
        mTxtSubHeader = findViewById(R.id.txt_sub_heading);
        mTxtFromAddressValue = findViewById(R.id.txt_from_address_value);
        mTxtToAddressValue = findViewById(R.id.txt_to_address_value);
        mTxtValueData = findViewById(R.id.txt_value_data);
        mTxtGasPriceValue = findViewById(R.id.txt_gas_price_value);
        mTxtDataValue = findViewById(R.id.txt_data_value);
        mTxtNonceValue = findViewById(R.id.txt_nonce_value);
        mTxtWalletAddressValue = findViewById(R.id.txt_wallet_from_address);
        mBtnReject = findViewById(R.id.btn_reject);
        mBtnApprove = findViewById(R.id.btn_approve);

        mBtnReject.setOnClickListener(v -> {
            //TODO :- When user clicks on Reject Button
            mLayoutScr1.setVisibility(View.VISIBLE);
            mLayoutScr2.setVisibility(View.GONE);
            mTxtHeader.setText(getString(R.string.label_sign_transaction));
            mTxtSubHeader.setText("Dapp-URL-1");
        });

        mBtnApprove.setOnClickListener(v -> {
            //TODO :- When user clicks on Reject Button
            mLayoutScr1.setVisibility(View.GONE);
            mLayoutScr2.setVisibility(View.VISIBLE);
            mTxtHeader.setText("Dapp-URL-1");
            mTxtSubHeader.setText(getString(R.string.label_connect_wallet));
        });
    }
}
