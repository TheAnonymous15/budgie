package com.example.budgie.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "ModelDownloadManager"

/**
 * Download state machine for model files
 */
sealed class DownloadState {
    object Idle : DownloadState()
    object Preparing : DownloadState()
    data class Downloading(val progress: Float) : DownloadState()
    data class Paused(val progress: Float) : DownloadState()
    data class Completed(val file: File) : DownloadState()
    data class Error(val message: String) : DownloadState()

    fun isInProgress(): Boolean =
        this is Downloading || this is Preparing
}

/**
 * ModelDownloadManager - Handles downloading of AI models from Hugging Face
 *
 * Model: Qwen2.5-3B-Instruct-Q4_K_M.gguf (~2GB)
 * Source: https://huggingface.co/bartowski/Qwen2.5-3B-Instruct-GGUF
 */
class ModelDownloadManager private constructor(private val context: Context) {

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _downloadedBytes = MutableStateFlow(0L)
    val downloadedBytes: StateFlow<Long> = _downloadedBytes.asStateFlow()

    private val _totalBytes = MutableStateFlow(0L)
    val totalBytes: StateFlow<Long> = _totalBytes.asStateFlow()

    private var modelUrl = DEFAULT_MODEL_URL
    private var downloadJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        // Qwen2.5-3B-Instruct Q4_K_M quantized model from Hugging Face
        const val DEFAULT_MODEL_URL =
            "https://huggingface.co/bartowski/Qwen2.5-3B-Instruct-GGUF/resolve/main/Qwen2.5-3B-Instruct-Q4_K_M.gguf"

        const val MODEL_FILENAME = "qwen2.5-3b-instruct-q4_k_m.gguf"
        const val EXPECTED_SIZE_MB = 2048 // ~2GB

        @Volatile
        private var INSTANCE: ModelDownloadManager? = null

        fun getInstance(context: Context): ModelDownloadManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ModelDownloadManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private fun modelsDir(): File {
        val dir = File(context.filesDir, "models")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun modelFile(): File = File(modelsDir(), MODEL_FILENAME)
    private fun tempFile(): File = File(modelsDir(), "$MODEL_FILENAME.tmp")

    fun getModelFilePath(): String = modelFile().absolutePath

    fun setModelUrl(url: String) {
        modelUrl = url
    }

    fun isModelDownloaded(): Boolean {
        val file = modelFile()
        return file.exists() && file.length() > 100_000_000 // At least 100MB
    }

    fun getModelSize(): Long = modelFile().length()

    fun deleteModel() {
        modelFile().delete()
        tempFile().delete()
        resetState()
        Log.d(TAG, "Model deleted")
    }

    fun pauseDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _downloadState.value = DownloadState.Paused(_downloadProgress.value)
        Log.d(TAG, "Download paused at ${_downloadProgress.value}%")
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        tempFile().delete()
        resetState()
        Log.d(TAG, "Download cancelled")
    }

    /**
     * Start downloading the model
     */
    fun startDownload(customUrl: String? = null): Flow<DownloadState> = flow {
        val url = customUrl ?: modelUrl

        emit(DownloadState.Preparing)
        _downloadState.value = DownloadState.Preparing
        Log.d(TAG, "Starting download from: $url")

        try {
            // Check if already downloaded
            if (isModelDownloaded()) {
                Log.d(TAG, "Model already downloaded")
                emit(DownloadState.Completed(modelFile()))
                _downloadState.value = DownloadState.Completed(modelFile())
                return@flow
            }

            // Start download
            downloadJob = scope.launch {
                downloadFile(url)
            }
            downloadJob?.join()

            if (modelFile().exists() && modelFile().length() > 100_000_000) {
                emit(DownloadState.Completed(modelFile()))
                _downloadState.value = DownloadState.Completed(modelFile())
                Log.d(TAG, "Download completed: ${modelFile().length()} bytes")
            } else {
                throw Exception("Download incomplete or file corrupted")
            }

        } catch (e: CancellationException) {
            Log.d(TAG, "Download cancelled")
            emit(DownloadState.Paused(_downloadProgress.value))
        } catch (e: Exception) {
            Log.e(TAG, "Download error: ${e.message}", e)
            emit(DownloadState.Error(e.message ?: "Unknown error"))
            _downloadState.value = DownloadState.Error(e.message ?: "Unknown error")
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun downloadFile(urlString: String) = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        var outputStream: FileOutputStream? = null

        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            connection.setRequestProperty("User-Agent", "Budgie-App/1.0")

            // Check for resume capability
            val tempFileSize = tempFile().length()
            if (tempFileSize > 0) {
                connection.setRequestProperty("Range", "bytes=$tempFileSize-")
                Log.d(TAG, "Resuming from byte $tempFileSize")
            }

            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw Exception("HTTP error: $responseCode")
            }

            val contentLength = connection.contentLengthLong
            val totalSize = if (tempFileSize > 0) tempFileSize + contentLength else contentLength
            _totalBytes.value = totalSize
            _downloadedBytes.value = tempFileSize

            Log.d(TAG, "Total size: ${formatBytes(totalSize)}")

            // Open output stream (append if resuming)
            outputStream = FileOutputStream(tempFile(), tempFileSize > 0)
            val inputStream = connection.inputStream

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var downloadedSoFar = tempFileSize

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (!isActive) break // Check for cancellation

                outputStream.write(buffer, 0, bytesRead)
                downloadedSoFar += bytesRead
                _downloadedBytes.value = downloadedSoFar

                val progress = if (totalSize > 0) (downloadedSoFar.toFloat() / totalSize) else 0f
                _downloadProgress.value = progress
                _downloadState.value = DownloadState.Downloading(progress)
            }

            outputStream.flush()
            outputStream.close()

            // Rename temp file to final file
            if (tempFile().length() > 100_000_000) {
                tempFile().renameTo(modelFile())
                Log.d(TAG, "Download complete, file saved to ${modelFile().absolutePath}")
            }

        } finally {
            outputStream?.close()
            connection?.disconnect()
        }
    }

    fun resumeDownload(): Flow<DownloadState> = startDownload()

    fun formatBytes(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }

    fun getEstimatedTimeRemaining(downloadSpeed: Long): String {
        if (downloadSpeed <= 0) return "Calculating..."
        val remainingBytes = _totalBytes.value - _downloadedBytes.value
        val secondsRemaining = remainingBytes / downloadSpeed
        return when {
            secondsRemaining < 60 -> "${secondsRemaining}s"
            secondsRemaining < 3600 -> "${secondsRemaining / 60}m ${secondsRemaining % 60}s"
            else -> "${secondsRemaining / 3600}h ${(secondsRemaining % 3600) / 60}m"
        }
    }

    private fun resetState() {
        _downloadProgress.value = 0f
        _downloadedBytes.value = 0L
        _totalBytes.value = 0L
        _downloadState.value = DownloadState.Idle
    }

    fun release() {
        downloadJob?.cancel()
        scope.cancel()
    }
}

