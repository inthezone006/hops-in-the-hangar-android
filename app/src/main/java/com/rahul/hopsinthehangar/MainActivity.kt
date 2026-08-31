package com.rahul.hopsinthehangar

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlin.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.rahul.hopsinthehangar.ui.theme.HopsInTheHangarTheme
import com.rahul.hopsinthehangar.ui.theme.PremierPurple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

fun getResourceName(name: String?): String {
    if (name == null) return ""
    // Special case for Mama Bear's Mac
    if (name.contains("Mama Bear", ignoreCase = true) && name.contains("Mac", ignoreCase = true)) {
        return "mamabears_mac"
    }
    
    return name.lowercase()
        .replace("&", " and ")
        .replace(" ", "_")
        .replace(Regex("[^a-z0-9_]"), "")
        .replace(Regex("__+"), "_")
        .trim('_')
}

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "favorites")

class FavoritesRepository(private val dataStore: DataStore<Preferences>) {
    private val favoritesKey = stringSetPreferencesKey("favorite_ids")

    val favoriteIds: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[favoritesKey] ?: emptySet()
    }

    suspend fun toggleFavorite(id: String) {
        dataStore.edit { preferences ->
            val current = preferences[favoritesKey] ?: emptySet()
            if (current.contains(id)) {
                preferences[favoritesKey] = current - id
            } else {
                preferences[favoritesKey] = current + id
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        
        setContent {
            HopsInTheHangarTheme {
                MainScreen()
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Sponsors : Screen("sponsors", "Sponsors", Icons.Default.Star)
    object Entertainment : Screen("entertainment", "Events", Icons.AutoMirrored.Filled.List)
    object Vendors : Screen("vendors", "Vendors", Icons.Default.ShoppingCart)
    object Detail : Screen("detail/{type}/{id}", "Detail", Icons.Default.Info)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(analytics: FirebaseAnalytics? = Firebase.analytics) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // FAB Visibility state
    var isFabVisible by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // available.y < 0 means scrolling down
                // -40f threshold for "fast" scroll down
                if (available.y < -40f) {
                    isFabVisible = false
                } else if (available.y > 10f) {
                    // Any significant scroll up shows it back
                    isFabVisible = true
                }
                return Offset.Zero
            }
        }
    }

    // Data Management
    val repository = remember { FavoritesRepository(context.dataStore) }
    val favoriteIds by repository.favoriteIds.collectAsState(initial = emptySet())
    var eventData by remember { mutableStateOf<EventData?>(null) }

    LaunchedEffect(Unit) {
        eventData = loadEventData(context)
    }

    // Log screen views
    LaunchedEffect(currentRoute) {
        // Reset FAB visibility when switching screens
        isFabVisible = true
        
        currentRoute?.let { route ->
            analytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                param(FirebaseAnalytics.Param.SCREEN_NAME, route)
                param(FirebaseAnalytics.Param.SCREEN_CLASS, "MainActivity")
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Sponsors,
        Screen.Entertainment,
        Screen.Vendors
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val title = bottomNavItems.find { it.route == currentRoute }?.label?.uppercase() ?: "HOPS IN THE HANGAR"
            
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    ) 
                },
                navigationIcon = {
                    if (currentRoute?.startsWith("detail") == true) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                bottomNavItems.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            unselectedIconColor = MaterialTheme.colorScheme.secondary,
                            unselectedTextColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = isFabVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                    FloatingActionButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://middletownaviationfoundation.ticketspice.com/hops-in-the-hangar-2026"))
                            context.startActivity(intent)
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(
                            imageVector = Icons.Default.ConfirmationNumber,
                            contentDescription = "Get Tickets"
                        )
                    }
                }
            }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen(eventData) }
            composable(Screen.Sponsors.route) { 
                SponsorsScreen(
                    sponsors = eventData?.sponsors ?: emptyList(),
                    onSponsorClick = { id -> 
                        navController.navigate("detail/sponsor/$id")
                    },
                    favoriteIds = favoriteIds.toList(),
                    onToggleFavorite = { id -> 
                        scope.launch { repository.toggleFavorite(id) }
                    }
                ) 
            }
            composable(Screen.Entertainment.route) { 
                EntertainmentScreen(
                    schedule = eventData?.schedule ?: emptyList()
                ) 
            }
            composable(Screen.Vendors.route) { 
                VendorsScreen(
                    vendors = eventData?.vendors ?: emptyList(),
                    onVendorClick = { id -> 
                        analytics?.logEvent("vendor_detail_view") {
                            param("vendor_id", id)
                        }
                        navController.navigate("detail/vendor/$id") 
                    },
                    favoriteIds = favoriteIds.toList(),
                    onToggleFavorite = { id -> 
                        scope.launch { repository.toggleFavorite(id) }
                    }
                ) 
            }
            composable(Screen.Detail.route) { backStackEntry ->
                val type = backStackEntry.arguments?.getString("type") ?: ""
                val id = backStackEntry.arguments?.getString("id") ?: ""
                val item = when(type) {
                    "vendor" -> eventData?.vendors?.find { it.name == id }
                    "sponsor" -> eventData?.sponsors?.find { it.name == id }
                    else -> null
                }
                DetailScreen(type, id, item)
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoBackground(videoResIds: List<Int>) {
    val context = LocalContext.current
    var currentVideoIndex by remember { mutableIntStateOf(0) }
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            // Setup the player
            repeatMode = Player.REPEAT_MODE_OFF // We'll handle looping/cycling manually
            playWhenReady = true
            
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        // Cycle to next video
                        currentVideoIndex = (currentVideoIndex + 1) % videoResIds.size
                    }
                }
            })
        }
    }

    // Effect to update media item when index changes
    LaunchedEffect(currentVideoIndex, videoResIds) {
        if (videoResIds.isNotEmpty()) {
            val videoResId = videoResIds[currentVideoIndex]
            val uri = "android.resource://${context.packageName}/$videoResId".toUri()
            exoPlayer.setMediaItem(MediaItem.fromUri(uri))
            
            // Set clipping to 7 seconds (7,000,000 microseconds)
            // Note: Media3 clipping is done via MediaItem.ClippingConfiguration
            val clippedItem = MediaItem.Builder()
                .setUri(uri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setEndPositionMs(7000) // Cut to 7 seconds
                        .build()
                )
                .build()
            
            exoPlayer.setMediaItem(clippedItem)
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Serializable
data class EventData(
    val sponsors: List<SponsorItem>,
    val vendors: List<VendorItem>,
    val schedule: List<ScheduleItem>,
    val info: GeneralInfo,
    val faq: List<FaqItemData> = emptyList()
)

@Serializable
data class FaqItemData(val question: String, val answer: String)

@Serializable
data class SponsorLink(val label: String, val url: String)

@Serializable
data class SponsorItem(
    val name: String,
    val level: String,
    val description: String,
    val website: String? = null,
    val links: List<SponsorLink>? = null,
    val mapId: String? = null
)

@Serializable
data class VendorItem(
    val name: String,
    val category: String,
    val description: String,
    val email: String? = null,
    val phone: String? = null,
    val website: String? = null,
    val mapId: String? = null
)

@Serializable
data class ScheduleItem(val time: String, val event: String)

@Serializable
data class GeneralInfo(
    val parking: String,
    val rules: String,
    val hotels: List<HotelItem>
)

@Serializable
data class HotelItem(val name: String, val link: String)

suspend fun loadEventData(context: Context): EventData? = withContext(Dispatchers.IO) {
    try {
        val jsonString = context.assets.open("event_data.json").bufferedReader().use { it.readText() }
        val json = Json { 
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        json.decodeFromString<EventData>(jsonString)
    } catch (e: Exception) {
        Log.e("DataLoader", "Error loading event data: ${e.message}", e)
        null
    }
}

@Composable
fun LinkifyText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    onNonLinkClick: (() -> Unit)? = null
) {
    val annotatedString = buildAnnotatedString {
        append(text)
        
        val linkStyles = TextLinkStyles(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                fontWeight = FontWeight.Bold
            )
        )

        // URLs
        val urlMatcher = Patterns.WEB_URL.matcher(text)
        while (urlMatcher.find()) {
            val url = urlMatcher.group()
            if (url.startsWith("http://") || url.startsWith("https://")) {
                addLink(
                    url = LinkAnnotation.Url(
                        url = url,
                        styles = linkStyles
                    ),
                    start = urlMatcher.start(),
                    end = urlMatcher.end()
                )
            }
        }
        
        // Emails
        val emailMatcher = Patterns.EMAIL_ADDRESS.matcher(text)
        while (emailMatcher.find()) {
            val email = emailMatcher.group()
            addLink(
                url = LinkAnnotation.Url(
                    url = "mailto:$email",
                    styles = linkStyles
                ),
                start = emailMatcher.start(),
                end = emailMatcher.end()
            )
        }
    }

    Text(
        text = annotatedString,
        modifier = if (onNonLinkClick != null) {
            modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onNonLinkClick()
            }
        } else {
            modifier
        },
        style = style.copy(color = color, textAlign = textAlign ?: style.textAlign)
    )
}

