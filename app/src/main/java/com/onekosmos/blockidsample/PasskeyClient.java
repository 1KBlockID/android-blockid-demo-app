package com.onekosmos.blockidsample;

import android.app.Activity;
import android.os.Bundle;
import android.os.CancellationSignal;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.credentials.CreateCredentialResponse;
import androidx.credentials.CreatePublicKeyCredentialRequest;
import androidx.credentials.CreatePublicKeyCredentialResponse;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.GetPublicKeyCredentialOption;
import androidx.credentials.PasswordCredential;
import androidx.credentials.PublicKeyCredential;
import androidx.credentials.exceptions.CreateCredentialException;
import androidx.credentials.exceptions.GetCredentialException;

import org.json.JSONObject;

import java.util.concurrent.Executor;

public final class PasskeyClient {

    public interface JsonCallback {
        void onSuccess(@NonNull String webAuthnJson);

        void onError(@NonNull Exception e);
    }

    public interface PasswordCallback {
        void onPassword(@NonNull String username, @NonNull String password);
    }

    private final CredentialManager cm;
    private final Executor mainExecutor;

    public PasskeyClient(@NonNull Activity activity) {
        this.cm = CredentialManager.create(activity);
        this.mainExecutor = ContextCompat.getMainExecutor(activity);
    }

    // --- REGISTER: create a passkey (RP “register”/“signup”) ---
    // creationOptionsJson: the exact WebAuthn "PublicKeyCredentialCreationOptions" as JSON from your server
    public void registerPasskey(
            @NonNull Activity activity,
            @NonNull String creationOptionsJson,
            @NonNull JsonCallback callback
    ) {
        CreatePublicKeyCredentialRequest request =
                new CreatePublicKeyCredentialRequest(creationOptionsJson);

        cm.createCredentialAsync(
                activity,
                request,
                new CancellationSignal(),
                mainExecutor,

                new CredentialManagerCallback<CreateCredentialResponse, CreateCredentialException>() {
                    @Override
                    public void onResult(CreateCredentialResponse response) {
                        try {
                            // Typed way (preferred)
                            if (response instanceof CreatePublicKeyCredentialResponse) {
                                String regJson = ((CreatePublicKeyCredentialResponse) response)
                                        .getRegistrationResponseJson();
                                callback.onSuccess(regJson);
                                return;
                            }
                            // Fallback: read from Bundle for older impls
                            Bundle data = response.getData();
                            String regJson = data.getString(
                                    "androidx.credentials.BUNDLE_KEY_REGISTRATION_RESPONSE_JSON");
                            if (regJson != null) {
                                callback.onSuccess(regJson);
                            } else {
                                callback.onError(new IllegalStateException("No registration JSON in response"));
                            }
                        } catch (Exception e) {
                            callback.onError(e);
                        }
                    }

                    @Override
                    public void onError(CreateCredentialException e) {
                        callback.onError(e);
                    }
                }
        );
    }

    // --- SIGN-IN: get a passkey assertion (RP “signin”/“authenticate”) ---
    // requestOptionsJson: the exact WebAuthn "PublicKeyCredentialRequestOptions" as JSON from your server
    public void signInWithPasskey(
            @NonNull Activity activity,
            @NonNull String requestOptionsJson,
            @NonNull JsonCallback passkeyCallback,
            @NonNull PasswordCallback passwordFallback // invoked if a user picks a saved password instead
    ) {
        GetPublicKeyCredentialOption passkeyOption =
                new GetPublicKeyCredentialOption(requestOptionsJson);

        // You can also add a GetPasswordOption() if you want password fallback in the same sheet.
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(passkeyOption)
                // .addCredentialOption(new GetPasswordOption())
                .build();

        cm.getCredentialAsync(
                activity,
                request,
                new CancellationSignal(),
                mainExecutor,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        try {
                            Credential cred = result.getCredential();

                            if (cred instanceof PublicKeyCredential) {
                                String authJson = ((PublicKeyCredential) cred).getAuthenticationResponseJson();
                                JSONObject authJsonObject = new JSONObject(authJson);
                                authJsonObject.put("tenantId", "68418b2587942f1d3158a798");
                                authJsonObject.put("communityId","68418b2587942f1d3158a799");
                                authJsonObject.put("dns","1k-dev.1kosmos.net");
                                String finalJsonString = authJsonObject.toString();
                                passkeyCallback.onSuccess(finalJsonString);
                                return;
                            }

                            if (cred instanceof PasswordCredential) {
                                PasswordCredential p = (PasswordCredential) cred;
                                passwordFallback.onPassword(p.getId(), p.getPassword());
                                return;
                            }

                            if (cred instanceof CustomCredential) {
                                // e.g., Google ID (GetGoogleIdOption) if you add that option
                                // Handle or ignore as needed for your app.
                                passkeyCallback.onError(
                                        new UnsupportedOperationException("Unhandled CustomCredential type"));
                                return;
                            }

                            passkeyCallback.onError(
                                    new IllegalStateException("Unknown credential type: " + cred.getClass().getName()));
                        } catch (Exception e) {
                            passkeyCallback.onError(e);
                        }
                    }

                    @Override
                    public void onError(GetCredentialException e) {

                        passkeyCallback.onError(e);
                    }
                }
        );
    }
}

