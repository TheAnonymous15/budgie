package com.example.budgie.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

// ═══════════════════════════════════════════════════════════════════════════════
// MODEL DOWNLOAD MANAGER
// ═══════════════════════════════════════════════════════════════════════════════
//
// Handles downloading the GGUF model from a remote server to the app's internal
// storage where the llama.cpp native library can access it via fopen/mmap.
//
// Architecture:
//
// ┌─────────────────────────────────────────────────────────────────────────┐
// │                 Android App (Kotlin / Compose)                          │
// │                            │                                            │
// │                            │ JNI call                                   │
// │                            ▼                                            │
// │          libllama_jni.so ←──────────── Kotlin bindings                  │
// │                            │                                            │
// │                            ▼                                            │
// │          libllama.so / libggml.so                                       │
// │                            │                                            │
// │                            │ fopen("model.gguf")                        │
// │                            │ mmap / read                                │
// │                            ▼                                            │
// │           GGUF model loaded into memory                                 │
// │                                                                         │
// │   Model stored at:                                                      │
// │   /data/data/com.example.budgie/files/models/*.gguf                     │
// └─────────────────────────────────────────────────────────────────────────┘
//
// Why internal storage?
// - Readable by JNI/native code
// - mmap-compatible
// - No scoped-storage issues
// - Survives app restarts
//
// Flow design:
// - StateFlow exposes current state to UI
// - channelFlow is required for multi-coroutine emission
// - trySend() avoids Flow invariant violations
//
// ═══════════════════════════════════════════════════════════════════════════════

