package com.onekosmos.blockidsample.ui.nationalID;

import java.util.LinkedHashMap;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class NationalIDTempData {
    private LinkedHashMap<String,Object> mNationalIDFirstSideDataB64;
    private String mSignatureToken;

    private static final NationalIDTempData ourInstance = new NationalIDTempData();

    public static NationalIDTempData getInstance() {
        return ourInstance;
    }

    private NationalIDTempData() {
    }

    public void setNationalIDFirstSideData(LinkedHashMap<String,Object> editValue) {
        this.mNationalIDFirstSideDataB64 = editValue;
    }

    public LinkedHashMap<String,Object> getNationalIDFirstSideData() {
        return mNationalIDFirstSideDataB64;
    }

    public void clearNationalIDData() {
        mSignatureToken = null;
        mNationalIDFirstSideDataB64 = null;
    }

    public String getmSignatureToken() {
        return mSignatureToken;
    }

    public void setmSignatureToken(String mSignatureToken) {
        this.mSignatureToken = mSignatureToken;
    }
}