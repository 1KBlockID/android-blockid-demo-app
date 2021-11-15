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
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.onekosmos.blockid.sdk.cameramodule.nationalID.NationalIDManualScanHelper;
import com.onekosmos.blockidsample.R;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class NationalIDManualCaptureActivity extends AppCompatActivity {
    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1001;
    private AppCompatImageView mImgDocumentBack, mImgDocumentFront;
    private AppCompatButton mBtnCaptureBack, mBtnCaptureFront;
    private boolean isBackCaptured = false;
    private NationalIDManualScanHelper mNationalIDHelper;
    private AppCompatTextView mTxtBackData;
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
                            .toArray(new String[listPermissionsNeeded.size()]),
                    REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ID_MULTIPLE_PERMISSIONS:
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(),
                            "Requires Access to Camara.", Toast.LENGTH_SHORT)
                            .show();
                } else if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(),
                            "FlagUp Requires Access to Your Storage.",
                            Toast.LENGTH_SHORT).show();
                } else {
                    chooseImage(this);
                }
                break;
        }
    }


    // function to let's the user to choose image from camera or gallery
    private void chooseImage(Activity context) {
        final CharSequence[] optionsMenu = {"Take Photo", "Choose from Gallery", "Exit"}; // create a menuOption Array
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
                Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
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
                            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
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
            mBtnCaptureBack.setClickable(false);
            mBtnCaptureBack.setOnClickListener(v -> mNationalIDHelper.processBackImage(bitmap, (response, error) -> {
                mNationalIdMap = response;

                if (mNationalIdMap != null) {
                    mTxtBackData.setText("" + "Document ID: " + mNationalIdMap.get("id") + "\nMRZ: \n" +
                            mNationalIdMap.get("mrzResult") + "\nQR Code: " + mNationalIdMap.get("qrCodeData"));
                } else {
                    mTxtBackData.setText("Error :" + error.getMessage());
                }
            }));
        } else {
            mImgDocumentFront.setImageBitmap(bitmap);
            mBtnCaptureFront.setText("Process Front Image");
        }
    }
}