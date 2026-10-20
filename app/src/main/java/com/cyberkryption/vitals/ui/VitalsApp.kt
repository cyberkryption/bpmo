package com.cyberkryption.vitals.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import androidx.window.core.layout.WindowSizeClass
import com.cyberkryption.vitals.AppContainer
import com.cyberkryption.vitals.core.Reading
import com.cyberkryption.vitals.review.ReviewItem
import com.cyberkryption.vitals.ui.capture.CaptureScreen
import com.cyberkryption.vitals.ui.dashboard.DashboardScreen
import com.cyberkryption.vitals.ui.detail.ReadingDetailScreen
import com.cyberkryption.vitals.ui.history.HistoryScreen
import com.cyberkryption.vitals.ui.importer.ImportScreen
import com.cyberkryption.vitals.ui.navigation.CaptureRoute
import com.cyberkryption.vitals.ui.navigation.HistoryRoute
import com.cyberkryption.vitals.ui.navigation.ImportRoute
import com.cyberkryption.vitals.ui.navigation.ReadingRoute
import com.cyberkryption.vitals.ui.navigation.ReviewRoute
import com.cyberkryption.vitals.ui.navigation.SettingsRoute
import com.cyberkryption.vitals.ui.navigation.TodayRoute
import com.cyberkryption.vitals.ui.navigation.TopLevel
import com.cyberkryption.vitals.ui.navigation.TrendsRoute
import com.cyberkryption.vitals.ui.navigation.VitalsNavigationBar
import com.cyberkryption.vitals.ui.navigation.VitalsNavigationRail
import com.cyberkryption.vitals.ui.review.ReviewScreen
import com.cyberkryption.vitals.ui.settings.SettingsScreen
import com.cyberkryption.vitals.ui.theme.Vitals
import com.cyberkryption.vitals.ui.trends.TrendsScreen

/**
 * App shell. Compact windows get the bottom bar; medium and up get the rail, which holds
 * Scan, so the Dashboard drops its FABs. Expanded windows show Trends beside the Dashboard.
 */
@Composable
fun VitalsApp(container: AppContainer, navController: NavHostController) {
    val sizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val useRail = sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    val expanded = sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    val topLevel = when {
        destination == null -> null
        destination.hasRoute<TodayRoute>() -> TopLevel.TODAY
        destination.hasRoute<HistoryRoute>() -> TopLevel.HISTORY
        destination.hasRoute<TrendsRoute>() -> TopLevel.TRENDS
        else -> null
    }
    var historyDetailOnly by rememberSaveable { mutableStateOf(false) }
    val showRail = useRail && topLevel != null
    val showBar = !useRail && topLevel != null && !(topLevel == TopLevel.HISTORY && historyDetailOnly)
    val fullBleed = destination?.hasRoute<CaptureRoute>() == true

    fun select(item: TopLevel) {
        val route: Any = when (item) {
            TopLevel.TODAY -> TodayRoute
            TopLevel.HISTORY -> HistoryRoute
            TopLevel.TRENDS -> TrendsRoute
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun edit(reading: Reading) {
        val id = container.reviewSessions.create(
            listOf(ReviewItem(Uri.parse(reading.photoUri), reading.takenAt, reading.takenAtSource, existing = reading)),
        )
        navController.navigate(ReviewRoute(id))
    }

    val scan = { navController.navigate(CaptureRoute) }
    val c = Vitals.colors

    // The NavHost keeps one position in the tree whatever chrome surrounds it, so no state is lost on resize.
    Row(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
    ) {
        if (showRail) VitalsNavigationRail(topLevel, ::select, onScan = scan)
        Column(Modifier.weight(1f)) {
            Box(
                Modifier
                    .weight(1f)
                    .then(if (fullBleed) Modifier else Modifier.statusBarsPadding())
                    .then(if (fullBleed || showBar) Modifier else Modifier.navigationBarsPadding()),
            ) {
                NavHost(navController, startDestination = TodayRoute, modifier = Modifier.fillMaxSize()) {
                    composable<TodayRoute> {
                        val dashboard: @Composable (Modifier) -> Unit = { modifier ->
                            DashboardScreen(
                                onScan = scan,
                                onImport = { navController.navigate(ImportRoute) },
                                onOpenReading = { navController.navigate(ReadingRoute(it)) },
                                onSeeAll = { select(TopLevel.HISTORY) },
                                onSettings = { navController.navigate(SettingsRoute) },
                                showFabs = !useRail,
                                modifier = modifier,
                            )
                        }
                        if (expanded) {
                            Row(Modifier.fillMaxSize()) {
                                dashboard(Modifier.weight(1f))
                                Box(Modifier.width(1.dp).fillMaxHeight().background(c.divider))
                                TrendsScreen(Modifier.width(520.dp))
                            }
                        } else {
                            dashboard(Modifier)
                        }
                    }
                    composable<HistoryRoute> {
                        HistoryScreen(onEdit = ::edit, onDetailOnlyChange = { historyDetailOnly = it })
                    }
                    composable<TrendsRoute> { TrendsScreen() }
                    composable<ReadingRoute> { backStackEntry ->
                        val route = backStackEntry.toRoute<ReadingRoute>()
                        ReadingDetailScreen(
                            readingId = route.id,
                            showBack = true,
                            onBack = { navController.popBackStack() },
                            onEdit = ::edit,
                            onDeleted = { navController.popBackStack() },
                        )
                    }
                    composable<CaptureRoute> {
                        CaptureScreen(
                            onClose = { navController.popBackStack() },
                            onGallery = {
                                navController.navigate(ImportRoute) { popUpTo<CaptureRoute> { inclusive = true } }
                            },
                            onCaptured = { id ->
                                navController.navigate(ReviewRoute(id)) { popUpTo<CaptureRoute> { inclusive = true } }
                            },
                        )
                    }
                    composable<ImportRoute> {
                        ImportScreen(
                            onClose = { navController.popBackStack() },
                            onReview = { id ->
                                navController.navigate(ReviewRoute(id)) { popUpTo<ImportRoute> { inclusive = true } }
                            },
                        )
                    }
                    composable<ReviewRoute> {
                        ReviewScreen(onDone = { navController.popBackStack() })
                    }
                    composable<SettingsRoute> {
                        SettingsScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
            if (showBar) VitalsNavigationBar(topLevel, ::select)
        }
    }
}
