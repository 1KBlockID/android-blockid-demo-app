package com.onekosmos.blockidsample.ui.fido2;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.google.android.material.textfield.TextInputEditText;
import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockidsample.R;

public class PINManagementActivity extends AppCompatActivity {
    private AppCompatImageView mImgBack;
    private AppCompatButton mBtnIsPINConfigured, mBtnSetPIN, mBtnChangePIN, mBtnReset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fido2_pinmanagement);

        mImgBack = findViewById(R.id.img_back);
        mImgBack.setOnClickListener(view -> onBackPressed());

        mBtnIsPINConfigured = findViewById(R.id.btn_pin_configured);
        mBtnSetPIN = findViewById(R.id.btn_set_pin);
        mBtnChangePIN = findViewById(R.id.btn_change_pin);
        mBtnReset = findViewById(R.id.btn_reset_pin);

        mBtnSetPIN.setOnClickListener(view -> {
            showPINInputDialog(PINMethod.SET_PIN);
        });

        mBtnChangePIN.setOnClickListener(view -> {
            showPINInputDialog(PINMethod.CHANGE_PIN);
        });

        mBtnReset.setOnClickListener(view -> {
            BlockIDSDK.getInstance().resetSecurityKey(this, ((status, pinMethods, error) -> {
                if (status) {
                    Toast.makeText(this,
                            getResources().getString(R.string.label_security_key_reset_successfully),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }));
        });

        mBtnIsPINConfigured.setOnClickListener(view -> {
            BlockIDSDK.getInstance().isPinConfigured(this, ((status, pinMethods, error) -> {
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
            }));
        });
    }

    private void showPINInputDialog(PINMethod pinMethod) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_pin_management);
        TextInputEditText edtEnterPin = dialog.findViewById(R.id.edt_enter_pin);
        TextInputEditText edtEnterNewPin = dialog.findViewById(R.id.edt_enter_new_pin);
        AppCompatButton btnCancel = dialog.findViewById(R.id.btn_dialog_pin_input_cancel);
        AppCompatButton btnVerify = dialog.findViewById(R.id.btn_dialog_pin_input_verify);
        AppCompatTextView title = dialog.findViewById(R.id.txt_dialog_pin_input_title);
        AppCompatTextView text = dialog.findViewById(R.id.txt_dialog_pin_input_message);
        if (pinMethod.equals(PINMethod.SET_PIN)) {
            edtEnterNewPin.setVisibility(View.GONE);
            edtEnterPin.setHint("");
            edtEnterPin.setHint(getString(R.string.label_enter_the_pin));
            btnVerify.setText(R.string.label_set_pin);
            title.setText(R.string.label_set_pin);
            text.setText(R.string.label_enter_the_key_pin);
        } else if (pinMethod.equals(PINMethod.CHANGE_PIN)) {
            edtEnterNewPin.setVisibility(View.VISIBLE);
            btnVerify.setText(R.string.label_change_pin);
            title.setText(R.string.label_change_pin);
            text.setText(getString(R.string.label_enter_the_old_and_new_pin));
            edtEnterPin.setHint("");
            edtEnterPin.setHint(getString(R.string.label_enter_old_pin));
            edtEnterNewPin.setHint(getString(R.string.label_enter_new_pin));
        }

        btnCancel.setOnClickListener(view -> dialog.dismiss());
        dialog.show();


        btnVerify.setOnClickListener(view -> {
            String securityOldPin = edtEnterPin.getEditableText().toString();
            String securityNewPIn = edtEnterNewPin.getEditableText().toString();
            if (pinMethod.equals(PINMethod.SET_PIN)) {
                BlockIDSDK.getInstance().setPIN(this, securityOldPin,
                        ((status, pinMethods, error) -> {
                            if (status) {
                                Toast.makeText(this, getString(R.string.label_pin_set_successfully),
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }));
            } else if (pinMethod.equals(PINMethod.CHANGE_PIN)) {
                BlockIDSDK.getInstance().changePIN(this, securityOldPin, securityNewPIn,
                        ((status, pinMethods, error) -> {
                            if (status) {
                                Toast.makeText(this, getString(R.string.label_pin_changed_successfully),
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }));
            }
            dialog.dismiss();
            finish();
        });
    }

    public enum PINMethod {
        SET_PIN,
        CHANGE_PIN
    }
}