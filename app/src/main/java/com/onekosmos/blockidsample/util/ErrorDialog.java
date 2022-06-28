package com.onekosmos.blockidsample.util;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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
public class ErrorDialog extends Dialog {
    private AppCompatTextView mTxtTitle, mTxtMessage;
    private AppCompatButton mBtn1, mBtn2;
    private AppCompatImageView mImgDialogIcon;
    private OnDismissListener mDismissListener;
    private View mViewDialogError, mViewDialogErrorVertical;
    private OnClickListener mOnClickListener;

    public ErrorDialog(@NonNull Context context) {
        super(context);
        setContentView(R.layout.dialog_error);
        setCancelable(false);
        Objects.requireNonNull(getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        mBtn1 = findViewById(R.id.btn_dialog_error_btn1);
        mBtn2 = findViewById(R.id.btn_dialog_error_btn2);
        mTxtMessage = findViewById(R.id.txt_dialog_error_message);
        mTxtTitle = findViewById(R.id.txt_dialog_error_title);
        mImgDialogIcon = findViewById(R.id.img_dialog_error_icon);
        mViewDialogError = findViewById(R.id.view_dialog_error);
        mViewDialogErrorVertical = findViewById(R.id.view_dialog_error_vertical);
        mBtn1.setOnClickListener(v -> {
            mDismissListener.onDismiss(this);
            this.dismiss();
        });
    }

    public void show(Drawable dialogIcon, String title, String message, Dialog.OnDismissListener dismissListener) {
        mDismissListener = dismissListener;
        mBtn1.setVisibility(View.VISIBLE);
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

    public void showWithOKButton(Drawable dialogIcon, String title, String message, Dialog.OnDismissListener dismissListener) {
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

    public void showWithOneButton(Drawable dialogIcon, String title, String message, String btnTitle, Dialog.OnDismissListener dismissListener) {
        mDismissListener = dismissListener;
        mBtn1.setVisibility(View.VISIBLE);
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
            if (!TextUtils.isEmpty(btnTitle)) {
                mBtn1.setText(btnTitle);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.show();
    }

    public void showWithTwoButton(Drawable dialogIcon, String title, String message, String btn1Title, String btn2Title, OnClickListener clickListener, Dialog.OnDismissListener dismissListener) {
        mDismissListener = dismissListener;
        mOnClickListener = clickListener;
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
            if (!TextUtils.isEmpty(btn1Title)) {
                mBtn1.setVisibility(View.VISIBLE);
                mBtn1.setText(btn1Title);
            }
            if (!TextUtils.isEmpty(btn2Title)) {
                mBtn2.setVisibility(View.VISIBLE);
                mBtn2.setText(btn2Title);
                mViewDialogErrorVertical.setVisibility(View.VISIBLE);
            }

            mBtn1.setOnClickListener(v -> {
                mDismissListener.onDismiss(this);
                this.dismiss();
            });

            mBtn2.setOnClickListener(v -> {
                mOnClickListener.onClick(this, DialogInterface.BUTTON_POSITIVE);
                this.dismiss();
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
        this.show();
    }

    public void showNoInternetDialog(Dialog.OnDismissListener dismissListener) {
        mDismissListener = dismissListener;
        mBtn1.setVisibility(View.VISIBLE);
        try {
            mImgDialogIcon.setVisibility(View.GONE);
            mTxtTitle.setVisibility(View.VISIBLE);
            mTxtTitle.setText(R.string.label_your_are_offline);
            mTxtMessage.setVisibility(View.VISIBLE);
            mTxtMessage.setText(R.string.label_please_check_your_internet_connection);
        } catch (
                Exception e) {
            e.printStackTrace();
        }
        this.show();
    }
}