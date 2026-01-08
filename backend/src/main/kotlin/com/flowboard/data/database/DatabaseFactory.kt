package com.flowboard.data.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    
    fun init() {
        val database = Database.connect(createHikariDataSource())
        
        transaction(database) {
            SchemaUtils.create(
                Users,
                Tasks,
                Projects,
                BoardPermissions,
                Documents,
                DocumentPermissions,
                Notifications,
                ChatRooms,
                ChatParticipants,
                Messages
            )
        }
    }
    
    private fun createHikariDataSource(): HikariDataSource {
        val databaseUrl = System.getenv("DATABASE_URL")

        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"

            if (databaseUrl != null && databaseUrl.startsWith("postgresql://")) {
                // Render/Heroku format: postgresql://user:password@host:port/database
                // Convert to JDBC format: jdbc:postgresql://host:port/database
                val regex = Regex("postgresql://([^:]+):([^@]+)@(.+)")
                val match = regex.find(databaseUrl)

                if (match != null) {
                    val username = match.groupValues[1]
                    val password = match.groupValues[2]
                    val hostAndDb = match.groupValues[3]

                    // Add SSL parameters for Render
                    this.jdbcUrl = "jdbc:postgresql://$hostAndDb?sslmode=require"
                    this.username = username
                    this.password = password

                    println("Database connection configured for Render")
                    println("JDBC URL: jdbc:postgresql://$hostAndDb?sslmode=require")
                } else {
                    throw IllegalArgumentException("Invalid DATABASE_URL format: $databaseUrl")
                }
            } else {
                // Local development format
                this.jdbcUrl = databaseUrl ?: "jdbc:postgresql://localhost:5432/flowboard"
                this.username = System.getenv("DATABASE_USER") ?: "flowboard"
                this.password = System.getenv("DATABASE_PASSWORD") ?: "flowboard"

                println("Database connection configured for local development")
            }

            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"

            // Connection timeout settings
            connectionTimeout = 30000 // 30 seconds
            idleTimeout = 600000 // 10 minutes
            maxLifetime = 1800000 // 30 minutes

            validate()
        }
        return HikariDataSource(config)
    }
    
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}