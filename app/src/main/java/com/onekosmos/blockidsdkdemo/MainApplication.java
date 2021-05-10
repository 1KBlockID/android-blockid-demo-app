package com.onekosmos.blockidsdkdemo;

import android.app.Application;

import com.blockid.sdk.BlockIDSDK;

/**
 * Created by Pankti Mistry on 30-04-2021.
 * Copyright © 2020 1Kosmos. All rights reserved.
 */
public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        BlockIDSDK.initialize(this);
        BlockIDSDK.getInstance().setLicenseKey(AppConstant.licenseKey);
    }
}