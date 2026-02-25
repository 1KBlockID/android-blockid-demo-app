package com.onekosmos.blockidsample.ui.nationalID;

import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_CONNECTION_ERROR;
import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_SOMETHING_WENT_WRONG;
import static com.onekosmos.blockid.sdk.document.BIDDocumentProvider.RegisterDocCategory.identity_document;
import static com.onekosmos.blockid.sdk.document.RegisterDocType.NATIONAL_ID;
import static com.onekosmos.blockid.sdk.documentScanner.DocumentScannerActivity.K_DOCUMENT_SCAN_ERROR;
import static com.onekosmos.blockid.sdk.documentScanner.DocumentScannerActivity.K_DOCUMENT_SCAN_TYPE;
import static com.onekosmos.blockid.sdk.documentScanner.DocumentScannerActivity.K_UID;
import static com.onekosmos.blockid.sdk.documentScanner.DocumentScannerType.DL;
import static com.onekosmos.blockid.sdk.documentScanner.DocumentScannerType.IDCARD;
import static com.onekosmos.blockid.sdk.documentScanner.DocumentScannerType.PPT;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
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
import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.ErrorResponse;
import com.onekosmos.blockid.sdk.BIDAPIs.userapis.UserAPI;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.document.BIDDocumentProvider;
import com.onekosmos.blockid.sdk.document.RegisterDocType;
import com.onekosmos.blockid.sdk.documentScanner.BIDDocumentDataHolder;
import com.onekosmos.blockid.sdk.documentScanner.DocumentScannerActivity;
import com.onekosmos.blockid.sdk.documentScanner.DocumentScannerType;
import com.onekosmos.blockid.sdk.utils.BIDUtil;
import com.onekosmos.blockidsample.AppConstant;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.document.DocumentHolder;
import com.onekosmos.blockidsample.ui.passport.EPassportChipActivity;
import com.onekosmos.blockidsample.util.AppPermissionUtils;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.IDVErrorCode;
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
public class NationalIDScanActivity extends AppCompatActivity {
    private static final int K_CAMERA_PERMISSION_REQUEST_CODE = 1011;
    private final String[] K_CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private AppCompatImageView mImgBack;
    private AppCompatTextView mTxtBack;
    private LinkedHashMap<String, Object> mNationalIDMap, mDocumentMap;
    private boolean isRegistrationInProgress, isDeviceHasNfc;
    private static final String K_LIVEID_OBJECT = "liveid_object";
    private static final String K_FACE = "face";
    private static final String K_PROOFED_BY = "proofedBy";
    private String mLiveIDImageB64, mLiveIDProofedBy;
    private static final String K_EXPIRED = "EXPIRED";
    private static final String K_ABANDONED = "ABANDONED";
    private String mUID;

