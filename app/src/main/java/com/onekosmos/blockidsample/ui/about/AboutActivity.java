package com.onekosmos.blockidsample.ui.about;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
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

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 🔒 Lock the orientation to portrait
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_about);
        initView();
    }

    /**
     * Initialize and update UI
     */
    private void initView() {
        AppCompatImageView mImgBack = findViewById(R.id.img_back_about);
        mImgBack.setOnClickListener(view -> onBackPressed());

        BIDTenant tenant = BlockIDSDK.getInstance().getTenant();
        BIDTenant appTenant = BlockIDSDK.getInstance().getAppTenant();
        AppCompatTextView txtTenantDNS = findViewById(R.id.txt_about_tenant_info);

        // Get and set tenant info
        String tenantInfo = getString(R.string.label_root_tenant) + ":\n"
                + getString(R.string.label_dns) + ": " + tenant.getDns() + "\n"
                + getString(R.string.label_tag) + ": " + tenant.getTenantTag()
                + " (" + tenant.getTenantId() + ")" + "\n"
                + getString(R.string.label_community) + ": " + tenant.getCommunity()
                + " (" + tenant.getCommunityId() + ")" + "\n\n"
                + getString(R.string.label_app_tenant) + ":\n"
                + getString(R.string.label_dns) + ": " + appTenant.getDns() + "\n"
                + getString(R.string.label_tag) + ": " + appTenant.getTenantTag()
                + " (" + appTenant.getTenantId() + ")"+ "\n"
                + getString(R.string.label_community) + ": " + appTenant.getCommunity()
                + " (" + appTenant.getCommunityId() + ")";
        txtTenantDNS.setText(tenantInfo);

        // Get and set license key
        AppCompatTextView txtLicenseKey = findViewById(R.id.txt_about_license_key);
        String licenseKey = AppConstant.licenseKey;
        licenseKey = licenseKey.replace(licenseKey.substring(8, licenseKey.length() - 4),
                "-xxxx-xxxx-xxxx-xxxxxxxx");
        licenseKey = getString(R.string.label_license_key) + ":\n" + licenseKey;
        txtLicenseKey.setText(licenseKey);

        // Get and set DID
        String did = getString(R.string.label_did) + ":\n" + BlockIDSDK.getInstance().getDID();
        AppCompatTextView txtDid = findViewById(R.id.txt_about_did);
        txtDid.setText(did);

        // Get and set public key
        String publicKey = getString(R.string.label_public_key) + ":\n"
                + BlockIDSDK.getInstance().getPublicKey();
        AppCompatTextView txtPublicKey = findViewById(R.id.txt_about_public_key);
        txtPublicKey.setText(publicKey);

        // Get and set SDK version
        String sdkVersion = getString(R.string.label_sdk_version) + ":\n"
                + getVersionText(BlockIDSDK.getInstance().getVersion());
        AppCompatTextView txtSDKVersion = findViewById(R.id.txt_about_sdk_version);
        txtSDKVersion.setText(sdkVersion);

        // Get and set APP version
        String appVersion = getString(R.string.label_app_version) + ":\n"
                + getVersionText(BuildConfig.VERSION_NAME);
        AppCompatTextView txtAppVersion = findViewById(R.id.txt_about_app_version);
        txtAppVersion.setText(appVersion);

        // Prepare content for copying to clipboard
        String copy = txtTenantDNS.getText() + "\n"
                + txtLicenseKey.getText() + "\n"
                + txtDid.getText() + "\n"
                + txtPublicKey.getText() + "\n"
                + txtSDKVersion.getText() + "\n"
                + txtAppVersion.getText();

        // Button click copy the content
        AppCompatButton btnCopy = findViewById(R.id.btn_about_copy);
        btnCopy.setOnClickListener(view -> copyToClipboard(AboutActivity.this, copy));
    }

    /**
     * @param context Activity context
     * @param text    Content to be copy
     */
    private void copyToClipboard(Context context, String text) {
        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(
                    CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("msg", text);
            clipboard.setPrimaryClip(clip);
        } catch (Exception ignored) {
        }
    }

    /**
     * @param inputVersion version number
     * @return Return formatted version text
     */
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