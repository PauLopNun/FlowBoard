package com.flowboard.data.local.dao

import androidx.room.*
import com.flowboard.data.local.entities.PendingOperationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingOperationDao {

    /**
     * Obtener todas las operaciones pendientes ordenadas por fecha de creación
     */
    @Query("SELECT * FROM pending_operations ORDER BY createdAt ASC")
    suspend fun getAllPendingOperations(): List<PendingOperationEntity>

    /**
     * Observar operaciones pendientes
     */
    @Query("SELECT * FROM pending_operations ORDER BY createdAt ASC")
    fun observePendingOperations(): Flow<List<PendingOperationEntity>>

    /**
     * Obtener operaciones pendientes por tipo de entidad
     */
    @Query("SELECT * FROM pending_operations WHERE entityType = :entityType ORDER BY createdAt ASC")
    suspend fun getPendingOperationsByType(entityType: String): List<PendingOperationEntity>

    /**
     * Obtener operaciones pendientes para una entidad específica
     */
    @Query("SELECT * FROM pending_operations WHERE entityType = :entityType AND entityId = :entityId ORDER BY createdAt ASC")
    suspend fun getPendingOperationsForEntity(entityType: String, entityId: String): List<PendingOperationEntity>

    /**
     * Obtener operaciones con pocos intentos (para reintentar)
     */
    @Query("SELECT * FROM pending_operations WHERE attempts < :maxAttempts ORDER BY createdAt ASC")
    suspend fun getRetryableOperations(maxAttempts: Int = 5): List<PendingOperationEntity>

    /**
     * Insertar una nueva operación pendiente
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: PendingOperationEntity): Long

    /**
     * Insertar múltiples operaciones
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperations(operations: List<PendingOperationEntity>)

    /**
     * Actualizar operación (para incrementar intentos, actualizar error, etc.)
     */
    @Update
    suspend fun updateOperation(operation: PendingOperationEntity)

    /**
     * Eliminar operación (cuando se sincronizó exitosamente)
     */
    @Delete
    suspend fun deleteOperation(operation: PendingOperationEntity)

    /**
     * Eliminar operación por ID
     */
    @Query("DELETE FROM pending_operations WHERE id = :id")
    suspend fun deleteOperationById(id: Long)

    /**
     * Eliminar todas las operaciones para una entidad específica
     */
    @Query("DELETE FROM pending_operations WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun deleteOperationsForEntity(entityType: String, entityId: String)

    /**
     * Incrementar el contador de intentos
     */
    @Query("UPDATE pending_operations SET attempts = attempts + 1, lastAttemptAt = :attemptTime, lastError = :error WHERE id = :id")
    suspend fun incrementAttempts(id: Long, attemptTime: String, error: String?)

    /**
     * Limpiar operaciones que fallaron demasiadas veces
     */
    @Query("DELETE FROM pending_operations WHERE attempts >= :maxAttempts")
    suspend fun cleanupFailedOperations(maxAttempts: Int = 10)

    /**
     * Contar operaciones pendientes
     */
    @Query("SELECT COUNT(*) FROM pending_operations")
    suspend fun countPendingOperations(): Int

    /**
     * Observar el conteo de operaciones pendientes
     */
    @Query("SELECT COUNT(*) FROM pending_operations")
    fun observePendingOperationsCount(): Flow<Int>
}
