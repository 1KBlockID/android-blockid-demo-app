package com.onekosmos.blockidsample.util;

import android.app.Dialog;
import android.content.Context;

import androidx.annotation.NonNull;

import com.onekosmos.blockidsample.R;

import java.util.Objects;

/**
 * Created by Pankti Mistry on 30-04-2021.
 * Copyright © 2020 1Kosmos. All rights reserved.
 */
public class ProgressDialog extends Dialog {
    public ProgressDialog(@NonNull Context context) {
        super(context);
        setCancelable(false);
        setContentView(R.layout.dialog_loading);
        Objects.requireNonNull(getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
    }
}