package com.onekosmos.blockidsample.ui.about;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.datamodel.BIDTenant;
import com.onekosmos.blockid.sdk.utils.BIDUtil;
import com.onekosmos.blockidsample.AppConstant;
import com.onekosmos.blockidsample.BuildConfig;
import com.onekosmos.blockidsample.R;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        initView();
    }

    @SuppressLint("SetTextI18n")
    private void initView() {
        AppCompatImageView mImgBack = findViewById(R.id.img_back_about);
        mImgBack.setOnClickListener(view -> onBackPressed());

        BIDTenant tenant = BlockIDSDK.getInstance().getTenant();
        AppCompatTextView txtTenantDNS = findViewById(R.id.txt_about_tenant_info);
        txtTenantDNS.setText("Tenant Information: " + BIDUtil.objectToJSONString(tenant,
                true));

        AppCompatTextView txtLicenseKey = findViewById(R.id.txt_about_license_key);
        String licenseKey = AppConstant.licenseKey;
        licenseKey = licenseKey.replaceAll(licenseKey.substring(0, licenseKey.length() - 4),
                "X");
        txtLicenseKey.setText("License Key: " + licenseKey);

        AppCompatTextView txtDid = findViewById(R.id.txt_about_did);
        txtDid.setText("DID: " + BlockIDSDK.getInstance().getDID());

        AppCompatTextView txtPublicKey = findViewById(R.id.txt_about_public_key);
        txtPublicKey.setText("Public Key: " + BlockIDSDK.getInstance().getPublicKey());

        AppCompatTextView txtSDKVersion = findViewById(R.id.txt_about_sdk_version);
        txtSDKVersion.setText(getString(R.string.label_sdk_version) + ": "
                + getVersionText(BlockIDSDK.getInstance().getVersion()));

        AppCompatTextView txtAppVersion = findViewById(R.id.txt_about_app_version);
        txtAppVersion.setText(getString(R.string.label_app_version) + ": "
                + getVersionText(BuildConfig.VERSION_NAME));

        String copy = txtTenantDNS.getText() + "\n"
                + txtLicenseKey.getText()
                + txtDid.getText() + "\n"
                + txtPublicKey.getText() + "\n"
                + txtSDKVersion.getText() + "\n"
                + txtAppVersion.getText() + "\n";

        AppCompatButton btnCopy = findViewById(R.id.btn_about_copy);
        btnCopy.setOnClickListener(view -> copyToClipboard(AboutActivity.this, copy));
    }

    public void copyToClipboard(Context context, String text) {
        try {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context
                    .getSystemService(CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData
                    .newPlainText("msg", text);
            clipboard.setPrimaryClip(clip);
        } catch (Exception ignored) {
        }
    }

    private String getVersionText(String inputVersion) {
        String[] splitVersion = inputVersion.split("\\.");
        StringBuilder version = new StringBuilder();
        for (int index = 0; index < splitVersion.length - 1; index++) {
            version.append(index == 0 ? splitVersion[index] : "." + splitVersion[index]);
        }
        String hexCode = splitVersion[splitVersion.length - 1];
        return version + " (" + hexCode + ")";
    }
}