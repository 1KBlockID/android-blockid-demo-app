package com.onekosmos.blockidsdkdemo;

import android.content.Context;

import com.onekosmos.blockidsdkdemo.util.SharedPreferenceUtil;

import static com.onekosmos.blockidsdkdemo.AppVault.VaultKeys.K_DL;
import static com.onekosmos.blockidsdkdemo.AppVault.VaultKeys.K_LIVEID;
import static com.onekosmos.blockidsdkdemo.AppVault.VaultKeys.K_PP;

/**
 * Created by Pankti Mistry on 03-05-2021.
 * Copyright © 2020 1Kosmos. All rights reserved.
 */
public class AppVault {

    private static AppVault sharedInstance;
    private Context context;
    private SharedPreferenceUtil prefs;

    public static void initialize(Context context) {
        if (sharedInstance == null) {
            sharedInstance = new AppVault();
            sharedInstance.context = context;
            SharedPreferenceUtil.initialize(context);
        }
    }

    public static AppVault getInstance() {
        return sharedInstance;
    }

    private AppVault() {

    }

    public void setLiveID(String base64LiveId) {
        SharedPreferenceUtil.getInstance().setString(K_LIVEID, base64LiveId);
    }

    public String getLiveID() {
        return SharedPreferenceUtil.getInstance().getString(K_LIVEID);
    }

    public void setDLData(String dlData) {
        SharedPreferenceUtil.getInstance().setString(K_DL, dlData);
    }

    public String getDLData() {
        return SharedPreferenceUtil.getInstance().getString(K_DL);
    }

    public void setPPData(String ppData) {
        SharedPreferenceUtil.getInstance().setString(K_PP, ppData);
    }

    public String getPPData() {
        return SharedPreferenceUtil.getInstance().getString(K_PP);
    }

    protected class VaultKeys {
        public static final String K_LIVEID = "K_LIVEID";
        public static final String K_DL = "K_DL";
        public static final String K_PP = "K_PP";
    }
}
