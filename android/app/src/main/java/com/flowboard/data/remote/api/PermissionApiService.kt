package com.flowboard.data.remote.api

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface PermissionApiService {
    @POST("boards/{boardId}/invite")
    suspend fun inviteUser(
        @Path("boardId") boardId: String,
        @Body inviteRequest: com.flowboard.routes.InviteRequest
    )
}
