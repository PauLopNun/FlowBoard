package com.flowboard.data.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    private val webClientId = "387871911602-cu1k74j3m3qltnih0763b44ooo6jdosi.apps.googleusercontent.com"
    private val tag = "GoogleAuthManager"

    suspend fun signInWithGoogle(activity: Activity): Result<GoogleSignInResult> = withContext(Dispatchers.Main) {
        val credentialManager = CredentialManager.create(activity)

        // Step 1: fast path — auto-select if the user already authorized this app.
        val authorizedResult = tryGoogleIdOption(credentialManager, activity)
        if (authorizedResult != null) return@withContext authorizedResult

        // Step 2: full account picker via GetSignInWithGoogleOption.
        // More reliable on physical devices than GetGoogleIdOption(filterAuthorized=false).
        return@withContext trySignInWithGoogle(credentialManager, activity)
    }

    private suspend fun tryGoogleIdOption(
        credentialManager: CredentialManager,
        activity: Activity
    ): Result<GoogleSignInResult>? {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(true)
            .setNonce(generateNonce())
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val response = credentialManager.getCredential(context = activity, request = request)
            handleSignInResult(response)
        } catch (e: Exception) {
            // Any failure in the fast path (including cancellation from bottom-sheet interaction
            // on some devices) falls through to the full GetSignInWithGoogleOption picker.
            Log.d(tag, "Fast path failed (${e.javaClass.simpleName}: ${e.message}), using full picker")
            null
        }
    }

    private suspend fun trySignInWithGoogle(
        credentialManager: CredentialManager,
        activity: Activity
    ): Result<GoogleSignInResult> {
        val signInOption = GetSignInWithGoogleOption.Builder(webClientId)
            .setNonce(generateNonce())
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        return try {
            val response = credentialManager.getCredential(context = activity, request = request)
            handleSignInResult(response)
        } catch (e: GetCredentialCancellationException) {
            Log.w(tag, "GetCredentialCancellationException — cause: ${e.cause?.javaClass?.simpleName} msg: ${e.message}")
            Result.failure(Exception("UserCancelled"))
        } catch (e: NoCredentialException) {
            Log.w(tag, "NoCredentialException in full picker: ${e.message}")
            Result.failure(Exception("NoCredential"))
        } catch (e: GetCredentialUnknownException) {
            val msg = e.message ?: ""
            Log.e(tag, "GetCredentialUnknownException: $msg")
            if (msg.contains("10") || msg.contains("developer_error", ignoreCase = true)) {
                Result.failure(Exception("SHA1NotRegistered"))
            } else {
                Result.failure(Exception("Google Sign-In error: $msg"))
            }
        } catch (e: GetCredentialException) {
            val msg = e.message ?: ""
            Log.e(tag, "GetCredentialException type=${e.javaClass.simpleName}: $msg")
            if (msg.contains("10") || msg.contains("developer_error", ignoreCase = true)) {
                Result.failure(Exception("SHA1NotRegistered"))
            } else {
                Result.failure(Exception("Google Sign-In error: $msg"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Unexpected exception: ${e.javaClass.simpleName}: ${e.message}")
            Result.failure(e)
        }
    }

    private fun generateNonce(): String {
        val raw = UUID.randomUUID().toString()
        return MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
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
