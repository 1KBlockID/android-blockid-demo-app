package com.onekosmos.blockidsample;

import com.onekosmos.blockid.sdk.datamodel.BIDTenant;

/**
 * Created by 1Kosmos Engineering
 * Copyright © 2021 1Kosmos. All rights reserved.
 */
public class AppConstant {
    public static final String licenseKey = "5809b7b7-886f-4c88-9061-59a2baf485be";
    public static final String dvcId = "default_config";
    public static final BIDTenant defaultTenant = new BIDTenant("1kosmos", "default",
            "https://1k-qa.1kosmos.net");
    public static final BIDTenant clientTenant = new BIDTenant("acme", "default",
            "https://blockid-qa.1kosmos.net");

//    WHERE IS MY ROOT
//    open my SD (always at {dns}/caas/sd)
//    open global_caas SD › {my SD}.global_caas + “/sd”
//    Root = “{my SD}.global_caas” + “/sd” . adminconsole
}