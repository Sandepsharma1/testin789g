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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.orignal.buddylynk.data.auth.AuthManager
import com.orignal.buddylynk.data.network.NetworkObserver
import com.orignal.buddylynk.navigation.BuddyLynkBottomBar
import com.orignal.buddylynk.navigation.BuddyLynkNavHost
import com.orignal.buddylynk.navigation.Screen
import com.orignal.buddylynk.ui.screens.NoConnectionScreen
import com.orignal.buddylynk.ui.theme.BuddylynkTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize AuthManager for session persistence
        AuthManager.init(this)
        
        enableEdgeToEdge()
        setContent {
            BuddylynkTheme(darkTheme = true) {
                BuddyLynkApp()
            }
        }
    }
}

@Composable
fun BuddyLynkApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Network connectivity state
    val isConnected by NetworkObserver.observeConnectivity(context).collectAsState(initial = true)
    var isRetrying by remember { mutableStateOf(false) }
    
    // Team inner view state (to hide bottom nav when inside team/channel)
    var isTeamInnerView by remember { mutableStateOf(false) }
    
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
        !isTeamInnerView
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Main App Content
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            bottomBar = { } // No scaffold bottom bar - we use floating dock
        ) { innerPadding ->
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                BuddyLynkNavHost(
                    navController = navController,
                    startDestination = Screen.Splash.route,
                    onTeamInnerViewChanged = { isTeamInnerView = it }
                )
                
                // Floating Dock Navigation (Premium Design)
                if (showBottomBar && isConnected) {
                    com.orignal.buddylynk.ui.components.FloatingDockNavigation(
                        currentRoute = currentRoute ?: "",
                        onNavigate = { route ->
                            if (route != currentRoute) {
                                navController.navigate(route) {
                                    // Pop up to home for clean navigation
                                    popUpTo(Screen.Home.route) {
                                        inclusive = (route == Screen.Home.route)
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = (route != Screen.Home.route)
                                }
                            }
                        },
                        modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
                    )
                }
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
}