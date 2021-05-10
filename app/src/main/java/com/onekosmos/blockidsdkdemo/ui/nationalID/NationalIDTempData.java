package com.onekosmos.blockidsdkdemo.ui.nationalID;

/**
 * Created by Pankti Mistry on 10-04-2021.
 * Copyright © 2020 1Kosmos. All rights reserved.
 */
public class NationalIDTempData {
    private String mNationalIDFirstSideDataB64;
    private String mSignatureToken;

    private static final NationalIDTempData ourInstance = new NationalIDTempData();

    public static NationalIDTempData getInstance() {
        return ourInstance;
    }

    private NationalIDTempData() {
    }

    public void setNationalIDFirstSideDataB64(String editValue) {
        this.mNationalIDFirstSideDataB64 = editValue;
    }

    public String getNationalIDFirstSideDataB64() {
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
