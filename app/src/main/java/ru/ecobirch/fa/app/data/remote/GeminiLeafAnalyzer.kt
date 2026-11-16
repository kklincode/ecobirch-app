package ru.ecobirch.fa.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.ecobirch.fa.app.domain.model.DetectionSource
import ru.ecobirch.fa.app.domain.model.LeafPoint
import ru.ecobirch.fa.app.domain.model.LeafPoints
import ru.ecobirch.fa.app.vision.LeafDetectionResult
import java.util.concurrent.TimeUnit

/**
 * Optional Google Gemini Vision landmark analyzer.
 * Never called unless the user explicitly enters their own API key and enables cloud AI in Settings.
 */
class GeminiLeafAnalyzer {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun analyzeLeaf(base64Jpeg: String, apiKey: String): LeafDetectionResult? =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) return@withContext null

            val prompt = """
                You are an expert botanical morphometry detector for Betula pendula (Silver Birch) leaves.
                Locate exactly 12 biological landmark points on a 0..100 coordinate grid (x: 0 left..100 right, y: 0 top..100 bottom):
                - base: petiole junction at leaf blade base
                - tip: sharp apex tip
                - leftVein1Base, leftVein1End, leftVein2Base, leftVein2End, leftWidthMax
                - rightVein1Base, rightVein1End, rightVein2Base, rightVein2End, rightWidthMax
                Return ONLY a JSON object with these 12 keys, each {"x": number, "y": number}.
            """.trimIndent()

            val payload = """
                {
                  "contents": [{
                    "parts": [
                      { "inline_data": { "mime_type": "image/jpeg", "data": "$base64Jpeg" } },
                      { "text": ${Json.encodeToString(kotlinx.serialization.serializer<String>(), prompt)} }
                    ]
                  }],
                  "generationConfig": {
                    "responseMimeType": "application/json",
                    "temperature": 0.1
                  }
                }
            """.trimIndent()

            runCatching {
                val req = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                    .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext null
                    val bodyStr = resp.body?.string().orEmpty()
                    val root = json.parseToJsonElement(bodyStr).jsonObject
                    val rawText = root["candidates"]
                        ?.jsonArray?.firstOrNull()
                        ?.jsonObject?.get("content")
                        ?.jsonObject?.get("parts")
                        ?.jsonArray?.firstOrNull()
                        ?.jsonObject?.get("text")
                        ?.jsonPrimitive?.content ?: return@withContext null

                    val obj = json.parseToJsonElement(rawText.trim()).jsonObject
                    val points = parsePoints(obj) ?: return@withContext null
                    LeafDetectionResult(
                        points = points,
                        confidence = 0.95f,
                        source = DetectionSource.GEMINI_AI,
                        statusMessage = "12 точек определены через Gemini Vision. Проверьте их с лупой."
                    )
                }
            }.getOrNull()
        }

    private fun parsePoints(obj: JsonObject): LeafPoints? {
        fun pt(key: String): LeafPoint? {
            val pObj = obj[key]?.jsonObject ?: return null
            val x = pObj["x"]?.jsonPrimitive?.floatOrNull ?: return null
            val y = pObj["y"]?.jsonPrimitive?.floatOrNull ?: return null
            return LeafPoint(x.coerceIn(1f, 99f), y.coerceIn(1f, 99f))
        }
        return LeafPoints(
            base = pt("base") ?: return null,
            tip = pt("tip") ?: return null,
            leftVein1Base = pt("leftVein1Base") ?: return null,
            leftVein1End = pt("leftVein1End") ?: return null,
            leftVein2Base = pt("leftVein2Base") ?: return null,
            leftVein2End = pt("leftVein2End") ?: return null,
            leftWidthMax = pt("leftWidthMax") ?: return null,
            rightVein1Base = pt("rightVein1Base") ?: return null,
            rightVein1End = pt("rightVein1End") ?: return null,
            rightVein2Base = pt("rightVein2Base") ?: return null,
            rightVein2End = pt("rightVein2End") ?: return null,
            rightWidthMax = pt("rightWidthMax") ?: return null
        )
    }
}
