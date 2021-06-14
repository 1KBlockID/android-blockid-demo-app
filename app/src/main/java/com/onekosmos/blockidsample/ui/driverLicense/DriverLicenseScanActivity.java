package com.onekosmos.blockidsample.ui.driverLicense;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.blockid.sdk.BlockIDSDK;
import com.blockid.sdk.cameramodule.DLScanner.DLScannerHelper;
import com.blockid.sdk.cameramodule.DLScanner.DLScanningOrder;
import com.blockid.sdk.cameramodule.camera.dlModule.IDriverLicenseListener;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.document.DocumentHolder;
import com.onekosmos.blockidsample.ui.liveID.LiveIDScanningActivity;
import com.onekosmos.blockidsample.util.AppPermissionUtils;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.ProgressDialog;

import org.json.JSONObject;

import java.util.LinkedHashMap;

import static com.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_SOMETHING_WENT_WRONG;
import static com.blockid.sdk.document.BIDDocumentProvider.RegisterDocCategory.identity_document;
import static com.blockid.sdk.document.RegisterDocType.DL;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class DriverLicenseScanActivity extends AppCompatActivity implements IDriverLicenseListener {
    private static final int K_DL_PERMISSION_REQUEST_CODE = 1012;
    private final String[] K_CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private static int K_DL_EXPIRY_GRACE_DAYS = 90;
    public static final int K_DL_SCAN_REQUEST_CODE = 1011;
    private LinkedHashMap<String, Object> mDriverLicense;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_license_scan);
        if (!AppPermissionUtils.isPermissionGiven(K_CAMERA_PERMISSION, this))
            AppPermissionUtils.requestPermission(this, K_DL_PERMISSION_REQUEST_CODE, K_CAMERA_PERMISSION);
        else
            startDLIntent();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (AppPermissionUtils.isGrantedPermission(this, requestCode, grantResults, K_CAMERA_PERMISSION)) {
            startDLIntent();
        } else {
            ErrorDialog errorDialog = new ErrorDialog(this);
            errorDialog.show(null,
                    "",
                    getString(R.string.label_passport_camera_permission_alert), dialog -> {
                        errorDialog.dismiss();
                        finish();
                    });
        }
    }

    @Override
    public void onDriverLicenseResponse(LinkedHashMap<String, Object> driverLicenseMap, String signatureToken, ErrorManager.ErrorResponse errorResponse) {
        if (driverLicenseMap == null) {
            if (errorResponse.getCode() != ErrorManager.CustomErrors.K_DL_SCAN_CANCEL.getCode()) {
                ErrorDialog errorDialog = new ErrorDialog(this);
                errorDialog.show(null,
                        getString(R.string.label_error),
                        errorResponse.getMessage(), dialog -> {
                            errorDialog.dismiss();
                            finish();
                        });
            }
            return;
        }
        mDriverLicense = driverLicenseMap;
        token = signatureToken;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == K_DL_SCAN_REQUEST_CODE) {
            if (resultCode == RESULT_OK)
                registerDL(mDriverLicense);
            else
                finish();
        }
    }

    private void startDLIntent() {
        DLScannerHelper.getInstance().setDLScanningOrder(DLScanningOrder.FIRST_BACK_THEN_FRONT);
        String blackColor = "#" + Integer.toHexString(ContextCompat.getColor(this, R.color.black));
        DLScannerHelper.getInstance().setBackButtonColorHex(blackColor);
        DLScannerHelper.getInstance().setTitleColorHex(blackColor);
        DLScannerHelper.getInstance().setErrorDialogButtonColorHex(blackColor);
        DLScannerHelper.getInstance().setErrorDialogMessageColor2(blackColor);
        Intent intent = DLScannerHelper.getInstance().getDLScanIntent(this, K_DL_EXPIRY_GRACE_DAYS, this);
        if (intent != null)
            startActivityForResult(intent, K_DL_SCAN_REQUEST_CODE);
    }

    private void registerDL(LinkedHashMap<String, Object> dlMap) {
        if(dlMap != null){
            Log.d("Driver City","===>" + dlMap.get("city"));
        }
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();
        if (dlMap != null) {
            dlMap.put("category", identity_document.name());
            dlMap.put("type", DL.getValue());
            dlMap.put("id", dlMap.get("id"));

            BlockIDSDK.getInstance().registerDocument(this, dlMap,
                    null, (status, error) -> {
                        progressDialog.dismiss();
                        if (status) {
                            Toast.makeText(this, getString(R.string.label_dl_enrolled_successfully), Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }

                        if (error == null)
                            error = new ErrorManager.ErrorResponse(K_SOMETHING_WENT_WRONG.getCode(), K_SOMETHING_WENT_WRONG.getMessage());

                        if (error.getCode() == ErrorManager.CustomErrors.K_LIVEID_IS_MANDATORY.getCode()) {
                            DocumentHolder.setData(dlMap, null);
                            Intent intent = new Intent(this, LiveIDScanningActivity.class);
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
}