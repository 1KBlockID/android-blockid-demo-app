package com.onekosmos.blockidsample.ui.fido2;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import com.onekosmos.blockidsample.R;
import com.yubico.yubikit.android.YubiKitManager;
import com.yubico.yubikit.android.transport.nfc.NfcConfiguration;
import com.yubico.yubikit.core.application.ApplicationNotAvailableException;
import com.yubico.yubikit.core.application.CommandException;
import com.yubico.yubikit.core.application.CommandState;
import com.yubico.yubikit.core.smartcard.ApduException;
import com.yubico.yubikit.core.smartcard.SmartCardConnection;
import com.yubico.yubikit.fido.client.BasicWebAuthnClient;
import com.yubico.yubikit.fido.client.ClientError;
import com.yubico.yubikit.fido.ctap.Ctap2Session;

import java.io.IOException;

public class SetAndChangePINActivity extends AppCompatActivity {
    private YubiKitManager yubiKitManager;
    PinManagementActivity.PINMethod pinMethod;
    String securityKeyOldPIN, securityKeyNewPIN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_and_change_pinactivity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pinMethod = getIntent().getSerializableExtra("pinMethod", PinManagementActivity.PINMethod.class);
        } else {
            pinMethod = getIntent().getParcelableExtra("pinMethod");
        }
        securityKeyOldPIN = getIntent().getStringExtra("securityKeyOldPIN");
        securityKeyNewPIN = getIntent().getStringExtra("securityKeyNewPIN");
        yubiKitManager = new YubiKitManager(this);
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
                        session.reset(null);
                        BasicWebAuthnClient bb = new BasicWebAuthnClient(session);
                        if (pinMethod.equals(PinManagementActivity.PINMethod.SET_PIN)) {
                            bb.setPin(securityKeyOldPIN.toCharArray());
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(SetAndChangePINActivity.this, "Pin set Successfully", Toast.LENGTH_SHORT).show();
                                }
                            });
                            finish();
                        } else if (pinMethod.equals(PinManagementActivity.PINMethod.CHANGE_PIN)) {
                            bb.changePin(securityKeyOldPIN.toCharArray(), securityKeyNewPIN.toCharArray());
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(SetAndChangePINActivity.this, "Pin changed Successfully", Toast.LENGTH_SHORT).show();
                                }
                            });
                            finish();
                        } else {
                            session.reset(null);
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(SetAndChangePINActivity.this, "Reset Successfully", Toast.LENGTH_SHORT).show();
                                }
                            });
                            finish();
                        }
                    } catch (ApplicationNotAvailableException | IOException e) {
                        // Handle errors
                        e.printStackTrace();
                    } catch (ApduException e) {
                        throw new RuntimeException(e);
                    } catch (CommandException e) {
                        throw new RuntimeException(e);
                    } catch (ClientError e) {
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
}