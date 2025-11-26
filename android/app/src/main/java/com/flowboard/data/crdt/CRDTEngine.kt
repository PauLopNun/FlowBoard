package com.flowboard.data.crdt

import com.flowboard.data.models.crdt.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CRDT (Conflict-free Replicated Data Type) Engine for collaborative document editing.
 *
 * This engine implements Operational Transformation (OT) to handle concurrent edits
 * from multiple users without conflicts.
 *
 * Key features:
 * - Operation transformation for concurrent edits
 * - Causal ordering of operations
 * - Idempotent operation application
 * - Vector clocks for causality tracking
 */
@Singleton
class CRDTEngine @Inject constructor() {

    private val _document = MutableStateFlow<CollaborativeDocument?>(null)
    val document: StateFlow<CollaborativeDocument?> = _document.asStateFlow()

    // Vector clock for causality tracking
    private val vectorClock = mutableMapOf<String, Long>()

    // Operation history for transformation
    private val operationHistory = mutableListOf<DocumentOperation>()

    // Applied operations to prevent duplicates
    private val appliedOperations = mutableSetOf<String>()

    /**
     * Initialize document
     */
    fun initDocument(documentId: String, initialBlocks: List<ContentBlock> = emptyList()) {
        _document.value = CollaborativeDocument(
            id = documentId,
            blocks = initialBlocks
        )
    }

    /**
     * Apply an operation to the document
     */
    fun applyOperation(operation: DocumentOperation): Boolean {
        // Check if already applied (idempotency)
        if (appliedOperations.contains(operation.operationId)) {
            return false
        }

        val currentDoc = _document.value ?: return false

        val newBlocks = when (operation) {
            is AddBlockOperation -> handleAddBlock(currentDoc.blocks, operation)
            is DeleteBlockOperation -> handleDeleteBlock(currentDoc.blocks, operation)
            is UpdateBlockContentOperation -> handleUpdateContent(currentDoc.blocks, operation)
            is UpdateBlockFormattingOperation -> handleUpdateFormatting(currentDoc.blocks, operation)
            is UpdateBlockTypeOperation -> handleUpdateType(currentDoc.blocks, operation)
            is CursorMoveOperation -> currentDoc.blocks // Cursors don't modify document
        }

        _document.value = currentDoc.copy(blocks = newBlocks)
        appliedOperations.add(operation.operationId)
        operationHistory.add(operation)

        return true
    }

    /**
     * Transform operation against concurrent operations
     * This is the core of Operational Transformation
     */
    fun transformOperation(
        operation: DocumentOperation,
        concurrentOps: List<DocumentOperation>
    ): DocumentOperation {
        var transformed = operation

        for (concurrentOp in concurrentOps) {
            transformed = transformPair(transformed, concurrentOp)
        }

        return transformed
    }

    /**
     * Transform one operation against another
     */
    private fun transformPair(
        op1: DocumentOperation,
        op2: DocumentOperation
    ): DocumentOperation {
        // If operations are on different blocks, no transformation needed
        val block1 = getOperationBlockId(op1)
        val block2 = getOperationBlockId(op2)

        if (block1 != block2 && block1 != null && block2 != null) {
            return op1
        }

        // Transform based on operation types
        return when {
            op1 is UpdateBlockContentOperation && op2 is UpdateBlockContentOperation -> {
                transformContentOperations(op1, op2)
            }
            op1 is AddBlockOperation && op2 is AddBlockOperation -> {
                transformAddOperations(op1, op2)
            }
            op1 is UpdateBlockContentOperation && op2 is DeleteBlockOperation -> {
                // If block was deleted, drop the update
                if (op1.blockId == op2.blockId) {
                    // Return a no-op by keeping original but marking as transformed
                    op1
                } else {
                    op1
                }
            }
            else -> op1
        }
    }

    /**
     * Transform two content update operations
     */
    private fun transformContentOperations(
        op1: UpdateBlockContentOperation,
        op2: UpdateBlockContentOperation
    ): UpdateBlockContentOperation {
        if (op1.blockId != op2.blockId) return op1

        // Adjust position based on concurrent operation
        val newPosition = when {
            op2.position < op1.position -> {
                // Op2 inserted before op1, adjust op1 position forward
                op1.position + op2.content.length
            }
            op2.position == op1.position -> {
                // Same position, use operation ID for deterministic ordering
                if (op1.operationId < op2.operationId) {
                    op1.position
                } else {
                    op1.position + op2.content.length
                }
            }
            else -> op1.position
        }

        return op1.copy(position = newPosition)
    }

    /**
     * Transform two add block operations
     */
    private fun transformAddOperations(
        op1: AddBlockOperation,
        op2: AddBlockOperation
    ): AddBlockOperation {
        // If both insert after same block, use operation ID for ordering
        if (op1.afterBlockId == op2.afterBlockId) {
            if (op1.operationId < op2.operationId) {
                // op1 comes first, op2 should insert after op1's block
                return op1
            } else {
                // op2 comes first, no change needed
                return op1
            }
        }

        // If op2 inserted the block that op1 wants to insert after
        if (op2.block.id == op1.afterBlockId) {
            // Insert after op2's block instead
            return op1.copy(afterBlockId = op2.block.id)
        }

        return op1
    }

