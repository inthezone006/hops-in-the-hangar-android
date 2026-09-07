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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlin.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.rahul.hopsinthehangar.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.text.BasicTextField
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.delay

fun parseHexColor(hex: String?, defaultColor: Color = NeoWhite): Color {
    if (hex.isNullOrBlank()) return defaultColor
    return try {
        val formattedHex = if (hex.startsWith("#")) hex else "#$hex"
        Color(formattedHex.toColorInt())
    } catch (e: Exception) {
        defaultColor
    }
}

fun getResourceName(name: String?): String {
    if (name == null) return ""
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

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
}

// Pure Neo-Brutalist Custom Components with Tactile Press Effects
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    containerColor: Color = Color.White,
    contentColor: Color = Color.Black,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val shadowOffsetX = if (isPressed && onClick != null) 2.dp else 6.dp
    val shadowOffsetY = if (isPressed && onClick != null) 2.dp else 6.dp
    val translationX = if (isPressed && onClick != null) 4.dp else 0.dp
    val translationY = if (isPressed && onClick != null) 4.dp else 0.dp

    Box(modifier = modifier.offset(x = translationX, y = translationY)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffsetX, y = shadowOffsetY)
                .background(Color.Black, shape = RoundedCornerShape(8.dp))
        )

        Surface(
            modifier = if (onClick != null) {
                Modifier.fillMaxWidth().clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
            } else {
                Modifier.fillMaxWidth()
            },
            shape = RoundedCornerShape(8.dp),
            color = containerColor,
            contentColor = contentColor,
            border = BorderStroke(3.dp, Color.Black)
        ) {
            Column(modifier = Modifier.padding(20.dp), content = content)
        }
    }
}

@Composable
fun NeoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = NeoYellow,
    contentColor: Color = Color.Black,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val shadowOffsetX = if (isPressed) 1.dp else 4.dp
    val shadowOffsetY = if (isPressed) 1.dp else 4.dp
    val translationX = if (isPressed) 3.dp else 0.dp
    val translationY = if (isPressed) 3.dp else 0.dp

    Box(modifier = modifier.offset(x = translationX, y = translationY)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffsetX, y = shadowOffsetY)
                .background(Color.Black, shape = RoundedCornerShape(8.dp))
        )
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
            shape = RoundedCornerShape(8.dp),
            color = containerColor,
            contentColor = contentColor,
            border = BorderStroke(3.dp, Color.Black)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

@Composable
fun NeoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 4.dp, y = 4.dp)
                .background(Color.Black, shape = RoundedCornerShape(8.dp))
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color.White,
            border = BorderStroke(3.dp, Color.Black)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty() && placeholder != null) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            placeholder()
                        }
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = singleLine,
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Start
                        ),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                }
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    trailingIcon()
                }
            }
        }
    }
}

@Composable
fun NeoListItem(
    headline: @Composable () -> Unit,
    supporting: @Composable (() -> Unit)? = null,
    overline: @Composable (() -> Unit)? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = if (onClick != null) Modifier.fillMaxWidth().clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        ) else Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                leading()
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                if (overline != null) {
                    overline()
                    Spacer(modifier = Modifier.height(2.dp))
                }
                headline()
                if (supporting != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    supporting()
                }
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(16.dp))
                trailing()
            }
        }
    }
}

