package com.onekosmos.blockidsample.ui.enrollment;

import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_CONNECTION_ERROR;
import static com.onekosmos.blockid.sdk.document.BIDDocumentProvider.RegisterDocCategory.identity_document;
import static com.onekosmos.blockid.sdk.document.RegisterDocType.DL;
import static com.onekosmos.blockid.sdk.document.RegisterDocType.NATIONAL_ID;
import static com.onekosmos.blockid.sdk.document.RegisterDocType.PPT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.ErrorResponse;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.authentication.BIDAuthProvider;
import com.onekosmos.blockid.sdk.authentication.biometric.IBiometricResponseListener;
import com.onekosmos.blockid.sdk.datamodel.BIDAccount;
import com.onekosmos.blockid.sdk.datamodel.BIDGenericResponse;
import com.onekosmos.blockid.sdk.datamodel.BIDLinkedAccount;
import com.onekosmos.blockid.sdk.datamodel.BIDTenant;
import com.onekosmos.blockid.sdk.document.BIDDocumentProvider;
import com.onekosmos.blockid.sdk.fido2.FIDO2KeyType;
import com.onekosmos.blockid.sdk.utils.BIDUtil;
import com.onekosmos.blockidsample.AppConstant;
import com.onekosmos.blockidsample.BaseActivity;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.ui.RegisterTenantActivity;
import com.onekosmos.blockidsample.ui.adduser.AddUserActivity;
import com.onekosmos.blockidsample.ui.driverLicense.DriverLicenseScanActivity;
import com.onekosmos.blockidsample.ui.enrollPin.PinEnrollmentActivity;
import com.onekosmos.blockidsample.ui.fido2.FIDO2BaseActivity;
import com.onekosmos.blockidsample.ui.liveID.LiveIDScanningActivity;
import com.onekosmos.blockidsample.ui.nationalID.NationalIDScanActivity;
import com.onekosmos.blockidsample.ui.passport.PassportScanningActivity;
import com.onekosmos.blockidsample.ui.qrAuth.AuthenticatorActivity;
import com.onekosmos.blockidsample.ui.restore.RecoverMnemonicActivity;
import com.onekosmos.blockidsample.ui.verifySSN.VerifySSNActivity;
import com.onekosmos.blockidsample.ui.walletconnect.WalletConnectActivity;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.ProgressDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import kotlin.Result;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import webauthnkit.core.authenticator.internal.ui.UserConsentUI;
import webauthnkit.core.authenticator.internal.ui.UserConsentUIFactory;
import webauthnkit.core.client.WebAuthnClient;
import webauthnkit.core.data.AuthenticatorAssertionResponse;
import webauthnkit.core.data.AuthenticatorTransport;
import webauthnkit.core.data.PublicKeyCredential;
import webauthnkit.core.data.PublicKeyCredentialDescriptor;
import webauthnkit.core.data.PublicKeyCredentialRequestOptions;
import webauthnkit.core.data.PublicKeyCredentialType;
import webauthnkit.core.data.UserVerificationRequirement;
import webauthnkit.core.util.WAKLogger;


