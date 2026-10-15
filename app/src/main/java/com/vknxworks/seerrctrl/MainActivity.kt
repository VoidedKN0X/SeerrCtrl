package com.vknxworks.seerrctrl

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
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
import com.vknxworks.seerrctrl.ui.SetupScreen
import com.vknxworks.seerrctrl.ui.UserSettingsScreen
import com.vknxworks.seerrctrl.ui.UsersScreen
import com.vknxworks.seerrctrl.ui.WatchlistScreen
import com.vknxworks.seerrctrl.ui.admin.AdminSettingsScreen
import com.vknxworks.seerrctrl.ui.admin.AdminSectionScreen
import com.vknxworks.seerrctrl.ui.isAppInDarkTheme
import com.vknxworks.seerrctrl.ui.lock.AppLockViewModel
import com.vknxworks.seerrctrl.ui.lock.LockScreen

// FragmentActivity (a ComponentActivity) is required by the fingerprint prompt.
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.init(this)
        enableEdgeToEdge()
        setContent {
            SeerrTheme {
                val dark = isAppInDarkTheme()
                LaunchedEffect(dark) {
                    val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                }
                val secure = App.lock.pinSet && App.lock.hideContent
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

@Composable
private fun Root(activity: FragmentActivity, lockVm: AppLockViewModel = viewModel()) {
    val nav = rememberNavController()
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun toSetup() {
        nav.navigate("setup") { popUpTo(0) { inclusive = true } }
    }

    fun logout() {
        scope.launch {
            // Best effort: end the session on the server too, then forget it here.
            try { withTimeout(4000) { App.api.post("/auth/logout") } } catch (_: Exception) {}
            App.prefs.clearSession()
            App.session.clear()
            App.api.reload()
            toSetup()
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
            toSetup()
            snack.showSnackbar("Your session ended. Please sign in again.")
        }
    }

    // Load who we are on start.
    LaunchedEffect(Unit) {
        if (App.prefs.isConfigured && App.session.me == null) {
            try { App.session.refresh() } catch (_: Exception) {}
        }
    }

    if (lockVm.locked && App.prefs.isConfigured) {
        LockScreen(
            vm = lockVm,
            onForgot = {
                App.lock.clearPin()
                lockVm.unlock()
                logout()
            },
        )
        return
    }

    CompositionLocalProvider(LocalNav provides nav, LocalSnackbar provides snack) {
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = nav,
                startDestination = if (App.prefs.isConfigured) "main" else "setup",
                enterTransition = { slideInHorizontally(tween(300)) { it / 5 } + fadeIn(tween(220)) },
                exitTransition = { slideOutHorizontally(tween(300)) { -it / 8 } + fadeOut(tween(160)) },
                popEnterTransition = { slideInHorizontally(tween(300)) { -it / 5 } + fadeIn(tween(220)) },
                popExitTransition = { slideOutHorizontally(tween(300)) { it / 8 } + fadeOut(tween(160)) },
            ) {
                composable("setup") {
                    SetupScreen(
                        onDone = { nav.navigate("main") { popUpTo(0) { inclusive = true } } },
                        onCancel = null,
                    )
                }
                composable("main") { MainScreen() }
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
                composable("admin") { AdminSettingsScreen() }
                composable("admin/{section}") { AdminSectionScreen(it.arguments?.getString("section").orEmpty()) }
                composable("appsettings") { AppSettingsScreen(onLogout = ::logout) }
            }
            SnackbarHost(snack, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 80.dp))
        }
    }
}
