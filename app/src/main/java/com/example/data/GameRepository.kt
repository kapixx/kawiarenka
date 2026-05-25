package com.example.data

import kotlinx.coroutines.flow.Flow

class GameRepository(private val gameDao: GameDao) {
    val cafeState: Flow<CafeState?> = gameDao.getCafeStateFlow()
    val allCats: Flow<List<CatEntity>> = gameDao.getAllCatsFlow()

    suspend fun getCafeStateDirect(): CafeState? {
        return gameDao.getCafeStateDirect()
    }

    suspend fun saveCafeState(state: CafeState) {
        gameDao.insertCafeState(state)
    }

    suspend fun addNewCat(cat: CatEntity) {
        gameDao.insertCat(cat)
    }

    suspend fun updateCat(cat: CatEntity) {
        gameDao.updateCat(cat)
    }

    suspend fun transactionAddCat(state: CafeState, cat: CatEntity) {
        gameDao.updateStateAndAddCat(state, cat)
    }

    suspend fun resetGameData() {
        gameDao.clearAllCats()
        gameDao.insertCafeState(CafeState(id = 1))
    }
}
