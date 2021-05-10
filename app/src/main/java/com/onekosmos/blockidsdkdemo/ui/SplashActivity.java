package com.onekosmos.blockidsdkdemo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.blockid.sdk.BlockIDSDK;
import com.example.blockidsdkdemo.R;
import com.onekosmos.blockidsdkdemo.ui.enrollment.EnrollmentActivity;
import com.onekosmos.blockidsdkdemo.ui.login.LoginActivity;

/**
 * Created by Pankti Mistry on 30-04-2021.
 * Copyright © 2020 1Kosmos. All rights reserved.
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
            intent = new Intent(this, LoginActivity.class);
        } else {
            intent = new Intent(this, RegisterTenantActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        finish();
    }
}