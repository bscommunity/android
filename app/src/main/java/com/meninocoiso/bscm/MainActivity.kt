package com.meninocoiso.bscm

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.meninocoiso.bscm.presentation.navigation.MainNav
import com.meninocoiso.bscm.presentation.ui.theme.BeatstarCommunityTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val intentFlow = MutableSharedFlow<Intent>(
        replay = 0,
        extraBufferCapacity = 1
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BeatstarCommunityTheme(
                darkTheme = false,
                dynamicColor = false,
            ) {
                MainNav(
                    hasUpdate = false,
                    user = null,
                    startOAuth = { },
                    intentFlow = intentFlow,
                )
            }
        }
    }
}