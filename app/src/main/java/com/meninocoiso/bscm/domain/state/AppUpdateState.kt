package com.meninocoiso.bscm.domain.state

import com.meninocoiso.bscm.domain.result.UiText
import java.io.File

sealed class AppUpdateState {
    data object Idle : AppUpdateState()
    data object UpToDate : AppUpdateState()
    data object Checking : AppUpdateState()
    data class Downloading(val progress: Float) : AppUpdateState()
    data class UpdateAvailable(val version: String) : AppUpdateState()
    data class ReadyToInstall(val apkFile: File) : AppUpdateState()
    data class Error(val message: UiText) : AppUpdateState()
}