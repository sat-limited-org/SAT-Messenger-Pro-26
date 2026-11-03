package com.example.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface AuthUiState {
    data object Initial : AuthUiState
    data object Loading : AuthUiState
    data class Authenticated(val user: FirebaseUser) : AuthUiState
    data class Error(val message: String) : AuthUiState
    data object Unauthenticated : AuthUiState
}

class AuthRepository(private val context: Context) {

    private val auth: FirebaseAuth = Firebase.auth
    private val credentialManager = CredentialManager.create(context)

    private val _authState = MutableStateFlow<AuthUiState>(
        auth.currentUser?.let { AuthUiState.Authenticated(it) } ?: AuthUiState.Unauthenticated
    )
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null) {
            _authState.value = AuthUiState.Authenticated(user)
        } else {
            _authState.value = AuthUiState.Unauthenticated
        }
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun attemptAutoSignIn(scope: CoroutineScope) {
        if (auth.currentUser != null) {
            _authState.value = AuthUiState.Authenticated(auth.currentUser!!)
            return
        }

        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            _authState.value = AuthUiState.Unauthenticated
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    authResult.user?.let {
                        _authState.value = AuthUiState.Authenticated(it)
                    } ?: run {
                        _authState.value = AuthUiState.Unauthenticated
                    }
                } else {
                    _authState.value = AuthUiState.Unauthenticated
                }
            } catch (e: Exception) {
                _authState.value = AuthUiState.Unauthenticated
            }
        }
    }

    fun signInWithGoogle(
        activity: Activity,
        scope: CoroutineScope,
        onError: (String) -> Unit = {}
    ) {
        val clientId = try {
            activity.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            val err = "Google Sign-In configuration missing: default_web_client_id not found"
            _authState.value = AuthUiState.Error(err)
            onError(err)
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

        _authState.value = AuthUiState.Loading

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(authCredential).await()
                    authResult.user?.let {
                        _authState.value = AuthUiState.Authenticated(it)
                    } ?: run {
                        _authState.value = AuthUiState.Error("Sign in failed")
                        onError("Sign in failed")
                    }
                } else {
                    val err = "Unexpected credential returned"
                    _authState.value = AuthUiState.Error(err)
                    onError(err)
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("Auth", "Google Sign-In flow cancelled: ${e.message}", e)
                _authState.value = AuthUiState.Unauthenticated
            } catch (e: Exception) {
                Log.e("Auth", "Google Sign-In failed", e)
                val errorMsg = e.localizedMessage ?: "Sign in failed"
                _authState.value = AuthUiState.Error(errorMsg)
                onError(errorMsg)
            }
        }
    }

    fun signOut(scope: CoroutineScope, onComplete: () -> Unit = {}) {
        auth.signOut()
        scope.launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e("Auth", "Failed to clear credential state", e)
            } finally {
                _authState.value = AuthUiState.Unauthenticated
                onComplete()
            }
        }
    }
}
