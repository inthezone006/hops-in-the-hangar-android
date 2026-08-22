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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

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
    object Map : Screen("map", "Map", Icons.Default.Map)
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
        Screen.Map,
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
            val title = when {
                currentRoute == Screen.Map.route -> "EVENT MAP"
                else -> bottomNavItems.find { it.route == currentRoute }?.label?.uppercase() ?: "HOPS IN THE HANGAR"
            }
            
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
            if (currentRoute != Screen.Map.route) {
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
            composable(Screen.Map.route) { 
                MapScreen(
                    eventData = eventData, 
                    favoriteIds = favoriteIds,
                    onVendorClick = { id ->
                        analytics?.logEvent("map_vendor_click") {
                            param("vendor_id", id)
                        }
                        navController.navigate("detail/vendor/$id")
                    },
                    onSponsorClick = { id ->
                        navController.navigate("detail/sponsor/$id")
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

    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedSponsor by remember { mutableStateOf<SponsorItem?>(null) }
    val sheetState = rememberModalBottomSheetState()

    if (showBottomSheet && selectedSponsor != null) {
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
                    text = selectedSponsor!!.name,
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

                selectedSponsor!!.links?.forEach { link ->
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

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(filteredSponsors) { sponsor ->
                val isFavorite = favoriteIds.contains(sponsor.name)
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val links = sponsor.links
                            if (links != null && links.size > 1) {
                                selectedSponsor = sponsor
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
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    ListItem(
                        headlineContent = { Text(sponsor.name, fontWeight = FontWeight.ExtraBold) },
                        supportingContent = { Text(sponsor.description, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
                        overlineContent = { 
                            Text(
                                sponsor.level.uppercase(), 
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
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
                                    .width(if (names.size > 1) 72.dp else 48.dp)
                                    .height(48.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                names.forEachIndexed { index, name ->
                                    val resourceName = name.lowercase()
                                        .replace("&", " and ")
                                        .replace(" ", "_")
                                        .replace(Regex("[^a-z0-9_]"), "")
                                        .replace(Regex("__+"), "_")
                                        .trim('_')

                                    val context = LocalContext.current
                                    val resourceId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
                                    
                                    Surface(
                                        modifier = Modifier
                                            .padding(start = (index * 24).dp)
                                            .size(48.dp),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant)
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
                                                    tint = when(sponsor.level) {
                                                        "Top Flight" -> MaterialTheme.colorScheme.primary
                                                        "First Class" -> Color(0xFFFFD700) // Gold
                                                        "Business Class" -> Color(0xFFC0C0C0) // Silver
                                                        "Coach Class" -> Color(0xFFCD7F32) // Bronze
                                                        "Brewery" -> MaterialTheme.colorScheme.secondary
                                                        else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        trailingContent = {
                            IconButton(onClick = { onToggleFavorite(sponsor.name) }) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
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
            
            var showFilterMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showFilterMenu = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter")
                }
                DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                    listOf("Brewery", "Food Truck").forEach { category ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = selectedCategories.contains(category),
                                        onCheckedChange = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(category)
                                }
                            },
                            onClick = {
                                selectedCategories = if (selectedCategories.contains(category)) {
                                    selectedCategories - category
                                } else {
                                    selectedCategories + category
                                }
                            }
                        )
                    }
                }
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
                            val resourceName = vendor.name.lowercase()
                                .replace("&", " and ")
                                .replace(" ", "_")
                                .replace(Regex("[^a-z0-9_]"), "")
                                .replace(Regex("__+"), "_")
                                .trim('_')
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
                    headlineContent = { Text("Jane Doe", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Entertainment Host") },
                    overlineContent = { Text("HOST", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall) },
                    leadingContent = { 
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ListItem(
                    headlineContent = { Text("DJ Mixmaster", fontWeight = FontWeight.Bold) },
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
                    headlineContent = { Text("John Smith", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("National Anthem Singer") },
                    overlineContent = { Text("ANTHEM", color = Color.Red, style = MaterialTheme.typography.labelSmall) },
                    leadingContent = { 
                        Surface(shape = CircleShape, color = Color.Red.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp)) }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }

        Text(
            text = "In Flight Performers",
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
                    headlineContent = { Text("Wild Bill", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Steven Hanshew") },
                    overlineContent = { Text("ANNOUNCER", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall) },
                    leadingContent = { 
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ListItem(
                    headlineContent = { Text("Team Fastrax", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Opening Jump") },
                    overlineContent = { Text("PERFORMANCE", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall) },
                    leadingContent = { 
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.AirplanemodeActive, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp)) }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    eventData: EventData?, 
    favoriteIds: Set<String>, 
    onVendorClick: (String) -> Unit,
    onSponsorClick: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("OUTSIDE\nHANGAR", "INSIDE\nHANGAR", "GETTING TO\nEVENT")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { 
                        Text(
                            text = title, 
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                lineHeight = 16.sp
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        ) 
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .clipToBounds() // Ensure map stays within these bounds
        ) {
            when (selectedTabIndex) {
                0 -> EventMapContent(eventData, favoriteIds, onVendorClick, onSponsorClick, showInside = false)
                1 -> EventMapContent(eventData, favoriteIds, onVendorClick, onSponsorClick, showInside = true)
                2 -> DirectionsMap()
            }
        }
    }
}

@Composable
fun EventMapContent(
    eventData: EventData?, 
    favoriteIds: Set<String>, 
    onVendorClick: (String) -> Unit,
    onSponsorClick: (String) -> Unit,
    showInside: Boolean
) {
    val context = LocalContext.current
    var regions by remember { mutableStateOf<List<MapRegion>>(emptyList()) }
    var selectedRegionId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }

    val filteredRegions = remember(regions, showInside) {
        regions.filter { region ->
            val isBeerBooth = region.id.contains("Beer Booth", ignoreCase = true) || 
                             eventData?.vendors?.any { it.mapId == region.id || region.id.contains(it.name, ignoreCase = true) || it.name.contains(region.id, ignoreCase = true) } == true
            
            // Specific items that only exist inside the hangar
            val isHangarOnly = listOf("War Birds", "Pretzel", "Hangar", "Inside")
                .any { region.id.contains(it, ignoreCase = true) }
            
            // Items that can exist in both or are general
            val isGeneralTableOrStation = (region.id.contains("Table", ignoreCase = true) || 
                                         region.id.contains("Station", ignoreCase = true))

            if (showInside) {
                // Inside: Show Beer Booths, Hangar-only items, and general items (Tables/Stations) if they are in the SVG
                isBeerBooth || isHangarOnly || isGeneralTableOrStation || !region.isClickable
            } else {
                // Outside: Show interactive elements except Beer Booths and Hangar-only items, plus general items
                ((!isBeerBooth && !isHangarOnly) && region.isClickable) || isGeneralTableOrStation || !region.isClickable
            }
        }
    }

    LaunchedEffect(showInside) {
        selectedRegionId = null
    }

    val selectedVendor = remember(selectedRegionId, eventData) {
        eventData?.vendors?.find { it.mapId == selectedRegionId || it.name == selectedRegionId || (selectedRegionId != null && (it.name.contains(selectedRegionId!!, ignoreCase = true) || selectedRegionId!!.contains(it.name, ignoreCase = true))) }
    }

    val selectedSponsor = remember(selectedRegionId, eventData, selectedVendor) {
        if (selectedVendor == null) {
            eventData?.sponsors?.find { it.mapId == selectedRegionId || it.name == selectedRegionId || (selectedRegionId != null && (it.name.contains(selectedRegionId!!, ignoreCase = true) || selectedRegionId!!.contains(it.name, ignoreCase = true))) }
        } else null
    }

    // Transformation state
    var zoomScale by remember { mutableFloatStateOf(1.5f) } 
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Natural SVG dimensions
    var svgWidth by remember { mutableFloatStateOf(2000f) }
    var svgHeight by remember { mutableFloatStateOf(2000f) }
    var svgOffsetX by remember { mutableFloatStateOf(0f) }
    var svgOffsetY by remember { mutableFloatStateOf(0f) }

    val heartedMapIds = remember(eventData, favoriteIds) {
        val vendorMapIds = eventData?.vendors?.filter { favoriteIds.contains(it.name) }
            ?.mapNotNull { it.mapId ?: it.name } ?: emptyList()
        val sponsorMapIds = eventData?.sponsors?.filter { favoriteIds.contains(it.name) }
            ?.mapNotNull { it.mapId ?: it.name } ?: emptyList()
        (vendorMapIds + sponsorMapIds).toSet()
    }

    LaunchedEffect(showInside) {
        withContext(Dispatchers.IO) {
            try {
                val fileName = if (showInside) "Inside.svg" else "Outside.svg"
                val inputStream = context.assets.open(fileName)
                val (parsedRegions, viewBox) = parseSvg(inputStream)
                
                svgWidth = viewBox.width()
                svgHeight = viewBox.height()
                svgOffsetX = viewBox.left
                svgOffsetY = viewBox.top
                
                regions = parsedRegions
                isLoading = false
            } catch (e: Exception) {
                Log.e("MapScreen", "Error loading map data", e)
                isLoading = false
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error loading map", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        val title = selectedVendor?.name ?: selectedSponsor?.name ?: selectedRegionId ?: "Tap Map to Select"
        val category = selectedVendor?.category ?: selectedSponsor?.level ?: (if (selectedRegionId != null) "Location Info" else "Select a location on the map")
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            onClick = { 
                selectedVendor?.let { onVendorClick(it.name) }
                selectedSponsor?.let { onSponsorClick(it.name) }
            }
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            selectedVendor != null -> "SELECTED VENDOR"
                            selectedSponsor != null -> "SELECTED SPONSOR"
                            selectedRegionId != null -> "SELECTED AREA"
                            else -> "EVENT MAP"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.background).clipToBounds()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                // Map Content Area
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                ) {
                    val canvasWidth = constraints.maxWidth.toFloat()
                    val canvasHeight = constraints.maxHeight.toFloat()
                    
                    val baseScaleX = canvasWidth / svgWidth
                    val baseScaleY = canvasHeight / svgHeight
                    val baseScale = maxOf(baseScaleX, baseScaleY)
                    
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val favoriteColor = Color(0xFFFF4081)
                    val sTotal = baseScale * zoomScale

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .pointerInput(filteredRegions, baseScale, svgOffsetX, svgOffsetY) {
                                detectTapGestures { offset ->
                                    val sTotal = baseScale * zoomScale
                                    val canvasWidth = size.width.toFloat()
                                    val canvasHeight = size.height.toFloat()
                                    
                                    val svgX = (offset.x - canvasWidth/2f - panOffset.x) / sTotal + svgWidth/2f + svgOffsetX
                                    val svgY = (offset.y - canvasHeight/2f - panOffset.y) / sTotal + svgHeight/2f + svgOffsetY
                                    
                                    val clickedRegion = filteredRegions.findLast { region ->
                                        val matchesVendorOrSponsor = eventData?.vendors?.any { it.mapId == region.id || it.name == region.id || (region.id != null && it.name.contains(region.id, ignoreCase = true)) } == true ||
                                                                    eventData?.sponsors?.any { it.mapId == region.id || it.name == region.id || (region.id != null && it.name.contains(region.id, ignoreCase = true)) } == true
                                        (region.isClickable || matchesVendorOrSponsor || region.id.contains("Table") || region.id.contains("Station")) && 
                                        hitTest(region.path, svgX, svgY)
                                    }
                                    
                                    selectedRegionId = clickedRegion?.id
                                }
                            }
                            .pointerInput(filteredRegions, baseScale) {
                                detectTransformGestures { centroid, pan, zoom, _ ->
                                    val oldScale = zoomScale
                                    val newScale = (oldScale * zoom).coerceIn(1f, 25f)
                                    val scaleFactor = newScale / oldScale
                                    
                                    val canvasWidth = size.width.toFloat()
                                    val canvasHeight = size.height.toFloat()

                                    panOffset = (panOffset * scaleFactor) + 
                                               (centroid - Offset(canvasWidth/2f, canvasHeight/2f)) * (1f - scaleFactor) + 
                                               pan
                                    zoomScale = newScale
                                }
                            }
                    ) {
                        drawIntoCanvas { canvas ->
                            canvas.save()
                            
                            val tx = canvasWidth/2f - (svgWidth/2f + svgOffsetX) * sTotal + panOffset.x
                            val ty = canvasHeight/2f - (svgHeight/2f + svgOffsetY) * sTotal + panOffset.y
                            
                            canvas.translate(tx, ty)
                            canvas.scale(sTotal, sTotal)
                            
                            filteredRegions.forEach { region ->
                                val isSelected = region.id == selectedRegionId
                                val isHearted = heartedMapIds.contains(region.id)
                                
                                val staticAlpha = 1f

                                if (staticAlpha > 0f) {
                                    if (region.image != null) {
                                        val bounds = region.path.getBounds()
                                        clipPath(region.path) {
                                            drawImage(
                                                image = region.image,
                                                dstOffset = IntOffset(bounds.left.toInt(), bounds.top.toInt()),
                                                dstSize = IntSize(bounds.width.toInt(), bounds.height.toInt()),
                                                alpha = staticAlpha
                                            )
                                        }
                                        if (isHearted || isSelected) {
                                            drawPath(
                                                path = region.path,
                                                color = if (isSelected) primaryColor else favoriteColor,
                                                style = Stroke(width = 10f)
                                            )
                                        }
                                    } else {
                                        drawPath(
                                            path = region.path,
                                            color = when {
                                                isSelected -> primaryColor
                                                isHearted -> favoriteColor
                                                else -> region.color
                                            },
                                            alpha = staticAlpha,
                                            style = Fill
                                        )
                                    }
                                    
                                }
                            }
                            canvas.restore()
                        }
                    }

                    // Map Overlays (Icons and Labels)
                    filteredRegions.forEach { region ->
                        if (!region.isClickable) return@forEach
                        val vendor = eventData?.vendors?.find { it.mapId == region.id || it.name == region.id || it.name.contains(region.id, ignoreCase = true) || region.id.contains(it.name, ignoreCase = true) }
                        val isHearted = heartedMapIds.contains(region.id)
                        val isSelected = region.id == selectedRegionId
                        
                        val isTableOrWater = region.id.contains("Table", ignoreCase = true) || region.id.contains("Water", ignoreCase = true)
                        // Decide what to show based on zoom and heart status
                        val shouldShowDetail = zoomScale > 1.5f || isHearted || isSelected || isTableOrWater
                        if (!shouldShowDetail) return@forEach

                        val screenX = (region.center.x - (svgWidth/2f + svgOffsetX)) * sTotal + canvasWidth/2f + panOffset.x
                        val screenY = (region.center.y - (svgHeight/2f + svgOffsetY)) * sTotal + canvasHeight/2f + panOffset.y
                        
                        // Culling
                        if (screenX < -50f || screenX > canvasWidth + 50f || screenY < -50f || screenY > canvasHeight + 50f) return@forEach
                        
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    translationX = screenX - 50.dp.toPx()
                                    translationY = screenY - 24.dp.toPx()
                                }
                                .width(100.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    selectedRegionId = region.id
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val resourceName = vendor?.name?.lowercase()
                                ?.replace("&", " and ")
                                ?.replace(" ", "_")
                                ?.replace(Regex("[^a-z0-9_]"), "")
                                ?.replace(Regex("__+"), "_")
                                ?.trim('_')
                            val resourceId = if (resourceName != null) context.resources.getIdentifier(resourceName, "drawable", context.packageName) else 0

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    modifier = Modifier.size(if (zoomScale > 4f || isHearted || isSelected) 40.dp else 24.dp),
                                    shape = CircleShape,
                                    color = when {
                                        isHearted -> favoriteColor
                                        isSelected -> primaryColor
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                    border = BorderStroke(2.dp, if (isHearted || isSelected) Color.White else MaterialTheme.colorScheme.primary),
                                    tonalElevation = 4.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        // Priority 1: High Zoom or Hearted/Selected -> Show Logo
                                        if ((zoomScale > 5.5f || isHearted || isSelected) && resourceId != 0) {
                                            AsyncImage(
                                                model = resourceId,
                                                contentDescription = vendor?.name,
                                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } 
                                        // Priority 2: Beer Booths -> Show Booth Number (if digit exists)
                                        else if (region.id.contains("Beer Booth", ignoreCase = true)) {
                                            val boothNum = region.id.filter { it.isDigit() }
                                            if (boothNum.isNotEmpty() && (zoomScale > 3.5f || isHearted || isSelected)) {
                                                Text(
                                                    text = boothNum,
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isHearted || isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                    )
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.LocalDrink,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(if (zoomScale > 3.5f) 20.dp else 12.dp),
                                                    tint = if (isHearted || isSelected) Color.White else MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        // Priority 3: All other interactive elements -> Show Relevant Icon
                                        else {
                                            val icon = when {
                                                region.id.contains("Sponsor", ignoreCase = true) -> Icons.Default.Campaign
                                                region.id.contains("Plane", ignoreCase = true) || region.id.contains("War Birds", ignoreCase = true) -> Icons.Default.AirplanemodeActive
                                                region.id.contains("Pilot", ignoreCase = true) -> Icons.Default.Person
                                                vendor?.category?.contains("Brewery", ignoreCase = true) == true ||
                                                    listOf("Brewery", "Brewing", "Brew", "Beer", "Ale", "Cider", "Meade", "Wing")
                                                        .any { region.id.contains(it, ignoreCase = true) } -> Icons.Default.LocalBar
                                                vendor?.category?.contains("Food", ignoreCase = true) == true || 
                                                    listOf("Food", "Grill", "Truck", "Wagon", "Pizza", "Italian", "Mac", "Station", "Medy", "Pretzel")
                                                        .any { region.id.contains(it, ignoreCase = true) } -> Icons.Default.Restaurant
                                                region.id.contains("Entrance", ignoreCase = true) -> Icons.Default.MeetingRoom
                                                region.id.contains("Bathroom", ignoreCase = true) || region.id.contains("Restroom", ignoreCase = true) || region.id.contains("Toilet", ignoreCase = true) -> Icons.Default.Wc
                                                region.id.contains("Stage", ignoreCase = true) || region.id.contains("Music", ignoreCase = true) || region.id.contains("DJ", ignoreCase = true) -> Icons.Default.MusicNote
                                                region.id.contains("VIP", ignoreCase = true) -> Icons.Default.Star
                                                region.id.contains("Information", ignoreCase = true) || region.id.contains("Info", ignoreCase = true) -> Icons.Default.Info
                                                region.id.contains("Balloon", ignoreCase = true) -> Icons.Default.Cloud
                                                region.id.contains("Table", ignoreCase = true) -> Icons.Default.TableBar
                                                region.id.contains("Cornhole", ignoreCase = true) -> Icons.Default.SportsScore
                                                region.id.contains("Water", ignoreCase = true) -> Icons.Default.WaterDrop
                                                region.id.contains("Photo", ignoreCase = true) -> Icons.Default.CameraAlt
                                                else -> Icons.Default.Place
                                            }
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(if (zoomScale > 3.5f) 20.dp else 12.dp),
                                                tint = if (isHearted || isSelected) Color.White else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                if (zoomScale > 1.5f) {
                                    Surface(
                                        modifier = Modifier.padding(top = 4.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isSelected) primaryColor.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.7f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                                    ) {
                                        Text(
                                            text = vendor?.name ?: region.id,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 9.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            
                            // Heart badge for favorites
                            if (isHearted) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(start = 28.dp) // Offset from center icon
                                        .size(16.dp),
                                    shape = CircleShape,
                                    color = favoriteColor,
                                    border = BorderStroke(1.dp, Color.White)
                                ) {
                                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.padding(2.dp))
                                }
                            }
                        }
                    }

                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
fun DirectionsMap() {
    val context = LocalContext.current
    
    // Initialize osmdroid configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permissions if needed
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(16.5)
                
                val startPoint = GeoPoint(39.5219738270208, -84.39756916125495)
                val endPoint = GeoPoint(39.52926176496046, -84.39276198740617)
                controller.setCenter(startPoint)

                // Add Path Polyline
                val path = Polyline().apply {
                    outlinePaint.color = android.graphics.Color.RED
                    outlinePaint.strokeWidth = 12f
                    val points = listOf(
                        GeoPoint(39.5219738270208, -84.39756916125495),
                        GeoPoint(39.52392228883309, -84.39758570695065),
                        GeoPoint(39.523858475447454, -84.39595871354263),
                        GeoPoint(39.527066399999974, -84.39509747693043),
                        GeoPoint(39.527587748793955, -84.39398167808926),
                        GeoPoint(39.52815437416716, -84.39416932546087),
                        GeoPoint(39.528705851297936, -84.39335856931429),
                        GeoPoint(39.52926176496046, -84.39276198740617)
                    )
                    setPoints(points)
                }
                overlays.add(path)

                // Entrance Marker
                val entranceMarker = Marker(this).apply {
                    position = startPoint
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "500 Tytus Ave Entrance"
                    snippet = "Start here to enter Smith Park"
                }
                overlays.add(entranceMarker)

                // Destination Marker
                val destinationMarker = Marker(this).apply {
                    position = endPoint
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "Start Skydiving / Event Gate"
                    snippet = "1711 Run Way, Middletown, OH 45042"
                }
                overlays.add(destinationMarker)

                // My Location Overlay
                val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this).apply {
                    enableMyLocation()
                }
                overlays.add(locationOverlay)
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds() // Double-check clipping at the View level
    )
}

data class MapRegion(
    val id: String,
    val path: Path,
    val color: Color,
    val isClickable: Boolean = true,
    val center: Offset = Offset.Zero,
    val image: ImageBitmap? = null
)

fun parseSvg(inputStream: java.io.InputStream): Pair<List<MapRegion>, android.graphics.RectF> {
    val svgBytes = inputStream.readBytes()
    inputStream.close()

    val imageMap = mutableMapOf<String, ImageBitmap>()
    val patternMap = mutableMapOf<String, String>()
    
    val factory = XmlPullParserFactory.newInstance()
    factory.isNamespaceAware = true
    val xlinkNamespace = "http://www.w3.org/1999/xlink"

    // Pass 1: Extract Images and Patterns
    var parser = factory.newPullParser()
    parser.setInput(svgBytes.inputStream(), null)
    var eventType = parser.eventType
    var currentPatternId: String? = null

    while (eventType != XmlPullParser.END_DOCUMENT) {
        val tagName = parser.name
        if (eventType == XmlPullParser.START_TAG) {
            when (tagName) {
                "image" -> {
                    val rawId = parser.getAttributeValue(null, "id")
                    val href = parser.getAttributeValue(xlinkNamespace, "href") ?: parser.getAttributeValue(null, "href")
                    if (href?.startsWith("data:image/png;base64,") == true) {
                        val base64Data = href.substringAfter("base64,")
                        try {
                            val decodedString = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                            val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                            if (bitmap != null) {
                                imageMap[rawId ?: ""] = bitmap.asImageBitmap()
                            }
                        } catch (e: Exception) {
                            Log.e("SVG", "Error decoding image $rawId", e)
                        }
                    }
                }
                "pattern" -> {
                    currentPatternId = parser.getAttributeValue(null, "id")
                }
                "use" -> {
                    if (currentPatternId != null) {
                        val href = parser.getAttributeValue(xlinkNamespace, "href") ?: parser.getAttributeValue(null, "href")
                        if (href?.startsWith("#") == true) {
                            patternMap[currentPatternId!!] = href.substring(1)
                        }
                    }
                }
            }
        } else if (eventType == XmlPullParser.END_TAG) {
            if (tagName == "pattern") currentPatternId = null
        }
        eventType = parser.next()
    }

    // Pass 2: Extract Regions
    val regions = mutableListOf<MapRegion>()
    parser = factory.newPullParser()
    parser.setInput(svgBytes.inputStream(), null)
    eventType = parser.eventType
    
    val groupIds = mutableListOf<String>()
    val viewBox = android.graphics.RectF(0f, 0f, 2000f, 2000f)
    
    val backgroundIds = setOf("Event Map Base", "Full Event Map", "Background", "HANGAR AREA", "OUTSIDE AREA", "ENTRANCE", "Frame 1", "Inside", "Outside", "Rectangle 1")
    val interactiveKeywords = listOf("Beer Booth", "Sponsor Tent", "Plane", "Pilot Tent", "Food Truck", "Entrance", "Grill", "Truck", "Wagon", "Pizza", "Italian", "Mac", "Station", "War Birds", "Balloon", "Table", "Cornhole", "Medy", "Booth", "Bathroom", "VIP", "Skydiving", "Brewery", "Brewing", "Brew", "Beer", "Ale", "Cider", "Meade", "Spirits", "Winery", "Wine", "Wing")

    val hangarColor = Color(0xFF112240)
    val runwayColor = Color(0xFF1C1C1C)
    val grassColor = Color(0xFF1A2E1F)
    val taxiwayColor = Color(0xFF2D2D2D)

    while (eventType != XmlPullParser.END_DOCUMENT) {
        val tagName = parser.name
        when (eventType) {
            XmlPullParser.START_TAG -> {
                val rawId = parser.getAttributeValue(null, "id")
                val id = rawId
                val transform = parser.getAttributeValue(null, "transform")
                
                if (tagName == "svg") {
                    val vb = parser.getAttributeValue(null, "viewBox")
                    if (vb != null) {
                        val parts = vb.split(Regex("[ ,]+")).filter { it.isNotEmpty() }
                        if (parts.size == 4) {
                            val x = parts[0].toFloat()
                            val y = parts[1].toFloat()
                            val w = parts[2].toFloat()
                            val h = parts[3].toFloat()
                            viewBox.set(x, y, x + w, y + h)
                        }
                    }
                } else if (tagName == "g") {
                    groupIds.add(id ?: "")
                } else if (tagName != "image" && tagName != "pattern" && tagName != "use" && tagName != "defs") {
                    val fillAttr = parser.getAttributeValue(null, "fill")
                    val styleAttr = parser.getAttributeValue(null, "style") ?: ""
                    
                    val fillFromStyle = if (styleAttr.contains("fill:")) {
                        styleAttr.substringAfter("fill:").substringBefore(";").trim()
                    } else null
                    
                    val fill = fillAttr ?: fillFromStyle ?: "#000000"
                    val fillOpacity = parser.getAttributeValue(null, "fill-opacity")?.toFloatOrNull() ?: 1f
                    
                    val finalId = (id ?: groupIds.lastOrNull { it.isNotEmpty() } ?: "untagged_${tagName}_${System.currentTimeMillis()}").let {
                        var cleaned = it.replace("_", " ")
                        if (cleaned == "Beer Booth") cleaned = "Beer Booth 1"
                        cleaned
                    }
                    
                    val parsedColor = try {
                        if (fill.startsWith("#")) {
                            val baseColor = android.graphics.Color.parseColor(fill)
                            Color(baseColor).copy(alpha = fillOpacity)
                        } else if (!fill.startsWith("url(#")) {
                            // Try parsing named colors like "white", "black", etc.
                            val baseColor = android.graphics.Color.parseColor(fill)
                            Color(baseColor).copy(alpha = fillOpacity)
                        } else null
                    } catch (_: Exception) { null }

                    val color = when {
                        fill.startsWith("url(#") -> Color.Transparent
                        parsedColor != null -> parsedColor
                        finalId.contains("Runway", ignoreCase = true) || finalId.contains("Outside", ignoreCase = true) -> runwayColor
                        finalId.contains("Taxiway", ignoreCase = true) || finalId.contains("Concrete", ignoreCase = true) -> taxiwayColor
                        finalId.contains("Grass", ignoreCase = true) -> grassColor
                        finalId.contains("Hangar", ignoreCase = true) -> hangarColor
                        else -> Color.Gray
                    }

                    val shouldSkip = (finalId.contains("Event Map Base") || finalId.contains("Background")) && tagName == "rect"

                    if (!shouldSkip) {
                        val androidPath = android.graphics.Path()
                        var pathFound = false

                        when (tagName) {
                            "path" -> {
                                val d = parser.getAttributeValue(null, "d")
                                if (d != null) {
                                    try {
                                        val p = PathParser().parsePathString(d).toPath().asAndroidPath()
                                        androidPath.set(p)
                                        pathFound = true
                                    } catch (_: Exception) { }
                                }
                            }
                            "rect" -> {
                                val x = parser.getAttributeValue(null, "x")?.toFloat() ?: 0f
                                val y = parser.getAttributeValue(null, "y")?.toFloat() ?: 0f
                                val width = parser.getAttributeValue(null, "width")?.toFloat() ?: 0f
                                val height = parser.getAttributeValue(null, "height")?.toFloat() ?: 0f
                                androidPath.addRect(x, y, x + width, y + height, android.graphics.Path.Direction.CW)
                                pathFound = true
                            }
                            "ellipse", "circle" -> {
                                val cx = parser.getAttributeValue(null, "cx")?.toFloat() ?: 0f
                                val cy = parser.getAttributeValue(null, "cy")?.toFloat() ?: 0f
                                val rx = if (tagName == "circle") parser.getAttributeValue(null, "r")?.toFloat() ?: 0f else parser.getAttributeValue(null, "rx")?.toFloat() ?: 0f
                                val ry = if (tagName == "circle") rx else parser.getAttributeValue(null, "ry")?.toFloat() ?: 0f
                                androidPath.addOval(cx - rx, cy - ry, cx + rx, cy + ry, android.graphics.Path.Direction.CW)
                                pathFound = true
                            }
                        }

                        if (pathFound) {
                            applySvgTransform(androidPath, transform)
                            val fillUrl = if (fill.startsWith("url(#")) fill.substring(5, fill.length - 1) else null
                            
                            var regionImage: ImageBitmap? = null
                            if (fillUrl != null) {
                                val imageId = patternMap[fillUrl] ?: fillUrl
                                regionImage = imageMap[imageId]
                            }

                            val isInteractive = interactiveKeywords.any { finalId.contains(it, ignoreCase = true) } && !backgroundIds.contains(finalId)
                            
                            val combinedPath = androidPath.asComposePath()
                            val bounds = combinedPath.getBounds()
                            val center = Offset(bounds.left + bounds.width / 2f, bounds.top + bounds.height / 2f)

                            regions.add(MapRegion(
                                id = finalId,
                                path = combinedPath,
                                color = color,
                                isClickable = isInteractive,
                                center = center,
                                image = regionImage
                            ))
                        }
                    }
                }
            }
            XmlPullParser.END_TAG -> {
                if (tagName == "g" && groupIds.isNotEmpty()) {
                    groupIds.removeAt(groupIds.size - 1)
                }
            }
        }
        eventType = parser.next()
    }

    return regions to viewBox
}


private fun applySvgTransform(path: android.graphics.Path, transform: String?) {
    if (transform == null) return
    val matrix = android.graphics.Matrix()
    
    if (transform.contains("rotate")) {
        val content = transform.substringAfter("rotate(").substringBefore(")")
        val values = content.split(Regex("[ ,]+")).filter { it.isNotEmpty() }
        try {
            when (values.size) {
                1 -> matrix.postRotate(values[0].toFloat())
                3 -> matrix.postRotate(values[0].toFloat(), values[1].toFloat(), values[2].toFloat())
            }
        } catch (_: Exception) {}
    }
    
    if (transform.contains("translate")) {
        val content = transform.substringAfter("translate(").substringBefore(")")
        val values = content.split(Regex("[ ,]+")).filter { it.isNotEmpty() }
        try {
            when (values.size) {
                1 -> matrix.postTranslate(values[0].toFloat(), 0f)
                2 -> matrix.postTranslate(values[0].toFloat(), values[1].toFloat())
            }
        } catch (_: Exception) {}
    }
    
    path.transform(matrix)
}

fun hitTest(path: Path, x: Float, y: Float): Boolean {
    val androidPath = path.asAndroidPath()
    val bounds = android.graphics.RectF()
    androidPath.computeBounds(bounds, true)
    
    val tolerance = 5f
    val region = android.graphics.Region()
    region.setPath(androidPath, android.graphics.Region(
        (bounds.left - tolerance).toInt(),
        (bounds.top - tolerance).toInt(),
        (bounds.right + tolerance).toInt(),
        (bounds.bottom + tolerance).toInt()
    ))
    
    if (region.contains(x.toInt(), y.toInt())) return true
    return bounds.contains(x, y)
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    HopsInTheHangarTheme {
        MainScreen(analytics = null)
    }
}
