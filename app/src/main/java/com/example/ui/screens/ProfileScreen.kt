package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.viewmodel.AuthScreen
import com.example.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: AuthViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()

    var name by remember(currentUser) { mutableStateOf(currentUser?.name ?: "") }
    var selectedAvatar by remember(currentUser) { mutableStateOf(currentUser?.avatarType ?: "Nebula") }

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
                        text = "Orion Profile Center",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AuthScreen.MainApp) }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
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
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Large Avatar display with glowing star depending on selection
                Box(
                    modifier = Modifier.size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val pulseTransition = rememberInfiniteTransition(label = "pulse_profile")
                    val pulse by pulseTransition.animateFloat(
                        initialValue = 0.85f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = EaseInOutSine),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "glow"
                    )

                    val avatarColor = when (selectedAvatar) {
                        "Cosmic" -> OrionSecondary
                        "Starlight" -> OrionTertiary
                        "Nova" -> Color(0xFFF59E0B) // Bright Amber Gold
                        else -> OrionPrimary
                    }

                    Box(
                        modifier = Modifier
                            .size(110.dp * pulse)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(avatarColor.copy(alpha = 0.3f), Color.Transparent)
                                )
                            )
                    )

                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .border(2.dp, avatarColor, CircleShape)
                            .background(OrionSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (selectedAvatar) {
                                "Cosmic" -> Icons.Default.AutoAwesome
                                "Starlight" -> Icons.Default.Brightness5
                                "Nova" -> Icons.Default.LocalFireDepartment
                                else -> Icons.Default.Brightness4
                            },
                            contentDescription = null,
                            tint = avatarColor,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                // Email display
                Text(
                    text = currentUser?.email ?: "explorer@orion.ai",
                    color = OrionTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )

                // Feedback bar
                AnimatedVisibility(
                    visible = errorMessage != null || successMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (errorMessage != null) Color(0xFF7F1D1D) else Color(0xFF064E3B)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (errorMessage != null) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorMessage ?: successMessage ?: "",
                                color = Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Custom options Card
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
                            text = "METRIC IDENTITY CONFIG",
                            color = OrionPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )

                        // Name input
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Display Name", color = OrionTextSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = OrionBackground,
                                unfocusedContainerColor = OrionBackground,
                                focusedBorderColor = OrionPrimary,
                                unfocusedBorderColor = OrionSurfaceVariant,
                                focusedTextColor = OrionTextPrimary,
                                unfocusedTextColor = OrionTextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_name_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Avatar insignia selector
                        Text(
                            text = "SELECT STAR INSIGNIA",
                            color = OrionTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val insignias = listOf(
                                Pair("Nebula", OrionPrimary),
                                Pair("Cosmic", OrionSecondary),
                                Pair("Starlight", OrionTertiary),
                                Pair("Nova", Color(0xFFF59E0B))
                            )

                            insignias.forEach { (type, color) ->
                                val isSelected = selectedAvatar == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) color.copy(alpha = 0.15f) else OrionBackground)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) color else OrionSurfaceVariant,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedAvatar = type }
                                        .padding(vertical = 12.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = when (type) {
                                                "Cosmic" -> Icons.Default.AutoAwesome
                                                "Starlight" -> Icons.Default.Brightness5
                                                "Nova" -> Icons.Default.LocalFireDepartment
                                                else -> Icons.Default.Brightness4
                                            },
                                            contentDescription = null,
                                            tint = color,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = type,
                                            color = if (isSelected) OrionTextPrimary else OrionTextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Sync changes action
                Button(
                    onClick = { viewModel.updateProfile(name, selectedAvatar) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_profile_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
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
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = "SYNCHRONIZE PROFILE METRICS",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // Logout button
                OutlinedButton(
                    onClick = { viewModel.logout() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("logout_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEF4444)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SECURE LOGOUT / DISCONNECT",
                            color = Color(0xFFEF4444),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
