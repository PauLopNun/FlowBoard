package com.flowboard.presentation.ui.screens.documents

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.flowboard.data.models.crdt.ContentBlock
import java.io.File
import java.io.FileOutputStream

fun exportToMarkdown(blocks: List<ContentBlock>, title: String, context: Context) {
    val sb = StringBuilder()
    blocks.forEach { block ->
        val content = block.content
        when (block.type) {
            "h1" -> sb.appendLine("# $content\n")
            "h2" -> sb.appendLine("## $content\n")
            "h3" -> sb.appendLine("### $content\n")
            "bullet" -> sb.appendLine("- $content")
            "numbered" -> sb.appendLine("1. $content")
            "code" -> sb.appendLine("```\n$content\n```\n")
            else -> {
                if (content.isBlank()) return@forEach
                var text = content
                if (block.fontWeight == "bold") text = "**$text**"
                if (block.fontStyle == "italic") text = "_${text}_"
                sb.appendLine("$text\n")
            }
        }
    }

    val safeTitle = title.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(50).ifEmpty { "document" }
    val file = File(context.cacheDir, "$safeTitle.md")
    file.writeText(sb.toString())
    shareFile(file, "text/markdown", "Export as Markdown", context)
}

fun exportToPdf(blocks: List<ContentBlock>, title: String, context: Context) {
    val pageWidth = 595
    val pageHeight = 842
    val leftMargin = 50f
    val topMargin = 60f
    val bottomMargin = 60f
    val usableWidth = pageWidth - leftMargin * 2

    val pdfDoc = PdfDocument()
    var pageNumber = 1
    var page = pdfDoc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
    var canvas = page.canvas
    var y = topMargin

    fun newPage() {
        pdfDoc.finishPage(page)
        pageNumber++
        page = pdfDoc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        canvas = page.canvas
        y = topMargin
    }

    val paint = Paint().apply { isAntiAlias = true; color = android.graphics.Color.BLACK }

    fun renderText(text: String) {
        val words = text.split(" ")
        var line = ""
        for (word in words) {
            val test = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(test) <= usableWidth) {
                line = test
            } else {
                if (y > pageHeight - bottomMargin) newPage()
                canvas.drawText(line, leftMargin, y, paint)
                y += paint.textSize * 1.6f
                line = word
            }
        }
        if (line.isNotEmpty()) {
            if (y > pageHeight - bottomMargin) newPage()
            canvas.drawText(line, leftMargin, y, paint)
            y += paint.textSize * 1.6f
        }
        y += paint.textSize * 0.4f
    }

    blocks.forEach { block ->
        val content = block.content.ifBlank { return@forEach }
        paint.textSize = when (block.type) {
            "h1" -> 22f; "h2" -> 18f; "h3" -> 15f; "code" -> 11f; else -> 13f
        }
        paint.typeface = when {
            block.type == "code" -> Typeface.MONOSPACE
            block.type.startsWith("h") || block.fontWeight == "bold" -> Typeface.DEFAULT_BOLD
            block.fontStyle == "italic" -> Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            else -> Typeface.DEFAULT
        }
        val prefix = when (block.type) { "bullet" -> "•  "; "numbered" -> "1. "; else -> "" }
        renderText("$prefix$content")
    }

    pdfDoc.finishPage(page)

    val safeTitle = title.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(50).ifEmpty { "document" }
    val file = File(context.cacheDir, "$safeTitle.pdf")
    FileOutputStream(file).use { pdfDoc.writeTo(it) }
    pdfDoc.close()
    shareFile(file, "application/pdf", "Export as PDF", context)
}

private fun shareFile(file: File, mimeType: String, chooserTitle: String, context: Context) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}
