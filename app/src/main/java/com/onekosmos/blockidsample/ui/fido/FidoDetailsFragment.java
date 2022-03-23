package com.onekosmos.blockidsample.ui.fido;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.datamodel.BIDTenant;
import com.onekosmos.blockidsample.AppConstant;
import com.onekosmos.blockidsample.BuildConfig;
import com.onekosmos.blockidsample.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class FidoDetailsFragment extends Fragment {
    private AppCompatButton mBtnContinue, mBtnAuthenticate, mBtnRegister;
    private TextInputEditText mEtUserName;
    private String userName;
//    private ChromeCustomTab mChromeCustomTab;

    @Override
    public void onDestroy() {
        super.onDestroy();
//        if (mChromeCustomTab != null) {
//            mChromeCustomTab.unbindCustomTabsService();
//        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_fido_details,
                container,
                false);

//        mChromeCustomTab = new ChromeCustomTab(getActivity());
        mEtUserName = view.findViewById(R.id.edt_user_name);
        mBtnContinue = view.findViewById(R.id.btn_continue);
        mBtnAuthenticate = view.findViewById(R.id.btn_authenticate);
        mBtnRegister = view.findViewById(R.id.btn_register);

        mBtnRegister.setOnClickListener(v -> {
            if (validateUserName()) {
                BIDTenant tenant = AppConstant.defaultTenant;
                BlockIDSDK.getInstance().registerFIDOKey(getContext(),
                        mEtUserName.getText().toString(),
                        tenant.getDns(),
                        tenant.getCommunity(),
                        (status, errorResponse) -> {

                        });
            }
        });

        mBtnAuthenticate.setOnClickListener(v -> {
            if (validateUserName()) {
                BIDTenant tenant = AppConstant.defaultTenant;
                BlockIDSDK.getInstance().authenticateFIDOKey(getContext(),
                        mEtUserName.getText().toString(),
                        tenant.getDns(),
                        tenant.getCommunity(),
                        (status, errorResponse) -> {

                        });
            }
        });

//        mBtnContinue.setOnClickListener(v -> {
//            userName = mEtUserName.getText().toString().trim();
//            hideKeyboard();
//            if (TextUtils.isEmpty(userName)) {
//                Toast.makeText(getActivity(),
//                        R.string.label_enter_username,
//                        Toast.LENGTH_SHORT).show();
//            } else {
//
//                Uri uri = createUri(getActivity());
//                Log.e("Uri", "--> " + uri);
//                String url = "https://1kfido.blockid.co/appless_demo/index2.html?username=" + userName;
//                mChromeCustomTab.show(url);
////                mChromeCustomTab.show(uri);
//            }
//        });
        return view;
    }

    private boolean validateUserName() {
        userName = mEtUserName.getText().toString().trim();
        hideKeyboard();
        if (TextUtils.isEmpty(userName)) {
            Toast.makeText(getActivity(),
                    R.string.label_enter_username,
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().
                getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null && inputMethodManager.isAcceptingText())
            inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().
                    getWindowToken(), 0);
    }

    //FIXME new to remove
    private Uri createUri(Activity context) {
        try {
            //Create an empty html file in external cache directory
            File redirect = new File(context.getExternalCacheDir(), "fido.html");

            //Open and read local html file from asset folder
            InputStream inputStream = context.getAssets().open("fido.html");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            String templateString = new String(buffer, "UTF-8");

            //Write the content of file in redirect.html
            FileOutputStream fileOutputStream = new FileOutputStream(redirect);
            fileOutputStream.write(templateString.getBytes());

            //Get the uri of redirect.html using content provider
            Uri uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", redirect);

            return uri;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}