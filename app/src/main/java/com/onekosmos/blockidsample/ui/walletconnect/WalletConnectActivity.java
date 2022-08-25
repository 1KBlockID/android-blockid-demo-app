package com.onekosmos.blockidsample.ui.walletconnect;

import static com.onekosmos.blockidsample.ui.qrAuth.ScanQRCodeActivity.IS_FROM_WALLET_CONNECT;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.onekosmos.blockid.sdk.utils.BIDUtil;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.WalletConnectHelper;
import com.onekosmos.blockidsample.ui.qrAuth.ScanQRCodeActivity;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.ProgressDialog;
import com.walletconnect.sign.client.Sign;
import com.walletconnect.sign.client.SignInterface;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2022 1Kosmos. All rights reserved.
 */
public class WalletConnectActivity extends AppCompatActivity {
    public static final String D_APP_URL = "D_APP_URL";
    public static final String SIGN_TRANSACTION_DATA = "SIGN_TRANSACTION_DATA";
    private WalletConnectHelper walletConnectHelper;
    private final List<DAppAdapter.DAppData> mDAppList = new ArrayList<>();
    private DAppAdapter adapter;
    private ProgressDialog mProgressDialog;
    private AppCompatButton mBtnDisconnect;
    private boolean isConnected;
    private Sign.Model.SessionProposal mSessionProposal;
    private Sign.Model.SessionRequest mSessionRequest;

    private final ActivityResultLauncher<Intent> scanQResult = registerForActivityResult(new
            StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_CANCELED) {
            Toast.makeText(this, getString(R.string.label_qr_code_scanning_canceled),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (result.getResultCode() == RESULT_OK) {
            String qrData = result.getData() != null ?
                    result.getData().getStringExtra("wc_data") : null;
            Log.e("Data", "-->" + qrData);
            if (!validateQRCodeData(qrData)) {
                showErrorDialog(getString(R.string.label_invalid_code),
                        getString(R.string.label_unsupported_qr_code));
            }
            connectToDApp(qrData);
        }
    });

    private final ActivityResultLauncher<Intent> connectionResult = registerForActivityResult(new
            StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_CANCELED) {
            rejectDApp(mSessionProposal);
            mSessionProposal = null;
            return;
        }

