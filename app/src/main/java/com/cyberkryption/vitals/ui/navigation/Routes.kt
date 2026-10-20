package com.cyberkryption.vitals.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object TodayRoute

@Serializable
data object HistoryRoute

@Serializable
data object TrendsRoute

@Serializable
data class ReadingRoute(val id: Long)

@Serializable
data object CaptureRoute

@Serializable
data object ImportRoute

@Serializable
data class ReviewRoute(val sessionId: String)

@Serializable
data object SettingsRoute
