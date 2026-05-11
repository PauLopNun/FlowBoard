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

        // Step 1: try accounts already authorized with this app (fast path for returning users).
        // If none are authorized, step 2 will show the full account picker.
        val authorizedResult = tryGetCredential(
            credentialManager, activity,
            filterByAuthorizedAccounts = true
        )

        if (authorizedResult != null) return@withContext authorizedResult

        // Step 2: show full account picker (new users, or no prior authorized account).
        return@withContext tryGetCredential(
            credentialManager, activity,
            filterByAuthorizedAccounts = false
        ) ?: Result.failure(Exception("NoCredential"))
    }

    private suspend fun tryGetCredential(
        credentialManager: CredentialManager,
        activity: Activity,
        filterByAuthorizedAccounts: Boolean
    ): Result<GoogleSignInResult>? {
        val nonce = generateNonce()
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(filterByAuthorizedAccounts) // auto-select only for returning users
            .setNonce(nonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val response = credentialManager.getCredential(context = activity, request = request)
            handleSignInResult(response)
        } catch (e: GetCredentialCancellationException) {
            Log.d(tag, "Sign-in cancelled by user (filterAuthorized=$filterByAuthorizedAccounts)")
            Result.failure(Exception("UserCancelled"))
        } catch (e: NoCredentialException) {
            Log.w(tag, "NoCredentialException (filterAuthorized=$filterByAuthorizedAccounts): ${e.message}")
            // Return null to allow caller to try next option; only the second call returns this as failure.
            if (filterByAuthorizedAccounts) null else Result.failure(Exception("NoCredential"))
        } catch (e: GetCredentialUnknownException) {
            val msg = e.message ?: ""
            Log.e(tag, "GetCredentialUnknownException (filterAuthorized=$filterByAuthorizedAccounts): $msg")
            if (msg.contains("10") || msg.contains("developer_error", ignoreCase = true)) {
                Result.failure(Exception("SHA1NotRegistered"))
            } else {
                Result.failure(Exception("Google Sign-In error: $msg"))
            }
        } catch (e: GetCredentialException) {
            val msg = e.message ?: ""
            Log.e(tag, "GetCredentialException type=${e.javaClass.simpleName} (filterAuthorized=$filterByAuthorizedAccounts): $msg")
            // DEVELOPER_ERROR manifests as GetCredentialException with "10" in some GPS versions
            if (msg.contains("10") || msg.contains("developer_error", ignoreCase = true)) {
                Result.failure(Exception("SHA1NotRegistered"))
            } else if (filterByAuthorizedAccounts) {
                null // fall through to full picker
            } else {
                Result.failure(Exception("Google Sign-In error: $msg"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Unexpected exception (filterAuthorized=$filterByAuthorizedAccounts): ${e.javaClass.simpleName}: ${e.message}")
            if (filterByAuthorizedAccounts) null else Result.failure(e)
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
