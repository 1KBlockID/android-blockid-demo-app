package com.onekosmos.blockidsample.ui.fido2;

import static com.onekosmos.blockidsample.util.SharedPreferenceUtil.K_PREF_FIDO2_USERNAME;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.datamodel.BIDTenant;
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
public class FIDO2Fragment extends Fragment {
    private final String K_FIDO_FILE_NAME = "fido3_legacy_vault.html";
    private AppCompatButton mBtnRegister, mBtnAuthenticate;
    private TextInputEditText mEtUserName;
    private boolean mBtnRegisterClicked, mBtnAuthenticateClicked;
    private ProgressDialog mProgressDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_fido2,
                container,
                false);

        String storedUserName = SharedPreferenceUtil.getInstance().getString(K_PREF_FIDO2_USERNAME);
        mEtUserName = view.findViewById(R.id.edt_user_name);
        if (!TextUtils.isEmpty(storedUserName)) {
            mEtUserName.setText(storedUserName);
        }

        mBtnRegister = view.findViewById(R.id.btn_register);
        mBtnAuthenticate = view.findViewById(R.id.btn_authenticate);
        mProgressDialog = new ProgressDialog(getActivity(), getString(R.string.label_please_wait));

        mBtnRegister.setOnClickListener(v -> {
            if (!mBtnRegisterClicked) {
                if (validateUserName(mEtUserName.getText().toString())) {
                    mProgressDialog.show();
                    mBtnRegisterClicked = true;
                    BIDTenant tenant = AppConstant.defaultTenant;
                    BlockIDSDK.getInstance().registerFIDO2Key(getActivity(),
                            mEtUserName.getText().toString(),
                            tenant.getDns(),
                            tenant.getCommunity(),
                            "",
                            (status, errorResponse) -> {
                                mProgressDialog.dismiss();
                                mBtnRegisterClicked = false;
                                if (!status) {
                                    showError(errorResponse);
                                } else {
                                    SharedPreferenceUtil.getInstance().setString(
                                            K_PREF_FIDO2_USERNAME, mEtUserName.getText().toString());
                                    showResultDialog(R.drawable.icon_dialog_success,
                                            getActivity().
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
                    BlockIDSDK.getInstance().authenticateFIDO2Key(getActivity(),
                            mEtUserName.getText().toString(),
                            tenant.getDns(),
                            tenant.getCommunity(),
                            K_FIDO_FILE_NAME,
                            (status, errorResponse) -> {
                                mProgressDialog.dismiss();
                                mBtnAuthenticateClicked = false;
                                if (!status) {
                                    showError(errorResponse);
                                } else {
                                    SharedPreferenceUtil.getInstance().setString(
                                            K_PREF_FIDO2_USERNAME, mEtUserName.getText().toString());
                                    showResultDialog(R.drawable.icon_dialog_success, getActivity().
                                            getString(R.string.label_successfully_authenticated_with_your_fido2_key));
                                }
                            });
                }
            }
        });
        return view;
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
            Toast.makeText(getActivity(),
                    R.string.label_enter_username,
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void showError(ErrorManager.ErrorResponse error) {
        ErrorDialog errorDialog = new ErrorDialog(getActivity());
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
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().
                getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null && inputMethodManager.isAcceptingText())
            inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().
                    getWindowToken(), 0);
    }

    private void showResultDialog(int imageId, String subMessage) {
        ResultDialog dialog = new ResultDialog(getContext(), imageId,
                SharedPreferenceUtil.getInstance().getString(K_PREF_FIDO2_USERNAME), subMessage);
        dialog.show();
        new Handler().postDelayed(() -> dialog.dismiss(), 2000);
    }
}