package com.onekosmos.blockidsample.ui.walletconnect;

import static com.onekosmos.blockidsample.ui.walletconnect.WalletConnectActivity.D_APP_URL;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;

import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockidsample.R;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2022 1Kosmos. All rights reserved.
 */
public class ConnectDAppConsentActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_dapp_consent_screen);
        initView();
    }

    private void initView() {
        String dAppUrl = getIntent().hasExtra(D_APP_URL) ?
                getIntent().getStringExtra(D_APP_URL) : "";
        AppCompatTextView txtHeader = findViewById(R.id.txt_dapp_url);
        txtHeader.setText(dAppUrl);

        AppCompatTextView txtWalletAddressValue = findViewById(R.id.txt_wallet_address);
        txtWalletAddressValue.setText(BlockIDSDK.getInstance().getDID());

        AppCompatButton btnReject = findViewById(R.id.btn_reject_dapp_consent);
        AppCompatButton btnApprove = findViewById(R.id.btn_approve_dapp_consent);

        btnReject.setOnClickListener(view -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        btnApprove.setOnClickListener(view -> {
            setResult(RESULT_OK);
            finish();
        });
    }
}