@Composable
fun NeoCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(if (checked) NeoYellow else Color.White, shape = RoundedCornerShape(4.dp))
            .border(BorderStroke(2.dp, Color.Black), shape = RoundedCornerShape(4.dp))
            .then(
                if (onCheckedChange != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onCheckedChange(!checked) }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun NeoFilterButton(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = if (checked) NeoCream else Color.White
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val shadowOffsetX = if (isPressed) 1.dp else 4.dp
    val shadowOffsetY = if (isPressed) 1.dp else 4.dp
    val translationX = if (isPressed) 3.dp else 0.dp
    val translationY = if (isPressed) 3.dp else 0.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .offset(x = translationX, y = translationY)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffsetX, y = shadowOffsetY)
                .background(Color.Black, shape = RoundedCornerShape(8.dp))
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { onCheckedChange(!checked) }
                ),
            shape = RoundedCornerShape(8.dp),
            color = containerColor,
            contentColor = Color.Black,
            border = BorderStroke(3.dp, Color.Black)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeoCheckbox(
                    checked = checked
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(analytics: FirebaseAnalytics? = Firebase.analytics) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var eventData by remember { mutableStateOf<EventData?>(null) }

    LaunchedEffect(Unit) {
        eventData = loadEventData(context)
    }

    LaunchedEffect(currentRoute) {
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
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val title = bottomNavItems.find { 
                it.route == currentRoute || 
                (it.route == Screen.Sponsors.route && currentRoute?.contains("sponsor") == true) ||
                (it.route == Screen.Vendors.route && currentRoute?.contains("vendor") == true)
            }?.label?.uppercase() ?: Screen.Home.label.uppercase()

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .layout { measurable, constraints ->
                        val bleedPx = 3.dp.roundToPx()
                        val targetWidth = constraints.maxWidth + (bleedPx * 2)
                        val placeable = measurable.measure(
                            constraints.copy(
                                minWidth = targetWidth,
                                maxWidth = targetWidth
                            )
                        )
                        layout(constraints.maxWidth, placeable.height - bleedPx) {
                            placeable.place(-bleedPx, -bleedPx)
                        }
                    },
                border = BorderStroke(3.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentRoute?.startsWith("detail") == true) {
                        Box(modifier = Modifier.align(Alignment.CenterStart)) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .offset(x = 2.dp, y = 2.dp)
                                    .background(Color.Black, shape = RoundedCornerShape(6.dp))
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { navController.popBackStack() }
                            ) {
                                Box(modifier = Modifier.padding(6.dp), contentAlignment = Alignment.Center) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    // Tickets Button on Top Bar
                    val ticketsInteractionSource = remember { MutableInteractionSource() }
                    val isTicketsPressed by ticketsInteractionSource.collectIsPressedAsState()
                    val ticketsShadowX = if (isTicketsPressed) 1.dp else 2.dp
                    val ticketsShadowY = if (isTicketsPressed) 1.dp else 2.dp
                    val ticketsTransX = if (isTicketsPressed) 2.dp else 0.dp
                    val ticketsTransY = if (isTicketsPressed) 2.dp else 0.dp

                    Box(modifier = Modifier.align(Alignment.CenterEnd).offset(x = ticketsTransX, y = ticketsTransY)) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .offset(x = ticketsShadowX, y = ticketsShadowY)
                                .background(Color.Black, shape = RoundedCornerShape(6.dp))
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NeoPink,
                            border = BorderStroke(2.dp, Color.Black),
                            modifier = Modifier.clickable(
                                interactionSource = ticketsInteractionSource,
                                indication = null
                            ) {
                                scope.launch {
                                    delay(100)
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://middletownaviationfoundation.ticketspice.com/hops-in-the-hangar-2026"))
                                    context.startActivity(intent)
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("TICKETS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp), color = Color.Black)
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .layout { measurable, constraints ->
                        val bleedPx = 3.dp.roundToPx()
                        val targetWidth = constraints.maxWidth + (bleedPx * 2)
                        val placeable = measurable.measure(
                            constraints.copy(
                                minWidth = targetWidth,
                                maxWidth = targetWidth
                            )
                        )
                        layout(constraints.maxWidth, placeable.height - bleedPx) {
                            placeable.place(-bleedPx, 0)
                        }
                    },
                border = BorderStroke(3.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bottomNavItems.forEachIndexed { index, screen ->
                        val selected = currentRoute == screen.route ||
                            (screen.route == Screen.Sponsors.route && currentRoute?.contains("sponsor") == true) ||
                            (screen.route == Screen.Vendors.route && currentRoute?.contains("vendor") == true)
                        val tabColors = listOf(NeoYellow, NeoPink, NeoGreen, NeoBlue)
                        val tabColor = tabColors[index % tabColors.size]

                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()

                        val isPressedOrSelected = selected || isPressed

                        val shadowOffsetX = if (isPressedOrSelected) 1.dp else 3.dp
                        val shadowOffsetY = if (isPressedOrSelected) 1.dp else 3.dp
                        val translationX = if (isPressedOrSelected) 2.dp else 0.dp
                        val translationY = if (isPressedOrSelected) 2.dp else 0.dp

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .offset(x = translationX, y = translationY)
                        ) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .offset(x = shadowOffsetX, y = shadowOffsetY)
                                    .background(Color.Black, shape = RoundedCornerShape(6.dp))
                            )
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null
                                    ) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = false
                                            }
                                            launchSingleTop = true
                                            restoreState = false
                                        }
                                    },
                                shape = RoundedCornerShape(6.dp),
                                color = if (selected) tabColor else NeoWhite,
                                border = BorderStroke(2.dp, Color.Black)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        screen.icon,
                                        contentDescription = screen.label,
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        screen.label.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Color.Black,
                                            fontSize = 9.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
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
                    onSponsorClick = { sponsor ->
                        analytics?.logEvent("sponsor_detail_view") {
                            param("sponsor_id", sponsor.name)
                        }
                    }
                )
            }
            composable(Screen.Entertainment.route) {
                EntertainmentScreen(
                    groundEntertainment = eventData?.groundEntertainment ?: emptyList(),
                    performers = eventData?.performers ?: emptyList(),
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
                    }
                )
            }
        }
    }
}

@Serializable
data class EntertainmentItem(
    val name: String,
    val role: String,
    val category: String? = null,
    val description: String? = null,
    val about: String? = null,
    val socialPlatform: String? = null,
    val socialHandle: String? = null,
    val socialUrl: String? = null,
    val contactInfo: String? = null
)

@Serializable
data class EventData(
    val sponsors: List<SponsorItem>,
    val vendors: List<VendorItem>,
    val schedule: List<ScheduleItem>,
    val info: GeneralInfo,
    val faq: List<FaqItemData> = emptyList(),
    val groundEntertainment: List<EntertainmentItem> = emptyList(),
    val performers: List<EntertainmentItem> = emptyList()
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
    val about: String? = null,
    val website: String? = null,
    val links: List<SponsorLink>? = null,
    val mapId: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val background: String? = null
)

@Serializable
data class VendorItem(
    val name: String,
    val category: String,
    val description: String,
    val about: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val website: String? = null,
    val mapId: String? = null,
    val background: String? = null
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
                color = Color.Blue,
                textDecoration = TextDecoration.Underline,
                fontWeight = FontWeight.Black
            )
        )

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
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onNonLinkClick()
            }
        } else {
            modifier
        },
        style = style.copy(color = color, textAlign = textAlign ?: style.textAlign)
    )
}

