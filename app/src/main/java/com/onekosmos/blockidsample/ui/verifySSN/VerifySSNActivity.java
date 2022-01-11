package com.onekosmos.blockidsample.ui.verifySSN;

import static com.onekosmos.blockid.sdk.document.BIDDocumentProvider.RegisterDocCategory.identity_document;
import static com.onekosmos.blockid.sdk.document.BIDDocumentProvider.RegisterDocCategory.misc_document;
import static com.onekosmos.blockid.sdk.document.RegisterDocType.DL;
import static com.onekosmos.blockid.sdk.document.RegisterDocType.SSN;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.document.BIDDocumentProvider;
import com.onekosmos.blockidsample.AppConstant;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.ProgressDialog;
import com.onekosmos.blockidsample.util.SharedPreferenceUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2022 1Kosmos. All rights reserved.
 */
public class VerifySSNActivity extends AppCompatActivity {

    private ImageView mBackBtn;
    private EditText mSSN, mFirstName, mMiddleName, mLastName, mBirthDate, mStreet, mCity, mState,
            mZipCode, mPhone, mEmail, mCountry;
    private CheckBox mConsentCB;
    private Button mContinueBtn;
    private LinkedHashMap<String, Object> mSSNMap = new LinkedHashMap<String, Object>();
    final Calendar mCalendar = Calendar.getInstance();
    private String requiredDateFormat = "yyyy/MM/dd";
    private String displayDateFormat = "MM-dd-yyyy";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_ssn);

        mBackBtn = findViewById(R.id.bckBtn);
        mSSN = findViewById(R.id.ssn_text);
        mFirstName = findViewById(R.id.firstname_text);
        mMiddleName = findViewById(R.id.middlename_text);
        mLastName = findViewById(R.id.lastname_text);
        mBirthDate = findViewById(R.id.bdate_text);
        mStreet = findViewById(R.id.address_text);
        mCity = findViewById(R.id.city_text);
        mState = findViewById(R.id.state_text);
        mZipCode = findViewById(R.id.zip_text);
        mCountry = findViewById(R.id.country);
        mPhone = findViewById(R.id.phone_text);
        mEmail = findViewById(R.id.email_text);
        mConsentCB = findViewById(R.id.consent_cb);
        mContinueBtn = findViewById(R.id.btn_continue);

        populateDLData();

        DatePickerDialog.OnDateSetListener date = (view, year, month, day) -> {
            mCalendar.set(Calendar.YEAR, year);
            mCalendar.set(Calendar.MONTH, month);
            mCalendar.set(Calendar.DAY_OF_MONTH, day);
            SimpleDateFormat dateFormat = new SimpleDateFormat(displayDateFormat, Locale.US);
            mBirthDate.setText(dateFormat.format(mCalendar.getTime()));
        };
        mBirthDate.setOnClickListener(view -> {
            DatePickerDialog dpDialog = new DatePickerDialog(VerifySSNActivity.this, date, mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH));
            dpDialog.getDatePicker().setMaxDate(new Date().getTime());
            dpDialog.show();
        });

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
    }

    private void populateDLData() {
        if (BlockIDSDK.getInstance().isDriversLicenseEnrolled()) {
            String dlArrayList = BIDDocumentProvider.getInstance().getUserDocument("", DL.getValue(), identity_document.name());
            try {
                JSONArray dlDoc = new JSONArray(dlArrayList);
                if (dlDoc != null && dlDoc.length() >= 0) {
                    if (dlDoc.getJSONObject(0).has("firstName")) {
                        mFirstName.setText(dlDoc.getJSONObject(0).getString("firstName"));
                    }

                    if (dlDoc.getJSONObject(0).has("middleName")) {
                        mMiddleName.setText(dlDoc.getJSONObject(0).getString("middleName"));
                    }

                    if (dlDoc.getJSONObject(0).has("lastName")) {
                        mLastName.setText(dlDoc.getJSONObject(0).getString("lastName"));
                    }

                    if (dlDoc.getJSONObject(0).has("dob")) {
                        mBirthDate.setText(changeDateFormat(dlDoc.getJSONObject(0).getString("dob"), "yyyymmdd", displayDateFormat));
                    }

                    if (dlDoc.getJSONObject(0).has("street")) {
                        mStreet.setText(dlDoc.getJSONObject(0).getString("street"));
                    }

                    if (dlDoc.getJSONObject(0).has("city")) {
                        mCity.setText(dlDoc.getJSONObject(0).getString("city"));
                    }

                    if (dlDoc.getJSONObject(0).has("state")) {
                        mState.setText(dlDoc.getJSONObject(0).getString("state"));
                    }

                    if (dlDoc.getJSONObject(0).has("zipCode")) {
                        mZipCode.setText(dlDoc.getJSONObject(0).getString("zipCode"));
                    }

                    if (dlDoc.getJSONObject(0).has("country")) {
                        mCountry.setText(dlDoc.getJSONObject(0).getString("country"));
                    }
                }
            } catch (JSONException e) {
                return;
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void validateAndVerifySSN() {
        if (TextUtils.isEmpty(mSSN.getText().toString().trim())) {
            Toast.makeText(this, "Enter SSN", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(mFirstName.getText().toString().trim())) {
            Toast.makeText(this, "Enter First Name", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(mLastName.getText().toString().trim())) {
            Toast.makeText(this, "Enter Last Name", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(mBirthDate.getText().toString().trim())) {
            Toast.makeText(this, "Enter Birth Date", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(mStreet.getText().toString().trim())) {
            Toast.makeText(this, "Enter Address", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(mCity.getText().toString().trim())) {
            Toast.makeText(this, "Enter City", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(mState.getText().toString().trim())) {
            Toast.makeText(this, "Enter State", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(mZipCode.getText().toString().trim())) {
            Toast.makeText(this, "Enter Zip Code", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(mCountry.getText().toString().trim())) {
            Toast.makeText(this, "Enter Country", Toast.LENGTH_SHORT).show();
        } else {
            verifySSN();
        }
    }

    public String changeDateFormat(String time, String inputFormatDate, String outputFormatDate) {
        String inputPattern = inputFormatDate;
        String outputPattern = outputFormatDate;
        SimpleDateFormat inputFormat = new SimpleDateFormat(inputPattern);
        SimpleDateFormat outputFormat = new SimpleDateFormat(outputPattern);

        Date date = null;

        try {
            date = inputFormat.parse(time);
            return outputFormat.format(date);
        } catch (ParseException e) {
            return null;
        }
    }

    private void verifySSN() {
        ProgressDialog progressDialog = new ProgressDialog(this, getString(
                R.string.label_verifying_ssn));
        progressDialog.show();

        mSSNMap.put("id", mSSN.getText().toString().trim());
        mSSNMap.put("type", SSN.getValue());
        mSSNMap.put("category", misc_document.name());
        mSSNMap.put("userConsent", mConsentCB.isChecked());
        mSSNMap.put("ssn", mSSN.getText().toString().trim());

        mSSNMap.put("firstName", mFirstName.getText().toString().trim());
        String middleName = mMiddleName.getText().toString().trim();
        if (!TextUtils.isEmpty(middleName)) {
            mSSNMap.put("middleName", middleName);
        }
        mSSNMap.put("lastName", mLastName.getText().toString().trim());
        mSSNMap.put("street", mStreet.getText().toString().trim());
        mSSNMap.put("city", mCity.getText().toString().trim());
        mSSNMap.put("state", mState.getText().toString().trim());
        mSSNMap.put("zipCode", mZipCode.getText().toString().trim());
        mSSNMap.put("country", mCountry.getText().toString().trim());
        mSSNMap.put("dob", changeDateFormat(mBirthDate.getText().toString().trim(), displayDateFormat, requiredDateFormat));
        String email = mEmail.getText().toString().trim();
        if (!TextUtils.isEmpty(email)) {
            mSSNMap.put("email", email);
        }
        String phone = mPhone.getText().toString().trim();
        if (!TextUtils.isEmpty(phone)) {
            mSSNMap.put("phone", phone);
        }

        BlockIDSDK.getInstance().verifyDocument(AppConstant.dvcID, mSSNMap, (status, documentVerification, error) -> {
            if (status) {
                progressDialog.dismiss();
                try {
                    JSONObject jsonObject = new JSONObject(documentVerification);
                    JSONArray certificates = jsonObject.getJSONArray("certifications");

                    boolean isVerified = false;

                    for (int index = 0; index < certificates.length(); index++) {
                        if (certificates.getJSONObject(index).has("status") && certificates.getJSONObject(index).getInt("status") == 400) {
                            handleInvalidData();
                            break;
                        }

                        if (certificates.getJSONObject(index).has("verified") && certificates.getJSONObject(index).getBoolean("verified")) {
                            isVerified = true;
                        } else {
                            isVerified = false;
                            SharedPreferenceUtil.getInstance().setBool(SharedPreferenceUtil.PREFS_KEY_IS_SSN_VERIFIED, false);
                            handleFailedSSNVerification();
                            break;
                        }
                    }

                    if (isVerified) {
                        SharedPreferenceUtil.getInstance().setBool(SharedPreferenceUtil.PREFS_KEY_IS_SSN_VERIFIED, true);
                        handleSuccessSSNVerification();
                    }
                } catch (JSONException e) {
                    return;
                }
            } else {
                progressDialog.dismiss();
                SharedPreferenceUtil.getInstance().setBool(SharedPreferenceUtil.PREFS_KEY_IS_SSN_VERIFIED, false);
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
        });
    }

    private void handleFailedSSNVerification() {
        ErrorDialog errorDialog = new ErrorDialog(VerifySSNActivity.this);

        errorDialog.showWithOneButton(null,
                getString(R.string.label_error),
                getString(R.string.ssn_verification_failed_no_match),
                getString(R.string.label_retry),
                dialog -> {
                    errorDialog.dismiss();
                });
    }

    private void handleSuccessSSNVerification() {
        ErrorDialog errorDialog = new ErrorDialog(VerifySSNActivity.this);

        errorDialog.showWithOneButton(null,
                getString(R.string.label_success),
                getString(R.string.ssn_verification_success),
                getString(R.string.label_ok),
                dialog -> {
                    errorDialog.dismiss();
                    finish();
                });
    }

    private void handleInvalidData() {
        ErrorDialog errorDialog = new ErrorDialog(VerifySSNActivity.this);
        DialogInterface.OnDismissListener onDismissListener = dialogInterface -> {
            errorDialog.dismiss();
        };

        errorDialog.showWithOneButton(null,
                getString(R.string.label_error),
                getString(R.string.bad_data),
                getString(R.string.label_retry),
                dialog -> {
                    errorDialog.dismiss();
                });
    }
}
