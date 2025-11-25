package com.flowboard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowboard.domain.model.*
import com.flowboard.domain.repository.PermissionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing permissions
 */
@HiltViewModel
class PermissionViewModel @Inject constructor(
    private val permissionRepository: PermissionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionUiState())
    val uiState: StateFlow<PermissionUiState> = _uiState.asStateFlow()

    private val _permissionsList = MutableStateFlow<PermissionListResponse?>(null)
    val permissionsList: StateFlow<PermissionListResponse?> = _permissionsList.asStateFlow()

    /**
     * Load permissions for a resource
     */
    fun loadResourcePermissions(resourceId: String, resourceType: ResourceType) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            permissionRepository.getResourcePermissions(resourceId, resourceType)
                .onSuccess { response ->
                    _permissionsList.value = response
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load permissions"
                        )
                    }
                }
        }
    }

    /**
     * Grant permission to a user
     */
    fun grantPermission(
        resourceId: String,
        resourceType: ResourceType,
        userEmail: String,
        level: PermissionLevel,
        expiresAt: Long? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val request = GrantPermissionRequest(
                resourceId = resourceId,
                resourceType = resourceType,
                userEmail = userEmail,
                level = level,
                expiresAt = expiresAt
            )

            permissionRepository.grantPermission(request)
                .onSuccess { permission ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            successMessage = "Permission granted to $userEmail"
                        )
                    }
                    // Reload permissions
                    loadResourcePermissions(resourceId, resourceType)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to grant permission"
                        )
                    }
                }
        }
    }

    /**
     * Update an existing permission
     */
    fun updatePermission(permissionId: String, newLevel: PermissionLevel) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val request = UpdatePermissionRequest(
                permissionId = permissionId,
                newLevel = newLevel
            )

            permissionRepository.updatePermission(request)
                .onSuccess { permission ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            successMessage = "Permission updated successfully"
                        )
                    }
                    // Reload permissions if we have the resource info
                    _permissionsList.value?.let { current ->
                        loadResourcePermissions(current.resourceId, current.resourceType)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to update permission"
                        )
                    }
                }
        }
    }

    /**
     * Revoke a permission
     */
    fun revokePermission(permissionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            permissionRepository.revokePermission(permissionId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            successMessage = "Permission revoked successfully"
                        )
                    }
                    // Reload permissions if we have the resource info
                    _permissionsList.value?.let { current ->
                        loadResourcePermissions(current.resourceId, current.resourceType)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to revoke permission"
                        )
                    }
                }
        }
    }

    /**
     * Check if a user has permission
     */
    fun hasPermission(
        resourceId: String,
        resourceType: ResourceType,
        userId: String,
        requiredLevel: PermissionLevel,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            permissionRepository.hasPermission(
                resourceId = resourceId,
                resourceType = resourceType,
                userId = userId,
                requiredLevel = requiredLevel
            ).onSuccess { hasPermission ->
                onResult(hasPermission)
            }.onFailure {
                onResult(false)
            }
        }
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI state for permission management
 */
data class PermissionUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
