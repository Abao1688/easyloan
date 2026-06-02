package com.easyloan.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var appUrl = "http://192.168.31.1:2688"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webView)

        // 读取配置的URL（可从 intent 或 SharedPreferences 读取）
        val url = intent.getStringExtra("url") ?: appUrl
        appUrl = url

        setupWebView()
        checkLocationPermission()

        webView.loadUrl(appUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false

        // 夜间模式跟随系统
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_AUTO)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // 页面加载完成后注入定位JS接口
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    updateLocation()
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                request.grant(request.resources)
            }
        }

        // JS接口：供前端页面调用获取位置
        webView.addJavascriptInterface(LocationInterface(this), "AndroidLocation")
    }

    private fun checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (fine != PackageManager.PERMISSION_GRANTED ||
                coarse != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    LOCATION_PERMISSION_REQUEST
                )
            }
        }
    }

    private fun updateLocation() {
        try {
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val gps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                val network = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                val loc = gps ?: network
                if (loc != null) {
                    webView.evaluateJavascript(
                        "window._lastLocation = {lat:${loc.latitude},lng:${loc.longitude},acc:${loc.accuracy}}; if(typeof onLocationUpdate === 'function') onLocationUpdate(window._lastLocation);",
                        null
                    )
                }
            }
        } catch (_: Exception) {}
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateLocation()
            }
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
    }
}

class LocationInterface(private val activity: MainActivity) {
    @JavascriptInterface
    fun getLatitude(): Double {
        return try {
            val lm = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (ActivityCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val gps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                val network = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                (gps ?: network)?.latitude ?: 0.0
            } else 0.0
        } catch (_: Exception) { 0.0 }
    }

    @JavascriptInterface
    fun getLongitude(): Double {
        return try {
            val lm = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (ActivityCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val gps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                val network = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                (gps ?: network)?.longitude ?: 0.0
            } else 0.0
        } catch (_: Exception) { 0.0 }
    }

    @JavascriptInterface
    fun getAccuracy(): Float {
        return try {
            val lm = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (ActivityCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val gps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                val network = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                (gps ?: network)?.accuracy ?: 0f
            } else 0f
        } catch (_: Exception) { 0f }
    }
}
