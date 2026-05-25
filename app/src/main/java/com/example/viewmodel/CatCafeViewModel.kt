package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

// Represents an active customer in our live simulator
data class ActiveCustomer(
    val id: Int,
    val tableIndex: Int,
    val name: String,
    val emoji: String,
    val progress: Float, // 0.0 to 1.0
    val orderedItem: String,
    val paymentValue: Double,
    val currentThoughtEmoji: String
)

// Floating text animation state
data class FloatingText(
    val id: Long,
    val text: String,
    val xOffset: Float,
    val yOffset: Float,
    val colorType: Int // 0 for yellow coins, 1 for red hearts, 2 for passive +
)

class CatCafeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository
    
    // Core game state exposed to UI safely
    val cafeState: StateFlow<CafeState>
    val adoptedCats: StateFlow<List<CatEntity>>

    // Live Simulator UI States (Transient, recalculated or run during coroutine loop)
    private val _activeCustomers = MutableStateFlow<List<ActiveCustomer>>(emptyList())
    val activeCustomers: StateFlow<List<ActiveCustomer>> = _activeCustomers.asStateFlow()

    // Floating text items that pop up when clicking
    val floatingTexts = mutableStateListOf<FloatingText>()
    private var floatIdCounter = 0L

    // Active boosts
    private val _boostTimeLeft = MutableStateFlow(0) // in seconds
    val boostTimeLeft: StateFlow<Int> = _boostTimeLeft.asStateFlow()

    // Sound / meow text pop-up message
    private val _lastMeow = MutableStateFlow("")
    val lastMeow: StateFlow<String> = _lastMeow.asStateFlow()

    // List of predefined cat species available for adoption
    val availablePredefinedCats = listOf(
        PredefinedCat("Mruczek", "Rudy Dachowiec", 15.0, 1.0, "🐾 Przyjazny i mięciutki, uwielbia drapanie za uchem."),
        PredefinedCat("Luna", "Czarna Pantera", 80.0, 1.6, "🐾 Elegancka dama o błyszczących oczach, kocha kartony."),
        PredefinedCat("Kluska", "Puszysta Szynszyla", 320.0, 2.8, "🐾 Okrągła i miękka kulka miłości, śpi w śmiesznych pozycjach."),
        PredefinedCat("Bazyl", "Brytyjski Lord", 1200.0, 4.8, "🐾 Stateczny jegomość o dystyngowanym spojrzeniu i gęstym futrze."),
        PredefinedCat("Bella", "Perska Księżniczka", 4500.0, 8.5, "🐾 Wymagająca arystokratka kawiarnianych poduszek, rozczula każdego."),
        PredefinedCat("Piorun", "Bengalski Odkrywca", 18000.0, 15.0, "🐾 Dziki pasiak, który skacze na najwyższe półki i bawi gości."),
        PredefinedCat("Frytka", "Maine Coon", 65000.0, 28.0, "🐾 Kolosalna kicia o rysich pędzelkach, ulubienica dzieci."),
        PredefinedCat("Cynamon", "Ciepły Ragdoll", 220000.0, 52.0, "🐾 Bezwładna szmaciana lalka, rozpływająca się przy braniu na ręce."),
        PredefinedCat("Chmurka", "Szkocki Zwisłouchy", 750000.0, 95.0, "🐾 Jej złożone uszka i niewinne oczka wymuszają rekordowe napiwki."),
        PredefinedCat("Lord Puszek", "Syberyjski Monarcha", 2500000.0, 180.0, "🐾 Król wszystkich kotów. Jego mruczenie wzmaga ochotę na espresso.")
    )

    // Predefined Polish client names
    private val clientNames = listOf(
        "Kasia", "Ania", "Tomek", "Kamil", "Monika", "Piotr", "Zosia", "Maciek",
        "Basia", "Mateusz", "Marta", "Dawid", "Ola", "Szymon", "Ada", "Kuba", "Natalia"
    )
    private val coffeeEmojis = listOf("☕", "🍵", "🧁", "🍩", "🍰", "🥛", "🧋")
    private val generalThoughts = listOf("😻", "❤️", "🥰", "😴", "🤩", "🐾", "☕")

    private var gameLoopJob: Job? = null

    init {
        val database = GameDatabase.getDatabase(application)
        repository = GameRepository(database.gameDao())

        // Collect flows and pipe them to UI StateFlows
        cafeState = repository.cafeState
            .map { it ?: CafeState() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = CafeState()
            )

        adoptedCats = repository.allCats
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        // Start the background game engine
        startGameEngine()
    }

    private fun startGameEngine() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            // First delay to let data load
            delay(500)
            
            // Auto initialize state if empty
            val currentDirect = repository.getCafeStateDirect()
            if (currentDirect == null) {
                repository.saveCafeState(CafeState(coins = 5.0, lifetimeCoins = 5.0))
            }

            var tickCounter = 0
            while (true) {
                delay(1000)
                tickCounter++

                val state = cafeState.value
                val cats = adoptedCats.value
                val catsCount = cats.size

                // 1. Calculate active boost
                var currentBoostMultiplier = 1.0
                if (_boostTimeLeft.value > 0) {
                    _boostTimeLeft.value -= 1
                    currentBoostMultiplier = 2.0
                }

                // 2. Baristas clicking action (Passive earnings based on baristas)
                var baristaEarnings = 0.0
                if (state.baristaLevel > 0) {
                    // Each barista generates automated brews: baristaLevel * clickPower * 0.4 per second
                    val clickPower = state.coffeeMachineLevel.toDouble()
                    baristaEarnings = state.baristaLevel * clickPower * 0.4
                }

                // 3. Customers progress action
                val customersList = _activeCustomers.value.toMutableList()
                var customerEarningsThisSecond = 0.0
                val finishedCustomerIndices = mutableListOf<Int>()

                // Global multiplier from toys and marketing
                val toyMultiplierBonus = 1.0 + (state.catToysLevel * 0.15)
                val marketingMultiplierBonus = 1.0 + (state.marketingLevel * 0.25)
                val totalMultiplier = toyMultiplierBonus * marketingMultiplierBonus * currentBoostMultiplier

                // Aggregate charm of our feline residents
                val totalCatCharm = cats.sumOf { it.baseCharm * (1.0 + (it.level - 1) * 0.2) }
                val averageCharm = if (catsCount > 0) totalCatCharm / catsCount.toDouble() else 1.0

                customersList.forEachIndexed { index, cust ->
                    // Customers advance their cozy time!
                    val newProgress = cust.progress + 0.15f // takes about 6-7 seconds
                    if (newProgress >= 1.0f) {
                        // Customer is fully satisfied, pays!
                        // Payment matches their designated value multiplied by the general cat charm rate and boosters!
                        val payment = cust.paymentValue * averageCharm * totalMultiplier
                        customerEarningsThisSecond += payment
                        finishedCustomerIndices.add(index)
                        
                        // Spawn floating payout text on their relative visually offset table
                        spawnFloatingText(
                            text = "+${String.format("%.1f", payment)} 🐾",
                            x = -100f + (cust.tableIndex % 3) * 120f,
                            y = -50f - (cust.tableIndex / 3) * 100f,
                            colorType = 0
                        )
                    } else {
                        // Customer continues sipping/petting, small micro-earnings or random cat thoughts!
                        val randomThoughtChance = Random.nextInt(100)
                        val updatedThought = if (randomThoughtChance < 15) {
                            generalThoughts.random()
                        } else {
                            cust.currentThoughtEmoji
                        }
                        customersList[index] = cust.copy(
                            progress = newProgress,
                            currentThoughtEmoji = updatedThought
                        )
                    }
                }

                // Remove finished customers
                for (i in finishedCustomerIndices.reversed()) {
                    if (i < customersList.size) {
                        customersList.removeAt(i)
                    }
                }

                // 4. Client Spawner
                // Maximum seats inside cafe depends on Level (cats count)!
                // Each cat unlocked adds 2 seating spaces in the cafe!
                val maxSeats = (catsCount * 2).coerceAtLeast(2)
                
                // If there are empty seats, try spawning a new customer
                if (customersList.size < maxSeats) {
                    val spawnChance = 30 + state.marketingLevel * 7
                    if (Random.nextInt(100) < spawnChance) {
                        // Find an unoccupied table index
                        val occupiedIndices = customersList.map { it.tableIndex }.toSet()
                        val availableTables = (0 until maxSeats).filter { it !in occupiedIndices }
                        if (availableTables.isNotEmpty()) {
                            val pickTable = availableTables.random()
                            val name = clientNames.random()
                            val emoji = coffeeEmojis.random()
                            
                            // Base payment scaling slightly with marketing level
                            val basePayment = 4.0 + (state.marketingLevel * 1.5)
                            
                            val newCustomer = ActiveCustomer(
                                id = Random.nextInt(1000000),
                                tableIndex = pickTable,
                                name = name,
                                emoji = emoji,
                                progress = 0.0f,
                                orderedItem = when (emoji) {
                                    "☕" -> "Espresso"
                                    "🍵" -> "Matcha"
                                    "🧁" -> "Muffin"
                                    "🍩" -> "Pączek"
                                    "🍰" -> "Tarta"
                                    "🥛" -> "Mleczko"
                                    else -> "Latte"
                                },
                                paymentValue = basePayment,
                                currentThoughtEmoji = "🐾"
                            )
                            customersList.add(newCustomer)
                        }
                    }
                }

                _activeCustomers.value = customersList

                // 5. Save all accumulative earnings back to database safely
                val totalSecEarnings = baristaEarnings + customerEarningsThisSecond
                if (totalSecEarnings > 0.0 || state.coins != currentDirect?.coins) {
                    val updatedState = state.copy(
                        coins = state.coins + totalSecEarnings,
                        lifetimeCoins = state.lifetimeCoins + totalSecEarnings,
                        lastActiveTime = System.currentTimeMillis()
                    )
                    repository.saveCafeState(updatedState)
                }

                // Random clean up of old floating texts to keep memory optimal
                if (floatingTexts.size > 25) {
                    floatingTexts.removeRange(0, 10)
                }
            }
        }
    }

    // Handles user tapping the giant cup/cat to click manually
    fun onManualClick(screenX: Float, screenY: Float) {
        viewModelScope.launch {
            val state = cafeState.value
            val cats = adoptedCats.value
            
            // Formula for click power: coffeeMachineLevel * (1.0 + 0.1 * totalCatCount)
            val baseClick = state.coffeeMachineLevel.toDouble()
            val catBonus = 1.0 + (cats.size * 0.15)
            val boostMultiplier = if (_boostTimeLeft.value > 0) 2.0 else 1.0
            
            val totalEarned = baseClick * catBonus * boostMultiplier

            // Upgrade coins
            val updatedState = state.copy(
                coins = state.coins + totalEarned,
                lifetimeCoins = state.lifetimeCoins + totalEarned,
                clicksCount = state.clicksCount + 1
            )
            repository.saveCafeState(updatedState)

            // Spawn visual puff marker right at clicking point
            spawnFloatingText(
                text = "+${String.format("%.1f", totalEarned)} 🐾",
                x = screenX - 50f + Random.nextInt(-30, 30),
                y = screenY - 120f + Random.nextInt(-20, 20),
                colorType = 0
            )

            // Random 3% chance for the active cats to meow on click!
            if (cats.isNotEmpty() && Random.nextInt(100) < 6) {
                triggerMeowSound(cats.random().name)
            }
        }
    }

    private fun spawnFloatingText(text: String, x: Float, y: Float, colorType: Int) {
        val fid = floatIdCounter++
        val ft = FloatingText(id = fid, text = text, xOffset = x, yOffset = y, colorType = colorType)
        floatingTexts.add(ft)
        
        // Remove from UI hierarchy after animation completes (approx 800ms)
        viewModelScope.launch {
            delay(1000)
            floatingTexts.removeAll { it.id == fid }
        }
    }

    private fun triggerMeowSound(catName: String) {
        val meows = listOf(
            "Miau! ~🐾", "Mrrrp!", "Nyah!", "*Mruczenie*", "*Ziewa*", "Chcę smakołyk! 🐟", "Patrz na mnie!"
        )
        _lastMeow.value = "$catName: ${meows.random()}"
        viewModelScope.launch {
            delay(3000)
            if (_lastMeow.value.startsWith(catName)) {
                _lastMeow.value = ""
            }
        }
    }

    // Interactive treat / petting boost
    fun feedCatsTreat() {
        val cost = 25.0 + (cafeState.value.catToysLevel * 10)
        val state = cafeState.value
        if (state.coins >= cost) {
            viewModelScope.launch {
                // Deduct coins & start 30 seconds double earnings booster
                val updatedState = state.copy(
                    coins = state.coins - cost
                )
                repository.saveCafeState(updatedState)
                
                _boostTimeLeft.value += 30
                
                // Spawn beautiful fireworks of hearts
                for (i in 1..8) {
                    spawnFloatingText(
                        text = "🐟 Mrał! ❤️",
                        x = -150f + Random.nextFloat() * 300f,
                        y = -100f - Random.nextFloat() * 200f,
                        colorType = 1
                    )
                }
                
                triggerMeowSound("Wszystkie Kotki")
            }
        }
    }

    // Upgrades Purchases Shop
    fun buyCoffeeMachineUpgrade() {
        val state = cafeState.value
        val cost = 20.0 * Math.pow(1.5, (state.coffeeMachineLevel - 1).toDouble())
        if (state.coins >= cost) {
            viewModelScope.launch {
                val updatedState = state.copy(
                    coins = state.coins - cost,
                    coffeeMachineLevel = state.coffeeMachineLevel + 1
                )
                repository.saveCafeState(updatedState)
                spawnFloatingText("☕ Ulepszono Ekspres!", 0f, -150f, 2)
            }
        }
    }

    fun buyCatToysUpgrade() {
        val state = cafeState.value
        val cost = 40.0 * Math.pow(1.6, state.catToysLevel.toDouble())
        if (state.coins >= cost) {
            viewModelScope.launch {
                val updatedState = state.copy(
                    coins = state.coins - cost,
                    catToysLevel = state.catToysLevel + 1
                )
                repository.saveCafeState(updatedState)
                spawnFloatingText("🧸 Kupiono Zabawki!", 0f, -150f, 2)
            }
        }
    }

    fun buyBaristaUpgrade() {
        val state = cafeState.value
        val cost = 150.0 * Math.pow(1.8, state.baristaLevel.toDouble())
        if (state.coins >= cost) {
            viewModelScope.launch {
                val updatedState = state.copy(
                    coins = state.coins - cost,
                    baristaLevel = state.baristaLevel + 1
                )
                repository.saveCafeState(updatedState)
                spawnFloatingText("🧑‍🍳 Zatrudniono Baristę!", 0f, -150f, 2)
            }
        }
    }

    fun buyMarketingUpgrade() {
        val state = cafeState.value
        val cost = 500.0 * Math.pow(2.0, state.marketingLevel.toDouble())
        if (state.coins >= cost) {
            viewModelScope.launch {
                val updatedState = state.copy(
                    coins = state.coins - cost,
                    marketingLevel = state.marketingLevel + 1
                )
                repository.saveCafeState(updatedState)
                spawnFloatingText("📣 Odpalono Reklamę!", 0f, -150f, 2)
            }
        }
    }

    // Adopt a New Cat
    fun adoptNextCat(predefinedCat: PredefinedCat) {
        val state = cafeState.value
        if (state.coins >= predefinedCat.cost) {
            viewModelScope.launch {
                val updatedState = state.copy(
                    coins = state.coins - predefinedCat.cost
                )
                
                val nextCat = CatEntity(
                    name = predefinedCat.name,
                    breed = predefinedCat.breed,
                    imageUrl = predefinedCat.breed,
                    level = 1,
                    baseCharm = predefinedCat.baseCharm,
                    isFavorite = false
                )
                
                repository.transactionAddCat(updatedState, nextCat)
                
                _lastMeow.value = "Nowy Kotek w kawiarni! Witamy ${predefinedCat.name}! 🐾😻"
                
                // Spawn mega sparkles
                for (i in 1..10) {
                    spawnFloatingText(
                        text = "🐾 HUUURRRA! ❤️",
                        x = -200f + Random.nextFloat() * 400f,
                        y = -150f - Random.nextFloat() * 250f,
                        colorType = 1
                    )
                }
            }
        }
    }

    // Upgrade individual Cat Level
    fun upgradeCatLevel(cat: CatEntity) {
        val cost = 30.0 * Math.pow(1.5, (cat.level - 1).toDouble())
        val state = cafeState.value
        if (state.coins >= cost) {
            viewModelScope.launch {
                // Deduct coins
                val updatedState = state.copy(
                    coins = state.coins - cost
                )
                repository.saveCafeState(updatedState)

                // Update individual cat properties
                val updatedCat = cat.copy(level = cat.level + 1)
                repository.updateCat(updatedCat)

                spawnFloatingText("🐱 ${cat.name} staje się uroczym lv ${cat.level + 1}!", 0f, -100f, 1)
                triggerMeowSound(cat.name)
            }
        }
    }

    // Toggle Favorite Cat
    fun toggleFavorite(cat: CatEntity) {
        viewModelScope.launch {
            repository.updateCat(cat.copy(isFavorite = !cat.isFavorite))
        }
    }

    // Delete database in case of reset
    fun fullResetGame() {
        viewModelScope.launch {
            _activeCustomers.value = emptyList()
            _boostTimeLeft.value = 0
            _lastMeow.value = ""
            repository.resetGameData()
        }
    }

    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
    }
}

// Represent helper for available predefined cats
data class PredefinedCat(
    val name: String,
    val breed: String,
    val cost: Double,
    val baseCharm: Double,
    val description: String
)
