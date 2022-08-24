package com.onekosmos.blockidsample.ui.walletconnect;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.recyclerview.widget.RecyclerView;

import com.onekosmos.blockidsample.R;
import com.walletconnect.sign.client.Sign;

import java.util.List;
import java.util.Objects;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class DAppAdapter extends RecyclerView.Adapter<DAppAdapter.ViewHolder> {
    private final List<Sign.Model.Session> mDAppUrls;
    private int mSelectedPosition = 0;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final AppCompatRadioButton mRadioButton;


        private ViewHolder(View view) {
            super(view);
            mRadioButton = view.findViewById(R.id.radio_btn_dapp_url);
        }
    }

    public DAppAdapter(List<Sign.Model.Session> sessions) {
        mDAppUrls = sessions;
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
        holder.mRadioButton.setText(Objects.requireNonNull(
                mDAppUrls.get(position).getMetaData()).getUrl());
        holder.mRadioButton.setChecked(position == mSelectedPosition);
        holder.mRadioButton.setOnCheckedChangeListener((compoundButton, check) -> {
            if (check) {
                mSelectedPosition = holder.getBindingAdapterPosition();
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDAppUrls.size();
    }

    @Override
    public long getItemId(int position) {
        return mSelectedPosition;
    }
}