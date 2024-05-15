package com.onekosmos.blockidsample.ui.userManagement;

import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_CONNECTION_ERROR;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.datamodel.BIDGenericResponse;
import com.onekosmos.blockid.sdk.datamodel.BIDLinkedAccount;
import com.onekosmos.blockid.sdk.fido2.FIDO2KeyType;
import com.onekosmos.blockidsample.BuildConfig;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.ui.qrAuth.AuthenticationPayloadV1;
import com.onekosmos.blockidsample.ui.qrAuth.ScanQRCodeActivity;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.ProgressDialog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class UserOptionsActivity extends AppCompatActivity {
    private static final String K_WEBAUTHN_CHALLENGE = "webauthn_challenge";
    private FIDO2KeyType mAuthenticationFido2KeyType;
    private boolean isPinRequired;

    private final ActivityResultLauncher<Intent> scanQRActivityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            AuthenticationPayloadV1 authenticationPayload = new Gson().fromJson(
                                    result.getData().getStringExtra("K_AUTH_REQUEST_MODEL"),
                                    AuthenticationPayloadV1.class);
                            if (isPinRequired)
                                showPINInputDialog(authenticationPayload,
                                        Fido2Operation.AUTHENTICATE);
                            else
                                authenticate(authenticationPayload, null, "none");
                        }
                    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_options);
        initView();
    }

    /**
     * Initialize UI Objects
     */
    @SuppressLint("SetTextI18n")
    private void initView() {
        AppCompatImageView mImgBack = findViewById(R.id.img_back_user);
        mImgBack.setOnClickListener(view -> onBackPressed());

        BIDLinkedAccount linkedAccount = null;
        BIDGenericResponse response = BlockIDSDK.getInstance().getLinkedUserList();
        if (response.getStatus()) {
            List<BIDLinkedAccount> mLinkedAccountsList = response.getDataObject();
            if (mLinkedAccountsList != null && mLinkedAccountsList.size() > 0) {
                linkedAccount = mLinkedAccountsList.get(0);
            }
        }

        AppCompatTextView mTxtUser = findViewById(R.id.txt_title_user);
        if (linkedAccount != null)
            mTxtUser.setText(getString(R.string.label_welcome) + " " + linkedAccount.getUserId());

        AppCompatButton btnRegisterPlatformAuthenticator = findViewById(
                R.id.btn_register_platform_authenticator);
        btnRegisterPlatformAuthenticator.setOnClickListener(
                view -> registerPlatformAuthenticator());

        AppCompatButton btnRegisterExternalAuthenticator = findViewById(
                R.id.btn_register_external_authenticator);
        btnRegisterExternalAuthenticator.setOnClickListener(
                view -> registerExternalAuthenticator(null));

        AppCompatButton btnAuthenticatePlatformAuthenticator = findViewById(
                R.id.btn_authenticate_platform_authenticator);
        btnAuthenticatePlatformAuthenticator.setOnClickListener(
                view -> authenticatePlatformAuthenticator());

        AppCompatButton btnAuthenticateExternalAuthenticator = findViewById(
                R.id.btn_authenticate_external_authenticator);
        btnAuthenticateExternalAuthenticator.setOnClickListener(
                view -> {
                    isPinRequired = false;
                    authenticateExternalAuthenticator();
                });

        AppCompatButton btnRegisterExternalAuthenticatorWithPin = findViewById(
                R.id.btn_register_external_authenticator_with_pin);
        btnRegisterExternalAuthenticatorWithPin.setOnClickListener(
                view -> showPINInputDialog(null, Fido2Operation.REGISTER));

        AppCompatButton btnAuthenticateExternalAuthenticatorWithPin = findViewById(
                R.id.btn_authenticate_external_authenticator_with_pin);
        btnAuthenticateExternalAuthenticatorWithPin.setOnClickListener(
                view -> {
                    isPinRequired = true;
                    authenticateExternalAuthenticator();
                });

        AppCompatButton btnRemoveAccount = findViewById(R.id.btn_remove_account);
        btnRemoveAccount.setOnClickListener(view -> removeAccount());
    }

    private void showPINInputDialog(AuthenticationPayloadV1 authenticationPayload,
                                    Fido2Operation operation) {
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
                registerExternalAuthenticator(securityPin);
            else
                authenticate(authenticationPayload, securityPin, "none");

            dialog.dismiss();
        });
    }

    private enum Fido2Operation {
        REGISTER,
        AUTHENTICATE
    }

    /**
     * Register FIDO2 platform authenticator
     */
    private void registerPlatformAuthenticator() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();

        BIDGenericResponse response = BlockIDSDK.getInstance().getLinkedUserList();
        List<BIDLinkedAccount> linkedAccounts = response.getDataObject();

        // For platform authenticator security pin will be empty string
        BlockIDSDK.getInstance().registerFIDO2Key(this, linkedAccounts.get(0),
                FIDO2KeyType.PLATFORM, "", (status, error) -> {
                    progressDialog.dismiss();
                    if (status) {
                        Toast.makeText(this, getString(
                                        R.string.label_register_platform_authenticator_successful),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    showError(error);
                });
    }

    /**
     * Register FIDO2 external authenticator
     */
    private void registerExternalAuthenticator(String securityKeyPin) {
        BIDGenericResponse response = BlockIDSDK.getInstance().getLinkedUserList();
        List<BIDLinkedAccount> linkedAccounts = response.getDataObject();

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();

        BlockIDSDK.getInstance().registerFIDO2Key(this, linkedAccounts.get(0),
                FIDO2KeyType.CROSS_PLATFORM, securityKeyPin, (status, error) -> {
                    progressDialog.dismiss();
                    if (status) {
                        Toast.makeText(this, getString(
                                        R.string.label_register_external_authenticator_successful),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    showError(error);
                });
    }

    private void authenticatePlatformAuthenticator() {
        mAuthenticationFido2KeyType = FIDO2KeyType.PLATFORM;
        startScanQRActivity();
    }

    private void authenticateExternalAuthenticator() {
        mAuthenticationFido2KeyType = FIDO2KeyType.CROSS_PLATFORM;
        startScanQRActivity();
    }

    /**
     * Start Scan QR code activity for result
     */
    private void startScanQRActivity() {
        Intent scanQRCodeIntent = new Intent(this, ScanQRCodeActivity.class);
        scanQRCodeIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        scanQRActivityResultLauncher.launch(scanQRCodeIntent);
    }

    private void authenticate(AuthenticationPayloadV1 payload, @Nullable String securityKeyPin,
                              String authType) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();
        LinkedHashMap<String, Object> metadata = null;
        if (payload.metadata != null && payload.metadata.webauthn_challenge != null) {
            metadata = new LinkedHashMap<>();
            metadata.put(K_WEBAUTHN_CHALLENGE, payload.metadata.webauthn_challenge);
        }
        BlockIDSDK.getInstance().authenticateFIDO2Key(this, mAuthenticationFido2KeyType,
                securityKeyPin, null, payload.session, payload.sessionURL, payload.scopes,
                metadata, payload.creds, payload.getOrigin(), null, null,
                BuildConfig.VERSION_NAME, authType, (status, error) -> {
                    progressDialog.dismiss();
                    if (status) {
                        Toast.makeText(this,
                                R.string.label_you_have_successfully_authenticated_to_log_in,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    showError(error);
                });
    }

    /**
     * Button click remove account
     */
    private void removeAccount() {
        BIDGenericResponse response = BlockIDSDK.getInstance().getLinkedUserList();
        List<BIDLinkedAccount> linkedAccounts = response.getDataObject();

        ErrorDialog errorDialog = new ErrorDialog(this);
        errorDialog.showWithTwoButton(null, null, getString(R.string.label_remove_user),
                getString(R.string.label_yes), getString(R.string.label_no),
                (dialogInterface, which) -> errorDialog.dismiss(),
                dialog -> {
                    errorDialog.dismiss();
                    unlinkAccount(linkedAccounts.get(0));
                });
    }

    /**
     * Unlink account
     *
     * @param linkedAccount {@link BIDLinkedAccount}
     */
    private void unlinkAccount(BIDLinkedAccount linkedAccount) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();
        BlockIDSDK.getInstance().unlinkAccount(linkedAccount, null, (status, error) -> {
            progressDialog.dismiss();
            if (status) {
                Toast.makeText(this, getString(R.string.label_account_removed),
                        Toast.LENGTH_SHORT).show();
                BlockIDSDK.getInstance().setSelectedAccount(null);
                finish();
                return;
            }
            showError(error);
        });
    }

    /**
     * Show error dialog
     *
     * @param error {@link ErrorManager.ErrorResponse}
     */
    private void showError(ErrorManager.ErrorResponse error) {
        ErrorDialog errorDialog = new ErrorDialog(this);
        DialogInterface.OnDismissListener onDismissListener = dialogInterface ->
                errorDialog.dismiss();
        if (error != null && error.getCode() == K_CONNECTION_ERROR.getCode()) {
            errorDialog.showNoInternetDialog(onDismissListener);
            return;
        }
        errorDialog.showWithOneButton(null, getString(R.string.label_error),
                Objects.requireNonNull(error).getMessage() + " (" + error.getCode() + ")",
                getString(R.string.label_ok),
                onDismissListener);
    }
}