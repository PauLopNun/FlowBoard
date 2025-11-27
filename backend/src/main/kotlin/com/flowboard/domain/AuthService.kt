package com.flowboard.domain

import com.flowboard.data.database.DatabaseFactory.dbQuery
import com.flowboard.data.database.Users
import com.flowboard.data.models.*
import com.flowboard.plugins.JwtConfig
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.mindrot.jbcrypt.BCrypt
import java.util.*

object AuthService {
    
    suspend fun register(request: RegisterRequest): LoginResponse = dbQuery {
        // Check if user already exists
        val existingUser = Users.select { Users.email eq request.email or (Users.username eq request.username) }
            .singleOrNull()
        
        if (existingUser != null) {
            throw IllegalArgumentException("User with this email or username already exists")
        }
        
        // Hash password
        val hashedPassword = BCrypt.hashpw(request.password, BCrypt.gensalt())
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val userId = UUID.randomUUID()
        
        // Create user
        Users.insert {
            it[id] = userId
            it[email] = request.email
            it[username] = request.username
            it[fullName] = request.fullName
            it[passwordHash] = hashedPassword
            it[createdAt] = now
        }
        
        // Create user object
        val user = User(
            id = userId.toString(),
            email = request.email,
            username = request.username,
            fullName = request.fullName,
            role = UserRole.USER,
            createdAt = now
        )
        
        // Generate JWT token
        val token = JwtConfig.makeToken(request.email, userId.toString())
        
        LoginResponse(token = token, user = user)
    }
    
    suspend fun login(request: LoginRequest): LoginResponse? = dbQuery {
        val userRow = Users.select { Users.email eq request.email }
            .singleOrNull()

        if (userRow != null && BCrypt.checkpw(request.password, userRow[Users.passwordHash])) {
            // Update last login
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            Users.update({ Users.email eq request.email }) {
                it[lastLoginAt] = now
            }

            val user = User(
                id = userRow[Users.id].toString(),
                email = userRow[Users.email],
                username = userRow[Users.username],
                fullName = userRow[Users.fullName],
                role = userRow[Users.role],
                profileImageUrl = userRow[Users.profileImageUrl],
                isActive = userRow[Users.isActive],
                createdAt = userRow[Users.createdAt],
                lastLoginAt = now
            )

            val token = JwtConfig.makeToken(request.email, userRow[Users.id].toString())

            LoginResponse(token = token, user = user)
        } else {
            null
        }
    }

    suspend fun googleSignIn(request: GoogleSignInRequest): LoginResponse = dbQuery {
        // TODO: In production, verify the idToken with Google's API
        // For now, we trust the client has verified it

        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        // Check if user exists
        val existingUser = Users.select { Users.email eq request.email }
            .singleOrNull()

        if (existingUser != null) {
            // User exists - log them in
            Users.update({ Users.email eq request.email }) {
                it[lastLoginAt] = now
                // Update profile image if provided
                if (request.profilePictureUrl != null) {
                    it[profileImageUrl] = request.profilePictureUrl
                }
            }

            val user = User(
                id = existingUser[Users.id].toString(),
                email = existingUser[Users.email],
                username = existingUser[Users.username],
                fullName = existingUser[Users.fullName],
                role = existingUser[Users.role],
                profileImageUrl = request.profilePictureUrl ?: existingUser[Users.profileImageUrl],
                isActive = existingUser[Users.isActive],
                createdAt = existingUser[Users.createdAt],
                lastLoginAt = now
            )

            val token = JwtConfig.makeToken(request.email, existingUser[Users.id].toString())
            LoginResponse(token = token, user = user)
        } else {
            // New user - create account
            val userId = UUID.randomUUID()

            // Generate username from email if displayName is not provided
            val username = request.displayName?.replace(" ", "")?.lowercase()
                ?: request.email.substringBefore("@")

            // Make sure username is unique
            var finalUsername = username
            var counter = 1
            while (Users.select { Users.username eq finalUsername }.count() > 0) {
                finalUsername = "$username$counter"
                counter++
            }

            Users.insert {
                it[id] = userId
                it[email] = request.email
                it[Users.username] = finalUsername
                it[fullName] = request.displayName ?: request.email.substringBefore("@")
                it[passwordHash] = "" // No password for Google sign-in users
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

            val token = JwtConfig.makeToken(request.email, userId.toString())
            LoginResponse(token = token, user = user)
        }
    }

    suspend fun getUserById(id: String): User? = dbQuery {
        Users.select { Users.id eq UUID.fromString(id) }
            .map { row ->
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
            .singleOrNull()
    }
    
    suspend fun getUserByEmail(email: String): User? = dbQuery {
        Users.select { Users.email eq email }
            .map { row ->
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
            .singleOrNull()
    }

    suspend fun updateProfile(userId: String, fullName: String?, profileImageUrl: String?): User? = dbQuery {
        val updateCount = Users.update({ Users.id eq UUID.fromString(userId) }) {
            if (fullName != null) it[Users.fullName] = fullName
            if (profileImageUrl != null) it[Users.profileImageUrl] = profileImageUrl
        }

        if (updateCount > 0) {
            getUserById(userId)
        } else {
            null
        }
    }

    suspend fun updatePassword(userId: String, oldPassword: String, newPassword: String): Boolean = dbQuery {
        val userRow = Users.select { Users.id eq UUID.fromString(userId) }
            .singleOrNull()

        if (userRow != null && BCrypt.checkpw(oldPassword, userRow[Users.passwordHash])) {
            val hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt())
            Users.update({ Users.id eq UUID.fromString(userId) }) {
                it[passwordHash] = hashedPassword
            }
            true
        } else {
            false
        }
    }
}