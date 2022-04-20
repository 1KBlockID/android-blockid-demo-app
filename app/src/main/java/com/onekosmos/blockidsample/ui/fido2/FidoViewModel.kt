package com.onekosmos.blockidsample.ui.fido2

import android.app.PendingIntent
import android.util.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.fido.fido2.Fido2ApiClient
import com.google.android.gms.fido.fido2.api.common.*
import com.onekosmos.blockid.sdk.BlockIDSDK
import com.onekosmos.blockidsample.util.SharedPreferenceUtil
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import ru.gildor.coroutines.okhttp.await
import java.io.StringReader
import java.io.StringWriter

class FidoViewModel : ViewModel() {
    private var fido2ApiClient: Fido2ApiClient? = null
    private val _processing = MutableStateFlow(false)
    private val signInStateMutable = MutableSharedFlow<SignInState>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val signInState = signInStateMutable.asSharedFlow()
    private var sessionID:String? = null
    private var client: OkHttpClient? = null
    private var userName: String? = null
    private var displayName: String? = null

    init {
        viewModelScope.launch {
            val userName = SharedPreferenceUtil.getInstance().getString(SharedPreferenceUtil.K_PREF_FIDO2_USERNAME)
            val displayName = SharedPreferenceUtil.getInstance().getString(SharedPreferenceUtil.K_PREF_FIDO2_DISPLAYNAME)
            val initialState = when {
                userName.isNullOrBlank() -> SignInState.SignedOut
                displayName.isNullOrBlank() -> SignInState.SignedOut
                else -> {
                    setUserDetail(userName, displayName)
                    SignInState.SignedIn(displayName)
                }
            }
            signInStateMutable.emit(initialState)
        }
    }
    fun setUserDetail(name: String, display: String) {
       userName = name
       displayName = display
    }
    fun setHTTPClient(okhttp: OkHttpClient) {
       client = okhttp
    }

    fun setSessionID (session: String) {
        sessionID = session
    }

    fun setFido2ApiClient(client: Fido2ApiClient?) {
        fido2ApiClient = client
    }

    suspend fun registerRequest(): PendingIntent? {
        _processing.value = true
        try {
            fido2ApiClient?.let { client ->
                when (val apiResult = register()) {
                    ApiResult.SignedOutFromServer -> signoutUser()
                    is ApiResult.Success -> {
                        val task = client.getRegisterPendingIntent(apiResult.data)
                        return task.await()
                    }
                    else -> return null
                }
            }
        } finally {
            _processing.value = false
        }

        return null
    }

    private suspend fun register(): ApiResult<PublicKeyCredentialCreationOptions>? {
        val call = client?.newCall(
            Request.Builder()
                .url("https://1k-dev.1kosmos.net/webauthn/u1/attestation/options")
                .addHeader("licensekey", LICENSE_KEY)
                .addHeader("requestid", BlockIDSDK.getInstance().requestID)
                .method("POST", jsonRequestBody {
                    name("dns").value("1k-dev.1kosmos.net")
//                    name("username").value("rishabh")
//                    name("displayName").value("Rishabh Srivastava")
                    name("username").value(userName)
                    name("displayName").value(displayName)
                    name("communityId").value("5f3d8d0cd866fa61019cf969")
                    name("tenantId").value("5f3d8d0cd866fa61019cf968")
                    name("attestation").value("direct")
                    name("authenticatorSelection").objectValue {
                        name("authenticatorAttachment").value("platform")
                        name("userVerification").value("required")
                    }
                })
                .build()
        )
        val response = call?.await()
        return response?.result("Error calling u1/attestation/options") {
            parsePublicKeyCredentialCreationOptions(
                body ?: throw ApiException("Empty response from u1/attestation/options")
            )
        }
    }

