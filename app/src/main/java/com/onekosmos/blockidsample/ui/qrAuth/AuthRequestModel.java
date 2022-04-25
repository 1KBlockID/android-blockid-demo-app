package com.onekosmos.blockidsample.ui.qrAuth;

import androidx.annotation.Keep;

import com.onekosmos.blockid.sdk.datamodel.AccountAuthConstants;
import com.onekosmos.blockid.sdk.datamodel.BIDOrigin;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
@Keep
public class AuthRequestModel {
    public String authtype;
    public String scopes;
    public String creds;
    public String publicKey;
    public String session;
    public String api;
    public String tag;
    public String community;
    public String authPage;
    public String name;
    public String sessionURL;

    public BIDOrigin getOrigin() {
        BIDOrigin bidOrigin = new BIDOrigin();
        bidOrigin.api = api;
        bidOrigin.authPage = authPage;
        bidOrigin.community = community;
        bidOrigin.name = name;
        bidOrigin.publicKey = publicKey;
        bidOrigin.session = session;
        bidOrigin.tag = tag;

        // default to native auth without a specific method.
        if (bidOrigin.authPage == null) {
            bidOrigin.authPage = AccountAuthConstants.K_NATIVE_AUTH_SCHEMA;
        }

        return bidOrigin;
    }
}