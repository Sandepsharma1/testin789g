package com.orignal.buddylynk.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.orignal.buddylynk.ui.screens.*

// =============================================================================
// NAVIGATION ROUTES
// =============================================================================

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Search : Screen("search")
    object Create : Screen("create")
    object Messages : Screen("messages")
    object Profile : Screen("profile")
    
    // User profile - takes userId argument
    object UserProfile : Screen("user/{userId}") {
        fun createRoute(userId: String) = "user/$userId"
    }
    
    // Chat with user
    object Chat : Screen("chat/{conversationId}") {
        fun createRoute(conversationId: String) = "chat/$conversationId"
    }
    
    // Comments screen - takes postId argument
    object Comments : Screen("comments/{postId}") {
        fun createRoute(postId: String) = "comments/$postId"
    }
    
    // Activity feed
    object Activity : Screen("activity")
    
    // Settings screens
    object Settings : Screen("settings")
    object NotificationsSettings : Screen("settings/notifications")
    object PrivacySettings : Screen("settings/privacy")
    object AppearanceSettings : Screen("settings/appearance")
    object HelpSettings : Screen("settings/help")
    
    // Edit profile
    object EditProfile : Screen("profile/edit")
    
    // Feature screens
    object TeamUp : Screen("teamup")
    object Live : Screen("live")
    object Events : Screen("events")
    object GoLive : Screen("golive")
    object ChatList : Screen("chatlist")
    object AddStory : Screen("addstory")
    object BlockedUsers : Screen("settings/blocked")
    object SavedPosts : Screen("settings/saved")
    object Shorts : Screen("shorts")
    object Call : Screen("call")
    object CreatePost : Screen("createpost")
    
    // Group/Channel creation
    object CreateGroup : Screen("creategroup/{isChannel}") {
        fun createRoute(isChannel: Boolean) = "creategroup/$isChannel"
    }
    
    // Group chat
    object GroupChat : Screen("groupchat/{groupId}") {
        fun createRoute(groupId: String) = "groupchat/$groupId"
    }
    
    // Event Chat - chat room for a specific event
    object EventChat : Screen("eventchat/{eventId}/{title}/{date}/{time}/{location}") {
        fun createRoute(eventId: String, title: String, date: String, time: String, location: String): String {
            // URL encode the parameters to handle special characters
            val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
            val encodedDate = java.net.URLEncoder.encode(date, "UTF-8")
            val encodedTime = java.net.URLEncoder.encode(time, "UTF-8")
            val encodedLocation = java.net.URLEncoder.encode(location, "UTF-8")
            return "eventchat/$eventId/$encodedTitle/$encodedDate/$encodedTime/$encodedLocation"
        }
    }
    
    // Event Settings - manage event details, members, chat permissions
    object EventSettings : Screen("eventsettings/{eventId}") {
        fun createRoute(eventId: String) = "eventsettings/$eventId"
    }
    
    // Event Members - list of event members with admin actions
    object EventMembers : Screen("eventmembers/{eventId}") {
        fun createRoute(eventId: String) = "eventmembers/$eventId"
    }
    
    // Create Event form
    object CreateEvent : Screen("createevent")
    
    // OTT Video Streaming
    object OttHome : Screen("ott")
    object OttUpload : Screen("ott/upload")
    object OttProfile : Screen("ott/profile")
    object OttPlayer : Screen("ott/player/{videoId}") {
        fun createRoute(videoId: String) = "ott/player/$videoId"
    }
}

// =============================================================================
// NAVIGATION HOST - With premium transitions
// =============================================================================

