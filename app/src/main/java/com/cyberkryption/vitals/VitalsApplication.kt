package com.cyberkryption.vitals

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner

open class VitalsApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = createContainer()
        ProcessLifecycleOwner.get().lifecycle.addObserver(container.appLock)
    }

    /** Screenshot tests swap in a fixed clock here. */
    protected open fun createContainer(): AppContainer = AppContainer(this)
}
