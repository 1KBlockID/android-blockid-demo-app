package com.onekosmos.blockidsample.ui.fido2

sealed class ApiResult<out R> {

    /**
     * API returned successfully with data.
     */
    class Success<T>(
        /**
         * The result data.
         */
        val data: T
    ) : ApiResult<T>()

    /**
     * API returned unsuccessfully with code 401, and the user should be signed out.
     */
    object SignedOutFromServer : ApiResult<Nothing>()
}
