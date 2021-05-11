package com.onekosmos.blockidsample.ui.enrollment;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class EnrollmentAsset {
    private boolean mAssetStatus;
    private String mAssetTitle;

    public EnrollmentAsset(boolean assetStatus, String assetTitle) {
        mAssetStatus = assetStatus;
        mAssetTitle = assetTitle;
    }

    public String getAssetTitle() {
        return mAssetTitle;
    }

    public boolean getAssetSuccess() {
        return mAssetStatus;
    }
}