package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.GenerationConfig
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.data.AppDatabase
import com.example.data.CodeReview
import com.example.data.CodeReviewRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReviewScores(
    val codeQuality: Int = 0,
    val security: Int = 0,
    val performance: Int = 0,
    val maintainability: Int = 0,
    val readiness: Int = 0
)

class ReviewViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = CodeReviewRepository(database.codeReviewDao())

    val historyList: StateFlow<List<CodeReview>> = repository.allReviews
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadingProgress = MutableStateFlow("")
    val loadingProgress: StateFlow<String> = _loadingProgress.asStateFlow()

    private val _currentReport = MutableStateFlow<CodeReview?>(null)
    val currentReport: StateFlow<CodeReview?> = _currentReport.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun selectHistoryReview(review: CodeReview) {
        _currentReport.value = review
        _error.value = null
    }

    fun deleteReview(review: CodeReview) {
        viewModelScope.launch {
            repository.deleteReviewById(review.id)
            if (_currentReport.value?.id == review.id) {
                _currentReport.value = null
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllReviews()
            _currentReport.value = null
        }
    }

    fun resetReport() {
        _currentReport.value = null
        _error.value = null
    }

    fun executeCodeReview(
        language: String,
        projectType: String,
        reviewMode: String,
        codeSnippet: String
    ) {
        if (codeSnippet.trim().isEmpty()) {
            _error.value = "Please enter some code to review."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _currentReport.value = null

            try {
                // High-fidelity progressive engineering steps
                val steps = listOf(
                    "Initializing Elite Review Deck...",
                    "Hashing abstract syntax tree for $language...",
                    "Analyzing patterns in $projectType configuration...",
                    "Aligning criteria with $reviewMode standards...",
                    "Executing security vulnerability audits...",
                    "Evaluating Big-O time and space complexity...",
                    "Synthesizing alternate refactor versions...",
                    "Polishing Developer DNA & Reviewer profiles..."
                )

                val animationJob = launch {
                    var i = 0
                    while (true) {
                        _loadingProgress.value = steps[i % steps.size]
                        delay(2000)
                        i++
                    }
                }

                val systemPrompt = """
You are "AI Senior Engineer", an elite autonomous code review system that combines the skills of:
- Senior Software Architect
- Security Engineer
- Performance Optimizer
- DevOps Reviewer
- Clean Code Mentor
- FAANG Interview Reviewer
- Bug Prediction Specialist

Your objective is to perform a brutally accurate, highly technical, and deeply insightful code review report.
Adhere strictly to the requested structure. You MUST output your responses in clean Markdown layout.

At the very beginning of your response, you MUST output a raw score block in this exact tag format, replacing key variables with integer scores out of 10:
[SCORES]
Code Quality: <integer>
Security: <integer>
Performance: <integer>
Maintainability: <integer>
Production Readiness: <integer>
[END_SCORES]

After this score block, write the full report in beautiful markdown layout:

# AI CODE REVIEW REPORT

## 1. Executive Summary
Provide a concise high-level summary of Code Quality, Security level, Performance quality, Maintainability, and Production readiness. Explain the overall architectural viability.

## 2. Critical Issues
List severe problems, logic bugs, or memory regressions first.
For each critical issue discovered, format as follows:
### Issue #1: <title>
- **Severity**: Critical / High / Medium / Low
- **Why It Matters**: <explanation>
- **Real-World Risk**: <real danger>
- **Suggested Fix**: 
```$language
<fix code>
```

## 3. Security Audit
Audit for SQL injection, XSS, CSRF, hardcoded secrets, unsafe deserialization, authentication/authorization flaws, and API exposure risks. Detailed attack scenario and mitigation.

## 4. Performance Analysis
Analyze time complexity, space complexity, redundant operations, and blocking calls. State the Big-O metrics clearly.

## 5. Code Smells & Maintainability
Explain architectural Coupling, Long functions, Dead code, Overengineering, or naming smells.

## 6. Hidden Bug Prediction
Predict possible future failures (concurrency races, null pointer crashes, overflow risks, edge-case crashes). Note the trigger condition and damage level.

## 7. AI Rewrite Battle
Provide three polished rewrites:
### Version A — Cleanest Code
Focus on readability and maintainability. Include brief comment explaining trade-offs.
### Version B — Fastest Code
Focus on speed, memory efficiency, and optimal Big-O complexity. Note trade-offs.
### Version C — Production Enterprise Version
Focus on scalability, modularity, type-safety, and production-ready industry patterns.

## 8. Developer DNA Analysis
Evaluate the developer's coding profile: Coding strengths, weaknesses, thinking patterns, and engineering maturity level (Junior, Mid, Senior, Tech Lead, Staff).

## 9. Human Reviewer Simulation
Simulate reviews from different personas:
- **Google Swe Reviewer**: <strictly adheres to clean code, testing, and formatting standards>
- **Startup CTO**: <pragmatic, focused on shipping rate, tech-debt trade-offs, and scaling bottlenecks>
- **Security Hunter**: <focuses exclusively on threat vector, credentials safety, and injection risks>
- **OSS Maintainer**: <evaluates backwards-compatibility, documentation, and extensibility>

## 10. Final Verdict
Review summary recommendation, and critical fixes checklist required before deployment.
""".trimIndent()

                val promptText = """
Programming Language: $language
Project Type: $projectType
Review Mode: $reviewMode

Code to Review:
```$language
$codeSnippet
```
""".trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = promptText)))),
                    systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
                    generationConfig = GenerationConfig(temperature = 0.2f)
                )

                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    throw IllegalStateException("API Key is missing or default. Reference 'Secrets' panel in AI Studio sidebar to add your GEMINI_API_KEY.")
                }

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.generateContent(apiKey, request)
                }

                animationJob.cancel()

                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: throw IllegalStateException("Received an empty response from the AI model engine.")

                val parsedScores = parseScoresFromText(rawText)
                val finalCleanedMarkdown = cleanReportText(rawText)

                val overallScore = (parsedScores.codeQuality + parsedScores.security +
                        parsedScores.performance + parsedScores.maintainability +
                        parsedScores.readiness).toFloat() / 5.0f

                val reviewTitle = "${language} ${projectType} (${reviewMode})"

                val completedReview = CodeReview(
                    title = reviewTitle,
                    language = language,
                    projectType = projectType,
                    reviewMode = reviewMode,
                    codeSnippet = codeSnippet,
                    reportContent = finalCleanedMarkdown,
                    score = overallScore,
                    timestamp = System.currentTimeMillis()
                )

                withContext(Dispatchers.IO) {
                    val insertedId = repository.insertReview(completedReview)
                    val insertedReview = completedReview.copy(id = insertedId.toInt())
                    _currentReport.value = insertedReview
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Review Failed: ${e.localizedMessage ?: "Unknown network error"}"
            } finally {
                _isLoading.value = false
                _loadingProgress.value = ""
            }
        }
    }

    fun parseScoresFromText(text: String): ReviewScores {
        var q = 5
        var s = 5
        var p = 5
        var m = 5
        var r = 5
        try {
            val startTag = "[SCORES]"
            val endTag = "[END_SCORES]"
            if (text.contains(startTag) && text.contains(endTag)) {
                val startIdx = text.indexOf(startTag) + startTag.length
                val endIdx = text.indexOf(endTag)
                val block = text.substring(startIdx, endIdx)
                block.lineSequence().forEach { line ->
                    val cleanLine = line.trim()
                    if (cleanLine.contains(":")) {
                        val key = cleanLine.substringBefore(":").trim()
                        val valueText = cleanLine.substringAfter(":").trim()
                        val value = valueText.toIntOrNull() ?: 5
                        when {
                            key.equals("Code Quality", ignoreCase = true) -> q = value
                            key.equals("Security", ignoreCase = true) -> s = value
                            key.equals("Performance", ignoreCase = true) -> p = value
                            key.equals("Maintainability", ignoreCase = true) -> m = value
                            key.equals("Production Readiness", ignoreCase = true) -> r = value
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ReviewScores(
            codeQuality = q.coerceIn(0, 10),
            security = s.coerceIn(0, 10),
            performance = p.coerceIn(0, 10),
            maintainability = m.coerceIn(0, 10),
            readiness = r.coerceIn(0, 10)
        )
    }

    private fun cleanReportText(text: String): String {
        return try {
            val startTag = "[SCORES]"
            val endTag = "[END_SCORES]"
            if (text.contains(startTag) && text.contains(endTag)) {
                val startIdx = text.indexOf(startTag)
                val endIdx = text.indexOf(endTag) + endTag.length
                text.removeRange(startIdx, endIdx).trim()
            } else text
        } catch (e: Exception) {
            text
        }
    }
}
