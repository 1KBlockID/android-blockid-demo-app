package com.onekosmos.blockidsample.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

/*
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class AppUtil {
    public static Bitmap imageBase64ToBitmap(String img) {
        if (TextUtils.isEmpty(img))
            return null;

        byte[] decodedString = Base64.decode(img, Base64.DEFAULT);
        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        return decodedByte;
    }

    public static String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null)
            return null;

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    /**
     * @param img base64 of image
     * @return bitmap of image
     */
    public static Bitmap convertBase64ToBitmap(String img) {
        if (TextUtils.isEmpty(img)) {
            return null;
        }
        byte[] decodedString = Base64.decode(img, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }

    /**
     * @param byteArrayImage byte array of image
     * @return base64 string of byte array
     */
    public static String getBase64FromBytes(byte[] byteArrayImage) {
        return Base64.encodeToString(byteArrayImage, Base64.NO_WRAP);
    }
}
