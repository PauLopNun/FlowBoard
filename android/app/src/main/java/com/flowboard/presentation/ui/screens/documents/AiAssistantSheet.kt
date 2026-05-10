package com.flowboard.presentation.ui.screens.documents

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowboard.presentation.viewmodel.AiEditProposal
import com.flowboard.presentation.viewmodel.AiEditTarget
import com.flowboard.presentation.viewmodel.AiMessage
import com.flowboard.presentation.viewmodel.AiProposedBlock
import com.flowboard.presentation.viewmodel.AiViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantSheet(
    documentContext: String,
    documentStructure: String = documentContext,
    selectedText: String? = null,
    focusedBlockText: String? = null,
    onApplyProposal: (AiEditProposal) -> Unit = {},
    onDismiss: () -> Unit,
    viewModel: AiViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val selectedSnippet = selectedText?.takeIf { it.isNotBlank() }
    val focusedSnippet = focusedBlockText?.takeIf { it.isNotBlank() }

    fun proposeEdit(instruction: String, targetOverride: AiEditTarget? = null) {
        if (instruction.isBlank()) return
        val lowerInstruction = instruction.lowercase()
        val asksForDocument = lowerInstruction.contains("summar") ||
            lowerInstruction.contains("resum") ||
            lowerInstruction.contains("document") ||
            lowerInstruction.contains("doc") ||
            lowerInstruction.contains("documento")

        val target = when {
            targetOverride == AiEditTarget.SELECTION && selectedSnippet != null -> AiEditTarget.SELECTION
            targetOverride == AiEditTarget.BLOCK && focusedSnippet != null -> AiEditTarget.BLOCK
            targetOverride == AiEditTarget.DOCUMENT -> AiEditTarget.DOCUMENT
            asksForDocument -> AiEditTarget.DOCUMENT
            selectedSnippet != null -> AiEditTarget.SELECTION
            else -> AiEditTarget.DOCUMENT
        }

        val targetText = when (target) {
            AiEditTarget.SELECTION -> selectedSnippet.orEmpty()
            AiEditTarget.BLOCK -> focusedSnippet.orEmpty()
            AiEditTarget.DOCUMENT -> documentContext
        }
        val targetLabel = when (target) {
            AiEditTarget.SELECTION -> "the selected text"
            AiEditTarget.BLOCK -> "the current block"
            AiEditTarget.DOCUMENT -> "the document"
        }

        viewModel.proposeEdit(
            instruction = instruction,
            target = target,
            targetLabel = targetLabel,
            targetText = targetText,
            documentContext = documentStructure
        )
    }

    LaunchedEffect(uiState.messages.size, uiState.editProposal) {
        val itemCount = uiState.messages.size + if (uiState.editProposal != null) 1 else 0
        if (itemCount > 0) {
            listState.animateScrollToItem(itemCount - 1)
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        val panelWidth = (maxWidth * 0.58f).coerceIn(320.dp, 520.dp)
        val panelHeight = (maxHeight * 0.62f).coerceIn(420.dp, 680.dp)

        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .padding(end = 16.dp, bottom = 88.dp)
                .width(panelWidth)
                .height(panelHeight)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                AiAssistantHeader(onDismiss = onDismiss)
                HorizontalDivider()

                AiSuggestionRow(
                    hasSelection = selectedSnippet != null,
                    hasFocusedBlock = focusedSnippet != null,
                    onAsk = { prompt -> viewModel.ask(prompt, documentContext) },
                    onPropose = { prompt, target -> proposeEdit(prompt, target) }
                )

                HorizontalDivider()

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.messages) { message ->
                        AiMessageBubble(message)
                    }

                    uiState.editProposal?.let { proposal ->
                        item {
                            AiDiffPreview(
                                proposal = proposal,
                                onApply = {
                                    onApplyProposal(proposal)
                                    viewModel.markEditProposalApplied()
                                },
                                onReject = { viewModel.rejectEditProposal() }
                            )
                        }
                    }

                    if (uiState.isLoading) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Text(
                                            if (uiState.isProposingEdit) "Drafting changes..." else "Thinking...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    uiState.error?.let { error ->
                        item {
                            Text(
                                error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                HorizontalDivider()
                AiInputRow(
                    inputText = inputText,
                    onInputTextChange = { inputText = it },
                    isLoading = uiState.isLoading,
                    onPropose = {
                        proposeEdit(inputText.trim())
                        inputText = ""
                    },
                    onAsk = {
                        viewModel.ask(inputText.trim(), documentContext)
                        inputText = ""
                    }
                )
            }
        }
    }
}

@Composable
private fun AiAssistantHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text("AI Agent", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun AiSuggestionRow(
    hasSelection: Boolean,
    hasFocusedBlock: Boolean,
    onAsk: (String) -> Unit,
    onPropose: (String, AiEditTarget) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SuggestionChip(
            onClick = { onPropose("Summarize this document into a shorter version", AiEditTarget.DOCUMENT) },
            label = { Text("Summarize doc") }
        )
        SuggestionChip(
            onClick = { onPropose("Rewrite this document so it is clearer and better structured", AiEditTarget.DOCUMENT) },
            label = { Text("Rewrite doc") }
        )
        SuggestionChip(
            onClick = { onPropose("Generate a clear outline for this document", AiEditTarget.DOCUMENT) },
            label = { Text("Outline") }
        )
        if (hasSelection) {
            SuggestionChip(
                onClick = { onPropose("Improve the selected text", AiEditTarget.SELECTION) },
                label = { Text("Improve selection") }
            )
            SuggestionChip(
                onClick = { onPropose("Fix grammar and spelling in the selected text", AiEditTarget.SELECTION) },
                label = { Text("Fix selection") }
            )
        }
        if (hasFocusedBlock) {
            SuggestionChip(
                onClick = { onPropose("Improve this block", AiEditTarget.BLOCK) },
                label = { Text("Improve block") }
            )
        }
        SuggestionChip(
            onClick = { onAsk("What is this document about?") },
            label = { Text("Ask only") }
        )
    }
}

@Composable
private fun AiMessageBubble(message: AiMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isUser) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AiDiffPreview(
    proposal: AiEditProposal,
    onApply: () -> Unit,
    onReject: () -> Unit
) {
    val diffLines = remember(proposal.originalText, proposal.proposedText) {
        buildLineDiff(proposal.originalText, proposal.proposedText)
    }
    val proposedBlocks = proposal.proposedBlocks

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.EditNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Proposed changes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        proposal.targetLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            if (!proposedBlocks.isNullOrEmpty()) {
                ProposedBlockSummary(proposedBlocks)
                Spacer(Modifier.height(10.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                diffLines.forEach { line ->
                    DiffLineRow(line)
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onReject) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Reject")
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onApply) {
                    Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Apply")
                }
            }
        }
    }
}

