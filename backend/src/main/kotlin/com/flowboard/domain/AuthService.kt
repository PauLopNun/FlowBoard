package com.flowboard.domain

import com.flowboard.data.database.DatabaseFactory.dbQuery
import com.flowboard.data.database.PasswordResetTokens
import com.flowboard.data.database.Users
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.flowboard.data.models.*
import com.flowboard.plugins.JwtConfig
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.mindrot.jbcrypt.BCrypt
import java.util.*

object AuthService {

    suspend fun register(request: RegisterRequest): LoginResponse = dbQuery {
        val existing = Users
            .select { Users.email eq request.email or (Users.username eq request.username) }
            .firstOrNull()

        if (existing != null) {
            throw IllegalArgumentException("User with this email or username already exists")
        }

        val hashedPassword = BCrypt.hashpw(request.password, BCrypt.gensalt())
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val userId = UUID.randomUUID()
        val resolvedFullName = request.fullName?.takeIf { it.isNotBlank() } ?: request.username

        Users.insert {
            it[id] = userId
            it[email] = request.email
            it[username] = request.username
            it[fullName] = resolvedFullName
            it[passwordHash] = hashedPassword
            it[createdAt] = now
        }

        val user = User(
            id = userId.toString(),
            email = request.email,
            username = request.username,
            fullName = request.fullName,
            role = UserRole.USER,
            createdAt = now
        )

        LoginResponse(
            token = JwtConfig.makeToken(request.email, userId.toString(), request.username),
            user = user
        )
    }

    suspend fun login(request: LoginRequest): LoginResponse? = dbQuery {
        val row = Users.select { Users.email eq request.email }.singleOrNull()

        if (row != null && BCrypt.checkpw(request.password, row[Users.passwordHash])) {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            Users.update({ Users.email eq request.email }) { it[lastLoginAt] = now }

            val user = User(
                id = row[Users.id].toString(),
                email = row[Users.email],
                username = row[Users.username],
                fullName = row[Users.fullName],
                role = row[Users.role],
                profileImageUrl = row[Users.profileImageUrl],
                isActive = row[Users.isActive],
                createdAt = row[Users.createdAt],
                lastLoginAt = now
            )

            LoginResponse(
                token = JwtConfig.makeToken(row[Users.email], row[Users.id].toString(), row[Users.username]),
                user = user
            )
        } else {
            null
        }
    }

    suspend fun getUserById(userId: String): User? = dbQuery {
        Users.select { Users.id eq java.util.UUID.fromString(userId) }
            .singleOrNull()
            ?.let { row ->
                User(
                    id = row[Users.id].toString(),
                    email = row[Users.email],
                    username = row[Users.username],
                    fullName = row[Users.fullName],
                    role = row[Users.role],
                    profileImageUrl = row[Users.profileImageUrl],
                    isActive = row[Users.isActive],
                    createdAt = row[Users.createdAt],
                    lastLoginAt = row[Users.lastLoginAt]
                )
            }
    }

    suspend fun getUserByEmail(email: String): User? = dbQuery {
        Users.select { Users.email eq email }
            .singleOrNull()
            ?.let { row ->
                User(
                    id = row[Users.id].toString(),
                    email = row[Users.email],
                    username = row[Users.username],
                    fullName = row[Users.fullName],
                    role = row[Users.role],
                    profileImageUrl = row[Users.profileImageUrl],
                    isActive = row[Users.isActive],
                    createdAt = row[Users.createdAt],
                    lastLoginAt = row[Users.lastLoginAt]
                )
            }
    }

    suspend fun updateProfile(userId: String, fullName: String?, profileImageUrl: String?): User? = dbQuery {
        val uuid = java.util.UUID.fromString(userId)
        Users.update({ Users.id eq uuid }) { row ->
            fullName?.let { row[Users.fullName] = it }
            profileImageUrl?.let { row[Users.profileImageUrl] = it }
        }
        Users.select { Users.id eq uuid }.singleOrNull()?.let { row ->
            User(
                id = row[Users.id].toString(),
                email = row[Users.email],
                username = row[Users.username],
                fullName = row[Users.fullName],
                role = row[Users.role],
                profileImageUrl = row[Users.profileImageUrl],
                isActive = row[Users.isActive],
                createdAt = row[Users.createdAt],
                lastLoginAt = row[Users.lastLoginAt]
            )
        }
    }

