package com.orignal.buddylynk.ui.screens

import android.net.Uri
import com.orignal.buddylynk.ui.components.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.orignal.buddylynk.data.api.ApiService
import com.orignal.buddylynk.ui.viewmodel.EventsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

// Colors
private val DarkBg = Color(0xFF050505)
private val ZincDark = Color(0xFF0A0A0A)
private val Zinc800 = Color(0xFF27272A)
private val Zinc700 = Color(0xFF3F3F46)
private val Zinc600 = Color(0xFF52525B)
private val Zinc500 = Color(0xFF71717A)
private val Zinc400 = Color(0xFFA1A1AA)
private val OrangeAccent = Color(0xFFF97316)
private val PinkAccent = Color(0xFFEC4899)
private val PurpleAccent = Color(0xFF8B5CF6)
private val CyanAccent = Color(0xFF22D3EE)

// Event Categories with emoji and colors
data class EventCategory(
    val name: String,
    val emoji: String,
    val color: Color
)

val eventCategories = listOf(
    EventCategory("Music", "🎵", Color(0xFFEC4899)),
    EventCategory("Tech", "💻", Color(0xFF6366F1)),
    EventCategory("Gaming", "🎮", Color(0xFF10B981)),
    EventCategory("Education", "📚", Color(0xFFF59E0B)),
    EventCategory("Fitness", "🏋️", Color(0xFFEF4444)),
    EventCategory("Food", "🍕", Color(0xFFF97316)),
    EventCategory("Art", "🎨", Color(0xFF8B5CF6)),
    EventCategory("Business", "💼", Color(0xFF3B82F6)),
    EventCategory("Social", "🎉", Color(0xFFEC4899)),
    EventCategory("Sports", "⚽", Color(0xFF22C55E)),
    EventCategory("Other", "📌", Color(0xFF71717A))
)

// Popular Games for Gaming Events
data class GameOption(
    val name: String,
    val emoji: String,
    val color: Color
)

val popularGames = listOf(
    GameOption("Free Fire", "🔥", Color(0xFFF97316)),
    GameOption("PUBG/BGMI", "🎯", Color(0xFFFBBF24)),
    GameOption("Call of Duty", "💀", Color(0xFFEF4444)),
    GameOption("Valorant", "⚔️", Color(0xFFEF4444)),
    GameOption("Fortnite", "🏝️", Color(0xFF3B82F6)),
    GameOption("Minecraft", "⛏️", Color(0xFF22C55E)),
    GameOption("GTA V", "🚗", Color(0xFFA855F7)),
    GameOption("Apex Legends", "🦾", Color(0xFFDC2626)),
    GameOption("Clash Royale", "👑", Color(0xFF6366F1)),
    GameOption("Other", "🎮", Color(0xFF71717A))
)

