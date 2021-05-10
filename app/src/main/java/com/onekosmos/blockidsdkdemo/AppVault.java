package com.onekosmos.blockidsdkdemo;

import android.content.Context;

import com.onekosmos.blockidsdkdemo.util.SharedPreferenceUtil;

import static com.onekosmos.blockidsdkdemo.AppVault.VaultKeys.K_PP;

/**
 * Created by Pankti Mistry on 03-05-2021.
 * Copyright © 2020 1Kosmos. All rights reserved.
 */
public class AppVault {

    private static AppVault sharedInstance;

    public static void initialize(Context context) {
        if (sharedInstance == null) {
            sharedInstance = new AppVault();
            SharedPreferenceUtil.initialize(context);
        }
    }

    public static AppVault getInstance() {
        return sharedInstance;
    }

    private AppVault() {

    }

    public void setPPData(String ppData) {
        SharedPreferenceUtil.getInstance().setString(K_PP, ppData);
    }

    public String getPPData() {
        return SharedPreferenceUtil.getInstance().getString(K_PP);
    }

    protected class VaultKeys {
        public static final String K_PP = "K_PP";
    }
}
