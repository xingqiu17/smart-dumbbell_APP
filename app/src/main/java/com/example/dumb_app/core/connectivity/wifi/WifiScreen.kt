package com.example.dumb_app.core.connectivity.wifi

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * 只在 Android 12+ 上工作，封装 Wi-Fi 扫描：
 * 1. 检查 NEARBY_WIFI_DEVICES 权限
 * 2. 注册 ScanResultsCallback
 * 3. 触发 startScan()
 * 4. 在回调里再次检查权限，才调用 wifiManager.scanResults
 */
class WifiScanner(private val context: Context) {

    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private var onResults: ((List<ScanResult>) -> Unit)? = null
    private var scanCallback: WifiManager.ScanResultsCallback? = null

    /**
     * 开始扫描，结果通过 resultListener 回调
     */
    @SuppressLint("MissingPermission") // 我们手动做了权限检查
    fun startScan(resultListener: (List<ScanResult>) -> Unit) {
        onResults = resultListener

        // 1. 只支持 Android 12+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            resultListener(emptyList())
            return
        }

        // 2. 权限检查：NEARBY_WIFI_DEVICES
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            resultListener(emptyList())
            return
        }

        // 3. 注册新的扫描回调
        scanCallback = object : WifiManager.ScanResultsCallback() {
            override fun onScanResultsAvailable() {
                // 再次检查，才获取 scanResults
                val results = if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.NEARBY_WIFI_DEVICES
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    wifiManager.scanResults
                } else {
                    emptyList()
                }
                onResults?.invoke(results)
            }
        }
        wifiManager.registerScanResultsCallback(context.mainExecutor, scanCallback!!)

        // 4. 触发底层扫描，catch 掉可能的 SecurityException
        try {
            wifiManager.startScan()
        } catch (e: SecurityException) {
            e.printStackTrace()
            onResults?.invoke(emptyList())
        }
    }

    /**
     * 停止扫描并清理
     */
    fun stopScan() {
        scanCallback?.let {
            wifiManager.unregisterScanResultsCallback(it)
        }
        scanCallback = null
        onResults = null
    }
}
