package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AIChatMessage
import com.example.data.model.ActivityCategory
import com.example.data.model.ActivityItem
import com.example.data.model.BudgetTier
import com.example.data.model.TimeSlot
import com.example.data.model.TravelParty
import com.example.data.model.TravelStyle
import com.example.data.model.Trip
import com.example.data.model.TripWithActivities
import com.example.data.repository.TripRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class TripListViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = TripRepository(database.tripDao(), database.activityDao())

    val trips: StateFlow<List<Trip>> = repository.allTrips
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun deleteTrip(tripId: Long) {
        viewModelScope.launch {
            repository.deleteTrip(tripId)
        }
    }
}

sealed interface TripBuilderUiState {
    object Form : TripBuilderUiState
    data class Generating(val progressMessage: String) : TripBuilderUiState
    data class Success(val tripId: Long) : TripBuilderUiState
    data class Error(val message: String) : TripBuilderUiState
}

class TripBuilderViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = TripRepository(database.tripDao(), database.activityDao())

    var destination = MutableStateFlow("Kyoto, Japan")
    var durationDays = MutableStateFlow(4)
    var budgetTotal = MutableStateFlow("750")
    var currency = MutableStateFlow("USD")
    var budgetTier = MutableStateFlow(BudgetTier.MODERATE)
    var travelStyle = MutableStateFlow(TravelStyle.BALANCED)
    var travelParty = MutableStateFlow(TravelParty.SOLO)
    var selectedInterests = MutableStateFlow(setOf("Street Food & Local Eats", "Historical Sights & UNESCO", "Parks, Hiking & Nature"))
    var specialNotes = MutableStateFlow("")

    private val _uiState = MutableStateFlow<TripBuilderUiState>(TripBuilderUiState.Form)
    val uiState: StateFlow<TripBuilderUiState> = _uiState.asStateFlow()

    fun toggleInterest(interestName: String) {
        val current = selectedInterests.value.toMutableSet()
        if (current.contains(interestName)) {
            if (current.size > 1) { // Keep at least 1 interest
                current.remove(interestName)
            }
        } else {
            current.add(interestName)
        }
        selectedInterests.value = current
    }

    fun selectPresetDestination(dest: String, country: String, defaultBudget: Double, dur: Int, interests: List<String>) {
        destination.value = dest
        budgetTotal.value = defaultBudget.toInt().toString()
        durationDays.value = dur
        selectedInterests.value = interests.toSet()
    }

    fun generateItinerary() {
        val dest = destination.value.trim()
        if (dest.isBlank()) {
            _uiState.value = TripBuilderUiState.Error("Please enter a destination.")
            return
        }

        val budgetVal = budgetTotal.value.toDoubleOrNull() ?: 500.0
        val daysVal = durationDays.value.coerceIn(1, 14)

        viewModelScope.launch {
            _uiState.value = TripBuilderUiState.Generating("Researching top spots in $dest...")
            delay(400)
            _uiState.value = TripBuilderUiState.Generating("Matching activities to your interests: ${selectedInterests.value.take(2).joinToString(", ")}...")
            delay(400)
            _uiState.value = TripBuilderUiState.Generating("Optimizing daily schedule for ${currency.value} $budgetVal budget constraint...")

            try {
                val newTripId = repository.generateAndSaveTrip(
                    destination = dest,
                    durationDays = daysVal,
                    budgetTotal = budgetVal,
                    currency = currency.value,
                    budgetTier = budgetTier.value,
                    travelStyle = travelStyle.value,
                    travelParty = travelParty.value,
                    interests = selectedInterests.value.toList(),
                    specialNotes = specialNotes.value
                )
                _uiState.value = TripBuilderUiState.Success(newTripId)
            } catch (e: Exception) {
                _uiState.value = TripBuilderUiState.Error("Failed to generate itinerary: ${e.message}")
            }
        }
    }

    fun resetState() {
        _uiState.value = TripBuilderUiState.Form
    }
}

