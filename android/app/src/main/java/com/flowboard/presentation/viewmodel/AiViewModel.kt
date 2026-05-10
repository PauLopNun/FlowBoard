package com.flowboard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowboard.data.remote.api.AiApiService
import com.flowboard.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

data class AiMessage(
    val role: String, // "user" | "assistant"
    val text: String
)

enum class AiEditTarget {
    DOCUMENT,
    SELECTION,
    BLOCK
}

data class AiEditProposal(
    val instruction: String,
    val target: AiEditTarget,
    val targetLabel: String,
    val originalText: String,
    val proposedText: String,
    val proposedBlocks: List<AiProposedBlock>? = null
)

@Serializable
data class AiProposedBlock(
    val type: String,
    val content: String
)

@Serializable
private data class AiStructuredEditResponse(
    val text: String? = null,
    val blocks: List<AiProposedBlock> = emptyList()
)

data class AiUiState(
    val messages: List<AiMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isProposingEdit: Boolean = false,
    val editProposal: AiEditProposal? = null,
    val error: String? = null
)

@HiltViewModel
class AiViewModel @Inject constructor(
    private val aiApiService: AiApiService,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiUiState())
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun ask(
        prompt: String,
        documentContext: String? = null,
        visiblePrompt: String = prompt
    ) {
        if (prompt.isBlank()) return

        _uiState.update { it.copy(
            messages = it.messages + AiMessage("user", visiblePrompt),
            isLoading = true,
            error = null
        ) }

        viewModelScope.launch {
            val token = authRepository.getToken() ?: run {
                _uiState.update { it.copy(isLoading = false, error = "Not authenticated") }
                return@launch
            }

            aiApiService.ask(prompt, documentContext, token).fold(
                onSuccess = { reply ->
                    _uiState.update { it.copy(
                        messages = it.messages + AiMessage("assistant", sanitizeEditableReply(reply)),
                        isLoading = false
                    ) }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = err.message ?: "AI request failed"
                    ) }
                }
            )
        }
    }

    fun rewriteSelection(
        instruction: String,
        selectedText: String,
        documentContext: String? = null
    ) {
        if (instruction.isBlank() || selectedText.isBlank()) return

        ask(
            prompt = buildRewritePrompt("the selected text", selectedText, instruction),
            documentContext = documentContext,
            visiblePrompt = instruction
        )
    }

    fun rewriteBlock(
        instruction: String,
        blockText: String,
        documentContext: String? = null
    ) {
        if (instruction.isBlank() || blockText.isBlank()) return

        ask(
            prompt = buildRewritePrompt("this block", blockText, instruction),
            documentContext = documentContext,
            visiblePrompt = instruction
        )
    }

    fun proposeEdit(
        instruction: String,
        target: AiEditTarget,
        targetLabel: String,
        targetText: String,
        documentContext: String? = null
    ) {
        if (instruction.isBlank()) return

        _uiState.update {
            it.copy(
                messages = it.messages + AiMessage("user", instruction),
                isLoading = true,
                isProposingEdit = true,
                editProposal = null,
                error = null
            )
        }

        viewModelScope.launch {
            val token = authRepository.getToken() ?: run {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isProposingEdit = false,
                        error = "Not authenticated"
                    )
                }
                return@launch
            }

            val prompt = buildAgentEditPrompt(
                targetDescription = targetLabel,
                targetText = targetText,
                instruction = instruction,
                target = target,
                documentContext = documentContext
            )

            // The edit prompt already includes the target and format context. Passing
            // documentContext again through the backend system prompt makes models
            // more likely to echo the whole source before adding the requested edit.
            aiApiService.ask(
                prompt = prompt,
                documentContext = null,
                token = token,
                structuredJson = true
            ).fold(
                onSuccess = { reply ->
                    val parsed = parseStructuredEdit(reply)
                    val nestedParsed = parsed?.text
                        ?.takeIf { it.looksLikeStructuredEditJson() }
                        ?.let { parseStructuredEdit(it) }
                    val structuredResponse = nestedParsed ?: parsed
                    val proposedBlocks = structuredResponse?.blocks
                        ?.mapNotNull { block ->
                            val type = normalizeBlockType(block.type)
                            val content = block.content.trim()
                            if (content.isBlank() && type != "divider") null else AiProposedBlock(type, content)
                        }
                        ?.let { blocks ->
                            if (target == AiEditTarget.DOCUMENT && instruction.isSummarizeInstruction()) {
                                dropEchoedSourceBlockPrefix(targetText, blocks)
                            } else {
                                blocks
                            }
                        }
                        ?.takeIf { it.isNotEmpty() }
                    val sanitizedReply = sanitizeEditableReply(reply)
                    val unparsedStructuredJson = structuredResponse == null && sanitizedReply.looksLikeStructuredEditJson()
                    if (unparsedStructuredJson) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isProposingEdit = false,
                                error = "AI returned an edit payload that could not be parsed. Please try again."
                            )
                        }
                        return@fold
                    }
                    val proposedText = removeSourceEcho(
                        target = target,
                        originalText = targetText,
                        proposedText = when {
                            target == AiEditTarget.DOCUMENT && proposedBlocks != null -> proposedBlocks.toProposalText()
                            !structuredResponse?.text.isNullOrBlank() -> structuredResponse?.text.orEmpty().trim()
                            proposedBlocks != null -> proposedBlocks.toProposalText()
                            else -> sanitizedReply
                        }
                    )
                    _uiState.update {
                        it.copy(
                            messages = it.messages + AiMessage("assistant", "I drafted changes for $targetLabel. Review the diff before applying."),
                            isLoading = false,
                            isProposingEdit = false,
                            editProposal = AiEditProposal(
                                instruction = instruction,
                                target = target,
                                targetLabel = targetLabel,
                                originalText = targetText,
                                proposedText = proposedText,
                                proposedBlocks = proposedBlocks
                            )
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isProposingEdit = false,
                            error = err.message ?: "AI request failed"
                        )
                    }
                }
            )
        }
    }

    fun rejectEditProposal() {
        _uiState.update { it.copy(editProposal = null) }
    }

    fun markEditProposalApplied() {
        _uiState.update {
            it.copy(
                editProposal = null,
                messages = it.messages + AiMessage("assistant", "Changes applied to the document.")
            )
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
    fun clearMessages() = _uiState.update { it.copy(messages = emptyList()) }

    private fun sanitizeEditableReply(reply: String): String {
        val trimmed = reply.trim()
        if (!trimmed.startsWith("```") || !trimmed.endsWith("```")) return trimmed

        val lines = trimmed.lines()
        if (lines.size < 2) return trimmed
        return lines.drop(1).dropLast(1).joinToString("\n").trim()
    }

    private fun buildRewritePrompt(
        targetDescription: String,
        targetText: String,
        instruction: String
    ): String = """
        The user is editing $targetDescription in a FlowBoard document:
        ${"\"\"\""}
        ${targetText.take(4000)}
        ${"\"\"\""}

        Rewrite only $targetDescription according to this instruction:
        $instruction

        Return only the replacement text. Do not include explanations, prefaces, bullets unless they belong in the replacement, or markdown code fences.
    """.trimIndent()

    private fun buildAgentEditPrompt(
        targetDescription: String,
        targetText: String,
        instruction: String,
        target: AiEditTarget,
        documentContext: String?
    ): String = """
        You are an editing agent inside FlowBoard.

        The user wants to change $targetDescription according to this instruction:
        $instruction

        ${documentContext?.takeIf { it.isNotBlank() }?.let {
            "Current document block structure for preserving format:\n\"\"\"\n${it.take(8000)}\n\"\"\"\n"
        }.orEmpty()}

        Current $targetDescription:
        ${"\"\"\""}
        ${targetText.take(8000)}
        ${"\"\"\""}

        Return only valid JSON. Do not wrap it in markdown fences.
        ${if (target == AiEditTarget.DOCUMENT) {
            """
            Use exactly this shape:
            {"blocks":[{"type":"h1","content":"Title"},{"type":"p","content":"Paragraph text"}]}

            Allowed block types: h1, h2, h3, p, bullet, numbered, todo, quote, callout, code, divider.
            Preserve the document's useful structure, but do not include source/template filler unless it belongs in the revised result.
            """.trimIndent()
        } else {
            """
            Use exactly this shape:
            {"text":"replacement text"}
            """.trimIndent()
        }}

        Return the full revised $targetDescription, not a commentary about it.
        If the user asks to summarize, compress the current text into the revised text.
        If the user asks to write or create a document about a topic, generate the document content.
        If summarizing, return only the summary/revised content, not the original content followed by a summary.
        Do not include explanations, prefaces, confirmation messages, or markdown code fences.
    """.trimIndent()

    private fun parseStructuredEdit(reply: String): AiStructuredEditResponse? {
        val payload = extractJsonPayload(reply) ?: return null
        val typed = runCatching {
            json.decodeFromString(AiStructuredEditResponse.serializer(), payload)
        }.getOrNull()
            ?.takeIf { !it.text.isNullOrBlank() || it.blocks.isNotEmpty() }
        return typed ?: parseFlexibleStructuredEdit(payload)
    }

    private fun extractJsonPayload(reply: String): String? {
        val cleaned = sanitizeEditableReply(reply)
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return cleaned.substring(start, end + 1)
    }

    private fun parseFlexibleStructuredEdit(payload: String): AiStructuredEditResponse? = runCatching {
        val root = json.parseToJsonElement(payload) as? JsonObject ?: return@runCatching null
        val text = root.stringValue("text")?.trim()?.takeIf { it.isNotBlank() }
        val blocks = (root["blocks"] as? JsonArray)
            ?.mapNotNull { blockElement -> blockElement.toProposedBlockOrNull() }
            .orEmpty()

        if (text == null && blocks.isEmpty()) null else AiStructuredEditResponse(text = text, blocks = blocks)
    }.getOrNull()

    private fun JsonElement.toProposedBlockOrNull(): AiProposedBlock? {
        val block = this as? JsonObject ?: return null
        val type = block.stringValue("type")
            ?: block.stringValue("blockType")
            ?: block.stringValue("kind")
            ?: "p"
        val content = block.stringValue("content")
            ?: block.stringValue("text")
            ?: block.stringValue("value")
            ?: ""
        return AiProposedBlock(type = type, content = content)
    }

    private fun JsonObject.stringValue(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull

    private fun normalizeBlockType(type: String): String = when (type.trim().lowercase()) {
        "h1", "title", "heading1", "heading_1" -> "h1"
        "h2", "heading2", "heading_2" -> "h2"
        "h3", "heading3", "heading_3" -> "h3"
        "p", "paragraph", "text", "normal", "body" -> "p"
        "bullet", "bulleted", "ul", "list", "list_item" -> "bullet"
        "numbered", "ordered", "ol", "numbered_list" -> "numbered"
        "todo", "task", "checklist", "checkbox" -> "todo"
        "quote", "blockquote" -> "quote"
        "callout", "code", "divider" -> type.trim().lowercase()
        else -> "p"
    }

    private fun String.looksLikeStructuredEditJson(): Boolean {
        val value = trim()
        return value.startsWith("{") &&
            value.endsWith("}") &&
            (value.contains("\"blocks\"") || value.contains("\"text\""))
    }

    private fun List<AiProposedBlock>.toProposalText(): String =
        joinToString("\n\n") { block ->
            when (block.type) {
                "h1" -> "# ${block.content}"
                "h2" -> "## ${block.content}"
                "h3" -> "### ${block.content}"
                "bullet" -> "- ${block.content}"
                "numbered" -> "1. ${block.content}"
                "quote" -> "> ${block.content}"
                "divider" -> "---"
                else -> block.content
            }
        }.trim()

    private fun removeSourceEcho(
        target: AiEditTarget,
        originalText: String,
        proposedText: String
    ): String {
        if (target != AiEditTarget.DOCUMENT) return proposedText.trim()

        val original = originalText.trim()
        val proposed = proposedText.trim()
        if (original.length < 80 || proposed.length <= original.length) return proposed

        return if (proposed.startsWith(original)) {
            proposed.removePrefix(original).trimStart('\n', ' ', ':', '-').trim()
        } else {
            proposed
        }
    }

    private fun String.isSummarizeInstruction(): Boolean {
        val value = lowercase()
        return value.contains("summar") || value.contains("resum")
    }

    private fun dropEchoedSourceBlockPrefix(
        originalText: String,
        proposedBlocks: List<AiProposedBlock>
    ): List<AiProposedBlock> {
        val originalSegments = originalText
            .lines()
            .map { it.normalizedForEchoCompare() }
            .filter { it.isNotBlank() }
        if (originalSegments.size < 3 || proposedBlocks.size <= originalSegments.size) return proposedBlocks

        var matched = 0
        while (
            matched < originalSegments.size &&
            matched < proposedBlocks.size &&
            proposedBlocks[matched].content.normalizedForEchoCompare() == originalSegments[matched]
        ) {
            matched++
        }

        val enoughPrefixRepeated = matched >= minOf(3, originalSegments.size)
        return if (enoughPrefixRepeated && proposedBlocks.size > matched) {
            proposedBlocks.drop(matched)
        } else {
            proposedBlocks
        }
    }

    private fun String.normalizedForEchoCompare(): String =
        trim()
            .removePrefix("#")
            .removePrefix("#")
            .removePrefix("#")
            .trim()
            .replace(Regex("\\s+"), " ")
            .lowercase()
}
