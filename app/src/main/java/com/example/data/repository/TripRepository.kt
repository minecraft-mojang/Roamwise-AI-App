package com.example.data.repository

import com.example.data.ai.GeminiTravelService
import com.example.data.local.ActivityDao
import com.example.data.local.ActivityEntity
import com.example.data.local.TripDao
import com.example.data.local.TripEntity
import com.example.data.model.ActivityCategory
import com.example.data.model.ActivityItem
import com.example.data.model.BudgetTag
import com.example.data.model.BudgetTier
import com.example.data.model.TimeSlot
import com.example.data.model.TravelParty
import com.example.data.model.TravelStyle
import com.example.data.model.Trip
import com.example.data.model.TripWithActivities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TripRepository(
    private val tripDao: TripDao,
    private val activityDao: ActivityDao,
    private val aiService: GeminiTravelService = GeminiTravelService()
) {

    val allTrips: Flow<List<Trip>> = tripDao.getAllTrips()
        .map { list -> list.map { it.toTrip() } }
        .flowOn(Dispatchers.IO)

    fun getTripWithActivities(tripId: Long): Flow<TripWithActivities?> {
        return combine(
            tripDao.getTripByIdFlow(tripId),
            activityDao.getActivitiesForTrip(tripId)
        ) { tripEntity, activityEntities ->
            if (tripEntity == null) null
            else {
                TripWithActivities(
                    trip = tripEntity.toTrip(),
                    activities = activityEntities.map { it.toActivityItem() }
                )
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getTripById(tripId: Long): Trip? = withContext(Dispatchers.IO) {
        tripDao.getTripById(tripId)?.toTrip()
    }

    suspend fun generateAndSaveTrip(
        destination: String,
        durationDays: Int,
        budgetTotal: Double,
        currency: String,
        budgetTier: BudgetTier,
        travelStyle: TravelStyle,
        travelParty: TravelParty,
        interests: List<String>,
        specialNotes: String
    ): Long = withContext(Dispatchers.IO) {
        val (trip, activities) = aiService.generateItinerary(
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

        val tripId = tripDao.insertTrip(TripEntity.fromTrip(trip))
        val activitiesWithId = activities.map { it.copy(tripId = tripId) }
        activityDao.insertAllActivities(activitiesWithId.map { ActivityEntity.fromActivityItem(it) })
        tripId
    }

    suspend fun rebalanceTripBudget(tripId: Long, targetBudgetReductionPercent: Int = 25) = withContext(Dispatchers.IO) {
        val tripEntity = tripDao.getTripById(tripId) ?: return@withContext
        val activities = activityDao.getActivitiesForTripDirect(tripId).map { it.toActivityItem() }
        val updatedActivities = aiService.rebalanceForBudget(
            trip = tripEntity.toTrip(),
            activities = activities,
            targetBudgetReductionPercent = targetBudgetReductionPercent
        )

        activityDao.deleteActivitiesForTrip(tripId)
        activityDao.insertAllActivities(updatedActivities.map { ActivityEntity.fromActivityItem(it.copy(tripId = tripId)) })
    }

    suspend fun tailorTripInterests(tripId: Long, newInterests: List<String>) = withContext(Dispatchers.IO) {
        val tripEntity = tripDao.getTripById(tripId) ?: return@withContext
        val activities = activityDao.getActivitiesForTripDirect(tripId).map { it.toActivityItem() }
        val updatedActivities = aiService.tailorToInterests(
            trip = tripEntity.toTrip(),
            activities = activities,
            focusedInterests = newInterests
        )

        // Also update trip interests in DB
        val updatedTrip = tripEntity.toTrip().copy(interests = newInterests)
        tripDao.updateTrip(TripEntity.fromTrip(updatedTrip))

        activityDao.deleteActivitiesForTrip(tripId)
        activityDao.insertAllActivities(updatedActivities.map { ActivityEntity.fromActivityItem(it.copy(tripId = tripId)) })
    }

    suspend fun generateSmartActivity(tripId: Long, dayNumber: Int, timeSlot: TimeSlot, query: String): ActivityItem = withContext(Dispatchers.IO) {
        val trip = tripDao.getTripById(tripId)?.toTrip() ?: Trip(
            title = "Trip",
            destination = "Destination",
            durationDays = 3,
            budgetTotal = 500.0
        )
        val activity = aiService.generateSmartActivity(trip, dayNumber, timeSlot, query)
        val newId = activityDao.insertActivity(ActivityEntity.fromActivityItem(activity.copy(tripId = tripId)))
        activity.copy(id = newId, tripId = tripId)
    }

    suspend fun askAssistant(tripId: Long, question: String): String = withContext(Dispatchers.IO) {
        val trip = tripDao.getTripById(tripId)?.toTrip() ?: return@withContext "Trip not found."
        val activities = activityDao.getActivitiesForTripDirect(tripId).map { it.toActivityItem() }
        aiService.askTravelAssistant(trip, activities, question)
    }

    suspend fun addActivity(activity: ActivityItem): Long = withContext(Dispatchers.IO) {
        activityDao.insertActivity(ActivityEntity.fromActivityItem(activity))
    }

    suspend fun updateActivity(activity: ActivityItem) = withContext(Dispatchers.IO) {
        activityDao.updateActivity(ActivityEntity.fromActivityItem(activity))
    }

    suspend fun deleteActivity(activityId: Long) = withContext(Dispatchers.IO) {
        activityDao.deleteActivityById(activityId)
    }

    suspend fun toggleActivityCompleted(activityId: Long, isCompleted: Boolean) = withContext(Dispatchers.IO) {
        activityDao.setActivityCompleted(activityId, isCompleted)
    }

    suspend fun deleteTrip(tripId: Long) = withContext(Dispatchers.IO) {
        tripDao.deleteTripById(tripId)
    }

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val count = tripDao.getTripById(1L)
        if (count == null) {
            seedSampleTrips()
        }
    }

    private suspend fun seedSampleTrips() {
        // Sample Trip 1: Tokyo Culinary & Pop Culture on a Budget
        val tokyoTrip = Trip(
            title = "Tokyo Food & Culture Explorer",
            destination = "Tokyo, Japan",
            country = "Japan",
            durationDays = 4,
            startDate = "Sep 15, 2026",
            budgetTotal = 600.0,
            currency = "USD",
            budgetTier = BudgetTier.BUDGET,
            travelStyle = TravelStyle.BALANCED,
            travelParty = TravelParty.SOLO,
            interests = listOf("Street Food & Local Eats", "Historical Sights & UNESCO", "Art & Museums", "Vintage & Local Markets"),
            summary = "Budget-optimized Tokyo itinerary balancing iconic street eats in Asakusa & Omoide Yokocho with free temple sights, teamLab wonders, and Shibuya exploration.",
            coverGradientIndex = 0,
            specialNotes = "Focus on cheap ramen shops, 7-Eleven snacks, and transit IC card savings."
        )
        val tokyoId = tripDao.insertTrip(TripEntity.fromTrip(tokyoTrip))

        val tokyoActivities = listOf(
            ActivityItem(
                tripId = tokyoId,
                dayNumber = 1,
                timeSlot = TimeSlot.MORNING,
                timeString = "09:00 AM",
                title = "Senso-ji Temple & Nakamise Dori",
                description = "Walk through the iconic Kaminarimon gate and explore Tokyo's oldest Buddhist temple grounds. Sample fresh melonpan and ningyo-yaki along the historic shopping street.",
                locationName = "Asakusa, Taito City",
                estimatedCost = 5.0,
                category = ActivityCategory.SIGHTSEEING,
                budgetTag = BudgetTag.FREE,
                tips = "Temple entrance is completely free. Arrive early for crowd-free photos at the giant red lantern."
            ),
            ActivityItem(
                tripId = tokyoId,
                dayNumber = 1,
                timeSlot = TimeSlot.AFTERNOON,
                timeString = "01:00 PM",
                title = "Kappabashi Kitchen Town & Ramen Lunch",
                description = "Stroll Kappabashi utensil street to see hyper-realistic wax food replicas and traditional chef knives, followed by a savory bowl of tonkotsu ramen.",
                locationName = "Kappabashi Street",
                estimatedCost = 9.5,
                category = ActivityCategory.FOOD_DINING,
                budgetTag = BudgetTag.CHEAP,
                tips = "Order ramen from the ticket vending machine at the door; extra noodles (kaedama) are often just 100 yen."
            ),
            ActivityItem(
                tripId = tokyoId,
                dayNumber = 1,
                timeSlot = TimeSlot.EVENING,
                timeString = "06:30 PM",
                title = "Tokyo Skytree Town & Sumida River Sunset",
                description = "Catch the sunset reflections along the Sumida river promenade with views of the illuminated tower, then visit the bustling Solamachi food basement.",
                locationName = "Sumida Riverside",
                estimatedCost = 0.0,
                category = ActivityCategory.RELAXATION,
                budgetTag = BudgetTag.FREE,
                tips = "Supermarkets and bento shops in Solamachi discount their fresh meals by 30-50% after 7:30 PM."
            ),
            ActivityItem(
                tripId = tokyoId,
                dayNumber = 2,
                timeSlot = TimeSlot.MORNING,
                timeString = "09:30 AM",
                title = "Meiji Shrine & Yoyogi Forest Walk",
                description = "Pass beneath towering cedar torii gates into a tranquil 170-acre forest in the heart of the metropolis. Write a wooden ema prayer plaque.",
                locationName = "Harajuku / Shibuya",
                estimatedCost = 3.0,
                category = ActivityCategory.NATURE_OUTDOORS,
                budgetTag = BudgetTag.FREE,
                tips = "Free entry. Visit early morning to witness Shinto priests conducting cleansing rituals."
            ),
            ActivityItem(
                tripId = tokyoId,
                dayNumber = 2,
                timeSlot = TimeSlot.AFTERNOON,
                timeString = "02:00 PM",
                title = "Takeshita Street & Cat Street Thrifting",
                description = "Experience colorful Harajuku pop culture, creative crepe stalls, and relaxed vintage thrift boutiques along pedestrian Cat Street.",
                locationName = "Harajuku to Shibuya",
                estimatedCost = 8.0,
                category = ActivityCategory.SHOPPING,
                budgetTag = BudgetTag.CHEAP,
                tips = "Marion Crepes has budget combos for around $4-$5 with fresh strawberries and custard."
            ),
            ActivityItem(
                tripId = tokyoId,
                dayNumber = 2,
                timeSlot = TimeSlot.EVENING,
                timeString = "07:00 PM",
                title = "Shibuya Scramble & Omoide Yokocho Yakitori",
                description = "Cross the world's busiest intersection, view the Hachiko statue, and head to Memory Lane for skewered chicken yakitori in nostalgic alleyways.",
                locationName = "Shinjuku & Shibuya",
                estimatedCost = 18.0,
                category = ActivityCategory.NIGHTLIFE,
                budgetTag = BudgetTag.MODERATE,
                tips = "Most yakitori stalls have an English menu and charge a small table appetizer fee (otoushi) of ~$2."
            ),
            ActivityItem(
                tripId = tokyoId,
                dayNumber = 3,
                timeSlot = TimeSlot.MORNING,
                timeString = "09:00 AM",
                title = "Tsukiji Outer Market Food Crawl",
                description = "Wander the vibrant market lanes tasting fresh tamagoyaki (rolled omelet), grilled scallops, tuna nigiri, and matcha ice cream.",
                locationName = "Tsukiji Outer Market",
                estimatedCost = 14.0,
                category = ActivityCategory.FOOD_DINING,
                budgetTag = BudgetTag.CHEAP,
                tips = "Cash is king at Tsukiji! Bring small 100 and 500 yen coins for quick street snack payments."
            ),
            ActivityItem(
                tripId = tokyoId,
                dayNumber = 3,
                timeSlot = TimeSlot.AFTERNOON,
                timeString = "02:00 PM",
                title = "teamLab Planets Digital Art Museum",
                description = "Immersive sensory art installation walking barefoot through water and infinite crystal universes.",
                locationName = "Toyosu, Koto City",
                estimatedCost = 28.0,
                category = ActivityCategory.ARTS_CULTURE,
                budgetTag = BudgetTag.MODERATE,
                tips = "Book timed tickets 2 weeks ahead online to secure your preferred afternoon slot."
            ),
            ActivityItem(
                tripId = tokyoId,
                dayNumber = 4,
                timeSlot = TimeSlot.MORNING,
                timeString = "10:00 AM",
                title = "Ueno Park & Ameyoko Market Bazaar",
                description = "Stroll around Shinobazu pond, see historic pagodas, and bargain hunt along Ameyoko market under the railway tracks for matcha sweets and snacks.",
                locationName = "Ueno, Taito City",
                estimatedCost = 6.0,
                category = ActivityCategory.SIGHTSEEING,
                budgetTag = BudgetTag.FREE,
                tips = "The park is free and peaceful; great spot to bring a takeaway convenience store onigiri breakfast."
            )
        )
        activityDao.insertAllActivities(tokyoActivities.map { ActivityEntity.fromActivityItem(it) })

        // Sample Trip 2: Paris Romantic & Arts
        val parisTrip = Trip(
            title = "Parisian Art & Bistro Charm",
            destination = "Paris, France",
            country = "France",
            durationDays = 3,
            startDate = "Oct 10, 2026",
            budgetTotal = 550.0,
            currency = "EUR",
            budgetTier = BudgetTier.MODERATE,
            travelStyle = TravelStyle.RELAXED,
            travelParty = TravelParty.COUPLE,
            interests = listOf("Art & Museums", "Fine Dining & Cafes", "Scenic Photography Spots", "Architecture & Modern City"),
            summary = "Romantic 3-day Paris escape balancing world-class art at Musée d'Orsay, sunset cruises on the Seine, Montmartre cobblestone cafes, and affordable gourmet patisseries.",
            coverGradientIndex = 1,
            specialNotes = "Couples trip with leisurely cafe mornings and golden hour photo walks."
        )
        val parisId = tripDao.insertTrip(TripEntity.fromTrip(parisTrip))

        val parisActivities = listOf(
            ActivityItem(
                tripId = parisId,
                dayNumber = 1,
                timeSlot = TimeSlot.MORNING,
                timeString = "09:30 AM",
                title = "Musée d'Orsay Impressionist Masterpieces",
                description = "Admire Monet, Van Gogh, and Renoir inside the breathtaking former Beaux-Arts railway station, with stunning views through the giant clock face.",
                locationName = "7th Arrondissement",
                estimatedCost = 16.0,
                category = ActivityCategory.ARTS_CULTURE,
                budgetTag = BudgetTag.MODERATE,
                tips = "Book the 9:30 AM first entrance to enjoy the 5th floor Impressionist gallery before tour groups arrive."
            ),
            ActivityItem(
                tripId = parisId,
                dayNumber = 1,
                timeSlot = TimeSlot.AFTERNOON,
                timeString = "02:00 PM",
                title = "Saint-Germain Cafe Terrace & Luxembourg Gardens",
                description = "Sip espresso and freshly baked croissants at a sidewalk cafe, then relax beside the Medici Fountain in the manicured gardens.",
                locationName = "Saint-Germain-des-Prés",
                estimatedCost = 12.0,
                category = ActivityCategory.RELAXATION,
                budgetTag = BudgetTag.CHEAP,
                tips = "Grab picnic baguette sandwiches from a nearby boulangerie for just €5 to eat in the garden."
            ),
            ActivityItem(
                tripId = parisId,
                dayNumber = 1,
                timeSlot = TimeSlot.EVENING,
                timeString = "07:30 PM",
                title = "Seine River Sunset Walk & Pont des Arts",
                description = "Golden hour stroll along the Seine quays with views of Notre-Dame and the Louvre illuminated against the twilight sky.",
                locationName = "Quai de la Seine",
                estimatedCost = 0.0,
                category = ActivityCategory.SIGHTSEEING,
                budgetTag = BudgetTag.FREE,
                tips = "Buskers and accordionists perform near Pont Neuf around sunset—pure Parisian atmosphere."
            ),
            ActivityItem(
                tripId = parisId,
                dayNumber = 2,
                timeSlot = TimeSlot.MORNING,
                timeString = "09:00 AM",
                title = "Montmartre & Sacré-Cœur Basilica Panorama",
                description = "Ascend the hill of Montmartre to discover the gleaming white domes of Sacré-Cœur and sweeping 360-degree vistas across all of Paris.",
                locationName = "Montmartre, 18th Arr.",
                estimatedCost = 0.0,
                category = ActivityCategory.SIGHTSEEING,
                budgetTag = BudgetTag.FREE,
                tips = "Basilica entry is free. Walk down the quieter Rue de l'Abreuvoir for postcard-worthy pink house photos."
            ),
            ActivityItem(
                tripId = parisId,
                dayNumber = 2,
                timeSlot = TimeSlot.AFTERNOON,
                timeString = "01:30 PM",
                title = "Traditional French Bistro Lunch & Place du Tertre",
                description = "Enjoy a classic 2-course formula lunch (French onion soup + duck confit) and watch portrait artists at work in the lively village square.",
                locationName = "Montmartre Square",
                estimatedCost = 24.0,
                category = ActivityCategory.FOOD_DINING,
                budgetTag = BudgetTag.MODERATE,
                tips = "Choose the 'Menu du Jour' (Daily Special) for authentic cooking at nearly half the à la carte price."
            ),
            ActivityItem(
                tripId = parisId,
                dayNumber = 3,
                timeSlot = TimeSlot.MORNING,
                timeString = "10:00 AM",
                title = "Le Marais Historic Mansions & Place des Vosges",
                description = "Explore Paris's most fashionable historic district with Renaissance mansions, falafel in Rue des Rosiers, and arcaded courtyards.",
                locationName = "Le Marais, 4th Arr.",
                estimatedCost = 9.0,
                category = ActivityCategory.SIGHTSEEING,
                budgetTag = BudgetTag.CHEAP,
                tips = "L'As du Fallafel serves the city's highest-rated falafel pita for under €10."
            )
        )
        activityDao.insertAllActivities(parisActivities.map { ActivityEntity.fromActivityItem(it) })
    }
}
