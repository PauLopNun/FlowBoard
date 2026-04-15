package com.flowboard.domain.model

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
    val isChecked: Boolean = false,
    val detail: String = ""
)
