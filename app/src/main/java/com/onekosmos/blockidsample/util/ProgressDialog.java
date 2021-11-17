package com.onekosmos.blockidsample.util;

import android.app.Dialog;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;

import com.onekosmos.blockidsample.R;

import java.util.Objects;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class ProgressDialog extends Dialog {
    public ProgressDialog(@NonNull Context context) {
        super(context);
        setCancelable(false);
        setContentView(R.layout.dialog_loading);
        Objects.requireNonNull(getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
    }

    public ProgressDialog(@NonNull Context context, String message) {
        super(context);
        setCancelable(false);
        setContentView(R.layout.dialog_loading);
        AppCompatTextView txtMessage = findViewById(R.id.txt_please_wait);
        txtMessage.setText(message);
        Objects.requireNonNull(getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
    }
}