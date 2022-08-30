package com.onekosmos.blockidsample.ui.walletconnect;

import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;

import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.utils.BIDUtil;
import com.onekosmos.blockid.sdk.walletconnect.WalletConnectHelper;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.walletconnect.sign.client.Sign;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2022 1Kosmos. All rights reserved.
 */
public class ConnectDAppConsentActivity extends AppCompatActivity {
    public static final String K_SESSION_PROPOSAL_DATA = "K_SESSION_PROPOSAL_DATA";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_dapp_consent);
        initView();
    }

    /**
     * Initialize UI Objects
     */
    private void initView() {
        Sign.Model.SessionProposal sessionProposal = BIDUtil.JSONStringToObject(
                getIntent().getStringExtra(K_SESSION_PROPOSAL_DATA),
                Sign.Model.SessionProposal.class);
        String dAppUrl = sessionProposal != null ? sessionProposal.getUrl() : "";
        AppCompatTextView txtHeader = findViewById(R.id.txt_dapp_url);
        txtHeader.setText(dAppUrl);

        AppCompatTextView txtWalletAddressValue = findViewById(R.id.txt_wallet_address);
        txtWalletAddressValue.setText(BlockIDSDK.getInstance().getDID());

        AppCompatButton btnReject = findViewById(R.id.btn_reject_dapp_consent);
        AppCompatButton btnApprove = findViewById(R.id.btn_approve_dapp_consent);
        WalletConnectHelper walletConnectHelper = WalletConnectHelper.getInstance();

        if (sessionProposal == null) {

            ErrorDialog errorDialog = new ErrorDialog(this);
            DialogInterface.OnDismissListener onDismissListener = dialogInterface -> {
                errorDialog.dismiss();
                finish();
            };
            errorDialog.showWithOneButton(null,
                    getString(R.string.label_error),
                    getString(R.string.label_invalid_session_proposal),
                    getString(R.string.label_ok),
                    onDismissListener);

            return;
        }

        btnReject.setOnClickListener(view -> {
            walletConnectHelper.rejectConnection(sessionProposal);
            finish();
        });

        btnApprove.setOnClickListener(view -> {
            walletConnectHelper.approveConnection(sessionProposal);
            finish();
        });
    }
}