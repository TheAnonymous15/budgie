package com.example.budgie.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.budgie.ai.DownloadState
import com.example.budgie.ai.ModelDownloadManager

/**
 * ViewModel for managing AI model downloads
 */
class ModelDownloadViewModel(context: Context) : ViewModel() {

    private val downloadManager = ModelDownloadManager.getInstance(context)

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _downloadedBytes = MutableStateFlow(0L)
    val downloadedBytes: StateFlow<Long> = _downloadedBytes.asStateFlow()

    private val _totalBytes = MutableStateFlow(0L)
    val totalBytes: StateFlow<Long> = _totalBytes.asStateFlow()

    private val _downloadSpeed = MutableStateFlow(0L)
    val downloadSpeed: StateFlow<Long> = _downloadSpeed.asStateFlow()

    private val _isModelReady = MutableStateFlow(downloadManager.isModelDownloaded())
    val isModelReady: StateFlow<Boolean> = _isModelReady.asStateFlow()

    fun checkAndStartDownload(url: String? = null) {
        // Stub for now
        _isModelReady.value = true
    }

    fun pauseDownload() {
        downloadManager.pauseDownload()
    }

    fun resumeDownload() {
        // Stub
    }

    fun cancelDownload() {
        downloadManager.cancelDownload()
    }

    fun formatBytes(bytes: Long): String = downloadManager.formatBytes(bytes)

    fun getEstimatedTimeRemaining(): String = downloadManager.getEstimatedTimeRemaining(_downloadSpeed.value)

    override fun onCleared() {
        super.onCleared()
        downloadManager.release()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ModelDownloadViewModel::class.java)) {
                return ModelDownloadViewModel(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