/**
 * Create Event Screen - Premium form to create a new event
 * Features: Date/Time picker, Cover image, Categories, Online toggle
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    onNavigateBack: () -> Unit,
    onEventCreated: (eventId: String, title: String, date: String, time: String, location: String) -> Unit,
    eventsViewModel: EventsViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Form state
    var title by remember { mutableStateOf("") }
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var selectedHour by remember { mutableStateOf(19) } // Default 7 PM
    var selectedMinute by remember { mutableStateOf(0) }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isOnline by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(eventCategories[0]) }
    var isCreating by remember { mutableStateOf(false) }
    
    // Gaming Profile state (for Gaming category)
    var gamerUsername by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var zipCode by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var selectedGame by remember { mutableStateOf<GameOption?>(null) }
    var customGameName by remember { mutableStateOf("") }
    
    // Popup state
    var showSuccessPopup by remember { mutableStateOf(false) }
    var showErrorPopup by remember { mutableStateOf(false) }
    var popupMessage by remember { mutableStateOf("") }
    var createdEventData by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    
    // Cover image state
    var coverImageUri by remember { mutableStateOf<Uri?>(null) }
    var uploadedImageUrl by remember { mutableStateOf<String?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }
    
    // Dialog states
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    // Date picker state
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    
    // Time picker state
    val timePickerState = rememberTimePickerState(
        initialHour = selectedHour,
        initialMinute = selectedMinute,
        is24Hour = false
    )
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        coverImageUri = uri
        // Upload image immediately after selection
        uri?.let { selectedUri ->
            isUploadingImage = true
            coroutineScope.launch {
                try {
                    val contentType = context.contentResolver.getType(selectedUri) ?: "image/jpeg"
                    val filename = "event_cover_${System.currentTimeMillis()}.jpg"
                    
                    val presignedResult = withContext(Dispatchers.IO) {
                        ApiService.getPresignedUrl(filename, contentType, "events")
                    }
                    
                    presignedResult.onSuccess { presignedJson ->
                        val uploadUrl = presignedJson.optString("uploadUrl").ifEmpty { presignedJson.optString("presignedUrl") }
                        val fileUrl = presignedJson.optString("fileUrl")
                        
                        if (uploadUrl.isEmpty()) {
                            isUploadingImage = false
                            return@onSuccess
                        }
                        
                        // Upload to S3 using streaming to prevent OOM
                        val uploadResult = withContext(Dispatchers.IO) {
                            ApiService.uploadToPresignedUrl(uploadUrl, selectedUri, context.contentResolver, contentType)
                        }
                        
                        uploadResult.onSuccess {
                            uploadedImageUrl = fileUrl
                            popupMessage = "Image uploaded!"
                            showSuccessPopup = true
                        }.onFailure {
                            popupMessage = "Upload failed"
                            showErrorPopup = true
                            coverImageUri = null
                        }
                    }.onFailure {
                        popupMessage = "Failed to get upload URL"
                        showErrorPopup = true
                        coverImageUri = null
                    }
                } catch (e: Exception) {
                    popupMessage = "Error: ${e.message}"
                    showErrorPopup = true
                    coverImageUri = null
                } finally {
                    isUploadingImage = false
                }
            }
        }
    }
    
    // Format date for display
    val formattedDate = remember(selectedDateMillis) {
        selectedDateMillis?.let {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            sdf.format(Date(it))
        } ?: ""
    }
    
    // Format time for display
    val formattedTime = remember(selectedHour, selectedMinute) {
        val hour12 = if (selectedHour == 0) 12 else if (selectedHour > 12) selectedHour - 12 else selectedHour
        val amPm = if (selectedHour < 12) "AM" else "PM"
        String.format("%d:%02d %s", hour12, selectedMinute, amPm)
    }
    
    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDateMillis = datePickerState.selectedDateMillis
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = OrangeAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = Zinc400)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = Zinc800
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = Zinc800,
                    titleContentColor = Color.White,
                    headlineContentColor = Color.White,
                    weekdayContentColor = Zinc400,
                    subheadContentColor = Zinc400,
                    dayContentColor = Color.White,
                    selectedDayContentColor = Color.White,
                    selectedDayContainerColor = OrangeAccent,
                    todayContentColor = OrangeAccent,
                    todayDateBorderColor = OrangeAccent
                )
            )
        }
    }
    
    // Time Picker Dialog
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = Zinc800,
            title = {
                Text("Select Time", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            containerColor = Zinc800,
                            clockDialColor = Zinc700,
                            clockDialSelectedContentColor = Color.White,
                            clockDialUnselectedContentColor = Zinc400,
                            selectorColor = OrangeAccent,
                            periodSelectorBorderColor = OrangeAccent,
                            periodSelectorSelectedContainerColor = OrangeAccent,
                            periodSelectorSelectedContentColor = Color.White,
                            periodSelectorUnselectedContainerColor = Zinc700,
                            periodSelectorUnselectedContentColor = Zinc400,
                            timeSelectorSelectedContainerColor = OrangeAccent,
                            timeSelectorSelectedContentColor = Color.White,
                            timeSelectorUnselectedContainerColor = Zinc700,
                            timeSelectorUnselectedContentColor = Zinc400
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedHour = timePickerState.hour
                        selectedMinute = timePickerState.minute
                        showTimePicker = false
                    }
                ) {
                    Text("OK", color = OrangeAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel", color = Zinc400)
                }
            }
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ZincDark)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                
                Text(
                    text = "Create Event",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Form
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Cover Image Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                if (coverImageUri != null) listOf(Color.Transparent, Color.Transparent)
                                else listOf(
                                    selectedCategory.color.copy(alpha = 0.3f),
                                    selectedCategory.color.copy(alpha = 0.1f)
                                )
                            )
                        )
                        .border(
                            2.dp,
                            selectedCategory.color.copy(alpha = 0.5f),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (coverImageUri != null) {
                        AsyncImage(
                            model = coverImageUri,
                            contentDescription = "Cover image",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp)),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Overlay with edit icon
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f))
                        )
                        
                        // Loading indicator
                        if (isUploadingImage) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        } else {
                            // Change photo button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(OrangeAccent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = "Change",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(selectedCategory.color.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.AddPhotoAlternate,
                                    contentDescription = "Add cover",
                                    tint = selectedCategory.color,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Text(
                                text = "Add Cover Image",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = selectedCategory.color
                            )
                            Text(
                                text = "Make your event stand out",
                                fontSize = 12.sp,
                                color = Zinc500
                            )
                        }
                    }
                }
                
                // Category Selection
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Category",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Zinc400
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(eventCategories) { category ->
                            val isSelected = category == selectedCategory
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) category.color
                                        else category.color.copy(alpha = 0.15f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) Color.Transparent
                                        else category.color.copy(alpha = 0.3f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedCategory = category }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = category.emoji,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = category.name,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else category.color
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Gaming Profile Form (only visible when Gaming category selected)
                if (selectedCategory.name == "Gaming") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Zinc800.copy(alpha = 0.5f))
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Gaming Profile Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🎮", fontSize = 24.sp)
                            Column {
                                Text(
                                    text = "Gaming Profile",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                                Text(
                                    text = "Fill your gamer info",
                                    fontSize = 12.sp,
                                    color = Zinc500
                                )
                            }
                        }
                        
                        // Gamer Username
                        EventFormField(
                            label = "Gamer Username",
                            value = gamerUsername,
                            onValueChange = { gamerUsername = it },
                            placeholder = "Your in-game name",
                            icon = Icons.Outlined.Person
                        )
                        
                        // First & Last Name Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("First Name", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Zinc400)
                                Spacer(modifier = Modifier.height(8.dp))
                                BasicTextField(
                                    value = firstName,
                                    onValueChange = { firstName = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Zinc700)
                                        .padding(14.dp),
                                    textStyle = TextStyle(fontSize = 15.sp, color = Color.White),
                                    cursorBrush = SolidColor(OrangeAccent),
                                    singleLine = true,
                                    decorationBox = { innerTextField ->
                                        Box {
                                            if (firstName.isEmpty()) Text("First", fontSize = 15.sp, color = Zinc500)
                                            innerTextField()
                                        }
                                    }
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Last Name", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Zinc400)
                                Spacer(modifier = Modifier.height(8.dp))
                                BasicTextField(
                                    value = lastName,
                                    onValueChange = { lastName = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Zinc700)
                                        .padding(14.dp),
                                    textStyle = TextStyle(fontSize = 15.sp, color = Color.White),
                                    cursorBrush = SolidColor(OrangeAccent),
                                    singleLine = true,
                                    decorationBox = { innerTextField ->
                                        Box {
                                            if (lastName.isEmpty()) Text("Last", fontSize = 15.sp, color = Zinc500)
                                            innerTextField()
                                        }
                                    }
                                )
                            }
                        }
                        
                        // Location (already exists, but for gaming profile context)
                        EventFormField(
                            label = "Location",
                            value = location,
                            onValueChange = { location = it },
                            placeholder = "City, State",
                            icon = Icons.Outlined.LocationOn
                        )
                        
                        // Zip Code & Country Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Zip Code", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Zinc400)
                                Spacer(modifier = Modifier.height(8.dp))
                                BasicTextField(
                                    value = zipCode,
                                    onValueChange = { zipCode = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Zinc700)
                                        .padding(14.dp),
                                    textStyle = TextStyle(fontSize = 15.sp, color = Color.White),
                                    cursorBrush = SolidColor(OrangeAccent),
                                    singleLine = true,
                                    decorationBox = { innerTextField ->
                                        Box {
                                            if (zipCode.isEmpty()) Text("123456", fontSize = 15.sp, color = Zinc500)
                                            innerTextField()
                                        }
                                    }
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Country", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Zinc400)
                                Spacer(modifier = Modifier.height(8.dp))
                                BasicTextField(
                                    value = country,
                                    onValueChange = { country = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Zinc700)
                                        .padding(14.dp),
                                    textStyle = TextStyle(fontSize = 15.sp, color = Color.White),
                                    cursorBrush = SolidColor(OrangeAccent),
                                    singleLine = true,
                                    decorationBox = { innerTextField ->
                                        Box {
                                            if (country.isEmpty()) Text("India", fontSize = 15.sp, color = Zinc500)
                                            innerTextField()
                                        }
                                    }
                                )
                            }
                        }
                        
                        // Phone Number
                        EventFormField(
                            label = "Phone Number",
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            placeholder = "+91 98765 43210",
                            icon = Icons.Outlined.Phone
                        )
                        
                        // Game Selection Header
                        Text(
                            text = "Select Game",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Zinc400
                        )
                        
                        // Game Grid
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 2 games per row
                            popularGames.chunked(2).forEach { rowGames ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowGames.forEach { game ->
                                        val isSelected = selectedGame == game
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isSelected) game.color
                                                    else game.color.copy(alpha = 0.15f)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isSelected) Color.Transparent
                                                    else game.color.copy(alpha = 0.3f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable { selectedGame = game }
                                                .padding(horizontal = 12.dp, vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(game.emoji, fontSize = 18.sp)
                                                Text(
                                                    text = game.name,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color.White else game.color
                                                )
                                            }
                                        }
                                    }
                                    // Fill remaining space if odd number
                                    if (rowGames.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                        
                        // Custom Game Name (when "Other" is selected)
                        if (selectedGame?.name == "Other") {
                            EventFormField(
                                label = "Game Name",
                                value = customGameName,
                                onValueChange = { customGameName = it },
                                placeholder = "Enter game name",
                                icon = Icons.Outlined.SportsEsports
                            )
                        }
                    }
                }
                
                // Event Title
                EventFormField(
                    label = "Event Title",
                    value = title,
                    onValueChange = { title = it },
                    placeholder = "Enter event name",
                    icon = Icons.Outlined.Event
                )
                
                // Date Picker Field
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Date",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Zinc400
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Zinc800)
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = OrangeAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (formattedDate.isEmpty()) "Select date" else formattedDate,
                            fontSize = 16.sp,
                            color = if (formattedDate.isEmpty()) Zinc500 else Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            tint = Zinc500
                        )
                    }
                }
                
                // Time Picker Field
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Time",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Zinc400
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Zinc800)
                            .clickable { showTimePicker = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = OrangeAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = formattedTime,
                            fontSize = 16.sp,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            tint = Zinc500
                        )
                    }
                }
                
                // Location
                EventFormField(
                    label = "Location",
                    value = location,
                    onValueChange = { location = it },
                    placeholder = "Enter location or meeting link",
                    icon = Icons.Outlined.LocationOn
                )
                
                // Online toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Zinc800)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Videocam,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Online Event",
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Virtual event via video call",
                                fontSize = 12.sp,
                                color = Zinc500
                            )
                        }
                    }
                    Switch(
                        checked = isOnline,
                        onCheckedChange = { isOnline = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CyanAccent,
                            uncheckedThumbColor = Zinc500,
                            uncheckedTrackColor = Zinc700
                        )
                    )
                }
                
                // Description
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Description (Optional)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Zinc400
                    )
                    BasicTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Zinc800)
                            .padding(16.dp),
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            color = Color.White
                        ),
                        cursorBrush = SolidColor(OrangeAccent),
                        decorationBox = { innerTextField ->
                            Box {
                                if (description.isEmpty()) {
                                    Text(
                                        text = "Tell people about your event...",
                                        fontSize = 16.sp,
                                        color = Zinc500
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Create Button
                Button(
                    onClick = {
                        if (title.isNotBlank() && selectedDateMillis != null) {
                            isCreating = true
                            val finalLocation = if (location.isBlank()) {
                                if (isOnline) "Online" else "TBD"
                            } else location
                            
                            coroutineScope.launch {
                                try {
                                    val result = ApiService.createEvent(
                                        title = title,
                                        description = description,
                                        date = formattedDate,
                                        time = formattedTime,
                                        location = finalLocation,
                                        category = selectedCategory.name,
                                        isOnline = isOnline,
                                        imageUrl = uploadedImageUrl
                                    )
                                    
                                    result.onSuccess { eventJson ->
                                        val eventId = eventJson.optString("eventId")
                                        createdEventData = Triple(eventId, formattedTime, finalLocation)
                                        popupMessage = "Event created successfully!"
                                        showSuccessPopup = true
                                    }.onFailure { error ->
                                        popupMessage = "Failed: ${error.message}"
                                        showErrorPopup = true
                                        isCreating = false
                                    }
                                } catch (e: Exception) {
                                    popupMessage = "Error: ${e.message}"
                                    showErrorPopup = true
                                    isCreating = false
                                }
                            }
                        }
                    },
                    enabled = title.isNotBlank() && selectedDateMillis != null && !isCreating && !isUploadingImage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Zinc700
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (title.isNotBlank() && selectedDateMillis != null)
                                    Brush.linearGradient(listOf(OrangeAccent, PinkAccent))
                                else
                                    Brush.linearGradient(listOf(Zinc700, Zinc700))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedCategory.emoji,
                                    fontSize = 20.sp
                                )
                                Text(
                                    text = "Create Event",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
        
        // Success Popup
        SuccessPopup(
            visible = showSuccessPopup,
            title = "Event Created!",
            message = popupMessage,
            onDismiss = {
                showSuccessPopup = false
                createdEventData?.let { (eventId, time, loc) ->
                    onEventCreated(eventId, title, formattedDate, time, loc)
                }
            }
        )
        
        // Error Popup
        ErrorPopup(
            visible = showErrorPopup,
            message = popupMessage,
            onDismiss = { showErrorPopup = false }
        )
    }
}

@Composable
private fun EventFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Zinc400
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Zinc800)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = OrangeAccent,
                modifier = Modifier.size(24.dp)
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = Color.White
                ),
                cursorBrush = SolidColor(OrangeAccent),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                fontSize = 16.sp,
                                color = Zinc500
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}