class ModelDownloadManager private constructor(
private val context: Context
) {

companion object {
private const val TAG = "ModelDownloadManager"

// Model configuration
private const val MODEL_FILENAME =
"qwen2.5-1.5b-instruct-q4_k_m.gguf"
private const val MODEL_DIR = "models"

// Default model URL (replace with your server)
private const val DEFAULT_MODEL_URL =
"https://your-server.com/models/qwen2.5-1.5b-instruct-q4_k_m.gguf"

// Networking
private const val BUFFER_SIZE = 8 * 1024
private const val CONNECTION_TIMEOUT = 30_000
private const val READ_TIMEOUT = 60_000

@Volatile
private var instance: ModelDownloadManager? = null

fun getInstance(context: Context): ModelDownloadManager =
instance ?: synchronized(this) {
instance ?: ModelDownloadManager(
context.applicationContext
).also { instance = it }
}
}

// ═══════════════════════════════════════════════════════════════════════
// State exposed to UI (Compose / ViewModel)
// ═══════════════════════════════════════════════════════════════════════

private val _downloadState =
MutableStateFlow<DownloadState>(DownloadState.Idle)
val downloadState: StateFlow<DownloadState> =
_downloadState.asStateFlow()

private val _downloadProgress = MutableStateFlow(0f)
val downloadProgress: StateFlow<Float> =
_downloadProgress.asStateFlow()

private val _downloadedBytes = MutableStateFlow(0L)
val downloadedBytes: StateFlow<Long> =
_downloadedBytes.asStateFlow()

private val _totalBytes = MutableStateFlow(0L)
val totalBytes: StateFlow<Long> =
_totalBytes.asStateFlow()

// Dedicated IO scope for downloads
private val scope =
CoroutineScope(Dispatchers.IO + SupervisorJob())

private var downloadJob: Job? = null
private var modelUrl: String = DEFAULT_MODEL_URL

// ═══════════════════════════════════════════════════════════════════════
// File system helpers
// ═══════════════════════════════════════════════════════════════════════

private fun modelsDir(): File =
File(context.filesDir, MODEL_DIR).apply {
if (!exists()) mkdirs()
}

private fun modelFile(): File =
File(modelsDir(), MODEL_FILENAME)

private fun tempFile(): File =
File(modelsDir(), "$MODEL_FILENAME.tmp")

/**
 * Path passed to native llama.cpp loader (fopen/mmap)
*/
fun getModelFilePath(): String =
modelFile().absolutePath

// ═══════════════════════════════════════════════════════════════════════
// Public API
// ═══════════════════════════════════════════════════════════════════════

fun setModelUrl(url: String) {
modelUrl = url
}

fun isModelDownloaded(): Boolean =
modelFile().exists() && modelFile().length() > 0

fun deleteModel() {
modelFile().delete()
tempFile().delete()
resetState()
}

fun pauseDownload() {
downloadJob?.cancel()
downloadJob = null
_downloadState.value =
DownloadState.Paused(_downloadProgress.value)
}

fun cancelDownload() {
downloadJob?.cancel()
downloadJob = null
tempFile().delete()
resetState()
}

// ═══════════════════════════════════════════════════════════════════════
// Download logic
// ═══════════════════════════════════════════════════════════════════════
//
// channelFlow is REQUIRED here because:
// - download runs in a child coroutine
// - progress updates are emitted asynchronously
// - flow{} + emit() would crash with invariant violations
//
fun startDownload(
customUrl: String? = null
): Flow<DownloadState> = channelFlow {

val url = customUrl ?: modelUrl

// Already downloaded → emit immediately
if (isModelDownloaded()) {
val completed =
DownloadState.Completed(modelFile())
_downloadState.value = completed
trySend(completed)
return@channelFlow
}

// Prevent duplicate downloads
if (_downloadState.value.isInProgress()) {
trySend(_downloadState.value)
return@channelFlow
}

downloadJob = launch {
try {
_downloadState.value = DownloadState.Preparing
trySend(DownloadState.Preparing)

val tmp = tempFile()
val out = modelFile()
val startByte =
if (tmp.exists()) tmp.length() else 0L

Log.d(TAG, "Resuming from byte $startByte")

val conn =
(URL(url).openConnection()
as HttpURLConnection).apply {
connectTimeout = CONNECTION_TIMEOUT
readTimeout = READ_TIMEOUT
requestMethod = "GET"
if (startByte > 0) {
setRequestProperty(
"Range",
"bytes=$startByte-"
)
}
}

conn.connect()

if (conn.responseCode !in listOf(200, 206)) {
throw IOException(
"HTTP ${conn.responseCode}"
)
}

val totalSize =
if (conn.responseCode == 206) {
conn.getHeaderField("Content-Range")
?.substringAfter("/")
?.toLongOrNull()
?: (startByte + conn.contentLengthLong)
} else conn.contentLengthLong

_totalBytes.value = totalSize
_downloadedBytes.value = startByte

conn.inputStream.use { input ->
FileOutputStream(
tmp,
startByte > 0
).use { output ->

val buffer = ByteArray(BUFFER_SIZE)
var bytesRead: Int
var downloaded = startByte

while (input.read(buffer)
.also { bytesRead = it } != -1
) {
ensureActive()

output.write(
buffer,
0,
bytesRead
)
downloaded += bytesRead

val progress =
if (totalSize > 0)
downloaded.toFloat() / totalSize
else 0f

_downloadedBytes.value = downloaded
_downloadProgress.value = progress

val state =
DownloadState.Downloading(progress)
_downloadState.value = state
trySend(state)
}
}
}

// Atomic replace
if (out.exists()) out.delete()
if (!tmp.renameTo(out)) {
tmp.copyTo(out, overwrite = true)
tmp.delete()
}

val completed =
DownloadState.Completed(out)
_downloadProgress.value = 1f
_downloadState.value = completed
trySend(completed)

Log.d(TAG, "Download complete")

} catch (e: CancellationException) {
val paused =
DownloadState.Paused(_downloadProgress.value)
_downloadState.value = paused
trySend(paused)

} catch (e: Exception) {
Log.e(TAG, "Download failed", e)
val error =
DownloadState.Error(
e.message ?: "Unknown error"
)
_downloadState.value = error
trySend(error)
}
}

// Cancels download if collector disappears
awaitClose {
downloadJob?.cancel()
}
}

// ═══════════════════════════════════════════════════════════════════════
// Utilities
// ═══════════════════════════════════════════════════════════════════════

private fun resetState() {
_downloadProgress.value = 0f
_downloadedBytes.value = 0L
_totalBytes.value = 0L
_downloadState.value = DownloadState.Idle
}

/**
 * Resume a paused download
 */
fun resumeDownload(): Flow<DownloadState> = startDownload()

/**
 * Format bytes to human-readable string
 */
fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
    bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
}

/**
 * Get estimated time remaining
 */
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

fun release() {
downloadJob?.cancel()
scope.cancel()
}
}

/**
 * Download state machine
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
