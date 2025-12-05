package com.flowboard.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad para rastrear operaciones pendientes que deben sincronizarse con el servidor
 * Esto permite un sistema offline-first robusto donde todas las operaciones se registran
 * localmente y se sincronizan cuando hay conexión
 */
@Entity(tableName = "pending_operations")
data class PendingOperationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Tipo de operación: CREATE, UPDATE, DELETE
     */
    val operationType: String,

    /**
     * Tipo de entidad: DOCUMENT, TASK, PROJECT, etc.
     */
    val entityType: String,

    /**
     * ID de la entidad afectada
     */
    val entityId: String,

    /**
     * Datos de la operación en formato JSON (si aplica)
     * Para CREATE/UPDATE contiene los datos completos
     * Para DELETE puede estar vacío
     */
    val data: String? = null,

    /**
     * Timestamp de cuando se creó la operación
     */
    val createdAt: String,

    /**
     * Número de intentos de sincronización
     */
    val attempts: Int = 0,

    /**
     * Timestamp del último intento de sincronización
     */
    val lastAttemptAt: String? = null,

    /**
     * Mensaje de error del último intento (si falló)
     */
    val lastError: String? = null
)

/**
 * Tipos de operación
 */
object OperationType {
    const val CREATE = "CREATE"
    const val UPDATE = "UPDATE"
    const val DELETE = "DELETE"
}

/**
 * Tipos de entidad
 */
object EntityType {
    const val DOCUMENT = "DOCUMENT"
    const val TASK = "TASK"
    const val PROJECT = "PROJECT"
}
