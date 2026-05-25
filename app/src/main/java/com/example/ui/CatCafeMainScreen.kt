package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CafeState
import com.example.data.CatEntity
import com.example.viewmodel.ActiveCustomer
import com.example.viewmodel.CatCafeViewModel
import com.example.viewmodel.PredefinedCat
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatCafeMainScreen(viewModel: CatCafeViewModel) {
    val cafeState by viewModel.cafeState.collectAsStateWithLifecycle()
    val cats by viewModel.adoptedCats.collectAsStateWithLifecycle()
    val activeCustomers by viewModel.activeCustomers.collectAsStateWithLifecycle()
    val boostTimeLeft by viewModel.boostTimeLeft.collectAsStateWithLifecycle()
    val lastMeow by viewModel.lastMeow.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) }
    var showResetDialog by remember { mutableStateOf(false) }

    // Navigation and tab descriptors
    val tabs = listOf(
        TabItem("Kawiarnia", Icons.Default.Home),
        TabItem("Koty", Icons.Default.Favorite),
        TabItem("Sklep", Icons.Default.ShoppingCart),
        TabItem("Statystyki", Icons.Default.Star)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Serca",
                            tint = SweetPeach
                        )
                        Text(
                            text = "Kocia Kawiarnia Tycoon",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    // Quick stats display
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .background(SweetPeach.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, SweetPeach.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "🐾", fontSize = 14.sp)
                            Text(
                                text = String.format("%.1f", cafeState.coins),
                                fontWeight = FontWeight.Bold,
                                color = SweetPeach,
                                fontSize = 14.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                        label = { Text(text = tab.title, fontWeight = FontWeight.Medium, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SweetPeach,
                            selectedTextColor = SweetPeach,
                            indicatorColor = SweetPeach.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag("nav_tab_$index")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Display Meow Alert if present
            AnimatedVisibility(
                visible = lastMeow.isNotEmpty(),
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = SweetCream),
                    border = BorderStroke(1.dp, SoftGinger.copy(alpha = 0.6f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = "🐱", fontSize = 24.sp)
                        Text(
                            text = lastMeow,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = DeepEspresso,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Display Active Boost alert
            if (boostTimeLeft > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = CoralPink),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = "🐟", fontSize = 18.sp)
                            Text(
                                text = "Głodne kotki! ZYSK SZYBKI x2 active!",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = "${boostTimeLeft}s",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .background(Color.Black.copy(0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Tabs Content switcher
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> CafeSimulatorTab(
                        viewModel = viewModel,
                        cafeState = cafeState,
                        cats = cats,
                        customers = activeCustomers,
                        boostTimeLeft = boostTimeLeft
                    )
                    1 -> CatsDexTab(
                        viewModel = viewModel,
                        cats = cats,
                        coins = cafeState.coins
                    )
                    2 -> UpgradesTab(
                        viewModel = viewModel,
                        state = cafeState
                    )
                    3 -> StatsAndSettingsTab(
                        state = cafeState,
                        cats = cats,
                        onResetClick = { showResetDialog = true }
                    )
                }
            }
        }
    }

    // Confirmation Wipe Reset Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(text = "Zresetować kawiarnię?", fontWeight = FontWeight.Bold) },
            text = { Text("Czy na pewno chcesz usunąć wszystkie zdobyte monety, zakupione ulepszenia i pożegnać się z adoptowanymi kotkami? Tego kroku nie można cofnąć.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.fullResetGame()
                        showResetDialog = false
                        selectedTab = 0
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Tak, zresetuj wszystko")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }
}

data class TabItem(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

// ==========================================
// TAB 1: CAFE SIMULATOR (PLAYGROUND & CLICKER)
// ==========================================
@Composable
fun CafeSimulatorTab(
    viewModel: CatCafeViewModel,
    cafeState: CafeState,
    cats: List<CatEntity>,
    customers: List<ActiveCustomer>,
    boostTimeLeft: Int
) {
    // Large tap size scale state
    var clickerScale by remember { mutableStateOf(1f) }
    val animatedScale by animateFloatAsState(
        targetValue = clickerScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium)
    )

    // Handle temporary scaling down when pressed
    LaunchedEffect(clickerScale) {
        if (clickerScale < 1f) {
            delay(100)
            clickerScale = 1f
        }
    }

    val maxSeats = (cats.size * 2).coerceAtLeast(2)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Cafe Capacity Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Poziom Kawiarni: ${cats.size} 🐈",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SweetPeach
                        )
                        Text(
                            text = "Seating / Stoliki: ${customers.size} / $maxSeats klientów",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    
                    // Simple interactive treat trigger
                    Button(
                        onClick = { viewModel.feedCatsTreat() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoralPink,
                            disabledContainerColor = CardboardGray
                        ),
                        enabled = cafeState.coins >= (25.0 + cafeState.catToysLevel * 10),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("feed_cats_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "🐟", fontSize = 16.sp)
                            Text(
                                text = "Kup Smakołyk (${String.format("%.0f", 25.0 + cafeState.catToysLevel * 10)} 🐾)", 
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Visual Simulator Desk Grid
        Text(
            text = "🐾 Sala Kawiarniana (Symulacja live):",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Box(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxWidth()
                .border(2.dp, CookieBrown, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(SweetCream, WarmLatte)
                    )
                )
        ) {
            if (maxSeats <= 0 || cats.isEmpty()) {
                // Empty state or introductory prompt
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "🙀 Kawiarnia jest pusta!", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Nie masz jeszcze żadnego kotka! Adoptuj swojego pierwszego kocura w zakładce 'Koty', aby przyciągnąć pierwszych klientów!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.7f)
                    )
                }
            } else {
                // Seating grids
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(maxSeats) { index ->
                        val activeCust = customers.find { it.tableIndex == index }
                        TableSeatingItem(index = index, customer = activeCust, cats = cats)
                    }
                }
            }

            // Render floating overlay texts
            viewModel.floatingTexts.forEach { ft ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center)
                ) {
                    // Quick state to animate coordinates upward
                    var currentY by remember { mutableStateOf(ft.yOffset) }
                    var opacity by remember { mutableStateOf(1.0f) }
                    
                    LaunchedEffect(Unit) {
                        animate(
                            initialValue = ft.yOffset,
                            targetValue = ft.yOffset - 120f,
                            animationSpec = tween(durationMillis = 800)
                        ) { value, _ ->
                            currentY = value
                        }
                    }
                    LaunchedEffect(Unit) {
                        animate(
                            initialValue = 1.0f,
                            targetValue = 0.0f,
                            animationSpec = tween(durationMillis = 850)
                        ) { value, _ ->
                            opacity = value
                        }
                    }

                    Text(
                        text = ft.text,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = when (ft.colorType) {
                            0 -> CaramelGold
                            1 -> CoralPink
                            else -> MintCream
                        },
                        modifier = Modifier
                            .offset(x = ft.xOffset.dp, y = currentY.dp)
                            .scale(1.2f)
                            .shadow(2.dp, CircleShape)
                            .alpha(opacity)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Large manual tap clicker
        Text(
            text = "☕ Parz kawę i zabawiaj kotki (Klikaj!):",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Box(
            modifier = Modifier
                .weight(0.9f)
                .fillMaxWidth()
                .scale(animatedScale)
                .shadow(6.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.sweepGradient(
                        colors = listOf(SoftGinger, SweetPeach, SoftGinger)
                    )
                )
                // Use absolute pointer input coordinates on tap
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            clickerScale = 0.94f
                        },
                        onTap = { offset ->
                            viewModel.onManualClick(offset.x / 4f, offset.y / 4f)
                        }
                    )
                }
                .testTag("giant_clicker_button"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "😻☕🐈‍⬛",
                    fontSize = 48.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = "KLIKNIJ ABY PARZYĆ KAWĘ",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Kliknięcie daje: +${String.format("%.1f", cafeState.coffeeMachineLevel.toDouble() * (1.0 + cats.size * 0.15))} 🐾",
                    color = SweetCream,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun TableSeatingItem(index: Int, customer: ActiveCustomer?, cats: List<CatEntity>) {
    val isOccupied = customer != null
    
    // Choose randomly a cute cat icon representing the table host
    val deskCat = remember(cats.size, index) {
        if (cats.isNotEmpty()) cats[index % cats.size] else null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .border(
                1.dp,
                if (isOccupied) SweetPeach.copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.2f),
                RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isOccupied) SweetCream else Color.White.copy(alpha = 0.5f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isOccupied && customer != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // User visual initial
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(SoftGinger, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = customer.name.take(1), 
                                    fontSize = 10.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = DeepEspresso
                                )
                            }
                            Text(
                                text = customer.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = DeepEspresso,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        // Bubble indicator of customer mood / item
                        Box(
                            modifier = Modifier
                                .background(CardboardGray.copy(0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 4.dp)
                        ) {
                            Text(text = "${customer.emoji} ${customer.currentThoughtEmoji}", fontSize = 11.sp)
                        }
                    }

                    Text(
                        text = "Zamawia: ${customer.orderedItem}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = CookieBrown,
                        maxLines = 1
                    )

                    // Cat host info
                    if (deskCat != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🐾 Gospodarz: ${deskCat.name} (${deskCat.breed.take(3)})", fontSize = 9.sp, color = Color.Gray)
                        }
                    }

                    // Processing satisfaction indicator
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(text = "Uroczych chwil...", fontSize = 8.sp, color = Color.Gray)
                            Text(text = "${(customer.progress * 100).toInt()}%", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { customer.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape),
                            color = CoralPink,
                            trackColor = CardboardGray.copy(0.4f)
                        )
                    }
                }
            } else {
                // Empty seating space
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Wolny stolik",
                        tint = Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Wolny stolik ${index + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )
                    Text(
                        text = "Oczekiwanie...",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 8.sp,
                        color = Color.Gray.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// ==========================================
// TAB 2: CATS DEX - ADOPT & TWOJE KOTKI
// ==========================================
@Composable
fun CatsDexTab(
    viewModel: CatCafeViewModel,
    cats: List<CatEntity>,
    coins: Double
) {
    // Collect next adoption queue index
    val nextCatIndexToAdopt = cats.size
    val nextAdoptableCat: PredefinedCat? = if (nextCatIndexToAdopt < viewModel.availablePredefinedCats.size) {
        viewModel.availablePredefinedCats[nextCatIndexToAdopt]
    } else {
        null
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Adoption Desk Header
        item {
            Text(
                text = "🐾 Kiosk Adopcyjny Kotów:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Dodanie kolejnego kocura to zwiększenie poziomu kawiarni i otwarcie dodatkowych 2 stolików dla klientów!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        // NEXT CAT ADOPTION OPTION
        item {
            if (nextAdoptableCat != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SweetCream),
                    border = BorderStroke(2.dp, SoftGinger),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "NOWA ADOPCJA 🐾",
                                fontWeight = FontWeight.Black,
                                color = SweetPeach,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .background(SweetPeach.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )

                            Text(
                                text = "Cena: ${String.format("%.1f", nextAdoptableCat.cost)} 🐾",
                                fontWeight = FontWeight.Bold,
                                color = DeepEspresso,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Virtual cat breed emoji
                            Text(
                                text = getBreedEmoji(nextAdoptableCat.breed),
                                fontSize = 48.sp,
                                modifier = Modifier
                                    .background(CellColorFromBreed(nextAdoptableCat.breed), CircleShape)
                                    .padding(8.dp)
                            )

                            Column {
                                Text(
                                    text = nextAdoptableCat.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = DeepEspresso
                                )
                                Text(
                                    text = "Rasa: ${nextAdoptableCat.breed}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CookieBrown
                                )
                                Text(
                                    text = "Efekt charm: +${String.format("%.1f", nextAdoptableCat.baseCharm)}x",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MintCream.let { DeepEspresso }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = nextAdoptableCat.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.adoptNextCat(nextAdoptableCat) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("adopt_cat_action"),
                            colors = ButtonDefaults.buttonColors(containerColor = SweetPeach),
                            enabled = coins >= nextAdoptableCat.cost,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Favorite, contentDescription = "Adoptuj")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ADOPTUJ I WEJDŹ NA POZIOM ${nextCatIndexToAdopt + 1}! 🐾",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MintCream.copy(0.2f)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "👑🏆🥇", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "KRÓLEWSKA KAWIARNIA!",
                            fontWeight = FontWeight.Bold,
                            color = DeepEspresso
                        )
                        Text(
                            text = "Adoptowałeś już wszystkie dostępne koty! Twoje imperium jest legendarne i klienci po prostu mdleją z zachwytu.",
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }

        // YOUR CURRENT ADOPTED CATS LIST
        item {
            Divider(color = CardboardGray.copy(alpha = 0.5f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "🐱 Twoje Adoptowane Kotki (${cats.size}):",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (cats.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "😿", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Brak adoptowanych kotków.",
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Text(
                        text = "Klikaj w parzenie kawy na pierwszym ekranie i zbierz monety na swoją pierwszą kocicę!",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = Color.Gray.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            items(cats) { cat ->
                MyCatProfileRow(cat = cat, coins = coins, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MyCatProfileRow(cat: CatEntity, coins: Double, viewModel: CatCafeViewModel) {
    val upgradeCost = 30.0 * Math.pow(1.5, (cat.level - 1).toDouble())
    val canAffordUpgrade = coins >= upgradeCost

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = getBreedEmoji(cat.breed),
                        fontSize = 36.sp,
                        modifier = Modifier
                            .background(CellColorFromBreed(cat.breed), CircleShape)
                            .padding(6.dp)
                    )

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = cat.name,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            // Golden heart fav
                            Icon(
                                imageVector = if (cat.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Ulubiony",
                                tint = if (cat.isFavorite) CoralPink else Color.Gray,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { viewModel.toggleFavorite(cat) }
                            )
                        }

                        Text(
                            text = "Breed: ${cat.breed} | Poziom Miłości: ${cat.level}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        
                        val activeCharmBonus = cat.baseCharm * (1.0 + (cat.level - 1) * 0.2)
                        Text(
                            text = "Charm Rate: +${String.format("%.1f", activeCharmBonus)}x",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SweetPeach
                        )
                    }
                }

                // Upgrade option
                Button(
                    onClick = { viewModel.upgradeCatLevel(cat) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SoftGinger,
                        disabledContainerColor = CardboardGray
                    ),
                    enabled = canAffordUpgrade,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("upgrade_cat_btn_${cat.name}")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Ulepsz ❤️", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${String.format("%.0f", upgradeCost)} 🐾", 
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 3: UPGRADES SHOP (KAWIARNIANE ASORTYMENTY)
// ==========================================
@Composable
fun UpgradesTab(viewModel: CatCafeViewModel, state: CafeState) {
    val itemsToUpgrade = listOf(
        UpgradeShopItem(
            id = 1,
            title = "Super Szybki Ekspres do Kawy ☕",
            subtitle = "Zwiększa moc ręcznych kliknięć o +1 🐾 na poziom, mnożone przez bonus od kotów.",
            level = state.coffeeMachineLevel,
            cost = 20.0 * Math.pow(1.5, (state.coffeeMachineLevel - 1).toDouble()),
            iconText = "☕",
            badge = "Parzenie ręczne",
            onUpgrade = { viewModel.buyCoffeeMachineUpgrade() }
        ),
        UpgradeShopItem(
            id = 2,
            title = "Zabawki dla Kota i Drapaki 🧸",
            subtitle = "Globalna premia przychodu od kotów wynosi +15% na poziom.",
            level = state.catToysLevel,
            cost = 40.0 * Math.pow(1.6, state.catToysLevel.toDouble()),
            iconText = "🧸",
            badge = "Charm Bonus",
            onUpgrade = { viewModel.buyCatToysUpgrade() }
        ),
        UpgradeShopItem(
            id = 3,
            title = "Zatrudnij Baristę Pomocnika 🧑‍🍳",
            subtitle = "Automatyczny parzyciel, który klika za Ciebie i dodaje pasywny zysk każdego sekunda.",
            level = state.baristaLevel,
            cost = 150.0 * Math.pow(1.8, state.baristaLevel.toDouble()),
            iconText = "🧑‍🍳",
            badge = "Pasywny Auto-Clicker",
            onUpgrade = { viewModel.buyBaristaUpgrade() }
        ),
        UpgradeShopItem(
            id = 4,
            title = "Kampania Marketingowa Social-Media 📣",
            subtitle = "Zwiększa tempo pojawiania się klientów oraz bazowe sumy napiwków o +25%.",
            level = state.marketingLevel,
            cost = 500.0 * Math.pow(2.0, state.marketingLevel.toDouble()),
            iconText = "📣",
            badge = "Napływ Klientów",
            onUpgrade = { viewModel.buyMarketingUpgrade() }
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "🏪 Kawiarniarskie Wyposażenie i Kadra:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Mądrze inwestuj w kawiarnię, aby zbalansować klikanie i pasywne generowanie przychodów!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        items(itemsToUpgrade) { item ->
            val affordable = state.coins >= item.cost

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.badge.uppercase(),
                            color = CookieBrown,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .background(SoftGinger.copy(0.15f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )

                        Text(
                            text = "Poziom: ${item.level}",
                            fontWeight = FontWeight.Bold,
                            color = SweetPeach,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = item.iconText,
                            fontSize = 32.sp,
                            modifier = Modifier
                                .background(CardboardGray.copy(0.3f), RoundedCornerShape(10.dp))
                                .padding(8.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = item.onUpgrade,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("buy_upgrade_button_${item.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SweetPeach,
                            disabledContainerColor = CardboardGray
                        ),
                        enabled = affordable,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "ZAKUP ZA: ${String.format("%.1f", item.cost)} 🐾",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

data class UpgradeShopItem(
    val id: Int,
    val title: String,
    val subtitle: String,
    val level: Int,
    val cost: Double,
    val iconText: String,
    val badge: String,
    val onUpgrade: () -> Unit
)

// ==========================================
// TAB 4: STATS AND SETTINGS (RESET BOARD)
// ==========================================
@Composable
fun StatsAndSettingsTab(
    state: CafeState,
    cats: List<CatEntity>,
    onResetClick: () -> Unit
) {
    var loadedByDev by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "📊 Kawiarniana Księgowość & Statystyki:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Raport o sukcesie Twojej Kiciej franczyzy",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Stats card indicators
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatMetricRow(label = "Aktualnie zebrane monety", value = "${String.format("%.1f", state.coins)} 🐾")
                    StatMetricRow(label = "Monety zebrane łącznie (Lifetime)", value = "${String.format("%.1f", state.lifetimeCoins)} 🐾")
                    StatMetricRow(label = "Ilość ugoszczonych kotków (Poziom)", value = "${cats.size} kotów")
                    StatMetricRow(label = "Liczba manualnych parzeń kawy", value = "${state.clicksCount} razy")
                    StatMetricRow(label = "Moc Ekspresu do Kawy", value = "Poziom ${state.coffeeMachineLevel}")
                    StatMetricRow(label = "Zatrudnieni Barisci pomocnicy", value = "${state.baristaLevel} baristów")
                    StatMetricRow(label = "Zabawki dla kotów (Global Charm)", value = "+${state.catToysLevel * 15}%")
                    StatMetricRow(label = "Przychód pasywny od klientów", value = "+${state.marketingLevel * 25}% z marketingu")
                }
            }
        }

        // Action settings area
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Ustawienia gry", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onResetClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Zresetuj dane")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Resetuj Kawiarnię (Zacznij od zera)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Informational footer about app quality
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Kocia Kawiarnia Tycoon v2.0",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.LightGray.let { Color.Gray }
                )
                Text(
                    text = "Dobrej jakości klimatyczna gra offline-first. Napisana w kotlinie przy użyciu Jetpack Compose i Room.",
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Gray.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun StatMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SweetPeach)
    }
}

// ==========================================
// UTILITY HELPERS FOR CAT STYLING
// ==========================================
fun getBreedEmoji(breed: String): String {
    return when (breed) {
        "Rudy Dachowiec" -> "🐈🍊"
        "Czarna Pantera" -> "🐈‍⬛🖤"
        "Puszysta Szynszyla" -> "🐈☁️"
        "Brytyjski Lord" -> "🐈🐾"
        "Perska Księżniczka" -> "🐩🌸"
        "Bengalski Odkrywca" -> "🐆🐾"
        "Maine Coon" -> "🦁🦁"
        "Ciepły Ragdoll" -> "🐈🧸"
        "Szkocki Zwisłouchy" -> "🐰🌸"
        "Syberyjski Monarcha" -> "👑🐯"
        else -> "🐈"
    }
}

fun CellColorFromBreed(breed: String): Color {
    return when (breed) {
        "Rudy Dachowiec" -> SoftGinger.copy(alpha = 0.25f)
        "Czarna Pantera" -> DeepEspresso.copy(alpha = 0.15f)
        "Puszysta Szynszyla" -> SweetCream.copy(alpha = 0.8f)
        "Brytyjski Lord" -> CardboardGray.copy(alpha = 0.4f)
        "Perska Księżniczka" -> CoralPink.copy(alpha = 0.15f)
        "Bengalski Odkrywca" -> SoftGinger.copy(alpha = 0.35f)
        "Maine Coon" -> CookieBrown.copy(alpha = 0.15f)
        "Ciepły Ragdoll" -> SweetCream.copy(alpha = 0.5f)
        "Szkocki Zwisłouchy" -> MintCream.copy(alpha = 0.4f)
        "Syberyjski Monarcha" -> CaramelGold.copy(alpha = 0.2f)
        else -> SoftGinger.copy(alpha = 0.2f)
    }
}
