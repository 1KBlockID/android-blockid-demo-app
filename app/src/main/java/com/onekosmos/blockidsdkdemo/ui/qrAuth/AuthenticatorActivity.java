package com.onekosmos.blockidsdkdemo.ui.qrAuth;

import android.Manifest;
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

import com.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.blockid.sdk.BlockIDSDK;
import com.blockid.sdk.cameramodule.camera.dlModule.model.DriverLicenseData;
import com.blockid.sdk.cameramodule.camera.passportModule.model.PassportData;
import com.blockid.sdk.datamodel.BIDGenericResponse;
import com.blockid.sdk.document.BIDDocumentProvider;
import com.blockid.sdk.scopeprovider.BIDScopesProvider;
import com.example.blockidsdkdemo.BuildConfig;
import com.example.blockidsdkdemo.R;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.gson.Gson;
import com.onekosmos.blockidsdkdemo.util.AppPermissionUtils;
import com.onekosmos.blockidsdkdemo.util.CurrentLocationHelper;
import com.onekosmos.blockidsdkdemo.util.ErrorDialog;
import com.onekosmos.blockidsdkdemo.util.ProgressDialog;

import java.util.HashMap;
import java.util.LinkedHashMap;

import static com.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_CONNECTION_ERROR;

/**
 * Created by Pankti Mistry on 04-04-2021.
 * Copyright © 2020 1Kosmos. All rights reserved.
 */
