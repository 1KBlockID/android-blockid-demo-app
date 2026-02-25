package com.onekosmos.blockidsample.util;

import androidx.annotation.Keep;

/**
 * @author Gaurav Rane. Created on 18/02/2026.
 * Copyright © 2026 1Kosmos. All rights reserved.
 * Enum for IDV (Identity Verification) error codes and their corresponding user-friendly messages
 * Used in Document Scanner Activity to handle various document verification errors
 */
@Keep
public enum IDVErrorCode {
    IDV0001("IDV0001", "Scan Failed. Please check the surroundings and try scanning again."),

    IDV0002("IDV0002", "We couldn't complete the verification of the document. Please try again."),

    IDV0003("IDV0003", "We couldn't complete the verification of the document. Please try again."),

    IDV0004("IDV0004", "We couldn't complete the verification of the document. Please try again."),

    IDV0005("IDV0005", "Scan Failed. Please scan a valid document."),

    IDV0006("IDV0006", "Unsupported Document. Please scan a valid document."),

    IDV0007("IDV0007", "We couldn't complete the verification. Please try again."),

    IDV0008("IDV0008", "We couldn't complete the verification of the document. Please try again."),

    IDV0009("IDV0009", "We couldn't complete the verification of the document. Please try again."),

    IDV0010("IDV0010", "We couldn't complete the verification. Please try again."),

    IDV0011("IDV0011", "The document which you are trying to enroll is already expired."),

    IDV0012("IDV0012", "We couldn't complete the verification of the document. Please try again."),

    IDV0014("IDV0014", "You have exhausted all retry attempts. Please try again."),

    IDV0015("IDV0015", "Unsupported Document. Please scan a valid document."),

    UNKNOWN("UNKNOWN", "We couldn't complete the verification. Please try again.");

    private final String code;
    private final String userMessage;

    IDVErrorCode(String code, String userMessage) {
        this.code = code;
        this.userMessage = userMessage;
    }

    public String getCode() {
        return code;
    }

    public String getUserMessage() {
        return userMessage;
    }

    /**
     * Get IDVErrorCode from error code string
     *
     * @param code Error code string (e.g., "IDV0001")
     * @return Corresponding IDVErrorCode enum, or UNKNOWN if not found
     */
    public static IDVErrorCode fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return UNKNOWN;
        }

        for (IDVErrorCode errorCode : values()) {
            if (errorCode.code.equalsIgnoreCase(code)) {
                return errorCode;
            }
        }
        return UNKNOWN;
    }

    /**
     * Get user-friendly message from error code string
     *
     * @param code Error code string (e.g., "IDV0001")
     * @return User-friendly message
     */
    public static String getUserMessageFromCode(String code) {
        return fromCode(code).getUserMessage() + "[" + code + "]";
    }

    /**
     * Check if the error code exists in the enum
     *
     * @param code Error code string
     * @return true if the code exists, false otherwise
     */
    public static boolean isValidCode(String code) {
        return fromCode(code) != UNKNOWN;
    }
}
