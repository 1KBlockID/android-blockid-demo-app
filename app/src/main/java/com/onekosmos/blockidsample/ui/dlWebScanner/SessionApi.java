package com.onekosmos.blockidsample.ui.dlWebScanner;

import android.content.Context;
import android.os.Handler;
import android.provider.Settings;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.ParsedRequestListener;
import com.google.gson.Gson;
import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BlockIDSDK;
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
        String reqID = getRequestId(mContext, publicKey);
        SessionRequest sessionRequest = new SessionRequest(AppConstant.defaultTenant.getDns().replace("http://", "").replace("https://", ""),
                AppConstant.defaultTenant.getCommunity(),
                K_DOC_TYPE_DL,
                "Vaishali", // TODO Vaishali
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
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String reqID = getRequestId(mContext, publicKey);
                SessionRequest sessionRequest = new SessionRequest(AppConstant.defaultTenant.getDns().replace("http://", "").replace("https://", ""),
                        AppConstant.defaultTenant.getCommunity(),
                        K_DOC_TYPE_DL,
                        "Vaishali", // TODO Vaishali
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
                                } catch (JSONException t) {
                                }

                                if (!sessionStatusDecryptedData.getResponseStatus().toString().toLowerCase().equals("success")) {
                                    // continue polling
                                    verifySessionStatus(publicKey);
                                }
                                if (sessionStatusDecryptedData.getResponseStatus().toString().toLowerCase().equals("success")) {
                                    sessionStatusResponseCallback.setSessionStatusResponse(true, sessionStatusDecryptedData, null);
                                } else
                                    sessionStatusResponseCallback.setSessionStatusResponse(false, sessionStatusDecryptedData, null);

                            }

                            @Override
                            public void onError(ANError anError) {
                                // continue polling
                                verifySessionStatus(publicKey);
                                sessionStatusResponseCallback.setSessionStatusResponse(false, null, null);
                            }
                        });
            }
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
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return reqID;
    }

    protected class SessionStatusDecryptedData {
        private String responseStatus;
        private String sessionId;
        public DLObject dl_object;
        public LiveidObject liveid_object;
        public String token;

        public String getResponseStatus() {
            return responseStatus;
        }

        public String getSessionId() {
            return sessionId;
        }

        public DLObject getDl_object() {
            return dl_object;
        }

        public LiveidObject getLiveid_object() {
            return liveid_object;
        }

        public String getToken() {
            return token;
        }
    }

    public class LiveidObject {
        public String id;
        public String type;
        public String category;
        public String proofedBy;
        public String face;
    }

    protected class DLObject {
        private String type;
        private String documentType;
        private String category;
        private String proofedBy;
        private String documentId;
        private String id;
        private String firstName;
        private String lastName;
        private String familyName;
        private String middleName;
        private String givenName;
        private String fullName;
        private String dob;
        private String doe;
        private String doi;
        private String face;
        private String image;
        private String imageBack;
        private String gender;
        private String height;
        private String street;
        private String city;
        private String restrictionCode;
        private String residenceCity;
        private String state;
        private String country;
        private String zipCode;
        private String residenceZipCode;
        private String classificationCode;
        private String complianceType;
        private String placeOfBirth;

        public String getType() {
            return type;
        }

        public String getDocumentType() {
            return documentType;
        }

        public String getCategory() {
            return category;
        }

        public String getProofedBy() {
            return proofedBy;
        }

        public String getDocumentId() {
            return documentId;
        }

        public String getId() {
            return id;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public String getFamilyName() {
            return familyName;
        }

        public String getMiddleName() {
            return middleName;
        }

        public String getGivenName() {
            return givenName;
        }

        public String getFullName() {
            return fullName;
        }

        public String getDob() {
            return dob;
        }

        public String getDoe() {
            return doe;
        }

        public String getDoi() {
            return doi;
        }

        public String getFace() {
            return face;
        }

        public String getImage() {
            return image;
        }

        public String getImageBack() {
            return imageBack;
        }

        public String getGender() {
            return gender;
        }

        public String getHeight() {
            return height;
        }

        public String getStreet() {
            return street;
        }

        public String getCity() {
            return city;
        }

        public String getRestrictionCode() {
            return restrictionCode;
        }

        public String getResidenceCity() {
            return residenceCity;
        }

        public String getState() {
            return state;
        }

        public String getCountry() {
            return country;
        }

        public String getZipCode() {
            return zipCode;
        }

        public String getResidenceZipCode() {
            return residenceZipCode;
        }

        public String getClassificationCode() {
            return classificationCode;
        }

        public String getComplianceType() {
            return complianceType;
        }

        public String getPlaceOfBirth() {
            return placeOfBirth;
        }
    }

    private class PublicKeysResponse {
        private String publicKey;
    }

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

    private class CreateSessionRequestModel {
        String data;

        private CreateSessionRequestModel(String data) {
            this.data = data;
        }
    }

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

    private class VerifySessionRequest {
        String dvcID;
        String sessionId;

        private VerifySessionRequest(String dvcID, String sessionId) {
            this.dvcID = dvcID;
            this.sessionId = sessionId;
        }
    }

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
        void setSessionStatusResponse(boolean status, SessionStatusDecryptedData response, ErrorManager.ErrorResponse error);
    }
}
