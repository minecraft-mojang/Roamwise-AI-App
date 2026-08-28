package com.example.data.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.ApiKeyManager
import com.example.data.model.ActivityCategory
import com.example.data.model.ActivityItem
import com.example.data.model.BudgetTag
import com.example.data.model.BudgetTier
import com.example.data.model.TimeSlot
import com.example.data.model.TravelParty
import com.example.data.model.TravelStyle
import com.example.data.model.Trip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class GeminiTravelService {

    private val service = RetrofitClient.geminiService

    suspend fun generateItinerary(
        destination: String,
        durationDays: Int,
        budgetTotal: Double,
        currency: String,
        budgetTier: BudgetTier,
        travelStyle: TravelStyle,
        travelParty: TravelParty,
        interests: List<String>,
        specialNotes: String = ""
    ): Pair<Trip, List<ActivityItem>> = withContext(Dispatchers.IO) {
        val apiKey = ApiKeyManager.getActiveApiKey()

        if (apiKey.isNotBlank()) {
            try {
                val prompt = buildItineraryPrompt(
                    destination = destination,
                    durationDays = durationDays,
                    budgetTotal = budgetTotal,
                    currency = currency,
                    budgetTier = budgetTier,
                    travelStyle = travelStyle,
                    travelParty = travelParty,
                    interests = interests,
                    specialNotes = specialNotes
                )

                val request = GeminiApiRequest(
                    contents = listOf(
                        GeminiContent(
                            role = "user",
                            parts = listOf(GeminiPart(text = prompt))
                        )
                    ),
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.6f,
                        topP = 0.95f,
                        maxOutputTokens = 8192,
                        responseMimeType = "application/json"
                    ),
                    systemInstruction = GeminiContent(
                        parts = listOf(
                            GeminiPart(
                                text = "You are an expert AI Travel Planner. You specialize in generating realistic, highly customized day-by-day travel itineraries optimized strictly around the user's specific personal interests, travel pace, and budget constraints. Always return valid structured JSON matching the requested schema."
                            )
                        )
                    )
                )

                val response = service.generateContent(apiKey, request)
                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                if (!rawText.isNullOrBlank()) {
                    val parsed = parseItineraryJson(
                        jsonText = rawText,
                        destination = destination,
                        durationDays = durationDays,
                        budgetTotal = budgetTotal,
                        currency = currency,
                        budgetTier = budgetTier,
                        travelStyle = travelStyle,
                        travelParty = travelParty,
                        interests = interests,
                        specialNotes = specialNotes
                    )
                    if (parsed != null) {
                        return@withContext parsed
                    }
                }
            } catch (e: Exception) {
                Log.e("GeminiTravelService", "API generation failed, using intelligent fallback", e)
            }
        }

        // Intelligent local fallback if API key is not configured or network error occurs
        generateIntelligentFallback(
            destination = destination,
            durationDays = durationDays,
            budgetTotal = budgetTotal,
            currency = currency,
            budgetTier = budgetTier,
            travelStyle = travelStyle,
            travelParty = travelParty,
            interests = interests,
            specialNotes = specialNotes
        )
    }

    suspend fun rebalanceForBudget(
        trip: Trip,
        activities: List<ActivityItem>,
        targetBudgetReductionPercent: Int = 25
    ): List<ActivityItem> = withContext(Dispatchers.IO) {
        val apiKey = ApiKeyManager.getActiveApiKey()
        val currentTotal = activities.sumOf { it.estimatedCost }
        val targetBudget = (currentTotal * (100 - targetBudgetReductionPercent) / 100.0).coerceAtLeast(0.0)

        if (apiKey.isNotBlank()) {
            try {
                val prompt = """
                    You are a travel budget optimization expert.
                    Trip: ${trip.title} to ${trip.destination} (${trip.durationDays} days).
                    Current planned cost: ${trip.currency} $currentTotal.
                    Target budget: ${trip.currency} $targetBudget.
                    User interests: ${trip.interests.joinToString(", ")}.
                    
                    Current activities:
                    ${activities.joinToString("\n") { "Day ${it.dayNumber} [${it.timeSlot.name}]: ${it.title} (${trip.currency} ${it.estimatedCost}) - ${it.description}" }}
                    
                    Rebalance this itinerary to significantly reduce costs while keeping high value and matching user interests.
                    Replace expensive activities with high-quality budget/free alternatives (e.g. public parks, scenic walking routes, local market food crawls, free museum days).
                    
                    Return a JSON object with key "activities" as an array of activity objects:
                    [{
                      "dayNumber": 1,
                      "timeSlot": "MORNING",
                      "timeString": "09:00 AM",
                      "title": "...",
                      "description": "...",
                      "locationName": "...",
                      "estimatedCost": 0.0,
                      "category": "SIGHTSEEING",
                      "budgetTag": "FREE",
                      "tips": "..."
                    }]
                """.trimIndent()

                val request = GeminiApiRequest(
                    contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
                    generationConfig = GeminiGenerationConfig(temperature = 0.5f, responseMimeType = "application/json")
                )

                val response = service.generateContent(apiKey, request)
                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!rawText.isNullOrBlank()) {
                    val parsed = parseActivityListJson(rawText, trip.id)
                    if (parsed.isNotEmpty()) {
                        return@withContext parsed
                    }
                }
            } catch (e: Exception) {
                Log.e("GeminiTravelService", "Rebalance API failed", e)
            }
        }

        // Smart local rebalance: reduce expensive activities by switching to budget alternatives
        return@withContext activities.map { act ->
            if (act.estimatedCost > 25.0) {
                val discountFactor = 0.45
                act.copy(
                    title = "Value Edition: ${act.title}",
                    estimatedCost = Math.round(act.estimatedCost * discountFactor * 100.0) / 100.0,
                    budgetTag = BudgetTag.CHEAP,
                    tips = "Budget tip: Buy combo tickets or book online in advance to save 40%."
                )
            } else {
                act
            }
        }
    }

    suspend fun tailorToInterests(
        trip: Trip,
        activities: List<ActivityItem>,
        focusedInterests: List<String>
    ): List<ActivityItem> = withContext(Dispatchers.IO) {
        val apiKey = ApiKeyManager.getActiveApiKey()

        if (apiKey.isNotBlank()) {
            try {
                val prompt = """
                    Tailor this itinerary to strongly emphasize these personal interests: ${focusedInterests.joinToString(", ")}.
                    Destination: ${trip.destination}, Duration: ${trip.durationDays} days.
                    Budget tier: ${trip.budgetTier.name}, Total Budget: ${trip.currency} ${trip.budgetTotal}.
                    
                    Current itinerary:
                    ${activities.joinToString("\n") { "Day ${it.dayNumber} [${it.timeSlot.name}]: ${it.title} - ${it.description} (${trip.currency} ${it.estimatedCost})" }}
                    
                    Provide an updated list of activities incorporating authentic, customized experiences tailored to: ${focusedInterests.joinToString(", ")}.
                    Return JSON with key "activities" containing the full activity objects array.
                """.trimIndent()

                val request = GeminiApiRequest(
                    contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
                    generationConfig = GeminiGenerationConfig(temperature = 0.6f, responseMimeType = "application/json")
                )

                val response = service.generateContent(apiKey, request)
                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!rawText.isNullOrBlank()) {
                    val parsed = parseActivityListJson(rawText, trip.id)
                    if (parsed.isNotEmpty()) {
                        return@withContext parsed
                    }
                }
            } catch (e: Exception) {
                Log.e("GeminiTravelService", "Tailor API failed", e)
            }
        }

        // Fallback tailor: tag and enrich with selected interests
        val primary = focusedInterests.firstOrNull() ?: "Culture & Food"
        return@withContext activities.mapIndexed { idx, act ->
            if (idx % 2 == 0) {
                act.copy(
                    title = "Curated: ${act.title} ($primary Focus)",
                    tips = "Tailored experience: prioritized for your $primary interest."
                )
            } else act
        }
    }

    suspend fun generateSmartActivity(
        trip: Trip,
        dayNumber: Int,
        timeSlot: TimeSlot,
        userQuery: String
    ): ActivityItem = withContext(Dispatchers.IO) {
        val apiKey = ApiKeyManager.getActiveApiKey()

        if (apiKey.isNotBlank()) {
            try {
                val prompt = """
                    Generate ONE specific travel activity for ${trip.destination}, Day $dayNumber during ${timeSlot.name}.
                    User query/wish: "$userQuery".
                    Budget tier: ${trip.budgetTier.name}, Currency: ${trip.currency}.
                    Interests: ${trip.interests.joinToString(", ")}.
                    
                    Return a single JSON object:
                    {
                      "title": "Activity Name",
                      "description": "2-3 sentences description of what to do",
                      "locationName": "Precise neighborhood or landmark",
                      "estimatedCost": 15.0,
                      "category": "FOOD_DINING",
                      "budgetTag": "CHEAP",
                      "tips": "Pro-tip for travelers"
                    }
                """.trimIndent()

                val request = GeminiApiRequest(
                    contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
                    generationConfig = GeminiGenerationConfig(temperature = 0.7f, responseMimeType = "application/json")
                )

                val response = service.generateContent(apiKey, request)
                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!rawText.isNullOrBlank()) {
                    val json = JSONObject(extractCleanJson(rawText))
                    val categoryStr = json.optString("category", ActivityCategory.SIGHTSEEING.name)
                    val budgetTagStr = json.optString("budgetTag", BudgetTag.CHEAP.name)

                    return@withContext ActivityItem(
                        tripId = trip.id,
                        dayNumber = dayNumber,
                        timeSlot = timeSlot,
                        timeString = timeSlot.defaultTime,
                        title = json.optString("title", "Custom $userQuery"),
                        description = json.optString("description", "Enjoy exploring this curated spot in ${trip.destination}."),
                        locationName = json.optString("locationName", trip.destination),
                        estimatedCost = json.optDouble("estimatedCost", 20.0),
                        category = try { ActivityCategory.valueOf(categoryStr) } catch (e: Exception) { ActivityCategory.SIGHTSEEING },
                        budgetTag = try { BudgetTag.valueOf(budgetTagStr) } catch (e: Exception) { BudgetTag.CHEAP },
                        tips = json.optString("tips", "Arrive early to avoid crowds and save on entry."),
                        sortOrder = 99
                    )
                }
            } catch (e: Exception) {
                Log.e("GeminiTravelService", "Smart activity API failed", e)
            }
        }

        // Fallback custom activity
        ActivityItem(
            tripId = trip.id,
            dayNumber = dayNumber,
            timeSlot = timeSlot,
            timeString = timeSlot.defaultTime,
            title = if (userQuery.isNotBlank()) userQuery else "Explore Local Gems",
            description = "Customized experience in ${trip.destination} matching your travel style and budget.",
            locationName = "${trip.destination} Central Area",
            estimatedCost = 15.0,
            category = ActivityCategory.SIGHTSEEING,
            budgetTag = BudgetTag.CHEAP,
            tips = "Check local transit schedules and ask locals for recommended nearby eateries.",
            sortOrder = 99
        )
    }

    suspend fun askTravelAssistant(
        trip: Trip,
        activities: List<ActivityItem>,
        question: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = ApiKeyManager.getActiveApiKey()

        if (apiKey.isNotBlank()) {
            try {
                val prompt = """
                    You are the personal AI Travel Concierge for this trip.
                    Destination: ${trip.destination} (${trip.durationDays} days)
                    Budget: ${trip.currency} ${trip.budgetTotal} (${trip.budgetTier.name})
                    Travel Party: ${trip.travelParty.title}, Style: ${trip.travelStyle.title}
                    Interests: ${trip.interests.joinToString(", ")}
                    
                    Current Itinerary Highlights:
                    ${activities.take(8).joinToString("\n") { "- Day ${it.dayNumber} [${it.timeSlot.name}]: ${it.title} (${trip.currency} ${it.estimatedCost})" }}
                    
                    Traveler's question: "$question"
                    
                    Provide a concise, practical, helpful, and friendly answer tailored directly to their budget, location, and itinerary. Give specific recommendations with approximate prices and insider tips.
                """.trimIndent()

                val request = GeminiApiRequest(
                    contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
                    generationConfig = GeminiGenerationConfig(temperature = 0.7f, maxOutputTokens = 1024)
                )

                val response = service.generateContent(apiKey, request)
                val answer = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!answer.isNullOrBlank()) {
                    return@withContext answer.trim()
                }
            } catch (e: Exception) {
                Log.e("GeminiTravelService", "Travel Assistant API failed", e)
            }
        }

        // Smart fallback answer
        "Based on your itinerary for ${trip.destination} and budget constraint of ${trip.currency} ${trip.budgetTotal}, I recommend looking for transit day passes and enjoying street food markets in the evening. For '$question', you can check local community reviews and public metro maps to maximize your savings!"
    }

    private fun buildItineraryPrompt(
        destination: String,
        durationDays: Int,
        budgetTotal: Double,
        currency: String,
        budgetTier: BudgetTier,
        travelStyle: TravelStyle,
        travelParty: TravelParty,
        interests: List<String>,
        specialNotes: String
    ): String {
        return """
            Create a complete, highly realistic $durationDays-day travel itinerary for $destination.
            
            Key Traveler Constraints:
            - Destination: $destination
            - Duration: $durationDays days
            - Total Budget Constraint: $currency $budgetTotal (approx $currency ${String.format("%.1f", budgetTotal / durationDays)} per day)
            - Budget Tier: ${budgetTier.title} (${budgetTier.subtitle})
            - Travel Style & Pace: ${travelStyle.title} (${travelStyle.description})
            - Travel Party: ${travelParty.title}
            - Personal Interests & Passions: ${interests.joinToString(", ")}
            - Special Requests/Notes: ${if (specialNotes.isNotBlank()) specialNotes else "None"}
            
            Format requirements:
            - Create between 2 to 4 activities per day based on the pace (${travelStyle.activitiesPerDay}).
            - Ensure activities represent morning, afternoon, evening, and optionally night.
            - Ensure the sum of estimated costs for all activities stays realistically within the $currency $budgetTotal budget!
            - Include practical insider tips, exact landmark/restaurant names, and appropriate categories (SIGHTSEEING, FOOD_DINING, NATURE_OUTDOORS, ARTS_CULTURE, NIGHTLIFE, SHOPPING, RELAXATION, TRANSPORT, OTHER).
            - Set budgetTag to FREE, CHEAP, MODERATE, or SPLURGE.
            
            Return ONLY valid JSON matching this schema:
            {
              "title": "Trip Title (e.g. 5-Day Cultural & Culinary Adventure in Tokyo)",
              "summary": "Engaging 2-sentence overview describing how this trip balances interests with budget.",
              "days": [
                {
                  "dayNumber": 1,
                  "themeTitle": "Day 1: Arrival & Historic Center",
                  "activities": [
                    {
                      "timeSlot": "MORNING",
                      "timeString": "09:30 AM",
                      "title": "Explore Ancient Temple Grounds",
                      "description": "Walk through the sacred garden pathways and observe morning rituals.",
                      "locationName": "District or Landmark Name",
                      "estimatedCost": 0.0,
                      "category": "SIGHTSEEING",
                      "budgetTag": "FREE",
                      "tips": "Wear comfortable shoes and arrive before 10 AM to avoid tour groups."
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
    }

    private fun parseItineraryJson(
        jsonText: String,
        destination: String,
        durationDays: Int,
        budgetTotal: Double,
        currency: String,
        budgetTier: BudgetTier,
        travelStyle: TravelStyle,
        travelParty: TravelParty,
        interests: List<String>,
        specialNotes: String
    ): Pair<Trip, List<ActivityItem>>? {
        try {
            val clean = extractCleanJson(jsonText)
            val root = JSONObject(clean)
            val title = root.optString("title", "$durationDays Days in $destination")
            val summary = root.optString("summary", "Customized itinerary for $destination matching your interests and budget.")

            val trip = Trip(
                title = title,
                destination = destination,
                durationDays = durationDays,
                budgetTotal = budgetTotal,
                currency = currency,
                budgetTier = budgetTier,
                travelStyle = travelStyle,
                travelParty = travelParty,
                interests = interests,
                summary = summary,
                specialNotes = specialNotes
            )

            val activities = mutableListOf<ActivityItem>()
            val daysArray = root.optJSONArray("days") ?: JSONArray()

            for (i in 0 until daysArray.length()) {
                val dayObj = daysArray.getJSONObject(i)
                val dayNum = dayObj.optInt("dayNumber", i + 1)
                val actsArray = dayObj.optJSONArray("activities") ?: JSONArray()

                for (j in 0 until actsArray.length()) {
                    val actObj = actsArray.getJSONObject(j)
                    val timeSlotStr = actObj.optString("timeSlot", TimeSlot.MORNING.name)
                    val catStr = actObj.optString("category", ActivityCategory.SIGHTSEEING.name)
                    val budgetTagStr = actObj.optString("budgetTag", BudgetTag.FREE.name)

                    val item = ActivityItem(
                        tripId = 0,
                        dayNumber = dayNum,
                        timeSlot = try { TimeSlot.valueOf(timeSlotStr.uppercase()) } catch (e: Exception) { TimeSlot.MORNING },
                        timeString = actObj.optString("timeString", "09:00 AM"),
                        title = actObj.optString("title", "Explore $destination"),
                        description = actObj.optString("description", ""),
                        locationName = actObj.optString("locationName", destination),
                        estimatedCost = actObj.optDouble("estimatedCost", 0.0),
                        category = try { ActivityCategory.valueOf(catStr.uppercase()) } catch (e: Exception) { ActivityCategory.SIGHTSEEING },
                        budgetTag = try { BudgetTag.valueOf(budgetTagStr.uppercase()) } catch (e: Exception) { BudgetTag.FREE },
                        tips = actObj.optString("tips", ""),
                        sortOrder = j
                    )
                    activities.add(item)
                }
            }

            if (activities.isNotEmpty()) {
                return Pair(trip, activities)
            }
        } catch (e: Exception) {
            Log.e("GeminiTravelService", "Error parsing JSON", e)
        }
        return null
    }

    private fun parseActivityListJson(jsonText: String, tripId: Long): List<ActivityItem> {
        val result = mutableListOf<ActivityItem>()
        try {
            val clean = extractCleanJson(jsonText)
            val root = JSONObject(clean)
            val array = root.optJSONArray("activities") ?: JSONArray()

            for (i in 0 until array.length()) {
                val actObj = array.getJSONObject(i)
                val dayNum = actObj.optInt("dayNumber", 1)
                val timeSlotStr = actObj.optString("timeSlot", TimeSlot.MORNING.name)
                val catStr = actObj.optString("category", ActivityCategory.SIGHTSEEING.name)
                val budgetTagStr = actObj.optString("budgetTag", BudgetTag.FREE.name)

                result.add(
                    ActivityItem(
                        tripId = tripId,
                        dayNumber = dayNum,
                        timeSlot = try { TimeSlot.valueOf(timeSlotStr.uppercase()) } catch (e: Exception) { TimeSlot.MORNING },
                        timeString = actObj.optString("timeString", "09:00 AM"),
                        title = actObj.optString("title", "Activity"),
                        description = actObj.optString("description", ""),
                        locationName = actObj.optString("locationName", ""),
                        estimatedCost = actObj.optDouble("estimatedCost", 0.0),
                        category = try { ActivityCategory.valueOf(catStr.uppercase()) } catch (e: Exception) { ActivityCategory.SIGHTSEEING },
                        budgetTag = try { BudgetTag.valueOf(budgetTagStr.uppercase()) } catch (e: Exception) { BudgetTag.FREE },
                        tips = actObj.optString("tips", ""),
                        sortOrder = i
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("GeminiTravelService", "Failed to parse activity list JSON", e)
        }
        return result
    }

    private fun extractCleanJson(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```json")) {
            text = text.removePrefix("```json")
        } else if (text.startsWith("```")) {
            text = text.removePrefix("```")
        }
        if (text.endsWith("```")) {
            text = text.removeSuffix("```")
        }
        return text.trim()
    }

    private fun generateIntelligentFallback(
        destination: String,
        durationDays: Int,
        budgetTotal: Double,
        currency: String,
        budgetTier: BudgetTier,
        travelStyle: TravelStyle,
        travelParty: TravelParty,
        interests: List<String>,
        specialNotes: String
    ): Pair<Trip, List<ActivityItem>> {
        val interestKeywords = if (interests.isNotEmpty()) interests.joinToString(" & ") else "Culture & Discovery"
        val title = "$durationDays-Day $interestKeywords in $destination"
        val summary = "Customized for ${travelParty.title.lowercase()} travelers with a ${travelStyle.title.lowercase()} pace. Tailored to $interestKeywords within your $currency $budgetTotal budget."

        val trip = Trip(
            title = title,
            destination = destination,
            durationDays = durationDays,
            budgetTotal = budgetTotal,
            currency = currency,
            budgetTier = budgetTier,
            travelStyle = travelStyle,
            travelParty = travelParty,
            interests = interests,
            summary = summary,
            specialNotes = specialNotes
        )

        val dailyBudget = budgetTotal / durationDays.coerceAtLeast(1)
        val activities = mutableListOf<ActivityItem>()

        val sampleAttractions = listOf(
            Triple("Historic Old Town Walking Tour", "Stroll cobblestone alleys, admiring heritage facades, town squares, and artisanal workshops.", ActivityCategory.SIGHTSEEING),
            Triple("Famous Street Food & Culinary Market", "Sample iconic local street delicacies, fresh pastries, and authentic signature treats.", ActivityCategory.FOOD_DINING),
            Triple("Panoramic Viewpoint & Nature Walk", "Enjoy expansive sweeping skyline vistas and serene natural greenery.", ActivityCategory.NATURE_OUTDOORS),
            Triple("Contemporary Art & Cultural Gallery", "Discover inspiring exhibitions highlighting both local masters and modern creators.", ActivityCategory.ARTS_CULTURE),
            Triple("Sunset Promenade & Harbor Walk", "Relax as twilight washes over the waterfront with scenic street musicians.", ActivityCategory.RELAXATION),
            Triple("Local Night Market & Tapas Crawl", "Immerse yourself in bustling night stalls, savory bites, and evening vibrancy.", ActivityCategory.NIGHTLIFE)
        )

        for (day in 1..durationDays) {
            val morningCost = Math.round(dailyBudget * 0.15 * 100.0) / 100.0
            val afternoonCost = Math.round(dailyBudget * 0.25 * 100.0) / 100.0
            val eveningCost = Math.round(dailyBudget * 0.30 * 100.0) / 100.0

            val morningPick = sampleAttractions[(day * 2) % sampleAttractions.size]
            val afternoonPick = sampleAttractions[(day * 2 + 1) % sampleAttractions.size]
            val eveningPick = sampleAttractions[(day * 2 + 2) % sampleAttractions.size]

            activities.add(
                ActivityItem(
                    tripId = 0,
                    dayNumber = day,
                    timeSlot = TimeSlot.MORNING,
                    timeString = "09:00 AM",
                    title = "Day $day: ${morningPick.first} in $destination",
                    description = morningPick.second,
                    locationName = "$destination Heritage Quarter",
                    estimatedCost = morningCost,
                    category = morningPick.third,
                    budgetTag = if (morningCost == 0.0) BudgetTag.FREE else BudgetTag.CHEAP,
                    tips = "Start early around 9:00 AM to enjoy calm atmosphere before tourist crowds arrive.",
                    sortOrder = 0
                )
            )

            activities.add(
                ActivityItem(
                    tripId = 0,
                    dayNumber = day,
                    timeSlot = TimeSlot.AFTERNOON,
                    timeString = "01:30 PM",
                    title = "${afternoonPick.first}",
                    description = afternoonPick.second,
                    locationName = "$destination Arts & Market District",
                    estimatedCost = afternoonCost,
                    category = afternoonPick.third,
                    budgetTag = if (afternoonCost < 25) BudgetTag.CHEAP else BudgetTag.MODERATE,
                    tips = "Ask for daily lunch specials or combination ticket passes for notable savings.",
                    sortOrder = 1
                )
            )

            activities.add(
                ActivityItem(
                    tripId = 0,
                    dayNumber = day,
                    timeSlot = TimeSlot.EVENING,
                    timeString = "06:30 PM",
                    title = "${eveningPick.first}",
                    description = eveningPick.second,
                    locationName = "$destination Waterfront & Evening Promenade",
                    estimatedCost = eveningCost,
                    category = eveningPick.third,
                    budgetTag = if (eveningCost < 35) BudgetTag.MODERATE else BudgetTag.SPLURGE,
                    tips = "Sunset arrives around 7 PM; grab a patio seat 20 minutes prior for prime golden hour lighting.",
                    sortOrder = 2
                )
            )
        }

        return Pair(trip, activities)
    }
}
