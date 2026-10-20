package com.vknxworks.seerrctrl

import android.graphics.Color
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation.NavGraphBuilder
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.vknxworks.seerrctrl.notify.Notifier
import com.vknxworks.seerrctrl.ui.CalendarScreen
import com.vknxworks.seerrctrl.ui.MainTab
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vknxworks.seerrctrl.ui.AppSettingsScreen
import com.vknxworks.seerrctrl.ui.BlocklistScreen
import com.vknxworks.seerrctrl.ui.BrowseScreen
import com.vknxworks.seerrctrl.ui.CollectionScreen
import com.vknxworks.seerrctrl.ui.IssueScreen
import com.vknxworks.seerrctrl.ui.LocalNav
import com.vknxworks.seerrctrl.ui.LocalSnackbar
import com.vknxworks.seerrctrl.ui.MainScreen
import com.vknxworks.seerrctrl.ui.MediaScreen
import com.vknxworks.seerrctrl.ui.PersonScreen
import com.vknxworks.seerrctrl.ui.ProfileScreen
import com.vknxworks.seerrctrl.ui.SeerrTheme
import com.vknxworks.seerrctrl.ui.ServerTrustPrompts
import com.vknxworks.seerrctrl.ui.SetupScreen
import com.vknxworks.seerrctrl.ui.UserSettingsScreen
import com.vknxworks.seerrctrl.ui.UsersScreen
import com.vknxworks.seerrctrl.ui.WatchlistScreen
import com.vknxworks.seerrctrl.ui.admin.AdminSettingsScreen
import com.vknxworks.seerrctrl.ui.admin.AdminSectionScreen
import com.vknxworks.seerrctrl.ui.isAppInDarkTheme
import com.vknxworks.seerrctrl.ui.lock.AppLockViewModel
import com.vknxworks.seerrctrl.ui.lock.LockScreen
import com.vknxworks.seerrctrl.ui.lock.expectReturn
import androidx.compose.ui.semantics.clearAndSetSemantics

// FragmentActivity (a ComponentActivity) is required by the fingerprint prompt.
class MainActivity : FragmentActivity() {
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        takeRoute(intent)
    }

    /**
     * Notifications open a specific screen ("movie/123", "issue/4", "requests"). This activity is exported (it's
     * the launcher), so only routes carrying this install's secret token are taken, and only a few route shapes.
     */
    private fun takeRoute(intent: android.content.Intent?) {
        // seerrctrl://movie/{tmdbId} and seerrctrl://tv/{tmdbId}, from the other Ctrl apps (e.g. ArrCtrl's
        // "Open in SeerrCtrl"). They only open a movie or series page, so they need no token, but only those
        // two shapes with a plain number are accepted.
        val link = intent?.data
        if (intent != null && link != null && intent.action == android.content.Intent.ACTION_VIEW && link.scheme == "seerrctrl") {
            val kind = link.host
            val id = link.pathSegments.singleOrNull()?.toLongOrNull()
            intent.data = null
            if ((kind == "movie" || kind == "tv") && id != null && id > 0) DeepLink.route = "$kind/$id"
            return
        }
        val r = intent?.getStringExtra(EXTRA_ROUTE) ?: return
        val token = intent.getStringExtra(EXTRA_TOKEN)
        intent.removeExtra(EXTRA_ROUTE)
        intent.removeExtra(EXTRA_TOKEN)
        if (!ALLOWED_ROUTE.matches(r) || !Notifier.checkRouteToken(this, token)) return
        if (r == "requests") MainTab.selected = MainTab.REQUESTS else DeepLink.route = r
    }

    companion object {
        const val EXTRA_ROUTE = "route"
        const val EXTRA_TOKEN = "route_token"
        private val ALLOWED_ROUTE = Regex("""^(movie|tv|issue)/\d+$|^requests$""")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.init(this)
        // Never keep a snapshot of the app for the recents screen.
        setRecentsScreenshotEnabled(false)
        takeRoute(intent)
        enableEdgeToEdge()
        setContent {
            SeerrTheme {
                val dark = isAppInDarkTheme()
                LaunchedEffect(dark) {
                    val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                }
                // Independent of the PIN; admin settings screens (API keys, passwords) are always protected.
                val secure = App.lock.hideContent || SecureScreen.active > 0
                LaunchedEffect(secure) {
                    if (secure) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { Root(this) }
            }
        }
    }
}

