package com.rickgram.NoBrowser

import android.graphics.Color
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebChromeClient
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar


class MainActivity : AppCompatActivity() {
    private lateinit var myWebView: WebView
    private lateinit var urlTextView: TextView
    private lateinit var toolbarTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
