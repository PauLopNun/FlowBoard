package com.flowboard.data.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    
    private var initialized = false
    private var initializationError: Exception? = null

    fun init() {
        if (initialized) {
            println("⚠️  Database already initialized")
            return
        }

        try {
            println("🔍 Attempting database initialization...")
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
                    Messages,
                    PasswordResetTokens,
                    Workspaces,
                    WorkspaceMembers
                )

                runCompatibilityMigrations()

                listOf<Table>(
                    Users,
                    Tasks,
                    Projects,
                    BoardPermissions,
                    Documents,
                    DocumentPermissions,
                    Notifications,
                    ChatRooms,
                    ChatParticipants,
                    Messages,
                    PasswordResetTokens,
                    Workspaces,
                    WorkspaceMembers
                ).forEach { table ->
                    try {
                        SchemaUtils.createMissingTablesAndColumns(table)
                    } catch (e: Exception) {
                        System.err.println("Schema sync skipped for ${table.tableName}: ${e.message}")
                    }
                }
            }
            initialized = true
            println("✅ Database initialized successfully")
        } catch (e: Exception) {
            initializationError = e
            System.err.println("❌ Database initialization failed: ${e.message}")
            System.err.println("⚠️  Application will start WITHOUT database functionality")
            System.err.println("📋 This is expected during Docker build - DB will initialize on first request")
            // Don't throw - allow app to start
        }
    }

    fun isInitialized(): Boolean = initialized

    fun getInitializationError(): Exception? = initializationError

    private fun createHikariDataSource(): HikariDataSource {
        val databaseUrl = System.getenv("DATABASE_URL")

        println("🔍 Configuring database connection...")
        println("DATABASE_URL present: ${databaseUrl != null}")

        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"

            if (databaseUrl != null && databaseUrl.startsWith("postgresql://")) {
                // Render/Heroku format: postgresql://user:password@host:port/database
                // Convert to JDBC format: jdbc:postgresql://host:port/database
                val regex = Regex("postgresql://([^:]+):([^@]+)@([^/]+)/(.+)")
                val match = regex.find(databaseUrl)

                if (match != null) {
                    val username = match.groupValues[1]
                    val password = match.groupValues[2]
                    val hostAndPort = match.groupValues[3]
                    val database = match.groupValues[4]

                    // Render injects the internal connection string (dpg-xxx-a) via fromDatabase.
                    // Free tier has no private networking, so we must use the external hostname.
                    // Build external host: dpg-xxx-a -> dpg-xxx-a.oregon-postgres.render.com:5432
                    val externalHost = when {
                        hostAndPort.contains(":") -> hostAndPort // already has port
                        hostAndPort.contains(".") -> "$hostAndPort:5432" // has domain, add port
                        hostAndPort.startsWith("dpg-") -> "$hostAndPort.oregon-postgres.render.com:5432"
                        else -> "$hostAndPort:5432"
                    }

                    this.jdbcUrl = "jdbc:postgresql://$externalHost/$database?sslmode=require"
                    this.username = username
                    this.password = password

                    println("✅ Database connection configured for Render (external)")
                    println("📍 Host: $externalHost")
                    println("🗄️  Database: $database")
                    println("🔗 JDBC URL: jdbc:postgresql://$externalHost/$database?sslmode=require")
                } else {
                    throw IllegalArgumentException("Invalid DATABASE_URL format: $databaseUrl")
                }
            } else {
                // Local development format
                this.jdbcUrl = databaseUrl ?: "jdbc:postgresql://localhost:5432/flowboard"
                this.username = System.getenv("DATABASE_USER") ?: "flowboard"
                this.password = System.getenv("DATABASE_PASSWORD") ?: "flowboard"

                println("✅ Database connection configured for local development")
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

    private fun Transaction.runCompatibilityMigrations() {
        exec("ALTER TABLE documents ADD COLUMN IF NOT EXISTS parent_id UUID")
        exec("ALTER TABLE documents ADD COLUMN IF NOT EXISTS visibility VARCHAR(20) NOT NULL DEFAULT 'private'")
        exec("ALTER TABLE documents ADD COLUMN IF NOT EXISTS workspace_id UUID")
        exec("ALTER TABLE documents ADD COLUMN IF NOT EXISTS last_edited_by UUID")
        exec("ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_image_url TEXT")
        exec("ALTER TABLE users ALTER COLUMN profile_image_url TYPE TEXT")
        exec("ALTER TABLE workspaces ADD COLUMN IF NOT EXISTS image_url TEXT")
        exec("ALTER TABLE workspaces ALTER COLUMN image_url TYPE TEXT")
        exec("ALTER TABLE document_permissions ADD COLUMN IF NOT EXISTS role VARCHAR(50) NOT NULL DEFAULT 'viewer'")
        exec("ALTER TABLE document_permissions ADD COLUMN IF NOT EXISTS granted_by UUID")
        exec(
            """
            UPDATE document_permissions dp
            SET granted_by = d.owner_id
            FROM documents d
            WHERE dp.document_id = d.id AND dp.granted_by IS NULL
            """.trimIndent()
        )
        exec("UPDATE document_permissions SET granted_by = user_id WHERE granted_by IS NULL")
        exec("ALTER TABLE document_permissions ALTER COLUMN granted_by SET NOT NULL")
        exec("ALTER TABLE document_permissions ADD COLUMN IF NOT EXISTS granted_at TIMESTAMP")
        exec(
            """
            UPDATE document_permissions dp
            SET granted_at = COALESCE(d.created_at, NOW())
            FROM documents d
            WHERE dp.document_id = d.id AND dp.granted_at IS NULL
            """.trimIndent()
        )
        exec("UPDATE document_permissions SET granted_at = NOW() WHERE granted_at IS NULL")
        exec("ALTER TABLE document_permissions ALTER COLUMN granted_at SET NOT NULL")
    }
    
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
