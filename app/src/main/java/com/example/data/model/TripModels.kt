package com.example.data.model

enum class BudgetTier(val title: String, val subtitle: String, val approxPerDayUSD: String) {
    BUDGET("Budget Friendly", "Hostels, street food, public transit, free sights", "$40 - $80 / day"),
    MODERATE("Comfort & Value", "Boutique hotels, casual restaurants, top attractions", "$100 - $220 / day"),
    LUXURY("Premium & Luxury", "4-5 star hotels, fine dining, private tours", "$300+ / day")
}

enum class TravelStyle(val title: String, val description: String, val activitiesPerDay: String) {
    RELAXED("Slow & Relaxed", "1-2 main activities per day with ample leisure time", "1-2 activities/day"),
    BALANCED("Balanced Explorer", "Comfortable mix of highlights and free exploration", "3-4 activities/day"),
    FAST_PACED("Action-Packed", "See as much as possible, early starts to late nights", "5+ activities/day")
}

enum class TravelParty(val title: String, val iconEmoji: String) {
    SOLO("Solo Traveler", "🎒"),
    COUPLE("Couple / Romantic", "💑"),
    FRIENDS("Friends Group", "👯"),
    FAMILY("Family with Kids", "👨‍👩‍👧‍👦")
}

enum class ActivityCategory(val displayName: String, val emoji: String) {
    SIGHTSEEING("Sightseeing & Landmarks", "🏛️"),
    FOOD_DINING("Food & Culinary", "🍜"),
    NATURE_OUTDOORS("Nature & Outdoors", "🌲"),
    ARTS_CULTURE("Arts & Culture", "🎨"),
    NIGHTLIFE("Nightlife & Entertainment", "🎭"),
    SHOPPING("Shopping & Markets", "🛍️"),
    RELAXATION("Wellness & Relaxation", "☕"),
    TRANSPORT("Transit & Travel", "🚆"),
    OTHER("Activity", "📍")
}

enum class BudgetTag(val label: String, val colorHex: Long) {
    FREE("Free", 0xFF2A9D8F),
    CHEAP("$ Affordable", 0xFF006686),
    MODERATE("$$ Moderate", 0xFFE07A5F),
    SPLURGE("$$$ Splurge", 0xFF9C27B0)
}

enum class TimeSlot(val title: String, val defaultTime: String, val emoji: String) {
    MORNING("Morning", "09:00 AM", "🌅"),
    AFTERNOON("Afternoon", "01:30 PM", "☀️"),
    EVENING("Evening", "06:30 PM", "🌇"),
    NIGHT("Night", "09:00 PM", "🌙")
}

data class TravelInterest(
    val id: String,
    val name: String,
    val emoji: String,
    val category: String
)

val PRESET_INTERESTS = listOf(
    TravelInterest("food", "Street Food & Local Eats", "🍜", "Culinary"),
    TravelInterest("fine_dining", "Fine Dining & Cafes", "☕", "Culinary"),
    TravelInterest("history", "Historical Sights & UNESCO", "🏛️", "Culture"),
    TravelInterest("museums", "Art & Museums", "🎨", "Culture"),
    TravelInterest("nature", "Parks, Hiking & Nature", "🏔️", "Outdoors"),
    TravelInterest("beaches", "Beaches & Water Sports", "🏖️", "Outdoors"),
    TravelInterest("nightlife", "Nightlife, Bars & Clubs", "🍸", "Entertainment"),
    TravelInterest("shopping", "Vintage & Local Markets", "🛍️", "Leisure"),
    TravelInterest("photo", "Scenic Photography Spots", "📸", "Sightseeing"),
    TravelInterest("architecture", "Architecture & Modern City", "🏙️", "Culture"),
    TravelInterest("offbeat", "Hidden Gems & Local Secrets", "🧭", "Adventure"),
    TravelInterest("wellness", "Spas, Onsens & Relaxation", "🧖", "Wellness"),
    TravelInterest("family", "Family & Kid-Friendly", "🎡", "Family")
)

data class Trip(
    val id: Long = 0,
    val title: String,
    val destination: String,
    val country: String = "",
    val durationDays: Int,
    val startDate: String = "",
    val budgetTotal: Double,
    val currency: String = "USD",
    val budgetTier: BudgetTier = BudgetTier.MODERATE,
    val travelStyle: TravelStyle = TravelStyle.BALANCED,
    val travelParty: TravelParty = TravelParty.SOLO,
    val interests: List<String> = emptyList(),
    val summary: String = "",
    val coverGradientIndex: Int = 0,
    val specialNotes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class ActivityItem(
    val id: Long = 0,
    val tripId: Long,
    val dayNumber: Int,
    val timeSlot: TimeSlot = TimeSlot.MORNING,
    val timeString: String = "09:00 AM",
    val title: String,
    val description: String,
    val locationName: String = "",
    val estimatedCost: Double = 0.0,
    val category: ActivityCategory = ActivityCategory.SIGHTSEEING,
    val budgetTag: BudgetTag = BudgetTag.FREE,
    val tips: String = "",
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0
)

data class TripWithActivities(
    val trip: Trip,
    val activities: List<ActivityItem>
) {
    val totalPlannedCost: Double
        get() = activities.sumOf { it.estimatedCost }

    val remainingBudget: Double
        get() = trip.budgetTotal - totalPlannedCost

    val costByCategory: Map<ActivityCategory, Double>
        get() = activities.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.estimatedCost } }

    val activitiesByDay: Map<Int, List<ActivityItem>>
        get() = activities.groupBy { it.dayNumber }
            .mapValues { entry -> entry.value.sortedWith(compareBy({ it.dayNumber }, { it.sortOrder }, { it.timeString })) }

    val isOverBudget: Boolean
        get() = totalPlannedCost > trip.budgetTotal && trip.budgetTotal > 0
}

data class AICustomizerOption(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String
)

data class AIChatMessage(
    val id: String,
    val isFromUser: Boolean,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
