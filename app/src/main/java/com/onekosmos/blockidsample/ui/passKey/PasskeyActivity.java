package com.onekosmos.blockidsample.ui.passKey;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.datamodel.BIDTenant;
import com.onekosmos.blockid.sdk.passKey.PasskeyRequest;
import com.onekosmos.blockidsample.AppConstant;
import com.onekosmos.blockidsample.R;

public class PasskeyActivity extends AppCompatActivity {
    AppCompatEditText etUsername, etDisplayName;
    AppCompatButton btnRegister, btnAuthenticate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_passkey);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        etUsername = findViewById(R.id.etUsername);
        etDisplayName = findViewById(R.id.etDisplayName);
        btnRegister = findViewById(R.id.btnRegister);
        btnAuthenticate = findViewById(R.id.btnAuthenticate);

        btnRegister.setOnClickListener(v -> {
            String username = etUsername.getText().toString();
            String displayName = etDisplayName.getText().toString();

            if (username.isEmpty() || displayName.isEmpty()) {
                Toast.makeText(this, "Please enter username and display name",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            BIDTenant tenant = new BIDTenant("1kosmos", "default", "https://1k-dev.1kosmos.net");
            PasskeyRequest passkeyRequest = new PasskeyRequest
                    (tenant, username, displayName);
            BlockIDSDK.getInstance().registerPasskey(PasskeyActivity.this,
                    passkeyRequest, (status, response, error) -> {
                        if (status) {
                            Toast.makeText(this, "Passkey registered successfully : " +
                                            response.sub,
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Passkey registration failed : " +
                                            error.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        btnAuthenticate.setOnClickListener(v -> {
            String username = etUsername.getText().toString();
            String displayName = etDisplayName.getText().toString();

            if (username.isEmpty() || displayName.isEmpty()) {
                Toast.makeText(this, "Please enter username and display name",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            BIDTenant tenant = new BIDTenant("1kosmos", "default", "https://1k-dev.1kosmos.net");
            PasskeyRequest passkeyRequest = new PasskeyRequest
                    (tenant, username, displayName);
            BlockIDSDK.getInstance().authenticatePasskey(PasskeyActivity.this,
                    passkeyRequest, (status, response, error) -> {
                        if (status) {
                            Toast.makeText(this, "Passkey Authentication successfully : " +
                                            response.sub,
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Passkey Authentication failed : " +
                                            error.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });

    }
}