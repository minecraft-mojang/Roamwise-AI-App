package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BudgetTier
import com.example.data.model.PRESET_INTERESTS
import com.example.data.model.TravelParty
import com.example.data.model.TravelStyle
import com.example.ui.components.ApiKeySettingsDialog
import com.example.ui.theme.ForestTeal
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.SunGold
import com.example.ui.theme.SunsetCoral
import com.example.ui.viewmodel.TripBuilderUiState
import com.example.ui.viewmodel.TripBuilderViewModel

val CURRENCIES = listOf("USD ($)", "EUR (€)", "GBP (£)", "JPY (¥)", "AUD ($)", "CAD ($)", "SGD ($)")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TripBuilderScreen(
    viewModel: TripBuilderViewModel,
    onBack: () -> Unit,
    onTripGenerated: (tripId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val durationDays by viewModel.durationDays.collectAsStateWithLifecycle()
    val budgetTotal by viewModel.budgetTotal.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val budgetTier by viewModel.budgetTier.collectAsStateWithLifecycle()
    val travelStyle by viewModel.travelStyle.collectAsStateWithLifecycle()
    val travelParty by viewModel.travelParty.collectAsStateWithLifecycle()
    val selectedInterests by viewModel.selectedInterests.collectAsStateWithLifecycle()
    val specialNotes by viewModel.specialNotes.collectAsStateWithLifecycle()
    var showApiKeyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is TripBuilderUiState.Success) {
            val tripId = (uiState as TripBuilderUiState.Success).tripId
            viewModel.resetState()
            onTripGenerated(tripId)
        }
    }

    if (showApiKeyDialog) {
        ApiKeySettingsDialog(onDismiss = { showApiKeyDialog = false })
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Trip Customizer & AI Planner",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showApiKeyDialog = true },
                        modifier = Modifier.testTag("builder_api_key_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "API Key Settings",
                            tint = OceanBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Section 1: Destination & Duration
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📍", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Where & How Long",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedTextField(
                            value = destination,
                            onValueChange = { viewModel.destination.value = it },
                            label = { Text("Destination City & Country *") },
                            placeholder = { Text("e.g. Kyoto, Japan or Florence, Italy") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.LocationCity, contentDescription = null, tint = OceanBlue)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("destination_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // Duration Stepper
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Trip Duration",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "$durationDays ${if (durationDays == 1) "day" else "days"} schedule",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { if (durationDays > 1) viewModel.durationDays.value = durationDays - 1 },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                ) {
                                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease")
                                }

                                Text(
                                    text = "$durationDays",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )

                                IconButton(
                                    onClick = { if (durationDays < 14) viewModel.durationDays.value = durationDays + 1 },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Increase")
                                }
                            }
                        }
                    }
                }

                // Section 2: Budget Constraints
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "💰", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Budget Constraints",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = budgetTotal,
                                onValueChange = { viewModel.budgetTotal.value = it },
                                label = { Text("Total Budget Target *") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.AttachMoney, contentDescription = null, tint = ForestTeal)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("budget_input"),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            // Currency Chips
                            Column(modifier = Modifier.weight(0.7f)) {
                                Text(
                                    text = "Currency",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("USD", "EUR", "GBP", "JPY").forEach { curr ->
                                        Surface(
                                            color = if (currency == curr) OceanBlue else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { viewModel.currency.value = curr }
                                        ) {
                                            Text(
                                                text = curr,
                                                color = if (currency == curr) Color.White else MaterialTheme.colorScheme.onSurface,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Budget Tier Selector
                        Text(
                            text = "Target Travel Tier:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            BudgetTier.values().forEach { tier ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (budgetTier == tier) OceanBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = if (budgetTier == tier) androidx.compose.foundation.BorderStroke(1.5.dp, OceanBlue) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.budgetTier.value = tier }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = tier.title,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = if (budgetTier == tier) OceanBlue else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = tier.subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = tier.approxPerDayUSD,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ForestTeal,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 3: Travel Party & Pace
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "👥", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Party & Travel Pace",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Party Chips
                        Text(text = "Travel Party:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TravelParty.values().forEach { party ->
                                FilterChip(
                                    selected = travelParty == party,
                                    onClick = { viewModel.travelParty.value = party },
                                    label = { Text("${party.iconEmoji} ${party.title.split(" ").first()}") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SunsetCoral.copy(alpha = 0.2f),
                                        selectedLabelColor = SunsetCoral
                                    )
                                )
                            }
                        }

                        // Style Chips
                        Text(text = "Pace & Rhythm:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TravelStyle.values().forEach { style ->
                                FilterChip(
                                    selected = travelStyle == style,
                                    onClick = { viewModel.travelStyle.value = style },
                                    label = { Text(style.title.split(" ").first()) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = OceanBlue.copy(alpha = 0.2f),
                                        selectedLabelColor = OceanBlue
                                    )
                                )
                            }
                        }
                    }
                }

                // Section 4: Personal Interests Multi-Selection
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🎯", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Personal Interests & Passions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Select what you love (${selectedInterests.size} selected). Gemini will customize activities around these topics:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PRESET_INTERESTS.forEach { interest ->
                                val isSelected = selectedInterests.contains(interest.name)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.toggleInterest(interest.name) },
                                    label = { Text("${interest.emoji} ${interest.name}") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SunGold.copy(alpha = 0.25f),
                                        selectedLabelColor = Color(0xFF6B4100)
                                    )
                                )
                            }
                        }
                    }
                }

                // Section 5: Custom Notes
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "✍️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Special Preferences & Constraints",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedTextField(
                            value = specialNotes,
                            onValueChange = { viewModel.specialNotes.value = it },
                            placeholder = { Text("e.g. Vegetarian dining, public transit only, avoid early morning 8 AM starts") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("special_notes_input"),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 2
                        )
                    }
                }

                // Error message if any
                if (uiState is TripBuilderUiState.Error) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = (uiState as TripBuilderUiState.Error).message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Generate Button
                Button(
                    onClick = { viewModel.generateItinerary() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("generate_itinerary_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OceanBlue)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Generate Custom AI Itinerary",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Animated Generation Overlay
            AnimatedVisibility(
                visible = uiState is TripBuilderUiState.Generating,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = OceanBlue,
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Crafting Your Custom Itinerary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = (uiState as? TripBuilderUiState.Generating)?.progressMessage
                                    ?: "Analyzing destination costs and interests...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
