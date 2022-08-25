package com.onekosmos.blockidsample.ui.walletconnect;

import static com.onekosmos.blockidsample.ui.qrAuth.ScanQRCodeActivity.IS_FROM_WALLET_CONNECT;
import static com.onekosmos.blockidsample.ui.qrAuth.ScanQRCodeActivity.WALLET_CONNECT_QR_DATA;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.onekosmos.blockid.sdk.utils.BIDUtil;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.WalletConnectCallback;
import com.onekosmos.blockidsample.WalletConnectHelper;
import com.onekosmos.blockidsample.ui.qrAuth.ScanQRCodeActivity;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.ProgressDialog;
import com.walletconnect.sign.client.Sign;

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
    private Sign.Model.SessionProposal mSessionProposal;
    private Sign.Model.SessionRequest mSessionRequest;

    private final ActivityResultLauncher<Intent> scanQResult = registerForActivityResult(new
            StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_CANCELED) {
            Toast.makeText(this, getString(R.string.label_qr_code_scanning_canceled), Toast.LENGTH_SHORT).show();
            return;
        }

        if (result.getResultCode() == RESULT_OK) {
            String qrData = result.getData() != null ?
                    result.getData().getStringExtra(WALLET_CONNECT_QR_DATA) : null;
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

    private final ActivityResultLauncher<Intent> signTransactionResult = registerForActivityResult(
            new StartActivityForResult(), result -> {
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

    private final WalletConnectCallback walletConnectCallback = new WalletConnectCallback() {
        @Override
        public void onConnectionStateChange(Sign.Model.ConnectionState connectionState) {
            boolean isConnected = connectionState.isAvailable();
            if (isConnected) {
                updateSessionList();
                hideProgressDialog();
            }
        }

        @Override
        public void onSessionProposal(Sign.Model.SessionProposal sessionProposal) {
            mSessionProposal = sessionProposal;
            startConnectDAppConsentActivity(sessionProposal.getUrl());
        }

        @Override
        public void onSessionSettleResponse(Sign.Model.SettledSessionResponse settleSessionResponse) {
            updateSessionList();
        }

        @Override
        public void onSessionDelete(Sign.Model.DeletedSession deletedSession) {
            updateSessionList();
        }

        @Override
        public void onSessionRequest(Sign.Model.SessionRequest sessionRequest) {
            if (!sessionRequest.getRequest().getMethod().equalsIgnoreCase(
                    "eth_signTransaction")) {
                runOnUiThread(() -> showErrorDialog(getString(R.string.label_error),
                        getString(R.string.label_invalid_session_request)));
                return;
            }
            mSessionRequest = sessionRequest;
            startSignTransactionConsentActivity(sessionRequest);
        }

        @Override
        public void onError(Sign.Model.Error error) {
            if (error != null) {
                runOnUiThread(() -> showErrorDialog(getString(R.string.label_error),
                        error.getThrowable().getMessage()));
            }
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
                getString(R.string.project_id), metadata, walletConnectCallback);
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
        updateDisconnectButton();
    }

    /**
     * Enable/Disable disconnect button
     */
    private void updateDisconnectButton() {
        if (adapter.getItemCount() == 0) {
            mBtnDisconnect.setBackgroundColor(getColor(android.R.color.darker_gray));
            mBtnDisconnect.setEnabled(false);
        } else {
            mBtnDisconnect.setBackgroundColor(getColor(android.R.color.black));
            mBtnDisconnect.setEnabled(true);
        }
    }

    /**
     * Get list of connected session and update in recycler view
     */
    @SuppressLint("NotifyDataSetChanged")
    private void updateSessionList() {
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
            updateDisconnectButton();
        });
    }

    /**
     * Connect to decentralized app
     *
     * @param paringURI String paring URI
     */
    private void connectToDApp(String paringURI) {
        if (walletConnectHelper == null)
            return;

        walletConnectHelper.connect(paringURI);
    }

    /**
     * Approve decentralized app proposal
     *
     * @param sessionProposal {@link Sign.Model.SessionProposal}
     */
    private void approveDApp(Sign.Model.SessionProposal sessionProposal) {
        if (walletConnectHelper == null)
            return;

        walletConnectHelper.approveConnectionRequest(sessionProposal);
    }

    /**
     * Reject decentralized app proposal
     *
     * @param sessionProposal {@link Sign.Model.SessionProposal}
     */
    private void rejectDApp(Sign.Model.SessionProposal sessionProposal) {
        if (walletConnectHelper == null)
            return;

        walletConnectHelper.rejectConnectionRequest(sessionProposal);
    }

    /**
     * Sign requested session
     *
     * @param sessionRequest {@link Sign.Model.SessionRequest}
     */
    private void signTransaction(Sign.Model.SessionRequest sessionRequest) {
        if (walletConnectHelper == null)
            return;
        walletConnectHelper.signTransaction(sessionRequest);
    }

    /**
     * Reject requested session
     *
     * @param sessionRequest {@link Sign.Model.SessionRequest}
     */
    private void rejectTransaction(Sign.Model.SessionRequest sessionRequest) {
        if (walletConnectHelper == null)
            return;
        walletConnectHelper.rejectTransaction(sessionRequest);
    }

    /**
     * Disconnect decentralized app
     */
    private void disconnect() {
        if (walletConnectHelper == null)
            return;

        if (adapter.getItemCount() == 0)
            return;

        String topic = adapter.getSelectedItem().session.getTopic();
        String url = adapter.getSelectedItem().session.getMetaData().getUrl();
        String message = getString(R.string.label_do_you_want_to_disconnect, url);
        ErrorDialog errorDialog = new ErrorDialog(this);
        errorDialog.showWithTwoButton(null, getString(R.string.label_are_you_sure),
                message, getString(R.string.label_yes), getString(R.string.label_no),
                (dialogInterface, which) ->
                        errorDialog.dismiss(),
                dialog -> {
                    errorDialog.dismiss();
                    // call disconnect
                    walletConnectHelper.disconnect(topic);
                    updateSessionList();
                });
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
     * Open {@link ConnectDAppConsentActivity} to approve/reject connection proposal for
     * decentralized app
     *
     * @param dAppURL decentralized app url
     */
    private void startConnectDAppConsentActivity(String dAppURL) {
        Intent connectDAppIntent = new Intent(this, ConnectDAppConsentActivity.class);
        connectDAppIntent.putExtra(D_APP_URL, dAppURL);
        connectionResult.launch(connectDAppIntent);
    }

    /**
     * Open {@link }
     *
     * @param sessionRequest {@link Sign.Model.SessionRequest }
     */
    private void startSignTransactionConsentActivity(Sign.Model.SessionRequest sessionRequest) {
        Intent connectDAppIntent = new Intent(this, SignTransactionConsentActivity.class);
        connectDAppIntent.putExtra(SIGN_TRANSACTION_DATA, BIDUtil.objectToJSONString(sessionRequest,
                true));
        signTransactionResult.launch(connectDAppIntent);
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
        errorDialog.showWithOneButton(null, title, message, getString(R.string.label_ok),
                onDismissListener);
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