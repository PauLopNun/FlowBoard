package com.flowboard.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class InviteRequest(
    val userId: String
)
