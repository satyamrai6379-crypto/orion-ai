package com.example.ui.screens

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
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
import com.android.billingclient.api.*
import com.example.ui.theme.*
import com.example.viewmodel.AuthScreen
import com.example.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumSubscriptionScreen(viewModel: AuthViewModel) {
    val context = LocalContext.current
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    
    // Play Billing state variables
    var billingClient by remember { mutableStateOf<BillingClient?>(null) }
    var billingStatus by remember { mutableStateOf("Initializing orbit...") }
    var premiumProductDetails by remember { mutableStateOf<ProductDetails?>(null) }
    var isBillingConnected by remember { mutableStateOf(false) }
    
    // UI state
    var selectedPlanIndex by remember { mutableIntStateOf(1) } // 0: Free, 1: Premium
    var showSuccessUpgradeDialog by remember { mutableStateOf(false) }

    // Initialize Play Billing Client
    LaunchedEffect(Unit) {
        val listener = PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        viewModel.setPremiumStatus(true)
                        showSuccessUpgradeDialog = true
                        Toast.makeText(context, "Celestial Starlight Unlocked!", Toast.LENGTH_LONG).show()
                    }
                }
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                Toast.makeText(context, "Upgrade sequence cancelled by user", Toast.LENGTH_SHORT).show()
            } else {
                Log.e("PlayBilling", "Billing error: ${billingResult.debugMessage}")
            }
        }

        try {
            val client = BillingClient.newBuilder(context)
                .setListener(listener)
                .enablePendingPurchases()
                .build()
            
            billingClient = client
            
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        isBillingConnected = true
                        billingStatus = "Synchronized with Google Play"
                        
                        // Query for subscription product
                        val productList = listOf(
                            QueryProductDetailsParams.Product.newBuilder()
                                .setProductId("orion_premium_starlight")
                                .setProductType(BillingClient.ProductType.SUBS)
                                .build()
                        )
                        
                        val params = QueryProductDetailsParams.newBuilder()
                            .setProductList(productList)
                            .build()
                        
                        client.queryProductDetailsAsync(params) { result, detailsList ->
                            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                                premiumProductDetails = detailsList.firstOrNull()
                            } else {
                                Log.e("PlayBilling", "Query product details failed: ${result.debugMessage}")
                            }
                        }
                    } else {
                        isBillingConnected = false
                        billingStatus = "Play services unavailable. Running Sandbox bypass mode."
                    }
                }

                override fun onBillingServiceDisconnected() {
                    isBillingConnected = false
                    billingStatus = "Disconnected from Play Services. Sandbox enabled."
                }
            })
        } catch (e: Exception) {
            isBillingConnected = false
            billingStatus = "Sandbox Active (Offline Mode)"
            Log.e("PlayBilling", "Failed to init Play Billing", e)
        }
    }

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
                        text = "Orion Starlight Upgrade",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AuthScreen.Settings) }) {
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
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Visual Brand
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(OrionPrimary, OrionBackground)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Text(
                        text = "UNLEASH STARLIGHT POWER",
                        color = OrionPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Elevate Orion AI Companion to infinite speeds, ad-free analytics, and higher reasoning engines.",
                        color = OrionTextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                // Service telemetry indicator
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = OrionSurface,
                    border = BorderStroke(1.dp, OrionSurfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isBillingConnected) OrionTertiary else OrionSecondary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Billing Node: $billingStatus",
                            color = OrionTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Billing options layout
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Plan 1: Free
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (selectedPlanIndex == 0) 2.dp else 1.dp,
                                color = if (selectedPlanIndex == 0) OrionSecondary else OrionSurfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedPlanIndex = 0 },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedPlanIndex == 0) OrionSurface.copy(alpha = 0.6f) else OrionSurface
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPlanIndex == 0,
                                onClick = { selectedPlanIndex = 0 },
                                colors = RadioButtonDefaults.colors(selectedColor = OrionSecondary)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Orion Free Orbit",
                                    color = OrionTextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Access standard chat models. Interstitial and Banner advertisements active.",
                                    color = OrionTextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$0.00",
                                    color = OrionTextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "forever",
                                    color = OrionTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Plan 2: Premium (Starlight Upgrade)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (selectedPlanIndex == 1) 2.dp else 1.dp,
                                color = if (selectedPlanIndex == 1) OrionPrimary else OrionSurfaceVariant.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedPlanIndex = 1 },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedPlanIndex == 1) OrionPrimary.copy(alpha = 0.05f) else OrionSurface
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            // Badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .clip(RoundedCornerShape(bottomStart = 12.dp))
                                    .background(OrionPrimary)
                                    .padding(vertical = 4.dp, horizontal = 12.dp)
                            ) {
                                Text(
                                    text = "POPULAR",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }

                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedPlanIndex == 1,
                                    onClick = { selectedPlanIndex = 1 },
                                    colors = RadioButtonDefaults.colors(selectedColor = OrionPrimary)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Orion Premium Starlight",
                                        color = OrionPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Zero ads. Access analytical Gemini Pro models. High-speed server allocation.",
                                        color = OrionTextSecondary,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$4.99",
                                        color = OrionPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "monthly",
                                        color = OrionTextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Premium benefits list
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
                            text = "STARLIGHT PRIVILEGES INCLUDED",
                            color = OrionPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )

                        BenefitItem(title = "Completely Ad-Free Experience", desc = "All banner and full-screen interstitials deactivated.")
                        BenefitItem(title = "Gemini 3.1 Pro Analytical Access", desc = "Leverage advanced reasoning engines.")
                        BenefitItem(title = "Priority Star Response Speeds", desc = "Sub-second text completions.")
                        BenefitItem(title = "Custom Gold Star Insignia Profile", desc = "Distinctive celestial badges on user panels.")
                    }
                }

                // Upgrade action button
                Button(
                    onClick = {
                        if (selectedPlanIndex == 0) {
                            // Demote to free
                            viewModel.setPremiumStatus(false)
                            Toast.makeText(context, "Free Orbit mode active. Ad servers re-aligned.", Toast.LENGTH_LONG).show()
                        } else {
                            if (isBillingConnected && premiumProductDetails != null) {
                                // Launch real Play Billing Flow
                                val productDetailsParamsList = listOf(
                                    BillingFlowParams.ProductDetailsParams.newBuilder()
                                        .setProductDetails(premiumProductDetails!!)
                                        .build()
                                )
                                val billingFlowParams = BillingFlowParams.newBuilder()
                                    .setProductDetailsParamsList(productDetailsParamsList)
                                    .build()
                                
                                val activity = context as? Activity
                                if (activity != null) {
                                    billingClient?.launchBillingFlow(activity, billingFlowParams)
                                }
                            } else {
                                // Fallback Sandbox transaction bypass
                                viewModel.setPremiumStatus(true)
                                showSuccessUpgradeDialog = true
                                Toast.makeText(context, "Sandbox mode: Premium Starlight purchased successfully!", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("subscribe_plan_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedPlanIndex == 1) OrionPrimary else OrionSecondary
                    )
                ) {
                    Text(
                        text = if (selectedPlanIndex == 1) "ACTIVATE STARLIGHT UPGRADE ($4.99/mo)" else "CONFIRM FREE ORBIT ACCESS",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "Subscription manages automatically. Cancel anytime in Google Play Store.",
                    color = OrionTextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // Success dialog
    if (showSuccessUpgradeDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessUpgradeDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSuccessUpgradeDialog = false
                        viewModel.navigateTo(AuthScreen.MainApp)
                    }
                ) {
                    Text("LAUNCH STARLIGHT CHAT", color = OrionPrimary)
                }
            },
            title = {
                Text(
                    text = "Celestial Starlight Activated!",
                    color = OrionTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Welcome to the high-tier constellation. Your profile insignia is now active, and ads have been permanently dissolved. Connect to our fastest models seamlessly.",
                    color = OrionTextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            containerColor = OrionSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun BenefitItem(title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = OrionTertiary,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                color = OrionTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = desc,
                color = OrionTextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}
