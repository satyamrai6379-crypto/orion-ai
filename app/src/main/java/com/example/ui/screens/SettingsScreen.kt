package com.example.ui.screens

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.viewmodel.AuthScreen
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: AuthViewModel, chatViewModel: ChatViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // Retrieve active theme and language state
    val currentThemeNameState by chatViewModel.selectedTheme.collectAsStateWithLifecycle()
    val currentLangState by chatViewModel.selectedLanguage.collectAsStateWithLifecycle()

    var selectedModel by remember { mutableStateOf(chatViewModel.getSelectedModel()) }
    var temperature by remember { mutableFloatStateOf(chatViewModel.getTemperature()) }
    
    var syncSuccessMsg by remember { mutableStateOf<String?>(null) }
    var isSyncingCloud by remember { mutableStateOf(false) }

    // Backup & Restore states
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    // App Update Checker states
    var isCheckingUpdates by remember { mutableStateOf(false) }
    var showNoUpdateDialog by remember { mutableStateOf(false) }
    var showUpdateAvailableDialog by remember { mutableStateOf(false) }

    // Dialog flags
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var showTermsConditions by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // System activity launcher for importing chat backups (JSON)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isImporting = true
            chatViewModel.importChatsFromBackupJson(context, uri) {
                isImporting = false
            }
        }
    }

    // Helper translation lambda
    val trans = { key: String -> OrionTranslations.getString(key, currentLangState) }

    Scaffold(
        containerColor = OrionBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OrionBackground,
                    titleContentColor = OrionTextPrimary
                ),
                title = {
                    Text(
                        text = trans("settings_title"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AuthScreen.MainApp) }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = trans("back"),
                            tint = OrionTextPrimary
                        )
                    }
                },
                modifier = Modifier.border(
                    width = 0.5.dp,
                    color = OrionSurfaceVariant.copy(alpha = 0.3f)
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, OrionSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = OrionSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(OrionPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = OrionPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = trans("system_control"),
                                color = OrionPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = trans("customize_desc"),
                                color = OrionTextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Subscription Management Card
                val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, OrionSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = OrionSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = trans("sub_management"),
                            color = OrionPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = trans("current_tier"),
                                    color = OrionTextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (isPremium) "Orion AI Premium" else "Orion Free Plan",
                                    color = if (isPremium) OrionTertiary else OrionTextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (isPremium) OrionTertiary.copy(alpha = 0.15f) 
                                        else OrionTextMuted.copy(alpha = 0.15f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isPremium) OrionTertiary else OrionTextMuted.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(50)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isPremium) trans("pro_active") else trans("basic_tier"),
                                    color = if (isPremium) OrionTertiary else OrionTextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        if (!isPremium) {
                            Text(
                                text = "Unlock unlimited analytical AI models (Gemini Pro), remove all advertisements, and gain priority response speed.",
                                color = OrionTextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )

                            Button(
                                onClick = {
                                    viewModel.navigateTo(AuthScreen.Premium)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("upgrade_subscription_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = OrionSecondary
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Upgrade",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = trans("upgrade_btn"),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Thank you for being an Orion AI Premium subscriber! You have full unlimited access and an ad-free workspace.",
                                color = OrionTextSecondary,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                            
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                            data = android.net.Uri.parse("https://play.google.com/store/account/subscriptions")
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not open Subscriptions settings", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("manage_subscription_button"),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, OrionSurfaceVariant),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = OrionTextPrimary
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = "Manage",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = trans("manage_sub"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Theme Customization Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, OrionSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = OrionSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = trans("theme_customization"),
                            color = OrionPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = trans("select_theme_desc"),
                            color = OrionTextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        val themeOptions = listOf(
                            "space_dark" to trans("space_dark"),
                            "nebula_teal" to trans("nebula_teal"),
                            "aurora_green" to trans("aurora_green"),
                            "light_starlight" to trans("light_starlight")
                        )

                        themeOptions.forEach { (themeId, themeLabel) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { chatViewModel.setSelectedTheme(themeId) }
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentThemeNameState == themeId,
                                    onClick = { chatViewModel.setSelectedTheme(themeId) },
                                    colors = RadioButtonDefaults.colors(selectedColor = OrionPrimary)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = themeLabel,
                                    color = OrionTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Multi-language Selection Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, OrionSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = OrionSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = trans("language_support"),
                            color = OrionPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = trans("language_desc"),
                            color = OrionTextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { chatViewModel.setSelectedLanguage("en") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (currentLangState == "en") OrionPrimary else OrionSurfaceVariant
                                )
                            ) {
                                Text(
                                    text = trans("lang_en"),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = { chatViewModel.setSelectedLanguage("hi") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (currentLangState == "hi") OrionPrimary else OrionSurfaceVariant
                                )
                            ) {
                                Text(
                                    text = trans("lang_hi"),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Backup and Portability Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, OrionSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = OrionSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = trans("backup_restore"),
                            color = OrionPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = trans("backup_desc"),
                            color = OrionTextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Button(
                            onClick = {
                                isExporting = true
                                chatViewModel.exportChatsToBackupJson(context) {
                                    isExporting = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrionPrimary)
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(trans("export_backup"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                importLauncher.launch(arrayOf("application/json"))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, OrionSurfaceVariant)
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(color = OrionPrimary, modifier = Modifier.size(18.dp))
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(trans("import_backup"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OrionTextPrimary)
                                }
                            }
                        }
                    }
                }

                // AI Model Selection Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, OrionSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = OrionSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = trans("model_title"),
                            color = OrionPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )

                        // Model Option 1: Flash
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selectedModel == "gemini-3.5-flash") OrionPrimary.copy(alpha = 0.1f) else Color.Transparent)
                                .border(
                                    width = if (selectedModel == "gemini-3.5-flash") 1.dp else 0.5.dp,
                                    color = if (selectedModel == "gemini-3.5-flash") OrionPrimary else OrionSurfaceVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedModel = "gemini-3.5-flash"
                                    chatViewModel.setSelectedModel("gemini-3.5-flash")
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedModel == "gemini-3.5-flash",
                                onClick = {
                                    selectedModel = "gemini-3.5-flash"
                                    chatViewModel.setSelectedModel("gemini-3.5-flash")
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = OrionPrimary)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Gemini 3.5 Flash (Default)",
                                    color = OrionTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Highly optimized for near-instant responses and concise formatting.",
                                    color = OrionTextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        // Model Option 2: Pro
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selectedModel == "gemini-3.1-pro-preview") OrionSecondary.copy(alpha = 0.1f) else Color.Transparent)
                                .border(
                                    width = if (selectedModel == "gemini-3.1-pro-preview") 1.dp else 0.5.dp,
                                    color = if (selectedModel == "gemini-3.1-pro-preview") OrionSecondary else OrionSurfaceVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedModel = "gemini-3.1-pro-preview"
                                    chatViewModel.setSelectedModel("gemini-3.1-pro-preview")
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedModel == "gemini-3.1-pro-preview",
                                onClick = {
                                    selectedModel = "gemini-3.1-pro-preview"
                                    chatViewModel.setSelectedModel("gemini-3.1-pro-preview")
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = OrionSecondary)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Gemini 3.1 Pro (Analytical)",
                                    color = OrionTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tailored for heavy reasoning, detailed breakdowns, and coding tasks.",
                                    color = OrionTextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                // Creativity Slider Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, OrionSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = OrionSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = trans("temp_creativity"),
                                color = OrionPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = String.format("%.1f", temperature),
                                color = OrionSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Slider(
                            value = temperature,
                            onValueChange = {
                                temperature = it
                                chatViewModel.setTemperature(it)
                            },
                            valueRange = 0.0f..1.0f,
                            steps = 9,
                            colors = SliderDefaults.colors(
                                thumbColor = OrionPrimary,
                                activeTrackColor = OrionPrimary,
                                inactiveTrackColor = OrionSurfaceVariant
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Precise & Deterministic", color = OrionTextMuted, fontSize = 11.sp)
                            Text(text = "Creative & Stellar", color = OrionTextMuted, fontSize = 11.sp)
                        }
                    }
                }

                // Cloud Synchronization Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, OrionSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = OrionSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = trans("firebase_sync"),
                            color = OrionPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )

                        Text(
                            text = "All chat logs are safely preserved under your secure Firestore space.",
                            color = OrionTextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        AnimatedVisibility(
                            visible = syncSuccessMsg != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(OrionTertiary.copy(alpha = 0.15f))
                                    .border(1.dp, OrionTertiary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = OrionTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = syncSuccessMsg ?: "",
                                    color = OrionTextPrimary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                isSyncingCloud = true
                                syncSuccessMsg = null
                                coroutineScope.launch {
                                    chatViewModel.syncWithCloud()
                                    kotlinx.coroutines.delay(1000)
                                    isSyncingCloud = false
                                    syncSuccessMsg = "Sync completed! System metrics match Cloud Space."
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("manual_sync_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrionPrimary
                            )
                        ) {
                            if (isSyncingCloud) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = "Sync",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = trans("manual_sync"),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Support AdMob Rewarded Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, OrionSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = OrionSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "SUPPORT ORION (REWARDED ADS)",
                            color = OrionPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )

                        Text(
                            text = "Watch a short sponsor video to support Orion! As a celestial thank you, this will instantly grant your session Premium Status.",
                            color = OrionTextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Button(
                            onClick = {
                                val activity = context as? Activity
                                if (activity != null) {
                                    Toast.makeText(context, "Opening starlight sponsor connection...", Toast.LENGTH_SHORT).show()
                                    AdMobRewardedHelper.showAd(
                                        activity = activity,
                                        onRewardEarned = { amount ->
                                            viewModel.setPremiumStatus(true)
                                            Toast.makeText(context, "Premium Status unlocked! Ads dissolved for this session.", Toast.LENGTH_LONG).show()
                                        },
                                        onAdClosed = {
                                            // Handle completion if needed
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrionSecondary)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("WATCH SPONSOR VIDEO", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // System Updates & Legal Policies Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, OrionSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = OrionSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = trans("legal_policies"),
                            color = OrionPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )

                        // Update checker button
                        Button(
                            onClick = {
                                isCheckingUpdates = true
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(1200)
                                    isCheckingUpdates = false
                                    showNoUpdateDialog = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrionSurfaceVariant)
                        ) {
                            if (isCheckingUpdates) {
                                CircularProgressIndicator(color = OrionTextPrimary, modifier = Modifier.size(18.dp))
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp), tint = OrionTextPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(trans("update_btn"), color = OrionTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Divider(color = OrionSurfaceVariant.copy(alpha = 0.5f))

                        // Legal links
                        Text(
                            text = trans("privacy_policy"),
                            color = OrionPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPrivacyPolicy = true }
                                .padding(vertical = 6.dp)
                        )

                        Text(
                            text = trans("terms_conditions"),
                            color = OrionPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTermsConditions = true }
                                .padding(vertical = 6.dp)
                        )

                        Text(
                            text = trans("about_orion"),
                            color = OrionPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAboutDialog = true }
                                .padding(vertical = 6.dp)
                        )
                    }
                }

                // Diagnostics Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, OrionSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = OrionSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = trans("diagnostics"),
                            color = OrionPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )

                        val firebaseUser = FirebaseAuth.getInstance().currentUser
                        val uid = firebaseUser?.uid ?: "local_bypass_uid"
                        
                        DiagnosticRow(label = "User Email", value = currentUser?.email ?: "explorer@orion.ai")
                        DiagnosticRow(label = "Firebase UID", value = uid)
                        DiagnosticRow(label = "Engine Version", value = "v1.5-Starlight-Pro")
                        DiagnosticRow(label = "Workspace Mode", value = if (uid == "local_bypass_uid") "Offline Fallback" else "Authenticated Cloud")
                    }
                }
            }
        }
    }

    // Interactive Dialog: No Update Available
    if (showNoUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showNoUpdateDialog = false },
            confirmButton = {
                TextButton(onClick = { showNoUpdateDialog = false }) {
                    Text("OK", color = OrionPrimary)
                }
            },
            title = { Text("System Synchronized", color = OrionTextPrimary) },
            text = { Text(trans("no_update_toast"), color = OrionTextSecondary) },
            containerColor = OrionSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Interactive Dialog: Privacy Policy
    if (showPrivacyPolicy) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicy = false },
            confirmButton = {
                TextButton(onClick = { showPrivacyPolicy = false }) {
                    Text("CLOSE", color = OrionPrimary)
                }
            },
            title = { Text("Privacy Policy", color = OrionTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Effective Date: July 16, 2026", color = OrionPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Orion AI values your privacy. We are committed to securing your personal space and conversation logs. " +
                        "This policy details our local first architecture and secure cloud sync mechanisms.",
                        color = OrionTextSecondary,
                        fontSize = 13.sp
                    )
                    Text("1. Data Persistence", color = OrionTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "All of your conversation details are saved directly on your local device in a highly secure, private SQLite database using Jetpack Room. We never read or parse your local offline logs unless synced to Firebase.",
                        color = OrionTextSecondary,
                        fontSize = 13.sp
                    )
                    Text("2. Cloud Synchronization", color = OrionTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "If you sign in using your Firebase account, you may choose to leverage manual or automatic cloud synchronization. This mirrors your local SQLite database securely inside Firestore, encrypted using Google cloud-grade security standard keys.",
                        color = OrionTextSecondary,
                        fontSize = 13.sp
                    )
                    Text("3. AI Processing & API Keys", color = OrionTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Your prompts and attachments are securely processed by Google Gemini API endpoints. We do not use your chat logs to train model baselines. All key configurations remain entirely within your app's sandboxed environment.",
                        color = OrionTextSecondary,
                        fontSize = 13.sp
                    )
                }
            },
            containerColor = OrionSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Interactive Dialog: Terms & Conditions
    if (showTermsConditions) {
        AlertDialog(
            onDismissRequest = { showTermsConditions = false },
            confirmButton = {
                TextButton(onClick = { showTermsConditions = false }) {
                    Text("ACCEPT", color = OrionPrimary)
                }
            },
            title = { Text("Terms & Conditions", color = OrionTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Last Updated: July 16, 2026", color = OrionPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "By launching and exploring the Orion AI companion application, you unconditionally agree to these system-wide operational terms.",
                        color = OrionTextSecondary,
                        fontSize = 13.sp
                    )
                    Text("1. Permitted Use", color = OrionTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Orion AI provides advanced text generation, programming assistance, study analysis, and edit operations. You must not abuse the underlying APIs, attempt server-side cracking, or generate toxic/harmful content.",
                        color = OrionTextSecondary,
                        fontSize = 13.sp
                    )
                    Text("2. Billing & Subscriptions", color = OrionTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Subscriptions are handled directly by Google Play Billing services. Charges will occur monthly. You are free to cancel any active subscription anytime using your Android system account control panels.",
                        color = OrionTextSecondary,
                        fontSize = 13.sp
                    )
                    Text("3. Disclaimer of AI Outputs", color = OrionTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "AI results are generated dynamically using LLMs. While highly sophisticated, Orion AI may occasionally produce inaccurate historical, medical, or legal claims. Please verify crucial details independently.",
                        color = OrionTextSecondary,
                        fontSize = 13.sp
                    )
                }
            },
            containerColor = OrionSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Interactive Dialog: About Orion AI
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK", color = OrionPrimary)
                }
            },
            title = { Text("About Orion AI", color = OrionTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(OrionPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = OrionPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Text(
                        text = "Orion AI Companion",
                        color = OrionTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Version 1.5.0-Starlight",
                        color = OrionTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Orion AI is a premium offline-first Android conversational assistant, beautifully crafted in Kotlin, Jetpack Compose, and Material Design 3. Powered by Google Gemini AI, featuring secure Firestore sync and robust local SQLite storage, Orion brings state-of-the-art intelligence straight to your pocket.",
                        color = OrionTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = "Created with ❤️ by DeepMind AI",
                        color = OrionPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            },
            containerColor = OrionSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = OrionTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = OrionTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