class TripDetailViewModel(
    application: Application,
    private val tripId: Long
) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = TripRepository(database.tripDao(), database.activityDao())

    val tripWithActivities: StateFlow<TripWithActivities?> = repository.getTripWithActivities(tripId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _selectedDay = MutableStateFlow<Int?>(1) // null = all days, or 1, 2, 3...
    val selectedDay = _selectedDay.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<ActivityCategory?>(null)
    val selectedCategoryFilter = _selectedCategoryFilter.asStateFlow()

    private val _isRebalancing = MutableStateFlow(false)
    val isRebalancing = _isRebalancing.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<AIChatMessage>>(
        listOf(
            AIChatMessage(
                id = UUID.randomUUID().toString(),
                isFromUser = false,
                message = "Hello! I'm your AI Travel Assistant. Ask me anything about local budget tips, transport hacks, food recommendations, or how to tweak this itinerary!"
            )
        )
    )
    val chatMessages = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading = _isChatLoading.asStateFlow()

    fun selectDay(day: Int?) {
        _selectedDay.value = day
    }

    fun setCategoryFilter(category: ActivityCategory?) {
        _selectedCategoryFilter.value = if (_selectedCategoryFilter.value == category) null else category
    }

    fun toggleActivityCompleted(activity: ActivityItem) {
        viewModelScope.launch {
            repository.toggleActivityCompleted(activity.id, !activity.isCompleted)
        }
    }

    fun deleteActivity(activityId: Long) {
        viewModelScope.launch {
            repository.deleteActivity(activityId)
        }
    }

    fun addActivity(
        dayNumber: Int,
        timeSlot: TimeSlot,
        title: String,
        description: String,
        locationName: String,
        cost: Double,
        category: ActivityCategory,
        tips: String
    ) {
        viewModelScope.launch {
            val item = ActivityItem(
                tripId = tripId,
                dayNumber = dayNumber,
                timeSlot = timeSlot,
                timeString = timeSlot.defaultTime,
                title = title,
                description = description,
                locationName = locationName,
                estimatedCost = cost,
                category = category,
                tips = tips,
                sortOrder = 10
            )
            repository.addActivity(item)
        }
    }

    fun rebalanceBudget(reductionPercent: Int = 25) {
        viewModelScope.launch {
            _isRebalancing.value = true
            try {
                repository.rebalanceTripBudget(tripId, reductionPercent)
            } finally {
                _isRebalancing.value = false
            }
        }
    }

    fun tailorInterests(interests: List<String>) {
        viewModelScope.launch {
            _isRebalancing.value = true
            try {
                repository.tailorTripInterests(tripId, interests)
            } finally {
                _isRebalancing.value = false
            }
        }
    }

    fun addSmartActivity(dayNumber: Int, timeSlot: TimeSlot, userWish: String) {
        viewModelScope.launch {
            _isRebalancing.value = true
            try {
                repository.generateSmartActivity(tripId, dayNumber, timeSlot, userWish)
            } finally {
                _isRebalancing.value = false
            }
        }
    }

    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return
        val userMsg = AIChatMessage(
            id = UUID.randomUUID().toString(),
            isFromUser = true,
            message = userText.trim()
        )
        _chatMessages.value = _chatMessages.value + userMsg
        _isChatLoading.value = true

        viewModelScope.launch {
            try {
                val reply = repository.askAssistant(tripId, userText)
                val aiMsg = AIChatMessage(
                    id = UUID.randomUUID().toString(),
                    isFromUser = false,
                    message = reply
                )
                _chatMessages.value = _chatMessages.value + aiMsg
            } catch (e: Exception) {
                _chatMessages.value = _chatMessages.value + AIChatMessage(
                    id = UUID.randomUUID().toString(),
                    isFromUser = false,
                    message = "I couldn't process that right now. Please try again."
                )
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    fun deleteTrip(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteTrip(tripId)
            onDeleted()
        }
    }

    fun generateShareableText(tripWithActivities: TripWithActivities): String {
        val trip = tripWithActivities.trip
        val sb = StringBuilder()
        sb.appendLine("✈️ ${trip.title}")
        sb.appendLine("📍 Destination: ${trip.destination} | ⏳ Duration: ${trip.durationDays} Days")
        sb.appendLine("💰 Total Budget: ${trip.currency} ${trip.budgetTotal} | Estimated Planned: ${trip.currency} ${tripWithActivities.totalPlannedCost}")
        sb.appendLine("🎯 Interests: ${trip.interests.joinToString(", ")}")
        sb.appendLine("--------------------------------------")
        sb.appendLine()

        tripWithActivities.activitiesByDay.forEach { (day, acts) ->
            sb.appendLine("📅 DAY $day (Total: ${trip.currency} ${acts.sumOf { it.estimatedCost }})")
            acts.forEach { act ->
                val check = if (act.isCompleted) "[✓]" else "[ ]"
                sb.appendLine("$check ${act.timeSlot.emoji} ${act.timeString} - ${act.title} (${trip.currency} ${act.estimatedCost})")
                if (act.locationName.isNotBlank()) sb.appendLine("    📍 ${act.locationName}")
                if (act.tips.isNotBlank()) sb.appendLine("    💡 Tip: ${act.tips}")
            }
            sb.appendLine()
        }
        sb.appendLine("Generated with Itinerary Builder AI ✨")
        return sb.toString()
    }
}

class TripDetailViewModelFactory(
    private val application: Application,
    private val tripId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TripDetailViewModel::class.java)) {
            return TripDetailViewModel(application, tripId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
