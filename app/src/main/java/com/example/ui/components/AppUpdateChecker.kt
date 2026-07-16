package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.theme.OrionPrimary
import com.example.ui.theme.OrionSurface
import com.example.ui.theme.OrionTextPrimary
import com.example.ui.theme.OrionTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppUpdateConfig {
    const val CURRENT_VERSION_NAME = "1.0.0"
    const val CURRENT_VERSION_CODE = 1
    
    // In production, these would be fetched from Firestore or a remote endpoint
    var LATEST_VERSION_NAME = "1.1.0"
    var LATEST_VERSION_CODE = 2
    var IS_FORCE_UPDATE = false
    var UPDATE_URL = "https://play.google.com/store/apps/details?id=com.orion.ai"
}

@Composable
fun AppUpdateChecker(
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        // Simulate a network delay for update checking
        withContext(Dispatchers.IO) {
            kotlinx.coroutines.delay(1500)
        }
        if (AppUpdateConfig.LATEST_VERSION_CODE > AppUpdateConfig.CURRENT_VERSION_CODE) {
            showDialog = true
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { 
                if (!AppUpdateConfig.IS_FORCE_UPDATE) {
                    showDialog = false
                    onDismiss()
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AppUpdateConfig.UPDATE_URL))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open update link", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrionPrimary)
                ) {
                    Text("UPDATE NOW")
                }
            },
            dismissButton = {
                if (!AppUpdateConfig.IS_FORCE_UPDATE) {
                    TextButton(
                        onClick = {
                            showDialog = false
                            onDismiss()
                        }
                    ) {
                        Text("LATER", color = OrionTextSecondary)
                    }
                }
            },
            title = {
                Text(
                    text = "New Star Signal: Update Available",
                    color = OrionTextPrimary
                )
            },
            text = {
                Text(
                    text = "A new version of Orion AI Companion (v${AppUpdateConfig.LATEST_VERSION_NAME}) is available on Google Play. Update to enjoy improved speed, enhanced stability, and new constellation modules.",
                    color = OrionTextSecondary
                )
            },
            containerColor = OrionSurface,
            shape = MaterialTheme.shapes.large
        )
    }
}
