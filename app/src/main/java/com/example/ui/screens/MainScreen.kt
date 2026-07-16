package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.db.ChatEntity
import com.example.data.db.MessageEntity
import com.example.ui.components.MarkdownText
import com.example.ui.components.OrionConstellation
import com.example.ui.components.copyToClipboard
import com.example.ui.theme.*
import com.example.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

/**
 * Parses simple inline search term matching and highlights them
 */
fun highlightSearchTerm(text: String, query: String, defaultColor: Color): AnnotatedString {
    if (query.trim().isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        var start = 0
        while (start < text.length) {
            val index = text.indexOf(query, start, ignoreCase = true)
            if (index == -1) {
                append(text.substring(start))
                break
            }
            append(text.substring(start, index))
            pushStyle(SpanStyle(background = Color(0xFFFFEB3B), color = Color.Black, fontWeight = FontWeight.Bold))
            append(text.substring(index, index + query.length))
            pop()
            start = index + query.length
        }
    }
}

/**
 * Saves camera preview snapshot bitmap into cache directory and returns Uri
 */
fun saveBitmapToCache(context: android.content.Context, bitmap: android.graphics.Bitmap): Uri? {
    return try {
        val file = java.io.File(context.cacheDir, "camera_capture_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
        }
        Uri.fromFile(file)
    } catch (e: Exception) {
        android.util.Log.e("MainScreen", "Error saving bitmap to cache", e)
        null
    }
}

/**
 * Local ML Kit text recognition (OCR) helper
 */
fun extractTextFromImageUri(context: android.content.Context, uri: Uri, onResult: (String) -> Unit) {
    try {
        val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(
            com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS
        )
        val image = com.google.mlkit.vision.common.InputImage.fromFilePath(context, uri)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val text = visionText.text
                if (text.trim().isNotEmpty()) {
                    onResult(text)
                } else {
                    onResult("No readable text found in this image.")
                }
            }
            .addOnFailureListener { e ->
                onResult("OCR Extraction failed: ${e.localizedMessage ?: "unknown error"}")
            }
    } catch (e: Exception) {
        onResult("OCR Failure: ${e.localizedMessage ?: "failed to load image content"}")
    }
}

/**
 * Local text extractor for Microsoft Word .docx files (XML unzip protocol)
 */
fun readDocxText(context: android.content.Context, uri: Uri): String {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return ""
        val tempFile = java.io.File.createTempFile("temp_docx", ".docx", context.cacheDir)
        tempFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        
        val zipFile = java.util.zip.ZipFile(tempFile)
        val entry = zipFile.getEntry("word/document.xml") ?: return ""
        val textBuilder = StringBuilder()
        
        zipFile.getInputStream(entry).bufferedReader().use { reader ->
            var line = reader.readLine()
            while (line != null) {
                var index = 0
                while (true) {
                    val start = line.indexOf("<w:t", index)
                    if (start == -1) break
                    val closeTag = line.indexOf(">", start)
                    if (closeTag == -1) break
                    val end = line.indexOf("</w:t>", closeTag)
                    if (end == -1) break
                    val text = line.substring(closeTag + 1, end)
                    textBuilder.append(text).append(" ")
                    index = end + 6
                }
                line = reader.readLine()
            }
        }
        zipFile.close()
        tempFile.delete()
        textBuilder.toString().trim()
    } catch (e: Exception) {
        "Failed to parse Word document: ${e.localizedMessage}"
    }
}

/**
 * Saves chat history as plain text files directly into Downloads folder
 */
