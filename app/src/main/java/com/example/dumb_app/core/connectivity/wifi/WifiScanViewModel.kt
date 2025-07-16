package com.example.dumb_app.core.connectivity.wifi

import android.app.Application
import android.net.wifi.ScanResult
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * WifiScanViewModel
 * - scanResults: StateFlow，外部可 collectAsState() 监听
 * - startScan(): UI 调用触发一次扫描
 * - onCleared(): 停止扫描，释放资源
 */
class WifiScanViewModel(application: Application) : AndroidViewModel(application) {

    // 用 Application context 初始化 WifiScanner
    private val scanner = WifiScanner(application)

    // 可变的内部流
    private val _scanResults = MutableStateFlow<List<ScanResult>>(emptyList())
    // 只读暴露给 UI
    val scanResults: StateFlow<List<ScanResult>> = _scanResults

    /** UI 调用：开始一次扫描 */
    fun startScan() {
        scanner.startScan { results ->
            // 切到主协程更新 StateFlow
            viewModelScope.launch {
                _scanResults.value = results
            }
        }
    }

    /** ViewModel 销毁时，停止扫描释放回调 */
    override fun onCleared() {
        super.onCleared()
        scanner.stopScan()
    }
}
