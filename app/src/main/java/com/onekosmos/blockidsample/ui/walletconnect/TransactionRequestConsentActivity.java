package com.onekosmos.blockidsample.ui.walletconnect;

import static com.onekosmos.blockidsample.ui.walletconnect.WalletConnectActivity.SIGN_TRANSACTION_DATA;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;

import com.onekosmos.blockid.sdk.utils.BIDUtil;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.WalletConnectHelper.SessionRequestParams;
import com.walletconnect.sign.client.Sign;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2022 1Kosmos. All rights reserved.
 */
public class TransactionRequestConsentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_request_consent_screen);
        initView();
    }

    private void initView() {

        Sign.Model.SessionRequest sessionRequest =
                BIDUtil.JSONStringToObject(getIntent().getStringExtra(SIGN_TRANSACTION_DATA),
                        Sign.Model.SessionRequest.class);
        AppCompatTextView txtDAppURL = findViewById(R.id.txt_dapp_url_sign_transaction);
        AppCompatTextView txtFromAddress = findViewById(R.id.txt_from_address);
        AppCompatTextView txtToAddress = findViewById(R.id.txt_to_address);
        AppCompatTextView txtValue = findViewById(R.id.txt_value);
        AppCompatTextView txtGasPrice = findViewById(R.id.txt_gas_price);
        AppCompatTextView txtData = findViewById(R.id.txt_data);
        AppCompatTextView txtNonce = findViewById(R.id.txt_nonce);

        if (sessionRequest != null) {
            txtDAppURL.setText(sessionRequest.getPeerMetaData().getUrl());
            String params = sessionRequest.getRequest().getParams().replaceAll("[\\[\\]]",
                    "");
            SessionRequestParams requestParams = BIDUtil.JSONStringToObject(params,
                    SessionRequestParams.class);
            txtFromAddress.setText(requestParams.from);
            txtToAddress.setText(requestParams.to);
            txtValue.setText(requestParams.value);
            txtGasPrice.setText(requestParams.gasPrice);
            txtData.setText(requestParams.data);
            txtNonce.setText(requestParams.nonce);
            txtGasPrice.setText(requestParams.gasPrice);
        }

        AppCompatButton btnReject = findViewById(R.id.btn_reject_sign_transaction);
        AppCompatButton btnApprove = findViewById(R.id.btn_approve_sign_transaction);

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