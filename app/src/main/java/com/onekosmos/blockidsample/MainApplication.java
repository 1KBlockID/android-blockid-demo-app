package com.onekosmos.blockidsample;

import android.app.Application;

import com.onekosmos.blockid.sdk.BlockIDSDK;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        BlockIDSDK.initialize(this);
        // To set any proxy uncomment below line
        //  BlockIDSDK.getInstance().setProxy("45.95.99.20", 7580, "vautvdmg", "ag2idbos8oo6");
        BlockIDSDK.getInstance().setLicenseKey(AppConstant.licenseKey);
    }

    public static String getVersionNumber() {
        return BuildConfig.VERSION_NAME.toUpperCase().substring(0, 5);
    }

    public static String getBuildNumber() {
        return BuildConfig.VERSION_NAME.toUpperCase().substring(6, 14);
    }
}