package com.flowboard.domain

import com.flowboard.data.database.BoardPermissions
import com.flowboard.data.database.DatabaseFactory.dbQuery
import com.flowboard.data.models.crdt.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import java.util.*
import java.util.concurrent.ConcurrentHashMap

interface DocumentService {
    suspend fun getDocument(boardId: String): CollaborativeDocument
    suspend fun applyOperation(operation: DocumentOperation): CollaborativeDocument
}

class InMemoryDocumentService(
    private val webSocketManager: com.flowboard.services.WebSocketManager
) : DocumentService {
    private val documents = ConcurrentHashMap<String, CollaborativeDocument>()


    override suspend fun getDocument(boardId: String): CollaborativeDocument {
        return documents.getOrPut(boardId) {
            CollaborativeDocument(
                id = boardId,
                blocks = listOf(
                    ContentBlock("1", "h1", "Welcome to the collaborative editor!"),
                    ContentBlock("2", "p", "This is a collaborative document.")
                )
            )
        }
    }

    override suspend fun applyOperation(operation: DocumentOperation): CollaborativeDocument {
        val document = getDocument(operation.boardId)
        val updatedDocument = when (operation) {
            is AddBlockOperation -> {
                val newBlocks = document.blocks.toMutableList()
                val index = if (operation.afterBlockId == null) 0 else newBlocks.indexOfFirst { it.id == operation.afterBlockId } + 1
                newBlocks.add(index, operation.block)
                document.copy(blocks = newBlocks)
            }
            is DeleteBlockOperation -> {
                val newBlocks = document.blocks.filter { it.id != operation.blockId }
                document.copy(blocks = newBlocks)
            }
            is UpdateBlockContentOperation -> {
                val newBlocks = document.blocks.map {
                    if (it.id == operation.blockId) {
                        it.copy(content = operation.content)
                    } else {
                        it
                    }
                }
                document.copy(blocks = newBlocks)
            }
            is UpdateBlockFormattingOperation -> {
                val newBlocks = document.blocks.map {
                    if (it.id == operation.blockId) {
                        it.copy(
                            fontWeight = operation.fontWeight ?: it.fontWeight,
                            fontStyle = operation.fontStyle ?: it.fontStyle,
                            textDecoration = operation.textDecoration ?: it.textDecoration,
                            fontSize = operation.fontSize ?: it.fontSize,
                            color = operation.color ?: it.color,
                            textAlign = operation.textAlign ?: it.textAlign
                        )
                    } else {
                        it
                    }
                }
                document.copy(blocks = newBlocks)
            }
            is UpdateBlockTypeOperation -> {
                val newBlocks = document.blocks.map {
                    if (it.id == operation.blockId) {
                        it.copy(type = operation.newType)
                    } else {
                        it
                    }
                }
                document.copy(blocks = newBlocks)
            }
            else -> document // Ignore other operations for now
        }
        documents[operation.boardId] = updatedDocument
        return updatedDocument
    }
}