@Composable
fun FaqSection(faqItems: List<FaqItemData>) {
    Text(
        "FAQ",
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.sp
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        faqItems.forEach { item ->
            var expanded by remember { mutableStateOf(false) }
            ElevatedCard(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            item.question,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    AnimatedVisibility(visible = expanded) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            LinkifyText(
                                text = item.answer,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                onNonLinkClick = { expanded = !expanded }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(eventData: EventData?) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        Surface(
            modifier = Modifier.size(140.dp),
            shape = CircleShape,
            color = Color.White, // Use white background for the circle
            border = BorderStroke(3.dp, Color.White)
        ) {
            Box(contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = R.mipmap.ic_launcher,
                    contentDescription = "App Icon",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Welcome to the Show",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                val fullText = "Welcome to Hops in the Hangar, your Craft Beer & Airshow event app! Explore a lineup of vendors and sponsors, discover detailed venue information, find the best hotels nearby, enjoy exciting entertainment, and get to know the featured airshow performers.\n\nCraft beer, beverages, and aircraft come together to create not only a fun social event, but also an extremely unique community experience. Hops in the Hangar celebrates aviation, local businesses, and great craft beverages while bringing people together for an unforgettable evening at the Middletown Regional Airport.\n\nWhether you're here for the thrilling air show performances, the incredible selection of breweries and beverage vendors, or simply to enjoy time with friends and family, this app will help you make the most of your experience. Stay connected with schedules, updates, event maps, and everything you need for an amazing experience at Hops in the Hangar 2026."
                val firstParagraph = fullText.substringBefore("\n\n")
                
                Text(
                    if (expanded) fullText else firstParagraph,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Show Less" else "Show More"
                    )
                }
            }
        }
        
        if (eventData != null) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "IN THE NEWS",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "Hops 2026 Recap",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "As featured on WLWT, Hops in the Hangar 2026 was a stellar celebration of craft beer and aviation. Saturday, August 22nd at the Middletown Regional Airport proved to be a perfect backdrop for a fun night where specialty beer enthusiasts and plane lovers combined their passions into one unforgettable experience.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val newsUrl = "https://www.wlwt.com/article/annual-hops-in-the-hangar-fundraiser-middletown-regional-airport/73466732?utm_campaign=snd-autopilot&fbclid=IwY2xjawUANr9wZG9mBWV4dG4DYWVtAjEwAGJyaWQRMVlwcXpNeXpWYUNFWWhGR29zcnRjBmFwcF9pZBAyMjIwMzkxNzg4MjAwODkyAAEe-doI-qUEQTUaFejKpMXCGEVudnU0I_GSflwfU8n9y6sPHQnYEw02Nxthr-I_aem_yoV-Z7vcQlzoQCkJhKZvYQ"
                    val annotatedNewsString = buildAnnotatedString {
                        append("Watch the full news segment ")
                        withLink(
                            LinkAnnotation.Url(
                                url = newsUrl,
                                styles = TextLinkStyles(
                                    style = SpanStyle(
                                        color = MaterialTheme.colorScheme.primary,
                                        textDecoration = TextDecoration.Underline,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            )
                        ) {
                            append("here")
                        }
                        append(".")
                    }
                    Text(
                        text = annotatedNewsString,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "VENUE & LOGISTICS",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Parking", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    LinkifyText(eventData.info.parking, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Event Rules", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    LinkifyText(eventData.info.rules, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            FaqSection(eventData.faq)

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "NEARBY HOTELS",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "Just a quick 15 minute drive there are hotels right by the I75 ramp off of 122.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val context = LocalContext.current
                    eventData.info.hotels.forEach { hotel ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { 
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse(hotel.link))
                                    context.startActivity(intent)
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Hotel, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(hotel.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "OUR TEAM",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Middletown Aviation Foundation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Your Hops in the Hangar Crew",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                val crew = listOf(
                    "Rich Bevis", "Kurt Yearout", "Sara Yearout", "Tom Spielmann",
                    "Sean Askren", "Mica Jones", "Missy Lawwill", "Jamie Murphy"
                )
                
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    maxItemsInEachRow = 3
                ) {
                    crew.forEach { name ->
                        Surface(
                            modifier = Modifier.padding(4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                        ) {
                            Text(
                                name,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        val context = LocalContext.current
        val versionName = remember {
            try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (e: Exception) {
                "Unknown"
            }
        }
        
        Text(
            text = "v$versionName",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(64.dp))
    }
}

@Composable
fun GlassCard(title: String, description: String) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.White.copy(alpha = 0.15f),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SponsorsScreen(
    sponsors: List<SponsorItem>, 
    onSponsorClick: (String) -> Unit,
    favoriteIds: List<String>,
    onToggleFavorite: (String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val filteredSponsors = sponsors.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.level.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            placeholder = { Text("Search Sponsors...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
            )
        )

        val pinnedNames = setOf("City of Middletown", "MWO", "Start Skydiving", "Team Fastrax")
        val pinnedSponsors = filteredSponsors.filter { it.name in pinnedNames }
        val otherSponsors = filteredSponsors.filter { it.name !in pinnedNames }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (pinnedSponsors.isNotEmpty()) {
                item {
                    Text(
                        "PREMIER SPONSORS",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(pinnedSponsors) { sponsor ->
                    SponsorCard(
                        sponsor = sponsor,
                        isFavorite = favoriteIds.contains(sponsor.name),
                        onToggleFavorite = onToggleFavorite,
                        isPinned = true
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            items(otherSponsors) { sponsor ->
                SponsorCard(
                    sponsor = sponsor,
                    isFavorite = favoriteIds.contains(sponsor.name),
                    onToggleFavorite = onToggleFavorite,
                    isPinned = false
                )
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SponsorCard(
    sponsor: SponsorItem,
    isFavorite: Boolean,
    onToggleFavorite: (String) -> Unit,
    isPinned: Boolean
) {
    val context = LocalContext.current
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = sponsor.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Which website would you like to visit?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                sponsor.links?.forEach { link ->
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Log.e("SponsorsScreen", "Error opening website: ${link.url}", e)
                            }
                            showBottomSheet = false
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(link.label)
                    }
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val links = sponsor.links
                if (links != null && links.size > 1) {
                    showBottomSheet = true
                } else {
                    val url = links?.firstOrNull()?.url ?: sponsor.website
                    
                    val noWebsiteSponsors = setOf("lewis horticultural", "askren balloon team", "kara goheen friends", "rh seals")
                    val isNoWebsite = noWebsiteSponsors.any { sponsor.name.lowercase().contains(it) }

                    if (isNoWebsite || url.isNullOrBlank()) {
                        Toast.makeText(context, "A website does not exist for this sponsor.", Toast.LENGTH_SHORT).show()
                    } else {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("SponsorsScreen", "Error opening website: $url", e)
                        }
                    }
                }
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPinned) PremierPurple else MaterialTheme.colorScheme.surface,
            contentColor = if (isPinned) Color(0xFF0A192F) else MaterialTheme.colorScheme.onSurface
        ),
        border = if (isPinned) BorderStroke(2.dp, Color.White.copy(alpha = 0.5f)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPinned) 8.dp else 2.dp)
    ) {
        ListItem(
            headlineContent = { 
                Text(
                    sponsor.name, 
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isPinned) Color(0xFF0A192F) else Color.Unspecified
                ) 
            },
            supportingContent = { 
                Text(
                    sponsor.description, 
                    color = if (isPinned) Color(0xFF0A192F).copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                ) 
            },
            overlineContent = { 
                Text(
                    sponsor.level.uppercase(), 
                    color = if (isPinned) Color(0xFF0A192F) else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                ) 
            },
            leadingContent = {
                val names = if (
                    sponsor.name.contains("Kara Goheen", ignoreCase = true) || 
                    sponsor.name.contains("Affordable Dentures", ignoreCase = true)
                ) {
                    listOf(sponsor.name)
                } else {
                    sponsor.name.split("&").map { it.trim() }
                }
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp) // Move down to center visually
                        .width(if (names.size > 1) 72.dp else 56.dp)
                        .height(56.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    names.forEachIndexed { index, name ->
                        val resourceName = getResourceName(name)
                        val context = LocalContext.current
                        val resourceId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
                        
                        Surface(
                            modifier = Modifier
                                .padding(start = (index * 24).dp)
                                .size(56.dp),
                            shape = CircleShape,
                            color = Color.White,
                            border = BorderStroke(2.dp, if (isPinned) Color.White else MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (resourceId != 0) {
                                    AsyncImage(
                                        model = resourceId,
                                        contentDescription = name,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (isPinned) PremierPurple else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
                headlineColor = if (isPinned) Color(0xFF0A192F) else MaterialTheme.colorScheme.onSurface,
                supportingColor = if (isPinned) Color(0xFF0A192F).copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                overlineColor = if (isPinned) Color(0xFF0A192F) else MaterialTheme.colorScheme.primary
            ),
            trailingContent = {
                IconButton(onClick = { onToggleFavorite(sponsor.name) }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else (if (isPinned) Color(0xFF0A192F) else MaterialTheme.colorScheme.outline)
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorsScreen(
    vendors: List<VendorItem>,
    onVendorClick: (String) -> Unit,
    favoriteIds: List<String>,
    onToggleFavorite: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategories by remember { mutableStateOf(setOf("Brewery", "Food Truck")) }
    
    val filteredVendors = vendors.filter {
        (it.name.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true)) &&
        selectedCategories.contains(it.category)
    }

    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Filter Vendors",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                listOf("Brewery", "Food Truck").forEach { category ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedCategories = if (selectedCategories.contains(category)) {
                                    selectedCategories - category
                                } else {
                                    selectedCategories + category
                                }
                            },
                        color = Color.Transparent
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Checkbox(
                                checked = selectedCategories.contains(category),
                                onCheckedChange = {
                                    selectedCategories = if (selectedCategories.contains(category)) {
                                        selectedCategories - category
                                    } else {
                                        selectedCategories + category
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = category,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Apply Filters")
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search Vendors...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                )
            )
            
            IconButton(onClick = { showFilterSheet = true }) {
                Icon(Icons.Default.FilterList, contentDescription = "Filter")
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(filteredVendors) { vendor ->
                val isFavorite = favoriteIds.contains(vendor.name)
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    onClick = { onVendorClick(vendor.name) },
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    ListItem(
                        headlineContent = { Text(vendor.name, fontWeight = FontWeight.ExtraBold) },
                        supportingContent = { Text(vendor.description, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
                        overlineContent = { 
                            Text(
                                vendor.category.uppercase(), 
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            ) 
                        },
                        leadingContent = {
                            val context = LocalContext.current
                            val resourceName = getResourceName(vendor.name)
                            val resourceId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)

                            Box(modifier = Modifier.padding(top = 8.dp)) { // Move down to center visually
                                Surface(
                                    modifier = Modifier.size(48.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (resourceId != 0) {
                                            AsyncImage(
                                                model = resourceId,
                                                contentDescription = vendor.name,
                                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                imageVector = when(vendor.category) {
                                                    "Food", "Food Truck" -> Icons.Default.Fastfood
                                                    "Brewery" -> Icons.Default.LocalBar
                                                    "Spirits" -> Icons.Default.WineBar
                                                    else -> Icons.Default.ShoppingCart
                                                },
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { onToggleFavorite(vendor.name) }) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun DetailScreen(type: String, id: String, item: Any?) {
    val context = LocalContext.current
    
    val description = when (item) {
        is VendorItem -> item.description
        is SponsorItem -> item.description
        else -> "Detailed information for $id"
    }

    val email = if (item is VendorItem) item.email else null
    val phone = if (item is VendorItem) item.phone else null
    val website = if (item is VendorItem) item.website else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            AsyncImage(
                model = "https://images.unsplash.com/photo-1532634896-26909d0d4b89?q=80&w=1000",
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(32.dp)),
                contentScale = ContentScale.Crop
            )
            
            Surface(
                modifier = Modifier.padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 8.dp
            ) {
                Text(
                    text = type.uppercase(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = id, 
            style = MaterialTheme.typography.headlineMedium, 
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "About", 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                LinkifyText(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
        
        if (email != null || phone != null || website != null) {
            val noWebsiteNames = setOf("lewis horticultural", "askren balloon team", "kara goheen friends", "rh seals")
            val isNoWebsite = noWebsiteNames.any { id.lowercase().contains(it) }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "Contact Information", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    email?.let { 
                        DetailContactRow(
                            icon = Icons.Default.Email, 
                            value = it,
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:$it")
                                }
                                context.startActivity(intent)
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    phone?.let { 
                        DetailContactRow(
                            icon = Icons.Default.Phone, 
                            value = it,
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:$it")
                                }
                                context.startActivity(intent)
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    website?.let { 
                        DetailContactRow(
                            icon = Icons.Default.Language, 
                            value = it,
                            onClick = {
                                if (isNoWebsite) {
                                    Toast.makeText(context, "A website does not exist.", Toast.LENGTH_SHORT).show()
                                } else {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                                    context.startActivity(intent)
                                }
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun DetailContactRow(icon: ImageVector, value: String, onClick: () -> Unit = {}) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntertainmentScreen(schedule: List<ScheduleItem>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Ground Entertainment",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                ListItem(
                    headlineContent = { Text("DJ Ron Perry", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Live Music DJ") },
                    overlineContent = { Text("MUSIC", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall) },
                    leadingContent = { 
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp)) }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ListItem(
                    headlineContent = { Text("Jennifer Kauffman", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("National Anthem Singer") },
                    overlineContent = { Text("ANTHEM", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall) },
                    leadingContent = { 
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp)) }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ListItem(
                    headlineContent = { Text("Steel Drum Dave", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Check In Entertainment") },
                    overlineContent = { Text("CHECK IN", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall) },
                    leadingContent = {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp)) }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }

        Text(
            text = "Airshow Pilots / Performers",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                val performers = listOf(
                    "Wild Bill" to "Steven Hanshew (Announcer)",
                    "Team Fastrax" to "Nicole Condrey Flag Jump",
                    "Brett Hunter" to "Aerobatic Performance",
                    "Nick Coleman" to "Aerobatic Performance",
                    "Bob Richards" to "Aerobatic Performance",
                    "Smoke on Aviation Team" to "8 Pilot Formation Team",
                    "Mike Hartman" to "Aerobatic Performance",
                    "Emerson Stewart III" to "Aerobatic Performance",
                    "Rob LaCerda" to "Aerobatic Performance"
                )

                performers.forEachIndexed { index, (name, role) ->
                    ListItem(
                        headlineContent = { Text(name, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text(role) },
                        overlineContent = { 
                            Text(
                                if (role.contains("Announcer")) "ANNOUNCER" else "PERFORMANCE", 
                                color = MaterialTheme.colorScheme.secondary, 
                                style = MaterialTheme.typography.labelSmall
                            ) 
                        },
                        leadingContent = { 
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                                Box(contentAlignment = Alignment.Center) { 
                                    Icon(
                                        if (role.contains("Announcer")) Icons.Default.Mic else Icons.Default.AirplanemodeActive, 
                                        contentDescription = null, 
                                        tint = MaterialTheme.colorScheme.secondary, 
                                        modifier = Modifier.size(20.dp)
                                    ) 
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    if (index < performers.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    }
                }
            }
        }

        Text(
            text = "Event Schedule",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                schedule.forEachIndexed { index, item ->
                    ListItem(
                        headlineContent = { Text(item.event, fontWeight = FontWeight.Bold) },
                        supportingContent = { Text(item.time, color = MaterialTheme.colorScheme.primary) },
                        leadingContent = { 
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), modifier = Modifier.size(40.dp)) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    if (index < schedule.size - 1) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}


@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    HopsInTheHangarTheme {
        MainScreen(analytics = null)
    }
}
