package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cafe_state")
data class CafeState(
    @PrimaryKey val id: Int = 1,
    val coins: Double = 0.0,
    val lifetimeCoins: Double = 0.0,
    val clicksCount: Int = 0,
    val coffeeMachineLevel: Int = 1, // Manual click multiplier
    val catToysLevel: Int = 0,       // Global decoration multiplier
    val baristaLevel: Int = 0,       // Auto-clicking power
    val marketingLevel: Int = 0,     // Attracts more clients & tips
    val activeMultiplier: Double = 1.0,
    val lastActiveTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "cat")
data class CatEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val breed: String,
    val imageUrl: String, // visual representative
    val level: Int = 1,
    val baseCharm: Double, // earning speed multiplier
    val isFavorite: Boolean = false,
    val adoptedTimestamp: Long = System.currentTimeMillis()
)
