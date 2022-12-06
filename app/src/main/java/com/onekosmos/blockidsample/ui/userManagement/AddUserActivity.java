package com.onekosmos.blockidsample.ui.userManagement;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CAMERA;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.provider.Settings.Secure;
import static android.provider.Settings.Secure.ANDROID_ID;
import static com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_CONNECTION_ERROR;
import static com.onekosmos.blockid.sdk.BIDAPIs.accessCode.AccessCodeAPIs.RESPONSE_CODE_LINK_EXPIRED;
import static com.onekosmos.blockid.sdk.BIDAPIs.accessCode.AccessCodeAPIs.RESPONSE_CODE_LINK_REDEEMED;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.ParsedRequestListener;
import com.google.android.gms.common.api.GoogleApiClient;
import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BIDAPIs.accessCode.GetAccessCodeResponse;
import com.onekosmos.blockid.sdk.BIDAPIs.publicip.GetPublicIP;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.cameramodule.BIDScannerView;
import com.onekosmos.blockid.sdk.cameramodule.QRCodeScanner.QRScannerHelper;
import com.onekosmos.blockid.sdk.cameramodule.ScanningMode;
import com.onekosmos.blockid.sdk.cameramodule.camera.qrCodeModule.IOnQRScanResponseListener;
import com.onekosmos.blockid.sdk.datamodel.AccountAuthConstants;
import com.onekosmos.blockid.sdk.datamodel.BIDAccount;
import com.onekosmos.blockid.sdk.datamodel.BIDGenericResponse;
import com.onekosmos.blockid.sdk.datamodel.BIDLinkedAccount;
import com.onekosmos.blockid.sdk.datamodel.BIDOrigin;
import com.onekosmos.blockid.sdk.fido2.FIDO2KeyType;
import com.onekosmos.blockid.sdk.utils.BIDUtil;
import com.onekosmos.blockidsample.AppConstant;
import com.onekosmos.blockidsample.BuildConfig;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.util.AppPermissionUtils;
import com.onekosmos.blockidsample.util.CurrentLocationHelper;
import com.onekosmos.blockidsample.util.ErrorDialog;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2022 1Kosmos. All rights reserved.
 */
