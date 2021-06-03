package com.onekosmos.blockidsample.document;

import java.util.LinkedHashMap;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public enum DocumentHolder {
    INSTANCE;
    private LinkedHashMap<String, Object> documentHolder;
    private String token;

    public static boolean hasData() {
        return INSTANCE.documentHolder != null;
    }

    public static void setData(final LinkedHashMap<String, Object> documentHolder, final String token) {
        INSTANCE.documentHolder = documentHolder;
        INSTANCE.token = token;
    }

    public static LinkedHashMap<String, Object> getData() {
        return INSTANCE.documentHolder;
    }

    public static String getToken() {
        return INSTANCE.token;
    }

    public static void clearData() {
        INSTANCE.documentHolder = null;
        INSTANCE.token = null;
    }
}