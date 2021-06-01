package com.onekosmos.blockidsample.document;

import com.blockid.sdk.document.BIDDocumentProvider;

import java.util.LinkedHashMap;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public enum DocumentHolder {
    INSTANCE;
    private LinkedHashMap<String, Object> documentHolder;
    private BIDDocumentProvider.BIDDocumentType documentType;
    private String token;

    public static boolean hasData() {
        return INSTANCE.documentHolder != null;
    }

    public static void setData(final LinkedHashMap<String, Object> documentHolder, final BIDDocumentProvider.BIDDocumentType documentType, final String token) {
        INSTANCE.documentHolder = documentHolder;
        INSTANCE.documentType = documentType;
        INSTANCE.token = token;
    }

    public static LinkedHashMap<String, Object> getData() {
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