package com.onekosmos.blockidsample;

import android.app.Application;
import android.text.TextUtils;
import android.util.Log;

import com.walletconnect.sign.client.Sign;
import com.walletconnect.sign.client.SignClient;

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
        if (TextUtils.isEmpty(projectId)) {
            Log.e("Init error", "project id is empty");
            return;
        }

        String relayServerUrl = "wss://" + WALLET_CONNECT_PROD_RELAY_URL + "?projectId=" +
                projectId;

        Sign.Params.Init init = null;
        try {
            init = new Sign.Params.Init(context, relayServerUrl, metaData,
                    null, Sign.ConnectionType.AUTOMATIC);

        } catch (Exception e) {
            Log.e("Init error1", "Exception --> " + e.getMessage());
            return;
        }

        SignClient.INSTANCE.initialize(init, error -> {
            if (error != null) {
                // Software caused connection abort (offline)
                Log.e("Init2 Error", error.getThrowable().getMessage());
            } else {
                Log.e("Init", "success");
            }
            return null;
        });
    }

    public void connect() {

    }
}