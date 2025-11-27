package com.flowboard.data.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for handling Google Sign-In using Credential Manager API
 */
@Singleton
class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    // Google Cloud Web Client ID for OAuth 2.0
    // From: https://console.cloud.google.com/apis/credentials
    private val webClientId = "387871911602-3ps8i85m95609nepmoboaaqcf7n40kos.apps.googleusercontent.com"

    /**
     * Sign in with Google and return the ID token
     * Requires Activity context for Credential Manager
     */
    suspend fun signInWithGoogle(activity: Activity): Result<GoogleSignInResult> = withContext(Dispatchers.IO) {
        try {
            val credentialManager = CredentialManager.create(activity)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)  // Changed to false to always show account picker
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                context = activity,
                request = request
            )

            handleSignInResult(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun handleSignInResult(result: GetCredentialResponse): Result<GoogleSignInResult> {
        return try {
            when (val credential = result.credential) {
                is CustomCredential -> {
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        try {
                            val googleIdTokenCredential = GoogleIdTokenCredential
                                .createFrom(credential.data)

                            Result.success(
                                GoogleSignInResult(
                                    idToken = googleIdTokenCredential.idToken,
                                    email = googleIdTokenCredential.id,
                                    displayName = googleIdTokenCredential.displayName,
                                    profilePictureUrl = googleIdTokenCredential.profilePictureUri?.toString()
                                )
                            )
                        } catch (e: GoogleIdTokenParsingException) {
                            Result.failure(Exception("Invalid Google ID token: ${e.message}"))
                        }
                    } else {
                        Result.failure(Exception("Unexpected credential type: ${credential.type}"))
                    }
                }
                else -> {
                    Result.failure(Exception("Unexpected credential type"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Result of Google Sign-In
 */
data class GoogleSignInResult(
    val idToken: String,
    val email: String,
    val displayName: String?,
    val profilePictureUrl: String?
)
