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
import android.widget.TextView;
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
    private TextView mBackText;
    private CheckBox mConsentCB;
    private Button mContinueBtn;
    private LinkedHashMap<String, Object> mSSNMap = new LinkedHashMap<String, Object>();
    final Calendar mCalendar = Calendar.getInstance();
    private String requiredDateFormat = "yyyy/MM/dd";
    private String displayDateFormat = "MM-dd-yyyy";
    private String maskData = "XXXXXX";

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
        mBackText = findViewById(R.id.txt_back);

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

        mBackText.setOnClickListener(v -> onBackPressed());
    }

    private void populateDLData() {
        if (BlockIDSDK.getInstance().isDriversLicenseEnrolled()) {
            String dlArrayList = BIDDocumentProvider.getInstance().getUserDocument("", DL.getValue(), identity_document.name());
            try {
                JSONArray dlDoc = new JSONArray(dlArrayList);
                if (dlDoc != null && dlDoc.length() >= 0) {

                    JSONObject dlObject = dlDoc.getJSONObject(0);

                    if (dlObject.has("firstName")) {
                        mFirstName.setText(dlObject.getString("firstName"));
                    }

                    if (dlObject.has("middleName")) {
                        mMiddleName.setText(dlObject.getString("middleName"));
                    }

                    if (dlObject.has("lastName")) {
                        mLastName.setText(dlObject.getString("lastName"));
                    }

                    if (dlObject.has("dob")) {
                        mBirthDate.setText(changeDateFormat(dlObject.getString("dob"), "yyyymmdd", displayDateFormat));
                    }

                    if (dlObject.has("street")) {
                        mStreet.setText(dlObject.getString("street"));
                    }

                    if (dlObject.has("city")) {
                        mCity.setText(dlObject.getString("city"));
                    }

                    if (dlObject.has("state")) {
                        mState.setText(dlObject.getString("state"));
                    }

                    if (dlObject.has("zipCode")) {
                        mZipCode.setText(dlObject.getString("zipCode"));
                    }

                    if (dlObject.has("country")) {
                        mCountry.setText(dlObject.getString("country"));
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
                            handleFailedSSNVerification(jsonObject);
                            break;
                        }
                    }

                    if (isVerified) {
                        handleSuccessSSNVerification();
                    }
                } catch (JSONException e) {
                    return;
                }
            } else {
                progressDialog.dismiss();
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

    private void handleFailedSSNVerification(JSONObject responseObject) {

        try {
            JSONArray certifications = responseObject.getJSONArray("certifications");
            for (int index = 0; index < certifications.length(); index++) {
                if (certifications.getJSONObject(index).has("metadata") &&
                        certifications.getJSONObject(index).getJSONObject("metadata").
                                has("verifiedPeople")) {
                    JSONArray verifiedPeopleArray = certifications.getJSONObject(index).
                            getJSONObject("metadata").getJSONArray("verifiedPeople");
                    for (int verifiedPeopleIndex = 0; verifiedPeopleIndex < verifiedPeopleArray.length(); verifiedPeopleIndex++) {
                        JSONObject data = verifiedPeopleArray.getJSONObject(verifiedPeopleIndex);

                        if (data.has("firstName") && data.getJSONObject("firstName").has("value")) {
                            data.getJSONObject("firstName").put("value", maskData);
                        }

                        if (data.has("middleName") && data.getJSONObject("middleName").has("value")) {
                            data.getJSONObject("middleName").put("value", maskData);
                        }

                        if (data.has("lastName") && data.getJSONObject("lastName").has("value")) {
                            data.getJSONObject("lastName").put("value", maskData);
                        }

                        if (data.has("ssn") && data.getJSONObject("ssn").has("value")) {
                            data.getJSONObject("ssn").put("value", maskData);
                        }

                        if (data.has("dateOfBirth")) {
                            if (data.getJSONObject("dateOfBirth").has("month")) {
                                data.getJSONObject("dateOfBirth").getJSONObject("month").put("value", maskData);
                            }

                            if (data.getJSONObject("dateOfBirth").has("day")) {
                                data.getJSONObject("dateOfBirth").getJSONObject("day").put("value", maskData);
                            }

                            if (data.getJSONObject("dateOfBirth").has("year")) {
                                data.getJSONObject("dateOfBirth").getJSONObject("year").put("value", maskData);
                            }
                        }

                        if (data.has("age") && data.getJSONObject("age").has("value")) {
                            data.getJSONObject("age").put("value", maskData);
                        }

                        if (data.has("addresses")) {
                            for (int addIndex = 0; addIndex < data.getJSONArray("addresses").length(); addIndex++) {
                                data.getJSONArray("addresses").getJSONObject(addIndex).put("value", maskData);
                            }
                        }

                        if (data.has("emails")) {
                            for (int emailIndex = 0; emailIndex < data.getJSONArray("emails").length(); emailIndex++) {
                                data.getJSONArray("emails").getJSONObject(emailIndex).put("value", maskData);
                            }
                        }

                        if (data.has("phones")) {
                            for (int phoneIndex = 0; phoneIndex < data.getJSONArray("phones").length(); phoneIndex++) {
                                data.getJSONArray("phones").getJSONObject(phoneIndex).put("value", maskData);
                            }
                        }

                        if (data.has("indicators")) {
                            data.put("indicators", maskData);
                        }
                    }
                }
            }
            JSONArray newCertifications = certifications;
        } catch (JSONException e) {
            return;
        }

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

        errorDialog.showWithOneButton(null,
                getString(R.string.label_error),
                getString(R.string.bad_data),
                getString(R.string.label_retry),
                dialog -> {
                    errorDialog.dismiss();
                });
    }
}