// Neo-Brutalist Accordion Component
@Composable
fun NeoAccordion(
    title: String,
    headerColor: Color = NeoYellow,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 6.dp, y = 6.dp)
                .background(Color.Black, shape = RoundedCornerShape(0.dp))
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { expanded = !expanded },
            shape = RoundedCornerShape(0.dp),
            color = headerColor,
            border = BorderStroke(3.dp, Color.Black)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Surface(
                        shape = RoundedCornerShape(0.dp),
                        color = Color.White,
                        border = BorderStroke(2.dp, Color.Black)
                    ) {
                        Text(
                            text = if (expanded) " [-] " else " [+] ",
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Hard-cut accordion body
                if (expanded) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(0.dp),
                        color = Color.White,
                        border = BorderStroke(2.dp, Color.Black)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), content = content)
                    }
                }
            }
        }
    }
}

@Composable
fun FaqSection(faqItems: List<FaqItemData>) {
    Text(
        "FAQ",
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp
    )

    Spacer(modifier = Modifier.height(16.dp))

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        faqItems.forEach { item ->
            NeoAccordion(title = item.question, headerColor = NeoYellow) {
                LinkifyText(
                    text = item.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black.copy(alpha = 0.9f)
                )
            }
        }
    }
}

sealed class CarouselItem {
    object Logo : CarouselItem()
    data class Photo(val url: String) : CarouselItem()
    data class LocalDrawable(val resId: Int) : CarouselItem()
}

@Composable
fun rememberCarouselItems(context: Context): List<CarouselItem> {
    return remember {
        val fields = R.drawable::class.java.fields
        val drawableNames = mutableListOf<String>()
        for (field in fields) {
            val name = field.name
            if (name.startsWith("carousel_")) {
                drawableNames.add(name)
            }
        }
        drawableNames.sort()

        val items = mutableListOf<CarouselItem>()
        if (drawableNames.isNotEmpty()) {
            val mid = drawableNames.size / 2 + 1
            drawableNames.forEachIndexed { index, name ->
                if (index == mid) {
                    items.add(CarouselItem.Logo)
                }
                val resId = context.resources.getIdentifier(name, "drawable", context.packageName)
                items.add(CarouselItem.LocalDrawable(resId))
            }
            if (!items.contains(CarouselItem.Logo)) {
                items.add(drawableNames.size / 2, CarouselItem.Logo)
            }
        } else {
            // Default Fallback items
            items.addAll(
                listOf(
                    CarouselItem.Photo("https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?q=80&w=1000"),
                    CarouselItem.Photo("https://images.unsplash.com/photo-1532634896-26909d0d4b89?q=80&w=1000"),
                    CarouselItem.Logo,
                    CarouselItem.Photo("https://images.unsplash.com/photo-1517457373958-b7bdd4587205?q=80&w=1000"),
                    CarouselItem.Photo("https://images.unsplash.com/photo-1569154941061-e231b4725ef1?q=80&w=1000")
                )
            )
        }
        items
    }
}