   suspend fun registerResponse(
        sessionId: String,
        credential: PublicKeyCredential
    ): ApiResult<Credential>? {
        val rawId = credential.rawId.toBase64()
        val response = credential.response as AuthenticatorAttestationResponse
        val call = client?.newCall(
            Request.Builder()
                .url("https://1k-dev.1kosmos.net/webauthn/u1/attestation/result")
                .addHeader("sessionInfo", sessionId)
                .addHeader("licensekey", LICENSE_KEY)
                .addHeader("requestid", BlockIDSDK.getInstance().requestID)
                .method("POST", jsonRequestBody {
                    name("dns").value("1k-dev.1kosmos.net")
                    name("communityId").value("5f3d8d0cd866fa61019cf969")
                    name("tenantId").value("5f3d8d0cd866fa61019cf968")
                    name("id").value(rawId)
                    name("type").value(PublicKeyCredentialType.PUBLIC_KEY.toString())
                    name("rawId").value(rawId)
                    name("authenticatorAttachment").value("platform")
                    name("getClientExtensionResults").objectValue {
                    }
                    name("response").objectValue {
                        name("clientDataJSON").value(
                            response.clientDataJSON.toBase64()
                        )
                        name("attestationObject").value(
                            response.attestationObject.toBase64()
                        )
                        name("getAuthenticatorData").objectValue {
                        }
                        name("getPublicKey").objectValue {
                        }
                        name("getPublicKeyAlgorithm").objectValue {
                        }
                        name("getTransports").objectValue {
                        }
                    }
                })
                .build()
        )
        val apiResponse = call?.await()
        return apiResponse?.result("Error calling /registerResponse") {
            parseUserCredentials(
                body ?: throw ApiException("Empty response from /registerResponse")
            )
        }
    }

    suspend fun signinRequest(): PendingIntent? {
        _processing.value = true
        try {
            fido2ApiClient?.let { client ->
                when (val apiResult = signin()) {
                    ApiResult.SignedOutFromServer -> signoutUser()
                    is ApiResult.Success -> {
                        val task = client.getSignPendingIntent(apiResult.data)
                        return task.await()
                    }
                    else -> return null
                }
            }
        } catch (e: Exception) {
            signInStateMutable.emit(SignInState.AuthError("Connection Error! Please try again later."))
        } finally {
            _processing.value = false
        }

        return null
    }

    suspend fun signin(): ApiResult<PublicKeyCredentialRequestOptions>? {
        val call = client?.newCall(
            Request.Builder()
                .url("https://1k-dev.1kosmos.net/webauthn/u1/assertion/options")
                .addHeader("licensekey", LICENSE_KEY)
                .addHeader("requestid", BlockIDSDK.getInstance().requestID)
                .method("POST", jsonRequestBody {
                    name("dns").value("1k-dev.1kosmos.net")
                    name("username").value(userName)
                    name("displayName").value(displayName)
                    name("communityId").value("5f3d8d0cd866fa61019cf969")
                    name("tenantId").value("5f3d8d0cd866fa61019cf968")
                })
                .build()
        )
        val response = call?.await()
        return response?.result("Error calling /signinRequest") {
            parsePublicKeyCredentialRequestOptions(
                body ?: throw ApiException("Empty response from /signinRequest")
            )
        }
    }

    fun signinResponse(credential: PublicKeyCredential) {
        viewModelScope.launch {
            _processing.value = true
            try {
                when (val result = signinResult(credential)) {
                    ApiResult.SignedOutFromServer -> signoutUser()
                    is ApiResult.Success -> {
                        Log.e("FidoViewModel", "Signin success ${result.data.sub}")
                        if (result.data.sub.isBlank()) {
                            signInStateMutable.emit(SignInState.AuthError(result.data.error ?: "Error Authenticating User"))
                            return@launch
                        }
                        signInStateMutable.emit(SignInState.Authenticated)
                    }
                }
            } catch (e: ApiException) {
                Log.e("FidoViewModel", "Cannot call signinResponse", e)
            } finally {
                _processing.value = false
            }
        }
    }

    private suspend fun signinResult(
        credential: PublicKeyCredential
    ): ApiResult<Credential>? {
        val rawId = credential.rawId.toBase64()
        val response = credential.response as AuthenticatorAssertionResponse

        val call = client?.newCall(
            Request.Builder()
                .url("https://1k-dev.1kosmos.net/webauthn/u1/assertion/result")
                .addHeader("sessionInfo", sessionID!!)
                .addHeader("licensekey", LICENSE_KEY)
                .addHeader("requestid", BlockIDSDK.getInstance().requestID)
                .method("POST", jsonRequestBody {
                    name("id").value(rawId)
                    name("type").value(PublicKeyCredentialType.PUBLIC_KEY.toString())
                    name("rawId").value(rawId)
                    name("dns").value("1k-dev.1kosmos.net")
                    name("communityId").value("5f3d8d0cd866fa61019cf969")
                    name("tenantId").value("5f3d8d0cd866fa61019cf968")
                    name("getClientExtensionResults").objectValue {
                    }
                    name("response").objectValue {
                        name("clientDataJSON").value(
                            response.clientDataJSON.toBase64()
                        )
                        name("authenticatorData").value(
                            response.authenticatorData.toBase64()
                        )
                        name("signature").value(
                            response.signature.toBase64()
                        )
                        name("userHandle").value(
                            response.userHandle?.toBase64() ?: credential.id
                        )
                    }
                })
                .build()
        )
        val apiResponse = call?.await()
        return apiResponse?.result("Error calling /signingResponse") {
            parseUserCredentials(body ?: throw ApiException("Empty response from /signinResponse"))
        }
    }

