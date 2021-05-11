package com.onekosmos.blockidsample.ui.enrollment;

import android.content.Context;

import com.blockid.sdk.BlockIDSDK;
import com.example.blockidsdkdemo.R;

import java.util.ArrayList;

/**
 * Created by Pankti Mistry on 05-05-2021.
 * Copyright © 2020 1Kosmos. All rights reserved.
 */
public class EnrollmentsDataSource {
    public static EnrollmentsDataSource sharedInstance = new EnrollmentsDataSource();

    public enum EnrollmentAssetEnum {
        ASSET_UNLOCK_SDK,
        ASSET_LIVE_ID,
        ASSET_PIN,
        ASSET_DEVICE_AUTH,
        ASSET_DL,
        ASSET_PP,
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
        arr.add(EnrollmentAssetEnum.ASSET_PP);
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
                enrollmentAsset = new EnrollmentAsset(BlockIDSDK.getInstance().isDriversLicenseEnrolled(),
                        context.getResources().getString(R.string.label_driver_license));
                break;

            case ASSET_PP:
                enrollmentAsset = new EnrollmentAsset(BlockIDSDK.getInstance().isPassportEnrolled(),
                        context.getResources().getString(R.string.label_passport));
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
                enrollmentAsset = new EnrollmentAsset(BlockIDSDK.getInstance().isNationalIDEnrolled(),
                        context.getResources().getString(R.string.label_national_id));
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
}