package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM cafe_state WHERE id = 1 LIMIT 1")
    fun getCafeStateFlow(): Flow<CafeState?>

    @Query("SELECT * FROM cafe_state WHERE id = 1 LIMIT 1")
    suspend fun getCafeStateDirect(): CafeState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCafeState(state: CafeState)

    @Update
    suspend fun updateCafeState(state: CafeState)

    @Query("SELECT * FROM cat ORDER BY adoptedTimestamp ASC")
    fun getAllCatsFlow(): Flow<List<CatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCat(cat: CatEntity): Long

    @Update
    suspend fun updateCat(cat: CatEntity)

    @Query("DELETE FROM cat")
    suspend fun clearAllCats()

    @Transaction
    suspend fun updateStateAndAddCat(state: CafeState, cat: CatEntity) {
        insertCafeState(state)
        insertCat(cat)
    }
}
