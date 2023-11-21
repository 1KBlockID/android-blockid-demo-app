package com.onekosmos.blockidsample.ui.passport;

import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_CONNECTION_ERROR;
import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_LIVEID_IS_MANDATORY;
import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_SOMETHING_WENT_WRONG;
import static com.onekosmos.blockid.sdk.document.BIDDocumentProvider.RegisterDocCategory.identity_document;
import static com.onekosmos.blockid.sdk.document.RegisterDocType.PPT;
import static com.onekosmos.blockid.sdk.documentScanner.DocumentScannerActivity.K_DOCUMENT_SCAN_ERROR;
import static com.onekosmos.blockid.sdk.documentScanner.DocumentScannerActivity.K_DOCUMENT_SCAN_TYPE;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.ErrorResponse;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.documentScanner.BIDDocumentDataHolder;
import com.onekosmos.blockid.sdk.documentScanner.DocumentScannerActivity;
import com.onekosmos.blockid.sdk.documentScanner.DocumentScannerType;
import com.onekosmos.blockid.sdk.utils.BIDUtil;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.document.DocumentHolder;
import com.onekosmos.blockidsample.ui.liveID.ActiveLiveIDScanningActivity;
import com.onekosmos.blockidsample.util.AppPermissionUtils;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.ProgressDialog;

