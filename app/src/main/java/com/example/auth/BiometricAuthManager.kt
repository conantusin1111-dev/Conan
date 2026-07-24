package com.example.auth

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.fragment.app.FragmentActivity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executor

enum class BiometricStatus {
    AVAILABLE,
    NO_HARDWARE,
    HARDWARE_UNAVAILABLE,
    NOT_ENROLLED,
    UNSUPPORTED
}

data class AuthUiState(
    val isAuthenticated: Boolean = false,
    val isBiometricEnabled: Boolean = true,
    val currentUser: FirebaseUser? = null,
    val authError: String? = null,
    val isAuthenticating: Boolean = false
)

class BiometricAuthManager(private val context: Context) {

    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            var app = if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseApp.getInstance()
            } else {
                FirebaseApp.initializeApp(context)
            }
            if (app == null) {
                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setApiKey("AIzaSyMockKeyForPayPulseBiometrics")
                    .setApplicationId("1:1234567890:android:mockappid")
                    .setProjectId("paypulse-upi")
                    .build()
                app = FirebaseApp.initializeApp(context, options)
            }
            if (app != null) FirebaseAuth.getInstance(app) else null
        } catch (e: Exception) {
            Log.e("BiometricAuthManager", "Firebase Auth initialization error", e)
            null
        }
    }
    private val credentialManager: CredentialManager by lazy { CredentialManager.create(context) }

    private val _uiState = MutableStateFlow(
        AuthUiState(
            currentUser = try { firebaseAuth?.currentUser } catch (e: Exception) { null },
            isAuthenticated = false // Require auth check on app startup if enabled
        )
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Ensure anonymous or active user session in Firebase Auth
        try {
            val auth = firebaseAuth
            if (auth != null && auth.currentUser == null) {
                auth.signInAnonymously()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            _uiState.value = _uiState.value.copy(currentUser = auth.currentUser)
                        }
                    }
            }
        } catch (e: Exception) {
            Log.e("BiometricAuthManager", "Firebase Auth init failed", e)
        }
    }

    fun checkBiometricStatus(): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            else -> BiometricStatus.UNSUPPORTED
        }
    }

    fun authenticateWithBiometrics(
        activity: FragmentActivity,
        title: String = "Unlock PayPulse",
        subtitle: String = "Verify fingerprint or face to proceed securely",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor: Executor = ContextCompat.getMainExecutor(context)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                _uiState.value = _uiState.value.copy(
                    isAuthenticated = true,
                    authError = null,
                    isAuthenticating = false
                )
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                val errorMessage = errString.toString()
                _uiState.value = _uiState.value.copy(
                    authError = errorMessage,
                    isAuthenticating = false
                )
                onError(errorMessage)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                _uiState.value = _uiState.value.copy(
                    authError = "Fingerprint not recognized. Please try again.",
                    isAuthenticating = false
                )
            }
        })

        _uiState.value = _uiState.value.copy(isAuthenticating = true, authError = null)
        biometricPrompt.authenticate(promptInfo)
    }

    suspend fun authenticateWithCredentials(
        activity: FragmentActivity,
        webClientId: String? = null,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            _uiState.value = _uiState.value.copy(isAuthenticating = true)
            
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId ?: "mock_client_id.apps.googleusercontent.com")
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result: GetCredentialResponse = credentialManager.getCredential(
                context = activity,
                request = request
            )

            _uiState.value = _uiState.value.copy(
                isAuthenticated = true,
                isAuthenticating = false,
                authError = null
            )
            onSuccess("Authenticated securely via Credential Manager")
        } catch (e: GetCredentialException) {
            val err = e.localizedMessage ?: "Credential authentication cancelled or failed"
            _uiState.value = _uiState.value.copy(
                isAuthenticating = false,
                authError = err
            )
            onError(err)
        } catch (e: Exception) {
            val err = e.localizedMessage ?: "Credential auth error"
            _uiState.value = _uiState.value.copy(
                isAuthenticating = false,
                authError = err
            )
            onError(err)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isBiometricEnabled = enabled)
    }

    fun lockApp() {
        _uiState.value = _uiState.value.copy(isAuthenticated = false)
    }

    fun bypassAuthForDemo() {
        _uiState.value = _uiState.value.copy(
            isAuthenticated = true,
            authError = null,
            isAuthenticating = false
        )
    }
}
