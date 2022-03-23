package com.onekosmos.blockidsample.util;

import android.app.Dialog;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.onekosmos.blockidsample.R;

import java.util.Objects;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2022 1Kosmos. All rights reserved.
 */
public class ResultDialog extends Dialog {

    /**
     * @param context    context in which the dialog should show
     * @param imageId    resource identifier of the drawable to set in dialog icon
     * @param message    text to be display in message
     * @param subMessage text to be display in sub message
     */
    public ResultDialog(@NonNull Context context, int imageId, String message,
                        String subMessage) {
        super(context);
        setCancelable(false);
        setContentView(R.layout.dialog_result);
        AppCompatImageView imgDialogIcon = findViewById(R.id.img_dialog_icon);
        imgDialogIcon.setImageResource(imageId);
        AppCompatTextView txtDialogMessage = findViewById(R.id.txt_dialog_message);
        txtDialogMessage.setText(message);
        AppCompatTextView txtDialogSubMessage = findViewById(R.id.txt_dialog_sub_message);
        txtDialogSubMessage.setText(subMessage);
        Objects.requireNonNull(getWindow()).setBackgroundDrawableResource
                (android.R.color.transparent);
    }
}