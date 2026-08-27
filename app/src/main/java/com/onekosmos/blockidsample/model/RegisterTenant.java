package com.onekosmos.blockidsample.model;

import androidx.annotation.Keep;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
@Keep
public class RegisterTenant {

    public String version;
    public String api;
    public String tag;
    public String community;

    public String getVersion() {
        return version;
    }

    public String getApi() {
        return api;
    }

    public String getTag() {
        return tag;
    }

    public String getCommunity() {
        return community;
    }
}