        if (result.getResultCode() == RESULT_OK) {
            approveDApp(mSessionProposal);
            mSessionProposal = null;
        }
    });

    private final ActivityResultLauncher<Intent> signTransaction = registerForActivityResult(new
            StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_CANCELED) {
            rejectTransaction(mSessionRequest);
            mSessionRequest = null;
            return;
        }

        if (result.getResultCode() == RESULT_OK) {
            signTransaction(mSessionRequest);
            mSessionRequest = null;
        }
    });

    private final SignInterface.WalletDelegate walletDelegate = new SignInterface.WalletDelegate() {
        @Override
        public void onSessionProposal(@NonNull Sign.Model.SessionProposal sessionProposal) {
            Log.e("Called", "onSessionProposal-->" +
                    BIDUtil.objectToJSONString(sessionProposal, true));
            mSessionProposal = sessionProposal;
            startConnectDAppConsentActivity(sessionProposal.getUrl());
        }

        @Override
        public void onSessionRequest(@NonNull Sign.Model.SessionRequest sessionRequest) {
            // sign transaction request
            Log.e("Called", "onSessionRequest-->" +
                    BIDUtil.objectToJSONString(sessionRequest, true));
            mSessionRequest = sessionRequest;
            startSignTransactionConsent(sessionRequest);
        }

        @Override
        public void onSessionDelete(@NonNull Sign.Model.DeletedSession deletedSession) {
            Log.e("Called", "onSessionDelete-->" +
                    BIDUtil.objectToJSONString(deletedSession, true));
            getConnectedSession();
        }

        @Override
        public void onSessionSettleResponse(@NonNull Sign.Model.SettledSessionResponse
                                                    settledSessionResponse) {
            Log.e("Called", "onSessionSettleResponse-->" +
                    BIDUtil.objectToJSONString(settledSessionResponse, true));
            getConnectedSession();
        }

        @Override
        public void onSessionUpdateResponse(@NonNull Sign.Model.SessionUpdateResponse sessionUpdateResponse) {
            Log.e("Called", "onSessionUpdateResponse-->" +
                    BIDUtil.objectToJSONString(sessionUpdateResponse, true));
        }

        @Override
        public void onConnectionStateChange(@NonNull Sign.Model.ConnectionState connectionState) {
            isConnected = connectionState.isAvailable();
            if (isConnected) {
                getConnectedSession();
                hideProgressDialog();
            }
            Log.e("Called", "onConnectionStateChange-->" +
                    BIDUtil.objectToJSONString(connectionState, true));
        }

        @Override
        public void onError(@NonNull Sign.Model.Error error) {
            Log.e("Called", "onError-->" + error.getThrowable().getMessage());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_connect);
        walletConnectHelper = WalletConnectHelper.getInstance();
        initWalletConnect();
        initView();
    }

    /**
     * Initialize wallet connect SDK
     */
    private void initWalletConnect() {
        if (walletConnectHelper == null) {
            return;
        }

        showProgressDialog();
        // FIXME  pankti need talk to vinoth about url and redirect
        List<String> iconList = new ArrayList<>();
        iconList.add("https://gblobscdn.gitbook.com/spaces%2F-LJJeCjcLrr53DcT1Ml7%2Favatar.png?alt=media");
        Sign.Model.AppMetaData metadata = new Sign.Model.AppMetaData(getString(R.string.app_name),
                getString(R.string.wallet_connect_description), "example.wallet", iconList,
                "kotlin-wallet-wc:/request");

        walletConnectHelper.initializeWalletConnectSDK(getApplication(),
                getString(R.string.project_id), metadata, walletDelegate);
    }

    /**
     * Initialize UI Objects
     */
    private void initView() {
        AppCompatImageView btnBack = findViewById(R.id.img_back_wallet_connect);
        btnBack.setOnClickListener(view -> onBackPressed());
        AppCompatButton btnConnect = findViewById(R.id.btn_connect_to_d_app);
        btnConnect.setOnClickListener(view -> openScanQRCodeActivity());

        mBtnDisconnect = findViewById(R.id.btn_disconnect);
        mBtnDisconnect.setOnClickListener(view -> disconnect());

        adapter = new DAppAdapter(mDAppList);
        RecyclerView recyclerViewDApps = findViewById(R.id.recyclerview_dapp);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewDApps.setLayoutManager(layoutManager);
        recyclerViewDApps.setAdapter(adapter);

        ViewGroup.LayoutParams params = recyclerViewDApps.getLayoutParams();
        recyclerViewDApps.setLayoutParams(params);
        updateDisconnectUi();
    }

    /**
     * Enable/Disable disconnect button
     */
    private void updateDisconnectUi() {
        if (adapter.getItemCount() == 0) {
            mBtnDisconnect.setBackgroundColor(getColor(android.R.color.darker_gray));
            mBtnDisconnect.setEnabled(false);
        } else {
            mBtnDisconnect.setBackgroundColor(getColor(android.R.color.black));
            mBtnDisconnect.setEnabled(true);
        }
    }

    /**
     * Get list of connected session and display it
     */
    @SuppressLint("NotifyDataSetChanged")
    private void getConnectedSession() {
        List<Sign.Model.Session> sessionList = walletConnectHelper.getConnectedSessions();
        if (sessionList == null) {
            return;
        }

        mDAppList.clear();
        for (int i = 0; i < sessionList.size(); i++) {
            DAppAdapter.DAppData data = new DAppAdapter.DAppData();
            data.session = sessionList.get(i);
            mDAppList.add(data);
        }
        runOnUiThread(() -> {
            adapter.notifyDataSetChanged();
            updateDisconnectUi();
        });
    }

    /**
     * Connect to DApp
     *
     * @param paringURI String paring URI
     */
    private void connectToDApp(String paringURI) {
        if (walletConnectHelper == null)
            return;

        walletConnectHelper.connect(paringURI);
    }

    /**
     * Approve DApp
     *
     * @param sessionProposal {@link Sign.Model.SessionProposal}
     */
    private void approveDApp(Sign.Model.SessionProposal sessionProposal) {
        if (walletConnectHelper == null)
            return;

        walletConnectHelper.approveDApp(sessionProposal);
    }

    /**
     * Reject DApp
     *
     * @param sessionProposal {@link Sign.Model.SessionProposal}
     */
    private void rejectDApp(Sign.Model.SessionProposal sessionProposal) {
        if (walletConnectHelper == null)
            return;

        walletConnectHelper.rejectDApp(sessionProposal);
    }

    private void signTransaction(Sign.Model.SessionRequest sessionRequest) {
        if (walletConnectHelper == null)
            return;
        walletConnectHelper.signTransaction(sessionRequest);
    }

    private void rejectTransaction(Sign.Model.SessionRequest sessionRequest) {
        if (walletConnectHelper == null)
            return;
        walletConnectHelper.rejectTransaction(sessionRequest);
    }

    /**
     * Disconnect from dApp
     */
    private void disconnect() {
        if (walletConnectHelper == null)
            return;

        if (adapter.getItemCount() == 0)
            return;

        String topic = adapter.getSelectedItem().session.getTopic();
        walletConnectHelper.disconnect(topic);
        getConnectedSession();
    }

    /**
     * Open scan QR code activity
     */
    private void openScanQRCodeActivity() {
        Intent scanQRIntent = new Intent(this, ScanQRCodeActivity.class);
        scanQRIntent.putExtra(IS_FROM_WALLET_CONNECT, true);
        scanQResult.launch(scanQRIntent);
    }

    /**
     * Open Connect to DApp Activity
     *
     * @param dAppURL String DApp URL
     */
    private void startConnectDAppConsentActivity(String dAppURL) {
        Intent connectDAppIntent = new Intent(this, ConnectDAppConsentActivity.class);
        connectDAppIntent.putExtra(D_APP_URL, dAppURL);
        connectionResult.launch(connectDAppIntent);
    }

    /**
     * Open Connect to DApp Activity
     *
     * @param sessionRequest
     */
    private void startSignTransactionConsent(Sign.Model.SessionRequest sessionRequest) {
        Intent connectDAppIntent = new Intent(this, TransactionRequestConsentActivity.class);
        connectDAppIntent.putExtra(SIGN_TRANSACTION_DATA, BIDUtil.objectToJSONString(sessionRequest,
                true));
        signTransaction.launch(connectDAppIntent);
    }

    /**
     * Validate QR Code data
     * If QR data is invalid show error dialog
     *
     * @param qrData QR code data
     */
    private boolean validateQRCodeData(String qrData) {
        return !TextUtils.isEmpty(qrData);
    }

    /**
     * Show error dialog
     *
     * @param title   to be show on dialog
     * @param message to be show on dialog
     */
    private void showErrorDialog(String title, String message) {
        ErrorDialog errorDialog = new ErrorDialog(this);
        DialogInterface.OnDismissListener onDismissListener = dialogInterface ->
                errorDialog.dismiss();
        errorDialog.show(null, title, message, onDismissListener);
    }

    /**
     * Show progress dialog
     */
    private void showProgressDialog() {
        if (mProgressDialog == null)
            mProgressDialog = new ProgressDialog(this,
                    getString(R.string.label_please_wait));

        mProgressDialog.show();
    }

    /**
     * Hide progress dialog
     */
    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing())
            mProgressDialog.dismiss();
    }
}