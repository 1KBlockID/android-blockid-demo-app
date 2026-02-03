package com.example.biometricauthdemo;

import android.os.Bundle;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.passKey.PasskeyRequest;

import java.nio.charset.StandardCharsets;
import java.security.Provider;
import java.security.Security;
import java.util.concurrent.Executor;

import javax.crypto.Cipher;

public class MainActivity extends AppCompatActivity {

    private TextView txtResult, txtProvider;
    private EditText edtUserName;
    private Executor executor;
    private Button btnPasskeyRegister, btnPasskeyAuthenticate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtResult = findViewById(R.id.txtResult);
        txtProvider = findViewById(R.id.txtProvider);
        edtUserName = findViewById(R.id.edtUserName);
        txtProvider.setText("Start: \n" + printProvider());

        Button btnEncrypt = findViewById(R.id.btnEncrypt);
        Button btnDecrypt = findViewById(R.id.btnDecrypt);
        Button btnTenantRegister = findViewById(R.id.btnTenantRegister);
        btnPasskeyRegister = findViewById(R.id.btnPasskeyRegister);
        btnPasskeyAuthenticate = findViewById(R.id.btnPasskeyAuthenticate);

        executor = ContextCompat.getMainExecutor(this);

        try {
            BiometricCryptoManager.generateKeyIfNeeded();
        } catch (Exception e) {
            txtResult.setText(e.getMessage());
        }

        btnEncrypt.setOnClickListener(v -> encrypt());
        btnDecrypt.setOnClickListener(v -> decrypt());
        btnTenantRegister.setOnClickListener(v -> tenantRegister());

        if (BlockIDSDK.getInstance().isReady()) {
            btnPasskeyRegister.setEnabled(true);
            btnPasskeyAuthenticate.setEnabled(true);
        } else {
            btnPasskeyRegister.setEnabled(false);
            btnPasskeyAuthenticate.setEnabled(false);
        }

