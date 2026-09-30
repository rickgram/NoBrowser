package com.rickgram.NoBrowser

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.graphics.Color
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.view.KeyEvent
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.net.toUri

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var cursorController: WebCursorController
    private var pendingDownload: DownloadDetails? = null

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val download = pendingDownload
        pendingDownload = null

        if (granted && download != null) {
            enqueueDownload(download)
        } else if (!granted) {
            Toast.makeText(this, R.string.download_permission_denied, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        extendToolbarBehindStatusBar(toolbar)
        setSupportActionBar(toolbar)

        webView = findViewById(R.id.webview)
        cursorController = WebCursorController(
            webView = webView,
            overlay = findViewById(R.id.cursor_overlay)
        )
        configureWebView()
        configureBackNavigation()
        loadFromIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        loadFromIntent(intent)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return cursorController.handleKeyEvent(event) || super.dispatchKeyEvent(event)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_share -> {
            shareCurrentUrl()
            true
        }

        R.id.action_refresh -> {
            webView.reload()
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    private fun extendToolbarBehindStatusBar(toolbar: Toolbar) {
        val toolbarContentHeight = toolbar.layoutParams.height
        val initialLeftPadding = toolbar.paddingLeft
        val initialRightPadding = toolbar.paddingRight

        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val topInsets = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.layoutParams = view.layoutParams.apply {
                height = toolbarContentHeight + topInsets.top
            }
            view.updatePadding(
                left = initialLeftPadding + topInsets.left,
                top = topInsets.top,
                right = initialRightPadding + topInsets.right
            )
            insets
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = true
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                cursorController.onPageStarted()
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                supportActionBar?.title = url
                cursorController.installPageObserver()
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                if (!request.isForMainFrame || BrowserSecurity.isWebScheme(request.url.scheme)) {
                    return false
                }

                Toast.makeText(
                    this@MainActivity,
                    R.string.unsupported_url_scheme,
                    Toast.LENGTH_SHORT
                ).show()
                return true
            }
        }

        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            if (!BrowserSecurity.isWebScheme(url.toUri().scheme)) {
                Toast.makeText(this, R.string.unsupported_download, Toast.LENGTH_SHORT).show()
                return@setDownloadListener
            }

            val guessedName = URLUtil.guessFileName(url, contentDisposition, mimeType)
            val download = DownloadDetails(
                url = url,
                userAgent = userAgent,
                mimeType = mimeType,
                fileName = BrowserSecurity.sanitizeFileName(guessedName)
            )

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                pendingDownload = download
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                enqueueDownload(download)
            }
        }

        webView.requestFocus()
    }

    private fun configureBackNavigation() {
        onBackPressedDispatcher.addCallback(this) {
            if (cursorController.exitFieldIfEditing()) {
                return@addCallback
            }
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun loadFromIntent(intent: Intent) {
        val uri = intent.data
        if (intent.action == Intent.ACTION_VIEW && uri != null) {
            if (BrowserSecurity.isWebScheme(uri.scheme)) {
                webView.loadUrl(uri.toString())
            } else {
                Toast.makeText(this, R.string.unsupported_url_scheme, Toast.LENGTH_SHORT).show()
                loadHomePage()
            }
        } else {
            loadHomePage()
        }
    }

    private fun loadHomePage() {
        webView.loadUrl(DEFAULT_HOME_PAGE)
    }

    private fun enqueueDownload(download: DownloadDetails) {
        val request = DownloadManager.Request(download.url.toUri()).apply {
            setMimeType(download.mimeType)
            addRequestHeader("User-Agent", download.userAgent)
            CookieManager.getInstance().getCookie(download.url)?.let { cookie ->
                addRequestHeader("Cookie", cookie)
            }
            setDescription(getString(R.string.downloading_file))
            setTitle(download.fileName)
            setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, download.fileName)
        }

        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        Toast.makeText(this, R.string.download_started, Toast.LENGTH_LONG).show()
    }

    private fun shareCurrentUrl() {
        val currentUrl = webView.url ?: return
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, currentUrl)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_url_via)))
    }

    private data class DownloadDetails(
        val url: String,
        val userAgent: String,
        val mimeType: String,
        val fileName: String
    )

    private companion object {
        const val DEFAULT_HOME_PAGE = "https://altl.io/"
    }
}
