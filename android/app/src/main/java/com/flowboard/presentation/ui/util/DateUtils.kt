package com.flowboard.presentation.ui.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Converts an ISO-8601 timestamp string to a human-readable relative label.
 * Examples: "Just now", "5 min ago", "Yesterday", "3 May", "12/03/25"
 */
fun formatRelativeDate(dateStr: String): String {
    if (dateStr.isBlank()) return ""
    return try {
        val normalized = dateStr.replace(" ", "T").trimEnd('Z').let {
            if ('.' in it) it.substringBefore('.') else it
        } + "Z"

        val instant = Instant.parse(normalized.replace("Z", "+00:00")
            .let { if (!it.contains("T")) "${it.substringBefore("+")}T00:00:00+00:00" else it })

        val zone = ZoneId.systemDefault()
        val date = instant.atZone(zone).toLocalDate()
        val now = LocalDate.now(zone)
        val minutesAgo = ChronoUnit.MINUTES.between(instant, Instant.now())
        val daysAgo = ChronoUnit.DAYS.between(date, now)

        when {
            minutesAgo < 2 -> "Just now"
            minutesAgo < 60 -> "${minutesAgo}m ago"
            minutesAgo < 1440 -> "${minutesAgo / 60}h ago"
            daysAgo == 1L -> "Yesterday"
            daysAgo < 7 -> "${daysAgo}d ago"
            date.year == now.year -> date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
            else -> date.format(DateTimeFormatter.ofPattern("d MMM yy", Locale.getDefault()))
        }
    } catch (_: Exception) {
        // Fallback: just take the date part
        try { dateStr.take(10).replace("-", "/").let {
            val p = it.split("/")
            if (p.size == 3) "${p[2]}/${p[1]}/${p[0].takeLast(2)}" else it
        } } catch (_: Exception) { "" }
    }
}
