package com.onekosmos.blockidsample.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.authentication.BIDAuthProvider;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.ui.enrollment.EnrollmentActivity;
import com.onekosmos.blockidsample.ui.login.LoginActivity;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class SplashActivity extends AppCompatActivity {
    private static int K_SPLASH_SCREEN_TIME_OUT = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Handler handler = new Handler();
        handler.postDelayed(() -> checkTenantRegistered(), K_SPLASH_SCREEN_TIME_OUT);
    }

    private void checkTenantRegistered() {
        Intent intent;
        if (BlockIDSDK.getInstance().isReady() && BlockIDSDK.getInstance().isDeviceAuthEnrolled()) {

            BIDAuthProvider.getInstance().unlockSDK();
            intent = new Intent(this, EnrollmentActivity.class);
        } else {
            intent = new Intent(this, RegisterTenantActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        finish();
    }
}