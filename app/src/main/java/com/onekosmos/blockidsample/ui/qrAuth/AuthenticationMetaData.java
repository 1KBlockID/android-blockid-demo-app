package com.onekosmos.blockidsample.ui.qrAuth;

import androidx.annotation.Keep;

import java.util.ArrayList;

@Keep
@SuppressWarnings("ALL")
public class AuthenticationMetaData {
   public WebauthnChallenge webauthn_challenge;

    @Keep
    public class WebauthnChallenge {
        String challenge;
        String rpId;
        long timeout;
        String userVerification;
        ArrayList<AllowCredential> allowCredentials;
        String status;
        String errorMessage;
    }

    @Keep
    private static class AllowCredential {
        String type;
        String id;
    }
}
