package com.onekosmos.blockidsample.ui.fido2;

import static com.onekosmos.blockidsample.util.SharedPreferenceUtil.K_PREF_FIDO2_USERNAME;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;

import com.google.android.material.textfield.TextInputEditText;
import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.datamodel.BIDTenant;
import com.onekosmos.blockid.sdk.fido2.FIDO2KeyType;
import com.onekosmos.blockid.sdk.fido2.FIDO2Observer;
import com.onekosmos.blockidsample.AppConstant;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.ProgressDialog;
import com.onekosmos.blockidsample.util.ResultDialog;
import com.onekosmos.blockidsample.util.SharedPreferenceUtil;
import com.onekosmos.fido2authenticator.fido2.Fido2AuthenticatorHelper;
import com.onekosmos.fido2authenticator.fido2.Fido2AuthenticatorHelper.Fido2AuthenticatorListener;
import com.onekosmos.fido2authenticator.fido2.Fido2AuthenticatorHelper.PublicKeyCREDS;
import com.onekosmos.fido2authenticator.utils.FidoErrorManager.FidoAuthnErrorResponse;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2022 1Kosmos. All rights reserved.
 */
public class FIDO2BaseActivity extends AppCompatActivity {
    // html file to show UI/UX as per app design
    private final String K_FILE_NAME = "fido3.html";
    // FIDO2Observer must initialize before onCreate()
    private final FIDO2Observer observer = new FIDO2Observer(this);
    private AppCompatImageView mImgBack;
    private AppCompatButton mBtnRegister, mBtnAuthenticate, mBtnRegisterPlatformAuthenticator,
            mBtnRegisterExternalAuthenticator, mBtnAuthenticatePlatformAuthenticator,
            mBtnAuthenticateExternalAuthenticator, mBtnCustomRegister, mBtnCustomAuthenticate;
    private TextInputEditText mEtUserName;
    private boolean mBtnRegisterClicked, mBtnAuthenticateClicked;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fido2_base);

        mImgBack = findViewById(R.id.img_back);
        mImgBack.setOnClickListener(view -> onBackPressed());

        String storedUserName = SharedPreferenceUtil.getInstance().getString(K_PREF_FIDO2_USERNAME);
        mEtUserName = findViewById(R.id.edt_user_name);
        if (!TextUtils.isEmpty(storedUserName)) {
            mEtUserName.setText(storedUserName);
        }

        mBtnRegister = findViewById(R.id.btn_register_web);
        mBtnAuthenticate = findViewById(R.id.btn_authenticate_web);

        mProgressDialog = new ProgressDialog(this, getString(R.string.label_please_wait));

        mBtnRegister.setOnClickListener(v -> {
            if (!mBtnRegisterClicked) {
                if (validateUserName(mEtUserName.getText().toString())) {
                    mProgressDialog.show();
                    mBtnRegisterClicked = true;
                    BIDTenant tenant = AppConstant.clientTenant;
                    BlockIDSDK.getInstance().registerFIDO2Key(this,
                            mEtUserName.getText().toString(),
                            tenant.getDns(),
                            tenant.getCommunity(),
                            K_FILE_NAME,
                            (status, errorResponse) -> {
                                mProgressDialog.dismiss();
                                mBtnRegisterClicked = false;
                                if (!status) {
                                    showError(errorResponse);
                                } else {
                                    SharedPreferenceUtil.getInstance().setString(
                                            K_PREF_FIDO2_USERNAME, mEtUserName.getText().toString());
                                    showResultDialog(R.drawable.icon_dialog_success,
                                            getString(R.string.label_fido2_key_has_been_successfully_registered));
                                }
                            });
                }
            }
        });

        mBtnAuthenticate.setOnClickListener(v -> {
            if (!mBtnAuthenticateClicked) {
                if (validateUserName(mEtUserName.getText().toString())) {
                    mProgressDialog.show();
                    mBtnAuthenticateClicked = true;
                    BIDTenant tenant = AppConstant.clientTenant;
                    BlockIDSDK.getInstance().authenticateFIDO2Key(this,
                            mEtUserName.getText().toString(),
                            tenant.getDns(),
                            tenant.getCommunity(),
                            K_FILE_NAME,
                            (status, errorResponse) -> {
                                mProgressDialog.dismiss();
                                mBtnAuthenticateClicked = false;
                                if (!status) {
                                    showError(errorResponse);
                                } else {
                                    SharedPreferenceUtil.getInstance().setString(
                                            K_PREF_FIDO2_USERNAME, mEtUserName.getText().toString());
                                    showResultDialog(R.drawable.icon_dialog_success,
                                            getString(R.string.label_successfully_authenticated_with_your_fido2_key));
                                }
                            });
                }
            }
        });

        mBtnRegisterPlatformAuthenticator = findViewById(
                R.id.btn_register_platform_authenticator);
        mBtnRegisterPlatformAuthenticator.setOnClickListener(
                view -> registerFIDO2(FIDO2KeyType.PLATFORM));

        mBtnRegisterExternalAuthenticator = findViewById(
                R.id.btn_register_external_authenticator);
        mBtnRegisterExternalAuthenticator.setOnClickListener(
                view -> registerFIDO2(FIDO2KeyType.CROSS_PLATFORM));

        mBtnAuthenticatePlatformAuthenticator = findViewById(
                R.id.btn_authenticate_platform_authenticator);
        mBtnAuthenticatePlatformAuthenticator.setOnClickListener(
                view -> authenticateFIDO2(FIDO2KeyType.PLATFORM));

        mBtnAuthenticateExternalAuthenticator = findViewById(
                R.id.btn_authenticate_external_authenticator);
        mBtnAuthenticateExternalAuthenticator.setOnClickListener(
                view -> authenticateFIDO2(FIDO2KeyType.CROSS_PLATFORM));

        mBtnCustomRegister = findViewById(R.id.btn_register_custom_authenticator);
        mBtnCustomRegister.setOnClickListener(v -> {
            String optionsJsonString = "{\"rp\":{\"name\":\"1k-dev.1kosmos.net\",\"id\":\"1k-dev.1kosmos.net\"},\"user\":{\"id\":\"KiC5KMxK5wiEe8kFFhLRf-20l-1xpuKJCjN1izoe5RM\",\"name\":\"gaurav\",\"displayName\":\"gaurav\"},\"attestation\":\"direct\",\"pubKeyCredParams\":[{\"type\":\"public-key\",\"alg\":-7}],\"timeout\":60000,\"authenticatorSelection\":{\"userVerification\":\"required\",\"requireResidentKey\":false},\"challenge\":\"ZXlKMGVYQWlPaUpLVjFRaUxDSmhiR2NpT2lKSVV6STFOaUo5LmV5SnlZVzVrSWpvaU56bEVXR3hPYVV0dmFtb3lObmRmTkhsT1VXdElhVkJ1YUdGdVlVZFhjVE5JVDNoNFRUZzFMU0lzSW1GMVpDSTZJakZyTFdSbGRpNHhhMjl6Ylc5ekxtNWxkQ0lzSW5OMVlpSTZJbWQxY21GMklpd2lZWFYwYUhObGJHVmpkR2x2YmlJNklpSXNJbUYwZEdWemRHRjBhVzl1SWpvaVpHbHlaV04wSWl3aWFXUWlPaUpMYVVNMVMwMTRTelYzYVVWbE9HdEdSbWhNVW1ZdE1qQnNMVEY0Y0hWTFNrTnFUakZwZW05bE5WSk5JaXdpWlhod0lqb3hOamMyTlRZMU16UTJmUS43YW1nVXJybnFCLXNiWTJ2TEVySkpUeUMwcDZKWE9rUUJ5NW5aVHVfNFc4\",\"excludeCredentials\":[],\"status\":\"ok\",\"errorMessage\":\"\"}";
            Fido2AuthenticatorHelper.getInstance().
                    registerFido2Key(this, "https://1k-dev.1kosmos.net",

                            optionsJsonString, new Fido2AuthenticatorListener() {
                                @Override
                                public void onSuccess(PublicKeyCREDS success) {
                                    Log.d("Sucess:-", success.getType());
                                    Toast.makeText(getApplicationContext(),
                                            success.getId()
                                            , Toast.LENGTH_LONG).show();
                                }

                                @Override
                                public void onFailure(FidoAuthnErrorResponse message) {
                                    Toast.makeText(getApplicationContext(),
                                            message.getMessage()
                                            , Toast.LENGTH_LONG).show();
                                }
                            });
        });
        mBtnCustomAuthenticate = findViewById(R.id.btn_authenticate_custom_authenticator);
        mBtnCustomAuthenticate.setOnClickListener(v -> {
            String optionsJsonString = "{\"challenge\":\"ZXlKMGVYQWlPaUpLVjFRaUxDSmhiR2NpT2lKSVV6STFOaUo5LmV5SnlZVzVrSWpvaVducEpOMGR1VjA5bmNsUTJjWFpMVkdSeldFMXlRVlV5Tm10ak9WTjBNemxQUmpoQmFYUnplQ0lzSW1GMVpDSTZJakZyTFdSbGRpNHhhMjl6Ylc5ekxtNWxkQ0lzSW5OMVlpSTZJbWRoZFhKaGRpSXNJbWxrSWpvaU1IZHBSalp2UW1aSlVTMVlSSFI2YzFoT2MzRjVUMkUxWW1aNlNtbGtMVEl5WWpKS1luZGpiemhyUlNJc0ltVjRjQ0k2TVRZM056VTVOekEyT0gwLllmeHo0WmV5T1Z5aUV2NTE0b0ZXSXZfbG8xRkQtcjFCYUJkNmJuS1pPczg\",\"rpId\":\"1k-dev.1kosmos.net\",\"timeout\":60000,\"userVerification\":\"preferred\",\"allowCredentials\":[{\"type\":\"public-key\",\"id\":\"ShMYAcxmNPtprIEzb5YjV5F91C5Eb15ocIANFteToGG9P1dK3juyoLG51YoZ3nmSHgpc2oWWN-_UzeGn6JMN2A\"},{\"type\":\"public-key\",\"id\":\"dwIGA-saJeLndkNDFTSyqsH8EiQCaVIU3yRurpykX1SWo5EyYo9h7U8b3IPSWRvVvc13v3uUllvQ45F1FTgkAMLK5cxUg2VbEXHAeLH6PaG4ULVD3DqxdzHwhJoQLywwuic-KuZIRvfZ1vd_8qFKV5pbCsLQ93iM\"},{\"type\":\"public-key\",\"id\":\"L2E-azldNpWUkLEl2YreOWfzEt7JhvcGDJZilcF0c-D5ic_ZSSEMrVn4SQvu5DDsZ4x1hn4k3r0nHHU61vXNSw\"},{\"type\":\"public-key\",\"id\":\"Bpfxe2O3qN73R0h-RszIARcFkZlVqQfaNWHjWzhc9TXeWLScou2AkVZyzZPK1FgRdyEe943pXIz-S3G3N8vxkw\"}],\"status\":\"ok\",\"errorMessage\":\"\"}";
            Fido2AuthenticatorHelper.getInstance().
                    authenticateFido2Key(this, "https://1k-dev.1kosmos.net",
                            optionsJsonString, new Fido2AuthenticatorListener() {
                                @Override
                                public void onSuccess(PublicKeyCREDS success) {
                                    Log.d("Autheticate success:", success.getType());
                                    Toast.makeText(getApplicationContext(),
                                            success.getId()
                                            , Toast.LENGTH_LONG).show();
                                }

                                @Override
                                public void onFailure(FidoAuthnErrorResponse message) {
                                    Log.d("message:-", message.getMessage());
                                    Toast.makeText(getApplicationContext(),
                                            message.getMessage()
                                            , Toast.LENGTH_LONG).show();
                                }
                            });
        });
    }

    /**
     * Check userName is empty or not
     *
     * @return userName is empty then return false and show toast else true
     */
    private boolean validateUserName(String userName) {
        userName = userName.trim();
        hideKeyboard();
        if (TextUtils.isEmpty(userName)) {
            Toast.makeText(this,
                    R.string.label_enter_username,
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void showError(ErrorManager.ErrorResponse error) {
        ErrorDialog errorDialog = new ErrorDialog(this);
        DialogInterface.OnDismissListener onDismissListener = dialogInterface -> {
            errorDialog.dismiss();
        };
        if (error.getCode() == ErrorManager.CustomErrors.K_CONNECTION_ERROR.getCode()) {
            errorDialog.showNoInternetDialog(onDismissListener);
            return;
        }
        errorDialog.showWithOneButton(null, getString(R.string.label_error),
                error.getMessage() + " (" + error.getCode() + ").",
                getString(R.string.label_ok),
                onDismissListener);
    }

    /**
     * Hide keyboard when user click on button
     */
    private void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) this.
                getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null && inputMethodManager.isAcceptingText())
            inputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().
                    getWindowToken(), 0);
    }

    private void showResultDialog(int imageId, String subMessage) {
        ResultDialog dialog = new ResultDialog(this, imageId,
                SharedPreferenceUtil.getInstance().getString(K_PREF_FIDO2_USERNAME), subMessage);
        dialog.show();
        new Handler().postDelayed(dialog::dismiss, 2000);
    }

    /**
     * Register FIDO2 key
     */
    private void registerFIDO2(FIDO2KeyType keyType) {
        if (!validateUserName(mEtUserName.getText().toString())) {
            return;
        }

        mProgressDialog.show();
        BlockIDSDK.getInstance().registerFIDO2Key(this,
                mEtUserName.getText().toString(),
                AppConstant.clientTenant.getDns(),
                AppConstant.clientTenant.getCommunity(),
                keyType,
                observer,
                (status, errorResponse) -> {
                    mProgressDialog.dismiss();
                    if (!status) {
                        showError(errorResponse);
                        return;
                    }
                    SharedPreferenceUtil.getInstance().setString(
                            K_PREF_FIDO2_USERNAME, mEtUserName.getText().toString());

                    String message = keyType.getValue().equalsIgnoreCase(
                            FIDO2KeyType.PLATFORM.getValue()) ?
                            getString(R.string.label_platform_key_registered) :
                            getString(R.string.label_security_key_registered);
                    showResultDialog(R.drawable.icon_dialog_success, message);
                });
    }

    /**
     * Authenticate FIDO2 key
     */
    private void authenticateFIDO2(FIDO2KeyType keyType) {
        if (!validateUserName(mEtUserName.getText().toString())) {
            return;
        }

        mProgressDialog.show();
        BlockIDSDK.getInstance().authenticateFIDO2Key(this,
                mEtUserName.getText().toString(),
                AppConstant.clientTenant.getDns(),
                AppConstant.clientTenant.getCommunity(),
                keyType,
                observer,
                (status, errorResponse) -> {
                    mProgressDialog.dismiss();
                    if (!status) {
                        runOnUiThread(() -> showError(errorResponse));
                        return;
                    }

                    SharedPreferenceUtil.getInstance().setString(
                            K_PREF_FIDO2_USERNAME, mEtUserName.getText().toString());
                    String message = keyType.getValue().equalsIgnoreCase(
                            FIDO2KeyType.PLATFORM.getValue()) ?
                            getString(R.string.label_platform_key_authenticated) :
                            getString(R.string.label_security_key_authenticated);

                    showResultDialog(R.drawable.icon_dialog_success, message);
                });
    }
}