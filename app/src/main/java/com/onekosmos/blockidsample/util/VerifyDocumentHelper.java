package com.onekosmos.blockidsample.util;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BlockIDSDK;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2022 1Kosmos. All rights reserved.
 */
public class VerifyDocumentHelper {
    private static VerifyDocumentHelper mSharedInstance;
    private static final int K_COMPARE_FACE_FAILED_CODE = 101;
    private static final int K_AUTHENTICATE_DOCUMENT_FAILED_CODE = 102;
    private static final String K_COMPARE_FACE_FAILED_MESSAGE = "Failed to match the selfie";
    private static final String K_AUTHENTICATE_DOCUMENT_FAILED_MESSAGE = "We are unable to verify your document.";
    private static final String K_FACE_COMPARE = "face_compare";
    private static final String K_ID = "id";
    private static final String K_TYPE = "type";
    private static final String K_IMAGE1 = "image1";
    private static final String K_IMAGE2 = "image2";
    public static final String K_PURPOSE = "purpose";
    public static final String K_PURPOSE_DOC_ENROLLMENT = "doc_enrollment";
    private static final String K_CERTIFICATIONS = "certifications";
    private static final String K_VERIFIED = "verified";
    private static final String K_DL_AUTHENTICATE = "dl_authenticate";
    private static final String K_RESULT = "result";
    private static final String K_STATUS = "status";
    private static final String K_MESSAGE = "message";
    private static final String K_DL = "dl";
    private LinkedHashMap<String, Object> mDocumentMap;

    /**
     * private constructor
     * restricted this class itself
     */
    private VerifyDocumentHelper() {

    }

    /**
     * create instance of Singleton class {@link VerifyDocumentHelper }
     *
     * @return {@link VerifyDocumentHelper }
     */
    public static VerifyDocumentHelper getInstance() {
        if (mSharedInstance == null)
            mSharedInstance = new VerifyDocumentHelper();
        return mSharedInstance;
    }

    /**
     * @param base64Image1 base64 image of document
     * @param base64Image2 base64 image of document
     * @param callback     {@link CompareFaceCallback }
     */
    public void compareFace(String base64Image1, String base64Image2,
                            CompareFaceCallback callback) {
        LinkedHashMap<String, Object> faceCompareMap = new LinkedHashMap<>();
        faceCompareMap.put(K_ID, BlockIDSDK.getInstance().getDID() + "." + K_FACE_COMPARE);
        faceCompareMap.put(K_TYPE, K_FACE_COMPARE);
        faceCompareMap.put(K_IMAGE1, base64Image1);
        faceCompareMap.put(K_IMAGE2, base64Image2);
        faceCompareMap.put(K_PURPOSE, K_PURPOSE_DOC_ENROLLMENT);

        BlockIDSDK.getInstance().verifyDocument(faceCompareMap,
                new String[]{K_FACE_COMPARE}, (status, result, errorResponse) -> {
                    if (!status) {
                        callback.onCompareFace(false, errorResponse);
                        return;
                    }

                    boolean verified = false;
                    try {
                        JSONObject resultObject = new JSONObject(result);
                        JSONArray certificates = resultObject.getJSONArray(K_CERTIFICATIONS);
                        JSONObject certificate = certificates.length() > 0
                                ? certificates.getJSONObject(0) : null;

                        if (certificate != null && certificate.has(K_VERIFIED)) {
                            verified = certificate.getBoolean(K_VERIFIED);
                        }
                    } catch (Exception ignored) {
                    }

                    if (!verified) {
                        callback.onCompareFace(false, new ErrorManager.ErrorResponse(
                                K_COMPARE_FACE_FAILED_CODE,
                                K_COMPARE_FACE_FAILED_MESSAGE, result));
                        return;
                    }
                    callback.onCompareFace(true, null);
                });
    }

    /**
     * interface to handle compare face response
     */
    public interface CompareFaceCallback {
        /**
         * @param status        true when two faces are identical else false
         * @param errorResponse {@link ErrorManager.ErrorResponse }
         */
        void onCompareFace(boolean status, ErrorManager.ErrorResponse errorResponse);
    }

    /**
     * Call verify document api to get DL document data
     *
     * @param documentMap DL Object with front_image, front_image_flash and back_image
     */
    public void authenticateDocument(LinkedHashMap<String, Object> documentMap,
                                     AuthenticateDocumentCallback callback) {
        documentMap.put(K_TYPE, K_DL);
        documentMap.put(K_ID, BlockIDSDK.getInstance().getDID() + "." + K_DL);
        BlockIDSDK.getInstance().verifyDocument(documentMap, new String[]{K_DL_AUTHENTICATE},
                (status, result, errorResponse) -> {
                    if (!status) {
                        callback.onAuthenticateDocument(false, null,
                                errorResponse);
                        return;
                    }

                    try {
                        JSONObject resultObject = new JSONObject(result);
                        JSONArray certificates = resultObject.getJSONArray(K_CERTIFICATIONS);
                        String documentData = certificates.length() > 0 &&
                                certificates.getJSONObject(0).has(K_RESULT) ?
                                certificates.getJSONObject(0).getString(K_RESULT) : null;

                        if (TextUtils.isEmpty(documentData)) {
                            int code = K_AUTHENTICATE_DOCUMENT_FAILED_CODE;
                            String message = K_AUTHENTICATE_DOCUMENT_FAILED_MESSAGE;
                            if (certificates.length() > 0) {
                                JSONObject errorObject = certificates.getJSONObject(0);
                                if (errorObject.has(K_STATUS)) {
                                    code = errorObject.getInt(K_STATUS);
                                }
                                if (errorObject.has(K_MESSAGE)) {
                                    message = errorObject.getString(K_MESSAGE);
                                }
                            }
                            callback.onAuthenticateDocument(false, null,
                                    new ErrorManager.ErrorResponse(code, message));
                            return;
                        }

                        JSONObject certificate = certificates.length() > 0
                                ? certificates.getJSONObject(0) : null;
                        boolean verified = false;
                        if (certificate != null && certificate.has(K_VERIFIED)) {
                            verified = certificate.getBoolean(K_VERIFIED);
                        }

                        if (!verified) {
                            callback.onAuthenticateDocument(false, null,
                                    new ErrorManager.ErrorResponse(
                                            K_AUTHENTICATE_DOCUMENT_FAILED_CODE,
                                            K_AUTHENTICATE_DOCUMENT_FAILED_MESSAGE
                                    ));
                            return;
                        }

                        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                        mDocumentMap = gson.fromJson(documentData,
                                new TypeToken<LinkedHashMap<String, Object>>() {
                                }.getType());
                        callback.onAuthenticateDocument(true, mDocumentMap,
                                null);
                    } catch (Exception e) {
                        callback.onAuthenticateDocument(false, null,
                                new ErrorManager.ErrorResponse(K_AUTHENTICATE_DOCUMENT_FAILED_CODE,
                                        K_AUTHENTICATE_DOCUMENT_FAILED_MESSAGE));
                    }
                });
    }

    /**
     * Interface to handle DL Verify response
     */
    public interface AuthenticateDocumentCallback {
        /**
         * @param status        true when verification is successful else false
         * @param documentData  data received as response from VerifyDocument API
         * @param errorResponse {@link ErrorManager.ErrorResponse }
         */
        void onAuthenticateDocument(boolean status,
                                    LinkedHashMap<String, Object> documentData,
                                    ErrorManager.ErrorResponse errorResponse);
    }
}
