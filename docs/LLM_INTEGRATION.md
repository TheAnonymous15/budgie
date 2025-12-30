# Qwen 2.5B LLM Integration Guide for Budgie

## Overview

This document explains how to integrate a quantized Qwen 2.5B model into Budgie for advanced natural language understanding and generation.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    BUDGIE AI ARCHITECTURE                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌───────────────┐      ┌───────────────┐     ┌──────────────┐ │
│  │   User Input  │ ───► │  QwenLLMEngine │ ◄──│ Financial    │ │
│  │  (Any Lang)   │      │    (NLU/NLG)   │    │  Learner     │ │
│  └───────────────┘      └───────────────┘     │  (Data/ML)   │ │
│                               │               └──────────────┘ │
│                               │                     │          │
│                               ▼                     │          │
│                    ┌───────────────────┐            │          │
│                    │   Intelligent     │ ◄──────────┘          │
│                    │   Financial       │                       │
│                    │   Assistant       │                       │
│                    └───────────────────┘                       │
│                               │                                │
│                               ▼                                │
│                    ┌───────────────────┐                       │
│                    │   Response        │                       │
│                    │  (User's Lang)    │                       │
│                    └───────────────────┘                       │
└─────────────────────────────────────────────────────────────────┘
```

## Model Options

### Recommended: Qwen 2.5 1.5B Instruct (Quantized)

| Model | Size | Quality | Speed | Recommended For |
|-------|------|---------|-------|-----------------|
| Q4_K_M | ~1.0GB | Good | Fast | Production use |
| Q4_0 | ~0.9GB | Acceptable | Fastest | Low-end devices |
| Q8_0 | ~1.6GB | Best | Slower | High-end devices |

### Download Links (HuggingFace)

```
# Q4_K_M (Recommended)
https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF

# For smaller devices, use Q4_0
# For better quality, use Q8_0
```

## Integration Steps

### Step 1: Add Native Dependencies

Add llama.cpp Android bindings to your project:

```kotlin
// build.gradle.kts (app)
android {
    // ... existing config
    
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
    
    ndkVersion = "25.2.9519653"
}
```

### Step 2: Create CMakeLists.txt

```cmake
# app/src/main/cpp/CMakeLists.txt
cmake_minimum_required(VERSION 3.22.1)
project("budgie_llm")

# Add llama.cpp as a subdirectory
add_subdirectory(llama.cpp)

# Create the JNI library
add_library(llama SHARED
    llama_jni.cpp
)

target_link_libraries(llama
    llama
    log
)
```

### Step 3: Create JNI Bindings

```cpp
// app/src/main/cpp/llama_jni.cpp
#include <jni.h>
#include "llama.h"
#include <string>
#include <android/log.h>

#define TAG "LlamaJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_example_budgie_ai_QwenLLMEngine_loadModel(
    JNIEnv *env,
    jobject thiz,
    jstring model_path
) {
    const char *path = env->GetStringUTFChars(model_path, nullptr);
    
    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0; // CPU only for compatibility
    
    llama_model *model = llama_load_model_from_file(path, model_params);
    
    env->ReleaseStringUTFChars(model_path, path);
    
    if (!model) {
        LOGI("Failed to load model");
        return 0;
    }
    
    LOGI("Model loaded successfully");
    return reinterpret_cast<jlong>(model);
}

JNIEXPORT void JNICALL
Java_com_example_budgie_ai_QwenLLMEngine_unloadModel(
    JNIEnv *env,
    jobject thiz,
    jlong handle
) {
    auto *model = reinterpret_cast<llama_model *>(handle);
    if (model) {
        llama_free_model(model);
    }
}

// Additional JNI methods for inference...

} // extern "C"
```

### Step 4: Model Download & Management

```kotlin
// In your app, provide UI for model download
class ModelDownloadManager(private val context: Context) {
    
    private val modelUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf"
    
    suspend fun downloadModel(onProgress: (Float) -> Unit): Result<File> {
        val modelsDir = File(context.filesDir, "models")
        modelsDir.mkdirs()
        
        val modelFile = File(modelsDir, "qwen2.5-1.5b-instruct-q4_k_m.gguf")
        
        // Download logic with OkHttp
        // ...
        
        return Result.success(modelFile)
    }
}
```

### Step 5: Use in Chat

```kotlin
// In your ChatScreen or ViewModel
val assistant = IntelligentFinancialAssistant.getInstance(context)

