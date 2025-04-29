package com.example.ui.common.components

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun MeasurementsWebViewScreen(url: String) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                    domStorageEnabled = false
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        return true
                    }
                }

                loadDataWithBaseURL("https://localhost/", url, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://localhost/", url, "text/html", "UTF-8", null)
        }
    )
}