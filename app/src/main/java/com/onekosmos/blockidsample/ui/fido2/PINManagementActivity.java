package com.onekosmos.blockidsample.ui.fido2;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.view.WindowCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.util.ErrorDialog;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2023 1Kosmos. All rights reserved.
 */
@SuppressWarnings("FieldCanBeLocal")
public class PINManagementActivity extends AppCompatActivity {
    private AppCompatImageView mImgBack;
    private AppCompatButton mBtnIsPINConfigured, mBtnSetPIN, mBtnChangePIN, mBtnReset;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 🔒 Lock the orientation to portrait
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 15+
            WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        }

        setContentView(R.layout.activity_fido2_pinmanagement);

        mImgBack = findViewById(R.id.img_back);
        mImgBack.setOnClickListener(view -> onBackPressed());

        mBtnIsPINConfigured = findViewById(R.id.btn_pin_configured);
        mBtnSetPIN = findViewById(R.id.btn_set_pin);
        mBtnChangePIN = findViewById(R.id.btn_change_pin);
        mBtnReset = findViewById(R.id.btn_reset_fido2_key);

        mBtnSetPIN.setOnClickListener(view -> showPINInputDialog(PINMethod.SET_PIN));

        mBtnChangePIN.setOnClickListener(view -> showPINInputDialog(PINMethod.CHANGE_PIN));

        mBtnReset.setOnClickListener(view -> onResetClick());

        mBtnIsPINConfigured.setOnClickListener(view ->
                BlockIDSDK.getInstance().isFido2PinConfigured(this,
                        ((status, pinMethods, error) -> {
                            if (status) {
                                Toast.makeText(this,
                                        getResources().getString(R.string.label_pin_is_configured),
                                        Toast.LENGTH_SHORT).show();
                            } else if (error != null) {
                                Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this,
                                        getResources().getString(R.string.label_pin_is_not_configured),
                                        Toast.LENGTH_SHORT).show();
                            }
                        })));
    }

    /**
     * Show pin input dialog box
     *
     * @param pinMethod enum value of method to be called
     */
    private void showPINInputDialog(PINMethod pinMethod) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_pin_management);
        TextInputLayout tilEnterNewPin = dialog.findViewById(R.id.til_dialog_change_pin);
        TextInputEditText edtEnterPin = dialog.findViewById(R.id.edt_enter_pin);
        TextInputEditText edtEnterNewPin = dialog.findViewById(R.id.edt_enter_new_pin);
        TextInputEditText edtEnterConfirmPin = dialog.findViewById(R.id.edt_enter_confirm_pin);
        AppCompatButton btnCancel = dialog.findViewById(R.id.btn_dialog_pin_input_cancel);
        AppCompatButton btnVerify = dialog.findViewById(R.id.btn_dialog_pin_input_verify);
        AppCompatTextView title = dialog.findViewById(R.id.txt_dialog_pin_input_title);
        AppCompatTextView text = dialog.findViewById(R.id.txt_dialog_pin_input_message);
        if (pinMethod.equals(PINMethod.SET_PIN)) {
            edtEnterConfirmPin.setHint(getString(R.string.label_enter_confirm_the_pin));
            edtEnterPin.setHint("");
            edtEnterPin.setHint(getString(R.string.label_enter_the_new_pin));
            edtEnterNewPin.setVisibility(View.GONE);
            tilEnterNewPin.setVisibility(View.GONE);
            btnVerify.setText(R.string.label_set_pin);
            title.setText(R.string.label_set_pin);
            text.setText(R.string.label_enter_the_key_pin);
        } else if (pinMethod.equals(PINMethod.CHANGE_PIN)) {
            edtEnterNewPin.setVisibility(View.VISIBLE);
            tilEnterNewPin.setVisibility(View.VISIBLE);
            btnVerify.setText(R.string.label_change_pin);
            title.setText(R.string.label_change_pin);
            text.setText(getString(R.string.label_enter_the_pin));
            edtEnterPin.setHint("");
            edtEnterPin.setHint(getString(R.string.label_enter_old_pin));
            edtEnterNewPin.setHint(getString(R.string.label_enter_the_new_pin));
            edtEnterConfirmPin.setHint(getString(R.string.label_enter_confirm_the_pin));
        }

        btnCancel.setOnClickListener(view -> dialog.dismiss());
        dialog.show();

        btnVerify.setOnClickListener(view -> {
            String securityOldPin = edtEnterPin.getEditableText().toString();
            String securityNewPin = edtEnterNewPin.getEditableText().toString();
            String confirmPin = edtEnterConfirmPin.getEditableText().toString();
            if (pinMethod.equals(PINMethod.SET_PIN)) {
                if (securityOldPin.equalsIgnoreCase(confirmPin)) {
                    BlockIDSDK.getInstance().setFido2PIN(this, securityOldPin,
                            ((status, pinMethods, error) -> {
                                if (status) {
                                    Toast.makeText(this,
                                            getString(R.string.label_pin_set_successfully),
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, error.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            }));
                } else {
                    Toast.makeText(this,
                            getString(R.string.label_pin_does_not_match),
                            Toast.LENGTH_SHORT).show();
                }
            } else if (pinMethod.equals(PINMethod.CHANGE_PIN)) {
                if (securityNewPin.equalsIgnoreCase(confirmPin)) {
                    BlockIDSDK.getInstance().changeFido2PIN(this, securityOldPin, securityNewPin,
                            ((status, pinMethods, error) -> {
                                if (status) {
                                    Toast.makeText(this,
                                            getString(R.string.label_pin_changed_successfully),
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, error.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            }));
                } else {
                    Toast.makeText(this,
                            getString(R.string.label_pin_does_not_match),
                            Toast.LENGTH_SHORT).show();
                }
            }
            dialog.dismiss();
        });
    }

    private void onResetClick() {
        ErrorDialog errorDialog = new ErrorDialog(this);
        errorDialog.showWithTwoButton(
                null,
                getString(R.string.label_warning),
                getString(R.string.label_do_you_want_to_reset_fido),
                getString(R.string.label_cancel), getString(R.string.label_ok),
                (dialogInterface, i) -> {
                    errorDialog.dismiss();
                    BlockIDSDK.getInstance().resetFido2(this,
                            ((status, pinMethods, error) -> {
                                if (status) {
                                    Toast.makeText(this,
                                            getResources().getString(
                                                    R.string.label_security_key_reset_successfully),
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, error.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            }));
                },
                dialog -> errorDialog.dismiss());
    }

    public enum PINMethod {
        SET_PIN,
        CHANGE_PIN
    }
}