        btnPasskeyRegister.setOnClickListener(v -> passkeyRegister());
        btnPasskeyAuthenticate.setOnClickListener(v -> passkeyAuthenticate());
    }

    private void passkeyAuthenticate() {
        String userName = edtUserName.getText().toString();

        if (userName.isEmpty() || userName.length() <= 3) {
            Toast.makeText(this, "Please enter correct user name", Toast.LENGTH_SHORT).show();
            return;
        }
        txtResult.setText("Passkey authenticate in progress...");
        PasskeyRequest request = new PasskeyRequest(AppConstant.defaultTenant, userName, null, null);
        BlockIDSDK.getInstance().issueJWTOnPasskeyAuthentication(this, request, (status, passkeyResponse, errorResponse) -> {
            txtResult.setText("");
            txtProvider.setText("After authenticate passkey:\n" + printProvider());
            Log.e("pankti", "passkey authenticate status: " + status);
            if (status) {
                BlockIDSDK.getInstance().commitApplicationWallet();
                Toast.makeText(this, "passkey authenticate success", Toast.LENGTH_SHORT).show();
                txtResult.setText("JWT: " + passkeyResponse.jwt);
            } else {
                Log.e("pankti", "passkey register error: " + errorResponse.getCode() + " : " + errorResponse.getMessage());
                Toast.makeText(this, "passkey authenticate failed: " + errorResponse.getCode() + " : " + errorResponse.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void passkeyRegister() {
        String userName = edtUserName.getText().toString();
        if (userName.isEmpty() || userName.length() <= 3) {
            Toast.makeText(this, "Please enter correct user name", Toast.LENGTH_SHORT).show();
            return;
        }

        txtResult.setText("Passkey registration in progress...");
        PasskeyRequest request = new PasskeyRequest(AppConstant.defaultTenant, userName, null, null);
        BlockIDSDK.getInstance().registerPasskeyWithAccountLinking(this, request,
                (status, passkeyResponse, errorResponse) -> {
                    txtResult.setText("");
                    txtProvider.setText("After register passkey:\n" + printProvider());
                    Log.e("pankti", "passkey register status: " + status);
                    if (status) {
                        BlockIDSDK.getInstance().commitApplicationWallet();
                        Toast.makeText(this, "passkey register success", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("pankti", "passkey register error: " + errorResponse.getCode() + " : " + errorResponse.getMessage());
                        Toast.makeText(this, "passkey register failed: " + errorResponse.getCode() + " : " + errorResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void tenantRegister() {
        txtResult.setText("Tenant registration in progress...");
        if (!BlockIDSDK.getInstance().isReady()) {
            BlockIDSDK.getInstance().initiateWallet();
            BlockIDSDK.getInstance().registerTenant(AppConstant.defaultTenant, (status, errorResponse, tenant) -> {
                txtResult.setText("");
                Log.e("pankti", "tenant register status: " + status);
                if (status) {
                    txtProvider.setText("After register tenant:\n" + printProvider());
                    BlockIDSDK.getInstance().commitApplicationWallet();
                    Toast.makeText(this, "Tenant register success", Toast.LENGTH_SHORT).show();

                    if (BlockIDSDK.getInstance().isReady()) {
                        btnPasskeyRegister.setEnabled(true);
                        btnPasskeyAuthenticate.setEnabled(true);
                    } else {
                        btnPasskeyRegister.setEnabled(false);
                        btnPasskeyAuthenticate.setEnabled(false);
                    }

                } else {
                    Log.e("pankti", "tenant register error: " + errorResponse.getCode() + " : " + errorResponse.getMessage());
                    Toast.makeText(this, "Tenant register failed", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "SDK is ready", Toast.LENGTH_SHORT).show();
        }
    }

    private void recoverFromBiometricChange() {
        // 1. Delete invalid key
        BiometricCryptoManager.deleteKey();

        // 2. Clear encrypted data
        SecureStorage.clear(this);

        // 3. Recreate key
        try {
            BiometricCryptoManager.generateKeyIfNeeded();
            txtResult.setText("Biometrics changed. Please re-enroll.");
        } catch (Exception e) {
            txtResult.setText("Re-enrollment failed");
        }
    }


    private void encrypt() {
        try {
            Cipher cipher = BiometricCryptoManager.getEncryptCipher();
            authenticate(cipher, true);
        } catch (KeyPermanentlyInvalidatedException e) {
            recoverFromBiometricChange();
        } catch (Exception e) {
            e.printStackTrace();
            txtResult.setText(e.getMessage());
        }
    }

    private void decrypt() {
        txtResult.setText("");
        try {
            byte[] iv = SecureStorage.getIv(this);
            if (iv == null) {
                txtResult.setText("No encrypted data found");
                return;
            }
            Cipher cipher = BiometricCryptoManager.getDecryptCipher(iv);
            authenticate(cipher, false);
        } catch (KeyPermanentlyInvalidatedException e) {
            recoverFromBiometricChange();
        } catch (Exception e) {
            e.printStackTrace();
            txtResult.setText(e.getMessage());
        }
    }

    private void authenticate(Cipher cipher, boolean encrypt) {
        BiometricPrompt biometricPrompt =
                new BiometricPrompt(this, executor,
                        new BiometricPrompt.AuthenticationCallback() {

                            @Override
                            public void onAuthenticationSucceeded(
                                    BiometricPrompt.AuthenticationResult result) {
                                try {
                                    Cipher c = result.getCryptoObject().getCipher();
                                    if (encrypt) {
                                        String secret = "USER_SESSION_TOKEN";
                                        byte[] encrypted = c.doFinal(
                                                secret.getBytes(StandardCharsets.UTF_8));

                                        SecureStorage.save(
                                                MainActivity.this,
                                                encrypted,
                                                c.getIV());

                                        txtResult.setText("Encrypted & stored securely");
                                    } else {
                                        byte[] decrypted = c.doFinal(
                                                SecureStorage.getEncrypted(MainActivity.this));
                                        txtResult.setText(
                                                "Decrypted: " + new String(decrypted));
                                        Log.e("pankti", "Decrypted: " + new String(decrypted));

                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    txtResult.setText(e.getMessage());
                                }
                            }

                            @Override
                            public void onAuthenticationFailed() {
                                super.onAuthenticationFailed();
                                Log.e("pankti", "Authentication failed");
                                txtResult.setText("Authentication failed");
                            }

                            @Override
                            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                                super.onAuthenticationError(errorCode, errString);
                                Log.e("pankti", "Authentication error: " + errString.toString() + " (" + errorCode + ")");
                                txtResult.setText("Authentication error: " + errString.toString() + " (" + errorCode + ")");
                            }
                        });

        BiometricPrompt.PromptInfo promptInfo =
                new BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Secure Biometric Authentication")
                        .setSubtitle("Authenticate to access secure data")
                        .setNegativeButtonText("Cancel")
                        .build();

        biometricPrompt.authenticate(
                promptInfo,
                new BiometricPrompt.CryptoObject(cipher)
        );
    }

    public static String printProvider() {
        String TAG = "pankti";
        StringBuilder sb = new StringBuilder();
        Provider[] providers = Security.getProviders();

        for (int i = 0; i < providers.length; i++) {
            Provider provider = providers[i];
            String line = "Position " + (i + 1) +
                    ": " + provider.getName() +
                    " (Version: " + provider.getVersion() + ")";
            Log.e(TAG, line);
            sb.append(line).append("\n"); // TextView
        }
        return sb.toString();
    }
}
