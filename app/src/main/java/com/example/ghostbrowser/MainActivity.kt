package com.example.nobrowser

import android.os.Bundle
import android.webkit.WebView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent // Import Intent class

class MainActivity : AppCompatActivity() {
    private lateinit var myWebView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        myWebView = findViewById(R.id.webview)
        myWebView.settings.javaScriptEnabled = true // Enable JavaScript if needed
        // Set up other WebView settings here

        // Intent handling for receiving URLs from other apps
        val intent = intent
        val action = intent.action
        val data = intent.data
        if (action == Intent.ACTION_VIEW && data != null) {
            myWebView.loadUrl(data.toString())
        }
    }
}
