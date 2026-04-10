package com.flowboard.domain

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable

@Serializable
private data class ResendRequest(
    val from: String,
    val to: List<String>,
    val subject: String,
    val html: String
)

object EmailService {

    private val apiKey = System.getenv("RESEND_API_KEY")

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json() }
    }

    suspend fun sendDocumentSharedEmail(
        recipientEmail: String,
        senderName: String,
        documentTitle: String
    ) {
        if (apiKey.isNullOrBlank()) return
        try {
            client.post("https://api.resend.com/emails") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(ResendRequest(
                    from = "FlowBoard <onboarding@resend.dev>",
                    to = listOf(recipientEmail),
                    subject = "$senderName shared a document with you",
                    html = """
                        <div style="font-family:sans-serif;max-width:500px;margin:auto">
                            <h2 style="color:#4F46E5">FlowBoard</h2>
                            <p><strong>$senderName</strong> shared <em>&ldquo;$documentTitle&rdquo;</em> with you.</p>
                            <p>Open FlowBoard to start collaborating.</p>
                        </div>
                    """.trimIndent()
                ))
            }
        } catch (_: Exception) {
            // Email failure must not break the share operation
        }
    }

    suspend fun sendDocumentInviteEmail(
        recipientEmail: String,
        senderName: String,
        documentTitle: String
    ) {
        if (apiKey.isNullOrBlank()) return
        try {
            client.post("https://api.resend.com/emails") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(ResendRequest(
                    from = "FlowBoard <onboarding@resend.dev>",
                    to = listOf(recipientEmail),
                    subject = "$senderName invited you to collaborate on FlowBoard",
                    html = """
                        <div style="font-family:sans-serif;max-width:500px;margin:auto">
                            <h2 style="color:#4F46E5">FlowBoard</h2>
                            <p><strong>$senderName</strong> invited you to collaborate on
                            <em>&ldquo;$documentTitle&rdquo;</em>.</p>
                            <p>To access the document, create a free FlowBoard account using
                            this email address (<strong>$recipientEmail</strong>) and the
                            document will be shared with you automatically.</p>
                            <p style="font-size:12px;color:#666">
                                FlowBoard — Collaborative task &amp; document management
                            </p>
                        </div>
                    """.trimIndent()
                ))
            }
        } catch (_: Exception) {
            // Email failure must not break the invite operation
        }
    }
}
