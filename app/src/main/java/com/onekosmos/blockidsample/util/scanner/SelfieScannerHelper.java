package com.onekosmos.blockidsample.util.scanner;

import android.content.Context;
import android.util.Base64;

import com.idmetrics.catfishair.CFASelfieController;
import com.idmetrics.catfishair.CFASelfieScanListener;
import com.idmetrics.catfishair.utils.CFASelfieCaptureMode;
import com.idmetrics.catfishair.utils.CFASelfieScanData;
import com.idmetrics.catfishair.utils.CFASelfieSettings;

import java.util.LinkedHashMap;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2022 1Kosmos. All rights reserved.
 */
@SuppressWarnings("FieldCanBeLocal")
public class SelfieScannerHelper {
    private final Context mContext;
    private static final String K_LIVEID = "liveId";
    private final int mImageCompressionQuality = 90;

    /**
     * @param context activity context, on which scanner will start
     */
    public SelfieScannerHelper(Context context) {
        mContext = context;
    }

    /**
     * initialize CFASelfieController and start selfie scan
     *
     * @param callback type of AuthenticIDLiveIDScanCallback to return liveId data, error
     *                 and cancellation of scanner
     */
    public void scanSelfie(boolean shouldAutoCapture, SelfieScanCallback callback) {
        CFASelfieSettings settings = new CFASelfieSettings();
        if (!shouldAutoCapture)
            settings.setSelfieCaptureMode(CFASelfieCaptureMode.MANUAL);
        else
            settings.setSelfieCaptureMode(CFASelfieCaptureMode.AUTO);
        settings.setEnableFarSelfie(false);
        settings.setCompressionQuality(mImageCompressionQuality);
        settings.setEnableSwitchCamera(false);
        CFASelfieController selfieController = CFASelfieController.getInstance(mContext);
        selfieController.scanSelfie(settings, new CFASelfieScanListener() {
            @Override
            public void onFinishSelfieScan(CFASelfieScanData selfieScanData) {
                LinkedHashMap<String, Object> mLiveIDMap = new LinkedHashMap<>();
                mLiveIDMap.put(K_LIVEID, getBase64FromBytes(selfieScanData.getSelfieData()));
                callback.onFinishSelfieScan(mLiveIDMap);
            }

            @Override
            public void onCancelSelfieScan() {
                callback.onCancelSelfieScan();
            }

            @Override
            public void onErrorSelfieScan(int errorCode, String errorMessage) {
                callback.onErrorSelfieScan(errorCode, errorMessage);
            }
        });
    }

    /**
     * @param byteArrayImage byte array of image
     * @return base64 string of byte array
     */
    private String getBase64FromBytes(byte[] byteArrayImage) {
        return Base64.encodeToString(byteArrayImage, Base64.NO_WRAP);
    }

    public interface SelfieScanCallback {
        void onCancelSelfieScan();

        void onErrorSelfieScan(int errorCode, String errorMessage);

        void onFinishSelfieScan(LinkedHashMap<String, Object> documentData);
    }
}
