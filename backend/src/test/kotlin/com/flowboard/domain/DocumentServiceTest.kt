package com.flowboard.domain

import com.flowboard.data.models.DocumentOperationMessage
import com.flowboard.data.models.DocumentWebSocketMessage
import com.flowboard.data.models.crdt.ContentBlock
import com.flowboard.data.models.crdt.MoveBlockOperation
import com.flowboard.services.WebSocketManager
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DocumentServiceTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `move block operation deserializes from websocket message`() {
        val messageJson = """
            {
              "type":"DOCUMENT_OPERATION",
              "timestamp":"2026-05-12T22:41:30.935098",
              "operation":{
                "type":"MOVE_BLOCK",
                "operationId":"op-1",
                "boardId":"board-1",
                "blockId":"block-3",
                "afterBlockId":"block-1"
              },
              "userId":"user-1"
            }
        """.trimIndent()

        val message = json.decodeFromString<DocumentWebSocketMessage>(messageJson)
        val operationMessage = assertIs<DocumentOperationMessage>(message)
        val operation = assertIs<MoveBlockOperation>(operationMessage.operation)

        assertEquals("block-3", operation.blockId)
        assertEquals("block-1", operation.afterBlockId)
    }

    @Test
    fun `move block operation reorders document blocks`() = runTest {
        val service = InMemoryDocumentService(WebSocketManager())
        service.initializeDocument(
            boardId = "board-1",
            blocks = listOf(
                ContentBlock(id = "block-1", type = "p", content = "One"),
                ContentBlock(id = "block-2", type = "p", content = "Two"),
                ContentBlock(id = "block-3", type = "p", content = "Three")
            )
        )

        val document = service.applyOperation(
            MoveBlockOperation(
                operationId = "op-1",
                boardId = "board-1",
                blockId = "block-3",
                afterBlockId = "block-1"
            )
        )

        assertEquals(listOf("block-1", "block-3", "block-2"), document.blocks.map { it.id })
    }

    @Test
    fun `move block operation supports moving to top`() = runTest {
        val service = InMemoryDocumentService(WebSocketManager())
        service.initializeDocument(
            boardId = "board-1",
            blocks = listOf(
                ContentBlock(id = "block-1", type = "p", content = "One"),
                ContentBlock(id = "block-2", type = "p", content = "Two")
            )
        )

        val document = service.applyOperation(
            MoveBlockOperation(
                operationId = "op-1",
                boardId = "board-1",
                blockId = "block-2",
                afterBlockId = null
            )
        )

        assertEquals(listOf("block-2", "block-1"), document.blocks.map { it.id })
    }
}
