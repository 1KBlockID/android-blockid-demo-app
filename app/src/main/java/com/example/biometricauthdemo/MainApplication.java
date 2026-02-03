package com.example.biometricauthdemo;

import android.app.Application;

import com.onekosmos.blockid.sdk.BlockIDSDK;

public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        BlockIDSDK.initialize(this);

        BlockIDSDK.getInstance().setLicenseKey(AppConstant.licenseKey);
    }
}