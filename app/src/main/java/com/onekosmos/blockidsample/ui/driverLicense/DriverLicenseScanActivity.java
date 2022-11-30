package com.onekosmos.blockidsample.ui.driverLicense;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.cameramodule.BIDScannerView;
import com.onekosmos.blockid.sdk.cameramodule.camera.dlModule.IDriverLicenseResponseListener;
import com.onekosmos.blockid.sdk.cameramodule.dlScanner.DLScannerHelper;
import com.onekosmos.blockidsample.AppConstant;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.document.DocumentHolder;
import com.onekosmos.blockidsample.ui.liveID.LiveIDScanningActivity;
import com.onekosmos.blockidsample.ui.liveID.SelfieScannerActivity;
import com.onekosmos.blockidsample.util.AppPermissionUtils;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.ProgressDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;

import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_SOMETHING_WENT_WRONG;
import static com.onekosmos.blockid.sdk.cameramodule.dlScanner.DLScanningOrder.FIRST_BACK_THEN_FRONT;
import static com.onekosmos.blockid.sdk.document.BIDDocumentProvider.RegisterDocCategory.identity_document;
import static com.onekosmos.blockid.sdk.document.RegisterDocType.DL;


/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class DriverLicenseScanActivity extends AppCompatActivity implements View.OnClickListener,
        IDriverLicenseResponseListener {
    private static final int K_DL_PERMISSION_REQUEST_CODE = 1011;
    private static int K_DL_EXPIRY_GRACE_DAYS = 90;
    private final String[] K_CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private AppCompatImageView mImgBack, mScannerOverlay, mImgSuccess;
    private AppCompatTextView mTxtBack, mTxtMessage, mTxtScanSide;
    private BIDScannerView mBIDScannerView;
    private LinearLayout mLayoutMessage;
    private DLScannerHelper mDriverLicenseScannerHelper;
    private LinkedHashMap<String, Object> mDriverLicenseMap;
    private String mSigToken, mScanSide;
    private boolean isRegistrationInProgress;
    private String K_NO_FACE_FOUND = "BlockIDFaceDetectionNotification";
    private String K_FACE_COUNT = "numberOfFaces";

    private BroadcastReceiver mDLScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int count = intent.getIntExtra(K_FACE_COUNT, 0);
            if (count > 1)
                mTxtScanSide.setText(R.string.label_many_faces);
            else
                mTxtScanSide.setText(mScanSide);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mDLScanReceiver,
                new IntentFilter(K_NO_FACE_FOUND));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_license_scan);
        initView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!AppPermissionUtils.isPermissionGiven(K_CAMERA_PERMISSION, this))
            AppPermissionUtils.requestPermission(this, K_DL_PERMISSION_REQUEST_CODE,
                    K_CAMERA_PERMISSION);
        else
            startScan();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (AppPermissionUtils.isGrantedPermission(this, requestCode, grantResults,
                K_CAMERA_PERMISSION)) {
            startScan();
        } else {
            ErrorDialog errorDialog = new ErrorDialog(this);
            errorDialog.show(null,
                    "",
                    getString(R.string.label_camera_permission_alert), dialog -> {
                        errorDialog.dismiss();
                        finish();
                    });
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.img_back:
            case R.id.txt_back:
                onCancelEnrollment();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (!isRegistrationInProgress)
            onCancelEnrollment();
    }

    @Override
    public void onStop() {
        super.onStop();
        mDriverLicenseScannerHelper.stopScanning();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mDLScanReceiver);
    }

    @Override
    public void onDriverLicenseResponse(LinkedHashMap<String, Object> driverLicenseMap,
                                        String signatureToken, ErrorManager.ErrorResponse error) {
        stopScan();
        if (driverLicenseMap != null) {
            mDriverLicenseMap = driverLicenseMap;
            mSigToken = signatureToken;
            verifyDriverLicenseDialog();
            return;
        }

        if (error == null)
            error = new ErrorManager.ErrorResponse(K_SOMETHING_WENT_WRONG.getCode(),
                    K_SOMETHING_WENT_WRONG.getMessage());

        ErrorDialog errorDialog = new ErrorDialog(this);
        DialogInterface.OnDismissListener onDismissListener = dialogInterface -> {
            errorDialog.dismiss();
            finish();
        };
        if (error.getCode() == ErrorManager.CustomErrors.K_CONNECTION_ERROR.getCode()) {
            errorDialog.showNoInternetDialog(onDismissListener);
            return;
        }
        errorDialog.show(null, getString(R.string.label_error), error.getMessage(), onDismissListener);
    }

    @Override
    public void scanFrontSide() {
        mScanSide = getString(R.string.label_scan_front);
        mTxtScanSide.setText(mScanSide);
    }

    @Override
    public void scanBackSide() {
        mScanSide = getString(R.string.label_scan_back);
        mTxtScanSide.setText(mScanSide);
    }

    private void initView() {
        mBIDScannerView = findViewById(R.id.bid_scanner_view);
        mScannerOverlay = findViewById(R.id.view_overlay);

        if (AppPermissionUtils.isPermissionGiven(K_CAMERA_PERMISSION, this))
            mBIDScannerView.setVisibility(View.VISIBLE);

        mImgBack = findViewById(R.id.img_back);
        mImgBack.setOnClickListener(this);

        mTxtBack = findViewById(R.id.txt_back);
        mTxtBack.setOnClickListener(this);

        mImgSuccess = findViewById(R.id.img_success);
        mTxtMessage = findViewById(R.id.txt_message);
        mLayoutMessage = findViewById(R.id.layout_message);
        mTxtScanSide = findViewById(R.id.txt_info);
    }

    private void registerDriverLicense() {
        mDriverLicenseScannerHelper.stopScanning();
        mBIDScannerView.setVisibility(View.GONE);
        mLayoutMessage.setVisibility(View.GONE);
        mTxtScanSide.setVisibility(View.GONE);
        mImgBack.setClickable(false);
        mTxtBack.setClickable(false);
        ProgressDialog progressDialog = new ProgressDialog(this,
                getString(R.string.label_registering_driver_license));
        progressDialog.show();
        isRegistrationInProgress = true;
        if (mDriverLicenseMap != null) {
            mDriverLicenseMap.put("category", identity_document.name());
            mDriverLicenseMap.put("type", DL.getValue());
            mDriverLicenseMap.put("id", mDriverLicenseMap.get("id"));
            BlockIDSDK.getInstance().registerDocument(this, mDriverLicenseMap,
                    null, (status, error) -> {
                        progressDialog.dismiss();
                        isRegistrationInProgress = false;
                        if (status) {
                            Toast.makeText(this, R.string.label_dl_enrolled_successfully,
                                    Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }

                        if (error.getCode() == ErrorManager.CustomErrors.K_LIVEID_IS_MANDATORY.getCode()) {
                            DocumentHolder.setData(mDriverLicenseMap, null);
                            Intent intent = new Intent(this, SelfieScannerActivity.class);
                            intent.putExtra(LiveIDScanningActivity.LIVEID_WITH_DOCUMENT, true);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivity(intent);
                            finish();
                            return;
                        }

                        ErrorDialog errorDialog = new ErrorDialog(this);
                        DialogInterface.OnDismissListener onDismissListener = dialogInterface -> {
                            errorDialog.dismiss();
                            finish();
                        };
                        if (error.getCode() == ErrorManager.CustomErrors.K_CONNECTION_ERROR.getCode()) {
                            errorDialog.showNoInternetDialog(onDismissListener);
                            return;
                        }
                        errorDialog.show(null, getString(R.string.label_error), error.getMessage(), onDismissListener);
                    });
        }
    }

    private void startScan() {
        if (!isRegistrationInProgress) {
            mBIDScannerView.setVisibility(View.VISIBLE);
            mScannerOverlay.setVisibility(View.VISIBLE);
            mDriverLicenseScannerHelper = new DLScannerHelper(this, FIRST_BACK_THEN_FRONT,
                    mBIDScannerView, K_DL_EXPIRY_GRACE_DAYS, this);
            mDriverLicenseScannerHelper.startScanning();
            mLayoutMessage.setVisibility(View.VISIBLE);
            mTxtMessage.setVisibility(View.VISIBLE);
            mTxtMessage.setText(R.string.label_scanning);
        }
    }

    private void stopScan() {
        mLayoutMessage.setVisibility(View.VISIBLE);
        mTxtMessage.setVisibility(View.VISIBLE);
        mTxtMessage.setText(R.string.label_scan_complete);
        mImgSuccess.setVisibility(View.VISIBLE);
        mDriverLicenseScannerHelper.stopScanning();
    }

    private void onCancelEnrollment() {
        ErrorDialog errorDialog = new ErrorDialog(this);
        errorDialog.showWithTwoButton(null,
                getString(R.string.cancellation_warning),
                getString(R.string.label_do_you_want_to_cancel_the_registration_process),
                getString(R.string.label_yes),
                getString(R.string.label_no),
                (dialogInterface, i) -> {
                    errorDialog.dismiss();
                },
                dialog -> {
                    mDriverLicenseScannerHelper.stopScanning();
                    errorDialog.dismiss();
                    finish();
                });
    }

    private void verifyDriverLicenseDialog() {
        ErrorDialog errorDialog = new ErrorDialog(this);
        errorDialog.showWithTwoButton(null,
                getString(R.string.label_verify_driver_license),
                getString(R.string.label_do_you_want_to_verify_driver_license),
                getString(R.string.label_yes),
                getString(R.string.label_no),
                (dialogInterface, i) -> registerDriverLicense(),
                dialog -> verifyDriverLicense());
    }

    private void verifyDriverLicense() {
        ProgressDialog progressDialog = new ProgressDialog(this, getString(
                R.string.label_verifying_driver_license));
        progressDialog.show();

        BlockIDSDK.getInstance().verifyDocument(AppConstant.dvcId, mDriverLicenseMap,
                new String[]{"dl_verify"}, (status, documentVerification, error) -> {
                    progressDialog.dismiss();
                    if (status) {
                        //Verification success, call documentRegistration API

                        // - Recommended for future use -
                        // Update DL dictionary to include array of token received
                        // from verifyDocument API response.

                        try {
                            JSONObject jsonObject = new JSONObject(documentVerification);
                            JSONArray certificates = jsonObject.getJSONArray("certifications");
                            String[] tokens = new String[certificates.length()];
                            for (int index = 0; index < certificates.length(); index++) {
                                tokens[index] = certificates.getJSONObject(index).getString("token");
                            }
                            mDriverLicenseMap.put("tokens", tokens);
                        } catch (Exception e) {
                            // do nothing
                        }
                        registerDriverLicense();
                    } else {
                        ErrorDialog errorDialog = new ErrorDialog(DriverLicenseScanActivity.this);
                        DialogInterface.OnDismissListener onDismissListener = dialogInterface -> {
                            errorDialog.dismiss();
                            finish();
                        };
                        errorDialog.show(null, getString(R.string.label_error),
                                error.getMessage() + " (" + error.getCode() + ")",
                                onDismissListener);
                    }
                });
    }
}