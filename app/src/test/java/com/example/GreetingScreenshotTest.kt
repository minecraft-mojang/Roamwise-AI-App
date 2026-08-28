package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.ActivityCategory
import com.example.data.model.ActivityItem
import com.example.data.model.BudgetTag
import com.example.data.model.BudgetTier
import com.example.data.model.TimeSlot
import com.example.data.model.TravelParty
import com.example.data.model.TravelStyle
import com.example.data.model.Trip
import com.example.data.model.TripWithActivities
import com.example.ui.components.BudgetTrackerCard
import com.example.ui.components.TripCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun trip_card_screenshot() {
        val sampleTrip = Trip(
            id = 1L,
            title = "Tokyo Food & Culture Explorer",
            destination = "Tokyo, Japan",
            country = "Japan",
            durationDays = 4,
            budgetTotal = 600.0,
            currency = "USD",
            budgetTier = BudgetTier.BUDGET,
            travelStyle = TravelStyle.BALANCED,
            travelParty = TravelParty.SOLO,
            interests = listOf("Street Food & Local Eats", "Historical Sights", "Art & Museums"),
            summary = "Budget-optimized Tokyo itinerary balancing iconic street eats with free temple sights."
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                TripCard(
                    trip = sampleTrip,
                    onClick = {},
                    onDelete = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/trip_card.png")
    }
}
