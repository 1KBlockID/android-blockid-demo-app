package com.onekosmos.blockidsample.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2022 1Kosmos. All rights reserved.
 */
@SuppressWarnings("UnusedReturnValue")
public class SharedPreferenceUtil {
    private static SharedPreferenceUtil mSharedPreferenceUtil;
    private static SharedPreferences mSharedPreferences;

    private static final String K_KEY_ALIAS = "_androidx_security_master_key_";
    private static final String K_MIGRATION_DONE_KEY = "bid_migration_v2_done";

    // The OLD SDK opened EncryptedSharedPreferences with this exact file name.
    // DO NOT CHANGE — this must match whatever was previously passed to
    // EncryptedSharedPreferences.create(context, <name>, ...).
    private static final String K_LEGACY_PREFS_NAME = "%s"; // historically: context.getPackageName()

    // The NEW plain-backed (per-value encrypted) store MUST use a DIFFERENT file
    // name than the legacy one. Reusing the same name means opening/clearing the
    // new store also opens/clears the legacy file underneath it — which silently
    // wipes any data you just migrated. This was the root cause of the upgrade bug.
    private static final String K_NEW_PREFS_NAME = "%s.bid_app_vault_v2";


    /**
     * Initialising encrypted shared preferences
     *
     * @param context should be ApplicationContext not Activity
     */
    public static void initialize(Context context) {
        if (mSharedPreferences == null) {
            mSharedPreferences = context.getSharedPreferences(
                    newPrefsName(context),
                    Context.MODE_PRIVATE
            );
            try {
                ensureKeyExists();
            } catch (Exception e) {
               e.printStackTrace();
            }
            migrateFromEncryptedSharedPreferences(context); // ← one-time migration
        }
        if (mSharedPreferenceUtil == null)
            mSharedPreferenceUtil = new SharedPreferenceUtil();
    }

    private static String legacyPrefsName(Context context) {
        return String.format(K_LEGACY_PREFS_NAME, context.getPackageName());
    }

    private static String newPrefsName(Context context) {
        return String.format(K_NEW_PREFS_NAME, context.getPackageName());
    }

    /**
     * The migration block intentionally still uses the deprecated MasterKey +
     * EncryptedSharedPreferences — that's fine, it's only used once for the migration read and
     * can be removed in a future release once your user base has fully migrated.
     *
     * @param context should be ApplicationContext not Activity
     */
    private static void migrateFromEncryptedSharedPreferences(Context context) {
        // Check if migration already done
        if (mSharedPreferences.getBoolean(K_MIGRATION_DONE_KEY, false)) return;

        try {
            // Attempt to open old EncryptedSharedPreferences
            MasterKey masterKey = new MasterKey.Builder(context,
                    MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            SharedPreferences oldPrefs = EncryptedSharedPreferences.create(
                    context,
                    legacyPrefsName(context),
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            Map<String, ?> allEntries = oldPrefs.getAll();
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof String) {
                    setString(entry.getKey(), (String) value);
                } else if (value instanceof Boolean) {
                    setBool(entry.getKey(), (Boolean) value);
                } else if (value instanceof Integer) {
                    setInt(entry.getKey(), (Integer) value);
                }
            }


            // Mark migration done
            mSharedPreferences.edit()
                    .putBoolean(K_MIGRATION_DONE_KEY, true)
                    .apply();

            // Delete old encrypted prefs file
            context.getSharedPreferences(legacyPrefsName(context), Context.MODE_PRIVATE)
                    .edit().clear().apply();

            Log.i("SharedPreferenceUtil", "Migration from EncryptedSharedPreferences complete");

        } catch (Exception e) {
            // Old prefs unreadable (e.g. key lost) — mark done and start fresh
            e.printStackTrace();
        }
    }

    private static void ensureKeyExists() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        if (!ks.containsAlias(K_KEY_ALIAS)) {
            KeyGenerator kg = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            kg.init(new KeyGenParameterSpec.Builder(
                    K_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build());
            kg.generateKey();
        }
    }