    private fun parsePublicKeyCredentialRequestOptions(
        body: ResponseBody
    ): PublicKeyCredentialRequestOptions {
        val builder = PublicKeyCredentialRequestOptions.Builder()
        JsonReader(body.byteStream().bufferedReader()).use { reader ->
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "challenge" -> builder.setChallenge(reader.nextString().decodeBase64())
                    "userVerification" -> reader.skipValue()
                    "allowCredentials" -> builder.setAllowList(parseCredentialDescriptors(reader))
                    "rpId" -> builder.setRpId(reader.nextString())
                    "timeout" -> builder.setTimeoutSeconds(reader.nextDouble())
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
        }
        return builder.build()
    }

    private fun parsePublicKeyCredentialCreationOptions(
        body: ResponseBody
    ): PublicKeyCredentialCreationOptions {
        val builder = PublicKeyCredentialCreationOptions.Builder()
        JsonReader(body.byteStream().bufferedReader()).use { reader ->
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "user" -> builder.setUser(parseUser(reader))
                    "challenge" -> builder.setChallenge(reader.nextString().decodeBase64())
                    "pubKeyCredParams" -> builder.setParameters(parseParameters(reader))
                    "timeout" -> builder.setTimeoutSeconds(reader.nextDouble())
                    "attestation" -> reader.skipValue() // Unused
                    "excludeCredentials" -> builder.setExcludeList(
                        parseCredentialDescriptors(reader)
                    )
                    "authenticatorSelection" -> builder.setAuthenticatorSelection(
                        parseSelection(reader)
                    )
                    "rp" -> builder.setRp(parseRp(reader))
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
        }
        return builder.build()
    }

    private fun parseRp(reader: JsonReader): PublicKeyCredentialRpEntity {
        var id: String? = null
        var name: String? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> id = reader.nextString()
                "name" -> name = reader.nextString()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return PublicKeyCredentialRpEntity(id!!, name!!, /* icon */ null)
    }

    private fun parseSelection(reader: JsonReader): AuthenticatorSelectionCriteria {
        val builder = AuthenticatorSelectionCriteria.Builder()
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "authenticatorAttachment" -> builder.setAttachment(
                    Attachment.fromString(reader.nextString())
                )
                "userVerification" -> reader.skipValue()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return builder.build()
    }

    private fun parseCredentialDescriptors(
        reader: JsonReader
    ): List<PublicKeyCredentialDescriptor> {
        val list = mutableListOf<PublicKeyCredentialDescriptor>()
        reader.beginArray()
        while (reader.hasNext()) {
            var id: String? = null
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "id" -> id = reader.nextString()
                    "type" -> reader.skipValue()
                    "transports" -> reader.skipValue()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            list.add(
                PublicKeyCredentialDescriptor(
                    PublicKeyCredentialType.PUBLIC_KEY.toString(),
                    id!!.decodeBase64(),
                    /* transports */ null
                )
            )
        }
        reader.endArray()
        return list
    }

    private fun String.decodeBase64(): ByteArray {
        return Base64.decode(this, BASE64_FLAG)
    }

    private fun ByteArray.toBase64(): String {
        return Base64.encodeToString(this, BASE64_FLAG)
    }

    private fun parseUser(reader: JsonReader): PublicKeyCredentialUserEntity {
        reader.beginObject()
        var id: String? = null
        var name: String? = null
        var displayName = ""
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> id = reader.nextString()
                "name" -> name = reader.nextString()
                "displayName" -> displayName = reader.nextString()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return PublicKeyCredentialUserEntity(
            id!!.decodeBase64(),
            name!!,
            null, // icon
            displayName
        )
    }

