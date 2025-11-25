package com.flowboard.data.models.crdt

import kotlinx.serialization.Serializable

/**
 * Represents a generic operation on a collaborative document.
 * All operations should be identifiable and idempotent.
 */
@Serializable
sealed interface DocumentOperation {
    val operationId: String
    val boardId: String
}

/**
 * Inserts a new content block at a specific index.
 *
 * @param block The block to be inserted.
 * @param afterBlockId The ID of the block after which to insert the new block. If null, insert at the beginning.
 */
@Serializable
data class AddBlockOperation(
    override val operationId: String,
    override val boardId: String,
    val block: ContentBlock,
    val afterBlockId: String? = null
) : DocumentOperation

/**
 * Removes a content block from the document.
 *
 * @param blockId The ID of the block to be removed.
 */
@Serializable
data class DeleteBlockOperation(
    override val operationId: String,
    override val boardId: String,
    val blockId: String
) : DocumentOperation

/**
 * Updates the content of a specific block.
 * This operation will carry a delta representing the change.
 *
 * @param blockId The ID of the block to update.
 * @param content The new content of the block.
 * @param position The position to apply the update.
 */
@Serializable
data class UpdateBlockContentOperation(
    override val operationId: String,
    override val boardId: String,
    val blockId: String,
    val content: String, // For now, this is the full content. Later, it will be a delta.
    val position: Int
) : DocumentOperation

/**
 * Updates the formatting of a specific block.
 *
 * @param blockId The ID of the block to update.
 * @param fontWeight The new font weight.
 * @param fontStyle The new font style.
 * @param textDecoration The new text decoration.
 * @param fontSize The new font size.
 * @param color The new color.
 * @param textAlign The new text alignment.
 */
@Serializable
data class UpdateBlockFormattingOperation(
    override val operationId: String,
    override val boardId: String,
    val blockId: String,
    val fontWeight: String? = null,
    val fontStyle: String? = null,
    val textDecoration: String? = null,
    val fontSize: Int? = null,
    val color: String? = null,
    val textAlign: String? = null
) : DocumentOperation

/**
 * Updates the type of a specific block.
 *
 * @param blockId The ID of the block to update.
 * @param newType The new type of the block.
 */
@Serializable
data class UpdateBlockTypeOperation(
    override val operationId: String,
    override val boardId: String,
    val blockId: String,
    val newType: String
) : DocumentOperation

/**
 * Represents a cursor position change for a user.
 *
 * @param userId The ID of the user whose cursor is moving.
 * @param blockId The ID of the block where the cursor is located.
 * @param position The cursor's position within the block.
 * @param selectionEnd The end of the selection, if any.
 */
@Serializable
data class CursorMoveOperation(
    override val operationId: String,
    override val boardId: String,
    val userId: String,
    val blockId: String?,
    val position: Int,
    val selectionEnd: Int? = null
) : DocumentOperation
