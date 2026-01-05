package org.example.learnify.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class GeminiResponse(
    val candidates: List<Candidate>? = null,
    val usageMetadata: UsageMetadata? = null,
    val error: GeminiError? = null
)

@Serializable
data class GeminiError(
    val code: Int? = null,
    val message: String,
    val status: String? = null
)

@Serializable
data class Candidate(
    val content: Content,
    val finishReason: String? = null,
    val safetyRatings: List<SafetyRating>? = null
)

@Serializable
data class SafetyRating(
    val category: String,
    val probability: String
)

@Serializable
data class UsageMetadata(
    val promptTokenCount: Int,
    val candidatesTokenCount: Int,
    val totalTokenCount: Int
)