private fun NavBackStackEntry.intArg(name: String) = arguments?.getString(name)?.toIntOrNull() ?: 0

/** Screens that must never be captured (admin settings) bump [active] while shown. */
object SecureScreen {
    var active by mutableIntStateOf(0)
}

/** Keeps FLAG_SECURE on while the calling screen is showing, whatever the user's setting. */
@Composable
fun SecureWindow() {
    DisposableEffect(Unit) {
        SecureScreen.active++
        onDispose { SecureScreen.active-- }
    }
}

/**
 * Signed out or the session ended: background checks, posted notifications, cached title details and cached
 * images (posters, avatars) of this account are removed from the phone.
 */
private fun forgetAccountData(ctx: android.content.Context) {
    Notifier.signOut(ctx)
    com.vknxworks.seerrctrl.ui.MediaCache.clear()
    val images = coil3.SingletonImageLoader.get(ctx)
    images.memoryCache?.clear()
    Thread { runCatching { images.diskCache?.clear() } }.start()
}

/** Where a notification asked to go; picked up by whichever layout is showing. */
object DeepLink {
    var route by mutableStateOf<String?>(null)
}

@Composable
private fun Root(activity: FragmentActivity, lockVm: AppLockViewModel = viewModel()) {
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var signedIn by remember { mutableStateOf(App.prefs.isConfigured) }

    fun logout() {
        // Leave the account's screens straight away: after "Forgot PIN? Log out" nothing may show while the
        // server is being told (up to 4 s).
        signedIn = false
        scope.launch {
            // Best effort: end the session on the server too, then forget it here.
            try { withTimeout(4000) { App.api.post("/auth/logout") } } catch (_: Exception) {}
            App.prefs.clearSession()
            App.session.clear()
            App.api.reload()
            forgetAccountData(activity)
            MainTab.selected = MainTab.DISCOVER
            signedIn = false
        }
    }

    // Lock when the app goes to the background (immediately, or after the chosen delay).
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> if (!activity.isChangingConfigurations) lockVm.onBackground()
                Lifecycle.Event.ON_START -> lockVm.onForeground()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    // Session ended on the server (expired, revoked, password changed): sign in again.
    LaunchedEffect(App.api.sessionExpired) {
        if (App.api.sessionExpired) {
            App.prefs.clearSession()
            App.session.clear()
            forgetAccountData(activity)
            signedIn = false
            snack.showSnackbar("Your session ended. Please sign in again.")
        }
    }

    // Ask once for permission to post notifications (Android 13+).
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) Notifier.schedule(activity)
    }
    LaunchedEffect(signedIn) {
        if (signedIn && !App.settings.askedNotificationPermission && !Notifier.canPost(activity)) {
            App.settings.askedNotificationPermission = true
            expectReturn()
            permLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Load who we are on start (once unlocked: the sign-in may be sealed by the app lock), and keep
    // background checks scheduled.
    LaunchedEffect(signedIn, lockVm.locked) {
        if (signedIn && !lockVm.locked && App.session.me == null) {
            try { App.session.refresh() } catch (_: Exception) {}
        }
        if (signedIn) Notifier.schedule(activity) else Notifier.cancel(activity)
    }

    val showLock = lockVm.locked && App.prefs.isConfigured
    // Before the first unlock nothing of the account is shown. After that, the screens stay open underneath
    // the lock (hidden and unreachable), so a half-written request or issue is still there after unlocking.
    if (showLock && !lockVm.everUnlocked) {
        LockScreen(vm = lockVm, onForgot = { forgetPin(lockVm, ::logout) })
        return
    }

    CompositionLocalProvider(LocalSnackbar provides snack) {
        Box(Modifier.fillMaxSize().then(if (showLock) Modifier.clearAndSetSemantics { } else Modifier)) {
            if (!signedIn) {
                SetupScreen(onDone = { signedIn = true }, onCancel = null)
            } else {
                AdaptiveShell(activity, ::logout)
                ServerTrustPrompts(onLogout = ::logout)
            }
            SnackbarHost(snack, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 92.dp))
        }
    }
    if (showLock) LockScreen(vm = lockVm, onForgot = { forgetPin(lockVm, ::logout) })
}

