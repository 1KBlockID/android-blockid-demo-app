package com.onekosmos.blockidsample.ui.fido2;

import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_CONNECTION_ERROR;
import static com.onekosmos.blockidsample.util.SharedPreferenceUtil.K_PREF_FIDO2_USERNAME;
import static org.apache.commons.codec.language.bm.Rule.ALL;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;

import com.google.android.material.textfield.TextInputEditText;
import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.datamodel.BIDTenant;
import com.onekosmos.blockid.sdk.fido2.FIDO2KeyType;
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

@SuppressWarnings(ALL)
public class FIDO2BaseActivity extends AppCompatActivity {
    private AppCompatImageView mImgBack;
    // html file to show UI/UX as per app design
    private final String K_FILE_NAME = "fido3.html";
    private AppCompatButton mBtnRegister, mBtnAuthenticate, mBtnRegisterPlatformAuthenticator,
            mBtnRegisterExternalAuthenticator, mBtnAuthenticatePlatformAuthenticator,
            mBtnAuthenticateExternalAuthenticator, mBtnRegisterExternalAuthenticatorWithPin,
            mBtnAuthenticateExternalAuthenticatorWithPin;
    private TextInputEditText mEtUserName;
    private boolean mBtnRegisterClicked, mBtnAuthenticateClicked;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fido2_base);

        mImgBack = findViewById(R.id.img_back);
        mImgBack.setOnClickListener(view -> onBackPressed());

        String storedUserName = SharedPreferenceUtil.getInstance().getString(K_PREF_FIDO2_USERNAME);
        mEtUserName = findViewById(R.id.edt_user_name);
        if (!TextUtils.isEmpty(storedUserName)) {
            mEtUserName.setText(storedUserName);
        }

        mBtnRegister = findViewById(R.id.btn_register_web);
        mBtnAuthenticate = findViewById(R.id.btn_authenticate_web);
        mProgressDialog = new ProgressDialog(this, getString(R.string.label_please_wait));

        mBtnRegister.setOnClickListener(v -> {
            if (!mBtnRegisterClicked) {
                if (validateUserName(mEtUserName.getText().toString())) {
                    mProgressDialog.show();
                    mBtnRegisterClicked = true;
                    BIDTenant tenant = AppConstant.clientTenant;
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
                    BIDTenant tenant = AppConstant.clientTenant;
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
        mBtnRegisterPlatformAuthenticator.setOnClickListener(
                view -> registerFIDO2(FIDO2KeyType.PLATFORM, null));

        mBtnRegisterExternalAuthenticator = findViewById(
                R.id.btn_register_external_authenticator);
        mBtnRegisterExternalAuthenticator.setOnClickListener(
                view -> registerFIDO2(FIDO2KeyType.CROSS_PLATFORM, null));

        mBtnAuthenticatePlatformAuthenticator = findViewById(
                R.id.btn_authenticate_platform_authenticator);
        mBtnAuthenticatePlatformAuthenticator.setOnClickListener(
                view -> authenticateFIDO2(FIDO2KeyType.PLATFORM, null));

        mBtnAuthenticateExternalAuthenticator = findViewById(
                R.id.btn_authenticate_external_authenticator);
        mBtnAuthenticateExternalAuthenticator.setOnClickListener(
                view -> authenticateFIDO2(FIDO2KeyType.CROSS_PLATFORM, null));

        mBtnRegisterExternalAuthenticatorWithPin = findViewById(
                R.id.btn_register_external_authenticator_with_pin);
        mBtnRegisterExternalAuthenticatorWithPin.setOnClickListener(
                view -> showPINInputDialog(Fido2Operation.REGISTER));

        mBtnAuthenticateExternalAuthenticatorWithPin = findViewById(
                R.id.btn_authenticate_external_authenticator_with_pin);
        mBtnAuthenticateExternalAuthenticatorWithPin.setOnClickListener(
                view -> showPINInputDialog(Fido2Operation.AUTHENTICATE));
    }

    /**
     * Show Enter Security PIN Dialog
     *
     * @param operation {@link Fido2Operation}
     */
    private void showPINInputDialog(Fido2Operation operation) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_pin_input);

        TextInputEditText edtEnterPin = dialog.findViewById(R.id.edt_enter_pin);
        AppCompatButton btnCancel = dialog.findViewById(R.id.btn_dialog_pin_input_cancel);
        btnCancel.setOnClickListener(view -> dialog.dismiss());
        dialog.show();

        AppCompatButton btnVerify = dialog.findViewById(R.id.btn_dialog_pin_input_verify);
        btnVerify.setOnClickListener(view -> {
            String securityPin = edtEnterPin.getEditableText().toString();
            if (operation.equals(Fido2Operation.REGISTER))
                registerFIDO2(FIDO2KeyType.CROSS_PLATFORM, securityPin);
            else
                authenticateFIDO2(FIDO2KeyType.CROSS_PLATFORM, securityPin);

            dialog.dismiss();
        });
    }

    private enum Fido2Operation {
        REGISTER,
        AUTHENTICATE
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
        DialogInterface.OnDismissListener onDismissListener = dialogInterface ->
                errorDialog.dismiss();
        if (error.getMessage().equalsIgnoreCase(K_CONNECTION_ERROR.getMessage())) {
            errorDialog.showNoInternetDialog(onDismissListener);
            return;
        }
        errorDialog.showWithOneButton(null, getString(R.string.label_error),
                error.getMessage() + " (" + error.getCode() + ").",
                getString(R.string.label_ok),
                onDismissListener);
    }

    /**
     * Hide keyboard when user click on button
     */
    private void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) this.
                getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = FIDO2BaseActivity.this.getCurrentFocus();
        if (view == null) {
            return;
        }

        if (inputMethodManager != null && inputMethodManager.isAcceptingText()) {
            IBinder binder = view.getWindowToken();
            if (binder != null)
                inputMethodManager.hideSoftInputFromWindow(binder, 0);
        }
    }

    private void showResultDialog(int imageId, String subMessage) {
        ResultDialog dialog = new ResultDialog(this, imageId,
                SharedPreferenceUtil.getInstance().getString(K_PREF_FIDO2_USERNAME), subMessage);
        dialog.show();
        new Handler().postDelayed(dialog::dismiss, 2000);
    }

    /**
     * Register FIDO2 key
     */
    private void registerFIDO2(FIDO2KeyType keyType, @Nullable String securityKeyPin) {
        if (!validateUserName(mEtUserName.getText().toString())) {
            return;
        }

        mProgressDialog.show();
        BlockIDSDK.getInstance().registerFIDO2Key(this,
                mEtUserName.getText().toString(),
                AppConstant.clientTenant.getDns(),
                AppConstant.clientTenant.getCommunity(),
                keyType,
                securityKeyPin,
                (status, errorResponse) -> {
                    mProgressDialog.dismiss();
                    if (!status) {
                        showError(errorResponse);
                        return;
                    }
                    SharedPreferenceUtil.getInstance().setString(
                            K_PREF_FIDO2_USERNAME, mEtUserName.getText().toString());

                    String message = keyType.getValue().equalsIgnoreCase(
                            FIDO2KeyType.PLATFORM.getValue()) ?
                            getString(R.string.label_platform_key_registered) :
                            getString(R.string.label_security_key_registered);
                    showResultDialog(R.drawable.icon_dialog_success, message);
                });
    }

    /**
     * Authenticate FIDO2 key
     */
    private void authenticateFIDO2(FIDO2KeyType keyType, @Nullable String securityKeyPin) {
        if (!validateUserName(mEtUserName.getText().toString())) {
            return;
        }

        mProgressDialog.show();

        BlockIDSDK.getInstance().authenticateFIDO2Key(this,
                mEtUserName.getText().toString(),
                AppConstant.clientTenant.getDns(),
                AppConstant.clientTenant.getCommunity(),
                keyType,
                securityKeyPin,
                (status, errorResponse) -> {
                    mProgressDialog.dismiss();
                    if (!status) {
                        runOnUiThread(() -> showError(errorResponse));
                        return;
                    }

                    SharedPreferenceUtil.getInstance().setString(
                            K_PREF_FIDO2_USERNAME, mEtUserName.getText().toString());
                    String message = keyType.getValue().equalsIgnoreCase(
                            FIDO2KeyType.PLATFORM.getValue()) ?
                            getString(R.string.label_platform_key_authenticated) :
                            getString(R.string.label_security_key_authenticated);

                    showResultDialog(R.drawable.icon_dialog_success, message);
                });
    }

    // FIXME TBD
    @Override
    protected void onResume() {
        super.onResume();
        if (mProgressDialog.isShowing())
            mProgressDialog.dismiss();
    }
}