package com.onekosmos.blockidsample.ui.qrAuth;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.StringRequestListener;
import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2022 1Kosmos. All rights reserved.
 */
public class GetSessionData {
    private static GetSessionData sharedInstance;

    private GetSessionData() {
    }

    public static GetSessionData getInstance() {
        if (sharedInstance == null)
            sharedInstance = new GetSessionData();

        return sharedInstance;
    }

    public void getSessionData(String url, SessionResponseCallback callback) {
        AndroidNetworking.get(url)
                .build()
                .getAsString(new StringRequestListener() {
                    @Override
                    public void onResponse(String response) {
                        callback.onSessionResponse(true, response, null);
                    }

                    @Override
                    public void onError(ANError anError) {
                        callback.onSessionResponse(false, null,
                                new ErrorManager.ErrorResponse(anError.getErrorCode(),
                                        anError.getErrorBody() != null ? anError.getErrorBody() :
                                                anError.getMessage()));
                    }
                });
    }

    interface SessionResponseCallback {
        void onSessionResponse(boolean status, String response, ErrorManager.ErrorResponse error);
    }
}