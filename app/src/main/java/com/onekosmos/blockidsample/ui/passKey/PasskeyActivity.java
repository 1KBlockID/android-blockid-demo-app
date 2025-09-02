package com.onekosmos.blockidsample.ui.passKey;

import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.Passkey.ALREADY_REGISTERED_PASS_KEY;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.passKey.PasskeyRequest;
import com.onekosmos.blockid.sdk.passKey.PasskeyResponse;
import com.onekosmos.blockid.sdk.utils.BIDUtil;
import com.onekosmos.blockidsample.AppConstant;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.ProgressDialog;
import com.onekosmos.blockidsample.util.SuccessDialog;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2025 1Kosmos. All rights reserved.
 */
public class PasskeyActivity extends AppCompatActivity {
    private AppCompatEditText mEdittextUsername;
    private AppCompatButton mBtnRegister, mBtnAuthenticate;
    private ProgressDialog mProgressDialog;

    /**
     * @noinspection DataFlowIssue, deprecation
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_passkey);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mEdittextUsername = findViewById(R.id.edt_username);
        mBtnRegister = findViewById(R.id.btn_register);
        mBtnAuthenticate = findViewById(R.id.btn_authenticate);

        AppCompatImageView mImgBack = findViewById(R.id.img_back_passkey);
        mImgBack.setOnClickListener(view -> onBackPressed());

        mBtnRegister.setEnabled(false);
        mBtnAuthenticate.setEnabled(false);

        mEdittextUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // not used
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String username = s.toString().trim();
                boolean isValid = username.length() >= 3;

                mBtnRegister.setEnabled(isValid);
                mBtnAuthenticate.setEnabled(isValid);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // not used
            }
        });


        mBtnRegister.setOnClickListener(v -> {
            String username = mEdittextUsername.getText().toString();
            showProgressDialog(getString(R.string.label_registering_passkey));
            BlockIDSDK.getInstance().fetchUserByUserName(this, AppConstant.defaultTenant,
                    username, (status, fetchUserResponse, errorResponse) -> {
                        mProgressDialog.dismiss();
                        if (status) {
                            FetchUserResponse responseUser =
                                    BIDUtil.JSONStringToObject(fetchUserResponse,
                                            FetchUserResponse.class);
                            PasskeyRequest passkeyRequest = new PasskeyRequest
                                    //uid is an Unique ID for user
                                    (AppConstant.defaultTenant, responseUser.data.uid,
                                            responseUser.data.username);
                            BlockIDSDK.getInstance().registerPasskey(PasskeyActivity.this,
                                    passkeyRequest, (statusKey, response, error) -> {
                                        if (statusKey) {
                                            showSuccessDialog(response, PassKeyAction.REGISTER);
                                        } else {
                                            showErrorDialog(error, username,
                                                    PassKeyAction.REGISTER);
                                        }
                                    });
                        } else {
                            showErrorDialog(errorResponse, username, PassKeyAction.REGISTER);
                        }
                    });
        });

        mBtnAuthenticate.setOnClickListener(v -> {
            String username = mEdittextUsername.getText().toString();
            showProgressDialog(getString(R.string.label_authenticating_passkey));
            BlockIDSDK.getInstance().fetchUserByUserName(this, AppConstant.defaultTenant,
                    username, (status, fetchUserResponse, errorResponse) -> {
                        mProgressDialog.dismiss();
                        if (status) {
                            FetchUserResponse responseUser =
                                    BIDUtil.JSONStringToObject(fetchUserResponse,
                                            FetchUserResponse.class);
                            PasskeyRequest passkeyRequest = new PasskeyRequest
                                    (AppConstant.defaultTenant, responseUser.data.uid,
                                            responseUser.data.username);
                            BlockIDSDK.getInstance().authenticatePasskey(
                                    PasskeyActivity.this,
                                    passkeyRequest, (statusKey, response, error) -> {
                                        if (statusKey) {
                                            showSuccessDialog(response,
                                                    PassKeyAction.AUTHENTICATION);
                                        } else {
                                            showErrorDialog(error, username,
                                                    PassKeyAction.AUTHENTICATION);
                                        }
                                    });
                        } else {
                            showErrorDialog(errorResponse, username, PassKeyAction.AUTHENTICATION);
                        }
                    });
        });
    }

    /**
     * Show progress dialog
     *
     * @param message to be shown on progress dialog
     */
    private void showProgressDialog(String message) {
        mProgressDialog = new ProgressDialog(this, message);
        mProgressDialog.show();
    }

    /**
     * Show success dialog
     *
     * @param response      {@link PasskeyResponse}
     * @param passKeyAction {@link PassKeyAction} to be shown on success dialog message
     */
    private void showSuccessDialog(PasskeyResponse response, PassKeyAction passKeyAction) {
        SuccessDialog successDialog = new SuccessDialog(this);
        String message;

        if (passKeyAction.equals(PassKeyAction.REGISTER)) {
            message = "Passkey registration successful for " + response.sub +
                    "\n Authenticator ID : " + response.authenticatorId;
        } else {
            message = "Passkey verification successful for " + response.sub +
                    "\n Authenticator ID : " + response.authenticatorId;
        }

        successDialog.show(null, getString(R.string.label_success), message,
                dialog -> successDialog.dismiss());
    }

    private void showErrorDialog(ErrorManager.ErrorResponse errorResponse, String userName,
                                 PassKeyAction passKeyAction) {
        ErrorDialog errorDialog = new ErrorDialog(this);
        DialogInterface.OnDismissListener onDismissListener = dialogInterface ->
                errorDialog.dismiss();

        if (errorResponse.getCode() == ErrorManager.CustomErrors.K_CONNECTION_ERROR.getCode()) {
            errorDialog.showNoInternetDialog(onDismissListener);
            return;
        }

        if (errorResponse.getCode() == 404) {
            errorDialog.show(null,
                    getString(R.string.label_error),
                    getString(R.string.label_user_not_found),
                    dialog -> errorDialog.dismiss());
            return;
        }

        if (errorResponse.getCode() == ALREADY_REGISTERED_PASS_KEY.getCode())
            errorDialog.show(null,
                    getString(R.string.label_error),
                    errorResponse.getMessage() +
                            "\n(" + getString(R.string.label_error_code)
                            + errorResponse.getCode() + ")",
                    dialog -> errorDialog.dismiss());

        String title;
        String message;
        if (passKeyAction.equals(PassKeyAction.REGISTER)) {
            title = getString(R.string.label_passkey_registration_failed);
            message = "We couldn’t register passkey with " + userName +
                    ". Please try again.";
        } else {
            title = getString(R.string.label_passkey_verification_failed);
            message = "We couldn’t verify passkey with " + userName +
                    ". Please try again.";
        }

        errorDialog.show(null, title, message,
                dialog -> errorDialog.dismiss());
    }

    public enum PassKeyAction {
        REGISTER,
        AUTHENTICATION
    }

    public static class FetchUserResponse {
        public UserData data;
        public String publicKey;
    }

    /**
     * @noinspection unused
     */
    public static class UserData {
        public String username;
        public String status;
        public String roleValue;
        public String moduleId;
        public String email;
        public String firstname;
        public String lastname;
        public String phone;
        public String uid;
    }
}