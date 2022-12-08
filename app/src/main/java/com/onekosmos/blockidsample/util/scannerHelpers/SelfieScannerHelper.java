package com.onekosmos.blockidsample.util.scannerHelpers;

import android.content.Context;

import com.idmetrics.catfishair.CFASelfieController;
import com.idmetrics.catfishair.CFASelfieScanListener;
import com.idmetrics.catfishair.utils.CFASelfieCaptureMode;
import com.idmetrics.catfishair.utils.CFASelfieScanData;
import com.idmetrics.catfishair.utils.CFASelfieSettings;
import com.onekosmos.blockidsample.util.AppUtil;

import java.util.LinkedHashMap;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2022 1Kosmos. All rights reserved.
 */
@SuppressWarnings("FieldCanBeLocal")
public class SelfieScannerHelper {
    private static final String K_LIVEID = "liveId";

    // set default image capture mode
    private static final CFASelfieCaptureMode mCaptureMode = CFASelfieCaptureMode.MANUAL;

    // set Image Compression
    private static final int imageCompressionQuality = 80;

    private final Context mContext;

    /**
     * @param context activity context, on which scanner will start
     */
    public SelfieScannerHelper(Context context) {
        mContext = context;
    }

    /**
     * initialize CFASelfieController and start selfie scan
     *
     * @param callback type of SelfieScanCallback to return liveId data, error
     *                 and cancellation of scanner
     */
    public void scanSelfie(SelfieScanCallback callback) {
        // create and configure selfie settings
        CFASelfieSettings settings = new CFASelfieSettings();

        // capture mode is set to Manual mode
        // can change to auto mode
        settings.setSelfieCaptureMode(mCaptureMode);

        // disable far-selfie option
        settings.setEnableFarSelfie(false);

        // do not show an option to switch camera for taking selfie
        settings.setEnableSwitchCamera(false);

        // set Image Compression
        settings.setCompressionQuality(imageCompressionQuality);

        CFASelfieController selfieController = CFASelfieController.getInstance(mContext);
        selfieController.scanSelfie(settings, new CFASelfieScanListener() {
            @Override
            public void onFinishSelfieScan(CFASelfieScanData selfieScanData) {
                LinkedHashMap<String, Object> mLiveIDMap = new LinkedHashMap<>();
                mLiveIDMap.put(K_LIVEID, AppUtil.getBase64FromBytes(selfieScanData
                        .getSelfieData()));
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

    public interface SelfieScanCallback {
        void onFinishSelfieScan(LinkedHashMap<String, Object> documentData);

        void onErrorSelfieScan(int errorCode, String errorMessage);

        void onCancelSelfieScan();
    }
}
