package ru.ecobirch.fa.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class GeocodePlace(
    val name: String,
    val description: String,
    val fullAddress: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * Offline-resilient Geocoding Service using OpenStreetMap Nominatim + Photon Komoot fallback
 * plus direct numeric coordinate parsing. Requires zero paid API keys.
 */
class GeocodingService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }
    private val searchCache = ConcurrentHashMap<String, List<GeocodePlace>>()
    private val reverseCache = ConcurrentHashMap<String, GeocodePlace>()

    suspend fun search(rawQuery: String): List<GeocodePlace> = withContext(Dispatchers.IO) {
        val query = rawQuery.trim()
        if (query.isBlank()) return@withContext emptyList()

        // 1. Direct coordinates input (e.g. "55.7558, 37.6173" or "55.7558 37.6173")
        val coordRegex = Regex("""^(-?\d+(?:\.\d+)?)[,\s]+(-?\d+(?:\.\d+)?)$""")
        coordRegex.matchEntire(query)?.let { match ->
            val lat = match.groupValues[1].toDoubleOrNull()
            val lng = match.groupValues[2].toDoubleOrNull()
            if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                return@withContext listOf(
                    GeocodePlace(
                        name = "Координаты: ${"%.4f".format(lat)}, ${"%.4f".format(lng)}",
                        description = "Прямые географические координаты",
                        fullAddress = "Широта $lat, Долгота $lng",
                        latitude = lat,
                        longitude = lng
                    )
                )
            }
        }

        val cacheKey = query.lowercase()
        searchCache[cacheKey]?.let { return@withContext it }

        val encoded = URLEncoder.encode(query, "UTF-8")

        // 2. Nominatim OpenStreetMap
        runCatching {
            val req = Request.Builder()
                .url("https://nominatim.openstreetmap.org/search?q=$encoded&format=json&addressdetails=1&limit=6&accept-language=ru")
                .header("User-Agent", "BetulaBioindicationAndroid/2.0 (ru.ecobirch.fa.app)")
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    val arr = json.parseToJsonElement(body).jsonArray
                    val parsed = arr.mapNotNull { el ->
                        val obj = el.jsonObject
                        val lat = obj["lat"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@mapNotNull null
                        val lon = obj["lon"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@mapNotNull null
                        val displayName = obj["display_name"]?.jsonPrimitive?.content.orEmpty()
                        val parts = displayName.split(",").map { it.trim() }
                        val shortName = obj["name"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                            ?: parts.firstOrNull().orEmpty().ifBlank { "Точка" }
                        val desc = parts.drop(1).take(3).joinToString(", ")
                        GeocodePlace(shortName, desc, displayName.ifBlank { shortName }, lat, lon)
                    }
                    if (parsed.isNotEmpty()) {
                        searchCache[cacheKey] = parsed
                        return@withContext parsed
                    }
                }
            }
        }

        // 3. Photon Komoot fallback
        runCatching {
            val req = Request.Builder()
                .url("https://photon.komoot.io/api/?q=$encoded&limit=6")
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    val root = json.parseToJsonElement(body).jsonObject
                    val features = root["features"]?.jsonArray.orEmpty()
                    val parsed = features.mapNotNull { feat ->
                        val fObj = feat.jsonObject
                        val coords = fObj["geometry"]?.jsonObject?.get("coordinates")?.jsonArray ?: return@mapNotNull null
                        if (coords.size < 2) return@mapNotNull null
                        val lon = coords[0].jsonPrimitive.content.toDoubleOrNull() ?: return@mapNotNull null
                        val lat = coords[1].jsonPrimitive.content.toDoubleOrNull() ?: return@mapNotNull null
                        val props = fObj["properties"]?.jsonObject ?: return@mapNotNull null
                        val name = props["name"]?.jsonPrimitive?.content
                            ?: props["street"]?.jsonPrimitive?.content
                            ?: "Найденный адрес"
                        val city = props["city"]?.jsonPrimitive?.content.orEmpty()
                        val country = props["country"]?.jsonPrimitive?.content.orEmpty()
                        val desc = listOf(city, country).filter { it.isNotBlank() }.joinToString(", ")
                        GeocodePlace(name, desc, listOf(name, desc).filter { it.isNotBlank() }.joinToString(", "), lat, lon)
                    }
                    if (parsed.isNotEmpty()) {
                        searchCache[cacheKey] = parsed
                        return@withContext parsed
                    }
                }
            }
        }

        emptyList()
    }

    suspend fun reverseGeocode(lat: Double, lng: Double): GeocodePlace = withContext(Dispatchers.IO) {
        val key = "${"%.4f".format(lat)},${"%.4f".format(lng)}"
        reverseCache[key]?.let { return@withContext it }

        runCatching {
            val req = Request.Builder()
                .url("https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lng&format=json&addressdetails=1&accept-language=ru")
                .header("User-Agent", "BetulaBioindicationAndroid/2.0 (ru.ecobirch.fa.app)")
                .header("Accept", "application/json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    val root = json.parseToJsonElement(body).jsonObject
                    val displayName = root["display_name"]?.jsonPrimitive?.content.orEmpty()
                    val addr = root["address"]?.jsonObject
                    val park = addr?.get("park")?.jsonPrimitive?.content
                        ?: addr?.get("leisure")?.jsonPrimitive?.content
                        ?: addr?.get("nature_reserve")?.jsonPrimitive?.content
                    val road = addr?.get("road")?.jsonPrimitive?.content
                        ?: addr?.get("pedestrian")?.jsonPrimitive?.content
                    val house = addr?.get("house_number")?.jsonPrimitive?.content
                    val suburb = addr?.get("suburb")?.jsonPrimitive?.content
                        ?: addr?.get("city_district")?.jsonPrimitive?.content
                    val city = addr?.get("city")?.jsonPrimitive?.content
                        ?: addr?.get("town")?.jsonPrimitive?.content
                        ?: addr?.get("village")?.jsonPrimitive?.content

                    val shortName = when {
                        !park.isNullOrBlank() -> park
                        !road.isNullOrBlank() -> if (!house.isNullOrBlank()) "$road, д. $house" else road
                        !suburb.isNullOrBlank() -> suburb
                        !city.isNullOrBlank() -> city
                        displayName.isNotBlank() -> displayName.split(",").firstOrNull()?.trim().orEmpty()
                        else -> "Точка (${"%.4f".format(lat)}°, ${"%.4f".format(lng)}°)"
                    }

                    val place = GeocodePlace(
                        name = shortName,
                        description = city.orEmpty(),
                        fullAddress = displayName.ifBlank { shortName },
                        latitude = lat,
                        longitude = lng
                    )
                    reverseCache[key] = place
                    return@withContext place
                }
            }
        }

        GeocodePlace(
            name = "Точка (${"%.4f".format(lat)}°, ${"%.4f".format(lng)}°)",
            description = "Офлайн-координаты GPS",
            fullAddress = "Координаты: ${"%.5f".format(lat)}, ${"%.5f".format(lng)}",
            latitude = lat,
            longitude = lng
        )
    }
}
