package com.onekosmos.blockidsample.ui.qrAuth;

import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_CONNECTION_ERROR;
import static com.onekosmos.blockid.sdk.document.BIDDocumentProvider.RegisterDocCategory.identity_document;
import static com.onekosmos.blockid.sdk.document.RegisterDocType.DL;
import static com.onekosmos.blockid.sdk.document.RegisterDocType.NATIONAL_ID;
import static com.onekosmos.blockid.sdk.document.RegisterDocType.PPT;
import static com.onekosmos.blockidsample.ui.liveID.LiveIDScanningActivity.IS_FROM_AUTHENTICATE;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.gson.Gson;
import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.authentication.BIDAuthProvider;
import com.onekosmos.blockid.sdk.authentication.biometric.IBiometricResponseListener;
import com.onekosmos.blockid.sdk.document.BIDDocumentProvider;
import com.onekosmos.blockidsample.BuildConfig;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.ui.liveID.LiveIDScanningActivity;
import com.onekosmos.blockidsample.ui.login.PinVerificationActivity;
import com.onekosmos.blockidsample.util.AppPermissionUtils;
import com.onekosmos.blockidsample.util.CurrentLocationHelper;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.ProgressDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class AuthenticatorActivity extends AppCompatActivity {
    private static final String K_AUTH_REQUEST_MODEL = "K_AUTH_REQUEST_MODEL";
    private static final String K_FACE = "face";
    private static final String K_PIN = "pin";
    private static final String K_FINGERPRINT = "fingerprint";
    private static final String K_NONE = "none";
    private static final String K_WEBAUTHN_CHALLENGE = "webauthn_challenge";
    private final String[] K_LOCATION_PERMISSION = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION};
    private static final int K_LOCATION_PERMISSION_REQUEST_CODE = 1041;
    private CurrentLocationHelper mCurrentLocationHelper;
    private GoogleApiClient mGoogleApiClient;
    private double mLatitude = 0.0, mLongitude = 0.0;
    private AppCompatButton mBtnQRScope, mBtnQRPresetData, mBtnAuthenticate;
    private AppCompatEditText mEtPresetData;
    private AuthenticationPayloadV1 mAuthenticationPayloadV1 = new AuthenticationPayloadV1();
    private RecyclerView mRvUserScope;
    private boolean mScanQRWithScope = false;
    private String authFactorType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authenticator);
        initView();
        mCurrentLocationHelper = new CurrentLocationHelper(this);
        if (!mCurrentLocationHelper.isGooglePlayServicesAvailable()) {
            finish();
        }
        mCurrentLocationHelper.createLocationRequest();
        if (!AppPermissionUtils.isPermissionGiven(K_LOCATION_PERMISSION, this))
            AppPermissionUtils.requestPermission(this, K_LOCATION_PERMISSION_REQUEST_CODE,
                    K_LOCATION_PERMISSION);
        else {
            mGoogleApiClient = mCurrentLocationHelper.getGoogleApiClient(this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null && checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mGoogleApiClient.connect();
            setLocation();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            mCurrentLocationHelper.stopLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (AppPermissionUtils.isGrantedPermission(this, requestCode, grantResults,
                K_LOCATION_PERMISSION)) {
            mGoogleApiClient = mCurrentLocationHelper.getGoogleApiClient(this);
            setLocation();
        }
    }

    private void initView() {
        mBtnQRScope = findViewById(R.id.btn_qr_scope);
        mBtnQRScope.setOnClickListener(view -> {
            mScanQRWithScope = true;
            startScanQRCodeActivity();
        });
        mBtnQRPresetData = findViewById(R.id.btn_qr_preset_data);
        mBtnQRPresetData.setOnClickListener(view -> {
            mScanQRWithScope = false;
            startScanQRCodeActivity();
        });
        mBtnAuthenticate = findViewById(R.id.btn_authenticate);
        mBtnAuthenticate.setOnClickListener(view -> authenticate());
        mEtPresetData = findViewById(R.id.et_qr_preset_data);

        mRvUserScope = findViewById(R.id.rv_user_scope);
        mRvUserScope.setNestedScrollingEnabled(false);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mRvUserScope.setLayoutManager(layoutManager);
    }

    private void setLocation() {
        if (mGoogleApiClient != null) {
            Location mLocation = mCurrentLocationHelper.getLocation();
            if (mLocation != null) {
                mLatitude = mLocation.getLatitude();
                mLongitude = mLocation.getLongitude();
            }
        }
    }

    private void updateAuthenticateUI() {
        mBtnQRScope.setVisibility(View.GONE);
        mBtnQRPresetData.setVisibility(View.GONE);
        mBtnAuthenticate.setVisibility(View.VISIBLE);
        if (mScanQRWithScope) {
            mRvUserScope.setVisibility(View.VISIBLE);
            mEtPresetData.setVisibility(View.GONE);
        } else {
            mRvUserScope.setVisibility(View.GONE);
            mEtPresetData.setVisibility(View.VISIBLE);
        }
    }

    private final ActivityResultLauncher<Intent> scanQRActivityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            updateAuthenticateUI();
                            mAuthenticationPayloadV1 = new Gson().fromJson(
                                    result.getData().getStringExtra(K_AUTH_REQUEST_MODEL),
                                    AuthenticationPayloadV1.class);
                            ProgressDialog progressDialog = new ProgressDialog(this);
                            progressDialog.show();
                            BlockIDSDK.getInstance().getScopes(
                                    null, mAuthenticationPayloadV1.scopes,
                                    mAuthenticationPayloadV1.creds,
                                    mAuthenticationPayloadV1.getOrigin(),
                                    String.valueOf(mLatitude), String.valueOf(mLongitude),
                                    (linkedHashMap, errorResponse) -> {
                                        progressDialog.dismiss();
                                        if (linkedHashMap != null) {
                                            LinkedHashMap<String, Object> mDisplayScopes =
                                                    changeDisplayName(linkedHashMap);
                                            if (mDisplayScopes != null) {
                                                StringBuilder stringBuilder = new StringBuilder();
                                                for (String key : mDisplayScopes.keySet()) {
                                                    stringBuilder.append(key).append(" : ").
                                                            append(mDisplayScopes.get(key)).
                                                            append("\n");
                                                }
                                                UserScopeAdapter mUserScopeAdapter =
                                                        new UserScopeAdapter(mDisplayScopes);
                                                mRvUserScope.setAdapter(mUserScopeAdapter);
                                            }
                                            return;
                                        }
                                        ErrorDialog errorDialog = new ErrorDialog(
                                                AuthenticatorActivity.this);
                                        errorDialog.show(null,
                                                getString(R.string.label_error),
                                                errorResponse.getMessage(), dialogInterface -> {
                                                    errorDialog.dismiss();
                                                    finish();
                                                });
                                    });
                        } else {
                            finish();
                        }
                    });

    private void startScanQRCodeActivity() {
        Intent scanQRCodeIntent = new Intent(this, ScanQRCodeActivity.class);
        scanQRCodeIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        scanQRActivityResultLauncher.launch(scanQRCodeIntent);
    }

    private void authenticate() {
        mBtnAuthenticate.setClickable(false);
        verifyAuth(mAuthenticationPayloadV1.authType);
    }

    private void verifyAuth(String authType) {
        switch (authType.toLowerCase()) {
            case K_FACE:
                if (BlockIDSDK.getInstance().isLiveIDRegistered())
                    startLiveIDVerification();
                else {
                    Toast.makeText(this,
                            R.string.label_enroll_liveid_in_order_to_authenticate,
                            Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            case K_PIN:
                if (BlockIDSDK.getInstance().isPinRegistered()) {
                    startPinVerification();
                } else {
                    Toast.makeText(this,
                            R.string.label_enroll_pin_in_order_to_authenticate,
                            Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            case K_FINGERPRINT:
                if (BlockIDSDK.getInstance().isDeviceAuthEnrolled()) {
                    startVerifyDeviceAuth();
                } else {
                    Toast.makeText(this,
                            R.string.label_enroll_device_auth_in_order_to_authenticate,
                            Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            default:
                onSuccessFullVerification(K_NONE);
                break;
        }
    }

    private void startVerifyDeviceAuth() {
        BIDAuthProvider.getInstance().verifyDeviceAuth(this,
                getResources().getString(R.string.label_biometric_auth),
                getResources().getString(R.string.label_biometric_auth_req), false,
                new IBiometricResponseListener() {
                    @Override
                    public void onBiometricAuthResult(boolean status,
                                                      ErrorManager.ErrorResponse errorResponse) {
                        if (status)
                            onSuccessFullVerification(K_FINGERPRINT);
                        else
                            mBtnAuthenticate.setClickable(true);
                    }

                    @Override
                    public void onNonBiometricAuth(boolean status) {
                        if (status)
                            onSuccessFullVerification(K_FINGERPRINT);
                        else
                            mBtnAuthenticate.setClickable(true);
                    }
                });
    }

    private final ActivityResultLauncher<Intent> verifyAuthResultLauncher =
            registerForActivityResult(new
                    ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    onSuccessFullVerification(authFactorType);
                } else {
                    mBtnAuthenticate.setClickable(true);
                }
            });

    private void startPinVerification() {
        Intent scanLiveIdIntent = new Intent(this, PinVerificationActivity.class);
        scanLiveIdIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        scanLiveIdIntent.putExtra(IS_FROM_AUTHENTICATE, true);
        authFactorType = K_PIN;
        verifyAuthResultLauncher.launch(scanLiveIdIntent);
    }

    private void startLiveIDVerification() {
        Intent scanLiveIdIntent = new Intent(this, LiveIDScanningActivity.class);
        scanLiveIdIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        scanLiveIdIntent.putExtra(IS_FROM_AUTHENTICATE, true);
        authFactorType = K_FACE;
        verifyAuthResultLauncher.launch(scanLiveIdIntent);
    }

    private void onSuccessFullVerification(String authFactor) {
        if (mScanQRWithScope) {
            callAuthenticateService(mAuthenticationPayloadV1, mLatitude, mLongitude, authFactor);
        } else {
            String presetData = Objects.requireNonNull(mEtPresetData.getText()).toString();
            LinkedHashMap<String, Object> dataObject = new LinkedHashMap<>();
            dataObject.put("data", presetData);
            callAuthenticateService(mAuthenticationPayloadV1, dataObject, mLatitude, mLongitude,
                    authFactor);
        }
    }

    // authenticate user with scope
    private void callAuthenticateService(AuthenticationPayloadV1 authenticationPayloadV1,
                                         double latitude, double longitude, String authFactor) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();

        LinkedHashMap<String, Object> metadata = null;
        if (mAuthenticationPayloadV1.metadata != null &&
                mAuthenticationPayloadV1.metadata.webauthn_challenge != null) {
            metadata = new LinkedHashMap<>();
            metadata.put(K_WEBAUTHN_CHALLENGE,
                    mAuthenticationPayloadV1.metadata.webauthn_challenge);
        }
        if (authFactor.equals(K_FACE)) authFactor = "LiveID";
        if (authFactor.equals(K_FINGERPRINT)) authFactor = "Biometric";
        BlockIDSDK.getInstance().authenticateUser(this, null,
                authenticationPayloadV1.session, mAuthenticationPayloadV1.sessionURL,
                authenticationPayloadV1.scopes, metadata, authenticationPayloadV1.creds,
                authenticationPayloadV1.getOrigin(), String.valueOf(latitude),
                String.valueOf(longitude), BuildConfig.VERSION_NAME, null, authFactor,
                (status, sessionId, error) -> {
                    mBtnAuthenticate.setClickable(true);
                    progressDialog.dismiss();
                    onUserAuthenticated(status, error);
                });
    }

    // authenticate user with pre-set data
    private void callAuthenticateService(AuthenticationPayloadV1 authenticationPayloadV1,
                                         LinkedHashMap<String, Object> dataObject,
                                         double latitude, double longitude, String authFactor) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();
        if (authFactor.equals(K_FACE)) authFactor = "LiveID";
        if (authFactor.equals(K_FINGERPRINT)) authFactor = "Biometric";
        BlockIDSDK.getInstance().authenticateUser(null, authenticationPayloadV1.session,
                authenticationPayloadV1.sessionURL, dataObject, authenticationPayloadV1.creds,
                authenticationPayloadV1.getOrigin(), String.valueOf(latitude),
                String.valueOf(longitude), BuildConfig.VERSION_NAME, authFactor,
                (status, sessionId, error) -> {
                    mBtnAuthenticate.setClickable(true);
                    progressDialog.dismiss();
                    onUserAuthenticated(status, error);
                });
    }

    private void onUserAuthenticated(boolean status, ErrorManager.ErrorResponse error) {
        if (status) {
            Toast.makeText(this, R.string.label_you_have_successfully_authenticated_to_log_in,
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ErrorDialog errorDialog = new ErrorDialog(this);
        DialogInterface.OnDismissListener onDismissListener = dialogInterface -> {
            errorDialog.dismiss();
            finish();
        };

        if (error.getCode() == K_CONNECTION_ERROR.getCode()) {
            errorDialog.showNoInternetDialog(onDismissListener);
            return;
        }
        String message = error.getMessage();
        if (message == null) {
            message = "Server Error (" + error.getCode() + ")";
        }
        errorDialog.show(null,
                getString(R.string.label_error),
                message, onDismissListener);
    }

    private LinkedHashMap<String, Object> changeDisplayName
            (HashMap<String, Object> scopesMap) {
        LinkedHashMap<String, Object> pScopesMap = new LinkedHashMap<>();
        try {
            if (scopesMap != null) {
                if (isAnyDocumentEnrolled()) {
                    if (scopesMap.containsKey("firstname") && scopesMap.containsKey("lastname"))
                        pScopesMap.put("Name : ", scopesMap.get("firstname") + " " +
                                scopesMap.get("lastname"));

                    else if (scopesMap.containsKey("firstname"))
                        pScopesMap.put("Name : ", scopesMap.get("firstname"));

                    else if (scopesMap.containsKey("lastname"))
                        pScopesMap.put("Name : ", scopesMap.get("lastname"));
                }

                if (scopesMap.containsKey("did"))
                    pScopesMap.put("DID : ", scopesMap.get("did"));

                if (scopesMap.containsKey("userid"))
                    pScopesMap.put("User ID : ", scopesMap.get("userid"));

                if (scopesMap.containsKey("ppt")) {
                    pScopesMap.put("Passport # : ", ((JSONObject)
                            Objects.requireNonNull(scopesMap.get("ppt"))).get("documentId"));
                }

                if (scopesMap.containsKey("nationalid")) {
                    pScopesMap.put("National ID # : ", ((JSONObject)
                            Objects.requireNonNull(scopesMap.get("nationalid"))).get("documentId"));
                }

                if (scopesMap.containsKey("dl")) {
                    pScopesMap.put("Drivers license # : ", ((JSONObject)
                            Objects.requireNonNull(scopesMap.get("dl"))).get("documentId"));
                }

                if (scopesMap.containsKey("scep_creds"))
                    pScopesMap.put("SCEP : ", scopesMap.get("scep_creds"));

                if (scopesMap.containsKey("creds"))
                    pScopesMap.put("Creds : ", scopesMap.get("creds"));
            }
        } catch (JSONException e) {
            return null;
        }
        return pScopesMap;
    }

    private boolean isAnyDocumentEnrolled() {
        return BIDDocumentProvider.getInstance().isDocumentEnrolled(PPT.getValue(),
                identity_document.name()) ||
                BIDDocumentProvider.getInstance().isDocumentEnrolled(DL.getValue(),
                        identity_document.name()) ||
                BIDDocumentProvider.getInstance().isDocumentEnrolled(NATIONAL_ID.getValue(),
                        identity_document.name());
    }
}