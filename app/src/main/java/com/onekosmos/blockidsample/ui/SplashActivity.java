package com.onekosmos.blockidsample.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.ui.login.LoginActivity;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class SplashActivity extends AppCompatActivity {
    private static int K_SPLASH_SCREEN_TIME_OUT = 1000;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 🔒 Lock the orientation to portrait
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 15+
            WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        }

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