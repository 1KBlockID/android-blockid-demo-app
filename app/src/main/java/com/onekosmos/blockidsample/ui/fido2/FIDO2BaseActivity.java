package com.onekosmos.blockidsample.ui.fido2;

import static com.onekosmos.blockidsample.util.SharedPreferenceUtil.K_PREF_FIDO2_USERNAME;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;

import com.google.android.material.textfield.TextInputEditText;
import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.datamodel.BIDTenant;
import com.onekosmos.blockid.sdk.fido2.FIDO2KeyType;
import com.onekosmos.blockid.sdk.fido2.FIDO2Observer;
import com.onekosmos.blockidsample.AppConstant;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.ProgressDialog;
import com.onekosmos.blockidsample.util.ResultDialog;
import com.onekosmos.blockidsample.util.SharedPreferenceUtil;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2022 1Kosmos. All rights reserved.
 */
public class FIDO2BaseActivity extends AppCompatActivity implements View.OnClickListener {
    private AppCompatImageView mImgBack;
    // html file to show UI/UX as per app design
    private final String K_FILE_NAME = "fido3.html";
    private AppCompatButton mBtnRegister, mBtnAuthenticate, mBtnRegisterPlatformAuthenticator,
            mBtnRegisterExternalAuthenticator;
    private TextInputEditText mEtUserName;
    private boolean mBtnRegisterClicked, mBtnAuthenticateClicked;
    private ProgressDialog mProgressDialog;

    // FIDO2Observer must initialize before onCreate()
    private final FIDO2Observer observer = new FIDO2Observer(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fido2_base);

        mImgBack = findViewById(R.id.img_back);
        mImgBack.setOnClickListener(v -> onBackPressed());

        String storedUserName = SharedPreferenceUtil.getInstance().getString(K_PREF_FIDO2_USERNAME);
        mEtUserName = findViewById(R.id.edt_user_name);
        if (!TextUtils.isEmpty(storedUserName)) {
            mEtUserName.setText(storedUserName);
        }

        mBtnRegister = findViewById(R.id.btn_register);
        mBtnAuthenticate = findViewById(R.id.btn_authenticate);
        mProgressDialog = new ProgressDialog(this, getString(R.string.label_please_wait));

        mBtnRegister.setOnClickListener(v -> {
            if (!mBtnRegisterClicked) {
                if (validateUserName(mEtUserName.getText().toString())) {
                    mProgressDialog.show();
                    mBtnRegisterClicked = true;
                    BIDTenant tenant = AppConstant.defaultTenant;
                    BlockIDSDK.getInstance().registerFIDO2Key(this,
                            mEtUserName.getText().toString(),
                            tenant.getDns(),
                            tenant.getCommunity(),
                            K_FILE_NAME,
                            (status, errorResponse) -> {
                                mProgressDialog.dismiss();
                                mBtnRegisterClicked = false;
                                if (!status) {
                                    showError(errorResponse);
                                } else {
                                    SharedPreferenceUtil.getInstance().setString(
                                            K_PREF_FIDO2_USERNAME, mEtUserName.getText().toString());
                                    showResultDialog(R.drawable.icon_dialog_success,
                                            getString(R.string.label_fido2_key_has_been_successfully_registered));
                                }
                            });
                }
            }
        });

        mBtnAuthenticate.setOnClickListener(v -> {
            if (!mBtnAuthenticateClicked) {
                if (validateUserName(mEtUserName.getText().toString())) {
                    mProgressDialog.show();
                    mBtnAuthenticateClicked = true;
                    BIDTenant tenant = AppConstant.defaultTenant;
                    BlockIDSDK.getInstance().authenticateFIDO2Key(this,
                            mEtUserName.getText().toString(),
                            tenant.getDns(),
                            tenant.getCommunity(),
                            K_FILE_NAME,
                            (status, errorResponse) -> {
                                mProgressDialog.dismiss();
                                mBtnAuthenticateClicked = false;
                                if (!status) {
                                    showError(errorResponse);
                                } else {
                                    SharedPreferenceUtil.getInstance().setString(
                                            K_PREF_FIDO2_USERNAME, mEtUserName.getText().toString());
                                    showResultDialog(R.drawable.icon_dialog_success,
                                            getString(R.string.label_successfully_authenticated_with_your_fido2_key));
                                }
                            });
                }
            }
        });

        mBtnRegisterPlatformAuthenticator = findViewById(
                R.id.btn_register_platform_authenticator);
        mBtnRegisterExternalAuthenticator = findViewById(
                R.id.btn_register_external_authenticator);

        mBtnRegisterExternalAuthenticator.setOnClickListener(this);
        mBtnRegisterPlatformAuthenticator.setOnClickListener(this);
    }

    /**
     * Check userName is empty or not
     *
     * @return userName is empty then return false and show toast else true
     */
    private boolean validateUserName(String userName) {
        userName = userName.trim();
        hideKeyboard();
        if (TextUtils.isEmpty(userName)) {
            Toast.makeText(this,
                    R.string.label_enter_username,
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void showError(ErrorManager.ErrorResponse error) {
        ErrorDialog errorDialog = new ErrorDialog(this);
        DialogInterface.OnDismissListener onDismissListener = dialogInterface -> {
            errorDialog.dismiss();
        };
        if (error.getCode() == ErrorManager.CustomErrors.K_CONNECTION_ERROR.getCode()) {
            errorDialog.showNoInternetDialog(onDismissListener);
            return;
        }
        errorDialog.show(null, getString(R.string.label_error),
                error.getMessage() + " (" + error.getCode() + ")",
                onDismissListener);
    }

    /**
     * Hide keyboard when user click on button
     */
    private void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) this.
                getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null && inputMethodManager.isAcceptingText())
            inputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().
                    getWindowToken(), 0);
    }

    private void showResultDialog(int imageId, String subMessage) {
        ResultDialog dialog = new ResultDialog(this, imageId,
                SharedPreferenceUtil.getInstance().getString(K_PREF_FIDO2_USERNAME), subMessage);
        dialog.show();
        new Handler().postDelayed(() -> dialog.dismiss(), 2000);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_register_platform_authenticator:
                mProgressDialog.show();
                BlockIDSDK.getInstance().registerFIDO2Key(this,
                        mEtUserName.getText().toString(),
                        AppConstant.defaultTenant,
                        FIDO2KeyType.PLATFORM,
                        null,
                        (status, errorResponse) -> {
                            mProgressDialog.dismiss();
                            if (!status) {
                                showError(errorResponse);
                            } else {
                                SharedPreferenceUtil.getInstance().setString(
                                        K_PREF_FIDO2_USERNAME, mEtUserName.getText().toString());
                                showResultDialog(R.drawable.icon_dialog_success,
                                        getString(R.string.label_fido2_key_has_been_successfully_registered));
                            }
                        });
                break;

            case R.id.btn_register_external_authenticator:
                mProgressDialog.show();
                BlockIDSDK.getInstance().registerFIDO2Key(this,
                        mEtUserName.getText().toString(),
                        AppConstant.defaultTenant,
                        FIDO2KeyType.CROSS_PLATFORM,
                        observer,
                        (status, errorResponse) -> {
                            mProgressDialog.dismiss();
                            if (!status) {
                                showError(errorResponse);
                            } else {
                                SharedPreferenceUtil.getInstance().setString(
                                        K_PREF_FIDO2_USERNAME, mEtUserName.getText().toString());
                                showResultDialog(R.drawable.icon_dialog_success,
                                        getString(R.string.label_fido2_key_has_been_successfully_registered));
                            }
                        });
                break;
        }
    }
}