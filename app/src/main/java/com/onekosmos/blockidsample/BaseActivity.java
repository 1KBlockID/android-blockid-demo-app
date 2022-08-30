package com.onekosmos.blockidsample;


import static com.onekosmos.blockidsample.ui.walletconnect.ConnectConsentActivity.K_SESSION_PROPOSAL_DATA;
import static com.onekosmos.blockidsample.ui.walletconnect.SignConsentActivity.K_SESSION_REQUEST_DATA;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.onekosmos.blockid.sdk.utils.BIDUtil;
import com.onekosmos.blockid.sdk.walletconnect.WalletConnectCallback;
import com.onekosmos.blockid.sdk.walletconnect.WalletConnectHelper;
import com.onekosmos.blockidsample.ui.walletconnect.ConnectConsentActivity;
import com.onekosmos.blockidsample.ui.walletconnect.DAppViewModel;
import com.onekosmos.blockidsample.ui.walletconnect.SignConsentActivity;
import com.walletconnect.sign.client.Sign;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2022 1Kosmos. All rights reserved.
 */
public abstract class BaseActivity extends AppCompatActivity {
    private static DAppViewModel mViewModel;
    private WalletConnectHelper mWalletConnectHelper;
    private List<Sign.Model.Session> mSessionList;

    private final WalletConnectCallback mWalletConnectCallback = new WalletConnectCallback() {
        @Override
        public void onConnectionStateChange(Sign.Model.ConnectionState connectionState) {
            boolean isConnected = connectionState.isAvailable();
            if (isConnected) {
                getSessionList();
            }
        }

        @Override
        public void onSessionProposal(Sign.Model.SessionProposal sessionProposal) {
            startConnectDAppConsentActivity(sessionProposal);
        }

        @Override
        public void onSessionSettleResponse(Sign.Model.SettledSessionResponse.Result
                                                    settleSessionResponse) {
            Sign.Model.AppMetaData metaData = settleSessionResponse.getSession().getMetaData();
            if (metaData != null)
                runOnUiThread(() -> Toast.makeText(
                        getApplicationContext(),
                        getString(R.string.label_wallet_has_been_connected,
                                metaData.getUrl()),
                        Toast.LENGTH_SHORT).show());
            getSessionList();
        }

        @Override
        public void onSessionDelete(Sign.Model.DeletedSession deletedSession) {
            getSessionList();
        }

        @Override
        public void onSessionRequest(Sign.Model.SessionRequest sessionRequest) {
            startSignTransactionConsentActivity(sessionRequest);
        }

        @Override
        public void onError(Sign.Model.Error error) {
            if (error != null) {
                runOnUiThread(() -> Toast.makeText(
                        getApplicationContext(),
                        error.getThrowable().getMessage(),
                        Toast.LENGTH_SHORT).show());
            }
        }

        @Override
        public void onSessionUpdateResponse(Sign.Model.SessionUpdateResponse sessionUpdateResponse) {
            getSessionList();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWalletConnectHelper = WalletConnectHelper.getInstance();
        initWalletConnect();

        mViewModel = new ViewModelProvider(this).get(DAppViewModel.class);
    }

    /**
     * Initialize wallet connect SDK
     */
    private void initWalletConnect() {
        if (mWalletConnectHelper == null) {
            return;
        }

        List<String> iconList = new ArrayList<>();
        iconList.add("https://www.1kosmos.com/favicon.ico");

        Sign.Model.AppMetaData metadata = new Sign.Model.AppMetaData(
                getString(R.string.app_name),
                "1Kosmos-WalletConnect",
                "example.wallet",
                iconList,
                "kotlin-wallet-wc:/request");

        mWalletConnectHelper.initialize(getApplication(),
                getString(R.string.app_project_id), metadata, mWalletConnectCallback);
    }

    /**
     * Get list of connected session and update in recycler view
     */
    private void getSessionList() {
        if (mSessionList == null) {
            mSessionList = new ArrayList<>();
        } else {
            mSessionList.clear();
        }
        mSessionList.addAll(mWalletConnectHelper.getConnectedSessions());
        mViewModel.update(mSessionList);
    }

    /**
     * Open {@link ConnectConsentActivity} to approve/reject connection proposal for
     * decentralized app
     *
     * @param sessionProposal {@link Sign.Model.SessionProposal}
     */
    private void startConnectDAppConsentActivity(Sign.Model.SessionProposal sessionProposal) {
        Intent connectDAppIntent = new Intent(this, ConnectConsentActivity.class);
        connectDAppIntent.putExtra(K_SESSION_PROPOSAL_DATA,
                BIDUtil.objectToJSONString(sessionProposal, true));
        startActivity(connectDAppIntent);
    }

    /**
     * Open {@link SignConsentActivity} to approve/reject session request
     *
     * @param sessionRequest {@link Sign.Model.SessionRequest}
     */
    private void startSignTransactionConsentActivity(Sign.Model.SessionRequest sessionRequest) {
        Intent connectSignTransaction = new Intent(this,
                SignConsentActivity.class);
        connectSignTransaction.putExtra(K_SESSION_REQUEST_DATA,
                BIDUtil.objectToJSONString(sessionRequest, true));
        startActivity(connectSignTransaction);
    }

    public static DAppViewModel getModel() {
        return mViewModel;
    }
}