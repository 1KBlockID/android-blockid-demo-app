package com.onekosmos.blockidsample.ui.enrollment;

import android.content.Context;
import android.text.TextUtils;

import com.blockid.sdk.BlockIDSDK;
import com.blockid.sdk.document.BIDDocumentProvider;
import com.blockid.sdk.document.RegisterDocType;
import com.onekosmos.blockidsample.R;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

import static com.blockid.sdk.document.BIDDocumentProvider.RegisterDocCategory.identity_document;
import static com.onekosmos.blockidsample.doument.DocumentMapUtil.K_UUID;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class EnrollmentsDataSource {
    public static EnrollmentsDataSource sharedInstance = new EnrollmentsDataSource();

    public enum EnrollmentAssetEnum {
        ASSET_UNLOCK_SDK,
        ASSET_LIVE_ID,
        ASSET_PIN,
        ASSET_DEVICE_AUTH,
        ASSET_DL,
        ASSET_PP1,
        ASSET_PP2,
        ASSET_NATIONAL_ID,
        ASSET_RESET_SDK,
        ASSET_LOGIN_WITH_QR
    }

    private EnrollmentsDataSource() {
    }

    public static EnrollmentsDataSource getInstance() {
        return sharedInstance;
    }

    public ArrayList<EnrollmentAssetEnum> prepareAssetsList() {
        ArrayList<EnrollmentAssetEnum> arr = new ArrayList<EnrollmentAssetEnum>();

        arr.add(EnrollmentAssetEnum.ASSET_DL);
        arr.add(EnrollmentAssetEnum.ASSET_PP1);
        arr.add(EnrollmentAssetEnum.ASSET_PP2);
        arr.add(EnrollmentAssetEnum.ASSET_NATIONAL_ID);

        arr.add(EnrollmentAssetEnum.ASSET_PIN);
        arr.add(EnrollmentAssetEnum.ASSET_DEVICE_AUTH);
        arr.add(EnrollmentAssetEnum.ASSET_LIVE_ID);

        arr.add(EnrollmentAssetEnum.ASSET_LOGIN_WITH_QR);
        arr.add(EnrollmentAssetEnum.ASSET_RESET_SDK);
        return arr;
    }

    public EnrollmentAsset assetDataFor(Context context, EnrollmentAssetEnum type) {
        EnrollmentAsset enrollmentAsset = null;
        switch (type) {
            case ASSET_DL:
                String dlID1 = getDriverLicenseID(1);
                dlID1 = TextUtils.isEmpty(dlID1) ? "" : "\n(# " + dlID1 + ")";
                enrollmentAsset = new EnrollmentAsset(BlockIDSDK.getInstance().isDriversLicenseEnrolled(),
                        context.getResources().getString(R.string.label_driver_license_1) + dlID1);
                break;

            case ASSET_PP1:
                String ppID1 = getPassportID(1);
                ppID1 = TextUtils.isEmpty(ppID1) ? "" : " (# " + ppID1 + ")";
                enrollmentAsset = new EnrollmentAsset(isPassportEnrolled() > 0,
                        context.getResources().getString(R.string.label_passport1) + ppID1);
                break;

            case ASSET_PP2:
                String ppID2 = getPassportID(2);
                ppID2 = TextUtils.isEmpty(ppID2) ? "" : " (# " + ppID2 + ")";
                enrollmentAsset = new EnrollmentAsset(isPassportEnrolled() > 1,
                        context.getResources().getString(R.string.label_passport2) + ppID2);
                break;

            case ASSET_LIVE_ID:
                enrollmentAsset = new EnrollmentAsset(BlockIDSDK.getInstance().isLiveIDRegistered(),
                        context.getResources().getString(R.string.label_liveid));
                break;

            case ASSET_DEVICE_AUTH:
                enrollmentAsset = new EnrollmentAsset(BlockIDSDK.getInstance().isDeviceAuthEnrolled(),
                        context.getResources().getString(R.string.label_device_auth));
                break;

            case ASSET_PIN:
                enrollmentAsset = new EnrollmentAsset(BlockIDSDK.getInstance().isPinRegistered(),
                        context.getResources().getString(R.string.label_app_pin));
                break;

            case ASSET_NATIONAL_ID:
                String nID1 = getNationalID(1);
                nID1 = TextUtils.isEmpty(nID1) ? "" : " (# " + nID1 + ")";
                enrollmentAsset = new EnrollmentAsset(BlockIDSDK.getInstance().isNationalIDEnrolled(),
                        context.getResources().getString(R.string.label_national_id_1) + nID1);
                break;

            case ASSET_RESET_SDK:
                enrollmentAsset = new EnrollmentAsset(false,
                        context.getResources().getString(R.string.label_reset_app));
                break;
            case ASSET_LOGIN_WITH_QR:
                enrollmentAsset = new EnrollmentAsset(false,
                        context.getResources().getString(R.string.label_login_with_qr));
                break;
            case ASSET_UNLOCK_SDK:
                enrollmentAsset = new EnrollmentAsset(false,
                        context.getResources().getString(R.string.label_unlock_sdk));
                break;
        }
        return enrollmentAsset;
    }

    public int isPassportEnrolled() {
        JSONArray ppDoc = BIDDocumentProvider.getInstance().getDocument("", RegisterDocType.PPT.getValue(), identity_document.name());
        if (ppDoc != null)
            return ppDoc.length();
        return 0;
    }

    public String getPassportID(int count) {
        try {
            JSONArray ppDoc = BIDDocumentProvider.getInstance().getDocument("", RegisterDocType.PPT.getValue(), identity_document.name());
            if (ppDoc != null && ppDoc.length() >= count) {
                return ppDoc.getJSONObject(count - 1).getJSONObject(K_UUID).getString("id");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String getDriverLicenseID(int count) {
        try {
            JSONArray ppDoc = BIDDocumentProvider.getInstance().getDocument("", RegisterDocType.DL.getValue(), identity_document.name());
            if (ppDoc != null && ppDoc.length() >= count) {
                return ppDoc.getJSONObject(count - 1).getJSONObject(K_UUID).getString("id");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String getNationalID(int count) {
        try {
            JSONArray ppDoc = BIDDocumentProvider.getInstance().getDocument("", RegisterDocType.NATIONAL_ID.getValue(), identity_document.name());
            if (ppDoc != null && ppDoc.length() >= count) {
                return ppDoc.getJSONObject(count - 1).getJSONObject(K_UUID).getString("id");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }
}