    suspend fun updatePassword(userId: String, oldPassword: String, newPassword: String): Boolean = dbQuery {
        val uuid = java.util.UUID.fromString(userId)
        val row = Users.select { Users.id eq uuid }.singleOrNull() ?: return@dbQuery false
        if (!BCrypt.checkpw(oldPassword, row[Users.passwordHash])) return@dbQuery false
        Users.update({ Users.id eq uuid }) { it[passwordHash] = BCrypt.hashpw(newPassword, BCrypt.gensalt()) }
        true
    }

    suspend fun googleSignIn(request: GoogleSignInRequest): LoginResponse = dbQuery {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        val existing = Users.select { Users.email eq request.email }.singleOrNull()

        if (existing != null) {
            Users.update({ Users.email eq request.email }) { it[lastLoginAt] = now }

            val finalUsername = existing[Users.username]
            val user = User(
                id = existing[Users.id].toString(),
                email = existing[Users.email],
                username = finalUsername,
                fullName = existing[Users.fullName],
                role = existing[Users.role],
                profileImageUrl = request.profilePictureUrl ?: existing[Users.profileImageUrl],
                isActive = existing[Users.isActive],
                createdAt = existing[Users.createdAt],
                lastLoginAt = now
            )

            LoginResponse(
                token = JwtConfig.makeToken(request.email, existing[Users.id].toString(), finalUsername),
                user = user
            )
        } else {
            val userId = UUID.randomUUID()
            val baseUsername = request.email.substringBefore("@").replace(Regex("[^a-zA-Z0-9_]"), "_")
            val finalUsername = if (Users.select { Users.username eq baseUsername }.count() == 0L)
                baseUsername
            else
                "${baseUsername}_${userId.toString().take(4)}"

            Users.insert {
                it[id] = userId
                it[email] = request.email
                it[username] = finalUsername
                it[fullName] = request.displayName ?: request.email.substringBefore("@")
                it[passwordHash] = BCrypt.hashpw(UUID.randomUUID().toString(), BCrypt.gensalt())
                it[profileImageUrl] = request.profilePictureUrl
                it[createdAt] = now
                it[lastLoginAt] = now
            }

            val user = User(
                id = userId.toString(),
                email = request.email,
                username = finalUsername,
                fullName = request.displayName ?: request.email.substringBefore("@"),
                role = UserRole.USER,
                profileImageUrl = request.profilePictureUrl,
                createdAt = now,
                lastLoginAt = now
            )

            LoginResponse(
                token = JwtConfig.makeToken(request.email, userId.toString(), finalUsername),
                user = user
            )
        }
    }

    /**
     * Request a password reset. Generates a 6-digit OTP and emails it.
     * Returns true even when the email is unknown (security: no user enumeration).
     */
    suspend fun requestPasswordReset(email: String): Boolean = dbQuery {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val expires = Clock.System.now().plus(15, DateTimeUnit.MINUTE).toLocalDateTime(TimeZone.UTC)

        // Only proceed if the email exists, but always return true
        val userExists = Users.select { Users.email eq email }.count() > 0

        if (userExists) {
            // Invalidate any previous tokens for this email
            PasswordResetTokens.update({ PasswordResetTokens.email eq email }) {
                it[PasswordResetTokens.used] = true
            }

            val code = (100000..999999).random().toString()

            PasswordResetTokens.insert {
                it[PasswordResetTokens.id] = UUID.randomUUID()
                it[PasswordResetTokens.email] = email
                it[PasswordResetTokens.code] = code
                it[PasswordResetTokens.expiresAt] = expires
                it[PasswordResetTokens.used] = false
            }

            // Kick off email outside the transaction (fire-and-forget)
            kotlinx.coroutines.GlobalScope.launch {
                EmailService.sendPasswordResetEmail(email, code)
            }
        }

        true
    }

    /**
     * Confirm a password reset using the OTP code.
     * Returns false when the code is invalid, expired, or already used.
     */
    suspend fun confirmPasswordReset(email: String, code: String, newPassword: String): Boolean = dbQuery {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        val token = PasswordResetTokens
            .select {
                (PasswordResetTokens.email eq email) and
                (PasswordResetTokens.code eq code) and
                (PasswordResetTokens.used eq false)
            }
            .singleOrNull()

        if (token == null || token[PasswordResetTokens.expiresAt] < now) {
            return@dbQuery false
        }

        // Mark token used
        PasswordResetTokens.update({
            (PasswordResetTokens.email eq email) and (PasswordResetTokens.code eq code)
        }) {
            it[PasswordResetTokens.used] = true
        }

        // Update password
        Users.update({ Users.email eq email }) {
            it[Users.passwordHash] = BCrypt.hashpw(newPassword, BCrypt.gensalt())
        }

        true
    }
}