    private static SecretKey loadKey() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        return ((KeyStore.SecretKeyEntry) ks.getEntry(K_KEY_ALIAS, null)).getSecretKey();
    }

    private static String encrypt(String plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, loadKey());
        byte[] iv = cipher.getIV();
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
        return Base64.encodeToString(combined, Base64.NO_WRAP);
    }

    private static String decrypt(String encoded) throws Exception {
        byte[] combined = Base64.decode(encoded, Base64.NO_WRAP);
        byte[] iv = Arrays.copyOfRange(combined, 0, 12);
        byte[] ciphertext = Arrays.copyOfRange(combined, 12, combined.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, loadKey(), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
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
    public static boolean setString(String key, String value) {
        if (mSharedPreferences == null || TextUtils.isEmpty(key)) return false;
        try {
            return mSharedPreferences.edit().putString(key, encrypt(value)).commit();
        } catch (Exception e) {
            Log.w("SharedPreferenceUtil", "encrypt failed for key: " + key + ", " +
                    "storing plain", e);
            return mSharedPreferences.edit().putString(key, value).commit();
        }
    }

    /**
     * @param key   The name of the preference to modify.
     * @param val The new value for the preference.
     * @return Returns true if the new values were successfully written to persistent storage
     */
    public static boolean setInt(String key, int val) {
        if (mSharedPreferences == null || TextUtils.isEmpty(key)) return false;
        try {
            return mSharedPreferences.edit().putString(key, encrypt(String.valueOf(val))).commit();
        } catch (Exception e) {
            return mSharedPreferences.edit().putInt(key, val).commit();
        }
    }

    /**
     * @param key   The name of the preference to modify.
     * @param val The new value for the preference.
     * @return Returns true if the new values were successfully written to persistent storage
     */
    public static boolean setBool(String key, boolean val) {
        if (mSharedPreferences == null || TextUtils.isEmpty(key)) return false;
        try {
            return mSharedPreferences.edit().putString(key, encrypt(String.valueOf(val))).commit();
        } catch (Exception e) {
            return mSharedPreferences.edit().putBoolean(key, val).commit();
        }
    }

    /**
     * @param key The name of the preference to retrieve.
     * @return Returns the preference value if it exists, or default value null
     */
    public String getString(String key) {
        if (mSharedPreferences == null) return null;
        String stored = mSharedPreferences.getString(key, "");
        if (TextUtils.isEmpty(stored)) return stored;
        try {
            return decrypt(stored);
        } catch (Exception e) {
            // backward compat: was stored as plain text (pre-migration era)
            return stored;
        }
    }

    /**
     * @param key The name of the preference to retrieve.
     * @return Returns the preference value if it exists, or default value 0
     */
    public int getInt(String key) {
        if (mSharedPreferences == null) return 0;
        // Try encrypted string first (new format)
        String stored = mSharedPreferences.getString(key, null);
        if (stored != null) {
            try {
                return Integer.parseInt(decrypt(stored));
            } catch (Exception e) {
                // fallback: try parse plain stored string (migration era)
                try {
                    return Integer.parseInt(stored);
                } catch (Exception ignored) {
                }
            }
        }
        // fallback: old format stored as actual int
        return mSharedPreferences.getInt(key, 0);
    }

    /**
     * @param key The name of the preference to retrieve.
     * @return Returns true if the preference value exists true in the preferences,otherwise false
     */
    public boolean getBool(String key) {
        if (mSharedPreferences == null) return false;
        String stored = mSharedPreferences.getString(key, null);
        if (stored != null) {
            try {
                return Boolean.parseBoolean(decrypt(stored));
            } catch (Exception e) {
                try {
                    return Boolean.parseBoolean(stored);
                } catch (Exception ignored) {
                }
            }
        }
        return mSharedPreferences.getBoolean(key, false);
    }

    /**
     * Remove keys from {@link SharedPreferences}..
     *
     * @param keys The name of the key(s) to be removed.
     */
    public void remove(String... keys) {
        if (mSharedPreferences == null) return;
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        for (String key : keys) {
            editor.remove(key);
        }
        editor.apply();
    }

    /**
     * Remove all keys from {@link SharedPreferences}..
     */
    public void clear() {
        if (mSharedPreferences == null) return;
        mSharedPreferences.edit().clear().apply();
    }
}