package com.onekosmos.blockidsdkdemo.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Created by Pankti Mistry on 30-04-2021.
 * Copyright © 2020 1Kosmos. All rights reserved.
 */
public class SharedPreferenceUtil {

    private static SharedPreferenceUtil mSharedPreferenceUtil;
    private static SharedPreferences mSharedPreferences;

    /**
     * @param context should be ApplicationContext not Activity
     */
    public static void initialize(Context context) {
        if (mSharedPreferences == null) {
            try {
                String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
                mSharedPreferences = EncryptedSharedPreferences.create(
                        context.getPackageName(),
                        masterKeyAlias,
                        context,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
            } catch (GeneralSecurityException | IOException e) {
                e.printStackTrace();
            }

        }
        if (mSharedPreferenceUtil == null)
            mSharedPreferenceUtil = new SharedPreferenceUtil();
    }

    public static SharedPreferenceUtil getInstance() {
        return mSharedPreferenceUtil;
    }

    public boolean setString(String key, String val) {
        if (mSharedPreferences != null && !TextUtils.isEmpty(key))
            return mSharedPreferences.edit().putString(key, val).commit();
        return false;
    }

    public boolean setInt(String key, int val) {
        if (mSharedPreferences != null && !TextUtils.isEmpty(key))
            return mSharedPreferences.edit().putInt(key, val).commit();
        return false;
    }

    public boolean setBool(String key, boolean val) {
        if (mSharedPreferences != null && !TextUtils.isEmpty(key))
            return mSharedPreferences.edit().putBoolean(key, val).commit();
        return false;
    }

    public boolean setFloat(String key, float val) {
        if (mSharedPreferences != null && !TextUtils.isEmpty(key))
            return mSharedPreferences.edit().putFloat(key, val).commit();
        return false;
    }

    public boolean setDouble(String key, double val) {
        if (mSharedPreferences != null && !TextUtils.isEmpty(key))
            return mSharedPreferences.edit().putString(key, String.valueOf(val)).commit();
        return false;
    }

    public boolean setLong(String key, long val) {
        if (mSharedPreferences != null && !TextUtils.isEmpty(key))
            return mSharedPreferences.edit().putLong(key, val).commit();
        return false;
    }

    public String getString(String key) {
        if (mSharedPreferences != null)
            return mSharedPreferences.getString(key, "");
        return null;
    }

    public int getInt(String key) {
        if (mSharedPreferences != null)
            return mSharedPreferences.getInt(key, 0);
        return 0;
    }

    public long getLong(String key) {
        if (mSharedPreferences != null)
            return mSharedPreferences.getLong(key, 0);
        return 0;
    }

    public float getFloat(String key) {
        if (mSharedPreferences != null)
            return mSharedPreferences.getFloat(key, 0);
        return 0;
    }

    private double getDouble(String key) {
        try {
            return Double.valueOf(mSharedPreferences.getString(key, String.valueOf(0.0)));
        } catch (NumberFormatException nfe) {
            return 0.0;
        }
    }

    public boolean getBool(String key) {
        if (mSharedPreferences != null)
            return mSharedPreferences.getBoolean(key, false);
        return false;
    }

    /**
     * Remove keys from {@link SharedPreferences}..
     *
     * @param keys The name of the key(s) to be removed.
     */
    public void remove(String... keys) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        for (String key : keys) {
            editor.remove(key);
        }
        editor.commit();
    }

    /**
     * Remove all keys from {@link SharedPreferences}..
     */
    public void clear() {
        mSharedPreferences.edit().clear().commit();
    }
}

