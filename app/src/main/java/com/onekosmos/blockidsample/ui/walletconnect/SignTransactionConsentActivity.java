package com.onekosmos.blockidsample.ui.walletconnect;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.onekosmos.blockid.sdk.utils.BIDUtil;
import com.onekosmos.blockid.sdk.walletconnect.WalletConnectHelper;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.walletconnect.sign.client.Sign;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2022 1Kosmos. All rights reserved.
 */
public class SignTransactionConsentActivity extends AppCompatActivity {
    public static final String K_SESSION_REQUEST_DATA = "K_SESSION_PROPOSAL_DATA";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_transaction_consent);
        initView();
    }

    private void initView() {
        Sign.Model.SessionRequest sessionRequest = BIDUtil.JSONStringToObject(
                getIntent().getStringExtra(K_SESSION_REQUEST_DATA),
                Sign.Model.SessionRequest.class);

        if (!(sessionRequest.getRequest().getMethod().equalsIgnoreCase(
                "eth_signTransaction")
                || sessionRequest.getRequest().getMethod().equalsIgnoreCase(
                "personal_sign"))) {
            ErrorDialog errorDialog = new ErrorDialog(this);
            DialogInterface.OnDismissListener onDismissListener = dialogInterface -> {
                errorDialog.dismiss();
                finish();
            };
            errorDialog.showWithOneButton(null,
                    getString(R.string.label_error),
                    getString(R.string.label_invalid_session_request),
                    getString(R.string.label_ok),
                    onDismissListener);
            return;
        }

        ConstraintLayout layoutSignTransaction = findViewById(R.id.layout_sign_transaction);
        ConstraintLayout layoutPersonalSign = findViewById(R.id.layout_personal_sign);
        AppCompatTextView txtDAppURL = findViewById(R.id.txt_dapp_url_sign_transaction);
        AppCompatTextView txtFromAddress = findViewById(R.id.txt_from_address);
        AppCompatTextView txtToAddress = findViewById(R.id.txt_to_address);
        AppCompatTextView txtValue = findViewById(R.id.txt_value);
        AppCompatTextView txtGasPrice = findViewById(R.id.txt_gas_price);
        AppCompatTextView txtData = findViewById(R.id.txt_data);
        AppCompatTextView txtNonce = findViewById(R.id.txt_nonce);
        AppCompatTextView txtMessage = findViewById(R.id.txt_message_personal_sign);
        Sign.Model.AppMetaData metaData = sessionRequest.getPeerMetaData();
        if (metaData == null)
            return;

        txtDAppURL.setText(metaData.getUrl());

        if (sessionRequest.getRequest().getMethod().
                equalsIgnoreCase("eth_signTransaction")) {
            layoutSignTransaction.setVisibility(View.VISIBLE);
            String params = sessionRequest.getRequest().getParams().
                    replaceAll("[\\[\\]]", "");
            WalletConnectHelper.SessionRequestParams requestParams =
                    BIDUtil.JSONStringToObject(params,
                            WalletConnectHelper.SessionRequestParams.class);
            if (requestParams == null)
                return;

            txtFromAddress.setText(requestParams.from);
            txtToAddress.setText(requestParams.to);
            txtValue.setText(requestParams.value);
            txtGasPrice.setText(requestParams.gasPrice);
            txtData.setText(requestParams.data);
            txtNonce.setText(requestParams.nonce);
            txtGasPrice.setText(requestParams.gasPrice);
        } else if (sessionRequest.getRequest().getMethod().
                equalsIgnoreCase("personal_sign")) {
            layoutPersonalSign.setVisibility(View.VISIBLE);
            String[] messageData = sessionRequest.getRequest().getParams().split("\"");
            String message = null;
            try {
                message = new String(Hex.decodeHex(messageData[1].substring(2)));
            } catch (DecoderException e) {
                e.printStackTrace();
            }
            txtMessage.setText(message);
        }

        AppCompatButton btnReject = findViewById(R.id.btn_reject_sign_transaction);
        AppCompatButton btnApprove = findViewById(R.id.btn_approve_sign_transaction);
        WalletConnectHelper mWalletConnectHelper = WalletConnectHelper.getInstance();
        btnReject.setOnClickListener(view -> {
            mWalletConnectHelper.rejectSessionRequest(sessionRequest);
            finish();
        });

        btnApprove.setOnClickListener(view -> {
            mWalletConnectHelper.signSessionRequest(sessionRequest);
            finish();
        });
    }
}