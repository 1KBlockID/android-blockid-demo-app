package com.onekosmos.onekosmossample.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.ErrorResponse;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.authentication.BIDAuthProvider;
import com.onekosmos.blockid.sdk.authentication.biometric.IBiometricResponseListener;
import com.onekosmos.onekosmossample.AppConstant;
import com.onekosmos.onekosmossample.R;
import com.onekosmos.onekosmossample.ui.enrollment.EnrollmentActivity;
import com.onekosmos.onekosmossample.ui.restore.RestoreAccountActivity;
import com.onekosmos.onekosmossample.util.ErrorDialog;
import com.onekosmos.onekosmossample.util.ProgressDialog;
import com.onekosmos.onekosmossample.util.ResetSDKMessages;

import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_SOMETHING_WENT_WRONG;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class RegisterTenantActivity extends AppCompatActivity {
    private static int RESTORE_REQUEST_CODE = 1001;
    private ConstraintLayout mLayoutAuth;
    private AppCompatButton mBtnRegisterTenant, mBtnRestore, mBtnDeviceAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_tenant);
        mBtnRestore = findViewById(R.id.btn_restore_account);
        mBtnRestore.setOnClickListener(view -> restoreAccount());
        mBtnRegisterTenant = findViewById(R.id.btn_register);
        mBtnRegisterTenant.setOnClickListener(view -> registerTenant());
        mLayoutAuth = findViewById(R.id.layout_auth);
        mBtnDeviceAuth = findViewById(R.id.btn_device_auth);
        mBtnDeviceAuth.setOnClickListener(view -> enrollDeviceAuth());
        if (BlockIDSDK.getInstance().isReady() && !BlockIDSDK.getInstance().isDeviceAuthEnrolled()) {
            updateAuthUi();
        }
    }

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
        BlockIDSDK.getInstance().registerTenant(AppConstant.defaultTenant, (status, error, bidTenant) -> {
            progressDialog.dismiss();
            mBtnRegisterTenant.setClickable(true);
            if (status) {
                BlockIDSDK.getInstance().commitApplicationWallet();
                updateAuthUi();
                return;
            }
            if (error == null)
                error = new ErrorManager.ErrorResponse(K_SOMETHING_WENT_WRONG.getCode(), K_SOMETHING_WENT_WRONG.getMessage());

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
                                Toast.makeText(RegisterTenantActivity.this, R.string.label_device_auth_enrolled, Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(RegisterTenantActivity.this, EnrollmentActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(RegisterTenantActivity.this, errorResponse.getMessage(), Toast.LENGTH_SHORT).show();
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
        startActivityForResult(restoreIntent, RESTORE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESTORE_REQUEST_CODE && resultCode == RESULT_OK) {
            updateAuthUi();
        } else {
            BlockIDSDK.getInstance().resetSDK(AppConstant.licenseKey, AppConstant.defaultTenant,
                    ResetSDKMessages.ACCOUNT_RESTORATION_FAILED.getMessage());
        }
    }
}