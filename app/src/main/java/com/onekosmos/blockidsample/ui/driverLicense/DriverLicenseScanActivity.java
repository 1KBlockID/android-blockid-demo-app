package com.onekosmos.blockidsample.ui.driverLicense;

import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_SOMETHING_WENT_WRONG;
import static com.onekosmos.blockid.sdk.document.BIDDocumentProvider.RegisterDocCategory.identity_document;
import static com.onekosmos.blockid.sdk.document.RegisterDocType.DL;
import static com.onekosmos.blockid.sdk.documentScanner.DocumentScannerActivity.K_DOCUMENT_SCAN_ERROR;
import static com.onekosmos.blockid.sdk.documentScanner.DocumentScannerActivity.K_DOCUMENT_SCAN_TYPE;
import static com.onekosmos.blockid.sdk.documentScanner.DocumentScannerActivity.K_UID;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.view.WindowCompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.DocumentScanner;
import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.ErrorResponse;
import com.onekosmos.blockid.sdk.BIDAPIs.userapis.UserAPI;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.documentScanner.BIDDocumentDataHolder;
import com.onekosmos.blockid.sdk.documentScanner.DocumentScannerActivity;
import com.onekosmos.blockid.sdk.documentScanner.DocumentScannerType;
import com.onekosmos.blockid.sdk.utils.BIDUtil;
import com.onekosmos.blockidsample.AppConstant;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.util.AppPermissionUtils;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.ProgressDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.UUID;


