package com.onekosmos.blockidsample.ui.userManagement;

import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_CONNECTION_ERROR;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.datamodel.BIDGenericResponse;
import com.onekosmos.blockid.sdk.datamodel.BIDLinkedAccount;
import com.onekosmos.blockid.sdk.fido2.FIDO2KeyType;
import com.onekosmos.blockid.sdk.fido2.FIDO2Observer;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.ProgressDialog;

import java.util.List;
import java.util.Objects;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class UserOptionsActivity extends AppCompatActivity {
    // FIDO2Observer must initialize before onCreate()
    private final FIDO2Observer observer = new FIDO2Observer(this);

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
                view -> registerExternalAuthenticator());

        AppCompatButton btnAuthenticatePlatformAuthenticator = findViewById(
                R.id.btn_authenticate_platform_authenticator);
        btnAuthenticatePlatformAuthenticator.setOnClickListener(
                view -> authenticatePlatformAuthenticator());

        AppCompatButton btnAuthenticateExternalAuthenticator = findViewById(
                R.id.btn_authenticate_external_authenticator);
        btnAuthenticateExternalAuthenticator.setOnClickListener(
                view -> authenticateExternalAuthenticator());

        AppCompatButton btnRemoveAccount = findViewById(R.id.btn_remove_account);
        btnRemoveAccount.setOnClickListener(view -> removeAccount());

    }

    /**
     * Register FIDO2 platform authenticator
     */
    private void registerPlatformAuthenticator() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();

        BIDGenericResponse response = BlockIDSDK.getInstance().getLinkedUserList();
        List<BIDLinkedAccount> linkedAccounts = response.getDataObject();

        BlockIDSDK.getInstance().registerFIDO2Key(this, linkedAccounts.get(0),
                FIDO2KeyType.PLATFORM, null, (status, error) -> {
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
    private void registerExternalAuthenticator() {
        BIDGenericResponse response = BlockIDSDK.getInstance().getLinkedUserList();
        List<BIDLinkedAccount> linkedAccounts = response.getDataObject();

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();

        // FIDO2Observer must initialize before onCreate()
        // FIDO2Observer must not null
        BlockIDSDK.getInstance().registerFIDO2Key(this, linkedAccounts.get(0),
                FIDO2KeyType.CROSS_PLATFORM, observer, (status, error) -> {
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

    }

    private void authenticateExternalAuthenticator() {

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