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
    private static volatile SharedPreferenceUtil mSharedPreferenceUtil;
    private static volatile SharedPreferences mSharedPreferences;

    private static final String K_KEY_ALIAS = "_androidx_security_master_key_";
    private static final String K_MIGRATION_DONE_KEY = "demo_migration_v2_done";

    // The OLD SDK opened EncryptedSharedPreferences with this exact file name.
    // DO NOT CHANGE — this must match whatever was previously passed to
    // EncryptedSharedPreferences.create(context, <name>, ...).
    private static final String K_LEGACY_PREFS_NAME = "%s"; // historically: context.getPackageName()

    // The NEW plain-backed (per-value encrypted) store MUST use a DIFFERENT file
    // name than the legacy one. Reusing the same name means opening/clearing the
    // new store also opens/clears the legacy file underneath it — which silently
    // wipes any data you just migrated. This was the root cause of the upgrade bug.
    private static final String K_NEW_PREFS_NAME = "%s.bid_demo_vault_v2";


    /**
     * Initialising encrypted shared preferences
     *
     * @param context should be ApplicationContext not Activity
     */
    public static synchronized void initialize(Context context) {
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

            // Physically remove the legacy prefs file from shared_prefs/ so no
            // empty/orphaned file lingers on disk. clear() only wipes the entries
            // inside the file — deleteSharedPreferences() removes the file itself.
            // Available since API 24; app minSdk is 28, so this is always safe.
            deleteLegacyPrefsFile(context);

            Log.i("SharedPreferenceUtil", "Migration from EncryptedSharedPreferences complete");

        } catch (Exception e) {
            // Old prefs unreadable (e.g. key lost) — mark done and start fresh
            e.printStackTrace();
        }
    }

    /**
     * Physically deletes the legacy prefs file from the app's shared_prefs directory.
     * <p>
     * {@code SharedPreferences.edit().clear()} only removes the entries but leaves an
     * empty file behind. {@link Context#deleteSharedPreferences(String)} (API 24+)
     * removes the backing file itself so nothing orphaned is left in the cache.
     *
     * @param context should be ApplicationContext not Activity
     */
    private static void deleteLegacyPrefsFile(Context context) {
        String legacyName = legacyPrefsName(context);
        // Clear entries first so any in-memory instance releases its state, then
        // remove the physical file.
        context.getSharedPreferences(legacyName, Context.MODE_PRIVATE)
                .edit().clear().commit();
        boolean deleted = context.deleteSharedPreferences(legacyName);
        Log.i("SharedPreferenceUtil", "Legacy prefs file deletion "
                + (deleted ? "succeeded" : "failed or file already absent") + ": " + legacyName);
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
        return putEncrypted(key, value);
    }

    /**
     * @param key   The name of the preference to modify.
     * @param val The new value for the preference.
     * @return Returns true if the new values were successfully written to persistent storage
     */
    public static boolean setInt(String key, int val) {
        return putEncrypted(key, String.valueOf(val));
    }

    /**
     * @param key   The name of the preference to modify.
     * @param val The new value for the preference.
     * @return Returns true if the new values were successfully written to persistent storage
     */
    public static boolean setBool(String key, boolean val) {
        return putEncrypted(key, String.valueOf(val));
    }

    /**
     * Encrypts and stores a value. This is a security-critical vault, so it fails
     * closed: if encryption is unavailable (e.g. KeyStore error), the value is NOT
     * written and {@code false} is returned. Plaintext is never persisted.
     * <p>
     * On the first encryption failure it attempts to self-heal by regenerating the
     * KeyStore key and retrying exactly once, since the most common cause is an
     * invalidated/corrupted key.
     *
     * @param key the preference key; ignored when null/empty
     * @param val the value to encrypt and store
     * @return {@code true} if the encrypted value was committed, {@code false} otherwise
     */
    private static boolean putEncrypted(String key, String val) {
        if (mSharedPreferences == null || TextUtils.isEmpty(key)) return false;
        try {
            String encrypted = encrypt(val);
            return mSharedPreferences.edit().putString(key, encrypted).commit();
        } catch (Exception firstFailure) {
            // Encryption most often fails because the KeyStore key was invalidated
            // (e.g. lock-screen credentials changed) or the alias got corrupted.
            // Logging alone doesn't recover, so attempt to self-heal: regenerate the
            // key and retry the encrypt exactly once before giving up.
            Log.w("SharedPreferenceUtil", "encrypt failed for key: " + key
                    + "; attempting key recovery", firstFailure);
            if (regenerateKey()) {
                try {
                    String encrypted = encrypt(val);
                    return mSharedPreferences.edit().putString(key, encrypted).commit();
                } catch (Exception retryFailure) {
                    Log.e("SharedPreferenceUtil", "encrypt retry failed for key: " + key
                            + "; value NOT stored", retryFailure);
                }
            }
            // Fail closed — never fall back to storing plaintext in the vault.
            return false;
        }
    }

    /**
     * Deletes the existing (likely invalidated/corrupted) KeyStore alias and
     * generates a fresh AES key. Used as a recovery step when encryption fails.
     * <p>
     * Note: any values previously encrypted with the old key become undecryptable
     * once the key is replaced — this is unavoidable when the key is invalidated,
     * since the original key material is already gone.
     *
     * @return {@code true} if a usable key exists after regeneration
     */
    private static boolean regenerateKey() {
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            if (ks.containsAlias(K_KEY_ALIAS)) {
                ks.deleteEntry(K_KEY_ALIAS);
            }
            ensureKeyExists();
            return true;
        } catch (Exception e) {
            Log.e("SharedPreferenceUtil", "KeyStore key regeneration failed", e);
            return false;
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