@Composable
private fun ProposedBlockSummary(blocks: List<AiProposedBlock>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        blocks.groupingBy { it.type }.eachCount().forEach { (type, count) ->
            SuggestionChip(
                onClick = {},
                label = {
                    Text(
                        "$count $type",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )
        }
    }
}

@Composable
private fun DiffLineRow(line: AiDiffLine) {
    val background = when (line.type) {
        AiDiffLineType.ADDED -> Color(0xFFDFF7E8)
        AiDiffLineType.REMOVED -> Color(0xFFFFE1E6)
        AiDiffLineType.UNCHANGED -> Color.Transparent
    }
    val textColor = when (line.type) {
        AiDiffLineType.ADDED -> Color(0xFF14532D)
        AiDiffLineType.REMOVED -> Color(0xFF7F1D1D)
        AiDiffLineType.UNCHANGED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val prefix = when (line.type) {
        AiDiffLineType.ADDED -> "+"
        AiDiffLineType.REMOVED -> "-"
        AiDiffLineType.UNCHANGED -> " "
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            prefix,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = textColor,
            modifier = Modifier.width(18.dp)
        )
        Text(
            line.text.ifBlank { " " },
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = textColor
        )
    }
}

@Composable
private fun AiInputRow(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    isLoading: Boolean,
    onPropose: () -> Unit,
    onAsk: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Ask or describe an edit...") },
            maxLines = 4,
            shape = RoundedCornerShape(20.dp)
        )
        FilledIconButton(
            onClick = onPropose,
            enabled = inputText.isNotBlank() && !isLoading,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(Icons.Default.AutoFixHigh, contentDescription = "Propose changes")
        }
        OutlinedIconButton(
            onClick = onAsk,
            enabled = inputText.isNotBlank() && !isLoading,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Ask")
        }
    }
}

private enum class AiDiffLineType {
    UNCHANGED,
    ADDED,
    REMOVED
}

private data class AiDiffLine(
    val type: AiDiffLineType,
    val text: String
)

private fun buildLineDiff(original: String, revised: String): List<AiDiffLine> {
    val oldLines = original.toDiffLines()
    val newLines = revised.toDiffLines()
    if (oldLines == newLines) {
        return oldLines.map { AiDiffLine(AiDiffLineType.UNCHANGED, it) }
    }

    val dp = Array(oldLines.size + 1) { IntArray(newLines.size + 1) }
    for (i in oldLines.indices.reversed()) {
        for (j in newLines.indices.reversed()) {
            dp[i][j] = if (oldLines[i] == newLines[j]) {
                dp[i + 1][j + 1] + 1
            } else {
                maxOf(dp[i + 1][j], dp[i][j + 1])
            }
        }
    }

    val result = mutableListOf<AiDiffLine>()
    var i = 0
    var j = 0
    while (i < oldLines.size && j < newLines.size) {
        when {
            oldLines[i] == newLines[j] -> {
                result += AiDiffLine(AiDiffLineType.UNCHANGED, oldLines[i])
                i++
                j++
            }
            dp[i + 1][j] >= dp[i][j + 1] -> {
                result += AiDiffLine(AiDiffLineType.REMOVED, oldLines[i])
                i++
            }
            else -> {
                result += AiDiffLine(AiDiffLineType.ADDED, newLines[j])
                j++
            }
        }
    }
    while (i < oldLines.size) {
        result += AiDiffLine(AiDiffLineType.REMOVED, oldLines[i])
        i++
    }
    while (j < newLines.size) {
        result += AiDiffLine(AiDiffLineType.ADDED, newLines[j])
        j++
    }

    return result
}

private fun String.toDiffLines(): List<String> {
    val cleaned = trim()
    return if (cleaned.isBlank()) emptyList() else cleaned.lines()
}
