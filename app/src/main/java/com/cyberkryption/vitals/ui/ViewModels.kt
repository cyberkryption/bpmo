package com.cyberkryption.vitals.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.cyberkryption.vitals.AppContainer
import com.cyberkryption.vitals.VitalsApplication

/** The app's dependency graph, from inside a `viewModelFactory { initializer { … } }` block. */
val CreationExtras.container: AppContainer
    get() = (this[APPLICATION_KEY] as VitalsApplication).container
