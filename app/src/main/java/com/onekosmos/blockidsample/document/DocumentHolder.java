package com.onekosmos.blockidsample.document;

import java.util.LinkedHashMap;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public enum DocumentHolder {
    INSTANCE;
    private LinkedHashMap<String, Object> documentHolder;

    public static boolean hasData() {
        return INSTANCE.documentHolder != null;
    }

    public static void setData(final LinkedHashMap<String, Object> documentHolder) {
        INSTANCE.documentHolder = documentHolder;
    }

    public static LinkedHashMap<String, Object> getData() {
        return INSTANCE.documentHolder;
    }


    public static void clearData() {
        INSTANCE.documentHolder = null;
    }
}