package com.onekosmos.blockidsample.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.ErrorResponse;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.authentication.BIDAuthProvider;
import com.onekosmos.blockid.sdk.authentication.biometric.IBiometricResponseListener;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.ui.enrollment.EnrollmentActivity;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class LoginActivity extends AppCompatActivity {
    private static final int K_PIN_VERIFICATION_REQUEST_CODE = 1189;
    private AppCompatButton mBtnAppPin, mBtnDeviceAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initView();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == K_PIN_VERIFICATION_REQUEST_CODE && resultCode == RESULT_OK) {
            onLoginSuccess();
        }
    }

    private void initView() {
        mBtnAppPin = findViewById(R.id.btn_pin);
        mBtnDeviceAuth = findViewById(R.id.btn_device_auth);
        if (!BlockIDSDK.getInstance().isDeviceAuthEnrolled()) {
            mBtnDeviceAuth.setBackgroundColor(getColor(android.R.color.darker_gray));
        }
        if (!BlockIDSDK.getInstance().isPinRegistered()) {
            mBtnAppPin.setBackgroundColor(getColor(android.R.color.darker_gray));
        }

        mBtnDeviceAuth.setOnClickListener(view -> deviceAuthLogin());
        mBtnAppPin.setOnClickListener(view -> pinLogin());
    }

    private void deviceAuthLogin() {
        if (BlockIDSDK.getInstance().isReady() && BlockIDSDK.getInstance().isDeviceAuthEnrolled()) {
            String title = getResources().getString(R.string.label_biometric_auth);
            String desc = getResources().getString(R.string.label_biometric_auth_req);
            BIDAuthProvider.getInstance().verifyDeviceAuth(this, title, desc, false, new IBiometricResponseListener() {
                @Override
                public void onBiometricAuthResult(boolean status, ErrorResponse errorResponse) {
                    if (status)
                        onLoginSuccess();
                }

                @Override
                public void onNonBiometricAuth(boolean b) {
                    // do nothing
                }
            });
        }
    }

    private void pinLogin() {
        if (BlockIDSDK.getInstance().isReady() && BlockIDSDK.getInstance().isPinRegistered()) {
            Intent intent = new Intent(this, PinVerificationActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivityForResult(intent, K_PIN_VERIFICATION_REQUEST_CODE);
        }
    }

    private void onLoginSuccess() {
        Toast.makeText(this, R.string.label_login_successfully, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, EnrollmentActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        finish();
    }
}