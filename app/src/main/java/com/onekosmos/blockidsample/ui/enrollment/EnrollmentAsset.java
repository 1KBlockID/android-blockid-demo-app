package com.onekosmos.blockidsample.ui.enrollment;

/**
 * Created by Pankti Mistry on 05-05-2021.
 * Copyright © 2020 1Kosmos. All rights reserved.
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