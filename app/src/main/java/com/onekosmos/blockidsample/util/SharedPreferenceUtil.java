package com.onekosmos.blockidsample.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2022 1Kosmos. All rights reserved.
 */
public class SharedPreferenceUtil {
    private static SharedPreferenceUtil mSharedPreferenceUtil;
    private static SharedPreferences mSharedPreferences;

    /**
     * Initialising encrypted shared preferences
     *
     * @param context should be ApplicationContext not Activity
     */
    public static void initialize(Context context) {
        if (mSharedPreferences == null) {
            try {
                mSharedPreferences = EncryptedSharedPreferences.create(
                        context.getPackageName(),
                        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
                        context,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
            } catch (GeneralSecurityException | IOException e) {
                return;
            }
        }
        if (mSharedPreferenceUtil == null)
            mSharedPreferenceUtil = new SharedPreferenceUtil();
    }

    /**
     * @return instance on SharedPreferenceUtil
     */
    public static SharedPreferenceUtil getInstance() {
        return mSharedPreferenceUtil;
    }

    /**
     * @param key   The name of the preference to modify.
     * @param value The new value for the preference.
     * @return Returns true if the new values were successfully written to persistent storage
     */
    public boolean setString(String key, String value) {
        if (mSharedPreferences != null && !TextUtils.isEmpty(key))
            return mSharedPreferences.edit().putString(key, value).commit();
        return false;
    }

    /**
     * @param key   The name of the preference to modify.
     * @param value The new value for the preference.
     * @return Returns true if the new values were successfully written to persistent storage
     */
    public boolean setInt(String key, int value) {
        if (mSharedPreferences != null && !TextUtils.isEmpty(key))
            return mSharedPreferences.edit().putInt(key, value).commit();
        return false;
    }

    /**
     * @param key   The name of the preference to modify.
     * @param value The new value for the preference.
     * @return Returns true if the new values were successfully written to persistent storage
     */
    public boolean setBool(String key, boolean value) {
        if (mSharedPreferences != null && !TextUtils.isEmpty(key))
            return mSharedPreferences.edit().putBoolean(key, value).commit();
        return false;
    }

    /**
     * @param key The name of the preference to retrieve.
     * @return Returns the preference value if it exists, or default value null
     */
    public String getString(String key) {
        if (mSharedPreferences != null)
            return mSharedPreferences.getString(key, null);
        return null;
    }

    /**
     * @param key The name of the preference to retrieve.
     * @return Returns the preference value if it exists, or default value 0
     */
    public int getInt(String key) {
        if (mSharedPreferences != null)
            return mSharedPreferences.getInt(key, 0);
        return 0;
    }

    /**
     * @param key The name of the preference to retrieve.
     * @return Returns true if the preference value exists true in the preferences,otherwise false
     */
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