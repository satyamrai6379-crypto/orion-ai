package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.viewmodel.AuthScreen
import com.example.viewmodel.AuthViewModel

@Composable
fun SignUpScreen(viewModel: AuthViewModel) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Star logo/constellation container
            Box(
                modifier = Modifier.size(90.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(OrionSecondary.copy(alpha = 0.2f), Color.Transparent)
                            )
                        )
                )
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = OrionSecondary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Initiate Coordinates",
                color = OrionTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

            Text(
                text = "Establish your signature profile in the Orion registry",
                color = OrionTextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Notifications/Errors State Box
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF7F1D1D)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearFeedback() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Input Fields Card
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
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "EXPLORER PROFILE CREATION",
                        color = OrionPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )

                    // Full Name Field
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Explorer Full Name", color = OrionTextSecondary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = OrionSecondary
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = OrionBackground,
                            unfocusedContainerColor = OrionBackground,
                            focusedBorderColor = OrionSecondary,
                            unfocusedBorderColor = OrionSurfaceVariant,
                            focusedTextColor = OrionTextPrimary,
                            unfocusedTextColor = OrionTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("name_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        )
                    )

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address", color = OrionTextSecondary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = OrionSecondary
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = OrionBackground,
                            unfocusedContainerColor = OrionBackground,
                            focusedBorderColor = OrionSecondary,
                            unfocusedBorderColor = OrionSurfaceVariant,
                            focusedTextColor = OrionTextPrimary,
                            unfocusedTextColor = OrionTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_email_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Access Key (Min 6 chars)", color = OrionTextSecondary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = OrionSecondary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password visibility",
                                    tint = OrionTextSecondary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = OrionBackground,
                            unfocusedContainerColor = OrionBackground,
                            focusedBorderColor = OrionSecondary,
                            unfocusedBorderColor = OrionSurfaceVariant,
                            focusedTextColor = OrionTextPrimary,
                            unfocusedTextColor = OrionTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("signup_password_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        )
                    )

                    // Confirm Password Field
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Access Key", color = OrionTextSecondary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.EnhancedEncryption,
                                contentDescription = null,
                                tint = OrionSecondary
                            )
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = OrionBackground,
                            unfocusedContainerColor = OrionBackground,
                            focusedBorderColor = OrionSecondary,
                            unfocusedBorderColor = OrionSurfaceVariant,
                            focusedTextColor = OrionTextPrimary,
                            unfocusedTextColor = OrionTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_password_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                viewModel.signUp(name, email, password, confirmPassword)
                            }
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Create Account Action Button
            Button(
                onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    viewModel.signUp(name, email, password, confirmPassword)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sign_up_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(OrionSecondary, OrionPrimary)
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
                            text = "ESTABLISH CORRIDORS (SIGN UP)",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Back to login redirection
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Already registered? ",
                    color = OrionTextSecondary,
                    fontSize = 13.sp
                )
                Text(
                    text = "Sign in here",
                    color = OrionSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { viewModel.navigateTo(AuthScreen.Login) }
                        .padding(4.dp)
                )
            }
        }
    }
}
