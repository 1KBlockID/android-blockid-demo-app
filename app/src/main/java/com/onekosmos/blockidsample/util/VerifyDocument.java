package com.onekosmos.blockidsample.util;

import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_DOCUMENT_VERIFICATION_FAILED;
import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_LIVEID_DOC_FACE_NOT_MATCH;
import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_SOMETHING_WENT_WRONG;

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
public class VerifyDocument {
    private static VerifyDocument mSharedInstance;
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
    private LinkedHashMap<String, Object> mDocumentMap;


    /**
     * private constructor
     * restricted this class itself
     */
    private VerifyDocument() {

    }

    /**
     * create instance of Singleton class {@link VerifyDocument }
     *
     * @return {@link VerifyDocument }
     */
    public static VerifyDocument getInstance() {
        if (mSharedInstance == null)
            mSharedInstance = new VerifyDocument();
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
                                K_LIVEID_DOC_FACE_NOT_MATCH.getCode(),
                                K_LIVEID_DOC_FACE_NOT_MATCH.getMessage(), result));
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
     * @param documentMap DL Object with front_image, front_image_flash and back_image,
     */
    public void verifyDL(LinkedHashMap<String, Object> documentMap, VerifyDLCallback callback) {
        documentMap.put(K_TYPE, "dl");
        documentMap.put(K_ID, BlockIDSDK.getInstance().getDID() + ".dl");
        BlockIDSDK.getInstance().verifyDocument(
                documentMap, new String[]{K_DL_AUTHENTICATE},
                (status, result, errorResponse) -> {
                    if (!status) {
                        callback.onVerifyDL(false, null, errorResponse);
                        return;
                    }

                    try {
                        JSONObject resultObject = new JSONObject(result);
                        JSONArray certificates = resultObject.getJSONArray(K_CERTIFICATIONS);
                        String documentData = certificates.length() > 0 &&
                                certificates.getJSONObject(0).has(K_RESULT) ?
                                certificates.getJSONObject(0).getString(K_RESULT) : null;

                        if (TextUtils.isEmpty(documentData)) {
                            int code = K_SOMETHING_WENT_WRONG.getCode();
                            String message = K_SOMETHING_WENT_WRONG.getMessage();
                            if (certificates.length() > 0) {
                                JSONObject errorObject = certificates.getJSONObject(0);
                                if (errorObject.has(K_STATUS)) {
                                    code = errorObject.getInt(K_STATUS);
                                }
                                if (errorObject.has(K_MESSAGE)) {
                                    message = errorObject.getString(K_MESSAGE);
                                }
                            }
                            callback.onVerifyDL(false, null,
                                    new ErrorManager.ErrorResponse(code, message));
                            return;
                        }
                        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                        mDocumentMap = gson.fromJson(documentData,
                                new TypeToken<LinkedHashMap<String, Object>>() {
                                }.getType());

                        if (certificates.getJSONObject(0).getBoolean(K_VERIFIED)) {
                            callback.onVerifyDL(true, mDocumentMap, null);
                        } else {
                            callback.onVerifyDL(false, null,
                                    new ErrorManager.ErrorResponse(
                                            K_DOCUMENT_VERIFICATION_FAILED.getCode(),
                                            K_DOCUMENT_VERIFICATION_FAILED.getMessage()
                                    ));
                        }
                    } catch (Exception e) {
                        callback.onVerifyDL(false, null,
                                new ErrorManager.ErrorResponse(K_SOMETHING_WENT_WRONG.getCode(),
                                        K_SOMETHING_WENT_WRONG.getMessage()));
                    }
                });
    }

    /**
     * Interface to handle DL Verify response
     */
    public interface VerifyDLCallback {
        /**
         * @param status        true when verification is successful else false
         * @param documentData  data received as response from VerifyDocument API
         * @param errorResponse {@link ErrorManager.ErrorResponse }
         */
        void onVerifyDL(boolean status,
                        LinkedHashMap<String, Object> documentData,
                        ErrorManager.ErrorResponse errorResponse);
    }
}
