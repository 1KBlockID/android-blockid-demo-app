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
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.util.SharedPreferenceUtil;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class FidoDetailsFragment extends Fragment {
    private AppCompatButton mBtnContinue;
    private TextInputEditText mEtUserName;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_fido_details,
                container,
                false);

        mEtUserName = view.findViewById(R.id.edt_user_name);
        mBtnContinue = view.findViewById(R.id.btn_continue);

        mBtnContinue.setOnClickListener(v -> {
            String userName = mEtUserName.getText().toString().trim();
            hideKeyboard();
            if (TextUtils.isEmpty(userName)) {
                Toast.makeText(getActivity(),
                        R.string.label_enter_username,
                        Toast.LENGTH_SHORT).show();
            } else {
                SharedPreferenceUtil.getInstance().setString(
                        SharedPreferenceUtil.K_PREF_FIDO2_USERNAME, userName);
                getActivity().getSupportFragmentManager().beginTransaction().
                        add(R.id.fragment_fido_container, new FidoWebViewFragment()).commit();
            }
        });
        return view;
    }

    public void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().
                getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null && inputMethodManager.isAcceptingText())
            inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().
                    getWindowToken(), 0);
    }
}