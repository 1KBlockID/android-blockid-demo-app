package com.onekosmos.blockidsample.util;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.onekosmos.blockidsample.R;

import java.util.Objects;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class SuccessDialog extends Dialog {
    private AppCompatTextView mTxtTitle, mTxtMessage;
    private AppCompatButton mBtn1;
    private AppCompatImageView mImgDialogIcon;
    private OnDismissListener mDismissListener;
    private OnClickListener mOnClickListener;

    public SuccessDialog(@NonNull Context context) {
        super(context);
        setContentView(R.layout.dialog_error);
        setCancelable(false);
        Objects.requireNonNull(getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        mBtn1 = findViewById(R.id.btn_dialog_error_btn1);

        mTxtMessage = findViewById(R.id.txt_dialog_error_message);
        mTxtTitle = findViewById(R.id.txt_dialog_error_title);
        mImgDialogIcon = findViewById(R.id.img_dialog_error_icon);

        mBtn1.setOnClickListener(v -> {
            mDismissListener.onDismiss(this);
            this.dismiss();
        });
    }

    public void show(Drawable dialogIcon, String title, String message, OnDismissListener dismissListener) {
        mDismissListener = dismissListener;
        mBtn1.setVisibility(View.VISIBLE);
        mBtn1.setText(R.string.label_ok);
        try {
            if (dialogIcon != null) {
                mImgDialogIcon.setVisibility(View.VISIBLE);
                mImgDialogIcon.setImageDrawable(dialogIcon);
            }
            if (!TextUtils.isEmpty(title)) {
                mTxtTitle.setVisibility(View.VISIBLE);
                mTxtTitle.setText(title);
            }
            if (!TextUtils.isEmpty(message)) {
                mTxtMessage.setVisibility(View.VISIBLE);
                mTxtMessage.setText(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.show();
    }
}