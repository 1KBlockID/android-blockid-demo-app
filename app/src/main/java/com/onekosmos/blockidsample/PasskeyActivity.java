package com.onekosmos.blockidsample;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.credentials.CredentialManager;

import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ApiResponseCallback;
import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BIDAPIs.fido2.FIDO2NativeAPIs;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.utils.BIDUtil;

import org.json.JSONObject;

import java.util.ArrayList;

public class PasskeyActivity extends AppCompatActivity {
    private CredentialManager credentialManager;

    private AppCompatEditText edtUserName, edtPassword;
    private AppCompatButton btnRegister, btnAuth;
    private PasskeyClient passkeyClient;

    // Minimal test JSONs — for local UI testing only (no server verification)
    private static final String CREATION_OPTIONS_JSON = "{"
            + "\"rp\":{\"name\":\"Demo RP\",\"id\":\"example.com\"},"
            + "\"user\":{\"id\":\"dGVzdC11c2VyLWlk\",\"name\":\"user@example.com\",\"displayName\":\"Test User\"},"
            + "\"challenge\":\"dGVzdC1jaGFsbGVuZ2UtMTIz\","
            + "\"pubKeyCredParams\":[{\"type\":\"public-key\",\"alg\":-7}],"
            + "\"timeout\":60000,"
            + "\"attestation\":\"none\""
            + "}";

//    String creationOptionsJson = "{\n" +
//            "  \"rp\": {\n" +
//            "    \"name\": \"Example RP\",\n" +
//            "    \"id\": \"example.com\"\n" +
//            "  },\n" +
//            "  \"user\": {\n" +
//            "    \"id\": \"AQIDBAU=\",\n" + // base64url for [0x01, 0x02, 0x03, 0x04, 0x05]
//            "    \"name\": \"testuser\",\n" +
//            "    \"displayName\": \"Test User\"\n" +
//            "  },\n" +
//            "  \"challenge\": \"c29tZS1yYW5kb20tY2hhbGxlbmdl\", \n" + // "some-random-challenge" in base64url
//            "  \"pubKeyCredParams\": [\n" +
//            "    {\"type\": \"public-key\", \"alg\": -7}\n" + // ES256
//            "  ]\n" +
//            "}";

    String creationOptionsJson =
            "{\"rp\":{\"name\":\"1k-dev.1kosmos.net\",\"id\":\"1k-dev.1kosmos.net\"},\"user\":{\"id\":\"rnjSNw2n-txpJGqVBu5XXoGtazag4yWt7odPFx41LHo\",\"name\":\"pankti\",\"displayName\":\"panktimistry\"},\"attestation\":\"direct\",\"pubKeyCredParams\":[{\"type\":\"public-key\",\"alg\":-7}],\"timeout\":7200000,\"authenticatorSelection\":{\"userVerification\":\"preferred\",\"requireResidentKey\":false},\"challenge\":\"ZXlKaGJHY2lPaUpJVXpJMU5pSXNJblI1Y0NJNklrcFhWQ0o5LmV5SmhkV1FpT2lJeGF5MWtaWFl1TVd0dmMyMXZjeTV1WlhRaUxDSnpkV0lpT2lKd1lXNXJkR2tpTENKbGVIQWlPakUzTlRVeE9UTTNNREVzSW5KaGJtUWlPaUpwU1RaS2N6UjNOVTVIYjNONmNuSnBZbVpMV21rMk1IRjVSRWhLVjJkNGRDMWhVRzlPU0dKaklpd2lhV1FpT2lKeWJtcFRUbmN5YmkxMGVIQktSM0ZXUW5VMVdGaHZSM1JoZW1Gbk5IbFhkRGR2WkZCR2VEUXhURWh2SWl3aVlYVjBhSE5sYkdWamRHbHZiaUk2SWlJc0ltRjBkR1Z6ZEdGMGFXOXVJam9pWkdseVpXTjBJbjAubUEtNW9nb0lSY0tKR3E5LTI4RjJUQVlTT0ZHdENnaTk4a2tZODZTM1NmNA\",\"excludeCredentials\":[],\"status\":\"ok\",\"errorMessage\":\"\"}";

    String requestOptionsJson =
            "{"
                    + "\"challenge\":\"YOUR_BASE64URL_CHALLENGE_FROM_SERVER\","
                    + "\"timeout\":7200000,"
                    + "\"rpId\":\"1k-dev.1kosmos.net\","
                    + "\"allowCredentials\":["
                    + "    {"
                    + "        \"type\":\"public-key\","
                    + "        \"id\":\"BASE64URL_CREDENTIAL_ID_FROM_REGISTRATION\""
                    + "    }"
                    + "],"
                    + "\"userVerification\":\"preferred\""
                    + "}";


//    private static final String REQUEST_OPTIONS_JSON = "{"
//            + "\"challenge\":\"dGVzdC1sb2dpbi1jaGFsbGVuZ2UtNDU2\","
//            + "\"rpId\":\"1k-dev.1kosmos.net\","
//            + "\"userVerification\":\"discouraged\""
//            + "}";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passkey);

        // Optional: setContentView(R.layout.activity_main); and wire UI buttons
        passkeyClient = new PasskeyClient(this);
        edtUserName = findViewById(R.id.edt_username);
        edtPassword = findViewById(R.id.edt_psw);

        btnRegister = findViewById(R.id.register);
        btnAuth = findViewById(R.id.auth);

