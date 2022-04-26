package com.onekosmos.blockidsample.ui.qrAuth;

import androidx.annotation.Keep;

import com.google.gson.annotations.SerializedName;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2022 1Kosmos. All rights reserved.
 */
@Keep
public class AuthenticationPayloadV2 {
    public String scopes;
    @SerializedName("authtype")
    public String authType;
    @SerializedName("_id")
    public String requestId;
    public String sessionId;
    public Origin origin;
    public String publicKey;
    public String createdTS;
    public String expiryTS;
    public String expiresDate;
    @SerializedName("__v")
    public String version;

    public AuthenticationPayloadV1 getAuthRequestModel(String sessionUrl) {
        AuthenticationPayloadV1 authenticationPayloadV1 = new AuthenticationPayloadV1();
        authenticationPayloadV1.authType = authType;
        authenticationPayloadV1.scopes = scopes;
        authenticationPayloadV1.creds = "";
        authenticationPayloadV1.publicKey = publicKey;
        authenticationPayloadV1.session = sessionId;
        authenticationPayloadV1.api = origin.url;
        authenticationPayloadV1.tag = origin.tag;
        authenticationPayloadV1.community = origin.communityName;
        authenticationPayloadV1.authPage = origin.authPage;
        authenticationPayloadV1.sessionURL = sessionUrl;
        return authenticationPayloadV1;
    }

    public static class Origin {
        public String tag;
        public String url;
        public String communityName;
        public String communityId;
        public String authPage;
    }
}