package com.onekosmos.blockidsample.ui.enrollment;

import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_CONNECTION_ERROR;
import static com.onekosmos.blockid.sdk.document.BIDDocumentProvider.RegisterDocCategory.identity_document;
import static com.onekosmos.blockid.sdk.document.RegisterDocType.DL;
import static com.onekosmos.blockid.sdk.document.RegisterDocType.NATIONAL_ID;
import static com.onekosmos.blockid.sdk.document.RegisterDocType.PPT;
import static com.onekosmos.blockid.sdk.document.RegisterDocType.SSN;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatTextView;
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
import com.onekosmos.blockid.sdk.datamodel.BIDGenericResponse;
import com.onekosmos.blockid.sdk.datamodel.BIDLinkedAccount;
import com.onekosmos.blockid.sdk.document.BIDDocumentProvider;
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
        if (BlockIDSDK.getInstance().isDriversLicenseEnrolled()) {
            ErrorDialog errorDialog = new ErrorDialog(this);
            errorDialog.showWithTwoButton(
                    null,
                    getString(R.string.label_remove_dl_title),
                    getString(R.string.label_remove_dl),
                    getString(R.string.label_yes), getString(R.string.label_no),
                    (dialogInterface, i) -> errorDialog.dismiss(),
                    dialog -> {
                        errorDialog.dismiss();
                        try {
                            JSONArray jsonArray = new JSONArray(BIDDocumentProvider.getInstance().getUserDocument("", DL.getValue(), identity_document.name()));
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
        Intent intent = new Intent(this, DriverLicenseScanActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
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
        if (BlockIDSDK.getInstance().isSSNEnrolled()) {
            ErrorDialog errorDialog = new ErrorDialog(this);
            errorDialog.showWithTwoButton(
                    null,
                    getString(R.string.label_remove_ssn_title),
                    getString(R.string.label_remove_ssn),
                    getString(R.string.label_yes), getString(R.string.label_no),
                    (dialogInterface, i) -> errorDialog.dismiss(),
                    dialog -> {
                        errorDialog.dismiss();
                        try {
                            JSONArray jsonArray = new JSONArray(BIDDocumentProvider.getInstance().
                                    getUserDocument(null, SSN.getValue(),
                                            identity_document.name()));
                            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                            LinkedHashMap<String, Object> removeSSNMap = gson.fromJson(jsonArray.
                                            getString(0),
                                    new TypeToken<LinkedHashMap<String, Object>>() {
                                    }.getType());
                            removeDocument(removeSSNMap);
                        } catch (JSONException e) {
                            // do nothing
                        }
                    });
            return;
        } else if (!BlockIDSDK.getInstance().isDriversLicenseEnrolled()) {
            ErrorDialog errorDialog = new ErrorDialog(this);
            errorDialog.showWithOneButton(null, null,
                    getString(R.string.label_enroll_dl),
                    getString(R.string.label_ok), dialog -> {
                        errorDialog.dismiss();
                    });
            return;
        }
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

    /**
     * Get KYC Hash
     * <p>
     * Prerequisite
     * Any Identity document(DL, PPT, NationalID) should be registered
     * Verified SSN should be registered
     */
    private void getKYC() {
        String userDocument = BIDDocumentProvider.getInstance().getUserDocument(null,
                BlockIDSDK.getInstance().isDriversLicenseEnrolled() ? DL.getValue() :
                        BlockIDSDK.getInstance().isPassportEnrolled() ? PPT.getValue() :
                                BlockIDSDK.getInstance().isNationalIDEnrolled() ?
                                        NATIONAL_ID.getValue() : null, identity_document.name());
        if (TextUtils.isEmpty(userDocument)) {
            return;
        }

        String dob;
        try {
            JSONArray documentArray = new JSONArray(userDocument);
            JSONObject documentObject = documentArray.getJSONObject(0);
            dob = documentObject.getString("dob");
        } catch (Exception e) {
            return;
        }
        if (TextUtils.isEmpty(dob)) {
            return;
        }

        if (!BlockIDSDK.getInstance().isSSNEnrolled()) {
            return;
        }

        // dob format = yyyyMMdd
        BlockIDSDK.getInstance().getKYC(dob, (status, kyc, errorResponse) -> {
            if (status) {
                Log.d("KYC", kyc);
            } else {
                Log.d("KYC Error", "(" + errorResponse.getCode() + ") " +
                        errorResponse.getMessage());
            }
        });
    }
}