package com.onekosmos.blockidsample.ui.fido;

import android.app.Activity;
import android.content.ComponentName;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.onekosmos.blockidsample.R;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class FidoDetailsFragment extends Fragment {
    private AppCompatButton mBtnContinue;
    private TextInputEditText mEtUserName;
    private String userName;
    private CustomTabsClient mClient;
    private CustomTabsIntent mCustomTabsIntent;
    private CustomTabsSession mSession;
    private CustomTabsServiceConnection mConnection;

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mConnection != null) {
            getActivity().unbindService(mConnection);
            mConnection = null;
        }
    }

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
            userName = mEtUserName.getText().toString().trim();
            hideKeyboard();
            if (TextUtils.isEmpty(userName)) {
                Toast.makeText(getActivity(),
                        R.string.label_enter_username,
                        Toast.LENGTH_SHORT).show();
            } else {
                String url = "https://1kfido.blockid.co/appless_demo/index2.html?username=" + userName;
//                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
//                CustomTabsIntent customTabsIntent = builder.build();
//                customTabsIntent.launchUrl(getActivity(), Uri.parse(url));
                loadCustomTab(url);
            }
        });
        return view;
    }

    private void loadCustomTab(String url) {
        mConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(@NonNull ComponentName name,
                                                     @NonNull CustomTabsClient client) {
                Log.e("ComponentName", name.getPackageName());
                mClient = client;
                mClient.warmup(0);
                mSession = mClient.newSession(new CustomTabsCallback() {
                    @Override
                    public void onNavigationEvent(int navigationEvent, @Nullable Bundle extras) {
                        super.onNavigationEvent(navigationEvent, extras);
                        Log.e("navigationEvent", "--> " + navigationEvent);
                    }

                    @Override
                    public void onPostMessage(@NonNull String message, @Nullable Bundle extras) {
                        super.onPostMessage(message, extras);
                        Log.e("onPostMessage", "--> " + message);
                    }
                });
                if (mSession != null) {
                    mSession.mayLaunchUrl(Uri.parse(url), null, null);
                    mCustomTabsIntent = new CustomTabsIntent.Builder().build();
                    mCustomTabsIntent.launchUrl(getActivity(), Uri.parse(url));
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mConnection = null;
            }
        };

        boolean connected = CustomTabsClient.bindCustomTabsService(getActivity(),
                ChromeCustomTab.CUSTOM_TAB_PACKAGE_NAME, mConnection);
        Log.e("Connected", "--> " + connected);
    }

    public void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().
                getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null && inputMethodManager.isAcceptingText())
            inputMethodManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().
                    getWindowToken(), 0);
    }
}