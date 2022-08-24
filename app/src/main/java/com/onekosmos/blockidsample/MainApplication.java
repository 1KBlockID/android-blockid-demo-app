package com.onekosmos.blockidsample;

import static com.google.android.gms.common.util.CollectionUtils.listOf;

import android.app.Application;
import android.util.Log;

import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockidsample.util.SharedPreferenceUtil;
import com.walletconnect.sign.client.Sign;
import com.walletconnect.sign.client.SignClient;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        BlockIDSDK.initialize(this);

        SharedPreferenceUtil.initialize(this);
        // To set any proxy uncomment below line
        //  BlockIDSDK.getInstance().setProxy("45.95.99.20", 7580, "vautvdmg", "ag2idbos8oo6");
        BlockIDSDK.getInstance().setLicenseKey(AppConstant.licenseKey);
        BlockIDSDK.getInstance().setDvcId(AppConstant.dvcId);


        Sign.Params.Init init = null;
        try {
            Sign.Model.AppMetaData metadata = new Sign.Model.AppMetaData(
                    "BlockID Demo",
                    "Wallet description",
                    "example.wallet",
                    listOf("https://gblobscdn.gitbook.com/spaces%2F-LJJeCjcLrr53DcT1Ml7%2Favatar.png?alt=media"),
                    "kotlin-wallet-wc:/request");

            init = new Sign.Params.Init(this,
                    "wss://relay.walletconnect.com?projectId=932edbeee51ba767c6e1fb7947b92c39",
                    metadata,
                    null, Sign.ConnectionType.AUTOMATIC);

        } catch (Exception e) {
            Log.e("Init error1", "Exception");
            e.printStackTrace();
        }
        if(init == null){
            Log.e("Init error1", "Wallet connect init method");
        }
        try {
//            SignClient.INSTANCE.initialize(init, error -> {
//                error.getThrowable().printStackTrace();
//                Log.e("Error", error.getThrowable().getMessage());
//                return null;
//            });
            SignClient.INSTANCE.initialize(init, new Function1<Sign.Model.Error, Unit>() {
                @Override
                public Unit invoke(Sign.Model.Error error) {
                    if(error != null){

                    }
                    return null;
                }
            });
        }catch (Exception e){
            Log.e("Init error2", "Exception");
            e.printStackTrace();
        }
    }

    public static String getVersionNumber() {
        return BuildConfig.VERSION_NAME.toUpperCase().substring(0, 5);
    }

    public static String getBuildNumber() {
        return BuildConfig.VERSION_NAME.toUpperCase().substring(6, 14);
    }
}