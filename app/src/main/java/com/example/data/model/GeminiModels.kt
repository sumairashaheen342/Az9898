package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "responseModalities") val responseModalities: List<String>? = null,
    @Json(name = "imageConfig") val imageConfig: ImageConfig? = null,
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class ImageConfig(
    @Json(name = "aspectRatio") val aspectRatio: String? = null,
    @Json(name = "imageSize") val imageSize: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content? = null
)

// Veo Video Generation Request structures
@JsonClass(generateAdapter = true)
data class GenerateVideosRequest(
    @Json(name = "prompt") val prompt: String,
    @Json(name = "config") val config: VeoConfig? = null
)

@JsonClass(generateAdapter = true)
data class VeoConfig(
    @Json(name = "numberOfVideos") val numberOfVideos: Int = 1,
    @Json(name = "resolution") val resolution: String = "1080p",
    @Json(name = "aspectRatio") val aspectRatio: String = "16:9"
)

@JsonClass(generateAdapter = true)
data class VeoResponse(
    @Json(name = "name") val name: String? = null,
    @Json(name = "done") val done: Boolean? = null,
    @Json(name = "response") val response: VeoResult? = null,
    @Json(name = "error") val error: VeoError? = null
)

@JsonClass(generateAdapter = true)
data class VeoResult(
    @Json(name = "generatedVideos") val generatedVideos: List<VeoVideo>? = null
)

@JsonClass(generateAdapter = true)
data class VeoVideo(
    @Json(name = "videoUri") val videoUri: String? = null,
    @Json(name = "mimeType") val mimeType: String? = null
)

@JsonClass(generateAdapter = true)
data class VeoError(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "message") val message: String? = null
)
