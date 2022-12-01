package com.onekosmos.blockidsample.util;

import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_LIVEID_DOC_FACE_NOT_MATCH;

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
}
