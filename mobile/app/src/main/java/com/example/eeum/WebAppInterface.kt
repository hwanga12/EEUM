package com.example.eeum

import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class WebAppInterface(
    private val activity: ComponentActivity,
    private val webView: WebView,
    private val healthManager: SamsungHealthManager
) {
    // Vue에서 window.Android.fetchHeartRate()로 호출 가능
    @JavascriptInterface
    fun fetchHeartRate() {
        activity.lifecycleScope.launch {
            // 권한 확인 후 데이터 조회
            if (healthManager.checkAndRequestPermissions(activity)) {
                val data = healthManager.getLatestHeartRate()

                // 조회된 데이터를 다시 웹뷰(JS)로 전달
                webView.post {
                    webView.evaluateJavascript("javascript:onReceiveHeartData($data)", null)
                }
            }
        }
    }
}