@Composable
fun HomeScreen(eventData: EventData?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val carouselItems = rememberCarouselItems(context)
    val listState = rememberLazyListState()
    var selectedFullImageItem by remember { mutableStateOf<CarouselItem?>(null) }

    LaunchedEffect(carouselItems) {
        if (carouselItems.isNotEmpty()) {
            val middleIndex = carouselItems.size / 2
            listState.scrollToItem(middleIndex)
        }
    }

    if (selectedFullImageItem != null) {
        Dialog(
            onDismissRequest = { selectedFullImageItem = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }

            val model: Any = when (val item = selectedFullImageItem) {
                is CarouselItem.Photo -> item.url
                is CarouselItem.LocalDrawable -> item.resId
                is CarouselItem.Logo -> "file:///android_asset/main_icon.png"
                null -> ""
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { selectedFullImageItem = null }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                val oldScale = scale
                                val newScale = (scale * zoom).coerceIn(1f, 5f)
                                val effectiveZoom = if (oldScale == 0f) 1f else newScale / oldScale

                                val center = Offset(size.width / 2f, size.height / 2f)
                                offset = (offset + pan) + (centroid - center - offset) * (1f - effectiveZoom)
                                scale = newScale

                                if (scale <= 1f) {
                                    offset = Offset.Zero
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = model,
                        contentDescription = "Full Screen Zoomable Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Horizontal Scrolling Carousel with clickable app logo card
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val cardWidth = 240.dp
            val horizontalPadding = if (maxWidth > cardWidth) (maxWidth - cardWidth) / 2 else 16.dp

            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = horizontalPadding),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(carouselItems) { item ->
                    when (item) {
                        is CarouselItem.Logo -> {
                            val logoInteractionSource = remember { MutableInteractionSource() }
                            val isLogoPressed by logoInteractionSource.collectIsPressedAsState()
                            val logoShadowX = if (isLogoPressed) 2.dp else 6.dp
                            val logoShadowY = if (isLogoPressed) 2.dp else 6.dp
                            val logoTransX = if (isLogoPressed) 4.dp else 0.dp
                            val logoTransY = if (isLogoPressed) 4.dp else 0.dp

                            Box(modifier = Modifier
                                .size(width = 240.dp, height = 240.dp)
                                .offset(x = logoTransX, y = logoTransY)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .offset(x = logoShadowX, y = logoShadowY)
                                        .background(Color.Black, shape = RoundedCornerShape(8.dp))
                                )
                                Surface(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable(
                                            interactionSource = logoInteractionSource,
                                            indication = null
                                        ) {
                                            scope.launch {
                                                delay(100)
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://hopsinthehangar.com"))
                                                context.startActivity(intent)
                                            }
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    border = BorderStroke(3.dp, Color.Black)
                                ) {
                                    AsyncImage(
                                        model = "file:///android_asset/main_icon.png",
                                        contentDescription = "App Logo",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                        is CarouselItem.Photo -> {
                            val photoInteractionSource = remember { MutableInteractionSource() }
                            val isPhotoPressed by photoInteractionSource.collectIsPressedAsState()
                            val photoShadowX = if (isPhotoPressed) 2.dp else 6.dp
                            val photoShadowY = if (isPhotoPressed) 2.dp else 6.dp
                            val photoTransX = if (isPhotoPressed) 4.dp else 0.dp
                            val photoTransY = if (isPhotoPressed) 4.dp else 0.dp

                            Box(modifier = Modifier
                                .size(width = 240.dp, height = 240.dp)
                                .offset(x = photoTransX, y = photoTransY)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .offset(x = photoShadowX, y = photoShadowY)
                                        .background(Color.Black, shape = RoundedCornerShape(8.dp))
                                )
                                Surface(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable(
                                            interactionSource = photoInteractionSource,
                                            indication = null
                                        ) {
                                            selectedFullImageItem = item
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    border = BorderStroke(3.dp, Color.Black)
                                ) {
                                    AsyncImage(
                                        model = item.url,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                        is CarouselItem.LocalDrawable -> {
                            val drawableInteractionSource = remember { MutableInteractionSource() }
                            val isDrawablePressed by drawableInteractionSource.collectIsPressedAsState()
                            val drawableShadowX = if (isDrawablePressed) 2.dp else 6.dp
                            val drawableShadowY = if (isDrawablePressed) 2.dp else 6.dp
                            val drawableTransX = if (isDrawablePressed) 4.dp else 0.dp
                            val drawableTransY = if (isDrawablePressed) 4.dp else 0.dp

                            Box(modifier = Modifier
                                .size(width = 240.dp, height = 240.dp)
                                .offset(x = drawableTransX, y = drawableTransY)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .offset(x = drawableShadowX, y = drawableShadowY)
                                        .background(Color.Black, shape = RoundedCornerShape(8.dp))
                                )
                                Surface(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable(
                                            interactionSource = drawableInteractionSource,
                                            indication = null
                                        ) {
                                            selectedFullImageItem = item
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    border = BorderStroke(3.dp, Color.Black)
                                ) {
                                    AsyncImage(
                                        model = item.resId,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Accordion for Welcome / About
        NeoAccordion(title = "WELCOME TO THE SHOW", headerColor = NeoWhite) {
            val fullText = "Welcome to Hops in the Hangar, your Craft Beer & Airshow event app! Explore a lineup of vendors and sponsors, discover detailed venue information, find the best hotels nearby, enjoy exciting entertainment, and get to know the featured airshow performers.\n\nCraft beer, beverages, and aircraft come together to create not only a fun social event, but also an extremely unique community experience. Hops in the Hangar celebrates aviation, local businesses, and great craft beverages while bringing people together for an unforgettable evening at the Middletown Regional Airport.\n\nWhether you're here for the thrilling air show performances, the incredible selection of breweries and beverage vendors, or simply to enjoy time with friends and family, this app will help you make the most of your experience. Stay connected with schedules, updates, event maps, and everything you need for an amazing experience at Hops in the Hangar 2026."
            LinkifyText(
                text = fullText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black.copy(alpha = 0.9f)
            )
        }

        if (eventData != null) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "IN THE NEWS",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            NeoCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = NeoPink
            ) {
                Column {
                    Text(
                        "Hops 2026 Recap",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "As featured on WLWT, Hops in the Hangar 2026 was a stellar celebration of craft beer and aviation. Saturday, August 22nd at the Middletown Regional Airport proved to be a perfect backdrop for a fun night where specialty beer enthusiasts and plane lovers combined their passions into one unforgettable experience.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black.copy(alpha = 0.9f)
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
                                        color = Color.Blue,
                                        textDecoration = TextDecoration.Underline,
                                        fontWeight = FontWeight.Black
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
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "VENUE & LOGISTICS",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            NeoCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = NeoBlue
            ) {
                Column {
                    Text("Parking", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = Color.Black)
                    LinkifyText(eventData.info.parking, style = MaterialTheme.typography.bodyMedium, color = Color.Black.copy(alpha = 0.8f))

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Event Rules", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = Color.Black)
                    LinkifyText(eventData.info.rules, style = MaterialTheme.typography.bodyMedium, color = Color.Black.copy(alpha = 0.8f))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            FaqSection(eventData.faq)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "OUR TEAM",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = NeoGreen
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Middletown Aviation Foundation",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                    fontWeight = FontWeight.Black,
                    color = Color.Black,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Your Hops in the Hangar Crew",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Black.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                val crew = listOf(
                    "Rich Bevis", "Kurt Yearout", "Sara Yearout", "Tom Spielmann",
                    "Sean Askren", "Mica Jones", "Missy Lawwill", "Jamie Murphy",
                    "Rahul Menon"
                )

                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    maxItemsInEachRow = 3
                ) {
                    crew.forEach { name ->
                        Box(modifier = Modifier.padding(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .offset(x = 3.dp, y = 3.dp)
                                    .background(Color.Black, shape = RoundedCornerShape(8.dp))
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                border = BorderStroke(2.dp, Color.Black)
                            ) {
                                Text(
                                    name,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
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
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Spacer(modifier = Modifier.height(64.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SponsorsScreen(
    sponsors: List<SponsorItem>,
    onSponsorClick: (SponsorItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val allLevels = sponsors.map { it.level }.distinct()
    var selectedLevels by remember { mutableStateOf(allLevels.toSet()) }
    var selectedSponsor by remember { mutableStateOf<SponsorItem?>(null) }

    val filteredSponsors = sponsors.filter {
        (it.name.contains(searchQuery, ignoreCase = true) || it.level.contains(searchQuery, ignoreCase = true)) &&
                selectedLevels.contains(it.level)
    }

    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val sponsorSheetState = rememberModalBottomSheetState()

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "FILTER SPONSORS",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    allLevels.forEach { level ->
                        NeoFilterButton(
                            text = level,
                            checked = selectedLevels.contains(level),
                            onCheckedChange = { isChecked ->
                                selectedLevels = if (isChecked) {
                                    selectedLevels + level
                                } else {
                                    selectedLevels - level
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                NeoButton(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = NeoYellow
                ) {
                    Text("APPLY FILTERS", fontWeight = FontWeight.Black)
                }
            }
        }
    }

    if (selectedSponsor != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedSponsor = null },
            sheetState = sponsorSheetState,
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            val sponsor = selectedSponsor!!
            val context = LocalContext.current
            val names = if (
                sponsor.name.contains("Kara Goheen", ignoreCase = true) ||
                sponsor.name.contains("Affordable Dentures", ignoreCase = true)
            ) {
                listOf(sponsor.name)
            } else {
                sponsor.name.split("&").map { it.trim() }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(if (names.size > 1) 72.dp else 64.dp)
                            .height(64.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        names.forEachIndexed { index, name ->
                            val resourceName = getResourceName(name)
                            val resourceId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)

                            Surface(
                                modifier = Modifier
                                    .padding(start = (index * 24).dp)
                                    .size(64.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = parseHexColor(sponsor.background),
                                border = BorderStroke(2.dp, Color.Black)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (resourceId != 0) {
                                        AsyncImage(
                                            model = resourceId,
                                            contentDescription = name,
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NeoYellow,
                            border = BorderStroke(1.5.dp, Color.Black)
                        ) {
                            Text(
                                text = sponsor.level.uppercase(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                ),
                                color = Color.Black
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = sponsor.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                }

                NeoCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = NeoWhite
                ) {
                    Column {
                        Text(
                            "ABOUT",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinkifyText(
                            text = sponsor.about?.ifBlank { sponsor.description } ?: sponsor.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black.copy(alpha = 0.9f)
                        )
                    }
                }

                if (!sponsor.email.isNullOrBlank() || !sponsor.phone.isNullOrBlank() || !sponsor.website.isNullOrBlank() || !sponsor.links.isNullOrEmpty()) {
                    val noWebsiteSponsors = setOf("lewis horticultural", "askren balloon team", "kara goheen friends", "rh seals")
                    val isNoWebsite = noWebsiteSponsors.any { sponsor.name.lowercase().contains(it) }

                    NeoCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = NeoWhite
                    ) {
                        Column {
                            Text(
                                "CONTACT INFORMATION",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            sponsor.email?.let { email ->
                                DetailContactRow(
                                    icon = Icons.Default.Email,
                                    value = email,
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                                data = Uri.parse("mailto:$email")
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Log.e("SponsorBottomSheet", "Error sending email", e)
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            sponsor.phone?.let { phone ->
                                DetailContactRow(
                                    icon = Icons.Default.Phone,
                                    value = phone,
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                                data = Uri.parse("tel:$phone")
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Log.e("SponsorBottomSheet", "Error making phone call", e)
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            if (!sponsor.links.isNullOrEmpty()) {
                                sponsor.links.forEach { link ->
                                    DetailContactRow(
                                        icon = Icons.Default.Language,
                                        value = "${link.label}: ${link.url}",
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Log.e("SponsorBottomSheet", "Error opening website", e)
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            } else {
                                sponsor.website?.let { website ->
                                    DetailContactRow(
                                        icon = Icons.Default.Language,
                                        value = website,
                                        onClick = {
                                            if (isNoWebsite || website.isBlank()) {
                                                Toast.makeText(context, "A website does not exist.", Toast.LENGTH_SHORT).show()
                                            } else {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(website))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Log.e("SponsorBottomSheet", "Error opening website", e)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                NeoButton(
                    onClick = { selectedSponsor = null },
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = NeoYellow
                ) {
                    Text("CLOSE", fontWeight = FontWeight.Black)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            NeoTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("SEARCH SPONSORS...", fontWeight = FontWeight.Bold, color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black) },
                singleLine = true
            )

            Spacer(modifier = Modifier.width(12.dp))

            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val shadowOffsetX = if (isPressed) 1.dp else 4.dp
            val shadowOffsetY = if (isPressed) 1.dp else 4.dp
            val translationX = if (isPressed) 3.dp else 0.dp
            val translationY = if (isPressed) 3.dp else 0.dp

            Box(modifier = Modifier.size(56.dp).offset(x = translationX, y = translationY)) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = shadowOffsetX, y = shadowOffsetY)
                        .background(Color.Black, shape = RoundedCornerShape(8.dp))
                )
                Surface(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { showFilterSheet = true }
                        ),
                    shape = RoundedCornerShape(8.dp),
                    color = NeoYellow,
                    border = BorderStroke(3.dp, Color.Black)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = Color.Black)
                    }
                }
            }
        }

        val pinnedNames = setOf("City of Middletown", "MWO", "Start Skydiving", "Team Fastrax")
        val pinnedSponsors = filteredSponsors.filter { it.name in pinnedNames }
        val otherSponsors = filteredSponsors.filter { it.name !in pinnedNames }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (pinnedSponsors.isNotEmpty()) {
                item {
                    Text(
                        "PREMIER SPONSORS",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(pinnedSponsors) { sponsor ->
                    SponsorCard(
                        sponsor = sponsor,
                        isPinned = true,
                        onClick = {
                            onSponsorClick(sponsor)
                            selectedSponsor = sponsor
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(thickness = 3.dp, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            items(otherSponsors) { sponsor ->
                SponsorCard(
                    sponsor = sponsor,
                    isPinned = false,
                    onClick = {
                        onSponsorClick(sponsor)
                        selectedSponsor = sponsor
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun SponsorCard(
    sponsor: SponsorItem,
    isPinned: Boolean,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = 6.dp, y = 6.dp)
                .background(Color.Black, shape = RoundedCornerShape(8.dp))
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            shape = RoundedCornerShape(8.dp),
            color = if (isPinned) NeoYellow else NeoWhite,
            contentColor = Color.Black,
            border = BorderStroke(3.dp, Color.Black)
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                NeoListItem(
                    headline = { Text(sponsor.name, fontWeight = FontWeight.Black, color = Color.Black) },
                    supporting = { Text(sponsor.description, color = Color.Black.copy(alpha = 0.8f)) },
                    overline = {
                        Text(
                            sponsor.level.uppercase(),
                            color = Color.Black,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                        )
                    },
                    leading = {
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
                                    shape = RoundedCornerShape(8.dp),
                                    color = parseHexColor(sponsor.background),
                                    border = BorderStroke(2.dp, Color.Black)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (resourceId != 0) {
                                            AsyncImage(
                                                model = resourceId,
                                                contentDescription = name,
                                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)),
                                                contentScale = ContentScale.Fit
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.Star,
                                                contentDescription = null,
                                                tint = Color.Black
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorsScreen(
    vendors: List<VendorItem>,
    onVendorClick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategories by remember { mutableStateOf(setOf("Brewery", "Food Truck")) }
    var selectedVendor by remember { mutableStateOf<VendorItem?>(null) }

    val filteredVendors = vendors.filter {
        (it.name.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true)) &&
                selectedCategories.contains(it.category)
    }

    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val vendorSheetState = rememberModalBottomSheetState()

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "FILTER VENDORS",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("Brewery", "Food Truck").forEach { category ->
                        NeoFilterButton(
                            text = category,
                            checked = selectedCategories.contains(category),
                            onCheckedChange = { isChecked ->
                                selectedCategories = if (isChecked) {
                                    selectedCategories + category
                                } else {
                                    selectedCategories - category
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                NeoButton(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = NeoYellow
                ) {
                    Text("APPLY FILTERS", fontWeight = FontWeight.Black)
                }
            }
        }
    }

    if (selectedVendor != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedVendor = null },
            sheetState = vendorSheetState,
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            val vendor = selectedVendor!!
            val context = LocalContext.current
            val resourceName = getResourceName(vendor.name)
            val resourceId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.size(64.dp)) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .offset(x = 4.dp, y = 4.dp)
                                .background(Color.Black, shape = RoundedCornerShape(8.dp))
                        )
                        Surface(
                            modifier = Modifier.matchParentSize(),
                            shape = RoundedCornerShape(8.dp),
                            color = parseHexColor(vendor.background),
                            border = BorderStroke(2.dp, Color.Black)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (resourceId != 0) {
                                    AsyncImage(
                                        model = resourceId,
                                        contentDescription = vendor.name,
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Fit
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
                                        tint = Color.Black,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NeoYellow,
                            border = BorderStroke(1.5.dp, Color.Black)
                        ) {
                            Text(
                                text = vendor.category.uppercase(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                ),
                                color = Color.Black
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = vendor.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                }

                NeoCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = NeoWhite
                ) {
                    Column {
                        Text(
                            "ABOUT",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinkifyText(
                            text = vendor.about?.ifBlank { vendor.description } ?: vendor.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black.copy(alpha = 0.9f)
                        )
                    }
                }

                if (!vendor.email.isNullOrBlank() || !vendor.phone.isNullOrBlank() || !vendor.website.isNullOrBlank()) {
                    val noWebsiteNames = setOf("lewis horticultural", "askren balloon team", "kara goheen friends", "rh seals")
                    val isNoWebsite = noWebsiteNames.any { vendor.name.lowercase().contains(it) }

                    NeoCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = NeoWhite
                    ) {
                        Column {
                            Text(
                                "CONTACT INFORMATION",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            vendor.email?.let { email ->
                                DetailContactRow(
                                    icon = Icons.Default.Email,
                                    value = email,
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                                data = Uri.parse("mailto:$email")
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Log.e("VendorBottomSheet", "Error sending email", e)
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            vendor.phone?.let { phone ->
                                DetailContactRow(
                                    icon = Icons.Default.Phone,
                                    value = phone,
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                                data = Uri.parse("tel:$phone")
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Log.e("VendorBottomSheet", "Error making phone call", e)
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            vendor.website?.let { website ->
                                DetailContactRow(
                                    icon = Icons.Default.Language,
                                    value = website,
                                    onClick = {
                                        if (isNoWebsite) {
                                            Toast.makeText(context, "A website does not exist.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(website))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Log.e("VendorBottomSheet", "Error opening website", e)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                NeoButton(
                    onClick = { selectedVendor = null },
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = NeoYellow
                ) {
                    Text("CLOSE", fontWeight = FontWeight.Black)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            NeoTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("SEARCH VENDORS...", fontWeight = FontWeight.Bold, color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black) },
                singleLine = true
            )

            Spacer(modifier = Modifier.width(12.dp))

            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val shadowOffsetX = if (isPressed) 1.dp else 4.dp
            val shadowOffsetY = if (isPressed) 1.dp else 4.dp
            val translationX = if (isPressed) 3.dp else 0.dp
            val translationY = if (isPressed) 3.dp else 0.dp

            Box(modifier = Modifier.size(56.dp).offset(x = translationX, y = translationY)) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = shadowOffsetX, y = shadowOffsetY)
                        .background(Color.Black, shape = RoundedCornerShape(8.dp))
                )
                Surface(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { showFilterSheet = true }
                        ),
                    shape = RoundedCornerShape(8.dp),
                    color = NeoYellow,
                    border = BorderStroke(3.dp, Color.Black)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = Color.Black)
                    }
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(filteredVendors) { vendor ->
                NeoCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onVendorClick(vendor.name)
                        selectedVendor = vendor
                    },
                    containerColor = NeoWhite
                ) {
                    NeoListItem(
                        headline = { Text(vendor.name, fontWeight = FontWeight.Black) },
                        supporting = { Text(vendor.description, color = Color.Black.copy(alpha = 0.8f)) },
                        overline = {
                            Text(
                                vendor.category.uppercase(),
                                color = Color.Black,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black)
                            )
                        },
                        leading = {
                            val context = LocalContext.current
                            val resourceName = getResourceName(vendor.name)
                            val resourceId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)

                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = parseHexColor(vendor.background),
                                border = BorderStroke(2.dp, Color.Black)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (resourceId != 0) {
                                        AsyncImage(
                                            model = resourceId,
                                            contentDescription = vendor.name,
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)),
                                            contentScale = ContentScale.Fit
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
                                            tint = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun DetailContactRow(icon: ImageVector, value: String, onClick: () -> Unit = {}) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scope = rememberCoroutineScope()

    val shadowOffsetX = if (isPressed) 1.dp else 3.dp
    val shadowOffsetY = if (isPressed) 1.dp else 3.dp
    val translationX = if (isPressed) 2.dp else 0.dp
    val translationY = if (isPressed) 2.dp else 0.dp

    Box(modifier = Modifier.fillMaxWidth().offset(x = translationX, y = translationY)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffsetX, y = shadowOffsetY)
                .background(Color.Black, shape = RoundedCornerShape(6.dp))
        )
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = NeoWhite,
            border = BorderStroke(2.dp, Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    scope.launch {
                        delay(100)
                        onClick()
                    }
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(icon, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    value,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun EntertainmentButton(
    item: EntertainmentItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = NeoWhite
) {
    val category = item.category?.ifBlank { null } ?: when {
        item.role.contains("Singer", ignoreCase = true) || item.role.contains("Anthem", ignoreCase = true) -> "ANTHEM"
        item.role.contains("DJ", ignoreCase = true) || item.role.contains("Music", ignoreCase = true) -> "MUSIC"
        item.role.contains("Check In", ignoreCase = true) -> "CHECK IN"
        item.role.contains("Announcer", ignoreCase = true) -> "ANNOUNCER"
        else -> "PERFORMANCE"
    }

    val icon = when (category) {
        "MUSIC" -> Icons.Default.MusicNote
        "ANTHEM" -> Icons.Default.RecordVoiceOver
        "CHECK IN" -> Icons.Default.VolunteerActivism
        "ANNOUNCER" -> Icons.Default.Mic
        else -> Icons.Default.AirplanemodeActive
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val shadowOffsetX = if (isPressed) 2.dp else 5.dp
    val shadowOffsetY = if (isPressed) 2.dp else 5.dp
    val translationX = if (isPressed) 3.dp else 0.dp
    val translationY = if (isPressed) 3.dp else 0.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .offset(x = translationX, y = translationY)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffsetX, y = shadowOffsetY)
                .background(Color.Black, shape = RoundedCornerShape(8.dp))
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            shape = RoundedCornerShape(8.dp),
            color = containerColor,
            border = BorderStroke(3.dp, Color.Black)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = NeoYellow,
                        border = BorderStroke(2.dp, Color.Black),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NeoPink,
                            border = BorderStroke(1.dp, Color.Black)
                        ) {
                            Text(
                                text = category.uppercase(),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 8.sp,
                                    letterSpacing = 1.sp
                                ),
                                color = Color.Black
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntertainmentScreen(
    groundEntertainment: List<EntertainmentItem>,
    performers: List<EntertainmentItem>,
    schedule: List<ScheduleItem>
) {
    var selectedEntertainmentItem by remember { mutableStateOf<EntertainmentItem?>(null) }
    val itemSheetState = rememberModalBottomSheetState()

    if (selectedEntertainmentItem != null) {
        val item = selectedEntertainmentItem!!
        val context = LocalContext.current

        val category = item.category?.ifBlank { null } ?: when {
            item.role.contains("Singer", ignoreCase = true) || item.role.contains("Anthem", ignoreCase = true) -> "ANTHEM"
            item.role.contains("DJ", ignoreCase = true) || item.role.contains("Music", ignoreCase = true) -> "MUSIC"
            item.role.contains("Check In", ignoreCase = true) -> "CHECK IN"
            item.role.contains("Announcer", ignoreCase = true) -> "ANNOUNCER"
            else -> "PERFORMANCE"
        }

        val icon = when (category) {
            "MUSIC" -> Icons.Default.MusicNote
            "ANTHEM" -> Icons.Default.RecordVoiceOver
            "CHECK IN" -> Icons.Default.VolunteerActivism
            "ANNOUNCER" -> Icons.Default.Mic
            else -> Icons.Default.AirplanemodeActive
        }

        ModalBottomSheet(
            onDismissRequest = { selectedEntertainmentItem = null },
            sheetState = itemSheetState,
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.size(64.dp)) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .offset(x = 4.dp, y = 4.dp)
                                .background(Color.Black, shape = RoundedCornerShape(8.dp))
                        )
                        Surface(
                            modifier = Modifier.matchParentSize(),
                            shape = RoundedCornerShape(8.dp),
                            color = NeoYellow,
                            border = BorderStroke(2.dp, Color.Black)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NeoPink,
                            border = BorderStroke(1.5.dp, Color.Black)
                        ) {
                            Text(
                                text = category.uppercase(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                ),
                                color = Color.Black
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                }

                // Role / Details Card
                NeoCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = NeoWhite
                ) {
                    Column {
                        Text(
                            "ROLE & DETAILS",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.role,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        val aboutText = item.about?.ifBlank { item.description } ?: item.description
                        if (!aboutText.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinkifyText(
                                text = aboutText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                // Social / Contact Information Card
                val hasSocial = !item.socialPlatform.isNullOrBlank() || !item.socialHandle.isNullOrBlank() || !item.socialUrl.isNullOrBlank()
                val hasContact = !item.contactInfo.isNullOrBlank()

                if (hasSocial || hasContact) {
                    NeoCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = NeoWhite
                    ) {
                        Column {
                            Text(
                                "CONNECT & CONTACT",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            if (hasSocial) {
                                val handleText = buildString {
                                    if (!item.socialPlatform.isNullOrBlank()) append("${item.socialPlatform}: ")
                                    if (!item.socialHandle.isNullOrBlank()) append(item.socialHandle)
                                }
                                val icon = if (item.socialPlatform?.equals("website", ignoreCase = true) == true) {
                                    Icons.Default.Language
                                } else {
                                    Icons.Default.Share
                                }
                                if (handleText.isNotBlank()) {
                                    DetailContactRow(
                                        icon = icon,
                                        value = handleText,
                                        onClick = {
                                            if (!item.socialUrl.isNullOrBlank()) {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.socialUrl))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Log.e("EntertainmentSheet", "Error opening social URL", e)
                                                }
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                if (!item.socialUrl.isNullOrBlank() && item.socialHandle.isNullOrBlank()) {
                                    DetailContactRow(
                                        icon = Icons.Default.Language,
                                        value = item.socialUrl,
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.socialUrl))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Log.e("EntertainmentSheet", "Error opening social URL", e)
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            if (hasContact) {
                                val contactParts = item.contactInfo.orEmpty().split("|", ",").map { it.trim() }.filter { it.isNotEmpty() }
                                contactParts.forEach { contactStr ->
                                    val isEmail = contactStr.contains("@") || contactStr.startsWith("mailto:", ignoreCase = true)
                                    val isPhone = !isEmail && contactStr.any { it.isDigit() } && contactStr.replace(Regex("[^0-9]"), "").length >= 7
                                    val isUrl = contactStr.startsWith("http://", ignoreCase = true) || contactStr.startsWith("https://", ignoreCase = true)

                                    val icon = when {
                                        isEmail -> Icons.Default.Email
                                        isPhone -> Icons.Default.Phone
                                        isUrl -> Icons.Default.Language
                                        else -> Icons.Default.Info
                                    }

                                    DetailContactRow(
                                        icon = icon,
                                        value = contactStr,
                                        onClick = {
                                            try {
                                                when {
                                                    isEmail -> {
                                                        val cleanEmail = contactStr.replace("mailto:", "", ignoreCase = true).trim()
                                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                                            data = Uri.parse("mailto:$cleanEmail")
                                                        }
                                                        context.startActivity(intent)
                                                    }
                                                    isPhone -> {
                                                        val cleanPhone = contactStr.replace(Regex("[^0-9+]"), "")
                                                        val intent = Intent(Intent.ACTION_DIAL).apply {
                                                            data = Uri.parse("tel:$cleanPhone")
                                                        }
                                                        context.startActivity(intent)
                                                    }
                                                    isUrl -> {
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(contactStr))
                                                        context.startActivity(intent)
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                Log.e("EntertainmentSheet", "Error opening contact detail", e)
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }

                NeoButton(
                    onClick = { selectedEntertainmentItem = null },
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = NeoYellow
                ) {
                    Text("CLOSE", fontWeight = FontWeight.Black)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "GROUND ENTERTAINMENT",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )

        if (groundEntertainment.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                groundEntertainment.forEach { item ->
                    EntertainmentButton(
                        item = item,
                        onClick = { selectedEntertainmentItem = item }
                    )
                }
            }
        }

        Text(
            text = "AIRSHOW PILOTS / PERFORMERS",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.sp),
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )

        if (performers.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                performers.forEach { item ->
                    EntertainmentButton(
                        item = item,
                        onClick = { selectedEntertainmentItem = item }
                    )
                }
            }
        }

        Text(
            text = "EVENT SCHEDULE",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )

        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = NeoWhite
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                schedule.forEachIndexed { index, item ->
                    NeoListItem(
                        headline = { Text(item.event, fontWeight = FontWeight.Black) },
                        supporting = { Text(item.time, color = Color.Black, fontWeight = FontWeight.Bold) },
                        leading = {
                            Surface(shape = RoundedCornerShape(8.dp), color = NeoPink, border = BorderStroke(2.dp, Color.Black), modifier = Modifier.size(40.dp)) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Event, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp)) }
                            }
                        }
                    )
                    if (index < schedule.size - 1) HorizontalDivider(thickness = 2.dp, color = Color.Black, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
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
