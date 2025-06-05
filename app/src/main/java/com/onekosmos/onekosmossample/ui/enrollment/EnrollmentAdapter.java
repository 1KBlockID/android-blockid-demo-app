package com.onekosmos.onekosmossample.ui.enrollment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.onekosmos.onekosmossample.R;

import java.util.List;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class EnrollmentAdapter extends RecyclerView.Adapter<EnrollmentAdapter.BiometricAssetViewHolder> {
    private final EnrollmentClickListener mClickListener;
    private final List<EnrollmentAsset> mEnrollmentAssets;

    public static class BiometricAssetViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mAssetStatus;
        private final TextView mAssetTitle, mAssetSubTitle;

        private BiometricAssetViewHolder(View view) {
            super(view);
            mAssetTitle = view.findViewById(R.id.txt_asset_name);
            mAssetSubTitle = view.findViewById(R.id.txt_asset_sub_name);
            mAssetStatus = view.findViewById(R.id.img_enrollment_status);
        }
    }

    public EnrollmentAdapter(EnrollmentClickListener clickListener, List<EnrollmentAsset> biometricAssets) {
        mClickListener = clickListener;
        mEnrollmentAssets = biometricAssets;
    }

    @NonNull
    @Override
    public BiometricAssetViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_enrollment, parent, false);

        return new BiometricAssetViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull BiometricAssetViewHolder holder, int position) {
        EnrollmentAsset enrollmentAsset = mEnrollmentAssets.get(position);
        if (enrollmentAsset.getAssetSuccess())
            holder.mAssetStatus.setVisibility(View.VISIBLE);
        else
            holder.mAssetStatus.setVisibility(View.INVISIBLE);
        holder.mAssetTitle.setText(enrollmentAsset.getAssetTitle());
        if (!TextUtils.isEmpty(enrollmentAsset.getAssetSubTitle())) {
            holder.mAssetSubTitle.setVisibility(View.VISIBLE);
            holder.mAssetSubTitle.setText(enrollmentAsset.getAssetSubTitle());
        } else {
            holder.mAssetSubTitle.setVisibility(View.GONE);
        }
        holder.mAssetTitle.setOnClickListener(view ->
                mClickListener.onclick(mEnrollmentAssets, position));
    }

    @Override
    public int getItemCount() {
        return mEnrollmentAssets.size();
    }

    public interface EnrollmentClickListener {
        void onclick(List<EnrollmentAsset> biometricAssets, int position);
    }
}