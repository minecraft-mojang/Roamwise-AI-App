package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ActivityCategory
import com.example.data.model.ActivityItem
import com.example.ui.components.AIAssistantSheet
import com.example.ui.components.AIRebalanceDialog
import com.example.ui.components.AISmartActivityDialog
import com.example.ui.components.AITailorInterestsDialog
import com.example.ui.components.ActivityItemCard
import com.example.ui.components.AddManualActivityDialog
import com.example.ui.components.ApiKeySettingsDialog
import com.example.ui.components.BudgetTrackerCard
import com.example.ui.theme.ForestTeal
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.SunGold
import com.example.ui.theme.SunsetCoral
import com.example.ui.viewmodel.TripDetailViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TripDetailScreen(
    viewModel: TripDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tripWithActivities by viewModel.tripWithActivities.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val isRebalancing by viewModel.isRebalancing.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()

    var showRebalanceDialog by remember { mutableStateOf(false) }
    var showTailorDialog by remember { mutableStateOf(false) }
    var showSmartActivityDialog by remember { mutableStateOf(false) }
    var showManualAddDialog by remember { mutableStateOf(false) }
    var showChatSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }

    if (tripWithActivities == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = OceanBlue)
        }
        return
    }

    if (showApiKeyDialog) {
        ApiKeySettingsDialog(onDismiss = { showApiKeyDialog = false })
    }

    val trip = tripWithActivities!!.trip
    val allActivities = tripWithActivities!!.activities

    // Filter activities by day & category
    val displayedActivities = remember(allActivities, selectedDay, selectedCategoryFilter) {
        allActivities.filter { act ->
            val dayMatch = selectedDay == null || act.dayNumber == selectedDay
            val catMatch = selectedCategoryFilter == null || act.category == selectedCategoryFilter
            dayMatch && catMatch
        }.sortedWith(compareBy({ it.dayNumber }, { it.sortOrder }, { it.timeString }))
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = trip.destination,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${trip.durationDays} Days • ${trip.currency} ${trip.budgetTotal.toInt()} budget",
                            style = MaterialTheme.typography.labelSmall,
                            color = OceanBlue
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // API Key Settings
                    IconButton(
                        onClick = { showApiKeyDialog = true },
                        modifier = Modifier.testTag("detail_api_key_settings_button")
                    ) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = "API Key Settings", tint = OceanBlue)
                    }

                    // Share
                    IconButton(onClick = {
                        val shareText = viewModel.generateShareableText(tripWithActivities!!)
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Itinerary"))
                    }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                    }

                    // Delete
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSmartActivityDialog = true },
                containerColor = OceanBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_activity_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add Activity",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Budget Tracker Card
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    BudgetTrackerCard(tripWithActivities = tripWithActivities!!)
                }
            }

            // AI Action Quick Buttons Bar
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "AI Trip Customizer Tools",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SunGold.copy(alpha = 0.18f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showRebalanceDialog = true }
                                    .testTag("ai_rebalance_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null, tint = SunGold, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "⚡ Optimize Budget",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF7A4500)
                                    )
                                }
                            }
                        }

                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SunsetCoral.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showTailorDialog = true }
                                    .testTag("ai_tailor_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = SunsetCoral, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "🎯 Tailor Passions",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SunsetCoral
                                    )
                                }
                            }
                        }

                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = OceanBlue.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showChatSheet = true }
                                    .testTag("ai_concierge_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = OceanBlue, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "💬 Ask Concierge",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = OceanBlue
                                    )
                                }
                            }
                        }

                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showManualAddDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Manual Entry",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Rebalancing In Progress Banner
            if (isRebalancing) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Surface(
                            color = OceanBlue.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = OceanBlue,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "AI is reorganizing your itinerary schedule & budget...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OceanBlue,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Day Selector Tabs Carousel
            item {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // "All Days" Tab
                        item {
                            FilterChip(
                                selected = selectedDay == null,
                                onClick = { viewModel.selectDay(null) },
                                label = { Text("All Days (${allActivities.size})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OceanBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }

                        // Day 1, Day 2, Day 3...
                        items((1..trip.durationDays).toList()) { day ->
                            val dayActs = allActivities.filter { it.dayNumber == day }
                            val dayCost = dayActs.sumOf { it.estimatedCost }
                            FilterChip(
                                selected = selectedDay == day,
                                onClick = { viewModel.selectDay(day) },
                                label = {
                                    Text("Day $day (${trip.currency} ${dayCost.toInt()})")
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = OceanBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Category Filter Chips
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryFilter == null,
                            onClick = { viewModel.setCategoryFilter(null) },
                            label = { Text("All Categories") }
                        )
                    }
                    items(ActivityCategory.values()) { cat ->
                        FilterChip(
                            selected = selectedCategoryFilter == cat,
                            onClick = { viewModel.setCategoryFilter(cat) },
                            label = { Text("${cat.emoji} ${cat.displayName.split(" ").first()}") }
                        )
                    }
                }
            }

            // Activities Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedDay != null) "Day $selectedDay Schedule" else "Full Trip Itinerary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${displayedActivities.size} activities • ${displayedActivities.count { it.isCompleted }} done",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Activities Timeline List
            if (displayedActivities.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🏖️", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No activities for this selection",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Add Activity' to insert a custom stop or AI recommendation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(displayedActivities, key = { it.id }) { activity ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        ActivityItemCard(
                            activity = activity,
                            currency = trip.currency,
                            onToggleCompleted = { viewModel.toggleActivityCompleted(activity) },
                            onDelete = { viewModel.deleteActivity(activity.id) }
                        )
                    }
                }
            }
        }
    }

    // Dialogs & Sheets
    if (showRebalanceDialog) {
        AIRebalanceDialog(
            currentPlannedCost = tripWithActivities!!.totalPlannedCost,
            currency = trip.currency,
            onDismiss = { showRebalanceDialog = false },
            onConfirm = { pct ->
                showRebalanceDialog = false
                viewModel.rebalanceBudget(pct)
            }
        )
    }

    if (showTailorDialog) {
        AITailorInterestsDialog(
            currentInterests = trip.interests,
            onDismiss = { showTailorDialog = false },
            onConfirm = { interests ->
                showTailorDialog = false
                viewModel.tailorInterests(interests)
            }
        )
    }

    if (showSmartActivityDialog) {
        AISmartActivityDialog(
            totalDays = trip.durationDays,
            selectedDay = selectedDay ?: 1,
            currency = trip.currency,
            onDismiss = { showSmartActivityDialog = false },
            onGenerate = { day, slot, wish ->
                showSmartActivityDialog = false
                viewModel.addSmartActivity(day, slot, wish)
            }
        )
    }

    if (showManualAddDialog) {
        AddManualActivityDialog(
            dayNumber = selectedDay ?: 1,
            currency = trip.currency,
            onDismiss = { showManualAddDialog = false },
            onAdd = { title, desc, loc, cost, slot, cat, tips ->
                showManualAddDialog = false
                viewModel.addActivity(
                    dayNumber = selectedDay ?: 1,
                    timeSlot = slot,
                    title = title,
                    description = desc,
                    locationName = loc,
                    cost = cost,
                    category = cat,
                    tips = tips
                )
            }
        )
    }

    if (showChatSheet) {
        AIAssistantSheet(
            trip = trip,
            messages = chatMessages,
            isLoading = isChatLoading,
            onSendMessage = { query -> viewModel.sendChatMessage(query) },
            onDismiss = { showChatSheet = false }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Itinerary?") },
            text = { Text("Are you sure you want to delete '${trip.title}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteTrip(onDeleted = onBack)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
