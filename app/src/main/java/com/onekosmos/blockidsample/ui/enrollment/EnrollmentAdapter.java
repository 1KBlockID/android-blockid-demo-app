package com.onekosmos.blockidsample.ui.enrollment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.onekosmos.blockidsample.R;

import java.util.List;

/**
 * Created by Pankti Mistry on 05-05-2021.
 * Copyright © 2020 1Kosmos. All rights reserved.
 */
public class EnrollmentAdapter extends RecyclerView.Adapter<EnrollmentAdapter.BiometricAssetViewHolder> {
    private EnrollmentClickListener mClickListener;
    private List<EnrollmentAsset> mEnrollmentAssets;

    public class BiometricAssetViewHolder extends RecyclerView.ViewHolder {
        private ImageView mAssetStatus;
        private TextView mAssetTitle;

        private BiometricAssetViewHolder(View view) {
            super(view);
            mAssetTitle = view.findViewById(R.id.txt_asset_name);
            mAssetStatus = view.findViewById(R.id.img_enrollment_status);
        }
    }

    public EnrollmentAdapter(EnrollmentClickListener clickListener, List<EnrollmentAsset> biometricAssets) {
        mClickListener = clickListener;
        mEnrollmentAssets = biometricAssets;
    }

    @Override
    public BiometricAssetViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_enrollment, parent, false);

        return new BiometricAssetViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(BiometricAssetViewHolder holder, int position) {
        EnrollmentAsset enrollmentAsset = mEnrollmentAssets.get(position);
        if (enrollmentAsset.getAssetSuccess())
            holder.mAssetStatus.setVisibility(View.VISIBLE);
        else
            holder.mAssetStatus.setVisibility(View.INVISIBLE);
        holder.mAssetTitle.setText(enrollmentAsset.getAssetTitle());
        holder.mAssetTitle.setOnClickListener(view -> mClickListener.onclick(mEnrollmentAssets, position));
    }

    @Override
    public int getItemCount() {
        return mEnrollmentAssets.size();
    }

    public interface EnrollmentClickListener {
        void onclick(List<EnrollmentAsset> biometricAssets, int position);
    }
}