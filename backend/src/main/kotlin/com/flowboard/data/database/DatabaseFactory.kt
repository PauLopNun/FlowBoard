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
            SchemaUtils.create(Users, Tasks, Projects)
        }
    }
    
    private fun createHikariDataSource(): HikariDataSource {
        val databaseUrl = System.getenv("DATABASE_URL")

        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"

            if (databaseUrl != null && databaseUrl.startsWith("postgresql://")) {
                // Render/Heroku format: postgresql://user:password@host:port/database
                // Convert to JDBC format: jdbc:postgresql://host:port/database
                val jdbcUrl = databaseUrl.replace("postgresql://", "jdbc:postgresql://")
                    .replaceFirst(Regex("([^:]+):([^@]+)@"), "")

                // Extract username and password
                val credentialsRegex = Regex("postgresql://([^:]+):([^@]+)@")
                val match = credentialsRegex.find(databaseUrl)

                this.jdbcUrl = jdbcUrl
                this.username = match?.groupValues?.get(1) ?: "flowboard"
                this.password = match?.groupValues?.get(2) ?: "flowboard"
            } else {
                // Local development format
                this.jdbcUrl = databaseUrl ?: "jdbc:postgresql://localhost:5432/flowboard"
                this.username = System.getenv("DATABASE_USER") ?: "flowboard"
                this.password = System.getenv("DATABASE_PASSWORD") ?: "flowboard"
            }

            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        return HikariDataSource(config)
    }
    
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}