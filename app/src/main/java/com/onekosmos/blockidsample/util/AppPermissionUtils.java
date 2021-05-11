package com.onekosmos.blockidsample.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.onekosmos.blockidsample.R;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class AppPermissionUtils {
    public static void requestPermission(Activity activity, int requestCode, String[] requestPermission) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            activity.requestPermissions(requestPermission, requestCode);
        }
    }

    public static void requestPermission(Fragment fragment, int requestCode, String[] requestPermission) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            fragment.requestPermissions(requestPermission, requestCode);
        }
    }


    public static boolean isGrantedPermission(int requestCode, int[] grantResults, String[] permissions, Fragment fragment) {
        for (int i = 0; i < grantResults.length; i++) {
            boolean showRationale = fragment.shouldShowRequestPermissionRationale(permissions[i]);
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                showDialogBox(fragment, requestCode, showRationale, permissions);
                return false;
            }
        }
        return true;
    }

    public static boolean isGrantedPermission(int requestCode, int[] grantResults, String[] permissions, Activity activity) {
        for (int i = 0; i < grantResults.length; i++) {
            boolean showRationale = activity.shouldShowRequestPermissionRationale(permissions[i]);
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                showDialogBox(activity, requestCode, showRationale, permissions);
                return false;
            }
        }
        return true;
    }

    public static boolean isGrantedPermission(Activity activity, int requestCode, int[] grantResults, String[] permissions) {
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static boolean isPermissionGiven(String[] requestPermission, Activity activity) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            String array[] = requestPermission;
            for (int i = 0; i < array.length; i++) {
                if (ActivityCompat.checkSelfPermission(activity, array[i]) != PackageManager.PERMISSION_GRANTED)
                    return false;
            }
        }
        return true;
    }


    private static void showDialogBox(Activity activity, int requestCode, boolean showRationale, String[] permissions) {
        Dialog dialog = new Dialog(activity);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.dialog_permission);
        AppCompatButton mBtnCancel = dialog.findViewById(R.id.btn_permission_dialog_cancel);
        mBtnCancel.setOnClickListener(v -> dialog.dismiss());
        AppCompatButton mBtnOk = dialog.findViewById(R.id.btn_permission_dialog_ok);
        mBtnOk.setOnClickListener(v -> {
            dialog.dismiss();
            if (!showRationale)
                openSettings(activity);
            else
                requestPermission(activity, requestCode, permissions);
        });
        dialog.show();
    }

    private static void showDialogBox(Fragment fragment, int requestCode, boolean showRationale, String[] permissions) {
        Dialog dialog = new Dialog(fragment.requireActivity());
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.dialog_permission);
        AppCompatButton mBtnCancel = dialog.findViewById(R.id.btn_permission_dialog_cancel);
        mBtnCancel.setOnClickListener(v -> dialog.dismiss());
        AppCompatButton mBtnOk = dialog.findViewById(R.id.btn_permission_dialog_ok);
        mBtnOk.setOnClickListener(v -> {
            dialog.dismiss();
            if (!showRationale)
                openSettings(fragment.getActivity());
            else
                requestPermission(fragment, requestCode, permissions);
        });
        dialog.show();
    }

    /**
     * Open device app settings to allow user to enable permissions
     */
    public static void openSettings(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivity(intent);
    }
}