package com.onekosmos.blockidsample.ui.enrollment;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.blockid.sdk.BlockIDSDK;
import com.blockid.sdk.authentication.BIDAuthProvider;
import com.blockid.sdk.datamodel.BIDDocumentData;
import com.blockid.sdk.document.BIDDocumentProvider;
import com.blockid.sdk.utils.BIDUtil;
import com.google.gson.GsonBuilder;
import com.onekosmos.blockidsample.AppConstant;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.ui.RegisterTenantActivity;
import com.onekosmos.blockidsample.ui.driverLicense.DriverLicenseScanActivity;
import com.onekosmos.blockidsample.ui.enrollPin.PinEnrollmentActivity;
import com.onekosmos.blockidsample.ui.liveID.LiveIDScanningActivity;
import com.onekosmos.blockidsample.ui.nationalID.NationalIDScanActivity;
import com.onekosmos.blockidsample.ui.passport.PassportScanningActivity;
import com.onekosmos.blockidsample.ui.qrAuth.AuthenticatorActivity;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.ProgressDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static com.blockid.sdk.document.BIDDocumentProvider.RegisterDocCategory.identity_document;
import static com.blockid.sdk.document.RegisterDocType.DL;
import static com.blockid.sdk.document.RegisterDocType.NATIONAL_ID;
import static com.blockid.sdk.document.RegisterDocType.PPT;
import static com.onekosmos.blockidsample.document.DocumentMapUtil.K_CATEGORY;
import static com.onekosmos.blockidsample.document.DocumentMapUtil.K_ID;
import static com.onekosmos.blockidsample.document.DocumentMapUtil.K_PROOFEDBY;
import static com.onekosmos.blockidsample.document.DocumentMapUtil.K_TYPE;
import static com.onekosmos.blockidsample.document.DocumentMapUtil.K_UUID;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class EnrollmentActivity extends AppCompatActivity implements EnrollmentAdapter.EnrollmentClickListener {
    private RecyclerView mRvEnrollmentAssets;
    private List<EnrollmentAsset> enrollmentAssets = new ArrayList<>();
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
        if (TextUtils.equals(asset.getAssetTitle(), getResources().getString(R.string.label_liveid))) {
            onLiveIdClicked();
        } else if (TextUtils.equals(asset.getAssetTitle(), getResources().getString(R.string.label_device_auth))) {
            onDeviceAuthClicked();
        } else if (TextUtils.equals(asset.getAssetTitle(), getResources().getString(R.string.label_app_pin))) {
            onPinClicked();
        } else if (asset.getAssetTitle().contains(getResources().getString(R.string.label_driver_license_1))) {
            onDLClicked();
        } else if (asset.getAssetTitle().contains(getResources().getString(R.string.label_passport1))) {
            onPPClicked1();
        } else if (asset.getAssetTitle().contains(getResources().getString(R.string.label_passport2))) {
            onPPClicked2();
        } else if (asset.getAssetTitle().contains(getResources().getString(R.string.label_national_id_1))) {
            onNationalIDClick();
        } else if (TextUtils.equals(asset.getAssetTitle(), getResources().getString(R.string.label_reset_app))) {
            onResetAppClick();
        } else if (TextUtils.equals(asset.getAssetTitle(), getResources().getString(R.string.label_login_with_qr))) {
            onQrLoginClicked();
        }
    }

    private void initView() {
        populateEnrollmentAssetsData();
        mEnrollmentAdapter = new EnrollmentAdapter(this, enrollmentAssets);
        mRvEnrollmentAssets = findViewById(R.id.recycler_enrollment_assets);
        RecyclerView.LayoutManager mLayoutManagerBiometric = new LinearLayoutManager(this);
        mRvEnrollmentAssets.setLayoutManager(mLayoutManagerBiometric);
        mRvEnrollmentAssets.setItemAnimator(new DefaultItemAnimator());
        mRvEnrollmentAssets.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        mRvEnrollmentAssets.setAdapter(mEnrollmentAdapter);
    }

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

    private void onLiveIdClicked() {
        if (!BlockIDSDK.getInstance().isLiveIDRegistered()) {
            Intent intent = new Intent(this, LiveIDScanningActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
        }
    }

    private void onDeviceAuthClicked() {
        if (!BlockIDSDK.getInstance().isDeviceAuthEnrolled()) {
            String title = getResources().getString(R.string.label_biometric_auth);
            String desc = getResources().getString(R.string.label_biometric_auth_enroll);
            BIDAuthProvider
                    .getInstance()
                    .enrollDeviceAuth(this, title, desc, false, (success, errorResponse) -> {
                        if (success) {
                            refreshEnrollmentRecyclerView();
                            Toast.makeText(this, R.string.label_device_auth_enrolled, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, errorResponse.getMessage(), Toast.LENGTH_SHORT).show();
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
            if (status) {
                progressDialog.dismiss();
                refreshEnrollmentRecyclerView();
                return;
            }
            ErrorDialog errorDialog = new ErrorDialog(this);
            DialogInterface.OnDismissListener onDismissListener = dialogInterface -> {
                errorDialog.dismiss();
            };
            if (error != null && error.getCode() == ErrorManager.CustomErrors.K_CONNECTION_ERROR.getCode()) {
                errorDialog.showNoInternetDialog(onDismissListener);
                return;
            }
            errorDialog.show(null, getString(R.string.label_error), error.getMessage(), onDismissListener);
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
                        removeDocument(EnrollmentsDataSource.getInstance().getDriverLicenseID(1), DL.getValue(), identity_document.name(), BIDDocumentProvider.BIDDocumentType.driverLicense);
                    });
            return;
        }
        Intent intent = new Intent(this, DriverLicenseScanActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    private void onPPClicked1() {
        if (EnrollmentsDataSource.getInstance().isPassportEnrolled() > 0) {
            ErrorDialog errorDialog = new ErrorDialog(this);
            errorDialog.showWithTwoButton(
                    null,
                    getString(R.string.label_remove_pp_title),
                    getString(R.string.label_remove_pp),
                    getString(R.string.label_yes), getString(R.string.label_no),
                    (dialogInterface, i) -> errorDialog.dismiss(),
                    dialog -> {
                        errorDialog.dismiss();
                        removeDocument(EnrollmentsDataSource.getInstance().getPassportID(1), PPT.getValue(), identity_document.name(), BIDDocumentProvider.BIDDocumentType.passport);
                    });
            return;
        }
        Intent intent = new Intent(this, PassportScanningActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    private void onPPClicked2() {
        if (EnrollmentsDataSource.getInstance().isPassportEnrolled() > 1) {
            ErrorDialog errorDialog = new ErrorDialog(this);
            errorDialog.showWithTwoButton(
                    null,
                    getString(R.string.label_remove_pp_title),
                    getString(R.string.label_remove_pp),
                    getString(R.string.label_yes), getString(R.string.label_no),
                    (dialogInterface, i) -> errorDialog.dismiss(),
                    dialog -> {
                        errorDialog.dismiss();
                        removeDocument(EnrollmentsDataSource.getInstance().getPassportID(2), PPT.getValue(), identity_document.name(), BIDDocumentProvider.BIDDocumentType.passport);
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
                        removeDocument(EnrollmentsDataSource.getInstance().getNationalID(1), NATIONAL_ID.getValue(), identity_document.name(), BIDDocumentProvider.BIDDocumentType.nationalID);
                    });
            return;
        }
        Intent intent = new Intent(this, NationalIDScanActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    private void removeDocument(String id, String type, String category, BIDDocumentProvider.BIDDocumentType documentType) {
        try {
            JSONArray docArray = BIDDocumentProvider.getInstance().getDocument(id, type, category);
            if (docArray != null && docArray.length() > 0) {
                BIDDocumentData documentData = BIDUtil.JSONStringToObject(docArray.getString(0), BIDDocumentData.class);
                LinkedHashMap<String, Object> removeDocMap = new LinkedHashMap<>();
                removeDocMap.put(K_ID, documentData.id);
                removeDocMap.put(K_TYPE, documentData.type);
                removeDocMap.put(K_CATEGORY, documentData.category);
                removeDocMap.put(K_PROOFEDBY, documentData.proofedBy);
                removeDocMap.put(K_UUID, new JSONObject(new GsonBuilder().disableHtmlEscaping().create().toJson(documentData)));

                ProgressDialog dialog = new ProgressDialog(this);
                dialog.show();
                BlockIDSDK.getInstance().unRegisterDocument(this, documentType, removeDocMap, (status, error) -> {
                    dialog.dismiss();
                    if (status) {
                        refreshEnrollmentRecyclerView();
                        return;
                    }
                    ErrorDialog errorDialog = new ErrorDialog(this);
                    DialogInterface.OnDismissListener onDismissListener = dialogInterface -> {
                        errorDialog.dismiss();
                    };
                    if (error != null && error.getCode() == ErrorManager.CustomErrors.K_CONNECTION_ERROR.getCode()) {
                        errorDialog.showNoInternetDialog(onDismissListener);
                        return;
                    }
                    errorDialog.show(null, getString(R.string.label_error), error.getMessage(), onDismissListener);
                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
}