package com.flowboard.data.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
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
    private val webClientId = "387871911602-cu1k74j3m3qltnih0763b44ooo6jdosi.apps.googleusercontent.com"

    /**
     * Sign in with Google and return the ID token.
     * Uses a two-step approach:
     * 1. Try accounts already authorized with this app (silent / one-tap)
     * 2. Fall back to showing the full account picker for any Google account on the device
     *    — only if step 1 returned NoCredentialException (not on cancel or other errors)
     */
    suspend fun signInWithGoogle(activity: Activity): Result<GoogleSignInResult> = withContext(Dispatchers.IO) {
        val credentialManager = CredentialManager.create(activity)

        // Step 1: try accounts already authorized with this app
        val authorizedResult = attemptSignIn(credentialManager, activity, filterByAuthorized = true)
        if (authorizedResult.isSuccess) return@withContext authorizedResult

        // Only fall back to step 2 when there are simply no authorized accounts yet
        val step1Error = authorizedResult.exceptionOrNull()
        if (step1Error !is NoCredentialException) return@withContext authorizedResult

        // Step 2: show full account picker (all Google accounts on device)
        val allAccountsResult = attemptSignIn(credentialManager, activity, filterByAuthorized = false)
        if (allAccountsResult.isSuccess) return@withContext allAccountsResult
        val step2Error = allAccountsResult.exceptionOrNull()
        if (step2Error !is NoCredentialException) return@withContext allAccountsResult

        // Step 3: use GetSignInWithGoogleOption — the official "Sign in with Google" button flow
        // that always shows the Google account chooser regardless of prior authorization state
        attemptSignInWithGoogleOption(credentialManager, activity)
    }

    private suspend fun attemptSignInWithGoogleOption(
        credentialManager: CredentialManager,
        activity: Activity
    ): Result<GoogleSignInResult> {
        return try {
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId).build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            val result = credentialManager.getCredential(context = activity, request = request)
            handleSignInResult(result)
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Cancelled"))
        } catch (e: GetCredentialException) {
            Result.failure(Exception("Google Sign-In error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun attemptSignIn(
        credentialManager: CredentialManager,
        activity: Activity,
        filterByAuthorized: Boolean
    ): Result<GoogleSignInResult> {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(filterByAuthorized)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                context = activity,
                request = request
            )

            handleSignInResult(result)
        } catch (e: GetCredentialCancellationException) {
            // User dismissed the picker — treat as a silent cancel
            Result.failure(Exception("Cancelled"))
        } catch (e: NoCredentialException) {
            // No credentials for this filter level — caller decides whether to retry
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Result.failure(Exception("Google Sign-In error: ${e.message}"))
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
