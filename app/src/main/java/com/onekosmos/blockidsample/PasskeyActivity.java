package com.onekosmos.blockidsample;

import android.os.Bundle;
import android.util.Base64;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.SecureRandom;
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

    String creationOptionsJson;

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
                                //result.excludeCredentials = new ArrayList<>();
                                creationOptionsJson = BIDUtil.objectToJSONString(result, true);
                                register();
                            } else {
                                runOnUiThread(() -> Toast.makeText(PasskeyActivity.this, "attestationOption fail", Toast.LENGTH_SHORT).show());
                            }
                        }
                    });
        });
        btnAuth.setOnClickListener(v -> {
            BlockIDSDK.getInstance().assertionOption(this, "https://1k-dev.1kosmos.net/webauthn",
                    "8a7O4b7Q46BPHKrMjfZhl/azy4eOT1rKDI3NmQIYenDcm4uVyu95wqWl4EHRD86aKmc2y00KWrasWTrc/QzqWg==", new ApiResponseCallback<String>() {
                        @Override
                        public void apiResponse(boolean status, String message, ErrorManager.ErrorResponse error, String result) {
                            Log.e("pankti", "status " + status);
                            if (status) {
                                Log.e("pankti", "assertionOption: " + BIDUtil.objectToJSONString(result, true));
                                auth(result);
                            } else {
                                runOnUiThread(() -> Toast.makeText(PasskeyActivity.this, "assertionOption fail", Toast.LENGTH_SHORT).show());
                            }
                        }
                    });
        });
    }


    private void register() {
        try {
//            JSONObject options = new JSONObject();
//            options.put("rp", new JSONObject()
//                    .put("name", "BlockID App") // 1k-dev.1kosmos.net
//                    .put("id", "1k-dev.1kosmos.net"));
//
//            options.put("user", new JSONObject()
//                    .put("id", Base64.encodeToString("12345".getBytes(),
//                            Base64.URL_SAFE | Base64.NO_PADDING))
//                    .put("name", "test@example.com")
//                    .put("displayName", "test example"));
//
//            JSONArray pubKeyCredParams = new JSONArray();
//            pubKeyCredParams.put(
//                    new JSONObject().put("type", "public-key").
//                            put("alg", -7));   // ES256
//            options.put("pubKeyCredParams", pubKeyCredParams);
//
//            options.put("authenticatorSelection", new JSONObject()
//                    .put("authenticatorAttachment", "platform") // not there
//                    .put("residentKey", "required") // not there
//                    .put("userVerification", "required")); // preferred
//
//            options.put("attestation", "none"); // direct
//            options.put("challenge", generateChallenge());
//            options.put("timeout", 60000);
//
//            String optionsString = options.toString();
//            Log.e("mistry", "optionsString: " + optionsString);

            // ZXlKaGJHY2lPaUpJVXpJMU5pSXNJblI1Y0NJNklrcFhWQ0o5LmV5SmhkV1FpT2lJeGF5MWtaWFl1TVd0dmMyMXZjeTV1WlhRaUxDSnpkV0lpT2lKd1lXNXJkR2tpTENKbGVIQWlPakUzTlRVMU9EWXhPVGdzSW5KaGJtUWlPaUoxZDBZd2MyUTFiM2wxV0MwdFZsWkpWbEo0YkVwemMwTlJWMmwwY0haYVowRXlkM1ZoTVdWeUlpd2lhV1FpT2lKQllXRnhUV1ZuVkdaSlZITTRVRk5xTXpJM09VbDBNVWhvVUVOalQwOU9OWFZmTm10RWRUSk5ibEF3SWl3aVlYVjBhSE5sYkdWamRHbHZiaUk2SWlJc0ltRjBkR1Z6ZEdGMGFXOXVJam9pWkdseVpXTjBJbjAuYlVnZmVVbnV6Q0hCVHVyR0ppUFhhMTdsMWFmV0hMbnp5QXpJRVRWSF9uWQ
            // eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiIxay1kZXYuMWtvc21vcy5uZXQiLCJzdWIiOiJwYW5rdGkiLCJleHAiOjE3NTU1ODYxOTgsInJhbmQiOiJ1d0Ywc2Q1b3l1WC0tVlZJVlJ4bEpzc0NRV2l0cHZaZ0Eyd3VhMWVyIiwiaWQiOiJBYWFxTWVnVGZJVHM4UFNqMzI3OUl0MUhoUENjT09ONXVfNmtEdTJNblAwIiwiYXV0aHNlbGVjdGlvbiI6IiIsImF0dGVzdGF0aW9uIjoiZGlyZWN0In0.bUgfeUnuzCHBTurGJiPXa17l1afWHLnzyAzIETVH_nY

            // {
            //  "aud": "1k-dev.1kosmos.net",
            //  "sub": "pankti",
            //  "exp": 1755586198,
            //  "rand": "uwF0sd5oyuX--VVIVRxlJssCQWitpvZgA2wua1er",
            //  "id": "AaaqMegTfITs8PSj3279It1HhPCcOON5u_6kDu2MnP0",
            //  "authselection": "",
            //  "attestation": "direct"
            //}

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
        } catch (Exception e) {

        }
    }

    private void auth(String result) {
        try {
//            JSONObject options = new JSONObject();
//            options.put("challenge", generateChallenge());
//            options.put("timeout", 60000);
//            options.put("rpId", "1k-dev.1kosmos.net");
//            options.put("userVerification", "required");
//
//            String optionsString = options.toString();

            //JSONObject requestOptions = new JSONObject(result);

            // 🚫 Remove allowCredentials if present
            //requestOptions.remove("allowCredentials");

            // Convert back to string
            //String cleanedJson = requestOptions.toString();

//            Log.e("mistry", "auth optionsString: "+ optionsString);
            passkeyClient.signInWithPasskey(
                    this,
                    result,
                    new PasskeyClient.JsonCallback() {
                        @Override
                        public void onSuccess(@NonNull String webAuthnJson) {
                            Log.e("pankti", "Assertion JSON received: " + webAuthnJson);
                            //String fixed =  fixWebAuthnJson(webAuthnJson);
                            //Log.e("pankti", "fixed: " + fixed);
                            BlockIDSDK.getInstance().callAssertionResult(PasskeyActivity.this,
                                    "https://1k-dev.1kosmos.net/webauthn",
                                    "8a7O4b7Q46BPHKrMjfZhl/azy4eOT1rKDI3NmQIYenDcm4uVyu95wqWl4EHRD86aKmc2y00KWrasWTrc/QzqWg==",
                                    webAuthnJson, new ApiResponseCallback<FIDO2NativeAPIs.ResultResponse>() {
                                        @Override
                                        public void apiResponse(boolean status, String message, ErrorManager.ErrorResponse error, FIDO2NativeAPIs.ResultResponse result) {
                                            Log.e("pankti", "status " + status);
                                            if (status) {
                                                Log.e("pankti", BIDUtil.objectToJSONString(result, true));
                                                runOnUiThread(() -> Toast.makeText(PasskeyActivity.this, "callAssertionResult pass: " + result.sub, Toast.LENGTH_SHORT).show());
                                            } else {
                                                runOnUiThread(() -> Toast.makeText(PasskeyActivity.this, "callAssertionResult fail", Toast.LENGTH_SHORT).show());
                                            }
                                        }
                                    });
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
        } catch (Exception e) {

        }
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

    public static String generateChallenge() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] challenge = new byte[32]; // 32 bytes = 256 bits
        secureRandom.nextBytes(challenge);
        return Base64.encodeToString(challenge, Base64.URL_SAFE | Base64.NO_PADDING);
    }

    /**
     * Base64 encode
     *
     * @param input the data to encode
     * @return Base64 encoded string
     */
    private String base64Encode(byte[] input) {
        if (input == null)
            return null;
        return Base64.encodeToString(input, Base64.NO_PADDING | Base64.NO_WRAP |
                Base64.URL_SAFE);
    }

    // Convert to Base64URL without padding or newlines
    private static String toBase64Url(String base64Input) {
        if (base64Input == null || base64Input.isEmpty()) {
            return base64Input;
        }
        try {
            // Remove whitespace/newlines
            String cleaned = base64Input.trim();

            // Decode (handles both standard Base64 and Base64URL with padding)
            byte[] decoded = Base64.decode(cleaned, Base64.DEFAULT);

            // Encode to Base64URL (NO_PADDING | NO_WRAP | URL_SAFE)
            return Base64.encodeToString(decoded, Base64.NO_WRAP | Base64.URL_SAFE);
        } catch (IllegalArgumentException e) {
            // If it's not valid Base64, just clean up
            return base64Input.trim().replace("=", "");
        }
    }

    public static String fixWebAuthnJson(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);

            // Top-level fields
            json.put("id", toBase64Url(json.optString("id")));
            json.put("rawId", toBase64Url(json.optString("rawId")));

            // Response object
            JSONObject response = json.getJSONObject("response");
            response.put("clientDataJSON", toBase64Url(response.optString("clientDataJSON")));
            response.put("authenticatorData", toBase64Url(response.optString("authenticatorData")));
            response.put("signature", toBase64Url(response.optString("signature")));
            response.put("userHandle", toBase64Url(response.optString("userHandle")));


            response.put("dns", "1k-dev.1kosmos.net");
            response.put("communityId", "68418b2587942f1d3158a799");
            response.put("tenantId", "68418b2587942f1d3158a789");

            return json.toString();
        } catch (Exception e) {
            return null;
        }
    }

}