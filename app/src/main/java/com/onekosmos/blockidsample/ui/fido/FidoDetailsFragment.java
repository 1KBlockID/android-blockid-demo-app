package com.onekosmos.blockidsample.ui.fido;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.datamodel.BIDTenant;
import com.onekosmos.blockidsample.AppConstant;
import com.onekosmos.blockidsample.R;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class FidoDetailsFragment extends Fragment {
    private AppCompatButton mBtnRegister;
    private TextInputEditText mEtUserName;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_fido_details,
                container,
                false);

        mEtUserName = view.findViewById(R.id.edt_user_name);
        mBtnRegister = view.findViewById(R.id.btn_register);

        mBtnRegister.setOnClickListener(v -> {
            if (validateUserName(mEtUserName.getText().toString())) {
                BIDTenant tenant = AppConstant.defaultTenant;
                BlockIDSDK.getInstance().registerFIDOKey(getActivity(),
                        mEtUserName.getText().toString(),
                        tenant.getDns(),
                        tenant.getCommunity(),
                        (status, errorResponse) -> {
                        });
            }
        });
        return view;
    }

    /**
     * Check userName is empty or not
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

    /**
     * Hide keyboard when user click on button
     */
    public void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().
                getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null && inputMethodManager.isAcceptingText())
            inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().
                    getWindowToken(), 0);
    }
}