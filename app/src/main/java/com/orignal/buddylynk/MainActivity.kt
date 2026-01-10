package com.orignal.buddylynk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.orignal.buddylynk.data.auth.AuthManager
import com.orignal.buddylynk.data.network.NetworkObserver
import com.orignal.buddylynk.ui.components.FloatingDockNavigation
import com.orignal.buddylynk.navigation.BuddyLynkNavHost
import com.orignal.buddylynk.navigation.Screen
import com.orignal.buddylynk.ui.screens.NoConnectionScreen
import com.orignal.buddylynk.ui.theme.BuddylynkTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    companion object {
        // Pending deep link to be processed after login
        var pendingDeepLink: android.net.Uri? = null
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize AuthManager for session persistence
        AuthManager.init(this)
        
        // Initialize SensitiveContentManager for blur/hide preferences
        com.orignal.buddylynk.data.settings.SensitiveContentManager.init(this)
        
        // Initialize LikedPostsManager for local like persistence
        com.orignal.buddylynk.data.settings.LikedPostsManager.init(this)
        
        // Initialize Google Certified CMP (Consent Management Platform) for GDPR
        // Then initialize AdMob after consent is obtained
        com.orignal.buddylynk.data.ads.ConsentManager.initialize(this) {
            // Consent obtained or not required, now initialize ads
            com.orignal.buddylynk.data.ads.AdMobManager.initialize(this)
        }
        
        // Handle deep link from launch intent
        handleDeepLink(intent)
        
        enableEdgeToEdge()
        setContent {
            BuddylynkTheme(darkTheme = true) {
                BuddyLynkApp()
            }
        }
    }
    
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }
    
    private fun handleDeepLink(intent: android.content.Intent?) {
        val data = intent?.data
        if (data != null) {
            android.util.Log.d("MainActivity", "Deep link received: $data")
            android.util.Log.d("MainActivity", "Host: ${data.host}, Path: ${data.path}")
            pendingDeepLink = data
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuddyLynkApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Network connectivity state
    val isConnected by NetworkObserver.observeConnectivity(context).collectAsState(initial = true)
    var isRetrying by remember { mutableStateOf(false) }
    
    // Server status observer
    val serverStatus by com.orignal.buddylynk.data.network.ServerHealthObserver.serverStatus.collectAsState()
    val isServerOnline = serverStatus.isOnline()
    
    // Team inner view state (to hide bottom nav when inside team/channel)
    var isTeamInnerView by remember { mutableStateOf(false) }
    
    // Menu bottom sheet state
    var showMenuSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Handle retry
    LaunchedEffect(isRetrying) {
        if (isRetrying) {
            delay(2000)
            isRetrying = false
        }
    }
    
    // Hide bottom bar on splash, login, register, shorts, and fullscreen screens
    val showBottomBar = currentRoute != null && 
        currentRoute != Screen.Splash.route &&
        currentRoute != Screen.Login.route &&
        currentRoute != Screen.Register.route &&
        currentRoute != Screen.GoLive.route &&
        currentRoute != Screen.Events.route &&
        currentRoute != Screen.AddStory.route &&
        currentRoute != Screen.Chat.route &&
        currentRoute != Screen.Shorts.route &&
        currentRoute != Screen.OttHome.route &&  // Hide for OTT screen
        !currentRoute.startsWith("ott/") &&  // Hide for all OTT sub-screens
        !currentRoute.startsWith("eventchat") &&  // Hide for event chat inner view
        !isTeamInnerView
    
    // Track home refresh trigger - incremented when user double-taps home icon
    var homeRefreshTrigger by remember { mutableStateOf(0) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Main App Content
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                if (showBottomBar && isConnected && isServerOnline) {
                    FloatingDockNavigation(
                        currentRoute = currentRoute ?: "",
                        onNavigate = { route ->
                        if (route != currentRoute) {
                            navController.navigate(route) {
                                // Keep Home in the stack - never pop it
                                popUpTo(Screen.Home.route) {
                                    inclusive = false  // NEVER destroy Home
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true  // Always restore state
                            }
                        }
                        },
                        onMenuClick = { showMenuSheet = true },
                        onHomeDoubleTap = {
                            // Increment trigger to notify HomeScreen to refresh
                            homeRefreshTrigger++
                            android.util.Log.d("MainActivity", "Home double-tap! Refresh trigger: $homeRefreshTrigger")
                        }
                    )
                }
            }
        ) { _ ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                BuddyLynkNavHost(
                    navController = navController,
                    startDestination = Screen.Splash.route,
                    onTeamInnerViewChanged = { isTeamInnerView = it },
                    homeRefreshTrigger = homeRefreshTrigger  // Pass to NavHost
                )
            }
        }
        
        // No Connection Overlay
        AnimatedVisibility(
            visible = !isConnected,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            NoConnectionScreen(
                onRetry = { isRetrying = true },
                isRetrying = isRetrying
            )
        }
    }
    
    // Menu Bottom Sheet
    if (showMenuSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMenuSheet = false },
            containerColor = Color(0xFF1A1A2E),
            contentColor = Color.White,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.3f), androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Header with title and close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = "Extras",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    IconButton(onClick = { showMenuSheet = false }) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                // 1. Chat Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showMenuSheet = false
                            navController.navigate(Screen.Messages.route)
                        }
                        .padding(vertical = 16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Outlined.Forum,
                        contentDescription = "Chat",
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Chat",
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
                
                // 2. Events Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showMenuSheet = false
                            navController.navigate(Screen.Events.route)
                        }
                        .padding(vertical = 16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = "Events",
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Event",
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
                
                // 3. Buddylynk's OTT Option (with minimalist logo)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showMenuSheet = false
                            navController.navigate(Screen.OttHome.route)
                        }
                        .padding(vertical = 16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Minimalist Buddylynk Logo
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))
                                ),
                                androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Text(
                            text = "B",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Buddylynk's OTT",
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
                
                // 4. Settings Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showMenuSheet = false
                            navController.navigate(Screen.Settings.route)
                        }
                        .padding(vertical = 16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Settings",
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}