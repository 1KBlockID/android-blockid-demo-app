package com.onekosmos.blockidsample.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Base64;

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
}
