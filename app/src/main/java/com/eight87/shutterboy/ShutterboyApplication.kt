package com.eight87.shutterboy

import android.app.Application

/**
 * Process-scoped composition root holder. The single [AppGraph] is constructed
 * here on application start and lives for the process lifetime; activities,
 * view models, and (later) services obtain their narrow facets through
 * [graph].
 */
class ShutterboyApplication : Application() {

    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(applicationContext = this)
    }
}
