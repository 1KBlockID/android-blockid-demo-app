package com.onekosmos.blockidsample.ui.enrollment;

import static com.onekosmos.blockid.sdk.document.BIDDocumentProvider.RegisterDocCategory.identity_document;
import static com.onekosmos.blockid.sdk.document.RegisterDocType.DL;
import static com.onekosmos.blockid.sdk.document.RegisterDocType.NATIONAL_ID;
import static com.onekosmos.blockid.sdk.document.RegisterDocType.PPT;
import static com.onekosmos.blockid.sdk.document.RegisterDocType.SSN;

import android.content.Context;
import android.text.TextUtils;

import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.datamodel.BIDGenericResponse;
import com.onekosmos.blockid.sdk.datamodel.BIDLinkedAccount;
import com.onekosmos.blockid.sdk.document.BIDDocumentProvider;
import com.onekosmos.blockidsample.R;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class EnrollmentsDataSource {
    public static EnrollmentsDataSource sharedInstance = new EnrollmentsDataSource();

    public enum EnrollmentAssetEnum {
        ASSET_ADD_USER,
        ASSET_LIVE_ID_ACTIVE,
        ASSET_LIVE_ID_PASSIVE,
        ASSET_PIN,
        ASSET_DEVICE_AUTH,
        ASSET_DL,
        ASSET_PP1,
        ASSET_PP2,
        ASSET_NATIONAL_ID,
        ASSET_SSN,
        ASSET_LOGIN_WITH_QR,
        ASSET_RECOVER_MNEMONIC,
        ASSET_FIDO2,
        ASSET_WALLET_CONNECT,
        ASSET_ABOUT,
        ASSET_KYC,
        ASSET_RESET_SDK,
    }

    private EnrollmentsDataSource() {
    }

    public static EnrollmentsDataSource getInstance() {
        return sharedInstance;
    }

    public ArrayList<EnrollmentAssetEnum> prepareAssetsList() {
        ArrayList<EnrollmentAssetEnum> arr = new ArrayList<>();

        arr.add(EnrollmentAssetEnum.ASSET_ABOUT);

        arr.add(EnrollmentAssetEnum.ASSET_ADD_USER);

        arr.add(EnrollmentAssetEnum.ASSET_DL);
        arr.add(EnrollmentAssetEnum.ASSET_PP1);
        arr.add(EnrollmentAssetEnum.ASSET_PP2);
        arr.add(EnrollmentAssetEnum.ASSET_NATIONAL_ID);
        arr.add(EnrollmentAssetEnum.ASSET_SSN);
        arr.add(EnrollmentAssetEnum.ASSET_KYC);

        arr.add(EnrollmentAssetEnum.ASSET_PIN);
        arr.add(EnrollmentAssetEnum.ASSET_DEVICE_AUTH);
        arr.add(EnrollmentAssetEnum.ASSET_LIVE_ID_ACTIVE);
        arr.add(EnrollmentAssetEnum.ASSET_LIVE_ID_PASSIVE);

        arr.add(EnrollmentAssetEnum.ASSET_LOGIN_WITH_QR);
        arr.add(EnrollmentAssetEnum.ASSET_RECOVER_MNEMONIC);
        arr.add(EnrollmentAssetEnum.ASSET_FIDO2);
        arr.add(EnrollmentAssetEnum.ASSET_WALLET_CONNECT);
        arr.add(EnrollmentAssetEnum.ASSET_RESET_SDK);
        return arr;
    }

    public EnrollmentAsset assetDataFor(Context context, EnrollmentAssetEnum type) {
        EnrollmentAsset enrollmentAsset = null;
        switch (type) {
            case ASSET_ADD_USER:
                BIDLinkedAccount account = getLinkedAccountList();
                if (account == null)
                    enrollmentAsset = new EnrollmentAsset(false,
                            context.getResources().getString(R.string.label_add_user), null);
                else
                    enrollmentAsset = new EnrollmentAsset(false,
                            account.getUserId(), account.getOrigin().getTag() + " | " +
                            account.getOrigin().getCommunity());
                break;

            case ASSET_DL:
                String dlID1 = getDriverLicenseID(1);
                dlID1 = TextUtils.isEmpty(dlID1) ? "" : "\n(# " + dlID1 + ")";
                enrollmentAsset = new EnrollmentAsset(BIDDocumentProvider.getInstance().
                        isDocumentEnrolled(DL.getValue(), identity_document.name()),
                        context.getResources().getString(R.string.label_driver_license_1) + dlID1, null);
                break;

            case ASSET_PP1:
                String ppID1 = getPassportID(1);
                ppID1 = TextUtils.isEmpty(ppID1) ? "" : " (# " + ppID1 + ")";
                enrollmentAsset = new EnrollmentAsset(isPassportEnrolled(1),
                        context.getResources().getString(R.string.label_passport1) + ppID1, null);
                break;

            case ASSET_PP2:
                String ppID2 = getPassportID(2);
                ppID2 = TextUtils.isEmpty(ppID2) ? "" : " (# " + ppID2 + ")";
                enrollmentAsset = new EnrollmentAsset(isPassportEnrolled(2),
                        context.getResources().getString(R.string.label_passport2) + ppID2, null);
                break;

            case ASSET_LIVE_ID_ACTIVE:
                enrollmentAsset = new EnrollmentAsset(BlockIDSDK.getInstance().isLiveIDRegistered(),
                        context.getResources().getString(R.string.label_liveid_active), null);
                break;

            case ASSET_LIVE_ID_PASSIVE:
                enrollmentAsset = new EnrollmentAsset(BlockIDSDK.getInstance().isLiveIDRegistered(),
                        context.getResources().getString(R.string.label_liveid_passive), null);
                break;

            case ASSET_DEVICE_AUTH:
                enrollmentAsset = new EnrollmentAsset(BlockIDSDK.getInstance().isDeviceAuthEnrolled(),
                        context.getResources().getString(R.string.label_device_auth), null);
                break;

            case ASSET_PIN:
                enrollmentAsset = new EnrollmentAsset(BlockIDSDK.getInstance().isPinRegistered(),
                        context.getResources().getString(R.string.label_app_pin), null);
                break;

            case ASSET_NATIONAL_ID:
                String nID1 = getNationalID(1);
                nID1 = TextUtils.isEmpty(nID1) ? "" : " (# " + nID1 + ")";
                enrollmentAsset = new EnrollmentAsset(BIDDocumentProvider.getInstance().
                        isDocumentEnrolled(NATIONAL_ID.getValue(), identity_document.name()),
                        context.getResources().getString(R.string.label_national_id_1) + nID1, null);
                break;

            case ASSET_RESET_SDK:
                enrollmentAsset = new EnrollmentAsset(false,
                        context.getResources().getString(R.string.label_reset_app), null);
                break;
            case ASSET_LOGIN_WITH_QR:
                enrollmentAsset = new EnrollmentAsset(false,
                        context.getResources().getString(R.string.label_login_with_qr), null);
                break;
            case ASSET_RECOVER_MNEMONIC:
                enrollmentAsset = new EnrollmentAsset(false,
                        context.getResources().getString(R.string.label_recover_mnemonic), null);
                break;
            case ASSET_FIDO2:
                enrollmentAsset = new EnrollmentAsset(false,
                        context.getResources().getString(R.string.label_fido2), null);
                break;
            case ASSET_WALLET_CONNECT:
                enrollmentAsset = new EnrollmentAsset(false,
                        context.getResources().getString(R.string.label_wallet_connect), null);
                break;
            case ASSET_SSN:
                enrollmentAsset = new EnrollmentAsset(BIDDocumentProvider.getInstance().
                        isDocumentEnrolled(SSN.getValue(), identity_document.name()),
                        context.getResources().getString(R.string.label_enroll_ssn), null);
                break;
            case ASSET_ABOUT:
                enrollmentAsset = new EnrollmentAsset(false,
                        context.getResources().getString(R.string.label_about), null);
                break;
            case ASSET_KYC:
                enrollmentAsset = new EnrollmentAsset(false,
                        context.getResources().getString(R.string.label_my_kyc), null);
                break;
        }
        return enrollmentAsset;
    }

    private BIDLinkedAccount getLinkedAccountList() {
        BIDGenericResponse response = BlockIDSDK.getInstance().getLinkedUserList();
        if (response.getStatus()) {
            List<BIDLinkedAccount> mLinkedAccountsList = response.getDataObject();
            if (mLinkedAccountsList != null && mLinkedAccountsList.size() > 0) {
                return mLinkedAccountsList.get(0);
            }
        }
        return null;
    }

    public boolean isPassportEnrolled(int count) {
        try {
            String ppArrayList = BIDDocumentProvider.getInstance().getUserDocument("",
                    PPT.getValue(), identity_document.name());
            if (TextUtils.isEmpty(ppArrayList)) {
                return false;
            }
            JSONArray ppDoc = new JSONArray(ppArrayList);
            return ppDoc.length() >= count;
        } catch (JSONException e) {
            return false;
        }
    }

    public String getPassportID(int count) {
        String ppArrayList = BIDDocumentProvider.getInstance().getUserDocument("", PPT.getValue(), identity_document.name());
        if (TextUtils.isEmpty(ppArrayList)) {
            return null;
        }
        try {
            JSONArray ppDoc = new JSONArray(ppArrayList);
            if (ppDoc.length() >= count) {
                return ppDoc.getJSONObject(count - 1).getString("id");
            }
        } catch (JSONException e) {
            return null;
        }
        return null;
    }

    public String getDriverLicenseID(int count) {
        String dlArrayList = BIDDocumentProvider.getInstance().getUserDocument("", DL.getValue(), identity_document.name());
        if (TextUtils.isEmpty(dlArrayList)) {
            return null;
        }
        try {
            JSONArray dlDoc = new JSONArray(dlArrayList);
            if (dlDoc.length() >= count) {
                return dlDoc.getJSONObject(count - 1).getString("id");
            }
        } catch (JSONException e) {
            return null;
        }
        return null;
    }

    public String getNationalID(int count) {
        String nidArrayList = BIDDocumentProvider.getInstance().getUserDocument("", NATIONAL_ID.getValue(), identity_document.name());
        if (TextUtils.isEmpty(nidArrayList)) {
            return null;
        }
        try {
            JSONArray nidDoc = new JSONArray(nidArrayList);
            if (nidDoc.length() >= count) {
                return nidDoc.getJSONObject(count - 1).getString("id");
            }
        } catch (JSONException e) {
            return null;
        }
        return null;
    }
}