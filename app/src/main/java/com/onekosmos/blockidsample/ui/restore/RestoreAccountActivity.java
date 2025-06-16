package com.onekosmos.blockidsample.ui.restore;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.view.WindowCompat;

import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockidsample.AppConstant;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.ProgressDialog;
import com.onekosmos.blockidsample.util.ResetSDKMessages;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class RestoreAccountActivity extends AppCompatActivity {

    private AppCompatImageView mImgBack;
    private AppCompatTextView mTxtBack;
    private AppCompatButton mBtnRestore;
    private AppCompatEditText[] mEtPhrases = new AppCompatEditText[12];
    private TextWatcher[] mTextWatchers = new TextWatcher[12];
    private ProgressDialog mProgressDialog;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 🔒 Lock the orientation to portrait
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 15+
            WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        }

        setContentView(R.layout.activity_restore_account);
        initView();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(RESULT_CANCELED);
        finish();
    }

    private void initView() {
        mImgBack = findViewById(R.id.img_back);
        mImgBack.setOnClickListener(view -> onBackPressed());

        mTxtBack = findViewById(R.id.txt_back);
        mTxtBack.setOnClickListener(view -> onBackPressed());

        mBtnRestore = findViewById(R.id.btn_restore);
        mBtnRestore.setOnClickListener(view -> onClickRestore());

        mProgressDialog = new ProgressDialog(this);
        for (int index = 0; index < mEtPhrases.length; index++) {
            String number;
            String layoutName;
            if (index < 9) {
                number = "0" + (index + 1);
            } else {
                number = "" + (index + 1);
            }
            layoutName = "phrase_" + number;
            int resID = getResources().getIdentifier(layoutName, "id", getPackageName());
            mEtPhrases[index] = findViewById(resID).findViewById(R.id.edt_phrase);
            mEtPhrases[index].setHint(number);

            mTextWatchers[index] = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence sequence, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence sequence, int start, int before, int count) {
                    analyseTextViews(sequence.toString().substring(start, start + count));
                }

                @Override
                public void afterTextChanged(Editable editable) {
                }
            };
            mEtPhrases[index].addTextChangedListener(mTextWatchers[index]);
        }
    }

    private void analyseTextViews(String argText) {
        String separator = " ";
        if (argText.contains(separator)) {
            String[] arrPhrases = argText.split(separator);
            if (arrPhrases.length == mEtPhrases.length) {
                pastePhrases(arrPhrases);
            }
        }
    }

    private void pastePhrases(String[] argPhrases) {
        for (int index = 0; index < mEtPhrases.length; index++) {
            removeAllTextWatchers();
        }
        for (int index = 0; index < mEtPhrases.length; index++) {
            mEtPhrases[index].setText(argPhrases[index].trim());
        }
        addAllTextWatchers();
    }

    private void addAllTextWatchers() {
        for (int index = 0; index < mEtPhrases.length; index++) {
            mEtPhrases[index].addTextChangedListener(mTextWatchers[index]);
        }
    }

    private void removeAllTextWatchers() {
        for (int index = 0; index < mEtPhrases.length; index++) {
            mEtPhrases[index].removeTextChangedListener(mTextWatchers[index]);
        }
    }

    private List<String> getMnemonicPhrasesList() {
        List<String> listPhrases = new ArrayList<>();
        for (AppCompatEditText mTextView : mEtPhrases) {
            listPhrases.add(mTextView.getText().toString().trim());
        }
        return listPhrases;
    }

    private void onClickRestore() {
        boolean isAnyFieldEmpty = false;
        for (AppCompatEditText mTextView : mEtPhrases) {
            String text = mTextView.getText().toString();
            if (TextUtils.isEmpty(text)) {
                isAnyFieldEmpty = true;
                break;
            }
        }

        if (isAnyFieldEmpty) {
            Toast.makeText(this, R.string.label_enter_12_phrase,
                    Toast.LENGTH_SHORT).show();
        } else {
            boolean isWalletCreated = BlockIDSDK.getInstance().restoreWallet(getMnemonicPhrasesList());
            if (isWalletCreated) {
                mProgressDialog.show();
                mBtnRestore.setEnabled(false);
                BlockIDSDK.getInstance().setRestoreMode();
                registerTenant();
            } else {
                Toast.makeText(this, R.string.label_security_phrase_error,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void registerTenant() {
        BlockIDSDK.getInstance().registerTenant(AppConstant.defaultTenant,
                (status, error, bidTenant) -> {
                    if (status) {
                        restoreAccount();
                        return;
                    }
                    showErrorDialog(error.getMessage(),
                            ResetSDKMessages.TENANT_REGISTRATION_FAILED_DURING_RESTORATION.
                                    getMessage(error.getCode(), error.getMessage()));
                });
    }

    private void restoreAccount() {
        BlockIDSDK.getInstance().restoreUserDataFromWallet((status, error, message) -> {
            if (status) {
                mProgressDialog.dismiss();
                BlockIDSDK.getInstance().commitRestorationData();
                setResult(RESULT_OK);
                finish();
                return;
            }
            BlockIDSDK.getInstance().resetRestorationData();
            if (error.getCode() == ErrorManager.CustomErrors.K_CONNECTION_ERROR.getCode()) {
                showErrorDialog(error.getMessage(), ResetSDKMessages.
                        FETCH_WALLET_FAILED_DURING_RESTORATION.getMessage(
                                error.getCode(), error.getMessage()));
                return;
            }
            showErrorDialog(getString(R.string.label_account_restoration_failed), ResetSDKMessages.
                    FETCH_WALLET_FAILED_DURING_RESTORATION.getMessage(
                            error.getCode(), error.getMessage()));
        });
    }

    private void showErrorDialog(String argMessage, String reason) {
        mBtnRestore.setEnabled(true);
        BlockIDSDK.getInstance().resetSDK(AppConstant.licenseKey, AppConstant.defaultTenant,
                reason);
        mProgressDialog.dismiss();
        ErrorDialog errorDialog = new ErrorDialog(this);
        DialogInterface.OnDismissListener onDismissListener = dialogInterface -> {
            errorDialog.dismiss();
            setResult(RESULT_CANCELED);
            finish();
        };
        errorDialog.show(null, getString(R.string.label_error), argMessage,
                onDismissListener);
    }
}