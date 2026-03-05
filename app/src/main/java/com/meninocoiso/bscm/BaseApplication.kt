package com.meninocoiso.bscm

import android.app.Application
import com.meninocoiso.bscm.service.InteractionSyncService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BaseApplication : Application() {
    
    @Inject
    lateinit var interactionSyncService: InteractionSyncService
    // InteractionSyncService is initialized automatically via Hilt injection
    // It will start monitoring network connectivity and process queued interactions
}