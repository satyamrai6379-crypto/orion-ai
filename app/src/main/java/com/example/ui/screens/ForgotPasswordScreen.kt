package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.viewmodel.AuthScreen
import com.example.viewmodel.AuthViewModel

@Composable
fun ForgotPasswordScreen(viewModel: AuthViewModel) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OrionBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Stars recovery banner
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(OrionTertiary.copy(alpha = 0.2f), Color.Transparent)
                            )
                        )
                )
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = OrionTertiary,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Starlight Key Recovery",
                color = OrionTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

            Text(
                text = "Submit your credentials to transmit recovery signals",
                color = OrionTextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Notifications/Feedback State Box
            AnimatedVisibility(
                visible = errorMessage != null || successMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (errorMessage != null) Color(0xFF7F1D1D) else Color(0xFF064E3B)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (errorMessage != null) Icons.Default.ErrorOutline else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = errorMessage ?: successMessage ?: "",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Input fields
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        OrionSurfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = OrionSurface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "TRANSMIT VERIFICATION LINK",
                        color = OrionTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Registered Email Address", color = OrionTextSecondary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = OrionTertiary
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = OrionBackground,
                            unfocusedContainerColor = OrionBackground,
                            focusedBorderColor = OrionTertiary,
                            unfocusedBorderColor = OrionSurfaceVariant,
                            focusedTextColor = OrionTextPrimary,
                            unfocusedTextColor = OrionTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("forgot_email_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                viewModel.resetPassword(email)
                            }
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Button
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    viewModel.resetPassword(email)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("forgot_submit_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(OrionTertiary, OrionPrimary)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(
                            text = "TRANSMIT RECOVERY SIGNAL",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Back to sign in redirection
            Text(
                text = "Return to Navigation Base (Login)",
                color = OrionTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { viewModel.navigateTo(AuthScreen.Login) }
                    .padding(8.dp)
            )
        }
    }
}