@Composable
fun BuddyLynkNavHost(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route,
    onTeamInnerViewChanged: (Boolean) -> Unit = {},
    homeRefreshTrigger: Int = 0  // When this changes, HomeScreen should refresh
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn(
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + slideInHorizontally(
                initialOffsetX = { it / 4 },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            )
        },
        exitTransition = {
            fadeOut(
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            ) + slideOutHorizontally(
                targetOffsetX = { -it / 4 },
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            )
        },
        popEnterTransition = {
            fadeIn(
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + slideInHorizontally(
                initialOffsetX = { -it / 4 },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            )
        },
        popExitTransition = {
            fadeOut(
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            ) + slideOutHorizontally(
                targetOffsetX = { it / 4 },
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            )
        }
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }
        
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                },
                onNavigateToCreate = {
                    navController.navigate(Screen.Create.route)
                },
                onNavigateToTeamUp = {
                    navController.navigate(Screen.TeamUp.route)
                },
                onNavigateToLive = {
                    navController.navigate(Screen.Live.route)
                },
                onNavigateToEvents = {
                    navController.navigate(Screen.Events.route)
                },
                onNavigateToChat = {
                    navController.navigate(Screen.ChatList.route)
                },
                onNavigateToUserProfile = { userId ->
                    navController.navigate(Screen.UserProfile.createRoute(userId))
                },
                onNavigateToShorts = {
                    navController.navigate(Screen.Shorts.route)
                },
                refreshTrigger = homeRefreshTrigger  // Pass refresh trigger
            )
        }
        
        composable(Screen.Search.route) {
            PremiumSearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = { userId ->
                    navController.navigate("user/$userId")
                }
            )
        }
        
        // Shorts screen - TikTok/Reels style video feed
        composable(Screen.Shorts.route) {
            ShortsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = { userId -> navController.navigate(Screen.UserProfile.createRoute(userId)) },
                onNavigateToComments = { postId -> navController.navigate(Screen.Comments.createRoute(postId)) }
            )
        }
        
        // Comments screen - view and add comments to a post
        composable(
            route = Screen.Comments.route,
            arguments = listOf(
                androidx.navigation.navArgument("postId") {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
            CommentsScreen(
                postId = postId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = { userId -> navController.navigate(Screen.UserProfile.createRoute(userId)) }
            )
        }
        
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToEditProfile = { navController.navigate(Screen.EditProfile.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToNotifications = { navController.navigate(Screen.NotificationsSettings.route) },
                onNavigateToPrivacy = { navController.navigate(Screen.PrivacySettings.route) },
                onNavigateToAppearance = { navController.navigate(Screen.AppearanceSettings.route) },
                onNavigateToHelp = { navController.navigate(Screen.HelpSettings.route) }
            )
        }
        
        // User Profile screen (other users)
        composable(
            route = Screen.UserProfile.route,
            arguments = listOf(
                androidx.navigation.navArgument("userId") {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            UserProfileScreen(
                userId = userId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { conversationId ->
                    navController.navigate(Screen.Chat.createRoute(conversationId))
                }
            )
        }
        // Settings screen (main)
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToNotifications = { navController.navigate(Screen.NotificationsSettings.route) },
                onNavigateToPrivacy = { navController.navigate(Screen.PrivacySettings.route) },
                onNavigateToAppearance = { navController.navigate(Screen.AppearanceSettings.route) },
                onNavigateToHelp = { navController.navigate(Screen.HelpSettings.route) },
                onNavigateToBlockedUsers = { navController.navigate(Screen.BlockedUsers.route) },
                onNavigateToSavedPosts = { navController.navigate(Screen.SavedPosts.route) }
            )
        }
        
        // Edit Profile screen
        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Messages.route) {
            MessagesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Create.route) {
            CreatePostScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Settings screens
        composable(Screen.NotificationsSettings.route) {
            NotificationsSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.PrivacySettings.route) {
            PrivacySettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBlockedUsers = { navController.navigate(Screen.BlockedUsers.route) }
            )
        }
        
        composable(Screen.AppearanceSettings.route) {
            AppearanceSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.HelpSettings.route) {
            HelpScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Blocked Users screen
        composable(Screen.BlockedUsers.route) {
            BlockedUsersScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Saved Posts screen
        composable(Screen.SavedPosts.route) {
            SavedPostsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToComments = { postId ->
                    navController.navigate(Screen.Comments.createRoute(postId))
                },
                onNavigateToProfile = { userId ->
                    navController.navigate(Screen.UserProfile.createRoute(userId))
                }
            )
        }
        
        // User Profile screen - view other users
        composable(
            route = Screen.UserProfile.route,
            arguments = listOf(
                androidx.navigation.navArgument("userId") {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            UserProfileScreen(
                userId = userId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { chatUserId ->
                    navController.navigate(Screen.Chat.createRoute(chatUserId))
                }
            )
        }
        
        // Chat screen - real-time messaging
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                androidx.navigation.navArgument("conversationId") {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            PremiumInnerChatScreen(
                conversationId = conversationId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCall = { navController.navigate(Screen.Call.route) }
            )
        }
        
        // Activity screen
        composable(Screen.Activity.route) {
            ActivityScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = { userId ->
                    navController.navigate(Screen.UserProfile.createRoute(userId))
                },
                onNavigateToPost = { postId ->
                    // TODO: Navigate to post detail
                }
            )
        }
        
        // Edit Profile screen
        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() }
            )
        }
        
        // Feature screens
        composable(Screen.TeamUp.route) {
            PremiumTeamUpScreen(
                onNavigateBack = { navController.popBackStack() },
                onInnerViewChanged = onTeamInnerViewChanged,
                onCreateGroup = { isChannel ->
                    navController.navigate(Screen.CreateGroup.createRoute(isChannel))
                }
            )
        }
        
        composable(Screen.Live.route) {
            LiveScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToGoLive = { navController.navigate(Screen.GoLive.route) }
            )
        }
        
        composable(Screen.Events.route) {
            EventsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEventChat = { eventId, title, date, time, location ->
                    navController.navigate(Screen.EventChat.createRoute(eventId, title, date, time, location))
                },
                onNavigateToCreateEvent = {
                    navController.navigate(Screen.CreateEvent.route)
                }
            )
        }
        
        // Create Event Screen - form to create event
        composable(Screen.CreateEvent.route) {
            CreateEventScreen(
                onNavigateBack = { navController.popBackStack() },
                onEventCreated = { eventId, title, date, time, location ->
                    // Navigate back to events list after creation
                    navController.popBackStack()
                }
            )
        }
        
        // Event Chat Screen - chat room for a specific event
        composable(
            route = Screen.EventChat.route,
            arguments = listOf(
                androidx.navigation.navArgument("eventId") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("title") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("date") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("time") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("location") { type = androidx.navigation.NavType.StringType }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
            val title = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("title") ?: "", "UTF-8")
            val date = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("date") ?: "", "UTF-8")
            val time = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("time") ?: "", "UTF-8")
            val location = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("location") ?: "", "UTF-8")
            EventChatScreen(
                eventId = eventId,
                eventTitle = title,
                eventDate = date,
                eventTime = time,
                eventLocation = location,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(Screen.EventSettings.createRoute(eventId)) }
            )
        }
        
        // Event Settings Screen - manage event details, members, chat permissions
        composable(
            route = Screen.EventSettings.route,
            arguments = listOf(
                androidx.navigation.navArgument("eventId") { type = androidx.navigation.NavType.StringType }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
            EventSettingsScreen(
                eventId = eventId,
                onBack = { navController.popBackStack() },
                onNavigateToMembers = { navController.navigate(Screen.EventMembers.createRoute(eventId)) }
            )
        }
        
        // Event Members Screen - list of event members with admin actions
        composable(
            route = Screen.EventMembers.route,
            arguments = listOf(
                androidx.navigation.navArgument("eventId") { type = androidx.navigation.NavType.StringType }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
            EventMembersListScreen(
                eventId = eventId,
                onBack = { navController.popBackStack() },
                onNavigateToChat = { userId ->
                    navController.navigate(Screen.Chat.createRoute(userId))
                }
            )
        }
        
        // Go Live Screen
        composable(Screen.GoLive.route) {
            GoLiveScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Chat List Screen
        composable(Screen.ChatList.route) {
            PremiumChatListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { userId ->
                    navController.navigate(Screen.Chat.createRoute(userId))
                },
                onCreateGroup = { isChannel ->
                    navController.navigate(Screen.CreateGroup.createRoute(isChannel))
                }
            )
        }
        
        // Add Story Screen
        composable(Screen.AddStory.route) {
            AddStoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Blocked Users Screen
        composable(Screen.BlockedUsers.route) {
            BlockedUsersScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Call Screen
        composable(Screen.Call.route) {
            CallScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Create Post Screen
        composable(Screen.CreatePost.route) {
            CreatePostScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Create Group/Channel Screen
        composable(
            route = Screen.CreateGroup.route,
            arguments = listOf(
                androidx.navigation.navArgument("isChannel") {
                    type = androidx.navigation.NavType.BoolType
                }
            )
        ) { backStackEntry ->
            val isChannel = backStackEntry.arguments?.getBoolean("isChannel") ?: false
            CreateGroupScreen(
                isChannel = isChannel,
                onNavigateBack = { navController.popBackStack() },
                onGroupCreated = { groupId ->
                    navController.popBackStack()
                    // Could navigate to group chat here
                }
            )
        }
        
        // OTT Home Screen
        composable(Screen.OttHome.route) {
            OttHomeScreen(
                onNavigateBack = { navController.popBackStack() },
                onVideoClick = { videoId ->
                    navController.navigate(Screen.OttPlayer.createRoute(videoId))
                },
                onUploadClick = {
                    navController.navigate(Screen.OttUpload.route)
                },
                onSearchClick = {
                    // TODO: OTT search
                },
                onProfileClick = {
                    navController.navigate(Screen.OttProfile.route)
                }
            )
        }
        
        // OTT Video Player Screen
        composable(
            route = Screen.OttPlayer.route,
            arguments = listOf(
                androidx.navigation.navArgument("videoId") {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
            OttVideoPlayerScreen(
                videoId = videoId,
                onNavigateBack = { navController.popBackStack() },
                onCreatorClick = { creatorId ->
                    navController.navigate(Screen.UserProfile.createRoute(creatorId))
                }
            )
        }
        
        // OTT Upload Screen
        composable(Screen.OttUpload.route) {
            OttUploadScreen(
                onNavigateBack = { navController.popBackStack() },
                onUploadSuccess = {
                    navController.popBackStack()
                }
            )
        }
        
        // OTT Profile Screen - Uploads, Saved, Watched
        composable(Screen.OttProfile.route) {
            OttProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onVideoClick = { videoId ->
                    navController.navigate(Screen.OttPlayer.createRoute(videoId))
                },
                onUploadClick = {
                    navController.navigate(Screen.OttUpload.route)
                }
            )
        }
    }
}

// =============================================================================
// BOTTOM NAVIGATION BAR
// =============================================================================

@Composable
fun BuddyLynkBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onMenuClick: () -> Unit = {}
) {
    val items = listOf(
        NavItem(Screen.Home.route, Icons.Outlined.Home, Icons.Filled.Home, "Home"),
        NavItem(Screen.Search.route, Icons.Outlined.Search, Icons.Filled.Search, "Search"),
        NavItem(Screen.Shorts.route, Icons.Outlined.SlowMotionVideo, Icons.Filled.SlowMotionVideo, "Shorts"),
        NavItem(Screen.TeamUp.route, Icons.Outlined.Groups, Icons.Filled.Groups, "TeamUp")
    )
    
    // Full width nav bar with no gap
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1025).copy(alpha = 0.98f),
                        Color(0xFF0D0810).copy(alpha = 0.99f)
                    )
                )
            )
    ) {
        // Top border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF6B21A8).copy(alpha = 0.3f))
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                
                // Regular nav item - icons only
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(item.route) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) 
                            Color(0xFF00D9FF) 
                        else 
                            Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            // Menu button (Extras)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onMenuClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Menu,
                    contentDescription = "Extras",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

private data class NavItem(
    val route: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val label: String
)

// =============================================================================
// SHORTS PLACEHOLDER SCREEN
// =============================================================================

@Composable
private fun ShortsPlaceholderScreen(
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0A0A),
                        Color(0xFF1A1025),
                        Color(0xFF0A0A0A)
                    )
                )
            )
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Animated icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF6366F1),
                                Color(0xFF8B5CF6),
                                Color(0xFFA855F7)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Shorts",
                    tint = Color.White,
                    modifier = Modifier.size(50.dp)
                )
            }
            
            Text(
                text = "Shorts",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                text = "Coming Soon",
                fontSize = 16.sp,
                color = Color(0xFF00D9FF),
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "Short videos feature is under development",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}
