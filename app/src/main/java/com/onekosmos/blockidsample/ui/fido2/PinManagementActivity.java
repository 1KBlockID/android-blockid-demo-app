package com.onekosmos.blockidsample.ui.fido2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.onekosmos.blockidsample.R;
import com.yubico.yubikit.android.YubiKitManager;
import com.yubico.yubikit.android.transport.nfc.NfcConfiguration;
import com.yubico.yubikit.core.application.ApplicationNotAvailableException;
import com.yubico.yubikit.core.application.CommandException;
import com.yubico.yubikit.core.smartcard.ApduException;
import com.yubico.yubikit.core.smartcard.SmartCardConnection;
import com.yubico.yubikit.fido.client.BasicWebAuthnClient;
import com.yubico.yubikit.fido.ctap.Ctap2Session;

import java.io.IOException;

public class PinManagementActivity extends AppCompatActivity {
    private YubiKitManager yubiKitManager;
    private AppCompatButton mBtnSetPIN, mBtnChangePIN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_managemnet);
        mBtnSetPIN = findViewById(R.id.btn_set_pin);
        mBtnChangePIN = findViewById(R.id.btn_change_pin);
        yubiKitManager = new YubiKitManager(this);

        mBtnSetPIN.setOnClickListener(view -> {
            showPINInputDialog(PINMethod.SET_PIN);
        });

        mBtnChangePIN.setOnClickListener(view -> {
            showPINInputDialog(PINMethod.CHANGE_PIN);
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        try {
            yubiKitManager.startNfcDiscovery(new NfcConfiguration(), this, device -> {
                // A YubiKey was brought within NFC range

                device.requestConnection(SmartCardConnection.class, result -> {
                    // The result is a Result<SmartCardConnection, IOException>, which represents either a successful connection, or an error.
                    try {
                        SmartCardConnection connection = result.getValue();  // This may throw an IOException
                        Ctap2Session session = new Ctap2Session(connection);
                        BasicWebAuthnClient bb = new BasicWebAuthnClient(session);

                        runOnUiThread(new Runnable() {
                            public void run() {
                                if (bb.isPinConfigured())
                                    Toast.makeText(PinManagementActivity.this, "Pin is configured", Toast.LENGTH_SHORT).show();
                                else
                                    Toast.makeText(PinManagementActivity.this, "Pin is not configured", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (ApplicationNotAvailableException | IOException e) {
                        // Handle errors
                        e.printStackTrace();
                    } catch (ApduException e) {
                        throw new RuntimeException(e);
                    } catch (CommandException e) {
                        throw new RuntimeException(e);
                    }
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        yubiKitManager.stopNfcDiscovery(this);
        super.onPause();
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
            edtEnterPin.setHint("Enter the PIN");
            btnVerify.setText(R.string.label_set_pin);
            title.setText(R.string.label_set_pin);
            text.setText(R.string.label_enter_the_key_pin);
        } else {
            edtEnterNewPin.setVisibility(View.VISIBLE);
            btnVerify.setText(R.string.label_change_pin);
            title.setText(R.string.label_change_pin);
            text.setText("Enter the Old and New PIN");
        }

        btnCancel.setOnClickListener(view -> dialog.dismiss());
        dialog.show();


        btnVerify.setOnClickListener(view -> {
            String securityOldPin = edtEnterPin.getEditableText().toString();
            String securityNewPIn = edtEnterNewPin.getEditableText().toString();
            Intent intent = new Intent(this, SetAndChangePINActivity.class);
            intent.putExtra("pinMethod", pinMethod);
            intent.putExtra("securityKeyOldPIN",securityOldPin);
            intent.putExtra("securityKeyNewPIN",securityNewPIn);
            startActivity(intent);
            dialog.dismiss();
            finish();
        });
    }

    public enum PINMethod {
        SET_PIN,
        CHANGE_PIN
    }
}