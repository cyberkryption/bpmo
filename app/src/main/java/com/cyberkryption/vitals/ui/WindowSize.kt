package com.cyberkryption.vitals.ui

import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass

/** At least 600dp wide: rail navigation and two-pane History. */
val WindowSizeClass.isMediumOrWider: Boolean
    get() = windowWidthSizeClass != WindowWidthSizeClass.COMPACT

/** At least 840dp wide: Trends sits beside the Dashboard. */
val WindowSizeClass.isExpanded: Boolean
    get() = windowWidthSizeClass == WindowWidthSizeClass.EXPANDED
