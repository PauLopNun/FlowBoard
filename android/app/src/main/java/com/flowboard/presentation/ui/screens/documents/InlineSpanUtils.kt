package com.flowboard.presentation.ui.screens.documents

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.json.JSONArray
import org.json.JSONObject

/**
 * A single inline formatting span within a block's text.
 * Ranges are [start, end) — same convention as TextRange.
 */
data class InlineSpan(
    val start: Int,
    val end: Int,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val color: String = ""   // hex, e.g. "#EF4444", or "" for default
)

// ─── Serialisation ───────────────────────────────────────────────────────────

fun parseSpans(json: String): List<InlineSpan> {
    if (json.isBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            InlineSpan(
                start = o.optInt("s", 0),
                end   = o.optInt("e", 0),
                bold      = o.optBoolean("b", false),
                italic    = o.optBoolean("i", false),
                underline = o.optBoolean("u", false),
                color     = o.optString("c", "")
            )
        }.filter { it.start < it.end }
    } catch (_: Exception) { emptyList() }
}

fun serializeSpans(spans: List<InlineSpan>): String {
    val arr = JSONArray()
    spans.filter { it.start < it.end }
         .sortedBy { it.start }
         .forEach { sp ->
             arr.put(JSONObject().apply {
                 put("s", sp.start)
                 put("e", sp.end)
                 if (sp.bold)      put("b", true)
                 if (sp.italic)    put("i", true)
                 if (sp.underline) put("u", true)
                 if (sp.color.isNotEmpty()) put("c", sp.color)
             })
         }
    return if (arr.length() == 0) "" else arr.toString()
}

// ─── AnnotatedString builder ─────────────────────────────────────────────────

fun buildAnnotatedString(text: String, spansJson: String): AnnotatedString {
    val spans = parseSpans(spansJson)
    if (spans.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text)
        for (sp in spans) {
            val s = sp.start.coerceIn(0, text.length)
            val e = sp.end.coerceIn(s, text.length)
            if (s >= e) continue
            addStyle(
                SpanStyle(
                    fontWeight = if (sp.bold) FontWeight.Bold else null,
                    fontStyle  = if (sp.italic) FontStyle.Italic else null,
                    textDecoration = if (sp.underline) TextDecoration.Underline else null,
                    color = if (sp.color.isNotEmpty()) try {
                        Color(android.graphics.Color.parseColor(
                            if (sp.color.startsWith("#")) sp.color else "#${sp.color}"
                        ))
                    } catch (_: Exception) { Color.Unspecified }
                    else Color.Unspecified
                ),
                start = s, end = e
            )
        }
    }
}

// ─── Toggle helpers ──────────────────────────────────────────────────────────

/** Returns true if every character in [rangeStart, rangeEnd) has the given style. */
private fun isFullyCovered(spans: List<InlineSpan>, rangeStart: Int, rangeEnd: Int,
                           check: (InlineSpan) -> Boolean): Boolean {
    if (rangeStart >= rangeEnd) return false
    for (pos in rangeStart until rangeEnd) {
        if (spans.none { check(it) && it.start <= pos && it.end > pos }) return false
    }
    return true
}

/** Remove the given style from [rangeStart, rangeEnd) by splitting existing spans. */
private fun removeStyleFromRange(
    spans: List<InlineSpan>, rangeStart: Int, rangeEnd: Int,
    makeClear: (InlineSpan) -> InlineSpan   // returns span with the style cleared
): List<InlineSpan> {
    val result = mutableListOf<InlineSpan>()
    for (sp in spans) {
        if (sp.end <= rangeStart || sp.start >= rangeEnd) {
            result.add(sp)                      // no overlap
        } else {
            if (sp.start < rangeStart) result.add(sp.copy(end = rangeStart))   // before
            // Inside the range with style cleared
            val innerStart = maxOf(sp.start, rangeStart)
            val innerEnd   = minOf(sp.end,   rangeEnd)
            val cleared = makeClear(sp).copy(start = innerStart, end = innerEnd)
            // Only keep if it still has some style
            if (cleared.bold || cleared.italic || cleared.underline || cleared.color.isNotEmpty())
                result.add(cleared)
            if (sp.end > rangeEnd) result.add(sp.copy(start = rangeEnd))        // after
        }
    }
    return result.filter { it.start < it.end }
}

fun toggleBold(spansJson: String, rangeStart: Int, rangeEnd: Int): String {
    val spans = parseSpans(spansJson)
    val fully = isFullyCovered(spans, rangeStart, rangeEnd) { it.bold }
    val without = removeStyleFromRange(spans, rangeStart, rangeEnd) { it.copy(bold = false) }
    val result = if (fully) without
    else without + InlineSpan(rangeStart, rangeEnd, bold = true)
    return serializeSpans(result)
}

fun toggleItalic(spansJson: String, rangeStart: Int, rangeEnd: Int): String {
    val spans = parseSpans(spansJson)
    val fully = isFullyCovered(spans, rangeStart, rangeEnd) { it.italic }
    val without = removeStyleFromRange(spans, rangeStart, rangeEnd) { it.copy(italic = false) }
    val result = if (fully) without
    else without + InlineSpan(rangeStart, rangeEnd, italic = true)
    return serializeSpans(result)
}

fun toggleUnderline(spansJson: String, rangeStart: Int, rangeEnd: Int): String {
    val spans = parseSpans(spansJson)
    val fully = isFullyCovered(spans, rangeStart, rangeEnd) { it.underline }
    val without = removeStyleFromRange(spans, rangeStart, rangeEnd) { it.copy(underline = false) }
    val result = if (fully) without
    else without + InlineSpan(rangeStart, rangeEnd, underline = true)
    return serializeSpans(result)
}

fun setInlineColor(spansJson: String, rangeStart: Int, rangeEnd: Int, color: String): String {
    val spans = parseSpans(spansJson)
    val without = removeStyleFromRange(spans, rangeStart, rangeEnd) { it.copy(color = "") }
    val result = if (color.isEmpty()) without
    else without + InlineSpan(rangeStart, rangeEnd, color = color)
    return serializeSpans(result)
}

// ─── Span adjustment on text edit ────────────────────────────────────────────

/**
 * Adjusts span positions after a text edit.
 * [changeStart] = index where the change begins
 * [deletedLen]  = number of chars removed
 * [insertedLen] = number of chars inserted
 */
fun adjustSpans(spansJson: String, changeStart: Int, deletedLen: Int, insertedLen: Int): String {
    if (spansJson.isBlank()) return spansJson
    val spans = parseSpans(spansJson)
    val deleteEnd = changeStart + deletedLen
    val delta = insertedLen - deletedLen

    val adjusted = spans.mapNotNull { sp ->
        val newStart = when {
            sp.start >= deleteEnd -> sp.start + delta
            sp.start >= changeStart -> changeStart + insertedLen
            else -> sp.start
        }
        val newEnd = when {
            sp.end >= deleteEnd -> sp.end + delta
            sp.end > changeStart -> changeStart + insertedLen
            else -> sp.end
        }
        if (newStart < newEnd) sp.copy(start = newStart, end = newEnd) else null
    }
    return serializeSpans(adjusted)
}
