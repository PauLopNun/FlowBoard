package com.flowboard.plugins

import com.flowboard.data.database.DatabaseFactory
import io.ktor.server.application.*

fun Application.configureDatabase() {
    DatabaseFactory.init()
}