package com.onekosmos.blockidsample.ui.walletconnect;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.recyclerview.widget.RecyclerView;

import com.onekosmos.blockidsample.R;
import com.walletconnect.sign.client.Sign;

import java.util.List;
import java.util.Objects;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2022 1Kosmos. All rights reserved.
 */
public class DAppAdapter extends RecyclerView.Adapter<DAppAdapter.ViewHolder> {
    private final List<DAppData> mDAppList;
    private int mSelectedPosition = 0;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final AppCompatRadioButton mRadioButton;


        private ViewHolder(View view) {
            super(view);
            mRadioButton = view.findViewById(R.id.radio_btn_dapp_url);
        }
    }

    public DAppAdapter(List<DAppData> list) {
        mDAppList = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.row_conncted_dapp, parent, false);

        return new ViewHolder(itemView);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (mSelectedPosition >= mDAppList.size())
            mSelectedPosition = 0;

        holder.mRadioButton.setText(Objects.requireNonNull(
                mDAppList.get(position).session.getMetaData()).getUrl());

        holder.mRadioButton.setOnCheckedChangeListener(null);

        if (position == mSelectedPosition)
            mDAppList.get(position).checked = true;

        holder.mRadioButton.setChecked(mSelectedPosition == position &&
                mDAppList.get(position).checked);

        holder.mRadioButton.setOnCheckedChangeListener((compoundButton, check) -> {
            if (compoundButton.isChecked()) {
                holder.mRadioButton.setChecked(true);
                mSelectedPosition = holder.getBindingAdapterPosition();
            } else {
                holder.mRadioButton.setChecked(false);
            }

            for (int i = 0; i < mDAppList.size(); i++) {
                mDAppList.get(i).checked = i == mSelectedPosition;
                notifyItemChanged(i);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDAppList.size();
    }

    public DAppData getSelectedItem() {
        return mDAppList.get(mSelectedPosition);
    }

    @Keep
    static class DAppData {
        Sign.Model.Session session;
        boolean checked;
    }
}