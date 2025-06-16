package com.onekosmos.blockidsample.ui.userManagement;

import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_CONNECTION_ERROR;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.view.WindowCompat;

import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.datamodel.BIDGenericResponse;
import com.onekosmos.blockid.sdk.datamodel.BIDLinkedAccount;
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

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 🔒 Lock the orientation to portrait
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 15+
            WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        }

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

        AppCompatButton btnRemoveAccount = findViewById(R.id.btn_remove_account);
        btnRemoveAccount.setOnClickListener(view -> removeAccount());
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
        BlockIDSDK.getInstance().unlinkAccount(linkedAccount, null, null,
                (status, error) -> {
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