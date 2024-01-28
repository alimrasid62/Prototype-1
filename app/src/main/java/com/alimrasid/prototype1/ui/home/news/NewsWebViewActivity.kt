package com.alimrasid.prototype1.ui.home.news

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.alimrasid.prototype1.R

class NewsWebViewActivity : AppCompatActivity() {

    companion object {
        const val URL_EXTRA = "news_url"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news_web_view)

        val newsUrl = intent.getStringExtra(URL_EXTRA)

        val progressBar: ProgressBar = findViewById(R.id.progressBar)

        val webView: WebView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return super.shouldOverrideUrlLoading(view, url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
            }
        }
        if (newsUrl != null) {
            webView.loadUrl(newsUrl)
        }
    }
}
