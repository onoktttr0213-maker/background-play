package com.bgplay.dualwebplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Hosts two independent WebViews (YouTube on top, Spotify on the bottom) so
 * both can play audio at the same time. Neither this Activity nor either
 * WebView is ever told to pause on its own: onPause()/onStop() are
 * deliberately left at their defaults (no webView.onPause()/pauseTimers()
 * calls), so JS timers and media keep running when the screen turns off or
 * the app goes to the background. [MediaService] keeps the process alive
 * and unthrottled while that happens.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var youTubeWebView: WebView
    private lateinit var spotifyWebView: WebView

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* optional UX only */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        youTubeWebView = findViewById(R.id.webViewYouTube)
        spotifyWebView = findViewById(R.id.webViewSpotify)

        setupWebView(youTubeWebView, MOBILE_USER_AGENT)
        setupWebView(spotifyWebView, DESKTOP_USER_AGENT)

        youTubeWebView.loadUrl(YOUTUBE_URL)
        spotifyWebView.loadUrl(SPOTIFY_URL)

        requestNotificationPermissionIfNeeded()
        startMediaService()
    }

    private fun setupWebView(webView: WebView, userAgent: String) {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.databaseEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        settings.userAgentString = userAgent

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun startMediaService() {
        ContextCompat.startForegroundService(this, Intent(this, MediaService::class.java))
    }

    override fun onDestroy() {
        youTubeWebView.destroy()
        spotifyWebView.destroy()
        stopService(Intent(this, MediaService::class.java))
        super.onDestroy()
    }

    companion object {
        private const val YOUTUBE_URL = "https://m.youtube.com"
        private const val SPOTIFY_URL = "https://open.spotify.com"

        // YouTube's mobile site expects a mobile UA to serve m.youtube.com properly.
        private const val MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0.0.0 Mobile Safari/537.36"

        // open.spotify.com only serves its full Web Player (not the limited
        // "open in app" landing page) to a desktop-class UA.
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0.0.0 Safari/537.36"
    }
}
