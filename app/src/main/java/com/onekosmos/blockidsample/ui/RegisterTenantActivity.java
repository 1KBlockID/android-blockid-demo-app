package com.onekosmos.blockidsample.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.blockid.sdk.BlockIDSDK;
import com.blockid.sdk.authentication.BIDAuthProvider;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.AppConstant;
import com.onekosmos.blockidsample.ui.enrollment.EnrollmentActivity;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.ProgressDialog;

import static com.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_SOMETHING_WENT_WRONG;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class RegisterTenantActivity extends AppCompatActivity {
    private ConstraintLayout mLayoutAuth;
    private AppCompatButton mBtnRegisterTenant, mBtnDeviceAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_tenant);
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
    }

    private void registerTenant() {
        BlockIDSDK.getInstance().initiateWallet();
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();
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
            errorDialog.show(null, getString(R.string.label_error), error.getMessage(), onDismissListener);
        });
    }

    private void enrollDeviceAuth() {
        if (!BlockIDSDK.getInstance().isDeviceAuthEnrolled()) {
            String title = getResources().getString(R.string.label_biometric_auth);
            String desc = getResources().getString(R.string.label_biometric_auth_enroll);
            BIDAuthProvider
                    .getInstance()
                    .enrollDeviceAuth(this, title, desc, false, (success, errorResponse) -> {
                        if (success) {
                            Toast.makeText(this, R.string.label_device_auth_enrolled, Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(this, EnrollmentActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(this, errorResponse.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}