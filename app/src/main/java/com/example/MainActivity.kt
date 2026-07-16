package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.SignUpScreen
import com.example.ui.screens.ForgotPasswordScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.MainScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.PremiumSubscriptionScreen
import com.example.ui.theme.OrionTheme
import com.example.ui.screens.AdMobInterstitialHelper
import com.example.viewmodel.AuthScreen
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.ChatViewModel
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
    private val chatViewModel: ChatViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        try {
            MobileAds.initialize(this) {}
            AdMobInterstitialHelper.loadAd(this)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error initializing MobileAds", e)
        }

        setContent {
            val themeName by chatViewModel.selectedTheme.collectAsStateWithLifecycle()
            OrionTheme(themeName = themeName) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val currentScreen by authViewModel.currentScreen.collectAsStateWithLifecycle()

                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(400)) togetherWith
                            fadeOut(animationSpec = tween(400))
                        },
                        label = "auth_screen_transition"
                    ) { screen ->
                        when (screen) {
                            is AuthScreen.Splash -> SplashScreen()
                            is AuthScreen.Login -> LoginScreen(viewModel = authViewModel)
                            is AuthScreen.SignUp -> SignUpScreen(viewModel = authViewModel)
                            is AuthScreen.ForgotPassword -> ForgotPasswordScreen(viewModel = authViewModel)
                            is AuthScreen.Profile -> ProfileScreen(viewModel = authViewModel)
                            is AuthScreen.MainApp -> MainScreen(
                                chatViewModel = chatViewModel,
                                authViewModel = authViewModel
                            )
                            is AuthScreen.Settings -> SettingsScreen(
                                viewModel = authViewModel,
                                chatViewModel = chatViewModel
                            )
                            is AuthScreen.Premium -> PremiumSubscriptionScreen(
                                viewModel = authViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