public class AuthenticatorActivity extends AppCompatActivity {
    private static final String K_AUTH_REQUEST_MODEL = "authRequestModel";
    private static final String K_USER_ID = "user_id";
    private static final int K_SCAN_QR_REQUEST_CODE = 1112;
    private static final int K_USER_CONSENT_REQUEST_CODE = 1113;
    private CurrentLocationHelper mCurrentLocationHelper;
    private Location mLocation;
    private final String[] K_LOCATION_PERMISSION = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION};
    private static final int K_LOCATION_PERMISSION_REQUEST_CODE = 1041;
    private GoogleApiClient mGoogleApiClient;
    private double mLatitude = 0, mLongitude = 0;
    private AppCompatButton mBtnQRSession1, mBtnQRSession2, mBtnAuthenticate;
    private AppCompatEditText mEtPresetData;
    private AuthRequestModel mAuthRequestModel = new AuthRequestModel();
    private String mUserId;
    private LinkedHashMap<String, Object> mDisplayScopes = new LinkedHashMap<>();
    private RecyclerView mRvUserScope;
    private UserScopeAdapter mUserScopeAdapter;
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
            AppPermissionUtils.requestPermission(this, K_LOCATION_PERMISSION_REQUEST_CODE, K_LOCATION_PERMISSION);
        else {
            mGoogleApiClient = mCurrentLocationHelper.getGoogleApiClient(this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mGoogleApiClient.connect();
            setLocation();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mCurrentLocationHelper.stopLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (AppPermissionUtils.isGrantedPermission(this, requestCode, grantResults, K_LOCATION_PERMISSION)) {
            mGoogleApiClient = mCurrentLocationHelper.getGoogleApiClient(this);
            setLocation();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == K_SCAN_QR_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            updateAuthenticateUi();
            mAuthRequestModel = new Gson().fromJson(data.getStringExtra(K_AUTH_REQUEST_MODEL), AuthRequestModel.class);
            mUserId = getIntent().hasExtra(K_USER_ID) ? getIntent().getStringExtra(K_USER_ID) : null;
            BIDGenericResponse response =
                    BlockIDSDK.getInstance().getScopes(mUserId, mAuthRequestModel.scopes, mAuthRequestModel.creds, mAuthRequestModel.getOrigin(), String.valueOf(mLatitude),
                            String.valueOf(mLongitude));

            if (response != null) {
                mDisplayScopes = changeDisplayName(response.getDataObject());
                StringBuilder stringBuilder = new StringBuilder();
                for (String s : mDisplayScopes.keySet()) {
                    stringBuilder.append(s + " : " + mDisplayScopes.get(s) + "\n");
                }
                mUserScopeAdapter = new UserScopeAdapter(mDisplayScopes);
                mRvUserScope.setAdapter(mUserScopeAdapter);
            }
        } else if (requestCode == K_USER_CONSENT_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            AuthRequestModel authRequestModel = new Gson().fromJson(data.getStringExtra(K_AUTH_REQUEST_MODEL), AuthRequestModel.class);
            double lat = data.getDoubleExtra("lat", 0);
            double lon = data.getDoubleExtra("lon", 0);
            callAuthenticateService(data.getStringExtra(K_USER_ID), authRequestModel
                    , lat
                    , lon);
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
            mLocation = mCurrentLocationHelper.getLocation();
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
        Intent i = new Intent(this, ScanQRCodeActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivityForResult(i, K_SCAN_QR_REQUEST_CODE);
    }

    private void authenticate() {
        mBtnAuthenticate.setClickable(false);
        if (mScanQRWithScope) {
            callAuthenticateService(mUserId, mAuthRequestModel, mLatitude, mLongitude);
        } else {
            String presetData = mEtPresetData.getText().toString();
            LinkedHashMap<String, Object> dataObject = new LinkedHashMap<>();
            dataObject.put("data", presetData);
            callAuthenticateService(mUserId, mAuthRequestModel, dataObject, mLatitude, mLongitude);
        }
    }

    private void callAuthenticateService(String userID, AuthRequestModel authRequestModel, double lat, double lon) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();
        BlockIDSDK.getInstance().authenticateUser(userID, authRequestModel.session, authRequestModel.scopes, authRequestModel.creds,
                authRequestModel.getOrigin(), String.valueOf(lat), String.valueOf(lon), BuildConfig.VERSION_NAME, (status, error) -> {
                    mBtnAuthenticate.setClickable(true);
                    progressDialog.dismiss();
                    onUserAuthenticated(status, error);
                });
    }

    private void callAuthenticateService(String userID, AuthRequestModel authRequestModel, LinkedHashMap<String, Object> dataObject, double lat, double lon) {
        BlockIDSDK.getInstance().authenticateUser(userID, authRequestModel.session, dataObject, authRequestModel.creds,
                authRequestModel.getOrigin(), String.valueOf(lat), String.valueOf(lon), BuildConfig.VERSION_NAME, (status, error) -> {
                    onUserAuthenticated(status, error);
                });
    }

    private void onUserAuthenticated(boolean status, ErrorManager.ErrorResponse error) {
        if (status) {
            Toast.makeText(this, R.string.label_you_have_successfully_authenticated_to_log_in, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            ErrorDialog errorDialog = new ErrorDialog(this);
            if (error.getCode() == K_CONNECTION_ERROR.getCode()) {
                errorDialog.show(null,
                        getString(R.string.label_error),
                        error.getMessage(), dialog -> {
                            finish();
                        });
            } else {
                String message = error.getMessage();
                if (message == null) {
                    message = "Server Error (" + error.getCode() + ")";
                }
                errorDialog.show(null,
                        getString(R.string.label_error),
                        message, dialog -> {
                            finish();
                        });
            }
        }
    }

    private LinkedHashMap<String, Object> changeDisplayName(HashMap<String, Object> scopesMap) {
        LinkedHashMap<String, Object> pScopesMap = new LinkedHashMap<String, Object>();
        if (isAnyDocumentEnrolled()) {
            if (scopesMap.containsKey("firstname") && scopesMap.containsKey("lastname"))
                pScopesMap.put("Name : ", scopesMap.get("firstname") + " " + scopesMap.get("lastname"));

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
            pScopesMap.put("Passport # : ", ((PassportData) scopesMap.get("ppt")).getDocumentId());
        }

        if (scopesMap.containsKey("nationalid")) {
            pScopesMap.put("National ID # : ", ((BIDScopesProvider.NationalIDScope) scopesMap.get("nationalid")).getDocumentId());
        }

        if (scopesMap.containsKey("dl"))
            pScopesMap.put("Drivers license # : ", ((DriverLicenseData) scopesMap.get("dl")).getDocumentId());

        if (scopesMap.containsKey("scep_creds"))
            pScopesMap.put("SCEP : ", scopesMap.get("scep_creds"));

        if (scopesMap.containsKey("creds"))
            pScopesMap.put("Creds : ", scopesMap.get("creds"));

        return pScopesMap;
    }

    private boolean isAnyDocumentEnrolled() {
        if (BIDDocumentProvider.getInstance().isDocumentEnrolled(BIDDocumentProvider.BIDDocumentType.driverLicense) ||
                BIDDocumentProvider.getInstance().isDocumentEnrolled(BIDDocumentProvider.BIDDocumentType.passport) ||
                BIDDocumentProvider.getInstance().isDocumentEnrolled(BIDDocumentProvider.BIDDocumentType.nationalID
                ))
            return true;
        else return false;
    }
}