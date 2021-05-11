package com.onekosmos.blockidsample.util;

import com.blockid.sdk.datamodel.BIDDocumentData;
import com.blockid.sdk.document.BIDDocumentProvider;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public enum DocumentHolder {
    INSTANCE;
    private BIDDocumentData documentHolder;
    private BIDDocumentProvider.BIDDocumentType documentType;
    private String token;

    public static boolean hasData() {
        return INSTANCE.documentHolder != null;
    }

    public static void setData(final BIDDocumentData documentHolder, final BIDDocumentProvider.BIDDocumentType documentType, final String token) {
        INSTANCE.documentHolder = documentHolder;
        INSTANCE.documentType = documentType;
        INSTANCE.token = token;
    }

    public static BIDDocumentData getData() {
        return INSTANCE.documentHolder;
    }

    public static BIDDocumentProvider.BIDDocumentType getType() {
        return INSTANCE.documentType;
    }

    public static String getToken() {
        return INSTANCE.token;
    }

    public static void clearData() {
        INSTANCE.documentHolder = null;
        INSTANCE.token = null;
    }
}