fun sendMessage(userMessage: String) {
    viewModelScope.launch {
        assistant.processMessage(userMessage)
            .collect { response ->
                when (response) {
                    is AssistantResponse.Thinking -> showLoading()
                    is AssistantResponse.Streaming -> appendToken(response.token)
                    is AssistantResponse.Complete -> showResponse(response.response)
                    is AssistantResponse.Done -> hideLoading()
                    is AssistantResponse.Error -> showError(response.message)
                }
            }
    }
}
```

## Prompt Engineering

The system prompt is crucial for good responses:

```
<|im_start|>system
You are Budgie, an AI-powered personal finance advisor.

USER PROFILE:
- Name: {userName}
- Age: {userAge}

FINANCIAL STATUS:
- Monthly Income: ${monthlyIncome}
- Monthly Expenses: ${monthlyExpenses}
- Savings Rate: {savingsRate}%
- Active Loans: {loanCount}
- Financial Health: {healthScore}/100

GUIDELINES:
1. Use actual financial data in responses
2. Provide specific, actionable advice
3. Tailor language to user's age group
4. Never make up data
5. Be encouraging but honest
<|im_end|>
```

## Performance Optimization

### Memory Management

```kotlin
// Release model when app goes to background
override fun onStop() {
    super.onStop()
    if (!isChangingConfigurations) {
        llmEngine.release()
    }
}

// Reload when app returns
override fun onStart() {
    super.onStart()
    lifecycleScope.launch {
        llmEngine.initialize()
    }
}
```

### Inference Settings

```kotlin
// Optimal settings for mobile
data class InferenceParams(
    val contextSize: Int = 2048,      // Reduced for mobile
    val batchSize: Int = 512,
    val threads: Int = 4,             // Match CPU cores
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val maxTokens: Int = 512
)
```

## Fallback Strategy

When LLM is unavailable (not downloaded, loading, or error):

```kotlin
if (!llmEngine.isModelAvailable()) {
    // Use rule-based response from FinancialLearner
    val response = learner.answerQuestion(query)
    return formatRuleBasedResponse(response)
}
```

## Testing

### Unit Tests

```kotlin
@Test
fun testIntentDetection() {
    val assistant = IntelligentFinancialAssistant.getInstance(context)
    
    assertEquals(
        QueryIntent.FINANCIAL_STATUS,
        assistant.detectIntent("How am I doing financially?")
    )
}
```

### Integration Tests

```kotlin
@Test
fun testFullConversation() = runTest {
    val assistant = IntelligentFinancialAssistant.getInstance(context)
    
    assistant.processMessage("Hello")
        .toList()
        .also { responses ->
            assertTrue(responses.any { it is AssistantResponse.Complete })
        }
}
```

## Security Considerations

1. **Model files**: Store in app's private directory
2. **User data**: Never send to external servers
3. **Prompts**: Sanitize user input
4. **Memory**: Clear sensitive data after inference

## Troubleshooting

### Common Issues

1. **Model loading fails**: Check file path and permissions
2. **Out of memory**: Use smaller quantization (Q4_0)
3. **Slow inference**: Reduce context size and batch size
4. **Wrong language**: Ensure prompt includes language instruction

### Debug Logging

```kotlin
// Enable verbose logging
llama_log_set { level, message, _ ->
    when (level) {
        LLAMA_LOG_ERROR -> Log.e("Llama", message)
        LLAMA_LOG_WARN -> Log.w("Llama", message)
        LLAMA_LOG_INFO -> Log.i("Llama", message)
    }
}
```

## Resources

- [llama.cpp](https://github.com/ggerganov/llama.cpp)
- [Qwen Models](https://huggingface.co/Qwen)
- [GGUF Format](https://github.com/ggerganov/ggml/blob/master/docs/gguf.md)
- [Android NDK](https://developer.android.com/ndk)

## License

The Qwen 2.5 model is released under the Qwen License. Check the license terms before commercial use.

