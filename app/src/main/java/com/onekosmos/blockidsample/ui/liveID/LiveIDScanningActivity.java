package com.onekosmos.blockidsample.ui.liveID;

import android.Manifest;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.blockid.sdk.BIDAPIs.APIManager.ErrorManager;
import com.blockid.sdk.BlockIDSDK;
import com.blockid.sdk.cameramodule.BIDScannerView;
import com.blockid.sdk.cameramodule.ScanningMode;
import com.blockid.sdk.cameramodule.camera.liveIDModule.ILiveIDResponseListener;
import com.blockid.sdk.cameramodule.liveID.LiveIDScannerHelper;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.document.DocumentHolder;
import com.onekosmos.blockidsample.util.AppPermissionUtils;
import com.onekosmos.blockidsample.util.ErrorDialog;
import com.onekosmos.blockidsample.util.ProgressDialog;

import java.util.LinkedHashMap;

import static com.blockid.sdk.BIDAPIs.APIManager.ErrorManager.CustomErrors.K_SOMETHING_WENT_WRONG;
import static com.blockid.sdk.document.BIDDocumentProvider.RegisterDocCategory.identity_document;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class LiveIDScanningActivity extends AppCompatActivity implements View.OnClickListener, ILiveIDResponseListener {
    public static String LIVEID_WITH_DOCUMENT = "LIVEID_WITH_DOCUMENT";
    private final String[] K_CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private static final int K_LIVEID_PERMISSION_REQUEST_CODE = 1009;
    private AppCompatImageView mImgBack;
    private AppCompatTextView mTxtBack, mTxtMessage;
    private AppCompatButton mBtnCancel;
    private BIDScannerView mBIDScannerView;
    private LiveIDScannerHelper mLiveIDScannerHelper;
    private AppCompatImageView mScannerOverlay;
    private int mScannerOverlayMargin = 30;
    private LinearLayout mLayoutMessage;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_liveid_scan);
        initViews();
    }

    protected void onStart() {
        super.onStart();
        if (!AppPermissionUtils.isPermissionGiven(K_CAMERA_PERMISSION, this))
            AppPermissionUtils.requestPermission(this, K_LIVEID_PERMISSION_REQUEST_CODE, K_CAMERA_PERMISSION);
        else {
            mBIDScannerView.setVisibility(View.VISIBLE);
            mScannerOverlay.setVisibility(View.VISIBLE);

            mLiveIDScannerHelper = new LiveIDScannerHelper(this, ScanningMode.SCAN_LIVE, this, mBIDScannerView, mScannerOverlay);
            mLiveIDScannerHelper.startLiveIDScanning();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.img_back:
            case R.id.txt_back:
            case R.id.btn_cancel:
                finish();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (AppPermissionUtils.isGrantedPermission(this, requestCode, grantResults, K_CAMERA_PERMISSION)) {
            mBIDScannerView.setVisibility(View.VISIBLE);
            mScannerOverlay.setVisibility(View.VISIBLE);
            mLiveIDScannerHelper = new LiveIDScannerHelper(this, ScanningMode.SCAN_LIVE, this, mBIDScannerView, mScannerOverlay);
            mLiveIDScannerHelper.startLiveIDScanning();
        } else {
            ErrorDialog errorDialog = new ErrorDialog(this);
            errorDialog.show(null,
                    "",
                    getString(R.string.label_liveid_camera_permission_alert), dialog -> {
                        finish();
                    });
        }
    }

    @Override
    public void onFaceFocusChanged(boolean isFocused, String expression) {
        if (isFocused) {
            showFaceFocusedViews();
            mLayoutMessage.setVisibility(View.VISIBLE);
            mTxtMessage.setVisibility(View.VISIBLE);
            mTxtMessage.setText(getMessageForExpression(expression));
        } else
            showFaceNotFocusedViews();
    }

    @Override
    public void onLiveIDCaptured(Bitmap liveIDBitmap, String signatureToken, ErrorManager.ErrorResponse error) {
        mTxtMessage.setVisibility(View.GONE);
        mLayoutMessage.setVisibility(View.GONE);
        mLiveIDScannerHelper.stopLiveIDScanning();
        mBtnCancel.setClickable(false);
        mImgBack.setClickable(false);
        mTxtBack.setClickable(false);
        mBIDScannerView.setVisibility(View.GONE);
        mScannerOverlay.setVisibility(View.GONE);
        mBtnCancel.setVisibility(View.GONE);

        // call enrollLiveID func here
        ErrorDialog errorDialog = new ErrorDialog(this);
        if (liveIDBitmap == null) {
            errorDialog.show(null,
                    getString(R.string.label_error),
                    error.getMessage(), dialog -> {
                        errorDialog.dismiss();
                        finish();
                    });
            return;
        }
        if (getIntent().hasExtra(LIVEID_WITH_DOCUMENT) && getIntent().getBooleanExtra(LIVEID_WITH_DOCUMENT, false)) {
            registerLiveIDWithDocument(liveIDBitmap);
            return;
        }
        registerLiveID(liveIDBitmap);
    }

    private void initViews() {
        mBIDScannerView = findViewById(R.id.view_bid_scanner);
        mScannerOverlay = findViewById(R.id.view_overlay);
        mBIDScannerView.setScannerWidthMargin(mScannerOverlayMargin, mScannerOverlay);

        if (AppPermissionUtils.isPermissionGiven(K_CAMERA_PERMISSION, this)) {
            mBIDScannerView.setVisibility(View.VISIBLE);
        }

        mTxtMessage = findViewById(R.id.txt_msg);
        mLayoutMessage = findViewById(R.id.layout_msg);
        mImgBack = findViewById(R.id.img_back);
        mImgBack.setOnClickListener(this);

        mTxtBack = findViewById(R.id.txt_back);
        mTxtBack.setOnClickListener(this);

        mBtnCancel = findViewById(R.id.btn_cancel);
        mBtnCancel.setOnClickListener(this);
    }

    private void showFaceNotFocusedViews() {
        mScannerOverlay.setImageResource(R.drawable.group_3);
        mScannerOverlay.setColorFilter(getResources().getColor(R.color.misc2));
    }

    private void showFaceFocusedViews() {
        mScannerOverlay.setImageResource(R.drawable.group_3);
        mScannerOverlay.setColorFilter(getResources().getColor(R.color.misc1));
    }

    private String getMessageForExpression(String expression) {
        switch (expression) {
            case "Blink":
                return getResources().getString(R.string.label_liveid_please_blink_your_eyes);
            case "Smile":
                return getResources().getString(R.string.label_liveid_please_smile);
        }
        return "";
    }

    private void registerLiveID(Bitmap livIdBitmap) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();
        BlockIDSDK.getInstance().setLiveID(livIdBitmap, null, null, (status, msg, error) -> {
            progressDialog.dismiss();
            if (status) {
                Toast.makeText(this, getString(R.string.label_liveid_enrolled_successfully), Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            if (error == null)
                error = new ErrorManager.ErrorResponse(K_SOMETHING_WENT_WRONG.getCode(), K_SOMETHING_WENT_WRONG.getMessage());

            ErrorDialog errorDialog = new ErrorDialog(this);
            DialogInterface.OnDismissListener onDismissListener = dialogInterface -> {
                errorDialog.dismiss();
                finish();
            };
            if (error.getCode() == ErrorManager.CustomErrors.K_CONNECTION_ERROR.getCode()) {
                errorDialog.showNoInternetDialog(onDismissListener);
                return;
            }
            errorDialog.show(null, getString(R.string.label_error), error.getMessage(), onDismissListener);
        });
    }

    private void registerLiveIDWithDocument(Bitmap livIdBitmap) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();
        LinkedHashMap<String, Object> documentMap = DocumentHolder.getData();
        documentMap.put("category", identity_document.name());
        documentMap.put("type", documentMap.get("type"));
        documentMap.put("id", documentMap.get("id"));

        BlockIDSDK.getInstance().registerDocument(this, documentMap,
                livIdBitmap, null, null, null, (status, error) -> {
                    progressDialog.dismiss();
                    DocumentHolder.clearData();
                    if (status) {
                        Toast.makeText(this, getString(R.string.label_document_enrolled_successfully), Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    if (error == null)
                        error = new ErrorManager.ErrorResponse(K_SOMETHING_WENT_WRONG.getCode(), K_SOMETHING_WENT_WRONG.getMessage());

                    ErrorDialog errorDialog = new ErrorDialog(this);
                    DialogInterface.OnDismissListener onDismissListener = dialogInterface -> {
                        errorDialog.dismiss();
                        finish();
                    };
                    if (error.getCode() == ErrorManager.CustomErrors.K_CONNECTION_ERROR.getCode()) {
                        errorDialog.showNoInternetDialog(onDismissListener);
                        return;
                    }
                    errorDialog.show(null, getString(R.string.label_error), error.getMessage(), onDismissListener);
                });
    }
}