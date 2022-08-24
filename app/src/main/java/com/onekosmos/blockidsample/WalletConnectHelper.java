package com.onekosmos.blockidsample;

import android.app.Application;
import android.text.TextUtils;
import android.util.Log;

import com.walletconnect.sign.client.Sign;
import com.walletconnect.sign.client.SignClient;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class WalletConnectHelper {
    private static WalletConnectHelper sharedInstance;
    private static final String WALLET_CONNECT_PROD_RELAY_URL = "relay.walletconnect.com";

    /**
     * private constructor
     * restricted to this class itself
     */
    private WalletConnectHelper() {
    }

    /**
     * create instance of Singleton class {@link WalletConnectHelper}
     *
     * @return {@link WalletConnectHelper}
     */
    public static WalletConnectHelper getInstance() {
        if (sharedInstance == null) {
            sharedInstance = new WalletConnectHelper();
        }
        return sharedInstance;
    }

    public void initializeWalletConnectSDK(Application context, String projectId,
                                           Sign.Model.AppMetaData metaData) {
        // FIXME projectId should not be null
        if (TextUtils.isEmpty(projectId)) {
            Log.e("Init error", "project id is empty");
            return;
        }

        String relayServerUrl = "wss://" + WALLET_CONNECT_PROD_RELAY_URL + "?projectId=" + projectId;
        Log.e("URL", relayServerUrl);

        // Need Application context for this
        Sign.Params.Init init = null;
        try {
            init = new Sign.Params.Init(context, relayServerUrl, metaData,
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
                    return null;
                }
            });
        }catch (Exception e){
            Log.e("Init error2", "Exception");
            e.printStackTrace();
        }
    }

    public void connect() {

    }
}