fun exportChatToTxt(context: android.content.Context, chatTitle: String, messages: List<MessageEntity>) {
    try {
        val textBuilder = StringBuilder()
        textBuilder.append("=== ORION AI CHAT SESSION EXPORT ===\n")
        textBuilder.append("Topic: $chatTitle\n")
        textBuilder.append("Exported: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date())}\n\n")
        for (msg in messages) {
            val sender = if (msg.role == "user") "You" else "Orion AI"
            var text = msg.text
            if (text.startsWith("[attachment:")) {
                val endBracket = text.indexOf(']')
                if (endBracket != -1) {
                    text = text.substring(endBracket + 1)
                }
            }
            textBuilder.append("[$sender]: $text\n\n")
        }
        
        val filename = "${chatTitle.replace("[^a-zA-Z0-9]".toRegex(), "_")}_export.txt"
        val contentResolver = context.contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
        }
        
        val uri = contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(textBuilder.toString().toByteArray())
            }
            Toast.makeText(context, "Exported TXT successfully to Downloads", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Failed to export chat file.", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "TXT Export failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Native A4 PDF Document exporter
 */
fun exportChatToPdf(context: android.content.Context, chatTitle: String, messages: List<MessageEntity>) {
    try {
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val paint = android.graphics.Paint().apply {
            textSize = 12f
            color = android.graphics.Color.BLACK
        }
        val titlePaint = android.graphics.Paint().apply {
            textSize = 16f
            isFakeBoldText = true
            color = android.graphics.Color.BLACK
        }
        val headerPaint = android.graphics.Paint().apply {
            textSize = 10f
            color = android.graphics.Color.GRAY
        }
        
        var pageNumber = 1
        var pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNumber).create() // A4 Size
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        
        var y = 50f
        canvas.drawText("Orion AI Chat Session Export", 50f, y, titlePaint)
        y += 25f
        canvas.drawText("Session: $chatTitle", 50f, y, paint)
        y += 15f
        canvas.drawText("Date: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date())}", 50f, y, headerPaint)
        y += 30f
        
        for (msg in messages) {
            val sender = if (msg.role == "user") "YOU" else "ORION AI"
            var text = msg.text
            if (text.startsWith("[attachment:")) {
                val endBracket = text.indexOf(']')
                if (endBracket != -1) {
                    text = "[Attachment] " + text.substring(endBracket + 1)
                }
            }
            
            // Ensure space
            if (y > 780f) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 50f
            }
            
            val senderPaint = android.graphics.Paint().apply {
                textSize = 11f
                isFakeBoldText = true
                color = if (msg.role == "user") android.graphics.Color.BLUE else android.graphics.Color.MAGENTA
            }
            canvas.drawText(sender, 50f, y, senderPaint)
            y += 18f
            
            // Wrap text lines
            val words = text.split(" ")
            var line = ""
            for (word in words) {
                val testLine = if (line.isEmpty()) word else "$line $word"
                val width = paint.measureText(testLine)
                if (width > 495f) { // Page width 595 - margin 100
                    if (y > 780f) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        y = 50f
                    }
                    canvas.drawText(line, 50f, y, paint)
                    y += 15f
                    line = word
                } else {
                    line = testLine
                }
            }
            if (line.isNotEmpty()) {
                if (y > 780f) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    y = 50f
                }
                canvas.drawText(line, 50f, y, paint)
                y += 25f // message gap
            }
        }
        
        pdfDocument.finishPage(page)
        
        val filename = "${chatTitle.replace("[^a-zA-Z0-9]".toRegex(), "_")}_export.pdf"
        val contentResolver = context.contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
        }
        
        val uri = contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            Toast.makeText(context, "Exported PDF successfully to Downloads", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Failed to export PDF file.", Toast.LENGTH_SHORT).show()
        }
        pdfDocument.close()
    } catch (e: Exception) {
        Toast.makeText(context, "PDF Export failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun MainScreen(chatViewModel: ChatViewModel, authViewModel: com.example.viewmodel.AuthViewModel) {
    val splashActive by chatViewModel.splashActive.collectAsStateWithLifecycle()
    
    Box(modifier = Modifier.fillMaxSize().background(OrionBackground)) {
        AnimatedContent(
            targetState = splashActive,
            transitionSpec = {
                fadeIn(animationSpec = tween(600)) togetherWith
                fadeOut(animationSpec = tween(600))
            },
            label = "splash_transition"
        ) { active ->
            if (active) {
                SplashScreen()
            } else {
                ChatMainLayout(chatViewModel = chatViewModel, authViewModel = authViewModel)
            }
        }
    }
}

@Composable
fun SplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val logoPulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OrionBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(180.dp),
            contentAlignment = Alignment.Center
        ) {
            // Neon glowing radial circle representing Orion's core
            Box(
                modifier = Modifier
                    .size(140.dp * logoPulse)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(OrionPrimary.copy(alpha = 0.25f), Color.Transparent)
                        )
                    )
            )
            // Decorative star rings
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .border(1.5.dp, OrionSecondary.copy(alpha = 0.3f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .border(1.dp, OrionPrimary.copy(alpha = 0.4f), CircleShape)
            )
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = OrionPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "ORION AI",
            color = OrionTextPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 6.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your Stellar Intelligence Companion",
            color = OrionTextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(64.dp))

        // Futuristic neon linear loader
        Box(
            modifier = Modifier
                .width(180.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF1E293B))
        ) {
            val progressWidth by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = EaseInOutCirc),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "progress"
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progressWidth)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(OrionPrimary, OrionSecondary)
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Connecting to constellation...",
            color = OrionTextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun ChatMainLayout(chatViewModel: ChatViewModel, authViewModel: com.example.viewmodel.AuthViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    val chats by chatViewModel.allChats.collectAsStateWithLifecycle()
    val currentChat by chatViewModel.currentChat.collectAsStateWithLifecycle()
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val isPremium by authViewModel.isPremium.collectAsStateWithLifecycle()
    val messages by chatViewModel.messages.collectAsStateWithLifecycle()
    val searchMessageQuery by chatViewModel.searchMessageQuery.collectAsStateWithLifecycle()
    val sidebarSearchQuery by chatViewModel.searchQuery.collectAsStateWithLifecycle()

    val filteredChats = remember(chats, sidebarSearchQuery) {
        if (sidebarSearchQuery.trim().isEmpty()) {
            chats
        } else {
            chats.filter { it.title.contains(sidebarSearchQuery, ignoreCase = true) }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = OrionSurface,
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                modifier = Modifier
                    .width(290.dp)
                    .fillMaxHeight()
                    .border(
                        width = 1.dp,
                        color = OrionSurfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                    )
            ) {
                SidebarContent(
                    chatViewModel = chatViewModel,
                    chats = filteredChats,
                    currentChat = currentChat,
                    currentUser = currentUser,
                    onChatSelected = { chat ->
                        chatViewModel.selectChat(chat)
                        coroutineScope.launch { drawerState.close() }
                    },
                    onNewChatClick = {
                        chatViewModel.startNewChat()
                        coroutineScope.launch { drawerState.close() }
                    },
                    onDeleteChat = { chatId ->
                        chatViewModel.deleteChat(chatId)
                    },
                    onProfileClick = {
                        authViewModel.navigateTo(com.example.viewmodel.AuthScreen.Profile)
                        coroutineScope.launch { drawerState.close() }
                    },
                    onSettingsClick = {
                        authViewModel.navigateTo(com.example.viewmodel.AuthScreen.Settings)
                        coroutineScope.launch { drawerState.close() }
                    }
                )
            }
        },
        gesturesEnabled = true
    ) {
        Scaffold(
            containerColor = OrionBackground,
            topBar = {
                ChatTopBar(
                    currentChat = currentChat,
                    onMenuClick = {
                        coroutineScope.launch { drawerState.open() }
                    },
                    onResetChat = {
                        chatViewModel.startNewChat()
                    },
                    searchQuery = searchMessageQuery,
                    onSearchQueryChange = { query ->
                        chatViewModel.setSearchMessageQuery(query)
                    },
                    onExportPdf = {
                        exportChatToPdf(context, currentChat?.title ?: "Chat", messages)
                    },
                    onExportTxt = {
                        exportChatToTxt(context, currentChat?.title ?: "Chat", messages)
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    if (currentChat == null) {
                        HomeEmptyState(onPromptClick = { prompt ->
                            chatViewModel.sendMessage(prompt)
                        })
                    } else {
                        ActiveChatContent(chatViewModel, isPremium)
                    }
                }
                if (!isPremium) {
                    AdBannerView(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
fun SidebarContent(
    chatViewModel: ChatViewModel,
    chats: List<ChatEntity>,
    currentChat: ChatEntity?,
    currentUser: com.example.viewmodel.UserProfile?,
    onChatSelected: (ChatEntity) -> Unit,
    onNewChatClick: () -> Unit,
    onDeleteChat: (Long) -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp)
    ) {
        // Drawer Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = OrionPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "ORION AI",
                color = OrionTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // New Chat Button
        Button(
            onClick = onNewChatClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(48.dp)
                .testTag("new_chat_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(OrionPrimary, OrionSecondary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Chat",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "New Chat",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Chats Field
        val sidebarSearchQuery by chatViewModel.searchQuery.collectAsStateWithLifecycle()
        OutlinedTextField(
            value = sidebarSearchQuery,
            onValueChange = { chatViewModel.setSearchQuery(it) },
            placeholder = { Text("Search chats...", color = OrionTextMuted, fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = OrionTextMuted,
                    modifier = Modifier.size(16.dp)
                )
            },
            trailingIcon = {
                if (sidebarSearchQuery.isNotEmpty()) {
                    IconButton(onClick = { chatViewModel.setSearchQuery("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = OrionTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = OrionBackground,
                unfocusedContainerColor = OrionBackground,
                focusedBorderColor = OrionPrimary.copy(alpha = 0.5f),
                unfocusedBorderColor = OrionSurfaceVariant.copy(alpha = 0.3f),
                focusedTextColor = OrionTextPrimary,
                unfocusedTextColor = OrionTextPrimary
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // History Label
        Text(
            text = "CHAT HISTORY",
            color = OrionTextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
        )

        // List of history items
        if (chats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (sidebarSearchQuery.isNotEmpty()) "No matching conversations." else "No prior conversations.\nYour logs will manifest here.",
                    color = OrionTextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(chats, key = { it.id }) { chat ->
                    val isSelected = currentChat?.id == chat.id
                    val cardBg = if (isSelected) OrionSurfaceVariant else Color.Transparent
                    val borderModifier = if (isSelected) {
                        Modifier.border(1.dp, OrionPrimary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    } else Modifier

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(cardBg)
                            .then(borderModifier)
                            .clickable { onChatSelected(chat) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = if (isSelected) OrionPrimary else OrionTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = chat.title,
                            color = if (isSelected) OrionTextPrimary else OrionTextSecondary,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Chat",
                            tint = OrionTextMuted,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { onDeleteChat(chat.id) }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        // Dynamic Interactive User Profile Footer Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onProfileClick() }
                    .border(
                        1.dp,
                        OrionSurfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = OrionSurfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small glowing Insignia Avatar
                    val avatarColor = when (currentUser?.avatarType) {
                        "Cosmic" -> OrionSecondary
                        "Starlight" -> OrionTertiary
                        "Nova" -> Color(0xFFF59E0B)
                        else -> OrionPrimary
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(avatarColor.copy(alpha = 0.15f))
                            .border(1.dp, avatarColor.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (currentUser?.avatarType) {
                                "Cosmic" -> Icons.Default.AutoAwesome
                                "Starlight" -> Icons.Default.Brightness5
                                "Nova" -> Icons.Default.LocalFireDepartment
                                else -> Icons.Default.Brightness4
                            },
                            contentDescription = null,
                            tint = avatarColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = currentUser?.name ?: "Orion Explorer",
                            color = OrionTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentUser?.email ?: "explorer@orion.ai",
                            color = OrionTextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("sidebar_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Profile Settings",
                            tint = OrionTextSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    currentChat: ChatEntity?,
    onMenuClick: () -> Unit,
    onResetChat: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onExportPdf: () -> Unit,
    onExportTxt: () -> Unit
) {
    var isSearchActive by remember { mutableStateOf(false) }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = OrionBackground,
            titleContentColor = OrionTextPrimary
        ),
        title = {
            if (isSearchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search messages...", color = OrionTextMuted, fontSize = 14.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = OrionTextPrimary,
                        unfocusedTextColor = OrionTextPrimary,
                        cursorColor = OrionPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            } else if (currentChat != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(OrionTertiary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentChat.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = "Orion AI",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick, modifier = Modifier.testTag("menu_button")) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Navigation Menu",
                    tint = OrionTextPrimary
                )
            }
        },
        actions = {
            if (currentChat != null) {
                IconButton(onClick = {
                    isSearchActive = !isSearchActive
                    if (!isSearchActive) {
                        onSearchQueryChange("")
                    }
                }) {
                    Icon(
                        imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search Messages",
                        tint = if (isSearchActive) OrionPrimary else OrionTextSecondary
                    )
                }

                var showExportMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showExportMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export Chat",
                            tint = OrionTextSecondary
                        )
                    }
                    DropdownMenu(
                        expanded = showExportMenu,
                        onDismissRequest = { showExportMenu = false },
                        modifier = Modifier.background(OrionSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export to PDF", color = OrionTextPrimary, fontSize = 13.sp) },
                            onClick = {
                                showExportMenu = false
                                onExportPdf()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export to TXT", color = OrionTextPrimary, fontSize = 13.sp) },
                            onClick = {
                                showExportMenu = false
                                onExportTxt()
                            }
                        )
                    }
                }

                IconButton(onClick = onResetChat) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Start New Session",
                        tint = OrionTextSecondary
                    )
                }
            }
        },
        modifier = Modifier.border(
            width = 0.5.dp,
            color = OrionSurfaceVariant.copy(alpha = 0.3f)
        )
    )
}

@Composable
fun HomeEmptyState(onPromptClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .background(OrionBackground),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Orion Constellation custom canvas
        OrionConstellation(modifier = Modifier.weight(1f))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Welcome to Orion AI",
                color = OrionTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "A boundless cosmos of intelligence, ready to co-create with you.",
                color = OrionTextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick action starting prompts grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "DISCOVER POSSIBILITIES",
                color = OrionTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            val starterPrompts = listOf(
                "Draft an email about starry space travel",
                "Write a Python script for a simple clock",
                "Explain black holes like I'm 10 years old",
                "Brainstorm names for a deep space base"
            )

            starterPrompts.forEach { prompt ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(OrionSurface)
                        .border(1.dp, OrionSurfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable { onPromptClick(prompt) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = OrionPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = prompt,
                        color = OrionTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveChatContent(viewModel: ChatViewModel, isPremium: Boolean) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isTyping by viewModel.isTyping.collectAsStateWithLifecycle()
    val searchMessageQuery by viewModel.searchMessageQuery.collectAsStateWithLifecycle()
    val activeMode by viewModel.activeMode.collectAsStateWithLifecycle()
    
    val scrollState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val activity = context as? Activity

    var inputText by remember { mutableStateOf("") }
    
    // Attachment State
    var stagedAttachmentUri by remember { mutableStateOf<Uri?>(null) }
    var stagedMimeType by remember { mutableStateOf<String?>(null) }
    var stagedFileName by remember { mutableStateOf<String?>(null) }

    // Picker Launchers
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            stagedAttachmentUri = uri
            stagedMimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            stagedFileName = "image_upload.jpg"
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val uri = saveBitmapToCache(context, bitmap)
            if (uri != null) {
                stagedAttachmentUri = uri
                stagedMimeType = "image/jpeg"
                stagedFileName = "camera_capture.jpg"
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            stagedAttachmentUri = uri
            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
            stagedMimeType = mime
            
            var name = "file_upload"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ActiveChatContent", "Error querying file name", e)
            }
            stagedFileName = name
        }
    }

    val handleSendMessage = {
        val text = inputText.trim()
        val finalMessageText = when {
            stagedAttachmentUri != null && stagedMimeType?.startsWith("image/") == true -> {
                "[attachment:$stagedMimeType:$stagedAttachmentUri:$stagedFileName]$text"
            }
            stagedAttachmentUri != null && stagedMimeType == "application/pdf" -> {
                "[attachment:application/pdf:$stagedAttachmentUri:$stagedFileName]$text"
            }
            stagedAttachmentUri != null && (stagedMimeType?.contains("word") == true || stagedFileName?.endsWith(".docx") == true) -> {
                val extracted = readDocxText(context, stagedAttachmentUri!!)
                "[attachment:text/plain:$stagedAttachmentUri:$stagedFileName]\n\n[Word Document Context: $stagedFileName]\n\"\"\"\n$extracted\n\"\"\"\n\n$text"
            }
            stagedAttachmentUri != null && (stagedMimeType?.startsWith("text/") == true || stagedFileName?.endsWith(".txt") == true) -> {
                val extracted = try {
                    context.contentResolver.openInputStream(stagedAttachmentUri!!)?.bufferedReader()?.use { it.readText() } ?: ""
                } catch (e: Exception) { "" }
                "[attachment:text/plain:$stagedAttachmentUri:$stagedFileName]\n\n[Text Document Context: $stagedFileName]\n\"\"\"\n$extracted\n\"\"\"\n\n$text"
            }
            else -> text
        }

        if (finalMessageText.isNotEmpty()) {
            if (!isPremium) {
                viewModel.incrementMessageCount()
                if (viewModel.messageCountForAds >= 3) {
                    activity?.let { act ->
                        AdMobInterstitialHelper.showAd(act) {
                            viewModel.sendMessage(finalMessageText)
                            inputText = ""
                            stagedAttachmentUri = null
                            stagedMimeType = null
                            stagedFileName = null
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            viewModel.resetMessageCountForAds()
                        }
                    } ?: run {
                        viewModel.sendMessage(finalMessageText)
                        inputText = ""
                        stagedAttachmentUri = null
                        stagedMimeType = null
                        stagedFileName = null
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                } else {
                    viewModel.sendMessage(finalMessageText)
                    inputText = ""
                    stagedAttachmentUri = null
                    stagedMimeType = null
                    stagedFileName = null
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
            } else {
                viewModel.sendMessage(finalMessageText)
                inputText = ""
                stagedAttachmentUri = null
                stagedMimeType = null
                stagedFileName = null
                keyboardController?.hide()
                focusManager.clearFocus()
            }
        }
    }

    // Scroll to bottom when messages list size changes
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scrollState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // AI Mode selection row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                Text(
                    text = "MODE:",
                    color = OrionTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            items(ChatViewModel.AiMode.values()) { mode ->
                FilterChip(
                    selected = activeMode == mode,
                    onClick = { viewModel.setActiveMode(mode) },
                    label = { 
                        Text(
                            text = mode.displayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ) 
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OrionPrimary.copy(alpha = 0.25f),
                        selectedLabelColor = OrionPrimary,
                        selectedLeadingIconColor = OrionPrimary,
                        containerColor = OrionSurface,
                        labelColor = OrionTextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = activeMode == mode,
                        selectedBorderColor = OrionPrimary,
                        borderColor = OrionSurfaceVariant.copy(alpha = 0.4f)
                    )
                )
            }
        }

        // Message log LazyColumn
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val filteredMessages = if (searchMessageQuery.trim().isEmpty()) {
                messages
            } else {
                messages.filter { it.text.contains(searchMessageQuery, ignoreCase = true) }
            }

            if (filteredMessages.isEmpty() && searchMessageQuery.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No messages match your search.",
                            color = OrionTextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(filteredMessages, key = { it.id }) { msg ->
                    val isUser = msg.role == "user"
                    MessageBubble(
                        message = msg,
                        isUser = isUser,
                        searchQuery = searchMessageQuery,
                        onRegenerate = {
                            viewModel.regenerateLastResponse()
                        }
                    )
                }
            }

            if (isTyping) {
                item {
                    TypingIndicator()
                }
            }
        }

        // Attachment Preview Box
        if (stagedAttachmentUri != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .background(OrionSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, OrionSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (stagedMimeType?.startsWith("image/") == true) {
                    AsyncImage(
                        model = stagedAttachmentUri,
                        contentDescription = "Attachment Preview",
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, OrionPrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(OrionPrimary.copy(alpha = 0.12f))
                            .border(1.dp, OrionPrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (stagedMimeType == "application/pdf") Icons.Default.Description else Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            tint = OrionPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stagedFileName ?: "uploading_file",
                        color = OrionTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when (stagedMimeType) {
                            "application/pdf" -> "PDF Document"
                            "image/jpeg", "image/png" -> "Staged Image"
                            else -> if (stagedFileName?.endsWith(".docx") == true) "Word Document" else "Text Document"
                        },
                        color = OrionTextSecondary,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = {
                        stagedAttachmentUri = null
                        stagedMimeType = null
                        stagedFileName = null
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove Attachment",
                        tint = OrionTextMuted
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        } else {
            // Live Quick attachment drawer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ATTACH:",
                    color = OrionTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                IconButton(onClick = { cameraLauncher.launch(null) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Capture Photo",
                        tint = OrionPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = { imagePickerLauncher.launch("image/*") }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Attach Image",
                        tint = OrionSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = { filePickerLauncher.launch("application/pdf") }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "Attach PDF",
                        tint = OrionTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = { filePickerLauncher.launch("*/*") }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.InsertDriveFile,
                        contentDescription = "Attach Document",
                        tint = OrionTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Input bottom bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(OrionBackground)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Message Orion...", color = OrionTextMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = OrionSurface,
                    unfocusedContainerColor = OrionSurface,
                    focusedBorderColor = OrionPrimary,
                    unfocusedBorderColor = OrionSurfaceVariant.copy(alpha = 0.5f),
                    focusedTextColor = OrionTextPrimary,
                    unfocusedTextColor = OrionTextPrimary
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        handleSendMessage()
                    }
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Glowing Send Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(OrionPrimary, OrionSecondary)
                        )
                    )
                    .clickable {
                        handleSendMessage()
                    }
                    .testTag("send_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Message",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: MessageEntity,
    isUser: Boolean,
    searchQuery: String = "",
    onRegenerate: () -> Unit
) {
    val context = LocalContext.current
    
    // Parse attachments in messages
    val attachmentInfo = remember(message.text) {
        if (message.text.startsWith("[attachment:")) {
            val endBracket = message.text.indexOf(']')
            if (endBracket != -1) {
                val content = message.text.substring(12, endBracket)
                val parts = content.split(":", limit = 3)
                if (parts.size >= 3) {
                    val mimeType = parts[0]
                    val uriString = parts[1]
                    val fileName = parts[2]
                    val actualText = message.text.substring(endBracket + 1)
                    Triple(mimeType, uriString, actualText)
                } else null
            } else null
        } else null
    }

    val actualMessageText = attachmentInfo?.third ?: message.text
    var ocrResultText by remember { mutableStateOf<String?>(null) }
    var isOcrExtracting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Role Label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp, start = 8.dp, end = 8.dp)
        ) {
            if (!isUser) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = OrionPrimary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = if (isUser) "YOU" else "ORION AI",
                color = OrionTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // Bubble Content
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 16.dp
                    )
                )
                .background(
                    if (isUser) {
                        Brush.linearGradient(colors = listOf(OrionPrimary, OrionSecondary))
                    } else {
                        Brush.verticalGradient(colors = listOf(OrionSurface, OrionSurface))
                    }
                )
                .border(
                    width = if (isUser) 0.dp else 1.dp,
                    color = if (isUser) Color.Transparent else OrionSurfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 16.dp
                    )
                )
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // If there is an attachment, display it
                if (attachmentInfo != null) {
                    val mime = attachmentInfo.first
                    val uriStr = attachmentInfo.second
                    val fileName = message.text.substring(message.text.indexOf(":", message.text.indexOf(":") + 1) + 1, message.text.indexOf("]")).split(":").last()

                    if (mime.startsWith("image/")) {
                        Column {
                            AsyncImage(
                                model = Uri.parse(uriStr),
                                contentDescription = "User attached image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 180.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, OrionPrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            // OCR trigger button
                            Button(
                                onClick = {
                                    isOcrExtracting = true
                                    extractTextFromImageUri(context, Uri.parse(uriStr)) { result ->
                                        isOcrExtracting = false
                                        ocrResultText = result
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = OrionSurfaceVariant),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.DocumentScanner,
                                        contentDescription = null,
                                        tint = OrionPrimary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isOcrExtracting) "Reading..." else "Extract Text (OCR)",
                                        fontSize = 10.sp,
                                        color = OrionTextPrimary
                                    )
                                }
                            }
                            
                            ocrResultText?.let { ocrText ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                        .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        .padding(8.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "OCR TEXT EXTRACTED:",
                                            color = OrionPrimary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = ocrText,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else if (mime == "application/pdf") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = OrionPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = fileName,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "PDF Document",
                                    color = OrionTextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    } else {
                        // Word or txt file
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = OrionSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = fileName,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Text Document Context",
                                    color = OrionTextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                if (isUser) {
                    Text(
                        text = highlightSearchTerm(actualMessageText, searchQuery, Color.White),
                        color = Color.White,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                } else {
                    MarkdownText(
                        text = actualMessageText,
                        textColor = OrionTextPrimary
                    )
                }
            }
        }

        // Action controls (for responses)
        Row(
            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Copy Action
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                        copyToClipboard(context, actualMessageText)
                    }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy Response",
                    tint = OrionTextMuted,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Copy",
                    color = OrionTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Share Action
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, actualMessageText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Message"))
                    }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share Response",
                    tint = OrionTextMuted,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Share",
                    color = OrionTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Regenerate Action (for last AI responses)
            if (!isUser) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onRegenerate() }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Regenerate Response",
                        tint = OrionTextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Regenerate",
                        color = OrionTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    val dotScale1 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dotScale2 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dotScale3 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp, start = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = OrionPrimary,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "ORION AI",
                color = OrionTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp))
                .background(OrionSurface)
                .border(1.dp, OrionSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp))
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp * dotScale1)
                    .clip(CircleShape)
                    .background(OrionPrimary)
            )
            Box(
                modifier = Modifier
                    .size(8.dp * dotScale2)
                    .clip(CircleShape)
                    .background(OrionSecondary)
            )
            Box(
                modifier = Modifier
                    .size(8.dp * dotScale3)
                    .clip(CircleShape)
                    .background(OrionPrimary)
            )
        }
    }
}


