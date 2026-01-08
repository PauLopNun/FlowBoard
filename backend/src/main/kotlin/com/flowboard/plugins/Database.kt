package com.flowboard.plugins

import com.flowboard.data.database.DatabaseFactory
import io.ktor.server.application.*

fun Application.configureDatabase() {
    // Database initialization is now lazy - it will happen on first use
    // This prevents build failures when DATABASE_URL is not accessible during Docker build
    println("📦 Database configuration registered (lazy initialization)")

    environment.monitor.subscribe(ApplicationStarted) {
        println("🚀 Application started - initializing database...")
        DatabaseFactory.init()
    }
}