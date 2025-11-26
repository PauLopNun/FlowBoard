package com.flowboard.data.models.crdt

import kotlinx.serialization.Serializable

/**
 * Represents a collaborative document, which is composed of a list of content blocks.
 *
 * @param id The unique identifier for the document.
 * @param blocks The list of content blocks that make up the document.
 */
@Serializable
data class CollaborativeDocument(
    val id: String,
    val blocks: List<ContentBlock>
)

/**
 * Represents a single block of content within a collaborative document.
 *
 * @param id The unique identifier for the block.
 * @param type The type of the block (e.g., "h1", "p", "code").
 * @param content The content of the block.
 */
@Serializable
data class ContentBlock(
    val id: String,
    val type: String,
    val content: String,
    val fontWeight: String = "normal",
    val fontStyle: String = "normal",
    val textDecoration: String = "none",
    val fontSize: Int = 16,
    val color: String = "#000000",
    val textAlign: String = "start"
)
