package com.onekosmos.blockidsample.ui.qrAuth;

import androidx.annotation.Keep;

import com.google.gson.annotations.SerializedName;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2022 1Kosmos. All rights reserved.
 */
@Keep
public class AuthRequestModel2 {
    public String scopes;
    public String authtype;
    @SerializedName("_id")
    public String authRequestId;
    public String sessionId;
    public Origin origin;
    public String publicKey;
    public String createdTS;
    public String expiryTS;
    public String expiresDate;
    @SerializedName("__v")
    public String version;

    public AuthRequestModel getAuthRequestModel(String sessionUrl) {
        AuthRequestModel authRequestModel = new AuthRequestModel();
        authRequestModel.authtype = authtype;
        authRequestModel.scopes = scopes;
        authRequestModel.creds = "";
        authRequestModel.publicKey = publicKey;
        authRequestModel.session = sessionId;
        authRequestModel.api = origin.url;
        authRequestModel.tag = origin.tag;
        authRequestModel.community = origin.communityName;
        authRequestModel.authPage = origin.authPage;
        authRequestModel.sessionURL = sessionUrl;

        return authRequestModel;
    }

    public static class Origin {
        public String tag;
        public String url;
        public String communityName;
        public String communityId;
        public String authPage;
    }
}

