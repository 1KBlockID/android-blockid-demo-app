package com.onekosmos.blockidsample.ui.passport;

import java.util.LinkedHashMap;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
enum PassportDataHolder {
    INSTANCE;
    private LinkedHashMap<String, Object> passportMap;
    private String token;

    public static boolean hasData() {
        return INSTANCE.passportMap != null;
    }

    public static void setData(final LinkedHashMap<String, Object> passportMap, final String token) {
        INSTANCE.passportMap = passportMap;
        INSTANCE.token = token;
    }

    public static LinkedHashMap<String, Object> getData() {
        return INSTANCE.passportMap;
    }

    public static String getToken() {
        return INSTANCE.token;
    }

    public static void clearData() {
        INSTANCE.passportMap = null;
        INSTANCE.token = null;
    }
}