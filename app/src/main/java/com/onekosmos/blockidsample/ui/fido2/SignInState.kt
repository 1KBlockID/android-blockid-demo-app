package com.onekosmos.blockidsample.ui.fido2

sealed class SignInState {

    /**
     * The user is signed out.
     */
    object SignedOut : SignInState()

    /**
     * The user is signing in. The user has entered the username and is ready to sign in with
     * password or FIDO2.
     */
    data class SigningIn(
        val username: String
    ) : SignInState()

    /**
     * The user sign-in failed.
     */
    data class SignInError(
        val error: String
    ) : SignInState()

    /**
     * The user is signed in.
     */
    data class SignedIn(
        val username: String
    ) : SignInState()

    data class AuthError(
        val error: String
    ) : SignInState()

    object Authenticated : SignInState()
}
