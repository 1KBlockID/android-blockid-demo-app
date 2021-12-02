package com.onekosmos.blockidsample;

import android.app.Application;

import com.onekosmos.blockid.sdk.BlockIDSDK;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class MainApplication extends Application {
    int proxyPort = 7580;
    String proxyHost = "45.95.99.20";
    final String username = "vautvdmg";
    final String password = "ag2idbos8oo6";

    @Override
    public void onCreate() {
        super.onCreate();
        BlockIDSDK.initialize(this);
        BlockIDSDK.getInstance().setProxy(proxyHost, proxyPort, username, password);
        BlockIDSDK.getInstance().setLicenseKey(AppConstant.licenseKey);
    }

    public static String getVersionNumber() {
        return BuildConfig.VERSION_NAME.toUpperCase().substring(0, 5);
    }

    public static String getBuildNumber() {
        return BuildConfig.VERSION_NAME.toUpperCase().substring(6, 14);
    }
}