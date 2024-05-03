package com.onekosmos.blockidsample.util;

/**
 * @author Vaishali Sharma. Created on 19/04/2024.
 * Copyright © 2024 1Kosmos. All rights reserved.
 */
public enum ResetSDKMessages {
    RESET_APP_OPTION_CLICK("Reset App option click from Home Screen"),
    TENANT_REGISTRATION_FAILED_DURING_RESTORATION("Tenant Registration failed during Account Restoration"),
    FETCH_WALLET_FAILED_DURING_RESTORATION("Fetch wallet failed during Account Restoration"),
    ACCOUNT_RESTORATION_FAILED("Account Restoration failed"); // when device offline, publickeys api fails but resetsdk happen
    private final String message;

    ResetSDKMessages(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String getMessage(int code, String errorMsg) {
        return message + " ((" + code + ") " + errorMsg + ")";
    }
}
