package com.meninocoiso.bscm.presentation.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.meninocoiso.bscm.presentation.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

private const val TAG = "OAuthRedirect"

/**
 * This activity handles OAuth redirects from Custom Chrome Tabs and processes the callback.
 * It consolidates both redirect handling and callback processing in one place.
 */
@AndroidEntryPoint
class OAuthRedirectActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "OAuth redirect activity created")
        
        handleOAuthCallback()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        
        Log.d(TAG, "New intent received in OAuth redirect")
        handleOAuthCallback()
    }

    private fun handleOAuthCallback() {
        // Extract code or error from redirect URL
        val uri = intent.data
        if (uri != null && uri.scheme == "bscm" && uri.host == "auth") {
            Log.d(TAG, "Valid URI received: $uri")
            
            // Check if we have a code or an error
            val code = uri.getQueryParameter("code")
            val error = uri.getQueryParameter("error")
            
            when {
                // Success case: we have an authorization code
                code != null -> {
                    Log.d(TAG, "Authorization code received")
                    lifecycleScope.launch {
                        try {
                            // Send the code to the ViewModel to process
                            authViewModel.handleAuthCallback(code)
                            Log.d(TAG, "Code processed successfully")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing authorization code", e)
                        }
                    }
                }
                
                // Error case: user denied access or another error occurred
                error != null -> {
                    val errorDescription = uri.getQueryParameter("error_description") ?: "Unknown error"
                    Log.e(TAG, "Authorization error: $errorDescription")
                    // Handle error - you can add error handling to AuthViewModel if needed
                }
                
                // Invalid or incomplete URI
                else -> {
                    Log.e(TAG, "URI without code or error")
                    // Handle invalid response
                }
            }
        } else {
            Log.e(TAG, "Invalid or null URI: ${intent.data}")
            if (uri != null) {
                Log.e(TAG, "Scheme: ${uri.scheme}, Host: ${uri.host}")
            }
        }

        // Navigate back to the main activity
        startActivity(Intent(this, com.meninocoiso.bscm.MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        })

        // Always finish this activity to return to the main app
        finish()
    }
}
