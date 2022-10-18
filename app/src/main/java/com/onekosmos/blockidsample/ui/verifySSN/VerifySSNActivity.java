package com.onekosmos.blockidsample.ui.verifySSN;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_CONNECTION_ERROR;
import static com.onekosmos.blockid.sdk.document.BIDDocumentProvider.RegisterDocCategory.identity_document;
import static com.onekosmos.blockid.sdk.document.RegisterDocType.DL;
import static com.onekosmos.blockid.sdk.document.RegisterDocType.SSN;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.datamodel.BIDGenericResponse;
import com.onekosmos.blockid.sdk.document.BIDDocumentProvider;
import com.onekosmos.blockid.sdk.document.RegisterDocType;
import com.onekosmos.blockid.sdk.utils.BIDUtil;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.util.AppPermissionUtils;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.ProgressDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2022 1Kosmos. All rights reserved.
 */
public class VerifySSNActivity extends AppCompatActivity {
    private final String[] K_STORAGE_PERMISSION = new String[]{WRITE_EXTERNAL_STORAGE,
            READ_EXTERNAL_STORAGE};
    private int K_STORAGE_PERMISSION_REQUEST_CODE = 1001;
    private ImageView mBackBtn, mWebShareButton, mWebCancelButton;
    private EditText mSSN, mBirthDate;
    private ScrollView mScrollView;
    private RelativeLayout mWebLayout;
    private WebView mWebView;
    private TextView mBackText, mBackTextWebView;
    private CheckBox mConsentCB;
    private Button mContinueBtn;
    private String requiredDateFormat = "yyyyMMdd";
    private String displayDateFormat = "MM-dd-yyyy";
    private String mMaskData = "XXXXXX";
    private JSONObject mDLObject;
    private JSONArray maskedJsonArray = new JSONArray();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_ssn);

        mScrollView = findViewById(R.id.scrollView);
        mWebLayout = findViewById(R.id.webLayout);
        mBackBtn = findViewById(R.id.bckBtn);
        mSSN = findViewById(R.id.ssn_text);
        mBirthDate = findViewById(R.id.bdate_text);
        mConsentCB = findViewById(R.id.consent_cb);
        mContinueBtn = findViewById(R.id.btn_continue);
        mBackText = findViewById(R.id.txt_back);
        mWebView = findViewById(R.id.web_view);
        mWebShareButton = findViewById(R.id.webShareBtn);
        mWebCancelButton = findViewById(R.id.webBckBtn);
        mBackTextWebView = findViewById(R.id.txt_back_web);

        mScrollView.setVisibility(View.VISIBLE);
        mWebLayout.setVisibility(View.GONE);
        String dlDob = returnDataFromEnrolledDocument("dob");
        mBirthDate.setText(changeDateFormat(dlDob, requiredDateFormat, displayDateFormat));

        mConsentCB.setOnClickListener(v -> {
            if (!mConsentCB.isChecked()) {
                mContinueBtn.setBackgroundColor(getColor(android.R.color.darker_gray));
                mContinueBtn.setEnabled(false);
            } else {
                mContinueBtn.setBackgroundColor(getColor(R.color.black));
                mContinueBtn.setEnabled(true);
            }
        });
        mContinueBtn.setOnClickListener(v -> validateAndVerifySSN());
        mBackBtn.setOnClickListener(v -> onBackPressed());
        mBackText.setOnClickListener(v -> onBackPressed());
        mBackTextWebView.setOnClickListener(v -> onBackPressed());
        mWebShareButton.setOnClickListener(v -> {
            checkStoragePermission();
        });
        mWebCancelButton.setOnClickListener(v -> {
            mScrollView.setVisibility(View.VISIBLE);
            mWebLayout.setVisibility(View.GONE);
        });
    }

    @Override
    public void onBackPressed() {
        if (mWebLayout.getVisibility() == View.VISIBLE) {
            mScrollView.setVisibility(View.VISIBLE);
            mWebLayout.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    private void validateAndVerifySSN() {
        if (TextUtils.isEmpty(mSSN.getText().toString().trim())) {
            Toast.makeText(this, "Enter SSN", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(mBirthDate.getText().toString().trim())) {
            Toast.makeText(this, "Enter Birth Date", Toast.LENGTH_SHORT).show();
        } else {
            verifySSN();
        }
    }

    public String changeDateFormat(String time, String inputFormatDate, String outputFormatDate) {
        String inputPattern = inputFormatDate;
        String outputPattern = outputFormatDate;
        @SuppressLint("SimpleDateFormat") SimpleDateFormat inputFormat =
                new SimpleDateFormat(inputPattern);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat outputFormat =
                new SimpleDateFormat(outputPattern);

        Date date = null;

        try {
            date = inputFormat.parse(time);
            return outputFormat.format(date);
        } catch (ParseException e) {
            return null;
        }
    }

    private void verifySSN() {
        ProgressDialog progressDialog = new ProgressDialog(this,
                getString(R.string.label_verifying_ssn));
        progressDialog.show();
        LinkedHashMap<String, Object> mSSNMap = new LinkedHashMap<String, Object>();
        String ssnNumber = mSSN.getText().toString().trim();
        mSSNMap.put("id", BIDUtil.getSha256Hash(ssnNumber));
        mSSNMap.put("type", SSN.getValue());
        mSSNMap.put("ssn", ssnNumber);
        mSSNMap.put("dob", changeDateFormat(mBirthDate.getText().toString().trim(),
                displayDateFormat, requiredDateFormat));
        BlockIDSDK.getInstance().verifyDocument(mSSNMap, new String[]{"ssn_verify"},
                (status, documentVerification, error) -> {
                    if (status) {
                        progressDialog.dismiss();
                        try {
                            JSONObject jsonObject = new JSONObject(documentVerification);
                            JSONArray certificates = jsonObject.getJSONArray("certifications");
                            JSONObject certificate = certificates.length() > 0 ?
                                    certificates.getJSONObject(0) : null;

                            if (certificate == null) {
                                progressDialog.dismiss();
                                handleInvalidData();
                                return;
                            }


                            if (!certificate.getBoolean("verified") ||
                                    certificate.getJSONObject("metadata").
                                            getJSONArray("verifiedPeople")
                                            .length() != 1) {
                                progressDialog.dismiss();
                                handleFailedSSNVerification(jsonObject);
                            } else {
                                JSONObject verifiedPersonObject = certificate.
                                        getJSONObject("metadata")
                                        .getJSONArray("verifiedPeople").getJSONObject(0);

                                if (!isDataTriangulationSuccessful(verifiedPersonObject)) {
                                    progressDialog.dismiss();
                                    handleFailedSSNVerification(jsonObject);
                                    return;
                                }
                                verifiedPersonObject.put("ssn", mSSN.getText().
                                        toString().trim());
                                verifiedPersonObject.put("certificate_token_value",
                                        certificate
                                                .getString("token"));
                                verifiedPersonObject.put("proofedBy", certificate
                                        .getString("authority"));
                                handleSuccessSSNVerification(verifiedPersonObject);
                            }
                        } catch (JSONException ignore) {
                        }
                    } else {
                        progressDialog.dismiss();
                        ErrorDialog errorDialog = new ErrorDialog(this);
                        DialogInterface.OnDismissListener onDismissListener = dialogInterface -> {
                            errorDialog.dismiss();
                            finish();
                        };
                        if (error.getCode() == K_CONNECTION_ERROR.getCode()) {
                            errorDialog.showNoInternetDialog(onDismissListener);
                            return;
                        }
                        errorDialog.show(null, getString(R.string.label_error),
                                error.getMessage(), onDismissListener);
                    }
                });
    }

    private boolean isDataTriangulationSuccessful(JSONObject verifiedPersonObject) {
        boolean isDataTriangulationSuccessful = false;
        try {
            String ssnFirstName = verifiedPersonObject.getJSONObject("firstName")
                    .getString("value").toLowerCase();
            String ssnLastName = verifiedPersonObject.getJSONObject("lastName")
                    .getString("value").toLowerCase();
            String docFirstName = Objects.requireNonNull(
                    returnDataFromEnrolledDocument("firstName")).toLowerCase();
            String docLastName = Objects.requireNonNull(
                    returnDataFromEnrolledDocument("lastName")).toLowerCase();

            if ((ssnFirstName.equals(docFirstName) &&
                    ssnLastName.equals(docLastName) ||
                    (ssnFirstName.equals(docLastName)
                            && ssnLastName.equals(docFirstName)))) {
                isDataTriangulationSuccessful = true;
            }
        } catch (JSONException e) {
            return false;
        }
        return isDataTriangulationSuccessful;
    }

    private String returnDataFromEnrolledDocument(String detailType) {
        String value = null;
        try {
            JSONObject dataObject = getDLData();
            if (dataObject.has(detailType)) {
                value = dataObject.getString(detailType);
            }
        } catch (JSONException e) {
            return null;
        }
        return value;
    }

    private JSONObject getDLData() {
        if (mDLObject != null) {
            return mDLObject;
        }

        try {
            String dlArrayList = BIDDocumentProvider.getInstance().getUserDocument(null,
                    DL.getValue(), identity_document.name());
            JSONArray docData = new JSONArray(dlArrayList);
            if (docData.length() >= 0) {
                mDLObject = docData.getJSONObject(0);
                return mDLObject;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private void handleFailedSSNVerification(JSONObject responseObject) {
        maskSSNResponse(responseObject);
        ErrorDialog errorDialog = new ErrorDialog(VerifySSNActivity.this);
        errorDialog.showWithTwoButton(
                null,
                getString(R.string.label_error),
                getString(R.string.ssn_verification_failed_no_match),
                getString(R.string.label_details), getString(R.string.label_retry),
                (dialogInterface, i) ->
                        errorDialog.dismiss(),
                dialog -> {
                    errorDialog.dismiss();
                    viewResponse(responseObject);
                });
    }

    private void viewResponse(JSONObject responseObject) {
        mScrollView.setVisibility(View.GONE);
        mWebLayout.setVisibility(View.VISIBLE);
        mWebView.setVerticalScrollBarEnabled(true);
        mWebView.setHorizontalScrollBarEnabled(true);
        String respObjStr = responseObject.toString();
        mWebView.loadData(respObjStr, "text/json", "utf-8");
    }

    private void handleSuccessSSNVerification(JSONObject dataObject) {
        ErrorDialog errorDialog = new ErrorDialog(VerifySSNActivity.this);
        errorDialog.showWithTwoButton(null,
                getString(R.string.label_success),
                getString(R.string.label_register_ssn),
                getString(R.string.label_yes),
                getString(R.string.label_no),
                (dialogInterface, i) -> errorDialog.dismiss(),
                dialog -> registerSSN(dataObject));
    }

    private void registerSSN(JSONObject dataObject) {
        ProgressDialog progressDialog = new ProgressDialog(this,
                getString(R.string.label_enrolling_ssn));
        progressDialog.show();
        try {
            LinkedHashMap<String, Object> ssnMap = new LinkedHashMap<>();
            ssnMap.put("id", BIDUtil.getSha256Hash(dataObject.getString("ssn")));
            ssnMap.put("type", RegisterDocType.SSN.getValue());
            ssnMap.put("documentId", BIDUtil.getSha256Hash(dataObject.getString("ssn")));
            ssnMap.put("documentType", RegisterDocType.SSN.name());
            ssnMap.put("image", getSSNImage());
            ssnMap.put("category", identity_document.name());
            ssnMap.put("ssn", dataObject.getString("ssn"));
            ssnMap.put("proofedBy", dataObject.getString("proofedBy"));
            ssnMap.put("firstName", dataObject.getJSONObject("firstName")
                    .getString("value"));
            ssnMap.put("middleName", dataObject.getJSONObject("middleName")
                    .getString("value"));
            ssnMap.put("lastName", dataObject.getJSONObject("lastName")
                    .getString("value"));
            ssnMap.put("dob", parseDate(dataObject.getJSONObject("dateOfBirth")));
            ssnMap.put("doe", addYearsToDate(parseDate(dataObject.getJSONObject("dateOfBirth"))));
            ssnMap.put("face", getBitMapToBase64(Objects
                    .requireNonNull(getUserImage())));
            ssnMap.put("addresses", getAddressArray(dataObject.getJSONArray("addresses")));
            ssnMap.put("doi", parseDate(dataObject.getJSONObject("dateOfBirth")));
            ssnMap.put("verifiedScan", true);
            ssnMap.put("certificate_token", dataObject.getString("certificate_token_value"));

            BlockIDSDK.getInstance().registerDocument(this, ssnMap, null,
                    (status, error) -> {
                        progressDialog.dismiss();
                        if (status) {
                            ErrorDialog errorDialog = new ErrorDialog(VerifySSNActivity.this);
                            errorDialog.showWithOneButton(null,
                                    getString(R.string.label_success),
                                    getString(R.string.label_ssn_enrolled_successfully),
                                    getString(R.string.label_ok),
                                    dialog -> {
                                        errorDialog.dismiss();
                                        finish();
                                    });
                        } else {
                            handleInvalidData();
                        }
                    });

        } catch (Exception e) {
            progressDialog.dismiss();
            handleInvalidData();
        }
    }

    private String[] getAddressArray(JSONArray addresses) {
        int addressesLength = addresses.length();
        String[] addressList = new String[addressesLength];
        for (int addressIndex = 0; addressIndex < addresses.length(); addressIndex++) {
            try {
                addressList[addressIndex] = (addresses.getJSONObject(addressIndex)
                        .getString("value"));
            } catch (JSONException e) {
                return addressList;
            }
        }
        return addressList;
    }

    private Bitmap getUserImage() {
        BIDGenericResponse response = BlockIDSDK.getInstance().getLiveIdImage();
        if (response == null)
            return null;

        if (response.getDataObject() == null)
            return null;

        return response.getDataObject();
    }

    @SuppressLint("SimpleDateFormat")
    private String addYearsToDate(String date) {
        try {
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat inputFormat = new SimpleDateFormat(requiredDateFormat);
            SimpleDateFormat outputFormat = new SimpleDateFormat(requiredDateFormat);
            calendar.setTime(Objects.requireNonNull(inputFormat.parse(date)));
            calendar.add(Calendar.YEAR, 150);
            return outputFormat.format(calendar.getTime());
        } catch (ParseException e) {
            return null;
        }
    }

    private String parseDate(JSONObject dateObject) {
        String requiredDateFormat;
        try {
            String day = numberConversion(dateObject.getJSONObject("day").getString("value"));
            String month = numberConversion(dateObject.getJSONObject("month").
                    getString("value"));
            String year = dateObject.getJSONObject("year").getString("value");
            requiredDateFormat = year.concat(month).concat(day);
        } catch (JSONException ignored) {
            return null;
        }
        return requiredDateFormat;
    }

    private String numberConversion(String value) {
        String formattedValue;
        if (Integer.parseInt(value) > 9) {
            formattedValue = value;
        } else {
            formattedValue = "0".concat(value);
        }
        return formattedValue;
    }

    private String getSSNImage() {
        Bitmap ssnImage = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        return getBitMapToBase64(ssnImage);
    }

    private static String getBitMapToBase64(Bitmap bmp) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 0, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void handleInvalidData() {
        ErrorDialog errorDialog = new ErrorDialog(VerifySSNActivity.this);

        errorDialog.showWithOneButton(null,
                getString(R.string.label_error),
                getString(R.string.bad_data),
                getString(R.string.label_retry),
                dialog -> errorDialog.dismiss());
    }

    private void maskSSNResponse(JSONObject responseObject) {
        try {
            JSONArray certifications = responseObject.getJSONArray("certifications");
            for (int index = 0; index < certifications.length(); index++) {
                if (certifications.getJSONObject(index).has("metadata") &&
                        certifications.getJSONObject(index).getJSONObject("metadata").
                                has("verifiedPeople")) {
                    JSONArray verifiedPeopleArray = certifications.getJSONObject(index).
                            getJSONObject("metadata").getJSONArray("verifiedPeople");
                    for (int verifiedPeopleIndex = 0; verifiedPeopleIndex <
                            verifiedPeopleArray.length(); verifiedPeopleIndex++) {
                        JSONObject data = verifiedPeopleArray.getJSONObject(verifiedPeopleIndex);

                        if (data.has("firstName") && data.getJSONObject("firstName").
                                has("value")) {
                            data.getJSONObject("firstName").put("value", mMaskData);
                        }

                        if (data.has("middleName") && data.getJSONObject("middleName").
                                has("value")) {
                            data.getJSONObject("middleName").put("value", mMaskData);
                        }

                        if (data.has("lastName") && data.getJSONObject("lastName").
                                has("value")) {
                            data.getJSONObject("lastName").put("value", mMaskData);
                        }

                        if (data.has("ssn") && data.getJSONObject("ssn").has("value")) {
                            data.getJSONObject("ssn").put("value", mMaskData);
                        }

                        if (data.has("dateOfBirth")) {
                            if (data.getJSONObject("dateOfBirth").has("month")) {
                                data.getJSONObject("dateOfBirth").getJSONObject("month").
                                        put("value", mMaskData);
                            }

                            if (data.getJSONObject("dateOfBirth").has("day")) {
                                data.getJSONObject("dateOfBirth").getJSONObject("day").
                                        put("value", mMaskData);
                            }

                            if (data.getJSONObject("dateOfBirth").has("year")) {
                                data.getJSONObject("dateOfBirth").getJSONObject("year").
                                        put("value", mMaskData);
                            }
                        }

                        if (data.has("age") && data.getJSONObject("age").has("value")) {
                            data.getJSONObject("age").put("value", mMaskData);
                        }

                        if (data.has("addresses")) {
                            for (int addIndex = 0; addIndex < data.getJSONArray("addresses").
                                    length(); addIndex++) {
                                data.getJSONArray("addresses").getJSONObject(addIndex).
                                        put("value", mMaskData);
                            }
                        }

                        if (data.has("emails")) {
                            for (int emailIndex = 0; emailIndex < data.getJSONArray("emails").
                                    length(); emailIndex++) {
                                data.getJSONArray("emails").getJSONObject(emailIndex).
                                        put("value", mMaskData);
                            }
                        }

                        if (data.has("phones")) {
                            for (int phoneIndex = 0; phoneIndex < data.getJSONArray("phones").
                                    length(); phoneIndex++) {
                                data.getJSONArray("phones").getJSONObject(phoneIndex).
                                        put("value", mMaskData);
                            }
                        }

                        if (data.has("indicators")) {
                            data.put("indicators", mMaskData);
                        }
                    }
                }
            }
            maskedJsonArray = certifications;
        } catch (JSONException ignored) {
        }
    }

    public void checkStoragePermission() {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= android.os.Build.VERSION_CODES.M) {
            if (!AppPermissionUtils.isPermissionGiven(K_STORAGE_PERMISSION, this)) {
                AppPermissionUtils.requestPermission(this, K_STORAGE_PERMISSION_REQUEST_CODE,
                        K_STORAGE_PERMISSION);
            } else {
                shareMaskedResponse();
            }
        } else {
            shareMaskedResponse();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (AppPermissionUtils.isGrantedPermission(this, requestCode, grantResults,
                K_STORAGE_PERMISSION)) {
            shareMaskedResponse();
        } else {
            ErrorDialog errorDialog = new ErrorDialog(this);
            errorDialog.show(null, "",
                    getString(R.string.label_storage_permission_alert), dialog -> {
                        errorDialog.dismiss();
                        finish();
                    });
        }
    }

    private void shareMaskedResponse() {
        String maskedRespStr = maskedJsonArray.toString();

        if (!getFilesDir().exists()) {
            getFilesDir().mkdir();
        }
        String filePath = getFilesDir() + File.separator + "masked_resp.json";

        try {
            FileOutputStream fos = new FileOutputStream(filePath);
            DataOutputStream outStream = new DataOutputStream(new BufferedOutputStream(fos));
            outStream.writeBytes(maskedRespStr);
            outStream.close();
            File file = new File(filePath);
            Uri uri = FileProvider.getUriForFile(VerifySSNActivity.this,
                    "com.onekosmos.blockidsample.provider", file);
            String[] to = {"ssn-crowd-test@1kosmos.com"};

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/json");
            intent.putExtra(Intent.EXTRA_EMAIL, to);
            intent.putExtra(Intent.EXTRA_SUBJECT, "Masked JSON Response");
            intent.putExtra(Intent.EXTRA_TEXT, "Masked JSON Response");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            Intent finalIntent = Intent.createChooser(intent, "choose an email application");
            startActivity(finalIntent);
        } catch (IOException ignored) {
        }
    }
}