    private final ActivityResultLauncher<Intent> ePassportResult =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            finish();
                        }
                    });
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

        setContentView(R.layout.activity_nationalid_scanning);
        initView();
        mUID = getIntent().getStringExtra(K_UID);

        if (!AppPermissionUtils.isPermissionGiven(K_CAMERA_PERMISSION, this)) {
            AppPermissionUtils.requestPermission(this, K_CAMERA_PERMISSION_REQUEST_CODE,
                    K_CAMERA_PERMISSION);
        } else {
            startScan();
        }
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
                        setResult(RESULT_CANCELED);
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
     * Start NationalID Scanning
     */
    private void startScan() {
        Intent intent = new Intent(this, DocumentScannerActivity.class);
        intent.putExtra(K_DOCUMENT_SCAN_TYPE, DocumentScannerType.IDCARD.getValue());
        intent.putExtra(K_UID, mUID);
        documentSessionResult.launch(intent);
    }

    /**
     * Process the data received from the scanner
     *
     * @param data String result
     */
    private void processData(String data) {
        String responseStatus, idCardObject, token, proofJWT;
        try {
            JSONObject dataObject = new JSONObject(data);

            responseStatus = dataObject.has("sessionResult") ?
                    dataObject.getString("sessionResult") : null;

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
                handleErrorResponse(dataObject);
                return;
            }

            token = dataObject.has("token") ? dataObject.getString("token") : null;

            // toke is empty
            if (TextUtils.isEmpty(token)) {
                nidScanFailed();
                return;
            }

            // Detect what document type was actually scanned
            String detectedDocType = detectActualDocumentType(dataObject);

            // CASE III: Handle cross-document detection for National ID
            // If DL or Passport detected while scanning National ID
            if (detectedDocType != null &&
                    (getDocumentScannerType(detectedDocType).getValue()
                            .equalsIgnoreCase(DL.getValue()) ||
                            getDocumentScannerType(detectedDocType).getValue()
                                    .equalsIgnoreCase(PPT.getValue()))) {
                handleCrossDocumentDetection(detectedDocType, dataObject);
                return;
            }
            // If other document type detected (not DL, not PPT, not IDCARD), continue as normal
            // The API will handle validation and show appropriate error if needed

            idCardObject = dataObject.has("document") ?
                    dataObject.getString("document") : null;

            //idcard object is empty
            if (TextUtils.isEmpty(idCardObject)) {
                nidScanFailed();
                return;
            }

            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
            mNationalIDMap = gson.fromJson(idCardObject,
                    new TypeToken<LinkedHashMap<String, Object>>() {
                    }.getType());

            // passport map is null
            if (mNationalIDMap == null) {
                nidScanFailed();
                return;
            }

            proofJWT = mNationalIDMap.containsKey("proof_jwt")
                    ? mNationalIDMap.get("proof_jwt") != null
                    ? Objects.requireNonNull(mNationalIDMap.get("proof_jwt")).toString()
                    : null : null;

            // proofJWT is empty
            if (TextUtils.isEmpty(proofJWT)) {
                nidScanFailed();
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

            mNationalIDMap.put("certificate_token", token);
            mNationalIDMap.put("proof", proofJWT);
            if (BlockIDSDK.getInstance().isLiveIDRegistered()) {
                registerNationalID();
            } else {
                registerDocumentWithLiveID(mNationalIDMap, NATIONAL_ID.getValue());
            }
        } catch (Exception exception) {
            showError(new ErrorResponse(K_SOMETHING_WENT_WRONG.getCode(),
                    K_SOMETHING_WENT_WRONG.getMessage()));
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
    private void registerDocumentWithLiveID(LinkedHashMap<String, Object>
                                                    documentMap, String detectedDocType) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();
        isRegistrationInProgress = true;
        mImgBack.setClickable(false);
        mTxtBack.setClickable(false);
        DocumentScannerType registerDocType = getDocumentScannerType(detectedDocType);
        documentMap.put("category", identity_document.name());
        documentMap.put("type", registerDocType.getValue());
        documentMap.put("id", documentMap.get("id"));
        Bitmap liveIDBitmap = convertBase64ToBitmap(mLiveIDImageB64);
        String mobileSessionID = BIDDocumentDataHolder.getSessionID();
        String mobileDocumentID = registerDocType.getValue().toLowerCase() + "_with_liveid_" +
                UUID.randomUUID().toString();
        BlockIDSDK.getInstance().registerDocument(this, documentMap, liveIDBitmap,
                mLiveIDProofedBy, null, null, mobileSessionID, mobileDocumentID,
                (status, error) -> {
                    progressDialog.dismiss();
                    isRegistrationInProgress = false;
                    if (status) {
                        String message = "";
                        if (registerDocType.getValue().equalsIgnoreCase(DL.getValue())) {
                            message = getString(R.string.label_dl_enrolled_successfully);
                        } else if (registerDocType.getValue().equalsIgnoreCase(PPT.getValue())) {
                            message = getString(R.string.label_passport_enrolled_successfully);
                        } else if (registerDocType.getValue().equalsIgnoreCase(IDCARD.getValue())) {
                            message = getString(R.string.label_nid_enrolled_successfully);
                        }

                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                        setResult(RESULT_OK);
                        finish();
                        return;
                    }
                    showError(error);
                });
    }

    /**
     * Register NationalID
     */
    private void registerNationalID() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();
        isRegistrationInProgress = true;
        mImgBack.setClickable(false);
        mTxtBack.setClickable(false);
        mNationalIDMap.put("category", identity_document.name());
        mNationalIDMap.put("type", NATIONAL_ID.getValue());
        mNationalIDMap.put("id", mNationalIDMap.get("id"));
        String mobileSessionID = BIDDocumentDataHolder.getSessionID();
        String mobileDocumentID = NATIONAL_ID.getValue().toLowerCase() + "_" +
                UUID.randomUUID().toString();
        BlockIDSDK.getInstance().registerDocument(this, mNationalIDMap, null,
                mobileSessionID, mobileDocumentID,
                (status, error) -> {
                    progressDialog.dismiss();
                    isRegistrationInProgress = false;
                    if (status) {
                        Toast.makeText(this, R.string.label_nid_enrolled_successfully,
                                Toast.LENGTH_LONG).show();
                        setResult(RESULT_OK);
                        finish();
                        return;
                    }

                    showError(error);
                });
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

        if (errorResponse.getCode() == K_CONNECTION_ERROR.getCode()) {
            errorDialog.showNoInternetDialog(onDismissListener);
        } else {
            errorDialog.show(null,
                    getString(R.string.label_error),
                    errorResponse.getMessage(),
                    onDismissListener);
        }
    }

    // Show Error dialog when scan is failed
    private void nidScanFailed() {
        ErrorDialog errorDialog = new ErrorDialog(this);
        errorDialog.show(null, getString(R.string.label_error),
                getString(R.string.label_nid_fail_to_scan), dialog -> {
                    errorDialog.dismiss();
                    finish();
                });
    }

    /**
     * Detect actual document type from API response
     *
     * @param dataObject JSON object from API response
     * @return Detected document type or null
     */
    private String detectActualDocumentType(JSONObject dataObject) {
        try {
            // Check if document object exists
            if (dataObject.has("document")) {
                JSONObject documentObj = dataObject.getJSONObject("document");

                // Get documentType field from document object
                if (documentObj.has("documentType")) {
                    String docType = documentObj.getString("documentType");

                    // Map documentType to scanner type
                    if (DocType.DL.getValue().equalsIgnoreCase(docType)) {
                        return DL.getValue();
                    } else if (DocType.PPT.getValue().equalsIgnoreCase(docType)) {
                        return PPT.getValue();
                    } else {
                        return IDCARD.getValue();
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    /**
     * Handle cross-document detection for National ID
     * Shows confirmation dialog when DL or Passport is detected while scanning National ID
     *
     * @param detectedType The actual document type detected (DL or PPT)
     * @param dataObject   The JSON object containing document data
     */
    private void handleCrossDocumentDetection(String detectedType, JSONObject dataObject) {
        String documentName;
        String title;
        String message;

        if (detectedType.equalsIgnoreCase(DL.getValue())) {
            documentName = "Drivers License";
            title = "Drivers License Identified";
            message = "We identified that you have scanned a Drivers License. Do you want to register the Drivers License in this application?";

            // Check if DL is already enrolled
            if (isDocumentEnrolled(RegisterDocType.DL.getValue())) {
                showDocumentAlreadyEnrolledError(documentName);
                return;
            }
        } else if (detectedType.equalsIgnoreCase(PPT.getValue())) {
            documentName = "Passport";
            title = "Passport Identified";
            message = "We identified that you have scanned a Passport. Do you want to register the Passport in this application?";

            // Check if Passport is already enrolled
            if (isDocumentEnrolled(RegisterDocType.PPT.getValue())) {
                showDocumentAlreadyEnrolledError(documentName);
                return;
            }
        } else {
            // Unknown document type
            return;
        }

        // Show confirmation dialog
        ErrorDialog confirmDialog = new ErrorDialog(this);
        confirmDialog.showWithTwoButton(
                null,
                title,
                message,
                getString(R.string.label_yes),
                getString(R.string.label_no),
                (dialog, which) -> {
                    // NO button - Cancel registration
                    confirmDialog.dismiss();
                    finish();
                },
                dialog -> {
                    // YES button - Continue with detected document type
                    confirmDialog.dismiss();
                    continueDocumentProcessing(dataObject, detectedType);
                }
        );
    }

    /**
     * Check if a document type is already enrolled
     *
     * @param docType Document type to check
     * @return true if document is enrolled, false otherwise
     */
    private boolean isDocumentEnrolled(String docType) {
        return BIDDocumentProvider.getInstance().isDocumentEnrolled(
                docType, BIDDocumentProvider.RegisterDocCategory.identity_document.name());
    }

    /**
     * Show error when document is already enrolled
     *
     * @param documentName Name of the document (e.g., "Drivers License", "Passport")
     */
    private void showDocumentAlreadyEnrolledError(String documentName) {
        ErrorDialog errorDialog = new ErrorDialog(this);
        errorDialog.showWithOneButton(
                null,
                getString(R.string.label_error),
                documentName + " is already enrolled.",
                getString(R.string.label_ok),
                dialog -> {
                    errorDialog.dismiss();
                    finish();
                }
        );
    }

    /**
     * Continue processing document after user confirmation
     *
     * @param dataObject   JSON object containing document data.
     * @param detectedType String document type detected while scanning.
     */
    private void continueDocumentProcessing(JSONObject dataObject, String detectedType) {
        try {
            String token = dataObject.has("token") ? dataObject.getString("token") : null;

            // Get document object (now unified as "document" for all types)
            String documentObject = dataObject.has("document") ?
                    dataObject.getJSONObject("document").toString() : null;

            if (TextUtils.isEmpty(documentObject)) {
                nidScanFailed();
                return;
            }

            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
            mNationalIDMap = gson.fromJson(documentObject,
                    new TypeToken<LinkedHashMap<String, Object>>() {
                    }.getType());

            if (mDocumentMap == null) {
                documentScanFailed(detectedType);
                return;
            }

            String proofJWT = mDocumentMap.containsKey("proof_jwt")
                    ? mDocumentMap.get("proof_jwt") != null
                    ? Objects.requireNonNull(mDocumentMap.get("proof_jwt")).toString()
                    : null : null;

            if (TextUtils.isEmpty(proofJWT)) {
                documentScanFailed(detectedType);
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

            mDocumentMap.put("certificate_token", token);
            mDocumentMap.put("proof", proofJWT);

            if (getDocumentScannerType(detectedType).getValue().equalsIgnoreCase(DL.getValue())) {
                verifyDriverLicenseDialog(detectedType);
            } else if (getDocumentScannerType(detectedType).getValue().equalsIgnoreCase(PPT.getValue())) {
                isDeviceHasNfc = isDeviceHasNFC();
                if (isDeviceHasNfc) {
                    openEPassportChipActivity();
                } else {
                    if (BlockIDSDK.getInstance().isLiveIDRegistered()) {
                        registerPassport();
                    } else {
                        registerDocumentWithLiveID(mDocumentMap, detectedType);
                    }
                }
            }
        } catch (Exception exception) {
            showError(new ErrorResponse(K_SOMETHING_WENT_WRONG.getCode(),
                    K_SOMETHING_WENT_WRONG.getMessage()));
        }
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
        if (mDocumentMap != null) {
            mDocumentMap.put("category", identity_document.name());
            mDocumentMap.put("type", RegisterDocType.PPT.getValue());
            mDocumentMap.put("id", mDocumentMap.get("id"));
            String mobileSessionID = BIDDocumentDataHolder.getSessionID();
            String mobileDocumentID = RegisterDocType.PPT.getValue().toLowerCase() + "_" +
                    UUID.randomUUID().toString();
            BlockIDSDK.getInstance().registerDocument(this, mDocumentMap, null,
                    mobileSessionID, mobileDocumentID,
                    (status, error) -> {
                        progressDialog.dismiss();
                        isRegistrationInProgress = false;
                        if (status) {
                            Toast.makeText(this,
                                    R.string.label_passport_enrolled_successfully,
                                    Toast.LENGTH_LONG).show();
                            setResult(RESULT_OK);
                            finish();
                            return;
                        }

                        showError(error);
                    });
        }
    }

    /**
     * Start EPassportChipActivity for RFID scanning
     */
    private void openEPassportChipActivity() {
        DocumentHolder.setData(mDocumentMap);
        Intent intent = new Intent(this, EPassportChipActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        ePassportResult.launch(intent);
    }

    private boolean isDeviceHasNFC() {
        NfcManager manager = (NfcManager) getSystemService(Context.NFC_SERVICE);
        NfcAdapter adapter = manager.getDefaultAdapter();
        return adapter != null;
    }

    /**
     * Handle error response from API when sessionResult is not SUCCESS
     * Parses errorInfo and reasonCode to show user-friendly messages using IDPErrorCode
     *
     * @param dataObject JSON object from API response
     */
    private void handleErrorResponse(JSONObject dataObject) {
        try {
            String errorCode = null;
            String errorMessage = null;

            // Try to extract error information from the response
            if (dataObject.has("errorInfo")) {
                JSONObject errorInfo = dataObject.getJSONObject("errorInfo");
                if (errorInfo.has("reasonCode")) {
                    errorCode = errorInfo.getString("reasonCode");
                }
                if (errorInfo.has("message")) {
                    errorMessage = errorInfo.getString("message");
                }
            }

            // If we have a valid IDP error code, use the user-friendly message
            if (errorCode != null && IDVErrorCode.isValidCode(errorCode)) {
                String userMessage = IDVErrorCode.getUserMessageFromCode(errorCode);
                if (TextUtils.isEmpty(userMessage))
                    showErrorDialog(errorMessage);
                else
                    showErrorDialog(userMessage);
            } else {
                // EDGE CASE: If error code doesn't exist or doesn't match, show generic message
                showErrorDialog(getString(R.string.label_we_couldn_t_complete_the_verification_of_the_document_please_try_again));
            }
        } catch (Exception e) {
            // Fallback to generic error
            showErrorDialog(getString(R.string.label_we_couldn_t_complete_the_verification_of_the_document_please_try_again));
        }
    }

    /**
     * Show error dialog with custom message and navigate to My Identity on dismiss
     *
     * @param message Error message to display
     */
    private void showErrorDialog(String message) {
        ErrorDialog errorDialog = new ErrorDialog(this);
        errorDialog.show(null,
                getString(R.string.label_error),
                message,
                dialog -> {
                    errorDialog.dismiss();
                    finish();
                });
    }

    /**
     * Show document scanned failed when document (dl, ppt, id object) is empty
     */
    private void documentScanFailed(String detectedDocumentType) {
        String message = "";
        if (TextUtils.isEmpty(detectedDocumentType)) {
            nidScanFailed();
            return;
        }

        String doctype = getDocumentScannerType(detectedDocumentType).getValue();
        if (doctype == null) {
            nidScanFailed();
            return;
        }

        if (doctype.equalsIgnoreCase(DL.getValue())) {
            message = getString(R.string.label_dl_fail_to_scan);
        } else if (doctype.equalsIgnoreCase(PPT.getValue())) {
            message = getString(R.string.label_pp_fail_to_scan);
        } else if (doctype.equalsIgnoreCase(IDCARD.getValue())) {
            message = getString(R.string.label_nid_fail_to_scan);
        }

        ErrorDialog errorDialog = new ErrorDialog(this);
        errorDialog.show(null, getString(R.string.label_error), message,
                dialog -> {
                    errorDialog.dismiss();
                    finish();
                });
    }

    /**
     * Verify Driver License
     */
    private void verifyDriverLicenseDialog(String detectedType) {
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
                        registerDocumentWithLiveID(mDocumentMap, detectedType);
                    }
                },
                dialog -> verifyDriverLicense());
    }

    private void verifyDriverLicense() {
        ProgressDialog progressDialog = new ProgressDialog(this,
                getString(R.string.label_verifying_driver_license));
        progressDialog.show();

        BlockIDSDK.getInstance().verifyDocument(AppConstant.dvcId, mDocumentMap,
                new String[]{"dl_verify"}, null, null,
                (status, documentVerification, error) -> {
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
                            mDocumentMap.put("tokens", tokens);
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
     * Register Driver License
     */
    private void registerDriverLicense() {
        mImgBack.setClickable(false);
        mTxtBack.setClickable(false);
        ProgressDialog progressDialog = new ProgressDialog(this,
                getString(R.string.label_registering_driver_license));
        progressDialog.show();
        isRegistrationInProgress = true;
        if (mDocumentMap != null) {
            mDocumentMap.put("category", identity_document.name());
            mDocumentMap.put("type", RegisterDocType.DL.getValue());
            mDocumentMap.put("id", mDocumentMap.get("id"));
            String mobileSessionID = BIDDocumentDataHolder.getSessionID();
            String mobileDocumentID = RegisterDocType.DL.getValue().toLowerCase() + "_" +
                    UUID.randomUUID().toString();
            BlockIDSDK.getInstance().registerDocument(this, mDocumentMap,
                    null, mobileSessionID, mobileDocumentID, (status, error) -> {
                        progressDialog.dismiss();
                        isRegistrationInProgress = false;
                        if (status) {
                            Toast.makeText(this, R.string.label_dl_enrolled_successfully,
                                    Toast.LENGTH_LONG).show();
                            setResult(RESULT_OK);
                            finish();
                            return;
                        }

                        showError(error);
                    });
        }
    }

    public enum DocType {
        DL("DL"),
        PPT("PASSPORT");
        private final String docType;

        private DocType(String documentType) {
            this.docType = documentType;
        }

        public String getValue() {
            return this.docType;
        }
    }

    /**
     * Get DocumentScannerType based on document type
     *
     * @return string value of text
     */
    private DocumentScannerType getDocumentScannerType(String documentType) {
        if (documentType.equalsIgnoreCase(DocType.DL.getValue()))
            return DocumentScannerType.DL;
        else if (documentType.equalsIgnoreCase(DocType.PPT.getValue()))
            return DocumentScannerType.PPT;
        else
            return IDCARD;
    }
}