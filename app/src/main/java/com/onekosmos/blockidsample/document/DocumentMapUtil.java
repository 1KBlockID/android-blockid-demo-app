package com.onekosmos.blockidsample.document;

import com.blockid.sdk.datamodel.BIDDocumentData;
import com.blockid.sdk.document.BIDDocumentProvider;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class DocumentMapUtil {
    public static final String K_ID = "id";
    public static final String K_TYPE = "type";
    public static final String K_CATEGORY = "category";
    public static final String K_PROOFEDBY = "proofedBy";
    public static final String K_UUID = "uuid";
    public static final String K_FACE = "face";

    public static LinkedHashMap<String, Object> getDocumentMap(BIDDocumentData documentData, BIDDocumentProvider.RegisterDocCategory category) {
        try {
            LinkedHashMap<String, Object> dlMap = new LinkedHashMap<String, Object>();
            dlMap.put(K_ID, documentData.id);
            dlMap.put(K_TYPE, documentData.type);
            dlMap.put(K_CATEGORY, category.name());
            dlMap.put(K_PROOFEDBY, documentData.proofedBy);
            dlMap.put(K_UUID, new JSONObject(new GsonBuilder().disableHtmlEscaping().create().toJson(documentData)));
            return dlMap;
        } catch (JSONException e) {
            return null;
        }
    }
}