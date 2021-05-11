package com.onekosmos.blockidsample.ui.driverLicense;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.blockid.sdk.BlockIDSDK;
import com.blockid.sdk.cameramodule.DLScanner.DLScannerHelper;
import com.blockid.sdk.cameramodule.DLScanner.DLScanningOrder;
import com.blockid.sdk.cameramodule.camera.dlModule.IDriverLicenseListener;
import com.blockid.sdk.datamodel.BIDDriverLicense;
import com.blockid.sdk.document.BIDDocumentProvider;
import com.example.blockidsdkdemo.R;
import com.onekosmos.blockidsample.ui.liveID.LiveIDScanningActivity;
import com.onekosmos.blockidsample.util.DocumentHolder;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.ProgressDialog;

import static com.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_SOMETHING_WENT_WRONG;

/**
 * Created by Pankti Mistry on 04-04-2021.
 * Copyright © 2020 1Kosmos. All rights reserved.
 */
public class DriverLicenseScanActivity extends AppCompatActivity implements IDriverLicenseListener {
    private static int K_DL_EXPIRY_GRACE_DAYS = 90;
    public static final int K_DL_SCAN_REQUEST_CODE = 1011;
    private BIDDriverLicense mDriverLicense;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_license_scan);
        DLScannerHelper.getInstance().setDLScanningOrder(DLScanningOrder.FIRST_BACK_THEN_FRONT);
        DLScannerHelper.getInstance().setBackButtonColorHex("#" + Integer.toHexString(ContextCompat.getColor(this,R.color.black)));
        DLScannerHelper.getInstance().setTitleColorHex("#" + Integer.toHexString(ContextCompat.getColor(this,R.color.black)));
        DLScannerHelper.getInstance().setErrorDialogButtonColorHex("#" + Integer.toHexString(ContextCompat.getColor(this,R.color.black)));
        DLScannerHelper.getInstance().setmErrorDialogMessageColor2("#" + Integer.toHexString(ContextCompat.getColor(this,R.color.black)));
        Intent intent = DLScannerHelper.getInstance().getDLScanIntent(this, K_DL_EXPIRY_GRACE_DAYS, this);
        startActivityForResult(intent, K_DL_SCAN_REQUEST_CODE);
    }

    @Override
    public void onDriverLicenseResponse(BIDDriverLicense bidDriverLicense, String signatureToken, ErrorManager.ErrorResponse errorResponse) {
        if (bidDriverLicense == null) {
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
        mDriverLicense = bidDriverLicense;
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

    private void registerDL(BIDDriverLicense documentData) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();
        if (documentData != null) {
            BlockIDSDK.getInstance().registerDocument(this, documentData, BIDDocumentProvider.BIDDocumentType.driverLicense,
                    "", (status, error) -> {
                        progressDialog.dismiss();
                        if (status) {
                            Toast.makeText(this, getString(R.string.label_dl_enrolled_successfully), Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }

                        if (error == null)
                            error = new ErrorManager.ErrorResponse(K_SOMETHING_WENT_WRONG.getCode(), K_SOMETHING_WENT_WRONG.getMessage());

                        if (error.getCode() == ErrorManager.CustomErrors.K_LIVEID_IS_MANDATORY.getCode()) {
                            DocumentHolder.setData(documentData, BIDDocumentProvider.BIDDocumentType.driverLicense, "");
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