package com.flowboard.data.local.dao

import androidx.room.*
import com.flowboard.data.local.entities.DocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents ORDER BY updatedAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: String): DocumentEntity?

    @Query("SELECT * FROM documents WHERE id = :id")
    fun observeDocumentById(id: String): Flow<DocumentEntity?>

    @Query("SELECT * FROM documents WHERE ownerId = :userId ORDER BY updatedAt DESC")
    fun getDocumentsByOwner(userId: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isSync = 0")
    suspend fun getUnsyncedDocuments(): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE isSync = 0 AND lastSyncAt IS NULL")
    suspend fun getPendingSyncDocuments(): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE isSync = 0 AND lastSyncAt IS NOT NULL")
    suspend fun getFailedSyncDocuments(): List<DocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<DocumentEntity>)

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: String)

    @Query("UPDATE documents SET isSync = :isSync, lastSyncAt = :lastSyncAt WHERE id = :id")
    suspend fun updateSyncStatus(id: String, isSync: Boolean, lastSyncAt: String?)

    @Query("UPDATE documents SET content = :content, updatedAt = :updatedAt, isSync = 0 WHERE id = :id")
    suspend fun updateContent(id: String, content: String, updatedAt: String)
}
