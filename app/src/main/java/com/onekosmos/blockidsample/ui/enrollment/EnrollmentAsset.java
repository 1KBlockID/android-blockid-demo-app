package com.onekosmos.blockidsample.ui.enrollment;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class EnrollmentAsset {
    private final boolean mAssetStatus;
    private final String mAssetTitle;
    private final String mAssetSubTitle;

    public EnrollmentAsset(boolean assetStatus, String assetTitle, String assetSubTitle) {
        mAssetStatus = assetStatus;
        mAssetTitle = assetTitle;
        mAssetSubTitle = assetSubTitle;
    }

    public String getAssetTitle() {
        return mAssetTitle;
    }

    public boolean getAssetSuccess() {
        return mAssetStatus;
    }

    public String getAssetSubTitle() {
        return mAssetSubTitle;
    }
}