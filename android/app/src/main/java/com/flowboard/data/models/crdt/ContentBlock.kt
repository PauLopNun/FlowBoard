package com.flowboard.data.models.crdt

import kotlinx.serialization.Serializable

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
    val type: String,   // h1, h2, h3, p, code, bullet, numbered, todo, quote, callout, divider
    val content: String,
    val fontWeight: String = "normal",
    val fontStyle: String = "normal",
    val textDecoration: String = "none",
    val fontSize: Int = 16,
    val color: String = "#000000",
    val textAlign: String = "start",
    val isChecked: Boolean = false,  // used by "todo" type
    val detail: String = "",         // used by "toggle" type (expandable body content)
    val backgroundColor: String = "" // block highlight background color
)
