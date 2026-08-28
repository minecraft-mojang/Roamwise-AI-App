package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.ActivityCategory
import com.example.data.model.ActivityItem
import com.example.data.model.BudgetTag
import com.example.data.model.TimeSlot

@Entity(
    tableName = "activities",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["tripId"]), Index(value = ["tripId", "dayNumber"])]
)
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val dayNumber: Int,
    val timeSlot: String = TimeSlot.MORNING.name,
    val timeString: String = "09:00 AM",
    val title: String,
    val description: String,
    val locationName: String = "",
    val estimatedCost: Double = 0.0,
    val category: String = ActivityCategory.SIGHTSEEING.name,
    val budgetTag: String = BudgetTag.FREE.name,
    val tips: String = "",
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0
) {
    fun toActivityItem(): ActivityItem {
        val slot = try { TimeSlot.valueOf(timeSlot) } catch (e: Exception) { TimeSlot.MORNING }
        val cat = try { ActivityCategory.valueOf(category) } catch (e: Exception) { ActivityCategory.SIGHTSEEING }
        val bTag = try { BudgetTag.valueOf(budgetTag) } catch (e: Exception) { BudgetTag.FREE }

        return ActivityItem(
            id = id,
            tripId = tripId,
            dayNumber = dayNumber,
            timeSlot = slot,
            timeString = timeString,
            title = title,
            description = description,
            locationName = locationName,
            estimatedCost = estimatedCost,
            category = cat,
            budgetTag = bTag,
            tips = tips,
            isCompleted = isCompleted,
            sortOrder = sortOrder
        )
    }

    companion object {
        fun fromActivityItem(item: ActivityItem): ActivityEntity {
            return ActivityEntity(
                id = item.id,
                tripId = item.tripId,
                dayNumber = item.dayNumber,
                timeSlot = item.timeSlot.name,
                timeString = item.timeString,
                title = item.title,
                description = item.description,
                locationName = item.locationName,
                estimatedCost = item.estimatedCost,
                category = item.category.name,
                budgetTag = item.budgetTag.name,
                tips = item.tips,
                isCompleted = item.isCompleted,
                sortOrder = item.sortOrder
            )
        }
    }
}
