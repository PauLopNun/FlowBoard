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

@Singleton
class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    private val webClientId = "387871911602-cu1k74j3m3qltnih0763b44ooo6jdosi.apps.googleusercontent.com"

    suspend fun signInWithGoogle(activity: Activity): Result<GoogleSignInResult> = withContext(Dispatchers.Main) {
        val credentialManager = CredentialManager.create(activity)

        // First try GetSignInWithGoogleOption (shows the styled one-tap sheet).
        // On some real devices the tap doesn't register with that option, so fall
        // back to GetGoogleIdOption(filterByAuthorizedAccounts=false) which uses
        // the standard account-picker and is more reliable across devices.
        val primaryOption = GetSignInWithGoogleOption.Builder(webClientId).build()
        val fallbackOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        for ((index, option) in listOf(primaryOption, fallbackOption).withIndex()) {
            try {
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(option)
                    .build()
                val result = credentialManager.getCredential(context = activity, request = request)
                return@withContext handleSignInResult(result)
            } catch (e: GetCredentialCancellationException) {
                return@withContext Result.failure(Exception("UserCancelled"))
            } catch (e: NoCredentialException) {
                if (index == 1) return@withContext Result.failure(Exception("No Google account found on this device"))
                // primary failed silently — try fallback
            } catch (e: GetCredentialException) {
                if (index == 1) return@withContext Result.failure(Exception("Google Sign-In error: ${e.message}"))
                // primary failed — try fallback
            } catch (e: Exception) {
                if (index == 1) return@withContext Result.failure(e)
            }
        }
        Result.failure(Exception("Google Sign-In failed"))
    }

    private fun handleSignInResult(result: GetCredentialResponse): Result<GoogleSignInResult> {
        return try {
            when (val credential = result.credential) {
                is CustomCredential -> {
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        try {
                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
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
                else -> Result.failure(Exception("Unexpected credential type"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class GoogleSignInResult(
    val idToken: String,
    val email: String,
    val displayName: String?,
    val profilePictureUrl: String?
)
