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
}