public class AddUserActivity extends AppCompatActivity implements IOnQRScanResponseListener {
    private static final int K_PERMISSION_REQUEST_CODE = 1007;
    private final String[] K_PERMISSIONS = new String[]{CAMERA, ACCESS_FINE_LOCATION};
    private CurrentLocationHelper mCurrentLocationHelper;
    @SuppressWarnings("deprecation")
    private GoogleApiClient mGoogleApiClient;
    private double mLatitude = 0.0, mLongitude = 0.0;
    private LinearLayout mScannerView;
    private BIDScannerView mBIDScannerView;
    private View mScannerOverlay;
    private WebView mWebView;
    private AppCompatTextView mTxtPleaseWait;
    private ProgressBar mProgressBar;
    private QRScannerHelper mQRScannerHelper;
    private String mMagicLink, mAcrPublicKey;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_user);

        mCurrentLocationHelper = new CurrentLocationHelper(this);
        if (!mCurrentLocationHelper.isGooglePlayServicesAvailable()) {
            finish();
        }
        mCurrentLocationHelper.createLocationRequest();
        initView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!AppPermissionUtils.isPermissionGiven(K_PERMISSIONS, this))
            AppPermissionUtils.requestPermission(this, K_PERMISSION_REQUEST_CODE,
                    K_PERMISSIONS);
        else {
            startQRCodeScanning();
            mGoogleApiClient = mCurrentLocationHelper.getGoogleApiClient(this);
            setLocation();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mQRScannerHelper != null && mQRScannerHelper.isRunning()) {
            mQRScannerHelper.stopQRScanning();
        }

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PERMISSION_GRANTED) {
            mCurrentLocationHelper.stopLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int index = 0; index < permissions.length; index++) {
            if (permissions[index].equals(CAMERA)) {
                if (grantResults[index] == PERMISSION_GRANTED) {
                    startQRCodeScanning();
                } else {
                    showError(getString(R.string.label_camera_permission_alert));
                }
            }

            if (permissions[index].equals(ACCESS_FINE_LOCATION) && grantResults[index] ==
                    PERMISSION_GRANTED) {
                mGoogleApiClient = mCurrentLocationHelper.getGoogleApiClient(this);
                setLocation();
            }
        }
    }

    /**
     * Handle QR code scan result
     *
     * @param qrCodeData QR code data
     */
    @Override
    public void onQRScanResultResponse(String qrCodeData) {
        if (mQRScannerHelper != null && mQRScannerHelper.isRunning()) {
            mQRScannerHelper.stopQRScanning();
            mQRScannerHelper = null;
        }

        mMagicLink = qrCodeData;
        runOnUiThread(() -> {
            hideQRCodeScanner();
            validateQRData(qrCodeData);
        });
    }

    /**
     * Initialize UI Objects
     */
    private void initView() {
        AppCompatImageView mImgBack = findViewById(R.id.img_back_add_user);
        mImgBack.setOnClickListener(view -> onBackPressed());

        mScannerView = findViewById(R.id.scanner_view_add_user);
        mBIDScannerView = findViewById(R.id.bid_scanner_view_add_user);
        mScannerOverlay = findViewById(R.id.scanner_overlay_add_user);
        mBIDScannerView.setScannerWidthMargin(50, mScannerOverlay);

        mWebView = findViewById(R.id.web_view_add_user);

        mTxtPleaseWait = findViewById(R.id.txt_please_wait_add_user);
        mProgressBar = findViewById(R.id.progress_bar_add_user);
    }

    /**
     * Start QR code scanner
     */
    private void startQRCodeScanning() {
        mQRScannerHelper = new QRScannerHelper(this, ScanningMode.SCAN_LIVE,
                this, mBIDScannerView);
        mQRScannerHelper.startQRScanning();
        mBIDScannerView.setVisibility(View.VISIBLE);
        mScannerOverlay.setVisibility(View.VISIBLE);
    }

    /**
     * Hide QR code scanner ui
     */
    private void hideQRCodeScanner() {
        mScannerView.setVisibility(View.GONE);
        mBIDScannerView.setVisibility(View.GONE);
        mScannerOverlay.setVisibility(View.GONE);
    }

    /**
     * Get current location and set it
     */
    private void setLocation() {
        if (mGoogleApiClient != null) {
            Location location = mCurrentLocationHelper.getLocation();
            if (location != null) {
                mLatitude = location.getLatitude();
                mLongitude = location.getLongitude();
            }
        }
    }

    /**
     * After successful scan validate QR data
     */
    private void validateQRData(String qrCodeData) {
        // Check QR code data contains acr and code
        if (qrCodeData.startsWith("https://") && qrCodeData.contains("/acr/?code=")) {
            String linkData = Uri.parse(qrCodeData).getQueryParameter("code");
            try {
                // Validate Base64 Data
                String decodedLinkData = new String(Base64.decode(linkData, Base64.DEFAULT));
                validateAccessCode(decodedLinkData);
            } catch (Exception e) {
                // Show error when Base64 is not valid
                showError(getString(R.string.label_invalid_link));
            }
        } else {
            // Show error when QR code data does not contain acr and code
            showError(getString(R.string.label_unsupported_qr_code));
        }
    }

    // Check access code is valid (ex. Expired, already redeemed)
    private void validateAccessCode(String mMagicLinkData) {
        showProgress();
        MagicLinkData magicLinkDataModel = BIDUtil.JSONStringToObject(mMagicLinkData,
                MagicLinkData.class);
        BlockIDSDK.getInstance().checkIfADRequired(magicLinkDataModel.code, magicLinkDataModel.tag,
                magicLinkDataModel.api, magicLinkDataModel.community,
                (status, response, userId, error) -> {
                    if (!status) {
                        hideProgress();
                        showError(error);
                        return;
                    }
                    GetAccessCodeResponse accessCodeResponse = response.getDataObject();
                    if (accessCodeResponse.getAccessCodePayload().getAuthType().
                            equalsIgnoreCase("none")) {
                        // Get acr public key
                        getPublicKey(magicLinkDataModel);
                    } else {
                        String errorMessage = "Auth type " +
                                accessCodeResponse.getAccessCodePayload().getAuthType()
                                + " not supported";
                        showError(new ErrorManager.ErrorResponse(500, errorMessage));
                    }
                });
    }

    /**
     * Get ACR public key
     *
     * @param magicLinkData {@link MagicLinkData}
     */
    private void getPublicKey(MagicLinkData magicLinkData) {
        String[] splitData = mMagicLink.split("acr");
        AndroidNetworking.get(splitData[0] + "/acr/publickeys")
                .build()
                .getAsObject(ACRPublicKey.class, new ParsedRequestListener<ACRPublicKey>() {
                    @Override
                    public void onResponse(ACRPublicKey response) {
                        mAcrPublicKey = response.publicKey;
                        generatePayload(magicLinkData.code);
                    }

                    @Override
                    public void onError(ANError anError) {
                        hideProgress();
                        showError(new ErrorManager.ErrorResponse(anError.getErrorCode(),
                                anError.getMessage()));
                    }
                });
    }

    /**
     * Generate WebView payload
     */
    @SuppressLint("HardwareIds")
    private void generatePayload(String code) {
        // Get publicIp Address
        String publicIpAddress = null;
        try {
            publicIpAddress = new GetPublicIP().execute().get();
        } catch (Exception ignored) {
        }

        // Get IAL
        String ial = null;
        BIDGenericResponse bidGenericResponse = BlockIDSDK.getInstance().getIAL();
        if (bidGenericResponse.getStatus()) {
            ial = BlockIDSDK.getInstance().getIAL().getDataObject().toString();
        }

        // Generate event data
        EventData eventData = new EventData();
        eventData.license_hash = BIDUtil.getSha256Hash(AppConstant.licenseKey);
        eventData.authenticator_version = BuildConfig.VERSION_NAME;
        eventData.authenticator_id = getApplicationContext().getPackageName();
        eventData.authenticator_name = getResources().getString(R.string.app_name);
        eventData.authenticator_os = "android";
        eventData.person_id = BlockIDSDK.getInstance().getDID();
        eventData.person_publickey = BlockIDSDK.getInstance().getPublicKey();
        eventData.person_ial = ial;
        eventData.user_ial = "";
        eventData.user_lat = String.valueOf(mLatitude);
        eventData.user_lon = String.valueOf(mLongitude);
        eventData.device_id = Secure.getString(getContentResolver(), ANDROID_ID);
        eventData.user_agent = WebSettings.getDefaultUserAgent(AddUserActivity.this);
        eventData.device_name = BIDUtil.getDeviceName();
        eventData.network_info = publicIpAddress;

        String eventDataString = BIDUtil.objectToJSONString(eventData, true);

        // Encrypt event data using server public key
        String encryptEventData = BlockIDSDK.getInstance().encryptString(eventDataString,
                mAcrPublicKey);

        // Generate ACR request
        ACRRequest acrRequest = new ACRRequest();
        acrRequest.did = BlockIDSDK.getInstance().getDID();
        acrRequest.sender = AccountAuthConstants.K_AUTH_SENDER;
        acrRequest.code = code;
        acrRequest.os = "android";
        acrRequest.ial = ial;
        acrRequest.eventData = encryptEventData;

        String acrRequestString = BIDUtil.objectToJSONString(acrRequest, true);

        // Encrypt ACR request using server public key
        String encryptedAcrRequest = BlockIDSDK.getInstance().encryptString(acrRequestString,
                mAcrPublicKey);

        // Generate ACR Data request
        ACRRequestData acrRequestData = new ACRRequestData();
        acrRequestData.data = encryptedAcrRequest;
        acrRequestData.publicKey = BlockIDSDK.getInstance().getPublicKey();

        String acrDataRequestString = BIDUtil.objectToJSONString(acrRequestData, true);

        // Base64 of final ACR Request Data
        String base64AcrDataRequest = Base64.encodeToString(
                acrDataRequestString.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);

        // Load Request Data in WebView
        loadWebView(base64AcrDataRequest);
    }

    /**
     * Set Payload in WebView
     *
     * @param payload to be load in WebView
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void loadWebView(String payload) {
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.getUrl().toString().startsWith(AccountAuthConstants.K_AUTH_SENDER)) {
                    //pass through
                    return false;
                }

                //no payload available.
                if (request.getUrl().getQueryParameter("payload").equals("")) {
                    mWebView.setVisibility(View.GONE);
                    showError(getString(R.string.label_empty_payload));
                } else {
                    mWebView.setVisibility(View.GONE);
                    addUser(request.getUrl().getQueryParameter("payload"));
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mWebView.setVisibility(View.VISIBLE);
                hideProgress();
            }
        });
        mWebView.getSettings().setLoadsImagesAutomatically(true);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        mWebView.loadUrl(mMagicLink + "&payload=" + payload);
    }

    /**
     * Add user in BlockID SDK
     *
     * @param payload String payload return from WebView
     */
    private void addUser(String payload) {
        showProgress();
        // Base64 decode
        String base64DecodedPayload = new String(Base64.decode(payload, Base64.NO_WRAP));

        // Decrypt decoded payload
        String decryptedPayload = BlockIDSDK.getInstance().decryptString(base64DecodedPayload,
                mAcrPublicKey);

        // Generate acr response data
        ACRResponseData acrResponseData = BIDUtil.JSONStringToObject(decryptedPayload,
                ACRResponseData.class);

        // Decrypt data
        String decryptedData = BlockIDSDK.getInstance().decryptString(acrResponseData.data,
                acrResponseData.publickey);

        UserData userData = BIDUtil.JSONStringToObject(decryptedData, UserData.class);

        if (!userData.isLinked) {
            showError(getString(R.string.label_user_registration_unsuccessful));
            return;
        }

        // Add user data in SDK
        BlockIDSDK.getInstance().addPreLinkedUser(userData.userId, userData.scep_hash,
                userData.scep_privatekey, userData.scep_expiry, userData.origin,
                userData.account, (status, error) -> {
                    if (!status) {
                        showError(error);
                        return;
                    }

                    BIDGenericResponse response = BlockIDSDK.getInstance().getLinkedUserList();
                    List<BIDLinkedAccount> linkedAccounts = response.getDataObject();

                    registerFidoKey(linkedAccounts.get(0));
                });
    }

    private void registerFidoKey(BIDLinkedAccount linkedAccount) {
        BlockIDSDK.getInstance().registerFIDO2Key(this, linkedAccount,
                FIDO2KeyType.PLATFORM, null, (status, errorResponse) -> {
                    hideProgress();
                    Toast.makeText(this, getString(R.string.label_user_registration_successful),
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    /**
     * Show progress ui
     */
    private void showProgress() {
        mProgressBar.setVisibility(View.VISIBLE);
        mTxtPleaseWait.setVisibility(View.VISIBLE);
    }

    /**
     * Hide progress ui
     */
    private void hideProgress() {
        mProgressBar.setVisibility(View.GONE);
        mTxtPleaseWait.setVisibility(View.GONE);
    }

    /**
     * Show error dialog
     *
     * @param message String message of dialog
     */
    private void showError(String message) {
        ErrorDialog errorDialog = new ErrorDialog(this);
        DialogInterface.OnDismissListener onDismissListener = dialogInterface -> {
            errorDialog.dismiss();
            finish();
        };
        errorDialog.showWithOneButton(null, null, message, getString(R.string.label_ok),
                onDismissListener);
    }

    /**
     * Show error dialog
     *
     * @param error {@link com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.ErrorResponse}
     */
    private void showError(ErrorManager.ErrorResponse error) {
        ErrorDialog errorDialog = new ErrorDialog(this);
        Dialog.OnDismissListener dismissListener = dialog -> {
            errorDialog.dismiss();
            finish();
        };
        if (error.getCode() == K_CONNECTION_ERROR.getCode()) {
            errorDialog.showNoInternetDialog(dismissListener);
            return;
        }

        String errorMessage;
        if (error.getCode() == RESPONSE_CODE_LINK_EXPIRED) {
            errorMessage = "(" + error.getCode() + ") " + getString(R.string.label_link_Expired);
        } else if (error.getCode() == RESPONSE_CODE_LINK_REDEEMED) {
            errorMessage = "(" + error.getCode() + ") " +
                    getString(R.string.label_code_already_redeemed);
        } else {
            errorMessage = "(" + error.getCode() + ") " + error.getMessage();
        }
        errorDialog.showWithOneButton(null, null, errorMessage,
                getString(R.string.label_ok), dismissListener);
    }

    /**
     * Decoded base64 magic link data
     */
    @Keep
    private static class MagicLinkData {
        String tag;
        String community;
        String api;
        String code;
    }

    /**
     * ACR Server PublicKey
     */
    @Keep
    private static class ACRPublicKey {
        String publicKey;
    }

    /**
     * Event data add into {@link ACRRequest}
     */
    @Keep
    private static class EventData {
        String license_hash;
        String authenticator_version;
        String authenticator_id;
        String authenticator_name;
        String authenticator_os;
        String person_id;
        String person_publickey;
        String person_ial;
        String user_ial;
        String user_lat;
        String user_lon;
        String device_id;
        String user_agent;
        String network_info;
        String device_name;
    }

    /**
     * ACR Request Data add into {@link ACRRequestData}
     */
    @Keep
    private static class ACRRequest {
        String did;
        String sender;
        String code;
        String os;
        String ial;
        String eventData;
    }

    /**
     * Final ACRData to be send in WebView Payload
     */
    @Keep
    private static class ACRRequestData {
        String data;
        String publicKey;
    }

    /**
     * Response from WebView
     */
    @Keep
    private static class ACRResponseData {
        String data;
        String publickey;
    }

    /**
     * Data get from {@link ACRResponseData}
     */
    @Keep
    private static class UserData {
        String userId;
        String scep_hash;
        String scep_privatekey;
        String scep_expiry;
        boolean isLinked;
        BIDOrigin origin;
        BIDAccount account;
    }
}