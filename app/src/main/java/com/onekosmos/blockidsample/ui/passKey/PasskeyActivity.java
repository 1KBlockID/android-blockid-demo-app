package com.onekosmos.blockidsample.ui.passKey;

import static android.view.View.VISIBLE;
import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.Passkey.ALREADY_REGISTERED_PASS_KEY;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Keep;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
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
    private AppCompatEditText mEdittextUsername, mEdittextPasskeyName;
    private AppCompatTextView mTxtGeneratedJwtTitle, mTxtGeneratedJwtValue;
    private AppCompatButton mBtnRegisterPasskey, mBtnAuthenticateViaPasskey,
            mBtnRegisterPasskeyAndLink, mBtnAuthenticateViaPasskeyAndGetJWT,
            mBtnCopyJwt;
    private ProgressDialog mProgressDialog;

    /**
     * @noinspection DataFlowIssue
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

        // System back press
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        finish(); // close the activity
                    }
                });

        mEdittextUsername = findViewById(R.id.edt_username);
        mEdittextPasskeyName = findViewById(R.id.edt_passkey_name);

        mTxtGeneratedJwtTitle = findViewById(R.id.txt_generated_jwt_title);
        mTxtGeneratedJwtValue = findViewById(R.id.txt_generated_jwt_value);

        mBtnRegisterPasskey = findViewById(R.id.btn_register_passkey);
        mBtnAuthenticateViaPasskey = findViewById(R.id.btn_authenticate_via_passkey);
        mBtnRegisterPasskeyAndLink = findViewById(R.id.btn_register_passkey_and_link);
        mBtnAuthenticateViaPasskeyAndGetJWT = findViewById(
                R.id.btn_authenticate_via_passkey_and_get_jwt);
        mBtnCopyJwt = findViewById(R.id.btn_copy_jwt);

        AppCompatImageView mImgBack = findViewById(R.id.img_back_passkey);
        mImgBack.setOnClickListener(view -> finish());

        mBtnRegisterPasskey.setEnabled(false);
        mBtnAuthenticateViaPasskey.setEnabled(false);
        mBtnRegisterPasskeyAndLink.setEnabled(false);
        mBtnAuthenticateViaPasskeyAndGetJWT.setEnabled(false);

        mEdittextUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // not used
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String username = s.toString().trim();
                boolean isValid = username.length() >= 3;

                mBtnRegisterPasskey.setEnabled(isValid);
                mBtnAuthenticateViaPasskey.setEnabled(isValid);
                mBtnRegisterPasskeyAndLink.setEnabled(isValid);
                mBtnAuthenticateViaPasskeyAndGetJWT.setEnabled(isValid);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // not used
            }
        });

        mBtnRegisterPasskey.setOnClickListener(v -> {
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
                                    // dguid is an Unique ID for user
                                    (AppConstant.defaultTenant, responseUser.data.dguid,
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

        mBtnAuthenticateViaPasskey.setOnClickListener(v -> {
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
                                    // dguid is an Unique ID for user
                                    (AppConstant.defaultTenant, responseUser.data.dguid,
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

        mBtnRegisterPasskeyAndLink.setOnClickListener(view -> {
            String passkeyName = mEdittextPasskeyName.getText().toString();
            // FIXME need to implement
        });

        mBtnAuthenticateViaPasskeyAndGetJWT.setOnClickListener(view ->
                issueJWTOnPasskeyAuthentication());

        mBtnCopyJwt.setOnClickListener(view -> {
            String jwt = mTxtGeneratedJwtValue.getText().toString();
            copyToClipboard(PasskeyActivity.this, jwt);
        });
    }

    private void issueJWTOnPasskeyAuthentication() {
        showProgressDialog(getString(R.string.label_authenticating_passkey));

        String username = mEdittextUsername.getText().toString();

        PasskeyRequest passkeyRequest = new PasskeyRequest(
                AppConstant.defaultTenant, // Required BIDTenant (tenantTag, community, dns)
                username); // Required username

        BlockIDSDK.getInstance().issueJWTOnPasskeyAuthentication(PasskeyActivity.this,
                passkeyRequest, (status, response, error) -> {
                    mProgressDialog.dismiss();
                    if (!status) {
                        showErrorDialog(error, username, PassKeyAction.AUTHENTICATION);
                        return;
                    }

                    mTxtGeneratedJwtTitle.setVisibility(VISIBLE);
                    mTxtGeneratedJwtValue.setVisibility(VISIBLE);
                    mTxtGeneratedJwtValue.setText(response.jwt);
                    mBtnCopyJwt.setVisibility(VISIBLE);
                    showSuccessDialog(response, PassKeyAction.AUTHENTICATION);
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

    /**
     * Show error dialog
     *
     * @param errorResponse {@link ErrorManager.ErrorResponse}
     * @param userName      String username
     * @param passKeyAction {@link PassKeyAction} to be shown on error dialog message
     */
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
            errorDialog.showWithOneButton(null,
                    getString(R.string.label_no_account_found),
                    getString(R.string.label_we_could_not_find_any_account_with, userName),
                    getString(R.string.label_ok),
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

    /**
     * @param context Activity context
     * @param text    Content to be copy
     */
    private void copyToClipboard(Context context, String text) {
        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(
                    CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("msg", text);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(context, getResources().getString(R.string.label_jwt_copied),
                    Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {
        }
    }

    @Keep
    public enum PassKeyAction {
        REGISTER,
        AUTHENTICATION
    }

    /**
     * @noinspection unused
     */
    @Keep
    public static class FetchUserResponse {
        public UserData data;
        public String publicKey;
    }

    /**
     * @noinspection unused
     */
    @Keep
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
        public String dguid;
    }
}