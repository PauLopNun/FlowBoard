package com.flowboard.domain.repository

import com.flowboard.data.remote.api.PermissionApiService
import com.flowboard.routes.InviteRequest
import javax.inject.Inject

class PermissionRepository @Inject constructor(
    private val permissionApiService: PermissionApiService
) {
    suspend fun inviteUser(boardId: String, userId: String) {
        permissionApiService.inviteUser(boardId, InviteRequest(userId))
    }
}
