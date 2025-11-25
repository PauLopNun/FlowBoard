package com.flowboard.domain.model

data class CollaborativeDocument(
    val id: String,
    val blocks: List<ContentBlock>
)
