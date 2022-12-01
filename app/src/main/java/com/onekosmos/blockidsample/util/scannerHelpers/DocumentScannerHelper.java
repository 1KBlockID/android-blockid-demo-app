package com.onekosmos.blockidsample.util.scannerHelpers;

import android.content.Context;

import com.idmetrics.dc.DSHandler;
import com.idmetrics.dc.utils.DSCaptureMode;
import com.idmetrics.dc.utils.DSError;
import com.idmetrics.dc.utils.DSHandlerListener;
import com.idmetrics.dc.utils.DSID1Options;
import com.idmetrics.dc.utils.DSID1Type;
import com.idmetrics.dc.utils.DSResult;
import com.idmetrics.dc.utils.DSSide;
import com.idmetrics.dc.utils.FlashCapture;
import com.onekosmos.blockidsample.AppConstant;
import com.onekosmos.blockidsample.util.AppUtil;

import java.util.LinkedHashMap;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2022 1Kosmos. All rights reserved.
 */
@SuppressWarnings("FieldCanBeLocal")
public class DocumentScannerHelper {
    private static final String K_LICENSE_KEY = AppConstant.documentScannerLicense;
    private static final String K_BACK_IMAGE = "back_image";
    private static final String K_FRONT_IMAGE = "front_image";
    private static final String K_FRONT_IMAGE_FLASH = "front_image_flash";

    // set default image compression quality
    private final double mImageCompressionQuality = 0.5;
    private final Context mContext;
    private DocumentScanCallback mCallback;
    private LinkedHashMap<String, Object> mDocumentMap;
    private DSSide mSide;

    /**
     * @param context activity context, on which scanner will start
     */
    public DocumentScannerHelper(Context context) {
        mContext = context;
    }

    /**
     * @param docType  which doc type need to be scanned
     * @param callback type of DocumentScanCallback to return document data, error
     *                 and cancellation of scanner
     */
    public void startDocumentScan(DocumentScannerType docType, DocumentScanCallback callback) {
        mCallback = callback;
        if (docType.equals(DocumentScannerType.DL))
            startDLScan(DSSide.Front);
    }

    /**
     * @param side scanning order (front and back || back and front)
     */
    public void startDLScan(DSSide side) {
        DSHandler.staticLicenseKey = K_LICENSE_KEY;
        mSide = side;
        if (mSide == DSSide.Front)
            scanDLFrontSide();
        else
            scanDLBackSide();
    }

    /**
     * @param side initialize DSID1Options for side
     */
    private DSID1Options getDSIDOption(DSSide side) {
        DSID1Options dsid1Options = new DSID1Options();
        dsid1Options.type = DSID1Type.License;
        // Side of the Document
        dsid1Options.side = side;
        // Enable Camera Flash
        dsid1Options.enableFlashCapture = FlashCapture.Both;
        // Show preview Of captured document
        dsid1Options.showReviewScreen = true;
        // Set Image compression quality
        dsid1Options.imageCompressionQuality = mImageCompressionQuality;
        return dsid1Options;
    }

    /**
     * initialize DSHandler for front side and start scanning
     */
    private void scanDLFrontSide() {
        DSID1Options options = getDSIDOption(DSSide.Front);
        DSHandler dsHandler = DSHandler.getInstance(mContext);
        dsHandler.options = options;
        dsHandler.init(DSCaptureMode.Manual, new DSHandlerListener() {
            @Override
            public void handleScan(DSResult dsResult) {
                if (mDocumentMap == null)
                    mDocumentMap = new LinkedHashMap<>();
                mDocumentMap.put(K_FRONT_IMAGE, AppUtil.getBase64FromBytes(dsResult.image));
                mDocumentMap.put(K_FRONT_IMAGE_FLASH,
                        AppUtil.getBase64FromBytes(dsResult.flashImage));
                if (mSide == DSSide.Front)
                    scanDLBackSide();
                else
                    mCallback.handleScan(mDocumentMap);
            }

            @Override
            public void scanWasCancelled() {
                mCallback.scanWasCancelled();
            }

            @Override
            public void captureError(DSError dsError) {
                mCallback.captureError(dsError);
            }
        });
        dsHandler.start();
    }

    /**
     * initialize DSHandler for back side and start scanning
     */
    private void scanDLBackSide() {
        DSID1Options options = getDSIDOption(DSSide.Back);
        DSHandler dsHandler = DSHandler.getInstance(mContext);
        dsHandler.options = options;
        dsHandler.init(DSCaptureMode.Manual, new DSHandlerListener() {
            @Override
            public void handleScan(DSResult dsResult) {
                if (mDocumentMap == null)
                    mDocumentMap = new LinkedHashMap<>();
                mDocumentMap.put(K_BACK_IMAGE, AppUtil.getBase64FromBytes(dsResult.image));
                if (mSide == DSSide.Back)
                    scanDLFrontSide();
                else
                    mCallback.handleScan(mDocumentMap);
            }

            @Override
            public void scanWasCancelled() {
                mCallback.scanWasCancelled();
            }

            @Override
            public void captureError(DSError dsError) {
                mCallback.captureError(dsError);
            }
        });
        dsHandler.start();
    }

    /**
     * Enum for document Type.
     * Currently enum is only for DL
     */
    public enum DocumentScannerType {
        DL
    }

    /**
     * Document Scan callback
     */
    public interface DocumentScanCallback {
        void scanWasCancelled();

        void captureError(DSError dsError);

        void handleScan(LinkedHashMap<String, Object> documentData);
    }
}
