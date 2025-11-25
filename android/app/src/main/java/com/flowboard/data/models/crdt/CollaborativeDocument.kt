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
