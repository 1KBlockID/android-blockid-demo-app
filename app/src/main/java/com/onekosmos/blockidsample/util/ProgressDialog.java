package com.onekosmos.blockidsample.util;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;

import com.onekosmos.blockidsample.R;

import java.util.Objects;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class ProgressDialog extends Dialog {
    private final AppCompatTextView mTxtMessage;

    public ProgressDialog(@NonNull Context context) {
        super(context);
        setCancelable(false);
        setContentView(R.layout.dialog_loading);
        mTxtMessage = findViewById(R.id.txt_please_wait);
        Objects.requireNonNull(getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
    }

    public ProgressDialog(@NonNull Context context, String message) {
        super(context);
        setCancelable(false);
        setContentView(R.layout.dialog_loading);
        mTxtMessage = findViewById(R.id.txt_please_wait);
        mTxtMessage.setText(message);
        Objects.requireNonNull(getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
    }

    public void show(String message) {
        if (!TextUtils.isEmpty(message))
            mTxtMessage.setText(message);

        if (!this.isShowing())
            this.show();
    }
}