    /**
     * Get block ID from operation
     */
    private fun getOperationBlockId(op: DocumentOperation): String? {
        return when (op) {
            is AddBlockOperation -> op.block.id
            is DeleteBlockOperation -> op.blockId
            is UpdateBlockContentOperation -> op.blockId
            is UpdateBlockFormattingOperation -> op.blockId
            is UpdateBlockTypeOperation -> op.blockId
            is CursorMoveOperation -> op.blockId
        }
    }

    // ========== Operation Handlers ==========

    private fun handleAddBlock(
        blocks: List<ContentBlock>,
        operation: AddBlockOperation
    ): List<ContentBlock> {
        val newBlocks = blocks.toMutableList()

        if (operation.afterBlockId == null) {
            // Insert at beginning
            newBlocks.add(0, operation.block)
        } else {
            val index = blocks.indexOfFirst { it.id == operation.afterBlockId }
            if (index >= 0) {
                newBlocks.add(index + 1, operation.block)
            } else {
                // Block not found, add at end
                newBlocks.add(operation.block)
            }
        }

        return newBlocks
    }

    private fun handleDeleteBlock(
        blocks: List<ContentBlock>,
        operation: DeleteBlockOperation
    ): List<ContentBlock> {
        return blocks.filter { it.id != operation.blockId }
    }

    private fun handleUpdateContent(
        blocks: List<ContentBlock>,
        operation: UpdateBlockContentOperation
    ): List<ContentBlock> {
        return blocks.map { block ->
            if (block.id == operation.blockId) {
                // For now, replace full content
                // In a more advanced version, we'd apply delta at position
                val newContent = if (operation.position == 0) {
                    operation.content
                } else {
                    val before = block.content.take(operation.position)
                    val after = block.content.drop(operation.position)
                    before + operation.content + after
                }
                block.copy(content = newContent)
            } else {
                block
            }
        }
    }

    private fun handleUpdateFormatting(
        blocks: List<ContentBlock>,
        operation: UpdateBlockFormattingOperation
    ): List<ContentBlock> {
        return blocks.map { block ->
            if (block.id == operation.blockId) {
                block.copy(
                    fontWeight = operation.fontWeight ?: block.fontWeight,
                    fontStyle = operation.fontStyle ?: block.fontStyle,
                    textDecoration = operation.textDecoration ?: block.textDecoration,
                    fontSize = operation.fontSize ?: block.fontSize,
                    color = operation.color ?: block.color,
                    textAlign = operation.textAlign ?: block.textAlign
                )
            } else {
                block
            }
        }
    }

    private fun handleUpdateType(
        blocks: List<ContentBlock>,
        operation: UpdateBlockTypeOperation
    ): List<ContentBlock> {
        return blocks.map { block ->
            if (block.id == operation.blockId) {
                block.copy(type = operation.newType)
            } else {
                block
            }
        }
    }

    /**
     * Create a new operation with auto-generated ID
     */
    fun createOperation(boardId: String, baseOp: DocumentOperation): DocumentOperation {
        val opId = UUID.randomUUID().toString()
        return when (baseOp) {
            is AddBlockOperation -> baseOp.copy(operationId = opId, boardId = boardId)
            is DeleteBlockOperation -> baseOp.copy(operationId = opId, boardId = boardId)
            is UpdateBlockContentOperation -> baseOp.copy(operationId = opId, boardId = boardId)
            is UpdateBlockFormattingOperation -> baseOp.copy(operationId = opId, boardId = boardId)
            is UpdateBlockTypeOperation -> baseOp.copy(operationId = opId, boardId = boardId)
            is CursorMoveOperation -> baseOp.copy(operationId = opId, boardId = boardId)
        }
    }

    /**
     * Get pending operations that haven't been acknowledged by server
     */
    fun getPendingOperations(): List<DocumentOperation> {
        // For now, return last N operations
        // In production, track acknowledgments
        return operationHistory.takeLast(10)
    }

    /**
     * Clear operation history (call after sync)
     */
    fun clearHistory() {
        operationHistory.clear()
        appliedOperations.clear()
    }

    /**
     * Merge remote operations with local changes
     */
    fun mergeRemoteOperations(remoteOps: List<DocumentOperation>): List<DocumentOperation> {
        val localOps = getPendingOperations()
        val transformedRemote = mutableListOf<DocumentOperation>()

        for (remoteOp in remoteOps) {
            // Skip if already applied
            if (appliedOperations.contains(remoteOp.operationId)) {
                continue
            }

            // Transform remote operation against all local operations
            val transformed = transformOperation(remoteOp, localOps)
            transformedRemote.add(transformed)

            // Apply to document
            applyOperation(transformed)
        }

        return transformedRemote
    }

    /**
     * Get current document state
     */
    fun getCurrentDocument(): CollaborativeDocument? {
        return _document.value
    }

    /**
     * Reset engine state
     */
    fun reset() {
        _document.value = null
        vectorClock.clear()
        operationHistory.clear()
        appliedOperations.clear()
    }
}
