package com.example.biometricauthdemo;

import com.onekosmos.blockid.sdk.datamodel.BIDTenant;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class AppConstant {
    public static final String licenseKey = "d590166b-972d-46b2-9557-fdaa1cd6600e";
    public static final String dvcId = "default_config";
    public static final BIDTenant defaultTenant = new BIDTenant("staging",
            "chinabank",
            "https://staging-in.1kosmos.in");
}