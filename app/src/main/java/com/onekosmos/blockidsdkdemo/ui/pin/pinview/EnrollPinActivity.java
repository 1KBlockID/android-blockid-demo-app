package com.onekosmos.blockidsdkdemo.ui.pin.pinview;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.blockid.sdk.BlockIDSDK;
import com.example.blockidsdkdemo.R;
import com.onekosmos.blockidsdkdemo.util.ErrorDialog;

import java.util.Objects;

public class EnrollPinActivity extends AppCompatActivity {
    private static final int K_PIN_DIGIT_COUNT = 8;
    public static final String K_BID_TENANT = "K_BID_TENANT";
    public static final String K_CODE = "code";
    private AppCompatTextView  mTxtEnterPin, mTxtBack, mTxtPinError, mTxtPlsWait;
    private AppCompatImageView mImgShowHidePsw, mImgBack;
    private PinView mPvEnterPin;
    private boolean mShowPivValue;
    private String mEnteredPin;
    private String mConfirmPin;
    private TextWatcher textWatcherEnterPin;
    private boolean isEnteredPin = false;
    private ProgressBar mProgressBar;
    public static String K_FLOW = "K_FLOW";
    private WORKFLOW flow;

    public enum WORKFLOW {
        ENROLL_FRAG
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enroll_pin);
        if (getIntent().hasExtra(K_FLOW))
            flow = (WORKFLOW) getIntent().getExtras().get(K_FLOW);
        initView();
    }

    @Override
    public void onBackPressed() {
        if (!onBackPress()) {
            super.onBackPressed();
            if (flow != null && flow.equals(WORKFLOW.ENROLL_FRAG)) {
                finish();
                return;
            }
            setResult(RESULT_CANCELED);
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
        mProgressBar = findViewById(R.id.progress_bar);
        mTxtPlsWait = findViewById(R.id.txt_please_wait);

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
//        mPvEnterPin.setCursorColor(Color.parseColor(Brand.getInstance().getColor().getPrimary3()));
//        mPvEnterPin.setTextColor(Color.parseColor(Brand.getInstance().getColor().getPrimary3()));
//        mPvEnterPin.setLineColor(Color.parseColor(Brand.getInstance().getColor().getSecondary3()));
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
//        mPvEnterPin.setCursorColor(Color.parseColor(Brand.getInstance().getColor().getPrimary2()));
//        mPvEnterPin.setTextColor(Color.parseColor(Brand.getInstance().getColor().getPrimary2()));
//        mPvEnterPin.setLineColor(Color.parseColor(Brand.getInstance().getColor().getPrimary2()));
        mConfirmPin = null;
        mTxtPinError.setVisibility(View.VISIBLE);
    }

    private void setPin() {
        mProgressBar.setVisibility(View.VISIBLE);
        mTxtPlsWait.setVisibility(View.VISIBLE);
        hideKeyboard(this);

        BlockIDSDK.getInstance().enrollPin(mConfirmPin, (enroll_status, error) -> {
            mProgressBar.setVisibility(View.GONE);
            mTxtPlsWait.setVisibility(View.GONE);
            if (enroll_status) {
                afterSuccess();
                if (flow != null && flow.equals(WORKFLOW.ENROLL_FRAG)) {
                    Toast.makeText(this, R.string.label_pin_enrollment_success, Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                setResult(RESULT_OK);
                finish();
            } else {
                ErrorDialog errorDialog = new ErrorDialog(this);
                String message = error.getMessage();
                if (message == null) {
                    message = getString(R.string.label_pin_registration_failed);
                }
                errorDialog.show(null,
                        getString(R.string.label_error),
                        message, dialog -> {
                            errorDialog.dismiss();
                        });
            }
        });
    }

    public void hideKeyboard(Activity activity) {
        try {
            InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (inputMethodManager.isActive())
                inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        } catch (Exception e) {
            e.getMessage();
        }
    }
}