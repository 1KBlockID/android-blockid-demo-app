package com.onekosmos.blockidsample.ui.fido2

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.fido.Fido
import com.google.android.gms.fido.fido2.api.common.AuthenticatorErrorResponse
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import com.google.android.material.switchmaterial.SwitchMaterial
import com.onekosmos.blockid.sdk.BIDAPIs.APIManager.ErrorManager.ErrorResponse
import com.onekosmos.blockid.sdk.BIDAPIs.sessionapi.SessionApi.GenerateNewSessionResponsePayload
import com.onekosmos.blockid.sdk.BlockIDSDK
import com.onekosmos.blockid.sdk.datamodel.BIDOrigin
import com.onekosmos.blockidsample.R
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class FIDO2Activity : AppCompatActivity() {
    private val viewModel: FidoViewModel by viewModels()
    private val createCredentialIntentLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
        ::handleCreateCredentialResult
    )
    private val signIntentLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
        ::handleSignResult
    )
    private var register: Button? = null
    private var login: Button? = null
    private var logout: Button? = null
    private var editUserName: EditText? = null
    private var editDisplayName: EditText? = null
    private var userInfo:TextView? = null
    private lateinit var authenticator: SwitchMaterial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_native_fido)
        initialize()
        editUserName = findViewById(R.id.username)
        editDisplayName = findViewById(R.id.displayname)
        userInfo = findViewById(R.id.signedinuser)
        register = findViewById(R.id.register)
        authenticator = findViewById(R.id.authenticator)
        register?.setOnClickListener {
            val userName = editUserName?.text.toString()
            val displayName = editDisplayName?.text.toString()
            if (userName.isBlank() || displayName.isBlank()) {
                return@setOnClickListener
            }
            viewModel.setUserDetail(userName, displayName, if (authenticator.isChecked) {authenticator.textOn.toString()} else {authenticator.textOff.toString()})
            lifecycleScope.launch {
                val intent = viewModel.registerRequest()
                if (intent != null) {
                    createCredentialIntentLauncher.launch(
                        IntentSenderRequest.Builder(intent).build()
                    )
                }
            }
        }
        login = findViewById(R.id.login)
        login?.setOnClickListener {
            lifecycleScope.launch {
                val intent = viewModel.signinRequest()
                if (intent != null) {
                    signIntentLauncher.launch(
                        IntentSenderRequest.Builder(intent).build()
                    )
                }
            }
        }
        logout = findViewById(R.id.logout)
        logout?.setOnClickListener {
            lifecycleScope.launch {
                viewModel.signoutUser()
                updateSession()
            }
        }
    }

    private fun updateSession() {
        val origin = BIDOrigin()
        origin.tag = "5f3d8d0cd866fa61019cf968"
        origin.url = "5f3d8d0cd866fa61019cf968"
        origin.communityName = "5f3d8d0cd866fa61019cf969"
        origin.communityId = "5f3d8d0cd866fa61019cf969"
        BlockIDSDK.getInstance().generateNewSession(origin, "none", "")
        { status: Boolean, session: String?, generateNewSessionResponsePayload:
        GenerateNewSessionResponsePayload?, errorResponse: ErrorResponse? ->
            if (status) {
                Log.e("Session URL", session!!)
                Log.e("Session ID", generateNewSessionResponsePayload!!.sessionId)
                viewModel.setSessionID(generateNewSessionResponsePayload.sessionId)
            } else {
                Log.e("Session Error", "" + errorResponse?.code + " " + errorResponse?.message)
            }
        }
    }

    private fun initialize() {
        lifecycleScope.launchWhenStarted {
            viewModel.signInState.collect { state ->
                when (state) {
                    is SignInState.SignedIn -> {
                        register?.visibility = View.GONE
                        authenticator.visibility = View.GONE
                        login?.visibility = View.VISIBLE
                        logout?.visibility = View.VISIBLE
                        editDisplayName?.visibility = View.GONE
                        editUserName?.visibility = View.GONE
                        userInfo?.text = "Signed in as ${state.username}"
                    }
                    is SignInState.AuthError -> {
                        Toast.makeText(this@FIDO2Activity, state.error, Toast.LENGTH_LONG).show()
                    }
                    is SignInState.SignInError -> {
                        Toast.makeText(this@FIDO2Activity, state.error, Toast.LENGTH_LONG).show()
                    }
                    is SignInState.Authenticated -> {
                        Toast.makeText(this@FIDO2Activity, "Authenticated successfully", Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        register?.visibility = View.VISIBLE
                        authenticator.visibility = View.VISIBLE
                        login?.visibility = View.GONE
                        logout?.visibility = View.GONE
                        editDisplayName?.visibility = View.VISIBLE
                        editUserName?.visibility = View.VISIBLE
                        userInfo?.text = ""
                    }
                }
            }
        }
        viewModel.setHTTPClient(provideOkHttpClient())
        updateSession()
    }

    private fun provideOkHttpClient() : OkHttpClient {
        val logging = HttpLoggingInterceptor()
        logging.level = (HttpLoggingInterceptor.Level.BODY)

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(40, TimeUnit.SECONDS)
            .connectTimeout(40, TimeUnit.SECONDS)
            .build()
    }

    override fun onResume() {
        super.onResume()
        viewModel.setFido2ApiClient(Fido.getFido2ApiClient(this))
    }

    override fun onPause() {
        super.onPause()
        viewModel.setFido2ApiClient(null)
    }

    private fun handleCreateCredentialResult(activityResult: ActivityResult) {
        val bytes = activityResult.data?.getByteArrayExtra(Fido.FIDO2_KEY_CREDENTIAL_EXTRA)
        when {
            activityResult.resultCode != Activity.RESULT_OK ->
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
            bytes == null ->
                Toast.makeText(this, "Error creating credential", Toast.LENGTH_LONG)
                    .show()
            else -> {
                val credential = PublicKeyCredential.deserializeFromBytes(bytes)
                val response = credential.response
                if (response is AuthenticatorErrorResponse) {
                    val errorMessage = "${response.errorCodeAsInt} : ${response.errorMessage}"
                    Log.e("FIDOActivity", response.errorMessage)
                    if (response.errorCodeAsInt == 11) { // user already registered
                        viewModel.saveUser()
                        Toast.makeText(this, "User already registered", Toast.LENGTH_LONG)
                            .show()
                        return
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG)
                        .show()
                } else {
                    viewModel.registerResponse(credential)
                    Toast.makeText(this, "Register option successful", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
    }

    private fun handleSignResult(activityResult: ActivityResult) {
        val bytes = activityResult.data?.getByteArrayExtra(Fido.FIDO2_KEY_CREDENTIAL_EXTRA)
        when {
            activityResult.resultCode != Activity.RESULT_OK ->
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
            bytes == null ->
                Toast.makeText(this, "Error using credential", Toast.LENGTH_LONG).show()
            else -> {
                val credential = PublicKeyCredential.deserializeFromBytes(bytes)
                val response = credential.response
                if (response is AuthenticatorErrorResponse) {
                    val errorMessage = "${response.errorCodeAsInt} : ${response.errorMessage}"
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG)
                        .show()
                } else {
                    viewModel.signinResponse(credential)
                }
            }
        }
    }
}
