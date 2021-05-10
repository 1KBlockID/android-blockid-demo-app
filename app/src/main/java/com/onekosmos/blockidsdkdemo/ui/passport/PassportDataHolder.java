package com.onekosmos.blockidsdkdemo.ui.passport;

import com.blockid.sdk.datamodel.BIDPassport;

/**
 * Created by Pankti Mistry on 10-05-2021.
 * Copyright © 2020 1Kosmos. All rights reserved.
 */
enum PassportDataHolder {
    INSTANCE;
    private BIDPassport passportData;
    private String token;

    public static boolean hasData() {
        return INSTANCE.passportData != null;
    }

    public static void setData(final BIDPassport bidDocumentData, final String token) {
        INSTANCE.passportData = bidDocumentData;
        INSTANCE.token = token;
    }

    public static BIDPassport getData() {
        return INSTANCE.passportData;
    }

    public static String getToken() {
        return INSTANCE.token;
    }

    public static void clearData() {
        INSTANCE.passportData = null;
        INSTANCE.token = null;
    }
}