package com.flowboard.data.remote.api

import com.flowboard.routes.InviteRequest
import io.ktor.client.*
import io.ktor.client.request.*
import javax.inject.Inject

class PermissionApiServiceImpl @Inject constructor(
    private val client: HttpClient
) : PermissionApiService {
    override suspend fun inviteUser(boardId: String, inviteRequest: InviteRequest) {
        client.post("boards/$boardId/invite") {
            setBody(inviteRequest)
        }
    }
}
