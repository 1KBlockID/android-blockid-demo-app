package com.onekosmos.blockidsample.ui.dlWebScanner;

import android.content.Context;
import android.os.Handler;
import android.provider.Settings;

import androidx.annotation.Keep;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.ParsedRequestListener;
import com.google.gson.Gson;
import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.datamodel.BIDLinkedAccount;
import com.onekosmos.blockid.sdk.utils.BIDUtil;
import com.onekosmos.blockidsample.AppConstant;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2022 1Kosmos. All rights reserved.
 */
public class SessionApi {
    private static SessionApi sharedInstance;
    private static final String K_DOC_CREATE_SESSION = "/docuverify/document_share_session/create";
    public static final String K_CHECK_SESSION_STATUS = "/docuverify/document_share_session/result";
    private static final String K_PUBLIC_KEYS = "/docuverify/publickeys";
    private final String K_DOC_TYPE_DL = "dl_object";
    private Context mContext;
    private String mSessionID;
    private ICreateSessionResponseCallback createSessionResponseCallback;
    private ISessionStatusResponseCallback sessionStatusResponseCallback;
    Handler mHandler = new Handler();
    private String mUserID;

    private SessionApi() {
    }

    public static SessionApi getInstance() {
        if (sharedInstance == null)
            sharedInstance = new SessionApi();

        return sharedInstance;
    }

    public void createSession(Context context, ICreateSessionResponseCallback callback) {
        mContext = context;
        createSessionResponseCallback = callback;
        createSession(callback);
    }

    public void checkSessionStatus(String sessionId, Context context, ISessionStatusResponseCallback callback) {
        mContext = context;
        this.mSessionID = sessionId;
        sessionStatusResponseCallback = callback;
        verifySession(callback);
    }

    private void createSession(ICreateSessionResponseCallback callback) {
        AndroidNetworking.get(AppConstant.defaultTenant.getDns() + K_PUBLIC_KEYS)
                .addHeaders("Content-Type", "application/json")
                .doNotCacheResponse()
                .build()
                .getAsObject(PublicKeysResponse.class, new ParsedRequestListener<PublicKeysResponse>() {
                    @Override
                    public void onResponse(PublicKeysResponse response) {
                        String publicKey = response.publicKey;
                        createSession(publicKey);
                    }

                    @Override
                    public void onError(ANError anError) {
                        callback.onCreateSessionResponse(false, null,
                                new ErrorManager.ErrorResponse(anError.getErrorCode(),
                                        anError.getMessage()));
                    }
                });
    }

    private void verifySession(ISessionStatusResponseCallback callback) {
        AndroidNetworking.get(AppConstant.defaultTenant.getDns() + K_PUBLIC_KEYS)
                .addHeaders("Content-Type", "application/json")
                .doNotCacheResponse()
                .build()
                .getAsObject(PublicKeysResponse.class, new ParsedRequestListener<PublicKeysResponse>() {
                    @Override
                    public void onResponse(PublicKeysResponse response) {
                        String publicKey = response.publicKey;
                        verifySessionStatus(publicKey);
                    }

                    @Override
                    public void onError(ANError anError) {
                        callback.setSessionStatusResponse(false, null,
                                new ErrorManager.ErrorResponse(anError.getErrorCode(),
                                        anError.getMessage()));
                    }
                });
    }

    private void createSession(String publicKey) {
        BIDLinkedAccount selectedAccount = BlockIDSDK.getInstance().getSelectedAccount().getDataObject();
        if (selectedAccount != null)
            mUserID = selectedAccount.getUserId();
        String reqID = getRequestId(mContext, publicKey);
        SessionRequest sessionRequest = new SessionRequest(AppConstant.defaultTenant.getDns().replace("http://", "").replace("https://", ""),
                AppConstant.defaultTenant.getCommunity(),
                K_DOC_TYPE_DL,
                mUserID,
                BlockIDSDK.getInstance().getDID());
        CreateSessionRequest createSessionRequest = new CreateSessionRequest(AppConstant.dvcId, sessionRequest);
        String strRequest = BIDUtil.objectToJSONString(createSessionRequest, true);
        String encryptedSessionRequest = BlockIDSDK.getInstance().encryptString(strRequest, publicKey);
        CreateSessionRequestModel createSessionRequestModel = new CreateSessionRequestModel(encryptedSessionRequest);

        AndroidNetworking.post(AppConstant.defaultTenant.getDns() + K_DOC_CREATE_SESSION)
                .addHeaders("publickey", BlockIDSDK.getInstance().getPublicKey())
                .addHeaders("licensekey", BlockIDSDK.getInstance().encryptString(AppConstant.licenseKey, publicKey))
                .addHeaders("Content-Type", "application/json")
                .addHeaders("requestid", reqID)
                .addApplicationJsonBody(createSessionRequestModel)
                .build()
                .getAsObject(CreateSessionResponse.class, new ParsedRequestListener<CreateSessionResponse>() {
                    @Override
                    public void onResponse(CreateSessionResponse response) {
                        createSessionResponseCallback.onCreateSessionResponse(true, response, null);
                    }

                    @Override
                    public void onError(ANError anError) {
                        createSessionResponseCallback.onCreateSessionResponse(true, null, null);
                    }
                });
    }

