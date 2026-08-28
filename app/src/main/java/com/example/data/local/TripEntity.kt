package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.BudgetTier
import com.example.data.model.TravelParty
import com.example.data.model.TravelStyle
import com.example.data.model.Trip

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val destination: String,
    val country: String = "",
    val durationDays: Int,
    val startDate: String = "",
    val budgetTotal: Double,
    val currency: String = "USD",
    val budgetTier: String = BudgetTier.MODERATE.name,
    val travelStyle: String = TravelStyle.BALANCED.name,
    val travelParty: String = TravelParty.SOLO.name,
    val interests: String = "", // Comma-separated or JSON list
    val summary: String = "",
    val coverGradientIndex: Int = 0,
    val specialNotes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toTrip(): Trip {
        val interestList = if (interests.isBlank()) emptyList() else interests.split(",").map { it.trim() }
        val tier = try { BudgetTier.valueOf(budgetTier) } catch (e: Exception) { BudgetTier.MODERATE }
        val style = try { TravelStyle.valueOf(travelStyle) } catch (e: Exception) { TravelStyle.BALANCED }
        val party = try { TravelParty.valueOf(travelParty) } catch (e: Exception) { TravelParty.SOLO }

        return Trip(
            id = id,
            title = title,
            destination = destination,
            country = country,
            durationDays = durationDays,
            startDate = startDate,
            budgetTotal = budgetTotal,
            currency = currency,
            budgetTier = tier,
            travelStyle = style,
            travelParty = party,
            interests = interestList,
            summary = summary,
            coverGradientIndex = coverGradientIndex,
            specialNotes = specialNotes,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromTrip(trip: Trip): TripEntity {
            return TripEntity(
                id = trip.id,
                title = trip.title,
                destination = trip.destination,
                country = trip.country,
                durationDays = trip.durationDays,
                startDate = trip.startDate,
                budgetTotal = trip.budgetTotal,
                currency = trip.currency,
                budgetTier = trip.budgetTier.name,
                travelStyle = trip.travelStyle.name,
                travelParty = trip.travelParty.name,
                interests = trip.interests.joinToString(","),
                summary = trip.summary,
                coverGradientIndex = trip.coverGradientIndex,
                specialNotes = trip.specialNotes,
                createdAt = trip.createdAt
            )
        }
    }
}
