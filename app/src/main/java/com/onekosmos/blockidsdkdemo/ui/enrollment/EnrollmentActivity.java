package com.onekosmos.blockidsdkdemo.ui.enrollment;

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
import com.blockid.sdk.document.BIDDocumentProvider;
import com.example.blockidsdkdemo.R;
import com.onekosmos.blockidsdkdemo.AppConstant;
import com.onekosmos.blockidsdkdemo.ui.RegisterTenantActivity;
import com.onekosmos.blockidsdkdemo.ui.driverLicense.DriverLicenseScanActivity;
import com.onekosmos.blockidsdkdemo.ui.liveID.LiveIDScanningActivity;
import com.onekosmos.blockidsdkdemo.ui.passport.PassportScanningActivity;
import com.onekosmos.blockidsdkdemo.ui.enrollPin.EnrollPinActivity;
import com.onekosmos.blockidsdkdemo.ui.qrAuth.AuthenticatorActivity;
import com.onekosmos.blockidsdkdemo.util.ErrorDialog;
import com.onekosmos.blockidsdkdemo.util.ProgressDialog;
import com.onekosmos.blockidsdkdemo.util.SharedPreferenceUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Pankti Mistry on 30-04-2021.
 * Copyright © 2020 1Kosmos. All rights reserved.
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

        if (!BIDAuthProvider.getInstance().isSdkLocked())
            onUnlockSdkClick();
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
        } else if (TextUtils.equals(asset.getAssetTitle(), getResources().getString(R.string.label_driver_license))) {
            onDLClicked();
        } else if (TextUtils.equals(asset.getAssetTitle(), getResources().getString(R.string.label_passport))) {
            onPPClicked();
        } else if (TextUtils.equals(asset.getAssetTitle(), getResources().getString(R.string.label_national_id))) {
            onNationalIDClick();
        } else if (TextUtils.equals(asset.getAssetTitle(), getResources().getString(R.string.label_reset_app))) {
            onResetAppClick();
        } else if (TextUtils.equals(asset.getAssetTitle(), getResources().getString(R.string.label_unlock_sdk))) {
            onUnlockSdkClick();
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
        if (BlockIDSDK.getInstance().isPinRegistered()) {
            ErrorDialog errorDialog = new ErrorDialog(this);
            errorDialog.showWithTwoButton(
                    null,
                    getString(R.string.label_remove_pin_title),
                    getString(R.string.label_remove_pin),
                    getString(R.string.label_yes), getString(R.string.label_no),
                    (dialogInterface, i) -> {
                        errorDialog.dismiss();

                    },
                    dialog -> {
                        errorDialog.dismiss();
                        unEnrollPin();
                    });
            return;
        }
        Intent intent = new Intent(this, EnrollPinActivity.class);
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
                    (dialogInterface, i) -> removeDocument(BIDDocumentProvider.BIDDocumentType.driverLicense),
                    dialog -> {
                        errorDialog.dismiss();
                    });
            return;
        }
        Intent intent = new Intent(this, DriverLicenseScanActivity.class);
        startActivity(intent);
    }

    private void onPPClicked() {
        if (BlockIDSDK.getInstance().isPassportEnrolled()) {
            ErrorDialog errorDialog = new ErrorDialog(this);
            errorDialog.showWithTwoButton(
                    null,
                    getString(R.string.label_remove_pp_title),
                    getString(R.string.label_remove_pp),
                    getString(R.string.label_yes), getString(R.string.label_no),
                    (dialogInterface, i) -> removeDocument(BIDDocumentProvider.BIDDocumentType.passport),
                    dialog -> {
                        errorDialog.dismiss();
                    });
            return;
        }
        Intent intent = new Intent(this, PassportScanningActivity.class);
        startActivity(intent);
    }

    //FIXME will implement later
    private void onNationalIDClick() {
    }

    private void removeDocument(BIDDocumentProvider.BIDDocumentType documentType) {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.show();
        BlockIDSDK.getInstance().unRegisterDocument(this, documentType, (status, error) -> {
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

    private void onResetAppClick() {
        SharedPreferenceUtil.getInstance().clear();
        BlockIDSDK.getInstance().resetSDK(AppConstant.licenseKey);
        Intent intent = new Intent(this, RegisterTenantActivity.class);
        startActivity(intent);
        finish();
    }

    private void onUnlockSdkClick() {
        if (BlockIDSDK.getInstance().isReady() && BlockIDSDK.getInstance().isDeviceAuthEnrolled()) {
            String title = getResources().getString(R.string.label_biometric_auth);
            String desc = getResources().getString(R.string.label_biometric_auth_req);
            BIDAuthProvider.getInstance().verifyDeviceAuth(this, title, desc, false, (b, errorResponse) -> {
                if (b)
                    Toast.makeText(EnrollmentActivity.this, R.string.label_sdk_unlock_successfully, Toast.LENGTH_SHORT).show();
            });
        } else {
            Toast.makeText(this, R.string.label_enroll_device_uth_to_unlock_sdk, Toast.LENGTH_SHORT).show();
        }
    }

    private void onQrLoginClicked() {
        Intent intent = new Intent(this, AuthenticatorActivity.class);
        startActivity(intent);
    }
}
