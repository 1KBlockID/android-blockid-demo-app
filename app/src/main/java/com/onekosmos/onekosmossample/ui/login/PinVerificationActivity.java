package com.onekosmos.onekosmossample.ui.login;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.onekosmossample.R;
import com.onekosmos.onekosmossample.ui.enrollPin.PinView;

import java.util.Objects;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class PinVerificationActivity extends AppCompatActivity {
    private static final int K_PIN_DIGIT_COUNT = 8;
    private AppCompatTextView mTxtPinError;
    private AppCompatImageView mImgShowHidePsw;
    private PinView mPvEnterPin;
    private boolean mShowPivValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_verification);
        initView();
    }

    @Override
    public void onPause() {
        super.onPause();
        closeKeyboard();
    }

    private void initView() {
        mTxtPinError = findViewById(R.id.txt_pin_error);
        AppCompatTextView mTxtBack = findViewById(R.id.txt_back);
        mImgShowHidePsw = findViewById(R.id.img_show_hide_psw);
        AppCompatImageView mImgBack = findViewById(R.id.img_back);
        mPvEnterPin = findViewById(R.id.edt_enter_pin);
        setPinViewColor();
        mTxtBack.setOnClickListener(view -> onCancel());
        mImgBack.setOnClickListener(view -> onCancel());
        mImgShowHidePsw.setOnClickListener(view -> showHidePassWord());

        mPvEnterPin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count,
                                          int after) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                setPinViewColor();
                mTxtPinError.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mPvEnterPin.length() == K_PIN_DIGIT_COUNT)
                    verifyPin(Objects.requireNonNull(mPvEnterPin.getText()).toString());
            }
        });
    }

    private void onCancel() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void verifyPin(String pin) {
        if (TextUtils.isEmpty(pin) || pin.length() < K_PIN_DIGIT_COUNT ||
                !BlockIDSDK.getInstance().verifyPin(pin)) {
            inCorrectPin();
            return;
        }
        setResult(RESULT_OK);
        finish();
    }

    private void setPinViewColor() {
        mPvEnterPin.setCursorColor(getColor(R.color.black));
        mPvEnterPin.setTextColor(getColor(R.color.black));
        mPvEnterPin.setLineColor(getColor(android.R.color.darker_gray));
    }

    private void showHidePassWord() {
        if (!mShowPivValue) {
            mPvEnterPin.setInputType(InputType.TYPE_CLASS_NUMBER);
            mImgShowHidePsw.setImageResource(R.drawable.icon_active);
            mShowPivValue = true;
        } else {
            mPvEnterPin.setInputType(InputType.TYPE_CLASS_NUMBER |
                    InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            mImgShowHidePsw.setImageResource(R.drawable.icon_deactive);
            mShowPivValue = false;
        }
    }

    private void inCorrectPin() {
        mPvEnterPin.setCursorColor(getColor(R.color.misc2));
        mPvEnterPin.setTextColor(getColor(R.color.misc2));
        mPvEnterPin.setLineColor(getColor(R.color.misc2));
        mTxtPinError.setVisibility(View.VISIBLE);
    }

    private void closeKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getApplicationContext().
                getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(mPvEnterPin.getWindowToken(), 0);
    }
}