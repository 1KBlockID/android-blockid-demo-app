package com.onekosmos.blockidsample.ui.nationalID;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.onekosmos.blockid.sdk.cameramodule.nationalID.NationalIDManualScanHelper;
import com.onekosmos.blockidsample.R;
import com.onekosmos.blockidsample.util.AppUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class NationalIDManualCaptureActivity extends AppCompatActivity {
    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1001;
    private AppCompatImageView mImgDocumentBack, mImgDocumentFront, mImgFace;
    private AppCompatButton mBtnCaptureBack, mBtnCaptureFront;
    private boolean isBackCaptured = false;
    private NationalIDManualScanHelper mNationalIDHelper;
    private AppCompatTextView mTxtBackData, mTxtFrontData;
    private LinkedHashMap<String, Object> mNationalIdMap = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_national_id_manual_capture);
        initView();
        mNationalIDHelper = new NationalIDManualScanHelper(this);
    }

    private void initView() {
        findViewById(R.id.img_back).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.txt_back).setOnClickListener(v -> onBackPressed());
        mTxtBackData = findViewById(R.id.txt_back_document_data);
        mTxtFrontData = findViewById(R.id.txt_front_document_data);
        mBtnCaptureBack = findViewById(R.id.btn_capture_back);
        mBtnCaptureFront = findViewById(R.id.btn_capture_front);
        mBtnCaptureBack.setOnClickListener(v -> {
            if (checkAndRequestPermissions(NationalIDManualCaptureActivity.this)) {
                chooseImage(NationalIDManualCaptureActivity.this);
            }
        });

        mBtnCaptureFront.setOnClickListener(v -> {
            if (checkAndRequestPermissions(NationalIDManualCaptureActivity.this)) {
                chooseImage(NationalIDManualCaptureActivity.this);
            }
        });

        mImgDocumentBack = findViewById(R.id.img_document_back);
        mImgDocumentFront = findViewById(R.id.img_document_front);
        mImgFace = findViewById(R.id.img_face);
    }

    public static boolean checkAndRequestPermissions(Activity activity) {
        int storePermission = ContextCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int cameraPermission = ContextCompat.checkSelfPermission(activity,
                Manifest.permission.CAMERA);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CAMERA);
        }
        if (storePermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded
                    .add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(activity, listPermissionsNeeded
                            .toArray(new String[0]),
                    REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_ID_MULTIPLE_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(),
                        "Requires Access to Camara.", Toast.LENGTH_SHORT)
                        .show();
            } else if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(),
                        "FlagUp Requires Access to Your Storage.",
                        Toast.LENGTH_SHORT).show();
            } else {
                chooseImage(this);
            }
        }
    }

    // function to let's the user to choose image from camera or gallery
    private void chooseImage(Activity context) {
        final CharSequence[] optionsMenu = {"Take Photo", "Choose from Gallery", "Exit"};
        // create a menuOption Array
        // create a dialog for showing the optionsMenu
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        // set the items in builder
        builder.setItems(optionsMenu, (dialogInterface, i) -> {
            if (optionsMenu[i].equals("Take Photo")) {
                // Open the camera and get the photo
                Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(takePicture, 0);
            } else if (optionsMenu[i].equals("Choose from Gallery")) {
                // choose from  external storage
                Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickPhoto, 1);
            } else if (optionsMenu[i].equals("Exit")) {
                dialogInterface.dismiss();
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_CANCELED) {
            switch (requestCode) {
                case 0:
                    if (resultCode == RESULT_OK && data != null) {
                        Bitmap selectedImage = (Bitmap) data.getExtras().get("data");
                        upDateBackCapturedUi(selectedImage);
                    }
                    break;
                case 1:
                    if (resultCode == RESULT_OK && data != null) {
                        Uri selectedImage = data.getData();
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};
                        if (selectedImage != null) {
                            Cursor cursor = getContentResolver().query(selectedImage,
                                    filePathColumn, null, null, null);
                            if (cursor != null) {
                                cursor.moveToFirst();
                                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                                String picturePath = cursor.getString(columnIndex);
                                Bitmap bitmap = BitmapFactory.decodeFile(picturePath);
                                cursor.close();
                                upDateBackCapturedUi(bitmap);
                            }
                        }
                    }
                    break;
            }
        }
    }

    private void upDateBackCapturedUi(Bitmap bitmap) {
        if (!isBackCaptured) {
            mImgDocumentBack.setImageBitmap(bitmap);
            isBackCaptured = true;
            mBtnCaptureBack.setText("Process Back Image");
            mBtnCaptureBack.setOnClickListener(v -> {
                mBtnCaptureBack.setEnabled(false);
                mNationalIDHelper.processBackImage(bitmap, (response, error) -> {
                    mNationalIdMap = response;
                    if (mNationalIdMap != null) {
                        String documentDetails = "" +
                                "Document ID: " + mNationalIdMap.get("id") +
                                "\nMRZ: \n" + mNationalIdMap.get("mrzResult") +
                                "\nQR Code: " + mNationalIdMap.get("qrCodeData");
                        mTxtBackData.setText(documentDetails);
                    } else {
                        String errorData = "Error :" + error.getMessage();
                        mTxtBackData.setText(errorData);
                    }
                    mBtnCaptureFront.setVisibility(View.VISIBLE);
                });
            });
        } else {
            mImgDocumentFront.setImageBitmap(bitmap);
            mBtnCaptureFront.setText("Process Front Image");
            mBtnCaptureFront.setOnClickListener(v -> {
                mBtnCaptureFront.setEnabled(false);
                mNationalIDHelper.processFrontImage(bitmap, (nationalIDMap, error) -> {
                    if (nationalIDMap != null) {
                        mTxtFrontData.setText("OCR: \n" + nationalIDMap.get("ocr"));
                        String base64Face = nationalIDMap.get("face").toString();
                        if (!TextUtils.isEmpty(base64Face)) {
                            mImgFace.setImageBitmap(AppUtil.imageBase64ToBitmap(base64Face));
                        }
                    } else {
                        String errorData = "Error :" + error.getMessage();
                        mTxtFrontData.setText(errorData);
                    }
                });
            });
        }
    }
}