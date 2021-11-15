package com.onekosmos.blockidsample.ui.nationalID;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.onekosmos.blockidsample.R;

public class NationalIDScanActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nationa_id_scan);

        findViewById(R.id.img_back).setOnClickListener(v -> onBackPressed());

        findViewById(R.id.txt_back).setOnClickListener(v -> onBackPressed());

        findViewById(R.id.btn_manual_capture).setOnClickListener(v -> {
            Intent manualCapture = new Intent(NationalIDScanActivity.this,
                    NationalIDManualCaptureActivity.class);
            startActivity(manualCapture);
            finish();
        });

        findViewById(R.id.btn_live_scan).setOnClickListener(v -> {
            Intent manualCapture = new Intent(NationalIDScanActivity.this,
                    NationalIDLiveScanActivity.class);
            startActivity(manualCapture);
            finish();
        });
    }
}