/** "Forgot PIN?": the sign-in can't be decrypted without it, so turn the lock off and log out. */
private fun forgetPin(lockVm: AppLockViewModel, logout: () -> Unit) {
    App.lock.disable()
    lockVm.reset()
    logout()
}

/**
 * Phones get one stack of screens with a bottom bar. Wide windows (tablets, unfolded foldables, landscape)
 * get a navigation rail, and from 700 dp the tabs stay on the left while details open on the right —
 * split along the hinge when the phone has one.
 */
@Composable
private fun AdaptiveShell(activity: FragmentActivity, logout: () -> Unit) {
    val layoutInfo by remember(activity) { WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity) }
        .collectAsState(initial = null)
    val density = LocalDensity.current
    val hinge = layoutInfo?.displayFeatures?.filterIsInstance<FoldingFeature>()
        ?.firstOrNull { it.orientation == FoldingFeature.Orientation.VERTICAL && it.isSeparating }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = maxWidth
        val twoPane = width >= 700.dp
        MainTab.rail = width >= 600.dp
        key(twoPane) {
            val nav = rememberNavController()
            LaunchedEffect(DeepLink.route) {
                DeepLink.route?.let { r -> DeepLink.route = null; nav.navigate(r) }
            }
            CompositionLocalProvider(LocalNav provides nav) {
                if (!twoPane) {
                    NavHost(nav, startDestination = "main", enterTransition = enter, exitTransition = exit, popEnterTransition = popEnter, popExitTransition = popExit) {
                        composable("main") { MainScreen() }
                        detailRoutes(logout)
                    }
                } else {
                    val hingeStart = hinge?.let { with(density) { it.bounds.left.toDp() } }
                    val hingeEnd = hinge?.let { with(density) { it.bounds.right.toDp() } }
                    val listWidth = if (hingeStart != null && hingeStart > 280.dp && hingeStart < width - 280.dp) hingeStart
                    else (width * 0.42f).coerceIn(360.dp, 480.dp)
                    val gap = if (hingeStart != null && hingeEnd != null && hingeStart == listWidth) hingeEnd - hingeStart else 0.dp
                    Row(Modifier.fillMaxSize()) {
                        Box(Modifier.width(listWidth).fillMaxHeight()) { MainScreen() }
                        if (gap > 0.dp) Spacer(Modifier.width(gap)) else VerticalDivider()
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            NavHost(nav, startDestination = "empty", enterTransition = enter, exitTransition = exit, popEnterTransition = popEnter, popExitTransition = popExit) {
                                composable("empty") { EmptyPane() }
                                detailRoutes(logout)
                            }
                        }
                    }
                }
            }
        }
    }
}

private val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
    { slideInHorizontally(tween(300)) { it / 5 } + fadeIn(tween(220)) }
private val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
    { slideOutHorizontally(tween(300)) { -it / 8 } + fadeOut(tween(160)) }
private val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
    { slideInHorizontally(tween(300)) { -it / 5 } + fadeIn(tween(220)) }
private val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
    { slideOutHorizontally(tween(300)) { it / 8 } + fadeOut(tween(160)) }

private fun NavGraphBuilder.detailRoutes(logout: () -> Unit) {
    composable("movie/{id}") { MediaScreen("movie", it.intArg("id")) }
    composable("tv/{id}") { MediaScreen("tv", it.intArg("id")) }
    composable("person/{id}") { PersonScreen(it.intArg("id")) }
    composable("collection/{id}") { CollectionScreen(it.intArg("id")) }
    composable("browse/{key}") { BrowseScreen(it.arguments?.getString("key").orEmpty()) }
    composable("issue/{id}") { IssueScreen(it.intArg("id")) }
    composable("user/{id}") { ProfileScreen(it.intArg("id")) }
    composable("user/{id}/settings") { UserSettingsScreen(it.intArg("id")) }
    composable("users") { UsersScreen() }
    composable("watchlist/{id}") { WatchlistScreen(it.intArg("id")) }
    composable("blocklist") { BlocklistScreen() }
    composable("calendar") { CalendarScreen() }
    composable("admin") { AdminSettingsScreen() }
    composable("admin/{section}") { AdminSectionScreen(it.arguments?.getString("section").orEmpty()) }
    composable("appsettings") { AppSettingsScreen(onLogout = logout) }
}

@Composable
private fun EmptyPane() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.Movie, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(12.dp))
        Text("Pick something on the left", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
