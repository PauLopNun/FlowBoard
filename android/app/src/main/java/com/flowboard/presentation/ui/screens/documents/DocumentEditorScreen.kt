package com.flowboard.presentation.ui.screens.documents

import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flowboard.presentation.ui.components.ComposeRichTextEditor
import com.flowboard.presentation.ui.components.UserInvitationDialog
import com.flowboard.presentation.ui.components.SuggestedUser
import com.flowboard.presentation.ui.components.CollaboratorInfo
import com.flowboard.presentation.ui.components.PermissionLevel
import com.flowboard.presentation.viewmodel.DocumentEditorViewModel
import com.flowboard.data.remote.dto.UserPresenceInfo
import kotlinx.datetime.toLocalDateTime
import java.io.File
import java.io.FileOutputStream

/**
 * Pantalla de edición de documentos con el nuevo editor
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentEditorScreen(
    documentId: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DocumentEditorViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val documentState by viewModel.currentDocument.collectAsStateWithLifecycle()
    var showTitleDialog by remember { mutableStateOf(documentId == null) }
    var showExportMenu by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var documentTitle by remember { mutableStateOf("") }
    var documentContent by remember { mutableStateOf("") }

    // Mock data para usuarios activos y sugeridos
    val activeUsers = remember {
        listOf(
            UserPresenceInfo(
                userId = "user1",
                username = "johndoe",
                fullName = "John Doe",
                profileImageUrl = null,
                isOnline = true,
                lastActivity = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
            ),
            UserPresenceInfo(
                userId = "user2",
                username = "janesmith",
                fullName = "Jane Smith",
                profileImageUrl = null,
                isOnline = true,
                lastActivity = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
            )
        )
    }

    val suggestedUsers = remember {
        listOf(
            SuggestedUser("user3", "Alice Johnson", "alice@example.com"),
            SuggestedUser("user4", "Bob Wilson", "bob@example.com"),
            SuggestedUser("user5", "Charlie Brown", "charlie@example.com")
        )
    }

    val currentCollaborators = remember {
        listOf(
            CollaboratorInfo("user1", "John Doe", true, PermissionLevel.EDIT),
            CollaboratorInfo("user2", "Jane Smith", true, PermissionLevel.EDIT)
        )
    }

    // Cargar documento si existe
    LaunchedEffect(documentId) {
        if (documentId != null) {
            viewModel.loadDocument(documentId)
        }
    }

    // Actualizar título y contenido cuando cargue el documento
    LaunchedEffect(documentState) {
        documentState?.let { doc ->
            documentTitle = doc.title
            documentContent = doc.content
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = documentTitle.ifEmpty { "New Document" },
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Renombrar
                    IconButton(onClick = { showTitleDialog = true }) {
                        Icon(Icons.Default.Edit, "Rename")
                    }

                    // Exportar
                    IconButton(onClick = { showExportMenu = true }) {
                        Icon(Icons.Default.Share, "Export")
                    }

                    DropdownMenu(
                        expanded = showExportMenu,
                        onDismissRequest = { showExportMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export to PDF") },
                            onClick = {
                                showExportMenu = false
                                documentState?.let { doc ->
                                    exportToPDF(
                                        context = context,
                                        title = doc.title,
                                        content = doc.content
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.PictureAsPdf, null)
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = modifier.padding(padding)) {
            if (documentState != null || documentId == null) {
                ComposeRichTextEditor(
                    initialHtml = documentContent,
                    onContentChange = { htmlContent ->
                        documentContent = htmlContent
                        // Auto-save cada cambio
                        if (documentTitle.isNotEmpty()) {
                            val docId = documentId ?: java.util.UUID.randomUUID().toString()
                            viewModel.saveDocument(
                                id = docId,
                                title = documentTitle,
                                content = htmlContent
                            )
                        }
                    },
                    activeUsers = activeUsers,
                    onInviteUser = {
                        showInviteDialog = true
                    },
                    modifier = Modifier.fillMaxSize(),
                    placeholder = "Start writing something amazing..."
                )
            } else {
                // Loading
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        // Diálogo para título
        if (showTitleDialog) {
            var tempTitle by remember { mutableStateOf(documentTitle) }

            AlertDialog(
                onDismissRequest = {
                    if (documentId != null) {
                        showTitleDialog = false
                    }
                },
                title = { Text("Document Title") },
                text = {
                    OutlinedTextField(
                        value = tempTitle,
                        onValueChange = { tempTitle = it },
                        label = { Text("Title") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (tempTitle.isNotBlank()) {
                                documentTitle = tempTitle
                                showTitleDialog = false
                            }
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    if (documentId != null) {
                        TextButton(onClick = { showTitleDialog = false }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }

        // Diálogo de invitación
        if (showInviteDialog) {
            UserInvitationDialog(
                onDismiss = { showInviteDialog = false },
                onInviteUser = { userIdOrEmail, permission ->
                    // TODO: Implementar lógica de invitación real
                    Toast.makeText(
                        context,
                        "Invited $userIdOrEmail with ${permission.displayName} permission",
                        Toast.LENGTH_SHORT
                    ).show()
                    showInviteDialog = false
                },
                suggestedUsers = suggestedUsers,
                currentCollaborators = currentCollaborators
            )
        }
    }
}

/**
 * Exportar documento a PDF
 */
private fun exportToPDF(
    context: Context,
    title: String,
    content: String
) {
    try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        val paint = android.graphics.Paint()
        paint.textSize = 12f
        paint.color = android.graphics.Color.BLACK

        // Parse HTML content y extraer texto plano
        val text = android.text.Html.fromHtml(content, android.text.Html.FROM_HTML_MODE_LEGACY).toString()

        var yPosition = 50f
        val lineHeight = 20f
        val maxWidth = 500f

        // Escribir título
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText(title, 50f, yPosition, paint)
        yPosition += 40f

        // Escribir contenido
        paint.textSize = 12f
        paint.isFakeBoldText = false

        val lines = text.split("\n")
        for (line in lines) {
            if (yPosition > 800) break // No más espacio en la página

            // Dividir líneas largas
            val words = line.split(" ")
            var currentLine = ""

            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                val testWidth = paint.measureText(testLine)

                if (testWidth > maxWidth && currentLine.isNotEmpty()) {
                    canvas.drawText(currentLine, 50f, yPosition, paint)
                    yPosition += lineHeight
                    currentLine = word
                } else {
                    currentLine = testLine
                }
            }

            if (currentLine.isNotEmpty()) {
                canvas.drawText(currentLine, 50f, yPosition, paint)
                yPosition += lineHeight
            }
        }

        pdfDocument.finishPage(page)

        // Guardar PDF
        val fileName = "${title.replace(" ", "_")}.pdf"
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)

        FileOutputStream(file).use { outputStream ->
            pdfDocument.writeTo(outputStream)
        }

        pdfDocument.close()

        // Mostrar toast
        Toast.makeText(context, "PDF saved to Downloads: $fileName", Toast.LENGTH_LONG).show()

        // Abrir PDF
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        context.startActivity(Intent.createChooser(intent, "Open PDF"))

    } catch (e: Exception) {
        Toast.makeText(context, "Error exporting PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        e.printStackTrace()
    }
}
