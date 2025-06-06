package com.onekosmos.blockidsample.document;

import java.util.LinkedHashMap;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public enum DocumentHolder {
    INSTANCE;
    private LinkedHashMap<String, Object> documentHolder;
    private String liveIDImageBase64;
    private String liveIDProofedBy;

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
        INSTANCE.liveIDImageBase64 = null;
        INSTANCE.liveIDProofedBy = null;
    }

    public void setLiveIDImageBase64(String liveIDImageBase64) {
        INSTANCE.liveIDImageBase64 = liveIDImageBase64;
    }

    public String getLiveIDImageBase64() {
        return INSTANCE.liveIDImageBase64;
    }

    public void setLiveIDProofedBy(String liveIDProofedBy) {
        INSTANCE.liveIDProofedBy = liveIDProofedBy;
    }

    public String getLiveIDProofedBy() {
        return INSTANCE.liveIDProofedBy;
    }

}