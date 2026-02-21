package com.onekosmos.blockidsample.util;

/**
 * @author Gaurav Rane. Created on 18/02/2026.
 * Copyright © 2026 1Kosmos. All rights reserved.
 * Enum for IDP (Identity Proofing) error codes and their corresponding user-friendly messages
 * Used in Document Scanner Activity to handle various document verification errors
 */
public enum IDPErrorCode {
    IDV0001("IDV0001", "Image quality check failed",
            "Scan Failed. Please check the surrounding and try scanning again."),

    IDV0002("IDV0002", "Verification partially completed",
            "We couldn't complete the verification of the document. Please try again."),

    IDV0003("IDV0003", "Verification was completed only on front",
            "We couldn't complete the verification of the document. Please try again."),

    IDV0004("IDV0004", "Extraction of document failed",
            "We couldn't complete the verification of the document. Please try again."),

    IDV0005("IDV0005", "Incorrect document type presented",
            "Scan Failed. Please scan a valid document."),

    IDV0006("IDV0006", "Unsupported document was presented",
            "Unsupported Document. Please scan a valid document."),

    IDV0007("IDV0007", "Data check failed",
            "We couldn't complete the verification. Please try again."),

    IDV0008("IDV0008", "Document liveness check failed",
            "We couldn't complete the verification of the document. Please try again."),

    IDV0009("IDV0009", "Document validity check failed",
            "We couldn't complete the verification of the document. Please try again."),

    IDV0010("IDV0010", "Module failed",
            "We couldn't complete the verification. Please try again."),

    IDV0011("IDV0011", "Document expired",
            "The document which you are trying to enroll is already expired."),

    IDV0012("IDV0012", "One or more fraud check failed",
            "We couldn't complete the verification of the document. Please try again."),

    IDV0014("IDV0014", "All retry attempts exhausted",
            "You have exhausted all retry attempts. Please try again."),

    IDV0015("IDV0015", "Document is not allowed",
            "Unsupported Document. Please scan a valid document."),

    UNKNOWN("UNKNOWN", "Unknown error",
            "We couldn't complete the verification. Please try again.");

    private final String code;
    private final String technicalMessage;
    private final String userMessage;

    IDPErrorCode(String code, String technicalMessage, String userMessage) {
        this.code = code;
        this.technicalMessage = technicalMessage;
        this.userMessage = userMessage;
    }

    public String getCode() {
        return code;
    }

    /**
     * @noinspection unused
     */
    public String getTechnicalMessage() {
        return technicalMessage;
    }

    public String getUserMessage() {
        return userMessage;
    }

    /**
     * Get IDPErrorCode from error code string
     *
     * @param code Error code string (e.g., "IDV0001")
     * @return Corresponding IDPErrorCode enum, or UNKNOWN if not found
     */
    public static IDPErrorCode fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return UNKNOWN;
        }

        for (IDPErrorCode errorCode : values()) {
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
        return fromCode(code).getUserMessage();
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

    /**
     * Check if error code indicates wrong document type
     *
     * @param code Error code string
     * @return true if error is related to wrong document type
     */
    public static boolean isWrongDocumentType(String code) {
        IDPErrorCode errorCode = fromCode(code);
        return errorCode == IDV0005 || errorCode == IDV0006 || errorCode == IDV0015;
    }
}
