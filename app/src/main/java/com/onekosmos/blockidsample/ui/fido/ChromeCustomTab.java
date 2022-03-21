package com.onekosmos.blockidsample.ui.fido;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class ChromeCustomTab {
    private static final String CUSTOM_TAB_PACKAGE_NAME = "com.android.chrome";
    private Activity mActivity;

    private CustomTabsSession mCustomTabsSession;
    private CustomTabsClient mClient;
    private CustomTabsServiceConnection mConnection;

    public ChromeCustomTab(Activity activity) {
        this.mActivity = activity;
        bindCustomTabsService();
    }

    private void bindCustomTabsService() {
        if (mClient != null) {
            return;
        }

        mConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
                mClient = client;

                if (mClient == null) {
                    return;
                }

                mCustomTabsSession = mClient.newSession(new CustomTabsCallback() {
                    @Override
                    public void onNavigationEvent(int navigationEvent, Bundle extras) {
                        switch (navigationEvent) {
                            case NAVIGATION_STARTED:
                                Log.e("Navigation", "started");
                                break;
                            case NAVIGATION_FINISHED:
                                Log.e("Navigation", "finish");
                                break;
                            case NAVIGATION_FAILED:
                                Log.e("Navigation", "failed");
                                break;
                            case NAVIGATION_ABORTED:
                                Log.e("Navigation", "aborted");
                                break;
                            case TAB_SHOWN:
                                Log.e("Navigation", "show");
                                break;
                            case TAB_HIDDEN:
                                Log.e("Navigation", "hidden");
                                break;
                        }
                    }

                    @Override
                    public void onPostMessage(@NonNull String message, @Nullable Bundle extras) {
                        super.onPostMessage(message, extras);
                        Log.e("Message", "-->" + message);
                    }

                    @Override
                    public void onRelationshipValidationResult(int relation, @NonNull Uri requestedOrigin, boolean result, @Nullable Bundle extras) {
                        super.onRelationshipValidationResult(relation, requestedOrigin, result, extras);
                        Log.e("Call", "onRelationshipValidationResult");
                    }

                    @Override
                    public void onMessageChannelReady(@Nullable Bundle extras) {
                        super.onMessageChannelReady(extras);
                        Log.e("Call", "onMessageChannelReady");
                    }

                    @Override
                    public void extraCallback(@NonNull String callbackName, @Nullable Bundle args) {
                        super.extraCallback(callbackName, args);
                        Log.e("Call", "extraCallback");
                    }

                    @Nullable
                    @Override
                    public Bundle extraCallbackWithResult(@NonNull String callbackName, @Nullable Bundle args) {
                        return super.extraCallbackWithResult(callbackName, args);
                    }
                });
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mClient = null;
            }
        };
        boolean ok = CustomTabsClient.bindCustomTabsService(mActivity, CUSTOM_TAB_PACKAGE_NAME, mConnection);
        if (!ok) {
            mConnection = null;
        }
    }

    public void show(String url) {
        CustomTabsIntent.Builder builder = mCustomTabsSession != null ?
                new CustomTabsIntent.Builder(mCustomTabsSession) : new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(mActivity, Uri.parse(url));
    }

    public void show(Uri uri) {
        CustomTabsIntent.Builder builder = mCustomTabsSession != null ?
                new CustomTabsIntent.Builder(mCustomTabsSession) : new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        customTabsIntent.intent.setData(uri);
        customTabsIntent.launchUrl(mActivity, uri);
    }

    public void unbindCustomTabsService() {
        if (mConnection == null) {
            return;
        }
        mActivity.unbindService(mConnection);
        mClient = null;
        mCustomTabsSession = null;
    }
}
