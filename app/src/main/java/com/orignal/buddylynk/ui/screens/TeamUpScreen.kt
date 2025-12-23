package com.orignal.buddylynk.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orignal.buddylynk.ui.components.*
import com.orignal.buddylynk.ui.theme.*

/**
 * TeamUpScreen - Find and join teams for collaboration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamUpScreen(
    onNavigateBack: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Gaming", "Music", "Sports", "Study", "Work", "Creative")
    var showCreateMenu by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showCreateChannelDialog by remember { mutableStateOf(false) }
    
    // Create Group Dialog
    if (showCreateGroupDialog) {
        CreateTeamDialog(
            title = "Create Group",
            subtitle = "Groups are for private teams",
            icon = Icons.Filled.Group,
            gradientColors = listOf(GradientPurple, GradientPink),
            onDismiss = { showCreateGroupDialog = false },
            onCreate = { name, description ->
                // TODO: Create group logic
                showCreateGroupDialog = false
            }
        )
    }
    
    // Create Channel Dialog
    if (showCreateChannelDialog) {
        CreateTeamDialog(
            title = "Create Channel",
            subtitle = "Channels are public communities",
            icon = Icons.Filled.Forum,
            gradientColors = listOf(GradientCyan, GradientPurple),
            onDismiss = { showCreateChannelDialog = false },
            onCreate = { name, description ->
                // TODO: Create channel logic
                showCreateChannelDialog = false
            }
        )
    }
    
    AnimatedGradientBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                
                Text(
                    text = "Team Up",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                
                // + Button with dropdown
                Box {
                    IconButton(onClick = { showCreateMenu = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Create Team",
                            tint = TextPrimary
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showCreateMenu,
                        onDismissRequest = { showCreateMenu = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        DropdownMenuItem(
                            text = { 
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Brush.linearGradient(listOf(GradientPurple, GradientPink))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.Group, 
                                            null, 
                                            tint = Color.White, 
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text("Create Group", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                        Text("Private team", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                }
                            },
                            onClick = { 
                                showCreateMenu = false
                                showCreateGroupDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { 
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Brush.linearGradient(listOf(GradientCyan, GradientPurple))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.Forum, 
                                            null, 
                                            tint = Color.White, 
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text("Create Channel", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                        Text("Public community", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                }
                            },
                            onClick = { 
                                showCreateMenu = false
                                showCreateChannelDialog = true
                            }
                        )
                    }
                }
            }
            
            // Categories
            LazyRow(
                modifier = Modifier.padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GradientPurple,
                            selectedLabelColor = Color.White,
                            containerColor = GlassWhite,
                            labelColor = TextSecondary
                        )
                    )
                }
            }
            
            // Teams List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Empty State - No teams yet
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Groups,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Teams Yet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Create a team or join one to collaborate with buddies",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        GradientButton(
                            text = "Create Team",
                            onClick = { },
                            gradient = VibrantGradient
                        )
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun TeamCard(
    name: String,
    category: String,
    members: Int,
    isOpen: Boolean,
    gradient: List<Color>
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Team Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Groups,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Team Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                    Text("•", color = TextTertiary)
                    Text(
                        text = "$members members",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }
            
            // Join Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isOpen) Brush.linearGradient(gradient)
                        else Brush.linearGradient(listOf(GlassWhite, GlassWhite))
                    )
                    .clickable { }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isOpen) "Join" else "Request",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isOpen) Color.White else TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Dialog for creating a new Group or Channel
 */
@Composable
private fun CreateTeamDialog(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradientColors: List<Color>,
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(gradientColors)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Column {
                    Text(title, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("Enter name...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = gradientColors.first(),
                        focusedLabelColor = gradientColors.first(),
                        cursorColor = gradientColors.first(),
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    placeholder = { Text("What's it about?") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = gradientColors.first(),
                        focusedLabelColor = gradientColors.first(),
                        cursorColor = gradientColors.first(),
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (name.isNotBlank()) Brush.linearGradient(gradientColors)
                        else Brush.linearGradient(listOf(GlassWhite, GlassWhite))
                    )
                    .clickable(enabled = name.isNotBlank()) { onCreate(name, description) }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    "Create",
                    color = if (name.isNotBlank()) Color.White else TextTertiary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
