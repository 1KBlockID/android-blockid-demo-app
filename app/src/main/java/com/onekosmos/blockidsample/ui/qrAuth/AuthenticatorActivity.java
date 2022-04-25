package com.onekosmos.blockidsample.ui.qrAuth;

import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_CONNECTION_ERROR;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.gson.Gson;
import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.datamodel.BIDGenericResponse;
import com.onekosmos.blockidsample.BuildConfig;
import com.onekosmos.blockidsample.R;
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
    private static final int K_SCAN_QR_REQUEST_CODE = 1112;
    private CurrentLocationHelper mCurrentLocationHelper;
    private final String[] K_LOCATION_PERMISSION = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION};
    private static final int K_LOCATION_PERMISSION_REQUEST_CODE = 1041;
    private GoogleApiClient mGoogleApiClient;
    private double mLatitude = 0, mLongitude = 0;
    private AppCompatButton mBtnQRSession1, mBtnQRSession2, mBtnAuthenticate;
    private AppCompatEditText mEtPresetData;
    private AuthRequestModel mAuthRequestModel = new AuthRequestModel();
    private RecyclerView mRvUserScope;
    private boolean mScanQRWithScope = false;

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == K_SCAN_QR_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            updateAuthenticateUi();
            mAuthRequestModel = new Gson().fromJson(data.getStringExtra(K_AUTH_REQUEST_MODEL),
                    AuthRequestModel.class);
            BIDGenericResponse response =
                    BlockIDSDK.getInstance().getScopes(null, mAuthRequestModel.scopes,
                            mAuthRequestModel.creds, mAuthRequestModel.getOrigin(),
                            String.valueOf(mLatitude), String.valueOf(mLongitude));

            if (response != null) {
                LinkedHashMap<String, Object> mDisplayScopes = changeDisplayName(
                        response.getDataObject());
                if (mDisplayScopes != null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    for (String key : mDisplayScopes.keySet()) {
                        stringBuilder.append(key).append(" : ").append(mDisplayScopes.get(key)).
                                append("\n");
                    }
                    UserScopeAdapter mUserScopeAdapter = new UserScopeAdapter(mDisplayScopes);
                    mRvUserScope.setAdapter(mUserScopeAdapter);
                }
            }
        } else {
            finish();
        }
    }

    private void initView() {
        mBtnQRSession1 = findViewById(R.id.btn_qr_session1);
        mBtnQRSession1.setOnClickListener(view -> {
            mScanQRWithScope = true;
            startScanQRCodeActivity();
        });
        mBtnQRSession2 = findViewById(R.id.btn_qr_session2);
        mBtnQRSession2.setOnClickListener(view -> {
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

    private void updateAuthenticateUi() {
        mBtnQRSession1.setVisibility(View.GONE);
        mBtnQRSession2.setVisibility(View.GONE);
        mBtnAuthenticate.setVisibility(View.VISIBLE);
        if (mScanQRWithScope) {
            mRvUserScope.setVisibility(View.VISIBLE);
            mEtPresetData.setVisibility(View.GONE);
        } else {
            mRvUserScope.setVisibility(View.GONE);
            mEtPresetData.setVisibility(View.VISIBLE);
        }
    }

    private void startScanQRCodeActivity() {
        Intent scanQRCodeActivity = new Intent(this, ScanQRCodeActivity.class);
        scanQRCodeActivity.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivityForResult(scanQRCodeActivity, K_SCAN_QR_REQUEST_CODE);
    }

    private void authenticate() {
        mBtnAuthenticate.setClickable(false);
        if (mScanQRWithScope) {
            callAuthenticateService(mAuthRequestModel, mLatitude, mLongitude);
        } else {
            String presetData = Objects.requireNonNull(mEtPresetData.getText()).toString();
            LinkedHashMap<String, Object> dataObject = new LinkedHashMap<>();
            dataObject.put("data", presetData);
            callAuthenticateService(mAuthRequestModel, dataObject, mLatitude, mLongitude);
        }
    }

    // authenticate user with scope
    private void callAuthenticateService(AuthRequestModel authRequestModel, double latitude,
                                         double longitude) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();
        BlockIDSDK.getInstance().authenticateUser(null, authRequestModel.session,
                mAuthRequestModel.sessionURL, authRequestModel.scopes, authRequestModel.creds,
                authRequestModel.getOrigin(), String.valueOf(latitude), String.valueOf(longitude),
                BuildConfig.VERSION_NAME, (status, sessionId, error) -> {
                    mBtnAuthenticate.setClickable(true);
                    progressDialog.dismiss();
                    onUserAuthenticated(status, error);
                });
    }

    // authenticate user with pre-set data
    private void callAuthenticateService(AuthRequestModel authRequestModel,
                                         LinkedHashMap<String, Object> dataObject,
                                         double latitude, double longitude) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();
        BlockIDSDK.getInstance().authenticateUser(null, authRequestModel.session,
                authRequestModel.sessionURL, dataObject, authRequestModel.creds,
                authRequestModel.getOrigin(), String.valueOf(latitude),
                String.valueOf(longitude), BuildConfig.VERSION_NAME, (status, sessionId, error) -> {
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
        return BlockIDSDK.getInstance().isPassportEnrolled() ||
                BlockIDSDK.getInstance().isDriversLicenseEnrolled() ||
                BlockIDSDK.getInstance().isNationalIDEnrolled();
    }
}