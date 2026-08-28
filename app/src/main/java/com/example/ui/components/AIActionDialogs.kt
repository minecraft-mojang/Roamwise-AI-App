package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActivityCategory
import com.example.data.model.PRESET_INTERESTS
import com.example.data.model.TimeSlot
import com.example.ui.theme.ForestTeal
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.SunGold
import com.example.ui.theme.SunsetCoral

@Composable
fun AIRebalanceDialog(
    currentPlannedCost: Double,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (reductionPercent: Int) -> Unit
) {
    var selectedPercent by remember { mutableIntStateOf(25) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = null,
                    tint = SunGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Budget Optimizer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Current estimated cost is $currency ${currentPlannedCost.toInt()}. Gemini AI will analyze your schedule and swap costly experiences with top-rated free sights, food markets, and smart booking passes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Target Savings Tier:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                listOf(
                    Pair(15, "Moderate Tune-Up (-15%): Optimizes dining and booking tips"),
                    Pair(25, "Balanced Saver (-25%): Replaces costly tickets with high-value alternatives"),
                    Pair(40, "Maximum Value (-40%): Heavy focus on free sights, street food & transit passes")
                ).forEach { (pct, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPercent = pct },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPercent == pct,
                            onClick = { selectedPercent = pct }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedPercent) },
                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                modifier = Modifier.testTag("confirm_rebalance_button")
            ) {
                Text("Optimize Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AITailorInterestsDialog(
    currentInterests: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (selectedInterests: List<String>) -> Unit
) {
    var selected by remember { mutableStateOf(currentInterests.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = SunsetCoral,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tailor Trip Interests",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Pick the core passions you want Gemini AI to prioritize across all days:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PRESET_INTERESTS.forEach { interest ->
                        val isSelected = selected.contains(interest.name)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selected = if (isSelected) {
                                    if (selected.size > 1) selected - interest.name else selected
                                } else {
                                    selected + interest.name
                                }
                            },
                            label = {
                                Text("${interest.emoji} ${interest.name}")
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selected.toList()) },
                colors = ButtonDefaults.buttonColors(containerColor = SunsetCoral),
                modifier = Modifier.testTag("confirm_tailor_button")
            ) {
                Text("Regenerate Plan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AISmartActivityDialog(
    totalDays: Int,
    selectedDay: Int,
    currency: String,
    onDismiss: () -> Unit,
    onGenerate: (day: Int, slot: TimeSlot, wish: String) -> Unit
) {
    var day by remember { mutableIntStateOf(selectedDay.coerceIn(1, totalDays)) }
    var slot by remember { mutableStateOf(TimeSlot.AFTERNOON) }
    var wishText by remember { mutableStateOf("") }

    val quickPicks = listOf(
        "Cheap street food stall nearby",
        "Hidden scenic sunset rooftop",
        "Free museum or historic courtyard",
        "Cozy artisan coffee shop",
        "Relaxing waterfront walk"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = ForestTeal,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Smart Activity Inserter",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "What type of experience would you like Gemini to craft?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = wishText,
                    onValueChange = { wishText = it },
                    label = { Text("Your activity wish / prompt") },
                    placeholder = { Text("e.g. Authentic ramen under $10, or vintage camera shops") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_wish_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = "Or tap a quick idea:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    quickPicks.forEach { pick ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { wishText = pick }
                        ) {
                            Text(
                                text = "✨ $pick",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Slot selector
                Text(
                    text = "Time of Day:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TimeSlot.values().forEach { s ->
                        FilterChip(
                            selected = slot == s,
                            onClick = { slot = s },
                            label = { Text("${s.emoji} ${s.name.take(4)}") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (wishText.isNotBlank()) {
                        onGenerate(day, slot, wishText)
                    }
                },
                enabled = wishText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ForestTeal),
                modifier = Modifier.testTag("generate_smart_activity_button")
            ) {
                Text("Add Activity")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddManualActivityDialog(
    dayNumber: Int,
    currency: String,
    onDismiss: () -> Unit,
    onAdd: (title: String, desc: String, loc: String, cost: Double, slot: TimeSlot, cat: ActivityCategory, tips: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("0") }
    var slot by remember { mutableStateOf(TimeSlot.MORNING) }
    var category by remember { mutableStateOf(ActivityCategory.SIGHTSEEING) }
    var tips by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Custom Activity (Day $dayNumber)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Activity Title *") },
                    placeholder = { Text("e.g. Sunset drinks at Sky Bar") },
                    modifier = Modifier.fillMaxWidth().testTag("manual_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location / Address") },
                    placeholder = { Text("e.g. Central Plaza") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = costStr,
                    onValueChange = { costStr = it },
                    label = { Text("Estimated Cost ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("manual_cost_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )

                OutlinedTextField(
                    value = tips,
                    onValueChange = { tips = it },
                    label = { Text("Pro-Tip / Booking note") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text(text = "Time Slot:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TimeSlot.values().forEach { s ->
                        FilterChip(
                            selected = slot == s,
                            onClick = { slot = s },
                            label = { Text("${s.emoji} ${s.name.take(4)}") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val cost = costStr.toDoubleOrNull() ?: 0.0
                        onAdd(title, desc, location, cost, slot, category, tips)
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                modifier = Modifier.testTag("confirm_add_activity_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
