package com.onekosmos.blockidsdkdemo.ui.utils;

import android.app.Dialog;
import android.content.Context;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;

import com.example.blockidsdkdemo.R;

import java.util.Objects;

/**
 * Created by Pankti Mistry on 30-04-2021.
 * Copyright © 2020 1Kosmos. All rights reserved.
 */
public class ProgressDialog extends Dialog {
    private AppCompatTextView mTxtPleaseWait;
    private ProgressBar mProgressBar;

    public ProgressDialog(@NonNull Context context) {
        super(context);
        setCancelable(false);
        setContentView(R.layout.dialog_loading);
        mTxtPleaseWait = findViewById(R.id.txt_please_wait);
        mProgressBar = findViewById(R.id.progress_bar_enroll);
        Objects.requireNonNull(getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
    }
}
