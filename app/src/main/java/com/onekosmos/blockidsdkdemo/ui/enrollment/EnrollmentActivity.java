package com.onekosmos.blockidsdkdemo.ui.enrollment;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.blockid.sdk.BlockIDSDK;
import com.blockid.sdk.authentication.BIDAuthProvider;
import com.blockid.sdk.cameramodule.DLScanner.DLScannerHelper;
import com.blockid.sdk.cameramodule.DLScanner.DLScanningOrder;
import com.blockid.sdk.cameramodule.camera.dlModule.IDriverLicenseListener;
import com.blockid.sdk.datamodel.BIDDriverLicense;
import com.blockid.sdk.document.BIDDocumentProvider;
import com.blockid.sdk.utils.BIDUtil;
import com.example.blockidsdkdemo.R;
import com.onekosmos.blockidsdkdemo.AppConstant;
import com.onekosmos.blockidsdkdemo.AppVault;
import com.onekosmos.blockidsdkdemo.ui.LiveIDScanningActivity;
import com.onekosmos.blockidsdkdemo.ui.RegisterTenantActivity;
import com.onekosmos.blockidsdkdemo.ui.passport.PassportScanningActivity;
import com.onekosmos.blockidsdkdemo.ui.utils.ErrorDialog;
import com.onekosmos.blockidsdkdemo.ui.utils.ProgressDialog;
import com.onekosmos.blockidsdkdemo.util.SharedPreferenceUtil;

import java.util.ArrayList;
import java.util.List;

import static com.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_DL_SCAN_CANCEL;

/**
 * Created by Pankti Mistry on 30-04-2021.
 * Copyright © 2020 1Kosmos. All rights reserved.
 */
public class EnrollmentActivity extends AppCompatActivity implements IDriverLicenseListener, EnrollmentAdapter.EnrollmentClickListener {
    private ErrorManager.ErrorResponse mErrorResponse;
    private RecyclerView mRvEnrollmentAssets;
    private BIDDriverLicense mDriverLicense;
    private List<EnrollmentAsset> enrollmentAssets = new ArrayList<>();
    private EnrollmentAdapter mEnrollmentAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enrollment);
        initView();

        if (!BIDAuthProvider.getInstance().isSdkLocked())
            deviceAuthLogin();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshEnrollmentRecyclerView();
    }

    @Override
    public void onDriverLicenseResponse(BIDDriverLicense bidDriverLicense, String s, ErrorManager.ErrorResponse errorResponse) {
        if (bidDriverLicense == null) {
            if (errorResponse.getCode() != ErrorManager.CustomErrors.K_DL_SCAN_CANCEL.getCode()) {
                ErrorDialog errorDialog = new ErrorDialog(EnrollmentActivity.this);
                errorDialog.show(null,
                        getString(R.string.label_error),
                        errorResponse.getMessage(), dialog -> {
                            errorDialog.dismiss();
                        });
            }
            return;
        }
        mDriverLicense = bidDriverLicense;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1111 && resultCode == RESULT_OK) {
            if (mErrorResponse != null && mErrorResponse.getCode() == K_DL_SCAN_CANCEL.getCode()) {
                finish();
                return;
            }
            registerDL(mDriverLicense);
        }
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
        } else if (TextUtils.equals(asset.getAssetTitle(), getResources().getString(R.string.label_driver_license))) {
            onDLClicked();
        } else if (TextUtils.equals(asset.getAssetTitle(), getResources().getString(R.string.label_passport))) {
            onPPClicked();
        } else if (TextUtils.equals(asset.getAssetTitle(), getResources().getString(R.string.label_national_id))) {
            onNationalIDClick();
        } else if (TextUtils.equals(asset.getAssetTitle(), getResources().getString(R.string.label_reset_app))) {
            resetAppNSdk();
        } else if (TextUtils.equals(asset.getAssetTitle(), getResources().getString(R.string.label_unlock_sdk))) {
            deviceAuthLogin();
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

    private void onLiveIdClicked() {
        Intent intent = new Intent(this, LiveIDScanningActivity.class);
        startActivity(intent);
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

    }

    private void onDLClicked() {
        DLScannerHelper.getInstance().setDLScanningOrder(DLScanningOrder.FIRST_BACK_THEN_FRONT);
        Intent intent = DLScannerHelper.getInstance().getDLScanIntent(this, 90, this::onDriverLicenseResponse);
        startActivityForResult(intent, 1111);
    }

    private void registerDL(BIDDriverLicense documentData) {
        ProgressDialog progressDialog = new ProgressDialog(EnrollmentActivity.this);
        progressDialog.show();
        if (documentData != null) {
            BlockIDSDK.getInstance().registerDocument(this, documentData, BIDDocumentProvider.BIDDocumentType.driverLicense,
                    "", (status, errorResponse) -> {
                        progressDialog.dismiss();
                        if (!status) {
                            ErrorDialog errorDialog = new ErrorDialog(EnrollmentActivity.this);
                            errorDialog.show(null,
                                    getString(R.string.label_error),
                                    errorResponse.getMessage(), dialog -> {
                                        errorDialog.dismiss();
                                    });
                            return;
                        }
                        AppVault.getInstance().setDLData(BIDUtil.objectToJSONString(documentData, true));
                        Toast.makeText(this, "Driver License register successfully", Toast.LENGTH_LONG).show();
                    });
        }
    }

    private void onPPClicked() {
        Intent intent = new Intent(this, PassportScanningActivity.class);
        startActivity(intent);
    }

    private void onNationalIDClick() {
    }

    private void resetAppNSdk() {
        SharedPreferenceUtil.getInstance().clear();
        BlockIDSDK.getInstance().resetSDK(AppConstant.licenseKey);
        Intent intent = new Intent(this, RegisterTenantActivity.class);
        startActivity(intent);
        finish();
    }

    private void deviceAuthLogin() {
        if (BlockIDSDK.getInstance().isReady() && BlockIDSDK.getInstance().isDeviceAuthEnrolled()) {
            String title = getResources().getString(R.string.label_biometric_auth);
            String desc = getResources().getString(R.string.label_biometric_auth_req);
            BIDAuthProvider.getInstance().verifyDeviceAuth(this, title, desc, false, (b, errorResponse) -> {
                if (b)
                    Toast.makeText(EnrollmentActivity.this, R.string.label_sdk_unlock_successfully, Toast.LENGTH_SHORT).show();
            });
        } else {
            Toast.makeText(this, "Enroll device auth to unlock", Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshEnrollmentRecyclerView() {
        populateEnrollmentAssetsData();
    }
}