    private fun parseParameters(reader: JsonReader): List<PublicKeyCredentialParameters> {
        val parameters = mutableListOf<PublicKeyCredentialParameters>()
        reader.beginArray()
        while (reader.hasNext()) {
            reader.beginObject()
            var type: String? = null
            var alg = 0
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "type" -> type = reader.nextString()
                    "alg" -> alg = reader.nextInt()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            parameters.add(PublicKeyCredentialParameters(type!!, alg))
        }
        reader.endArray()
        return parameters
    }

    private fun JsonWriter.objectValue(body: JsonWriter.() -> Unit) {
        beginObject()
        body()
        endObject()
    }

    private val JSON = "application/json".toMediaTypeOrNull()
    private fun jsonRequestBody(body: JsonWriter.() -> Unit): RequestBody {
        val output = StringWriter()
        JsonWriter(output).use { writer ->
            writer.beginObject()
            writer.body()
            writer.endObject()
        }
        return output.toString().toRequestBody(JSON)
    }

    private fun <T> Response.result(errorMessage: String, data: Response.() -> T): ApiResult<T> {
        if (!isSuccessful) {
            if (code == 401) { // Unauthorized
                return ApiResult.SignedOutFromServer
            }
            // All other errors throw an exception.
            throwResponseError(this, errorMessage)
        }
        return ApiResult.Success(data())
    }

    private fun throwResponseError(response: Response, message: String): Nothing {
        val b = response.body
        if (b != null) {
            throw ApiException("$message; ${parseError(b)}")
        } else {
            throw ApiException(message)
        }
    }

    private fun parseError(body: ResponseBody): String {
        val errorString = body.string()
        try {
            JsonReader(StringReader(errorString)).use { reader ->
                reader.beginObject()
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    if (name == "error") {
                        val token = reader.peek()
                        if (token == JsonToken.STRING) {
                            return reader.nextString()
                        }
                        return "Unknown"
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
            }
        } catch (e: Exception) {
            Log.e("FidoViewModel", "Cannot parse the error: $errorString", e)
            // Don't throw; this method is called during throwing.
        }
        return ""
    }

    suspend fun signoutUser() {
        SharedPreferenceUtil.getInstance().remove(SharedPreferenceUtil.K_PREF_FIDO2_USERNAME,
            SharedPreferenceUtil.K_PREF_FIDO2_DISPLAYNAME)
        signInStateMutable.emit(SignInState.SignedOut)
    }

    fun saveUser() {
        viewModelScope.launch {
            SharedPreferenceUtil.getInstance().setString(
                SharedPreferenceUtil.K_PREF_FIDO2_USERNAME, userName
            )
            SharedPreferenceUtil.getInstance().setString(
                SharedPreferenceUtil.K_PREF_FIDO2_DISPLAYNAME, displayName
            )
            signInStateMutable.emit(SignInState.SignedIn(displayName!!))
        }
    }

    fun registerResponse(credential: PublicKeyCredential) {
        viewModelScope.launch {
            _processing.value = true
            try {
                when (val result = registerResponse(sessionID!!, credential)) {
                    ApiResult.SignedOutFromServer -> signoutUser()
                    is ApiResult.Success -> {
                        Log.d("FIDOViewModel", "Success ${result.data.sub}")
                        if (result.data.sub.isBlank()) {
                            signInStateMutable.emit(SignInState.SignInError(result.data.error ?: "Error registering user"))
                            return@launch
                        }
                        userName = result.data.sub
                        saveUser()
                    }
                }
            } catch (e: ApiException) {
                Log.e("FidoViewModel", "Cannot call registerResponse", e)
            } finally {
                _processing.value = false
            }
        }
    }

    private fun parseUserCredentials(body: ResponseBody): Credential {
        var credential = Credential("", "", "Unknown error! Please try again later")
        JsonReader(body.byteStream().bufferedReader()).use { reader ->
            reader.beginObject()
            var sub: String? = null
            var error: String? = null
            var status: String? = null
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "sub" -> sub = reader.nextString()
                    "errorMessage" -> error = reader.nextString()
                    "status" -> status = reader.nextString()
                }
            }
            reader.endObject()
            if (status != null) {
                credential = Credential(sub ?: "", status, error)
            }
        }

        return credential
    }

    companion object {
        private const val LICENSE_KEY = "3f2282e9-3d46-4961-b103-a9319ad4560c"
        private const val BASE64_FLAG = Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE
    }
}