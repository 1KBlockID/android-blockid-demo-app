package com.onekosmos.blockidsample.ui;

import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_CONNECTION_ERROR;
import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_SOMETHING_WENT_WRONG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.WindowCompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.ErrorResponse;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.authentication.BIDAuthProvider;
import com.onekosmos.blockid.sdk.authentication.biometric.IBiometricResponseListener;
import com.onekosmos.blockid.sdk.cameramodule.BIDScannerView;
import com.onekosmos.blockid.sdk.cameramodule.QRCodeScanner.QRScannerHelper;
import com.onekosmos.blockid.sdk.cameramodule.camera.qrCodeModule.IOnQRScanResponseListener;
import com.onekosmos.blockid.sdk.datamodel.BIDTenant;
import com.onekosmos.blockidsample.AppConstant;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.apis.GetSessionAuthRequestApi;
import com.onekosmos.blockidsample.model.RegisterTenant;
import com.onekosmos.blockidsample.ui.enrollment.EnrollmentActivity;
import com.onekosmos.blockidsample.ui.qrAuth.AuthenticationPayloadV2;
import com.onekosmos.blockidsample.ui.restore.RestoreAccountActivity;
import com.onekosmos.blockidsample.util.AppPermissionUtils;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.ProgressDialog;
import com.onekosmos.blockidsample.util.ResetSDKMessages;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class RegisterTenantActivity extends AppCompatActivity implements IOnQRScanResponseListener {

    private ConstraintLayout mLayoutAuth, mLayoutRegister, mLayoutQrScan;
    private AppCompatButton mBtnRegisterTenant, mBtnRestore, mBtnDeviceAuth;
    private BIDScannerView mBIDScannerView;
    private RelativeLayout mScannerOverlay;
    private QRScannerHelper mQRScannerHelper;

    // Logo tap tracking
    private int numberOfTaps = 0;
    private long lastTapTimeMs = 0;
    private boolean isDefaultTenantRegistration = true;
    private BIDTenant mScannedTenant;

    private final String[] K_CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private static final int K_QR_CODE_PERMISSION_REQUEST_CODE = 1007;

    private final ActivityResultLauncher<Intent> restoreAccountLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            updateAuthUi();
                        } else {
                            BlockIDSDK.getInstance().resetSDK(AppConstant.licenseKey,
                                    AppConstant.defaultTenant,
                                    ResetSDKMessages.ACCOUNT_RESTORATION_FAILED.getMessage());
                        }
                    });

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 15+
            WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        }

        setContentView(R.layout.activity_register_tenant);

        // Back press handling - toggle between QR scanner and register layout
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mLayoutQrScan.getVisibility() == View.VISIBLE) {
                    mLayoutQrScan.setVisibility(View.GONE);
                    mBIDScannerView.setVisibility(View.GONE);
                    mScannerOverlay.setVisibility(View.GONE);
                    mLayoutRegister.setVisibility(View.VISIBLE);

                    if (mQRScannerHelper != null) {
                        mQRScannerHelper.stopQRScanning();
                    }
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });

        initView();
    }

    private void initView() {
        mLayoutRegister = findViewById(R.id.layout_register);
        mLayoutQrScan = findViewById(R.id.layout_qr_scan);
        mBIDScannerView = findViewById(R.id.bid_scanner_view);
        mScannerOverlay = findViewById(R.id.scanner_overlay);
        mBIDScannerView.setScannerWidthMargin(50, mScannerOverlay);

        mBtnRestore = findViewById(R.id.btn_restore_account);
        mBtnRestore.setOnClickListener(view -> restoreAccount());
        mBtnRegisterTenant = findViewById(R.id.btn_register);
        mBtnRegisterTenant.setOnClickListener(view -> registerTenant());
        mLayoutAuth = findViewById(R.id.layout_auth);
        mBtnDeviceAuth = findViewById(R.id.btn_device_auth);
        mBtnDeviceAuth.setOnClickListener(view -> enrollDeviceAuth());

        // Logo tap listener for hidden QR scanner feature
        AppCompatImageView imgLogo = findViewById(R.id.img_logo);
        imgLogo.setOnClickListener(v -> onLogoClicked());

        // Back button in QR scanner
        AppCompatImageView imgBack = findViewById(R.id.img_back_scan_qr);
        imgBack.setOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());

        if (BlockIDSDK.getInstance().isReady() && !BlockIDSDK.getInstance().isDeviceAuthEnrolled()) {
            updateAuthUi();
        }
    }

    // region Logo Tap & QR Scanner

    private void onLogoClicked() {
        if (numberOfTaps > 0 && (System.currentTimeMillis() - lastTapTimeMs) < 500) {
            numberOfTaps += 1;
        } else {
            numberOfTaps = 1;
        }

        lastTapTimeMs = System.currentTimeMillis();

        if (numberOfTaps == 5) {
            numberOfTaps = 0;
            startQRScanFlow();
        }
    }

    private void startQRScanFlow() {
        if (!AppPermissionUtils.isPermissionGiven(K_CAMERA_PERMISSION, this)) {
            AppPermissionUtils.requestPermission(this, K_QR_CODE_PERMISSION_REQUEST_CODE,
                    K_CAMERA_PERMISSION);
        } else {
            showQRScanner();
        }
    }

    private void showQRScanner() {
        mLayoutQrScan.setVisibility(View.VISIBLE);
        mBIDScannerView.setVisibility(View.VISIBLE);
        mScannerOverlay.setVisibility(View.VISIBLE);
        mLayoutRegister.setVisibility(View.GONE);
        mQRScannerHelper = new QRScannerHelper(this, this, mBIDScannerView);
        mQRScannerHelper.startQRScanning();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == K_QR_CODE_PERMISSION_REQUEST_CODE) {
            if (AppPermissionUtils.isGrantedPermission(requestCode, grantResults,
                    K_CAMERA_PERMISSION, this)) {
                showQRScanner();
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR code",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onQRScanResultResponse(String qrData) {
        mQRScannerHelper.stopQRScanning();
        runOnUiThread(() -> onQRCodeScanResponse(qrData));
    }

    private void onQRCodeScanResponse(String qrResponseB64String) {
        try {
            // UWL 2.0 — Session URL (e.g. https://uat-root.1kosmos.net/sessions/session/<id>)
            if (qrResponseB64String.startsWith("https://") && qrResponseB64String.contains("/sessions/session/")) {
                String[] sessionDetails = qrResponseB64String.split("/session/");
                BlockIDSDK.getInstance().isTrustedSessionSource(sessionDetails[0], isTrusted -> {
                    if (!isTrusted) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Suspicious QR Code", Toast.LENGTH_SHORT).show();
                            hideQRScannerAndShowRegister();
                        });
                        return;
                    }

                    GetSessionAuthRequestApi.initialize();
                    GetSessionAuthRequestApi.getInstance().getSessionAuthRequest(qrResponseB64String, (status, message, error, result) -> runOnUiThread(() -> {
                        if (status && result != null) {
                            try {
                                Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                                AuthenticationPayloadV2 payload = gson.fromJson(result,
                                        AuthenticationPayloadV2.class);

                                if (payload.origin != null && payload.origin.tag != null
                                        && payload.origin.url != null
                                        && payload.origin.communityName != null) {
                                    mScannedTenant = new BIDTenant(payload.origin.tag,
                                            payload.origin.communityName,
                                            payload.origin.url);
                                    isDefaultTenantRegistration = false;
                                    Toast.makeText(this, "Tenant configured: " + payload.origin.tag,
                                            Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Toast.makeText(this, "Failed to parse session data",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            String errMsg = (error != null) ? error.getMessage() : "Failed to get session data";
                            Toast.makeText(this, errMsg, Toast.LENGTH_SHORT).show();
                        }
                        hideQRScannerAndShowRegister();
                    }));
                });
            }
            // Base64-encoded JSON tenant data
            else {
                String qrResponseString = new String(Base64.decode(qrResponseB64String, Base64.NO_WRAP));
                RegisterTenant registerTenant = new Gson().fromJson(qrResponseString, RegisterTenant.class);

                if (registerTenant != null && registerTenant.getTag() != null
                        && registerTenant.getCommunity() != null
                        && registerTenant.getApi() != null) {
                    mScannedTenant = new BIDTenant(registerTenant.getTag(),
                            registerTenant.getCommunity(),
                            registerTenant.getApi());
                    isDefaultTenantRegistration = false;
                    Toast.makeText(this, "Tenant configured: " + registerTenant.getTag(),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Invalid QR code data", Toast.LENGTH_SHORT).show();
                }
                hideQRScannerAndShowRegister();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Invalid QR code format", Toast.LENGTH_SHORT).show();
            hideQRScannerAndShowRegister();
        }
    }

    private void hideQRScannerAndShowRegister() {
        mLayoutQrScan.setVisibility(View.GONE);
        mBIDScannerView.setVisibility(View.GONE);
        mScannerOverlay.setVisibility(View.GONE);
        mLayoutRegister.setVisibility(View.VISIBLE);
    }

    // endregion

    // region Registration

    private void updateAuthUi() {
        mLayoutAuth.setVisibility(View.VISIBLE);
        mBtnRegisterTenant.setVisibility(View.GONE);
        mBtnRestore.setVisibility(View.GONE);
    }

    private void registerTenant() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();
        BlockIDSDK.getInstance().initiateWallet();
        mBtnRegisterTenant.setClickable(false);

        BIDTenant tenant = isDefaultTenantRegistration ? AppConstant.defaultTenant : mScannedTenant;

        BlockIDSDK.getInstance().registerTenant(tenant, (status, error, bidTenant) -> {
            progressDialog.dismiss();
            mBtnRegisterTenant.setClickable(true);
            if (status) {
                BlockIDSDK.getInstance().commitApplicationWallet();
                updateAuthUi();
                return;
            }
            if (error == null)
                error = new ErrorManager.ErrorResponse(K_SOMETHING_WENT_WRONG.getCode(),
                        K_SOMETHING_WENT_WRONG.getMessage());

            ErrorDialog errorDialog = new ErrorDialog(this);
            DialogInterface.OnDismissListener onDismissListener = dialogInterface -> {
                errorDialog.dismiss();
            };
            if (error.getCode() == ErrorManager.CustomErrors.K_CONNECTION_ERROR.getCode()) {
                errorDialog.showNoInternetDialog(onDismissListener);
                return;
            }

            String errorCode = error.getMessage() + " (" + error.getCode() + ").";
            errorDialog.showWithOneButton(null, getString(R.string.label_error),
                    errorCode, getString(R.string.label_ok),
                    onDismissListener);
        });
    }

    // endregion

    // region Device Auth & Restore

    private void enrollDeviceAuth() {
        if (!BlockIDSDK.getInstance().isDeviceAuthEnrolled()) {
            String title = getResources().getString(R.string.label_biometric_auth);
            String desc = getResources().getString(R.string.label_biometric_auth_enroll);
            BIDAuthProvider
                    .getInstance()
                    .enrollDeviceAuth(this, title, desc, false, new IBiometricResponseListener() {
                        @Override
                        public void onBiometricAuthResult(boolean success, ErrorResponse errorResponse) {
                            if (success) {
                                Toast.makeText(RegisterTenantActivity.this,
                                        R.string.label_device_auth_enrolled, Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(RegisterTenantActivity.this,
                                        EnrollmentActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(RegisterTenantActivity.this,
                                        errorResponse.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onNonBiometricAuth(boolean b) {
                            // do nothing
                        }
                    });
        }
    }

    private void restoreAccount() {
        Intent restoreIntent = new Intent(this, RestoreAccountActivity.class);
        restoreAccountLauncher.launch(restoreIntent);
    }

    // endregion
}
