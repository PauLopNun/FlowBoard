package com.flowboard.presentation.ui.screens.documents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flowboard.data.local.entities.DocumentEntity
import com.flowboard.presentation.viewmodel.DocumentViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun SearchScreen(
    onDocumentClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: DocumentViewModel = hiltViewModel()
) {
    val documentListState by viewModel.documentListState.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val allDocs = (documentListState.ownedDocuments + documentListState.sharedWithMe)
        .distinctBy { it.id }
        .filter { !it.isDeleted }
        .sortedByDescending { it.updatedAt }

    val results = remember(query, allDocs) {
        if (query.isBlank()) emptyList()
        else {
            val q = query.trim().lowercase()
            allDocs.filter { doc ->
                doc.title.lowercase().contains(q) || searchInContent(doc.content, q)
            }.take(30)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchAllDocuments()
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search pages\u2026") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (query.isNotBlank()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Clear, "Clear")
                                }
                            }
                        },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (query.isBlank()) {
            // Show recent docs when no query
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                if (allDocs.isNotEmpty()) {
                    Text("Recent", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    allDocs.take(8).forEach { doc ->
                        SearchResultItem(
                            doc = doc,
                            highlight = "",
                            onClick = { onDocumentClick(doc.id) }
                        )
                    }
                }
            }
        } else if (results.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SearchOff, null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(12.dp))
                    Text("No results for \"$query\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Text("${results.size} result${if (results.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp))
                }
                items(results, key = { it.id }) { doc ->
                    SearchResultItem(doc = doc, highlight = query, onClick = { onDocumentClick(doc.id) })
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(doc: DocumentEntity, highlight: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(doc.title.ifBlank { "Untitled" },
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            val snippet = getContentSnippet(doc.content, highlight)
            if (snippet.isNotBlank()) {
                Text(snippet, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        leadingContent = {
            Icon(Icons.Default.Article, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp))
        },
        trailingContent = {
            Text(doc.updatedAt.take(10),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}

private fun searchInContent(contentJson: String, query: String): Boolean {
    if (contentJson.isBlank()) return false
    return try {
        val arr = Json { ignoreUnknownKeys = true }.parseToJsonElement(contentJson).jsonArray
        arr.any { block ->
            block.jsonObject["content"]?.jsonPrimitive?.content?.lowercase()?.contains(query) == true
        }
    } catch (_: Exception) {
        contentJson.lowercase().contains(query)
    }
}

private fun getContentSnippet(contentJson: String, query: String): String {
    if (contentJson.isBlank()) return ""
    return try {
        val arr = Json { ignoreUnknownKeys = true }.parseToJsonElement(contentJson).jsonArray
        val texts = arr.mapNotNull { block ->
            block.jsonObject["content"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
        }
        val matching = texts.firstOrNull { it.lowercase().contains(query.lowercase()) } ?: texts.firstOrNull() ?: ""
        if (matching.length > 120) matching.take(120) + "\u2026" else matching
    } catch (_: Exception) {
        ""
    }
}