/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class EnrollmentActivity extends BaseActivity implements EnrollmentAdapter.EnrollmentClickListener {
    private final List<EnrollmentAsset> enrollmentAssets = new ArrayList<>();
    private EnrollmentAdapter mEnrollmentAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enrollment);
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshEnrollmentRecyclerView();
    }

    @Override
    public void onclick(List<EnrollmentAsset> enrollmentAssets, int position) {
        EnrollmentAsset asset = enrollmentAssets.get(position);
        if (position == 0) {
            onAddUserClicked();
        } else if (TextUtils.equals(asset.getAssetTitle(), getResources().getString(R.string.label_liveid))) {
            onLiveIdClicked(false);
        } else if (TextUtils.equals(asset.getAssetTitle(), getResources().getString(R.string.label_liveid_with_liveness_check))) {
            onLiveIdClicked(true);
        } else if (TextUtils.equals(asset.getAssetTitle(), getResources().getString(R.string.label_device_auth))) {
            onDeviceAuthClicked();
        } else if (TextUtils.equals(asset.getAssetTitle(), getResources().getString(R.string.label_app_pin))) {
            onPinClicked();
        } else if (asset.getAssetTitle().contains(getResources().getString(R.string.label_driver_license_1))) {
            onDLClicked();
        } else if (asset.getAssetTitle().contains(getResources().getString(R.string.label_passport1))) {
            onPPClicked(1);
        } else if (asset.getAssetTitle().contains(getResources().getString(R.string.label_passport2))) {
            onPPClicked(2);
        } else if (asset.getAssetTitle().contains(getResources().getString(R.string.label_national_id_1))) {
            onNationalIDClick();
        } else if (TextUtils.equals(asset.getAssetTitle(), getResources().getString(R.string.label_reset_app))) {
            onResetAppClick();
        } else if (TextUtils.equals(asset.getAssetTitle(), getResources().getString(R.string.label_login_with_qr))) {
            onQrLoginClicked();
        } else if (TextUtils.equals(asset.getAssetTitle(), getResources().
                getString(R.string.label_recover_mnemonic))) {
            onRecoverMnemonicClicked();
        } else if (TextUtils.equals(asset.getAssetTitle(), getResources().
                getString(R.string.label_fido2))) {
            onFido2Clicked();
        } else if (TextUtils.equals(asset.getAssetTitle(), getResources().
                getString(R.string.label_enroll_ssn))) {
            onVerifySSNClicked();
        } else if (TextUtils.equals(asset.getAssetTitle(), getString(R.string.label_wallet_connect))) {
            onWalletConnectClicked();
        }
    }

    private void initView() {
        populateEnrollmentAssetsData();
        mEnrollmentAdapter = new EnrollmentAdapter(this, enrollmentAssets);
        RecyclerView mRvEnrollmentAssets = findViewById(R.id.recycler_enrollment_assets);
        RecyclerView.LayoutManager mLayoutManagerBiometric = new LinearLayoutManager(this);
        mRvEnrollmentAssets.setLayoutManager(mLayoutManagerBiometric);
        mRvEnrollmentAssets.setItemAnimator(new DefaultItemAnimator());
        mRvEnrollmentAssets.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        mRvEnrollmentAssets.setAdapter(mEnrollmentAdapter);
        AppCompatTextView mTxtSdkVersion = findViewById(R.id.txt_sdk_version);
        String[] splitVersion = BlockIDSDK.getInstance().getVersion().split("\\.");
        StringBuilder version = new StringBuilder();
        for (int index = 0; index < splitVersion.length - 1; index++) {
            version.append(index == 0 ? splitVersion[index] : "." + splitVersion[index]);
        }
        String hexCode = splitVersion[splitVersion.length - 1];
        String versionText = getString(R.string.label_sdk_version) + ": " + version +
                " (" + hexCode + ")";
        mTxtSdkVersion.setText(versionText);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void populateEnrollmentAssetsData() {
        enrollmentAssets.clear();
        ArrayList<EnrollmentsDataSource.EnrollmentAssetEnum> listEnrollmentAssets = EnrollmentsDataSource.getInstance().prepareAssetsList();
        for (EnrollmentsDataSource.EnrollmentAssetEnum asset : listEnrollmentAssets) {
            EnrollmentAsset biometricAsset = EnrollmentsDataSource.getInstance().assetDataFor(this, asset);
            enrollmentAssets.add(biometricAsset);
        }
        if (mEnrollmentAdapter != null)
            mEnrollmentAdapter.notifyDataSetChanged();
    }

    private void refreshEnrollmentRecyclerView() {
        populateEnrollmentAssetsData();
    }

    private void onAddUserClicked() {
        BIDGenericResponse response = BlockIDSDK.getInstance().getLinkedUserList();

        if (!response.getStatus()) {
            startAddUserActivity();
            return;
        }
        List<BIDLinkedAccount> mLinkedAccountsList = response.getDataObject();
        if (!(mLinkedAccountsList != null && mLinkedAccountsList.size() > 0)) {
            startAddUserActivity();
            return;
        }

//        BIDLinkedAccount bidLinkedAccount = BIDUtil.JSONStringToObject("{\"account\":{\"authModuleId\":\"5f3d8d0cd866fa61019cf969.ad\",\"communityId\":\"5f3d8d0cd866fa61019cf969\",\"tenantId\":\"5f3d8d0cd866fa61019cf968\",\"uid\":\"CN=GauravRane,CN=Users,DC=blockid,DC=1kosmos\",\"urn\":\"urn:dns:1k-dev.1kosmos.net:community:5f3d8d0cd866fa61019cf969:mod:5f3d8d0cd866fa61019cf969.ad:uid:CN=GauravRane,CN=Users,DC=blockid,DC=1kosmos\",\"username\":\"grane\"},\"origin\":{\"api\":\"https://1k-dev.1kosmos.net\",\"authPage\":\"blockid://authenticate?method=scep\",\"community\":\"default\",\"publicKey\":\"dkW7xYymDr4Rh4wEGazMdtiDzaGQtzgfby7F/z1eJchUjebTmYxkKcW7cHAg12zFWYEeJF9erjwoKw0BOHqtYw==\",\"tag\":\"1kosmos\"},\"scep_expiry\":\"2025-02-20T10:10:35.000Z\",\"sceptoken\":\"MIIEowIBAAKCAQEAoVB+roYSty78hSSVfWYnsMq/Ur7vQoTdz/vbUyTk9ZaYejSIvPhQbyAg8x/h6MC+uMHgjyBgEWiFjRtoCjCEFvXK/G2LCrQ6DkLg+AGemJdosnHNo7wp8m4VzOBrgzJNCLOkovPOIAdLC/e8E+AxbAVcCYMhyhHYX4uVtaZt5rnxH2bPgssemhFA/XLLm/qNX7QmTkJ3A+lCvXVanqxr4VbtlYWp9NrJvWSF0ccmn6vj6Qi/eWFUN30G37PVu1bTfJWr7mEGg0sRPaCzDo8vh2454xlMNH90W23Xdy/9dXtxzTuzaWEIbfcv+Lc2sWqqBl1FREWnXpmQPz/NIYPCTQIDAQABAoIBAGx9YnWlnfitFQ/GZvOLPK5d7QaNewRVr4gtbnggnAu/WJT3t+6/Yfkato5MpvaOirZfTdN0hqeukAMyp2oS7wMyE25pjdWJGHJ28C5biHo/eh5pA1BXQC7XcrnzRNtbfQuZJeSh68MGpKZL2qXTZemsQRX0p0jrb4XyrqEYaVl+Kv4NGLSPB7XS4rh/8V5wp5RhTl9KxzhP+qkU1TNG66E4d1ZCEYHC1eKamRrD59r/9Ot0NT9zLVTeKQ+Md9aK61lJZDIYBQsgWVebWjV0EkjwyWSrdeeJYtKGwjrg58+u5bQn98yPAJcr8n8eOqN9AvxC+yBGecb9EO1yrc/FO9ECgYEA0a94eo/9k+56T92tq0vQVSinJg1ZfvRr+HAOdwsnWMJEnMavxBLVrGgg2s2PAZTYT1fux9m0JxMw0M/O6/Qx71yPMbLlbe0O2TtS6t8Xcp4EQ67LyDhleesFrpT6QJychgaoH28T4wFodX0jCUnZT2ZIy/vOB4aBfJcm74P5jaMCgYEAxPHqtvr7eJEuq7td0r+3Upnsb6Z/35QhBC4iPyKhxfcQgBIKDniiimzL0BCG1220nJvUDmq6FtCj20hLR5La0InwPDpADk+3WU0zon9Z9IxHojXC8P95bWT9kGv790Afu11Mw+1AaHXwvLH8P8t4UXDoCIoi8/AHxJrPdHlBj08CgYEAkwrgC+LJymFj+HnV/deugul9PZwC9Jpm1NOP8T8rGn0xLFfQjkk++iYTVBzuegdtIUbitdcfFH/KrcPssV6PXfGkoQ95AHtK/F8zqG1FviS9jNEZKpER6Es9ss3aKFErGnm0kEaOxZQJMsrMNQlKkPmDdzhfpLtYNoywyynbaM0CgYBYGnkL3n980kX0oV85lnZmR2GUGQH/fP7AJftADzgbnYkOIgPJsYHVNxJ+Q8ZuvS8dGEDnKiuRZUjIIjE7FaE5xVtpNg3N2S+GjZjZyurtEYxCLpbExSUHITSl1Qjk9RS89uIOjCZSFODbKSxVRarPlBjZKSK1yd1PwImp60y+1QKBgAH9OK4XctmUVL5x1MYRtk/N9yrg0W0ablAb2Fvo/u7/weE97rbVSHeBumUfzx7pyMzEVtf54dB5qeFW7cV10sENl3obaQr43ZxpIECweJea76J3Rofa7aGl7ghwR2GfBIaxApIi6nBjOCr5IxBj2ciLcOnkv36iRq2Cc7suwwQt\",\"smartcardhash\":\"QmTAnKCLP82ttEYw14UaJ67vsdcn31WCVqWMTj743NPHD3\",\"userId\":\"grane\"}",
//                BIDLinkedAccount.class);
//        BlockIDSDK.getInstance().registerFIDO2Key(this, bidLinkedAccount,
//                FIDO2KeyType.PLATFORM, (status, errorResponse) -> {
//                    Toast.makeText(this, getString(R.string.label_user_registration_successful),
//                            Toast.LENGTH_SHORT).show();
//                });

//        String metadata = "{\"challenge\":\"ZXlKMGVYQWlPaUpLVjFRaUxDSmhiR2NpT2lKSVV6STFOaUo5LmV5SnlZVzVrSWpvaWJUaFNYMEUzWWpsMGJqSlVYMmsyVjNkcE9VZGhXVkpIZGs1bFptcGZaVUpoUTBoSVpIQmFlQ0lzSW1GMVpDSTZJakZyTFdSbGRpNHhhMjl6Ylc5ekxtNWxkQ0lzSW5OMVlpSTZJbkJ0YVhOMGNua2lMQ0pwWkNJNklsOVFXRkF0YkhwVlEwVkZNeTFCVjNONmVtSlpNR1ZMUjNRM1prVTRTSE4zTjA5YWFFazBaRE56YjNjaUxDSmxlSEFpT2pFMk5qSTJNalV5TmpOOS5CYlMyazBpcXg2ZUJ4STdxMUNXNWd0Mk1iM3VhdXBOLUVIVXVtdjJRcG5F\",\"rpId\":\"1k-dev.1kosmos.net\",\"timeout\":60000,\"userVerification\":\"preferred\",\"allowCredentials\":[],\"status\":\"ok\",\"errorMessage\":\"\"}";
//        String data = signIn(this, mLinkedAccountsList.get(0), metadata);
//        Log.e("Data", "-->" + data);
        ErrorDialog errorDialog = new ErrorDialog(this);
        errorDialog.showWithTwoButton(null, null, getString(R.string.label_remove_user),
                getString(R.string.label_yes), getString(R.string.label_no),
                (dialogInterface, which) -> errorDialog.dismiss(),
                dialog -> {
                    errorDialog.dismiss();
                    unlinkAccount(mLinkedAccountsList.get(0));
                });
    }

    private void unlinkAccount(BIDLinkedAccount linkedAccount) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();
        BlockIDSDK.getInstance().unlinkAccount(linkedAccount, null, (status, error) -> {
            progressDialog.dismiss();
            if (status) {
                Toast.makeText(this, getString(R.string.label_account_removed),
                        Toast.LENGTH_SHORT).show();
                refreshEnrollmentRecyclerView();
                return;
            }
            ErrorDialog errorDialog = new ErrorDialog(this);
            DialogInterface.OnDismissListener onDismissListener = dialogInterface ->
                    errorDialog.dismiss();
            if (error != null && error.getCode() == K_CONNECTION_ERROR.getCode()) {
                errorDialog.showNoInternetDialog(onDismissListener);
                return;
            }
            errorDialog.show(null, getString(R.string.label_error),
                    Objects.requireNonNull(error).getMessage(), onDismissListener);
        });
    }

    private void startAddUserActivity() {
        Intent intent = new Intent(this, AddUserActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    private void onLiveIdClicked(boolean withLivenessCheck) {
        if (!BlockIDSDK.getInstance().isLiveIDRegistered()) {
            Intent intent = new Intent(this, LiveIDScanningActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.putExtra("liveness_check", withLivenessCheck);
            startActivity(intent);
        }
    }

    private void onDeviceAuthClicked() {
        if (!BlockIDSDK.getInstance().isDeviceAuthEnrolled()) {
            String title = getResources().getString(R.string.label_biometric_auth);
            String desc = getResources().getString(R.string.label_biometric_auth_enroll);
            BIDAuthProvider
                    .getInstance()
                    .enrollDeviceAuth(this, title, desc, false, new IBiometricResponseListener() {
                        @Override
                        public void onBiometricAuthResult(boolean success, ErrorResponse errorResponse) {
                            if (success) {
                                refreshEnrollmentRecyclerView();
                                Toast.makeText(EnrollmentActivity.this, R.string.label_device_auth_enrolled, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(EnrollmentActivity.this, errorResponse.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onNonBiometricAuth(boolean b) {
                            // do nothing
                        }
                    });
        }
    }

    private void onPinClicked() {
        if (BlockIDSDK.getInstance().isPinRegistered()) {
            ErrorDialog errorDialog = new ErrorDialog(this);
            errorDialog.showWithTwoButton(
                    null,
                    getString(R.string.label_remove_pin_title),
                    getString(R.string.label_remove_pin),
                    getString(R.string.label_yes), getString(R.string.label_no),
                    (dialogInterface, i) ->
                            errorDialog.dismiss(),
                    dialog -> {
                        errorDialog.dismiss();
                        unEnrollPin();
                    });
            return;
        }
        Intent intent = new Intent(this, PinEnrollmentActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    private void unEnrollPin() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();
        BlockIDSDK.getInstance().unenrollPin((status, error) -> {
            progressDialog.dismiss();
            if (status) {
                refreshEnrollmentRecyclerView();
                return;
            }
            ErrorDialog errorDialog = new ErrorDialog(this);
            DialogInterface.OnDismissListener onDismissListener = dialogInterface -> errorDialog.dismiss();
            if (error != null && error.getCode() == K_CONNECTION_ERROR.getCode()) {
                errorDialog.showNoInternetDialog(onDismissListener);
                return;
            }
            errorDialog.show(null, getString(R.string.label_error),
                    Objects.requireNonNull(error).getMessage(), onDismissListener);
        });
    }

    private void onDLClicked() {
        BIDGenericResponse response = BlockIDSDK.getInstance().getLinkedUserList();

        if (!response.getStatus()) {
            return;
        }

        List<BIDLinkedAccount> mLinkedAccountsList = response.getDataObject();
        if (!(mLinkedAccountsList != null && mLinkedAccountsList.size() > 0)) {
            return;
        }

//        BlockIDSDK.getInstance().callAssertionOption(this, mLinkedAccountsList.get(0));

        String metadata = "{\"challenge\":\"ZXlKMGVYQWlPaUpLVjFRaUxDSmhiR2NpT2lKSVV6STFOaUo5LmV5SnlZVzVrSWpvaU5qSmxhM3BRTFV0U1ZIVlNObXRsWTFFdGRrMUlMVGhHTjJKaGVtTmlTV1pIVjNjd1lWbFZOU0lzSW1GMVpDSTZJakZyTFdSbGRpNHhhMjl6Ylc5ekxtNWxkQ0lzSW5OMVlpSTZJa05PUFZCaGJtdDBhU0JOYVhOMGNua3NRMDQ5VlhObGNuTXNSRU05WW14dlkydHBaQ3hFUXoweGEyOXpiVzl6SWl3aWFXUWlPaUkxWm5kaGFFRnFYelJKVmtwNVYwdGFiRlpFVjJWdWRrSlpTMWswYUhsdWVtcGhOR3d4UmpVdE5FSnpJaXdpWlhod0lqb3hOall6TURreE5qVTRmUS5RNGV5RUNPVXRqVGlJMzRtN09ucEVFeWpKS1JqZFlOTFZ5LVdpSmROSE9F\",\"rpId\":\"1k-dev.1kosmos.net\",\"timeout\":60000,\"userVerification\":\"preferred\",\"allowCredentials\":[{\"type\":\"public-key\",\"id\":\"5nyo91EKTdmElLgfWZFP9Q\"},{\"type\":\"public-key\",\"id\":\"4Gn82snMSpyOCVAQGXrydw\"},{\"type\":\"public-key\",\"id\":\"y-z0u4lHSgiBRDm_vcjHSw\"},{\"type\":\"public-key\",\"id\":\"ve-twCp7T8eQNZV0H_ZHtA\"},{\"type\":\"public-key\",\"id\":\"3hDzaraWRdet4yQEYG2xWQ\"},{\"type\":\"public-key\",\"id\":\"iSLGnfGDTR6X3ICTLWudrQ\"},{\"type\":\"public-key\",\"id\":\"HMEOcCEQR5mIDJ6czVTSGA\"},{\"type\":\"public-key\",\"id\":\"omidPJYdRDeCRz4WhXLcXQ\"},{\"type\":\"public-key\",\"id\":\"BEtBgQ05QNunoN8aKNM_mQ\"},{\"type\":\"public-key\",\"id\":\"a05Oe6niSvS6QR6zxT8hHQ\"},{\"type\":\"public-key\",\"id\":\"oiEJczEHQW2QYXS-IAZGwg\"},{\"type\":\"public-key\",\"id\":\"gdzUO2pNTheR3tMMgGt05g\"},{\"type\":\"public-key\",\"id\":\"TuNKGRvfSOeJevTx5OSEaA\"},{\"type\":\"public-key\",\"id\":\"jLkxbTqqQKeWgwf4fbH9RA\"},{\"type\":\"public-key\",\"id\":\"NjrkRITfQ3u0637Rcq03-w\"}],\"status\":\"ok\",\"errorMessage\":\"\"}";
        String data = signIn(this, mLinkedAccountsList.get(0), metadata);
        Log.e("Data", "-->" + data);

//        if (BlockIDSDK.getInstance().isDriversLicenseEnrolled()) {
//            ErrorDialog errorDialog = new ErrorDialog(this);
//            errorDialog.showWithTwoButton(
//                    null,
//                    getString(R.string.label_remove_dl_title),
//                    getString(R.string.label_remove_dl),
//                    getString(R.string.label_yes), getString(R.string.label_no),
//                    (dialogInterface, i) -> errorDialog.dismiss(),
//                    dialog -> {
//                        errorDialog.dismiss();
//                        try {
//                            JSONArray jsonArray = new JSONArray(BIDDocumentProvider.getInstance().getUserDocument("", DL.getValue(), identity_document.name()));
//                            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
//                            LinkedHashMap<String, Object> removeDLMap = gson.fromJson(jsonArray.getString(0), new TypeToken<LinkedHashMap<String, Object>>() {
//                            }.getType());
//                            removeDocument(removeDLMap);
//                        } catch (JSONException e) {
//                            // do nothing
//                        }
//                    });
//            return;
//        }
//        Intent intent = new Intent(this, DriverLicenseScanActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
//        startActivity(intent);
    }

    private void onPPClicked(int count) {
        if (EnrollmentsDataSource.getInstance().isPassportEnrolled(count)) {
            ErrorDialog errorDialog = new ErrorDialog(this);
            errorDialog.showWithTwoButton(
                    null,
                    getString(R.string.label_remove_pp_title),
                    getString(R.string.label_remove_pp),
                    getString(R.string.label_yes), getString(R.string.label_no),
                    (dialogInterface, i) -> errorDialog.dismiss(),
                    dialog -> {
                        errorDialog.dismiss();
                        try {
                            JSONArray jsonArray = new JSONArray(BIDDocumentProvider.getInstance().getUserDocument("", PPT.getValue(), identity_document.name()));
                            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                            LinkedHashMap<String, Object> removeDLMap = gson.fromJson(jsonArray.getString(count - 1), new TypeToken<LinkedHashMap<String, Object>>() {
                            }.getType());
                            removeDocument(removeDLMap);
                        } catch (JSONException e) {
                            // do nothing
                        }
                    });
            return;
        }
        Intent intent = new Intent(this, PassportScanningActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    private void onNationalIDClick() {
        if (BlockIDSDK.getInstance().isNationalIDEnrolled()) {
            ErrorDialog errorDialog = new ErrorDialog(this);
            errorDialog.showWithTwoButton(
                    null,
                    getString(R.string.label_remove_nid_title),
                    getString(R.string.label_remove_nid),
                    getString(R.string.label_yes), getString(R.string.label_no),
                    (dialogInterface, i) -> errorDialog.dismiss(),
                    dialog -> {
                        errorDialog.dismiss();
                        try {
                            JSONArray jsonArray = new JSONArray(BIDDocumentProvider.getInstance().getUserDocument("", NATIONAL_ID.getValue(), identity_document.name()));
                            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                            LinkedHashMap<String, Object> removeDLMap = gson.fromJson(jsonArray.getString(0), new TypeToken<LinkedHashMap<String, Object>>() {
                            }.getType());
                            removeDocument(removeDLMap);
                        } catch (JSONException e) {
                            // do nothing
                        }
                    });
            return;
        }
        Intent intent = new Intent(this, NationalIDScanActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    private void onVerifySSNClicked() {
        Intent intent = new Intent(this, VerifySSNActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    private void removeDocument(LinkedHashMap<String, Object> removeDocMap) {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.show();
        BlockIDSDK.getInstance().unRegisterDocument(this, removeDocMap, (status, error) -> {
            dialog.dismiss();
            if (status) {
                refreshEnrollmentRecyclerView();
                return;
            }
            ErrorDialog errorDialog = new ErrorDialog(this);
            DialogInterface.OnDismissListener onDismissListener = dialogInterface ->
                    errorDialog.dismiss();
            if (error != null && error.getCode() == K_CONNECTION_ERROR.getCode()) {
                errorDialog.showNoInternetDialog(onDismissListener);
                return;
            }
            errorDialog.show(null, getString(R.string.label_error),
                    Objects.requireNonNull(error).getMessage(), onDismissListener);
        });
    }

    private void onResetAppClick() {
        ErrorDialog errorDialog = new ErrorDialog(this);
        errorDialog.showWithTwoButton(
                null,
                getString(R.string.label_warning),
                getString(R.string.label_do_you_want_reset_app),
                getString(R.string.label_cancel), getString(R.string.label_ok),
                (dialogInterface, i) -> {
                    errorDialog.dismiss();
                    BlockIDSDK.getInstance().resetSDK(AppConstant.licenseKey);
                    Intent intent = new Intent(this, RegisterTenantActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(intent);
                    finish();
                },
                dialog -> errorDialog.dismiss());
    }

    private void onQrLoginClicked() {
        Intent intent = new Intent(this, AuthenticatorActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    private void onRecoverMnemonicClicked() {
        Intent intent = new Intent(this, RecoverMnemonicActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    private void onFido2Clicked() {
        Intent intent = new Intent(this, FIDO2BaseActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    private void onWalletConnectClicked() {
        Intent intent = new Intent(this, WalletConnectActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    @SuppressLint("UnsafeOptInUsageWarning")
    public String signIn(@NonNull Context context, @NonNull BIDLinkedAccount linkedAccount,
                         @NonNull String metadata) {
        BIDTenant tenant = new BIDTenant(linkedAccount.getOrigin().tag,
                linkedAccount.getOrigin().community,
                linkedAccount.getOrigin().api);
        BIDAccount account = linkedAccount.getAccount();
        tenant.setTenantId(account != null ? account.getTenantId() : null);
        tenant.setCommunityId(account != null ? account.getCommunityId() : null);

        WebAuthnClient client = getWebAuthnClient(context, tenant.getDns());
        try {
            WebauthnChallenge webauthnChallenge = BIDUtil.JSONStringToObject(metadata,
                    WebauthnChallenge.class);

            PublicKeyCredentialRequestOptions builder =
                    new PublicKeyCredentialRequestOptions();

            if (webauthnChallenge == null)
                return null;

            builder.setChallenge(Base64.decode(webauthnChallenge.challenge,
                    Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE));
            builder.setRpId(webauthnChallenge.rpId);
            builder.setTimeout(webauthnChallenge.timeout);
            builder.setUserVerification(UserVerificationRequirement.Discouraged); // TBD
            ArrayList<AllowCredential> allowCredentials = webauthnChallenge.allowCredentials;
            ArrayList<PublicKeyCredentialDescriptor> credentialDescriptors = new ArrayList<>(
                    allowCredentials.size());

            ArrayList<AuthenticatorTransport> descriptors = new ArrayList();
            descriptors.add(AuthenticatorTransport.Internal);
            for (int index = 0; index < allowCredentials.size(); index++) {
                credentialDescriptors.add(new PublicKeyCredentialDescriptor(
                        PublicKeyCredentialType.PublicKey,
                        Base64.decode(allowCredentials.get(index).id,
                                Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE),
                        descriptors));
            }
            builder.setAllowCredential(credentialDescriptors);
            SignInResponse sign = new SignInResponse();
            client.get(builder, new Continuation<>() {
                @NonNull
                @Override
                public CoroutineContext getContext() {
                    return EmptyCoroutineContext.INSTANCE;
                }

                @Override
                public void resumeWith(@NonNull Object object) {
                    if (object instanceof Result.Failure) {
                        Log.e("Error", "-->" + ((Result.Failure) object).exception.toString());
                        return;
                    }

                    if (object instanceof PublicKeyCredential) {
                        @SuppressWarnings("rawtypes")
                        PublicKeyCredential credential = (PublicKeyCredential) object;
                        AuthenticatorAssertionResponse authenticatorResponse =
                                (AuthenticatorAssertionResponse) credential.getResponse();
                        Response response = new Response();
                        response.authenticatorData = Base64.encodeToString(
                                authenticatorResponse.getAuthenticatorData(),
                                Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE);

                        response.signature = Base64.encodeToString(
                                authenticatorResponse.getSignature(),
                                Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE);
                        response.userHandle = Base64.encodeToString(
                                authenticatorResponse.getUserHandle(),
                                Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE);
                        response.clientDataJSON = Base64.encodeToString(
                                authenticatorResponse.getClientDataJSON().getBytes(),
                                Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE);

                        sign.response = response;
                        sign.rawId = Base64.encodeToString(credential.getRawId(),
                                Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE);

                        sign.id = Base64.encodeToString(credential.getRawId(),
                                Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE);
                        sign.type = PublicKeyCredentialType.PublicKey.toString();
                        sign.tenantId = tenant.getTenantId();
                        sign.communityId = tenant.getCommunityId();
                        sign.dns = tenant.getDns();
                        Log.e("SignIn Data", BIDUtil.objectToJSONString(sign, true));
                    }
                }
            });
            return BIDUtil.objectToJSONString(sign, true); // FIXME need to check
        } catch (Exception e) {
            return null;
        }
    }

    @Keep
    private class WebauthnChallenge {
        String challenge;
        String rpId;
        long timeout;
        String userVerification;
        ArrayList<AllowCredential> allowCredentials;
        String status;
        String errorMessage;
    }

    @Keep
    private static class AllowCredential {
        String type;
        String id;
    }

    @Keep
    private class SignInResponse {
        String rawId;
        Response response;
        String id;
        String type;
        String tenantId;
        String communityId;
        String dns;
        @SuppressWarnings("unused")
        private JSONObject getClientExtensionResults;
    }

    @Keep
    private class Response {
        String authenticatorData;
        String signature;
        String userHandle;
        String clientDataJSON;
    }

    private WebAuthnClient getWebAuthnClient(Context context, String originDns) {
        @SuppressLint("UnsafeOptInUsageWarning") UserConsentUI consentUI =
                UserConsentUIFactory.INSTANCE.create((FragmentActivity) context);
        @SuppressLint("UnsafeOptInUsageWarning") WebAuthnClient client =
                WebAuthnClient.Companion.create((FragmentActivity) context,
                        originDns, consentUI);
        return client;
    }
}