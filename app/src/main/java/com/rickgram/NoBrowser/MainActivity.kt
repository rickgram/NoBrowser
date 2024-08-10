package com.rickgram.NoBrowser

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.app.DownloadManager
import android.os.Environment
import android.net.Uri
import android.widget.Toast
import android.content.Context


class MainActivity : AppCompatActivity() {
    private lateinit var myWebView: WebView
    private lateinit var urlTextView: TextView
    private lateinit var toolbarTitle: TextView

    private val REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check for permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_CODE
                )
            }
        }

        // Set up the Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        //Initialize WebView
        myWebView = findViewById(R.id.webview)

        // Enable JavaScript if needed
        myWebView.settings.javaScriptEnabled = true

        // Set up the WebViewClient to update the URL in the Toolbar
        myWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                // Update Toolbar title with the current URL
                supportActionBar?.title = url
            }
        }

        // Set up the DownloadListener
        myWebView.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            val request = DownloadManager.Request(Uri.parse(url))

            // Setting the download file type
            request.setMimeType(mimeType)
            // Tells the system to scan the downloaded file when completed
            request.allowScanningByMediaScanner()
            request.addRequestHeader("User-Agent", userAgent)
            request.setDescription("Downloading file...")
            request.setTitle(contentDisposition)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                contentDisposition.substringAfter("filename=").replace("\"", "")
            )

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(applicationContext, "Downloading File...", Toast.LENGTH_LONG).show()
        }


        // Intent handling for receiving URLs from other apps
        val intent = intent
        val action = intent.action
        val data = intent.data
        if (action == Intent.ACTION_VIEW && data != null) {
            myWebView.loadUrl(data.toString())
        } else {
            // Load a default URL if no intent data is received
            myWebView.loadUrl("https://altl.io/")
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share -> {
                shareCurrentUrl()
                true
            }
            R.id.action_refresh -> {
                refreshPage()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun shareCurrentUrl() {
        val currentUrl = myWebView.url
        if (currentUrl != null) {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, currentUrl)
                type = "text/plain"
            }
            startActivity(Intent.createChooser(shareIntent, "Share URL via"))
        }
    }

    private fun refreshPage() {
        myWebView.reload() // Reloads the current page in the WebView
    }


}
