package com.aries.webview.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.aries.common.constants.RouterPaths
import com.aries.common.util.StatusBarUtil
import com.aries.webview.bridge.Callback
import com.aries.webview.bridge.ConsolePipe
import com.aries.webview.bridge.Handler
import com.aries.webview.bridge.WebViewJavascriptBridge
import com.aries.webview.databinding.ActivityWebviewBinding
import java.lang.reflect.InvocationTargetException

@Route(path = RouterPaths.WebView_ACTIVITY)
class WebViewActivity: AppCompatActivity() {
    private lateinit var binding: ActivityWebviewBinding

    @Autowired
    @JvmField
    var isDarkTheme: Boolean = false

    @Autowired
    @JvmField
    var url: String = ""

    private var bridge: WebViewJavascriptBridge? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ARouter.getInstance().inject(this)
        binding = ActivityWebviewBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        initView()
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            finish()
        }
    }

    override fun onDestroy() {
        binding.webView.destroy()
        super.onDestroy()
    }

    private fun initView() {
        bridge = WebViewJavascriptBridge(this, binding.webView )
        setAllowUniversalAccessFromFileURLs(binding.webView)
        StatusBarUtil.setBarTextModal(this, isDarkTheme)
        binding.webView.settings.run {
            true.also { javaScriptEnabled = it }
            useWideViewPort = true
            loadWithOverviewMode = true
            displayZoomControls = false
            javaScriptCanOpenWindowsAutomatically = false
            loadsImagesAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            defaultTextEncodingName = "utf-8"
        }

//        webView.loadUrl(url)
        binding.webView.loadUrl("file:///android_asset/Demo.html")

        binding.webView.webViewClient = object: WebViewClient(){
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                println("shouldOverrideUrlLoading")
                return false
            }
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                println("onPageStarted")
                bridge?.injectJavascript()
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                println("onPageFinished")
            }
        }

        bridge?.consolePipe = object : ConsolePipe {
            override fun post(string : String){
                println("Next line is javascript console.log->>>")
                println(string)
            }
        }
        bridge?.register("DeviceLoadJavascriptSuccess",object : Handler {
            override fun handler(map: HashMap<String, Any>?, callback: Callback) {
                println("Next line is javascript data->>>")
                println(map)
                val result = HashMap<String, Any>()
                result["result"] = "Android"
                callback.call(result)
            }
        })
    }

    private fun setAllowUniversalAccessFromFileURLs(webView: WebView) {
        try {
            val clazz: Class<*> = webView.settings.javaClass
            val method = clazz.getMethod(
                "setAllowUniversalAccessFromFileURLs", Boolean::class.javaPrimitiveType
            )
            method.invoke(webView.settings, true)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }
    }
}