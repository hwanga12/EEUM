package com.example.eeum

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.eeum.ui.theme.EeumTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EeumTheme {
                WebViewScreen()
            }
        }
    }
}

@Composable
fun WebViewScreen() {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                // 🔥 WebView 설정 강화 (여기가 핵심!)
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true   // 로컬 스토리지 사용 (Vue 필수)

                    // 👇 이 설정들이 있어야 'file://' 경로에서 모듈을 불러올 수 있음
                    allowFileAccess = true
                    allowContentAccess = true
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = true
                }

                webViewClient = WebViewClient()

                // 웹뷰 로드
                loadUrl("file:///android_asset/index.html")
            }
        }
    )
}
