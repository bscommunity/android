package com.meninocoiso.bscm.presentation.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.meninocoiso.bscm.MainActivity

private const val TAG = "OAuthRedirect"

/**
 * This activity handles OAuth redirects from Custom Chrome Tabs and forwards the result
 * back to MainActivity via Intent extras.
 */
class OAuthRedirectActivity : ComponentActivity() {

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
        val uri = intent?.data
        var code: String? = null
        var error: String? = null

        if (uri != null && uri.scheme == "bscm" && uri.host == "auth") {
            Log.d(TAG, "Valid URI received: $uri")

            code = uri.getQueryParameter("code")
            error = uri.getQueryParameter("error")
                ?: uri.getQueryParameter("error_description")
        } else {
            Log.e(TAG, "Invalid or null URI: ${intent?.data}")
            if (uri != null) {
                Log.e(TAG, "Scheme: ${uri.scheme}, Host: ${uri.host}")
                error = "Invalid redirect URI"
            }
        }

        // Forward the result back to MainActivity
        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            code?.let { putExtra("code", it) }
            error?.let { putExtra("error", it) }
        })

        finish()
    }
}
