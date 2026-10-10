package com.togetherly.app.application

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.navigation.compose.rememberNavController
import com.togetherly.core.locale.AppLocaleController
import com.togetherly.designsystem.theme.TogetherlyTheme
import com.togetherly.navigation.host.TogetherlyNavHost
import org.koin.compose.koinInject

/**
 * [key(language)][key] forces a full recomposition of everything below it whenever the selected
 * language changes — necessary on Android, where [com.togetherly.core.locale.applyPlatformLocale]
 * mutates the JVM default [java.util.Locale] in place, a plain mutation Compose has no way to
 * observe on its own. See [AppLocaleController]'s own KDoc for why iOS doesn't get a live update
 * from this at all (a restart is required there).
 *
 * `navController`/`mainNavController` are both created *outside* the `key(language)` block, on
 * purpose, and threaded into [TogetherlyNavHost] rather than left to its own default
 * `rememberNavController()`s. Without this, remounting on a language change would tear down both
 * `NavHostController`s along with everything else: `navController` owns the *outer* back stack
 * (which root destination is showing — e.g. the Language screen itself, mid-selection, versus
 * [com.togetherly.navigation.destination.RootDestination.Main]), while `mainNavController` owns
 * [com.togetherly.navigation.shell.MainShell]'s own *nested* back stack (which of the four bottom
 * tabs is selected) — two separate controllers, so both need hoisting, not just one. Losing either
 * kicks the user back to the app's start destination (Today) on every switch, rather than leaving
 * them exactly where they were.
 */
@Composable
fun App() {
    val localeController = koinInject<AppLocaleController>()
    val language by localeController.language.collectAsState()
    val navController = rememberNavController()
    val mainNavController = rememberNavController()

    key(language) {
        TogetherlyTheme {
            TogetherlyNavHost(navController = navController, mainNavController = mainNavController)
        }
    }
}
