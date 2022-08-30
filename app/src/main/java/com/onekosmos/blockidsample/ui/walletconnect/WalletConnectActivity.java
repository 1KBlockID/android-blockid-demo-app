package com.onekosmos.blockidsample.ui.walletconnect;

import static com.onekosmos.blockidsample.ui.qrAuth.ScanQRCodeActivity.IS_FROM_WALLET_CONNECT;
import static com.onekosmos.blockidsample.ui.qrAuth.ScanQRCodeActivity.WALLET_CONNECT_QR_DATA;

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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.annimon.stream.Stream;
import com.onekosmos.blockid.sdk.walletconnect.WalletConnectHelper;
import com.onekosmos.blockidsample.BaseActivity;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.ui.qrAuth.ScanQRCodeActivity;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.walletconnect.sign.client.Sign;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2022 1Kosmos. All rights reserved.
 */
public class WalletConnectActivity extends AppCompatActivity {
    private final List<DAppAdapter.DAppData> mDAppList = new ArrayList<>();
    private WalletConnectHelper mWalletConnectHelper;
    private DAppAdapter mDAppAdapter;
    private AppCompatButton mBtnDisconnect;
    private final Observer<List<Sign.Model.Session>> userListUpdateObserver =
            this::updateSessionList;
    private DAppViewModel viewModel;

    private final ActivityResultLauncher<Intent> scanQResult = registerForActivityResult(new
            StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_CANCELED) {
            Toast.makeText(this, getString(R.string.label_qr_code_scanning_canceled),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (result.getResultCode() == RESULT_OK) {
            String qrData = result.getData() != null ?
                    result.getData().getStringExtra(WALLET_CONNECT_QR_DATA) : null;
            if (TextUtils.isEmpty(qrData)) {
                showErrorDialog(getString(R.string.label_invalid_qr_code),
                        getString(R.string.label_qr_code_scan_failed));
            }

            mWalletConnectHelper.connect(Objects.requireNonNull(qrData));
        }
    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_connect);
        mWalletConnectHelper = WalletConnectHelper.getInstance();
        viewModel = BaseActivity.getModel();
        initView();
    }

    /**
     * Initialize UI Objects
     */
    private void initView() {
        AppCompatImageView btnBack = findViewById(R.id.img_back_wallet_connect);
        btnBack.setOnClickListener(view -> onBackPressed());
        AppCompatButton btnConnect = findViewById(R.id.btn_connect_to_d_app);
        btnConnect.setOnClickListener(view -> startScanQRCodeActivity());

        mBtnDisconnect = findViewById(R.id.btn_disconnect);
        mBtnDisconnect.setOnClickListener(view -> disconnect());

        mDAppAdapter = new DAppAdapter(mDAppList);
        RecyclerView recyclerViewDApps = findViewById(R.id.recyclerview_dapp);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewDApps.setLayoutManager(layoutManager);
        recyclerViewDApps.setAdapter(mDAppAdapter);

        ViewGroup.LayoutParams params = recyclerViewDApps.getLayoutParams();
        recyclerViewDApps.setLayoutParams(params);
        updateDisconnectButton();

        viewModel.getUserMutableLiveData().observe(this, userListUpdateObserver);
    }

    /**
     * Enable/Disable disconnect button
     */
    private void updateDisconnectButton() {
        if (mDAppAdapter.getItemCount() == 0) {
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
    private void updateSessionList(List<Sign.Model.Session> userArrayList) {
        if (userArrayList == null) {
            return;
        }

        mDAppList.clear();
        for (int index = 0; index < userArrayList.size(); index++) {
            DAppAdapter.DAppData data = new DAppAdapter.DAppData();
            data.session = userArrayList.get(index);
            mDAppList.add(data);
        }
        runOnUiThread(() -> {
            mDAppAdapter.notifyDataSetChanged();
            updateDisconnectButton();
        });
    }

    /**
     * Disconnect from session
     */
    private void disconnect() {
        if (mDAppAdapter.getItemCount() == 0)
            return;

        Sign.Model.Session session = mDAppAdapter.getSelectedItem().session;
        Sign.Model.AppMetaData metaData = session.getMetaData();
        String url = metaData != null ? metaData.getUrl() : "";
        String message = getString(R.string.label_do_you_want_to_disconnect, url);
        ErrorDialog errorDialog = new ErrorDialog(this);
        errorDialog.showWithTwoButton(null, getString(R.string.label_are_you_sure),
                message, getString(R.string.label_yes), getString(R.string.label_no),
                (dialogInterface, which) ->
                        errorDialog.dismiss(),
                dialog -> {
                    errorDialog.dismiss();
                    // call disconnect
                    mWalletConnectHelper.disconnect(session);
                    updateSessionList(mWalletConnectHelper.getActiveSessions());
                });
    }

    /**
     * Open scan QR code activity
     */
    private void startScanQRCodeActivity() {
        Intent scanQRIntent = new Intent(this, ScanQRCodeActivity.class);
        scanQRIntent.putExtra(IS_FROM_WALLET_CONNECT, true);
        scanQResult.launch(scanQRIntent);
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
}