package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ApiKeyManager
import com.example.data.model.ActivityCategory
import com.example.data.model.ActivityItem
import com.example.data.model.BudgetTag
import com.example.data.model.BudgetTier
import com.example.data.model.TimeSlot
import com.example.data.model.TravelParty
import com.example.data.model.TravelStyle
import com.example.data.model.Trip
import com.example.data.model.TripWithActivities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Itinerary Builder", appName)
    }

    @Test
    fun `test api key manager default and custom key handling`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ApiKeyManager.init(context)
        
        // Reset to default
        ApiKeyManager.resetToDefault()
        assertFalse(ApiKeyManager.isUsingCustomKey())
        assertEquals("YOUR_API_KEY_HERE", ApiKeyManager.getActiveApiKey())
        assertTrue(ApiKeyManager.getMaskedActiveKey().contains("••••"))

        // Set custom key
        ApiKeyManager.setCustomApiKey("AIzaSyPersonalCustomKey12345")
        assertTrue(ApiKeyManager.isUsingCustomKey())
        assertEquals("AIzaSyPersonalCustomKey12345", ApiKeyManager.getActiveApiKey())

        // Reset back
        ApiKeyManager.resetToDefault()
        assertFalse(ApiKeyManager.isUsingCustomKey())
        assertEquals("YOUR_API_KEY_HERE", ApiKeyManager.getActiveApiKey())
    }

    @Test
    fun `test trip budget tracker calculations`() {
        val trip = Trip(
            id = 1L,
            title = "Tokyo Explorer",
            destination = "Tokyo, Japan",
            durationDays = 3,
            budgetTotal = 500.0,
            currency = "USD",
            budgetTier = BudgetTier.BUDGET,
            travelStyle = TravelStyle.BALANCED,
            travelParty = TravelParty.SOLO,
            interests = listOf("Street Food", "Temples")
        )

        val activities = listOf(
            ActivityItem(
                id = 1L,
                tripId = 1L,
                dayNumber = 1,
                timeSlot = TimeSlot.MORNING,
                timeString = "09:00 AM",
                title = "Senso-ji Temple",
                description = "Temple visit",
                locationName = "Asakusa",
                estimatedCost = 0.0,
                category = ActivityCategory.SIGHTSEEING,
                budgetTag = BudgetTag.FREE
            ),
            ActivityItem(
                id = 2L,
                tripId = 1L,
                dayNumber = 1,
                timeSlot = TimeSlot.AFTERNOON,
                timeString = "01:00 PM",
                title = "Ramen Tasting",
                description = "Tonkotsu ramen",
                locationName = "Ueno",
                estimatedCost = 15.0,
                category = ActivityCategory.FOOD_DINING,
                budgetTag = BudgetTag.CHEAP
            )
        )

        val tripWithActs = TripWithActivities(trip = trip, activities = activities)

        assertEquals(15.0, tripWithActs.totalPlannedCost, 0.01)
        assertEquals(485.0, tripWithActs.remainingBudget, 0.01)
        assertFalse(tripWithActs.isOverBudget)
        assertEquals(1, tripWithActs.activitiesByDay.size)
        assertEquals(2, tripWithActs.activitiesByDay[1]?.size)
    }
}
