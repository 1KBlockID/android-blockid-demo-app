package com.onekosmos.blockidsdkdemo.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.blockid.sdk.BlockIDSDK;
import com.example.blockidsdkdemo.R;
import com.onekosmos.blockidsdkdemo.AppConstant;
import com.onekosmos.blockidsdkdemo.ui.enrollment.EnrollmentActivity;
import com.onekosmos.blockidsdkdemo.util.ErrorDialog;
import com.onekosmos.blockidsdkdemo.util.ProgressDialog;

public class RegisterTenantActivity extends AppCompatActivity {

    AppCompatButton mBtnRegisterTenant;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_tenant);
        mBtnRegisterTenant = findViewById(R.id.btn_register);
        mBtnRegisterTenant.setOnClickListener(view -> registerTenant());
    }

    private void registerTenant() {
        BlockIDSDK.getInstance().initiateWallet();
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();

        BlockIDSDK.getInstance().registerTenant(AppConstant.defaultTenant, (status, error, bidTenant) -> {
            progressDialog.dismiss();
            if (status) {
                BlockIDSDK.getInstance().commitApplicationWallet();
                Intent intent = new Intent(this, EnrollmentActivity.class);
                startActivity(intent);
                finish();
            }
            if (!status) {
                ErrorDialog errorDialog = new ErrorDialog(this);
                errorDialog.show(null,
                        getString(R.string.label_error),
                        error.getMessage(), dialog -> {
                            errorDialog.dismiss();
                        });
            }
        });
    }
}