    private void verifySessionStatus(String publicKey) {
        BIDLinkedAccount selectedAccount = BlockIDSDK.getInstance().getSelectedAccount().getDataObject();
        if (selectedAccount != null)
            mUserID = selectedAccount.getUserId();
        mHandler.postDelayed(() -> {
            String reqID = getRequestId(mContext, publicKey);
            SessionRequest sessionRequest = new SessionRequest(AppConstant.defaultTenant.getDns().replace("http://", "").replace("https://", ""),
                    AppConstant.defaultTenant.getCommunity(),
                    K_DOC_TYPE_DL,
                    mUserID,
                    BlockIDSDK.getInstance().getDID());
            VerifySessionRequest verifySessionRequest = new VerifySessionRequest(AppConstant.dvcId, mSessionID);
            String strRequest = BIDUtil.objectToJSONString(verifySessionRequest, true);
            String encryptedSessionRequest = BlockIDSDK.getInstance().encryptString(strRequest, publicKey);
            CreateSessionRequestModel createSessionRequestModel = new CreateSessionRequestModel(encryptedSessionRequest);
            AndroidNetworking.post(AppConstant.defaultTenant.getDns() + K_CHECK_SESSION_STATUS)
                    .setTag(K_CHECK_SESSION_STATUS)
                    .addHeaders("publickey", BlockIDSDK.getInstance().getPublicKey())
                    .addHeaders("licensekey", BlockIDSDK.getInstance().encryptString(AppConstant.licenseKey, publicKey))
                    .addHeaders("Content-Type", "application/json")
                    .addHeaders("requestid", reqID)
                    .doNotCacheResponse()
                    .addApplicationJsonBody(createSessionRequestModel)
                    .build()
                    .getAsObject(SessionStatusResponse.class, new ParsedRequestListener<SessionStatusResponse>() {
                        @Override
                        public void onResponse(SessionStatusResponse response) {
                            String data = response.getData();
                            String decryptedData = BlockIDSDK.getInstance().decryptString(data.toString(), publicKey);
                            SessionStatusDecryptedData sessionStatusDecryptedData = null;
                            try {
                                JSONObject obj = new JSONObject(decryptedData);
                                Gson gson = new Gson();
                                sessionStatusDecryptedData = gson.fromJson(obj.toString(), SessionStatusDecryptedData.class);
                            } catch (JSONException ignore) {
                            }

                            if (!sessionStatusDecryptedData.getResponseStatus().toString().toLowerCase().equals("success")) {
                                // continue polling
                                verifySessionStatus(publicKey);
                            }
                            if (sessionStatusDecryptedData.getResponseStatus().toString().toLowerCase().equals("success")) {
                                sessionStatusResponseCallback.setSessionStatusResponse(true, decryptedData, null);
                            } else
                                sessionStatusResponseCallback.setSessionStatusResponse(false, null, null);

                        }

                        @Override
                        public void onError(ANError anError) {
                            // continue polling
                            verifySessionStatus(publicKey);
                            sessionStatusResponseCallback.setSessionStatusResponse(false, null, null);
                        }
                    });
        }, 500);
    }

    public void stopPolling() {
        mHandler.removeCallbacksAndMessages(null);
    }

    private String getRequestId(Context context, String publicKey) {
        String reqID = "";
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("ts", System.currentTimeMillis() / 1000);
            jsonObject.put("appid", context.getPackageName());
            jsonObject.put("deviceId", Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
            jsonObject.put("uuid", UUID.randomUUID().toString());
            reqID = BlockIDSDK.getInstance().encryptString(jsonObject.toString(), publicKey);
        } catch (JSONException ignore) {
            return null;
        }
        return reqID;
    }

    @Keep
    protected class SessionStatusDecryptedData {
        private String responseStatus;

        public String getResponseStatus() {
            return responseStatus;
        }
    }

    @Keep
    private class PublicKeysResponse {
        private String publicKey;
    }

    @Keep
    protected class SessionStatusResponse {
        private String publicKey;
        private String data;

        public String getPublicKey() {
            return publicKey;
        }

        public String getData() {
            return data;
        }
    }

    @Keep
    public class CreateSessionResponse {
        private String sessionId;
        private String url;

        CreateSessionResponse(String url) {
            this.url = url;
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getUrl() {
            return url;
        }
    }

    @Keep
    private class CreateSessionRequestModel {
        String data;

        private CreateSessionRequestModel(String data) {
            this.data = data;
        }
    }

    @Keep
    private class SessionRequest {
        private String tenantDNS;
        private String communityName;
        private String documentType;
        private String userUID;
        private String did;

        private SessionRequest(String tenantDNS, String communityName, String documentType, String userUID, String did) {
            this.tenantDNS = tenantDNS;
            this.communityName = communityName;
            this.documentType = documentType;
            this.userUID = userUID;
            this.did = did;
        }
    }

    @Keep
    private class VerifySessionRequest {
        String dvcID;
        String sessionId;

        private VerifySessionRequest(String dvcID, String sessionId) {
            this.dvcID = dvcID;
            this.sessionId = sessionId;
        }
    }

    @Keep
    private class CreateSessionRequest {
        String dvcID;
        SessionRequest sessionRequest;

        private CreateSessionRequest(String dvcID, SessionRequest sessionRequest) {
            this.dvcID = dvcID;
            this.sessionRequest = sessionRequest;
        }
    }

    public interface ICreateSessionResponseCallback {
        void onCreateSessionResponse(boolean status, CreateSessionResponse response, ErrorManager.ErrorResponse error);
    }

    public interface ISessionStatusResponseCallback {
        void setSessionStatusResponse(boolean status, String response, ErrorManager.ErrorResponse error);
    }
}
