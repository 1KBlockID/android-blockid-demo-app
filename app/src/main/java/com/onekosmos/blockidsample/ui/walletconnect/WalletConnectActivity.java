package com.onekosmos.blockidsample.ui.walletconnect;

import static com.onekosmos.blockidsample.ui.qrAuth.ScanQRCodeActivity.IS_FROM_WALLET_CONNECT;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
    private WalletConnectHelper walletConnectHelper;
    private List<Sign.Model.Session> mDAppList = new ArrayList<>();
    private DAppAdapter adapter;
    private ProgressDialog mProgressDialog;
    private boolean isConnected;

    private final SignInterface.WalletDelegate walletDelegate = new SignInterface.WalletDelegate() {
        @Override
        public void onSessionProposal(@NonNull Sign.Model.SessionProposal sessionProposal) {
            Log.e("Called", "onSessionProposal-->" +
                    BIDUtil.objectToJSONString(sessionProposal, true));
            approveDApp(sessionProposal);
        }

        @Override
        public void onSessionRequest(@NonNull Sign.Model.SessionRequest sessionRequest) {
            Log.e("Called", "onSessionRequest-->" +
                    BIDUtil.objectToJSONString(sessionRequest, true));
        }

        @Override
        public void onSessionDelete(@NonNull Sign.Model.DeletedSession deletedSession) {
            Log.e("Called", "onSessionDelete-->" +
                    BIDUtil.objectToJSONString(deletedSession, true));
        }

        @Override
        public void onSessionSettleResponse(@NonNull Sign.Model.SettledSessionResponse
                                                    settledSessionResponse) {
            Log.e("Called", "onSessionSettleResponse-->" +
                    BIDUtil.objectToJSONString(settledSessionResponse, true));
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
            Log.e("Called", "onError-->" +
                    BIDUtil.objectToJSONString(error, true));
        }
    };

    private final ActivityResultLauncher<Intent> scanQResult = registerForActivityResult(new
            StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_CANCELED) {
            Toast.makeText(this, getString(R.string.label_qr_code_scanning_canceled),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (result.getResultCode() == RESULT_OK) {
            // process wallet data
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_connect);
        walletConnectHelper = WalletConnectHelper.getInstance();
        initWalletConnect();
        initView();
    }

    /**
     * Initialize UI Objects
     */
    private void initView() {
        AppCompatImageView btnBack = findViewById(R.id.img_back_wallet_connect);
        btnBack.setOnClickListener(view -> onBackPressed());
        AppCompatButton btnConnect = findViewById(R.id.btn_connect_to_d_app);
        btnConnect.setOnClickListener(view -> openScanQRCodeActivity());

        AppCompatButton btnDisconnect = findViewById(R.id.btn_disconnect);
        btnDisconnect.setOnClickListener(view -> {
            // TODO
        });

        adapter = new DAppAdapter(mDAppList);
        RecyclerView recyclerViewDApps = findViewById(R.id.recyclerview_dapp);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewDApps.setLayoutManager(layoutManager);
        recyclerViewDApps.setAdapter(adapter);
    }

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

    @SuppressLint("NotifyDataSetChanged")
    private void getConnectedSession() {
        List<Sign.Model.Session> sessionList = walletConnectHelper.getConnectedSessions();
        if (sessionList == null) {
            return;
        }

        Log.e("sessionList", "" + sessionList.size());
        mDAppList.addAll(sessionList);
        runOnUiThread(() -> adapter.notifyDataSetChanged());
    }

    private void connectToDApp(String paringUri) {
        if (walletConnectHelper == null)
            return;

        walletConnectHelper.connect(paringUri);
    }

    private void approveDApp(Sign.Model.SessionProposal sessionProposal) {
        if (walletConnectHelper == null)
            return;

        walletConnectHelper.approveDApp(sessionProposal);
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
     * Validate QR Code data
     * If QR data is invalid show error dialog
     *
     * @param qrData QR code data
     */
    private boolean validateQRCodeData(String qrData) {
        if (TextUtils.isEmpty(qrData)) {
            return false;
        }
        // check other conditions
        return true;
    }

    /**
     * Show error dialog
     *
     * @param title   to be show on dialog
     * @param message to be show on dialog
     */
    private void showErrorDialog(String title, String message) {
        ErrorDialog errorDialog = new ErrorDialog(this);
        DialogInterface.OnDismissListener onDismissListener = dialogInterface -> {
            errorDialog.dismiss();
        };
        errorDialog.show(null, title, message, onDismissListener);
    }


    private void showProgressDialog() {
        if (mProgressDialog == null)
            mProgressDialog = new ProgressDialog(this,
                    getString(R.string.label_please_wait));

        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing())
            mProgressDialog.dismiss();
    }
}