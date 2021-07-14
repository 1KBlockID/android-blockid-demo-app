package com.onekosmos.blockidsample.ui.enrollPin;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.ProgressDialog;

import java.util.Objects;

import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_SOMETHING_WENT_WRONG;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class PinEnrollmentActivity extends AppCompatActivity {
    private static final int K_PIN_DIGIT_COUNT = 8;
    private AppCompatTextView mTxtEnterPin, mTxtBack, mTxtPinError;
    private AppCompatImageView mImgShowHidePsw, mImgBack;
    private PinView mPvEnterPin;
    private boolean mShowPivValue;
    private String mEnteredPin;
    private String mConfirmPin;
    private TextWatcher textWatcherEnterPin;
    private boolean isEnteredPin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_enrollment);
        initView();
    }

    @Override
    public void onBackPressed() {
        if (!onBackPress()) {
            super.onBackPressed();
        }
    }

    private void initView() {
        mTxtEnterPin = findViewById(R.id.txt_enter_pin);
        mTxtPinError = findViewById(R.id.txt_pin_error);
        mTxtBack = findViewById(R.id.txt_back);
        mImgShowHidePsw = findViewById(R.id.img_show_hide_psw);
        mImgBack = findViewById(R.id.img_back);
        mPvEnterPin = findViewById(R.id.edt_enter_pin);

        setPinViewColor();
        mTxtBack.setOnClickListener(v -> onBackPress());
        mImgBack.setOnClickListener(v -> onBackPress());
        mImgShowHidePsw.setOnClickListener(v -> showHidePassWord());

        textWatcherEnterPin = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setPinViewColor();
                mTxtPinError.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == K_PIN_DIGIT_COUNT && !isEnteredPin) {
                    firstPinEntered(s.toString());
                    mPvEnterPin.setText("");
                } else {
                    confirmPin(s.toString());
                }
            }
        };

        mPvEnterPin.addTextChangedListener(textWatcherEnterPin);
        mPvEnterPin.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                return firstPinEntered(v.getText().toString());
            }
            return false;
        });
    }

    private boolean firstPinEntered(String pin) {
        if (TextUtils.isEmpty(pin) || pin.length() < K_PIN_DIGIT_COUNT) {
            return false;
        }
        mPvEnterPin.removeTextChangedListener(textWatcherEnterPin);
        mEnteredPin = pin;
        mImgBack.setVisibility(View.VISIBLE);
        mTxtBack.setVisibility(View.VISIBLE);
        mTxtEnterPin.setText(R.string.label_confirm_your_pin);
        mPvEnterPin.setText("");
        isEnteredPin = true;
        mPvEnterPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        mImgShowHidePsw.setImageResource(R.drawable.icon_deactive);
        mShowPivValue = false;
        mPvEnterPin.addTextChangedListener(textWatcherEnterPin);
        return true;
    }

    private boolean confirmPin(String pin) {
        if (TextUtils.isEmpty(pin) || pin.length() < K_PIN_DIGIT_COUNT || !isEnteredPin) {
            return false;
        }
        mConfirmPin = pin;
        if (mConfirmPin.equals(mEnteredPin)) {
            setPin();
            return false;
        } else {
            inCorrectPin();
            return true;
        }
    }

    private void setPinViewColor() {
        mPvEnterPin.setCursorColor(getColor(R.color.black));
        mPvEnterPin.setTextColor(getColor(R.color.black));
        mPvEnterPin.setLineColor(getColor(android.R.color.darker_gray));
    }

    private boolean onBackPress() {
        if (!TextUtils.isEmpty(mEnteredPin)) {
            mPvEnterPin.removeTextChangedListener(textWatcherEnterPin);
            setPinViewColor();
            mPvEnterPin.setText(mEnteredPin);
            mTxtPinError.setVisibility(View.GONE);
            mTxtEnterPin.setText(R.string.label_enter_pin);
            mImgBack.setVisibility(View.INVISIBLE);
            mEnteredPin = null;
            mTxtBack.setVisibility(View.INVISIBLE);
            mShowPivValue = false;
            mPvEnterPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            mImgShowHidePsw.setImageResource(R.drawable.icon_deactive);
            mPvEnterPin.addTextChangedListener(textWatcherEnterPin);
            isEnteredPin = false;
            return true;
        }
        return false;
    }

    private void showHidePassWord() {
        if (!isEnteredPin)
            mPvEnterPin.removeTextChangedListener(textWatcherEnterPin);
        if (!mShowPivValue) {
            mPvEnterPin.setInputType(InputType.TYPE_CLASS_NUMBER);
            mImgShowHidePsw.setImageResource(R.drawable.icon_active);
            mShowPivValue = true;
        } else {
            mPvEnterPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            mImgShowHidePsw.setImageResource(R.drawable.icon_deactive);
            mShowPivValue = false;
        }
        if (!isEnteredPin)
            mPvEnterPin.addTextChangedListener(textWatcherEnterPin);
    }

    private void afterSuccess() {
        mTxtPinError.setVisibility(View.GONE);
        InputMethodManager inputMethodManager = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(Objects.requireNonNull(this.getCurrentFocus()).getWindowToken(), 0);
    }

    private void inCorrectPin() {
        mPvEnterPin.setCursorColor(getColor(R.color.misc2));
        mPvEnterPin.setTextColor(getColor(R.color.misc2));
        mPvEnterPin.setLineColor(getColor(R.color.misc2));
        mConfirmPin = null;
        mTxtPinError.setVisibility(View.VISIBLE);
    }

    private void setPin() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();
        hideKeyboard(this);

        // proofed by can be empty string or null. sdk by default set it as "blockid"
        BlockIDSDK.getInstance().enrollPin(mConfirmPin, null, (enroll_status, error) -> {
            progressDialog.dismiss();
            if (enroll_status) {
                afterSuccess();
                Toast.makeText(this, R.string.label_pin_enrollment_success, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                if (error == null)
                    error = new ErrorManager.ErrorResponse(K_SOMETHING_WENT_WRONG.getCode(), getString(R.string.label_pin_registration_failed));

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

    private void hideKeyboard(Activity activity) {
        try {
            InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (inputMethodManager.isActive())
                inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        } catch (Exception e) {
            e.getMessage();
        }
    }
}