import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class PassportScanningActivity extends AppCompatActivity {
    private static final int K_PASSPORT_PERMISSION_REQUEST_CODE = 1011;
    private final String[] K_CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private AppCompatImageView mImgBack;
    private AppCompatTextView mTxtBack;
    private LinkedHashMap<String, Object> mPassportMap;
    private boolean isDeviceHasNfc, isRegistrationInProgress;
    private  String sessionID;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passport_scanning);
        isDeviceHasNfc = isDeviceHasNFC();
        initView();
        if (!AppPermissionUtils.isPermissionGiven(K_CAMERA_PERMISSION, this))
            AppPermissionUtils.requestPermission(this, K_PASSPORT_PERMISSION_REQUEST_CODE,
                    K_CAMERA_PERMISSION);
        else startScan();
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
        if (!isRegistrationInProgress) {
            super.onBackPressed();
        }
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
     * Start Passport Scanning
     */
    private void startScan() {
        Intent intent = new Intent(this, DocumentScannerActivity.class);
        intent.putExtra(K_DOCUMENT_SCAN_TYPE, DocumentScannerType.PPT.getValue());
        documentSessionResult.launch(intent);
    }

    /**
     * Process the data received from the scanner
     *
     * @param data String result
     */
    private void processData(String data) {
        String responseStatus, pptObject, token, proofJWT;
        try {
            JSONObject dataObject = new JSONObject(data);

            sessionID = dataObject.has("sessionId") ?
                    dataObject.getString("sessionId") : "";
            Log.e("pankti", "session id: " + sessionID);

            responseStatus = dataObject.has("responseStatus") ?
                    dataObject.getString("responseStatus") : null;

            // responseStatus is empty or not success
            if (TextUtils.isEmpty(responseStatus) ||
                    !responseStatus.equalsIgnoreCase("SUCCESS")) {
                ppScanFailed();
                return;
            }

            token = dataObject.has("token") ? dataObject.getString("token") : null;

            // toke is empty
            if (TextUtils.isEmpty(token)) {
                ppScanFailed();
                return;
            }

            pptObject = dataObject.has("ppt_object") ?
                    dataObject.getString("ppt_object") : null;

            // ppt object is empty
            if (TextUtils.isEmpty(pptObject)) {
                ppScanFailed();
                return;
            }

            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
            mPassportMap = gson.fromJson(pptObject,
                    new TypeToken<LinkedHashMap<String, Object>>() {
                    }.getType());

            // passport map is null
            if (mPassportMap == null) {
                ppScanFailed();
                return;
            }

            proofJWT = mPassportMap.containsKey("proof_jwt")
                    ? mPassportMap.get("proof_jwt") != null
                    ? Objects.requireNonNull(mPassportMap.get("proof_jwt")).toString()
                    : null : null;

            // proofJWT is empty
            if (TextUtils.isEmpty(proofJWT)) {
                ppScanFailed();
                return;
            }

            mPassportMap.put("certificate_token", token);
            mPassportMap.put("proof", proofJWT);
            if (isDeviceHasNfc) {
                openEPassportChipActivity();
            } else {
                registerPassport();
            }
        } catch (Exception exception) {
            showError(new ErrorResponse(K_SOMETHING_WENT_WRONG.getCode(),
                    K_SOMETHING_WENT_WRONG.getMessage()));
        }
    }

    /**
     * Start EPassportChipActivity for RFID scanning
     */
    private void openEPassportChipActivity() {
        DocumentHolder.setData(mPassportMap);
        Intent intent = new Intent(this, EPassportChipActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        finish();
    }

    /**
     * Register Passport
     */
    private void registerPassport() {
        mImgBack.setClickable(false);
        mTxtBack.setClickable(false);
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();
        isRegistrationInProgress = true;
        if (mPassportMap != null) {
            mPassportMap.put("category", identity_document.name());
            mPassportMap.put("type", PPT.getValue());
            mPassportMap.put("id", mPassportMap.get("id"));
            BlockIDSDK.getInstance().registerDocument(this, mPassportMap, null,
                    (status, error) -> {
                        progressDialog.dismiss();
                        isRegistrationInProgress = false;
                        if (status) {
                            Toast.makeText(this,
                                    R.string.label_passport_enrolled_successfully,
                                    Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }

                        if (error.getCode() == K_LIVEID_IS_MANDATORY.getCode()) {
                            DocumentHolder.setData(mPassportMap);
                            Intent intent = new Intent(this,
                                    ActiveLiveIDScanningActivity.class);
                            intent.putExtra(ActiveLiveIDScanningActivity.LIVEID_WITH_DOCUMENT,
                                    true);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                            startActivity(intent);
                            finish();
                            return;
                        }

                        showError(error);
                    });
        }
    }

    private boolean isDeviceHasNFC() {
        NfcManager manager = (NfcManager) getSystemService(Context.NFC_SERVICE);
        NfcAdapter adapter = manager.getDefaultAdapter();
        return adapter != null;
    }

    /**
     * Show Error Dialog
     *
     * @param errorResponse = {@link ErrorResponse}
     */
    private void showError(ErrorResponse errorResponse) {
        // Don't show error when user canceled
        if (errorResponse.getCode() == ErrorManager.DocumentScanner.CANCELED.getCode()) {
            finish();
            return;
        }

        ErrorDialog errorDialog = new ErrorDialog(this);
        DialogInterface.OnDismissListener onDismissListener = dialogInterface -> {
            errorDialog.dismiss();
            finish();
        };

        if (errorResponse.getCode() == ErrorManager.DocumentScanner.TIMEOUT.getCode()) {
            errorDialog.show(null, getString(R.string.label_scan_timeout_title),
                    getString(R.string.label_scan_timeout_message), dialog -> {
                        errorDialog.dismiss();
                        finish();
                    });
            return;
        }

        if (errorResponse.getCode() == K_CONNECTION_ERROR.getCode()) {
            errorDialog.showNoInternetDialog(onDismissListener);
        } else {
            errorDialog.show(null, getString(R.string.label_error), errorResponse.getMessage(), onDismissListener);
        }
    }

    // Show Error dialog when scan is failed
    private void ppScanFailed() {
        ErrorDialog errorDialog = new ErrorDialog(this);
        errorDialog.show(null, getString(R.string.label_error),
                getString(R.string.label_pp_fail_to_scan), dialog -> {
                    copyToClipboard();
                    errorDialog.dismiss();
                    finish();
                });
    }

    private void copyToClipboard() {
        if (TextUtils.isEmpty(sessionID)) {
            return;
        }
        try {
            ClipboardManager clipboard = (ClipboardManager) this.getSystemService(
                    CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("msg", sessionID);
            clipboard.setPrimaryClip(clip);
        } catch (Exception ignored) {
        }
    }
}