/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class DriverLicenseScanActivity extends AppCompatActivity {
    private static final int K_DL_PERMISSION_REQUEST_CODE = 1011;
    private final String[] K_CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private AppCompatImageView mImgBack;
    private AppCompatTextView mTxtBack;
    private LinkedHashMap<String, Object> mDriverLicenseMap;
    private boolean isRegistrationInProgress;
    private static final String K_LIVEID_OBJECT = "liveid_object";
    private static final String K_FACE = "face";
    private static final String K_PROOFED_BY = "proofedBy";
    private String mLiveIDImageB64, mLiveIDProofedBy;
    private static final String K_EXPIRED = "EXPIRED";
    private static final String K_ABANDONED = "ABANDONED";
    private String mUID;

    private final ActivityResultLauncher<Intent> documentSessionResult =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_CANCELED) {
                            ErrorResponse error;
                            if (result.getData() != null) {
                                error = BIDUtil.JSONStringToObject(
                                        result.getData().getStringExtra(K_DOCUMENT_SCAN_ERROR),
                                        ErrorResponse.class);
                                if (error != null) {
                                    showError(error);
                                } else {
                                    error = new ErrorResponse(K_SOMETHING_WENT_WRONG.getCode(),
                                            K_SOMETHING_WENT_WRONG.getMessage());
                                    showError(error);
                                }
                            } else {
                                finish();
                            }
                            return;
                        }

                        if (BIDDocumentDataHolder.hasData()) {
                            processData(BIDDocumentDataHolder.getData());
                        } else {
                            showError(new ErrorResponse(K_SOMETHING_WENT_WRONG.getCode(),
                                    K_SOMETHING_WENT_WRONG.getMessage()));
                        }
                    });

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 15+
            WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        }

        setContentView(R.layout.activity_driver_license_scan);
        initView();
        mUID = getIntent().getStringExtra(K_UID);

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
            errorDialog.show(null, null,
                    getString(R.string.label_camera_permission_alert), dialog -> {
                        errorDialog.dismiss();
                        finish();
                    });
        }
    }

    @Override
    public void onBackPressed() {
        if (!isRegistrationInProgress)
            super.onBackPressed();
    }

    /**
     * Initialize UI Object
     */
    private void initView() {
        mImgBack = findViewById(R.id.img_back);
        mImgBack.setOnClickListener(v -> onBackPressed());

        mTxtBack = findViewById(R.id.txt_back);
        mTxtBack.setOnClickListener(v -> onBackPressed());
    }

    /**
     * Start Driver License Scanning
     */
    private void startScan() {
        if (!isRegistrationInProgress) {
            Intent intent = new Intent(this, DocumentScannerActivity.class);
            intent.putExtra(K_DOCUMENT_SCAN_TYPE, DocumentScannerType.DL.getValue());
            intent.putExtra(K_UID, mUID);
            documentSessionResult.launch(intent);
        }
    }

    /**
     * Process the data received from the scanner
     *
     * @param data String result
     */
    private void processData(String data) {
        String responseStatus, dlObject, token, proofJWT;
        try {
            JSONObject dataObject = new JSONObject(data);

            responseStatus = dataObject.has("responseStatus") ?
                    dataObject.getString("responseStatus") : null;

            if (TextUtils.isEmpty(responseStatus) ||
                    responseStatus.equalsIgnoreCase(K_EXPIRED)) {
                ErrorDialog errorDialog = new ErrorDialog(this);
                errorDialog.show(null, getString(R.string.label_session_expired),
                        getString(R.string.label_verification_session_no_longer_available),
                        dialog -> {
                            errorDialog.dismiss();
                            finish();
                        });
                return;
            }

            if (TextUtils.isEmpty(responseStatus) ||
                    responseStatus.equalsIgnoreCase(K_ABANDONED)) {
                ErrorDialog errorDialog = new ErrorDialog(this);
                errorDialog.show(null, getString(R.string.label_scan_timeout_title),
                        getString(R.string.label_verification_session_no_longer_available),
                        dialog -> {
                            errorDialog.dismiss();
                            finish();
                        });
                return;
            }

            // responseStatus is empty or not success
            if (TextUtils.isEmpty(responseStatus) ||
                    !responseStatus.equalsIgnoreCase("SUCCESS")) {
                dlScanFailed();
                return;
            }

            token = dataObject.has("token") ? dataObject.getString("token") : null;

            // toke is empty
            if (TextUtils.isEmpty(token)) {
                dlScanFailed();
                return;
            }

            dlObject = dataObject.has("dl_object") ?
                    dataObject.getString("dl_object") : null;

            // dl object is empty
            if (TextUtils.isEmpty(dlObject)) {
                dlScanFailed();
                return;
            }

            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
            mDriverLicenseMap = gson.fromJson(dlObject,
                    new TypeToken<LinkedHashMap<String, Object>>() {
                    }.getType());

            // passport map is null
            if (mDriverLicenseMap == null) {
                dlScanFailed();
                return;
            }

            proofJWT = mDriverLicenseMap.containsKey("proof_jwt")
                    ? mDriverLicenseMap.get("proof_jwt") != null
                    ? Objects.requireNonNull(mDriverLicenseMap.get("proof_jwt")).toString()
                    : null : null;

            // proofJWT is empty
            if (TextUtils.isEmpty(proofJWT)) {
                dlScanFailed();
                return;
            }

            if (dataObject.has(K_LIVEID_OBJECT)) {
                JSONObject liveIDObject = dataObject.getJSONObject(K_LIVEID_OBJECT);
                if (liveIDObject.has(K_FACE)) {
                    mLiveIDImageB64 = liveIDObject.getString(K_FACE);
                }

                if (liveIDObject.has(K_PROOFED_BY)) {
                    mLiveIDProofedBy = liveIDObject.getString(K_PROOFED_BY);
                }
            }

            mDriverLicenseMap.put("certificate_token", token);
            mDriverLicenseMap.put("proof", proofJWT);
            verifyDriverLicenseDialog();
        } catch (Exception exception) {
            showError(new ErrorResponse(K_SOMETHING_WENT_WRONG.getCode(),
                    K_SOMETHING_WENT_WRONG.getMessage()));
        }
    }

    /**
     * Register Driver License
     */
    private void registerDriverLicense() {
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
            String mobileSessionID = BIDDocumentDataHolder.getSessionID();
            String mobileDocumentID = DL.getValue().toLowerCase() + "_" + UUID.randomUUID().toString();
            BlockIDSDK.getInstance().registerDocument(this, mDriverLicenseMap,
                    null, mobileSessionID, mobileDocumentID, (status, error) -> {
                        progressDialog.dismiss();
                        isRegistrationInProgress = false;
                        if (status) {
                            Toast.makeText(this, R.string.label_dl_enrolled_successfully,
                                    Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }

                        showError(error);
                    });
        }
    }

    private Bitmap convertBase64ToBitmap(String img) {
        if (TextUtils.isEmpty(img)) {
            return null;
        }
        byte[] decodedString = Base64.decode(img, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }

    /**
     * Register documents with LiveID
     */
    private void registerDocumentWithLiveID() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();
        isRegistrationInProgress = true;
        mImgBack.setClickable(false);
        mTxtBack.setClickable(false);
        mDriverLicenseMap.put("category", identity_document.name());
        mDriverLicenseMap.put("type", DL.getValue());
        mDriverLicenseMap.put("id", mDriverLicenseMap.get("id"));
        Bitmap liveIDBitmap = convertBase64ToBitmap(mLiveIDImageB64);
        String mobileSessionID = BIDDocumentDataHolder.getSessionID();
        String mobileDocumentID = DL.getValue().toLowerCase() + "_with_liveid_" +
                UUID.randomUUID().toString();
        BlockIDSDK.getInstance().registerDocument(this, mDriverLicenseMap, liveIDBitmap,
                mLiveIDProofedBy, null, null, mobileSessionID, mobileDocumentID, (status, error) -> {
                    progressDialog.dismiss();
                    isRegistrationInProgress = false;
                    if (status) {
                        Toast.makeText(this, R.string.label_dl_enrolled_successfully,
                                Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    showError(error);
                });
    }


    /**
     * Verify Driver License
     */
    private void verifyDriverLicenseDialog() {
        ErrorDialog errorDialog = new ErrorDialog(this);
        errorDialog.showWithTwoButton(null,
                getString(R.string.label_verify_driver_license),
                getString(R.string.label_do_you_want_to_verify_driver_license),
                getString(R.string.label_yes),
                getString(R.string.label_no),
                (dialogInterface, i) -> {
                    if (BlockIDSDK.getInstance().isLiveIDRegistered()) {
                        registerDriverLicense();
                    } else {
                        registerDocumentWithLiveID();
                    }
                },
                dialog -> verifyDriverLicense());
    }

    private void verifyDriverLicense() {
        ProgressDialog progressDialog = new ProgressDialog(this,
                getString(R.string.label_verifying_driver_license));
        progressDialog.show();

        BlockIDSDK.getInstance().verifyDocument(AppConstant.dvcId, mDriverLicenseMap,
                new String[]{"dl_verify"}, null, null, (status, documentVerification, error) -> {
                    progressDialog.dismiss();
                    if (status) {
                        //Verification success, call documentRegistration API

                        // - Recommended for future use -
                        // Update DL dictionary to include array of token received
                        // from verifyDocument API response.

                        try {
                            UserAPI.DocuVerifyResult docuVerifyResult = BIDUtil.
                                    JSONStringToObject(documentVerification,
                                            UserAPI.DocuVerifyResult.class);
                            JSONObject jsonObject = new JSONObject(docuVerifyResult.result);
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
                        ErrorDialog errorDialog = new ErrorDialog(this);
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

    /**
     * Show Error Dialog
     *
     * @param errorResponse = {@link ErrorResponse}
     */
    private void showError(ErrorResponse errorResponse) {
        // Don't show error when user canceled
        if (errorResponse.getCode() == DocumentScanner.CANCELED.getCode()) {
            finish();
            return;
        }

        ErrorDialog errorDialog = new ErrorDialog(this);
        DialogInterface.OnDismissListener onDismissListener = dialogInterface -> {
            errorDialog.dismiss();
            finish();
        };

        if (errorResponse.getCode() == 0) {
            errorDialog.showNoInternetDialog(onDismissListener);
        } else {
            errorDialog.show(null,
                    getString(R.string.label_error),
                    errorResponse.getMessage(),
                    onDismissListener);
        }
    }

    // Show Error dialog when scan is failed
    private void dlScanFailed() {
        ErrorDialog errorDialog = new ErrorDialog(this);
        errorDialog.show(null, getString(R.string.label_error),
                getString(R.string.label_dl_fail_to_scan), dialog -> {
                    errorDialog.dismiss();
                    finish();
                });
    }
}