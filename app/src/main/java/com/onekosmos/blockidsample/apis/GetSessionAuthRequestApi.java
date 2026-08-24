package com.onekosmos.blockidsample.apis;

import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_CONNECTION_ERROR;

import androidx.annotation.Keep;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.StringRequestListener;
import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ApiResponseCallback;
import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class GetSessionAuthRequestApi {
    private static GetSessionAuthRequestApi mGetSessionAuthRequestApi;

    public static GetSessionAuthRequestApi getInstance() {
        return mGetSessionAuthRequestApi;
    }

    public static void initialize() {
        if (mGetSessionAuthRequestApi == null) {
            mGetSessionAuthRequestApi = new GetSessionAuthRequestApi();
        }
    }

    public void getSessionAuthRequest(String url, ApiResponseCallback<String> apiResponseCallback) {
        AndroidNetworking.get(url)
                .addHeaders("Connection", "Keep-Alive")
                .build()
                .getAsString(new StringRequestListener() {
                    @Override
                    public void onResponse(String response) {
                        if (response != null)
                            apiResponseCallback.apiResponse(true, "Data Fetched Successfully", null, response);
                        else
                            apiResponseCallback.apiResponse(false, SessionAuthRequestError.K_SESSION_AUTH_MODEL_NOT_FOUND.getMessage(),
                                    new ErrorManager.ErrorResponse(SessionAuthRequestError.K_SESSION_AUTH_MODEL_NOT_FOUND.getCode(),
                                            SessionAuthRequestError.K_SESSION_AUTH_MODEL_NOT_FOUND.getMessage()), null);
                    }

                    @Override
                    public void onError(ANError anError) {
                        if (anError.getErrorCode() == K_CONNECTION_ERROR.getCode())
                            apiResponseCallback.apiResponse(false, K_CONNECTION_ERROR.getMessage(),
                                    new ErrorManager.ErrorResponse(K_CONNECTION_ERROR.getCode(),
                                            K_CONNECTION_ERROR.getMessage()), null);
                        else
                            apiResponseCallback.apiResponse(false, anError.getMessage(),
                                    new ErrorManager.ErrorResponse(anError.getErrorCode(),
                                            anError.getMessage()), null);
                    }
                });
    }

    @Keep
    public enum SessionAuthRequestError {
        K_SESSION_AUTH_MODEL_NOT_FOUND(1006, "Session auth model not found"),
        K_REQUESTED_SESSION_NOT_AVAILABLE(1007, "Requested session is no longer available.");

        private final int code;
        private final String message;

        SessionAuthRequestError(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}
