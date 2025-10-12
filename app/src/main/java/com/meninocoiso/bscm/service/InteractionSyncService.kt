package com.meninocoiso.bscm.service

import android.util.Log
import com.meninocoiso.bscm.data.manager.InteractionQueueManager
import com.meninocoiso.bscm.monitor.NetworkConnectivityMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "InteractionSyncService"

@Singleton
class InteractionSyncService @Inject constructor(
    private val networkMonitor: NetworkConnectivityMonitor,
    private val queueManager: InteractionQueueManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    init {
        startMonitoring()
    }
    
    /**
     * Starts monitoring network connectivity and processes queue when online
     */
    private fun startMonitoring() {
        scope.launch {
            networkMonitor.isConnected
                .distinctUntilChanged()
                .filter { it } // Only process when connection becomes available
                .collect { isConnected ->
                    if (isConnected) {
                        Log.d(TAG, "Network connected, processing interaction queue")
                        queueManager.processQueuedInteractions()
                    }
                }
        }
    }
}
