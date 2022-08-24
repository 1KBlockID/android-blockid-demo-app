package com.onekosmos.blockidsample.ui.walletconnect;

import static com.onekosmos.blockidsample.ui.qrAuth.ScanQRCodeActivity.IS_FROM_WALLET_CONNECT;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;

import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.WalletConnectHelper;
import com.onekosmos.blockidsample.ui.qrAuth.ScanQRCodeActivity;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.walletconnect.sign.client.Sign;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2022 1Kosmos. All rights reserved.
 */
public class WalletConnectActivity extends AppCompatActivity {
    private WalletConnectHelper walletConnectHelper;
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
        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }

    private void initWalletConnect() {
        if (walletConnectHelper == null) {
            return;
        }

        // FIXME  pankti need talk to vinoth about url and redirect

        List<String> iconList = new ArrayList<>();
        iconList.add("https://gblobscdn.gitbook.com/spaces%2F-LJJeCjcLrr53DcT1Ml7%2Favatar.png?alt=media");
        Sign.Model.AppMetaData metadata = new Sign.Model.AppMetaData(
                getString(R.string.app_name),
                getString(R.string.wallet_connect_description),
                "example.wallet", iconList,
                "kotlin-wallet-wc:/request");

        walletConnectHelper.initializeWalletConnectSDK(getApplication(),
                "932edbeee51ba767c6e1fb7947b92c39", metadata);
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
}