        String username = edtUserName.getText().toString();
        String psw = edtPassword.getText().toString();

        btnRegister.setOnClickListener(v -> {
            BlockIDSDK.getInstance().attestationOption(this,
                    "https://1k-dev.1kosmos.net/webauthn",
                    "8a7O4b7Q46BPHKrMjfZhl/azy4eOT1rKDI3NmQIYenDcm4uVyu95wqWl4EHRD86aKmc2y00KWrasWTrc/QzqWg==",
                    new ApiResponseCallback<FIDO2NativeAPIs.AttestationOptionsData>() {
                        @Override
                        public void apiResponse(boolean status, String message, ErrorManager.ErrorResponse error,
                                                FIDO2NativeAPIs.AttestationOptionsData result) {

                            Log.e("pankti", "status " + status);
                            if (status) {
                                Log.e("pankti", BIDUtil.objectToJSONString(result, true));
                                // If already registered then the register id will be present in excluded list.
                                // usually we need to call getPublicKeyCredential to get the already register id.
                                // if we want to block duplicate registration if we don't want to block it then.
                                // we have to pass an empty array list to exclude creds to allow duplicate registration.
                                result.excludeCredentials = new ArrayList<>();
                                creationOptionsJson = BIDUtil.objectToJSONString(result, true);
                                register();
                            } else {
                                runOnUiThread(() -> Toast.makeText(PasskeyActivity.this, "attestationOption fail", Toast.LENGTH_SHORT).show());
                            }
                        }
                    });
        });
        btnAuth.setOnClickListener(v -> {
            auth();
        });
    }


    private void register() {
        // Example: call register (create) directly
        passkeyClient.registerPasskey(
                this,
                creationOptionsJson,
                new PasskeyClient.JsonCallback() {
                    @Override
                    public void onSuccess(@NonNull String webAuthnJson) {
                        Log.e("pankti", "Registration JSON received: " + webAuthnJson);
                        callResult(webAuthnJson);
                        runOnUiThread(() -> Toast.makeText(PasskeyActivity.this, "Passkey created (check log)", Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        Log.e("pankti", "Registration failed", e);
                        runOnUiThread(() -> Toast.makeText(PasskeyActivity.this, "Create failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                }
        );
    }

    private void auth() {
        passkeyClient.signInWithPasskey(
                this,
                requestOptionsJson,
                new PasskeyClient.JsonCallback() {
                    @Override
                    public void onSuccess(@NonNull String webAuthnJson) {
                        Log.e("pankti", "Assertion JSON received: " + webAuthnJson);
                        runOnUiThread(() -> Toast.makeText(PasskeyActivity.this, "Passkey sign-in (check log)", Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        Log.e("pankti", "Sign-in failed", e);
                        runOnUiThread(() -> Toast.makeText(PasskeyActivity.this, "Sign-in failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                },
                new PasskeyClient.PasswordCallback() {
                    @Override
                    public void onPassword(@NonNull String username, @NonNull String password) {
                        // Handle password fallback (if user selected a saved password)
                        Log.e("pankti", "Password fallback chosen: user=" + username);
                        runOnUiThread(() -> Toast.makeText(PasskeyActivity.this, "Password chosen: " + username, Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private void callResult(String result) {
        try {
            JSONObject jsonObject = new JSONObject(result);

            FIDO2NativeAPIs.AttestationResultRequest request = new FIDO2NativeAPIs.AttestationResultRequest();
            request.rawId = jsonObject.getString("rawId");
            request.authenticatorAttachment = jsonObject.getString("authenticatorAttachment");
            request.attestationResultRequestId = jsonObject.getString("id");
            request.type = jsonObject.getString("type");
            request.dns = "1k-dev.1kosmos.net";
            request.communityId = "68418b2587942f1d3158a799";
            request.tenantId = "68418b2587942f1d3158a789";

            JSONObject jsonObject1 = jsonObject.getJSONObject("response");
            FIDO2NativeAPIs.AuthenticatorResponse response = new FIDO2NativeAPIs.AuthenticatorResponse();

            response.clientDataJSON = jsonObject1.getString("clientDataJSON");
            response.attestationObject = jsonObject1.getString("attestationObject");
            request.response = response;

            BlockIDSDK.getInstance().callAttestationResult(this,
                    "https://1k-dev.1kosmos.net/webauthn",
                    "8a7O4b7Q46BPHKrMjfZhl/azy4eOT1rKDI3NmQIYenDcm4uVyu95wqWl4EHRD86aKmc2y00KWrasWTrc/QzqWg==",
                    request, new ApiResponseCallback<FIDO2NativeAPIs.ResultResponse>() {
                        @Override
                        public void apiResponse(boolean status, String message, ErrorManager.ErrorResponse error, FIDO2NativeAPIs.ResultResponse result) {
                            Log.e("pankti", "status2: " + status);
                            if (status) {
                                Log.e("pankti", BIDUtil.objectToJSONString(result, true));
                                creationOptionsJson = BIDUtil.objectToJSONString(result, true);
                                runOnUiThread(() -> Toast.makeText(PasskeyActivity.this, "Registration successfully: " + result.sub, Toast.LENGTH_SHORT).show());
                            } else {
                                runOnUiThread(() -> Toast.makeText(PasskeyActivity.this, "callAttestationResult fail", Toast.LENGTH_SHORT).show());
                            }

                        }
                    }
            );

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}