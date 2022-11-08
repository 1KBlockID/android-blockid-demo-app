package com.onekosmos.blockidsample.ui.about;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.onekosmos.blockid.sdk.BlockIDSDK;
import com.onekosmos.blockid.sdk.datamodel.BIDTenant;
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

        String tenantInfo = getString(R.string.label_tenant_info) + ":\n"
                + getString(R.string.label_dns) + ": " + tenant.getDns() + "\n"
                + getString(R.string.label_tag) + ": " + tenant.getTenantTag()
                + " (" + tenant.getTenantId() + ")" + "\n"
                + getString(R.string.label_community) + ": " + tenant.getCommunity()
                + " (" + tenant.getCommunityId() + ")";
        txtTenantDNS.setText(tenantInfo);
        AppCompatTextView txtLicenseKey = findViewById(R.id.txt_about_license_key);
        String licenseKey = AppConstant.licenseKey;
        licenseKey = licenseKey.replace(licenseKey.substring(8, licenseKey.length() - 4),
                "-xxxx-xxxx-xxxx-xxxxxxxx");
        txtLicenseKey.setText(getString(R.string.label_license_key) + ":\n" + licenseKey);

        AppCompatTextView txtDid = findViewById(R.id.txt_about_did);
        txtDid.setText(getString(R.string.label_did) + ":\n" + BlockIDSDK.getInstance().getDID());

        AppCompatTextView txtPublicKey = findViewById(R.id.txt_about_public_key);
        txtPublicKey.setText(getString(R.string.label_public_key) + ":\n"
                + BlockIDSDK.getInstance().getPublicKey());

        AppCompatTextView txtSDKVersion = findViewById(R.id.txt_about_sdk_version);
        txtSDKVersion.setText(getString(R.string.label_sdk_version) + ":\n"
                + getVersionText(BlockIDSDK.getInstance().getVersion()));

        AppCompatTextView txtAppVersion = findViewById(R.id.txt_about_app_version);
        txtAppVersion.setText(getString(R.string.label_app_version) + ":\n"
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
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(
                    CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("msg", text);
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