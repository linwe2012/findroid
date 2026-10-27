package dev.jdtech.jellyfin.api

import android.content.Context
import dev.jdtech.jellyfin.data.BuildConfig
import dev.jdtech.jellyfin.settings.domain.Constants
import java.util.UUID
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.jellyfin.sdk.api.client.HttpClientOptions
import org.jellyfin.sdk.api.client.extensions.authenticationApi
import org.jellyfin.sdk.api.client.extensions.brandingApi
import org.jellyfin.sdk.api.client.extensions.deviceApi
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.api.client.extensions.mediaInfoApi
import org.jellyfin.sdk.api.client.extensions.mediaSegmentApi
import org.jellyfin.sdk.api.client.extensions.sessionApi
import org.jellyfin.sdk.api.client.extensions.showApi
import org.jellyfin.sdk.api.client.extensions.suggestionApi
import org.jellyfin.sdk.api.client.extensions.systemApi
import org.jellyfin.sdk.api.client.extensions.trickPlayApi
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.api.client.extensions.userDataApi
import org.jellyfin.sdk.api.client.extensions.userViewApi
import org.jellyfin.sdk.api.client.extensions.videoApi
import org.jellyfin.sdk.api.okhttp.OkHttpFactory
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo

/**
 * Jellyfin API class using org.jellyfin.sdk:jellyfin-core
 *
 * @param androidContext The context
 * @param socketTimeout The socket timeout
 * @constructor Creates a new [JellyfinApi] instance
 */
class JellyfinApi(
    androidContext: Context,
    requestTimeout: Long = Constants.NETWORK_DEFAULT_REQUEST_TIMEOUT,
    connectTimeout: Long = Constants.NETWORK_DEFAULT_CONNECT_TIMEOUT,
    socketTimeout: Long = Constants.NETWORK_DEFAULT_SOCKET_TIMEOUT,
) {
    private val apiHttpClient =
        OkHttpClient.Builder().addInterceptor(MediaStreamOriginalCompatibilityInterceptor).build()

    val jellyfin = createJellyfin {
        clientInfo =
            ClientInfo(
                name =
                    androidContext.applicationInfo
                        .loadLabel(androidContext.packageManager)
                        .toString(),
                version = BuildConfig.VERSION_NAME,
            )
        context = androidContext
        apiClientFactory = OkHttpFactory(apiHttpClient)
    }
    val api =
        jellyfin.createApi(
            httpClientOptions =
                HttpClientOptions(
                    requestTimeout = requestTimeout.toDuration(DurationUnit.MILLISECONDS),
                    connectTimeout = connectTimeout.toDuration(DurationUnit.MILLISECONDS),
                    socketTimeout = socketTimeout.toDuration(DurationUnit.MILLISECONDS),
                )
        )
    var userId: UUID? = null

    val brandingApi = api.brandingApi
    val authenticationApi = api.authenticationApi
    val deviceApi = api.deviceApi
    val libraryApi = api.libraryApi
    val mediaInfoApi = api.mediaInfoApi
    val mediaSegmentApi = api.mediaSegmentApi
    val sessionApi = api.sessionApi
    val showApi = api.showApi
    val suggestionApi = api.suggestionApi
    val systemApi = api.systemApi
    val trickPlayApi = api.trickPlayApi
    val userApi = api.userApi
    val userDataApi = api.userDataApi
    val videoApi = api.videoApi
    val userViewApi = api.userViewApi

    companion object {
        @Volatile private var INSTANCE: JellyfinApi? = null

        fun getInstance(
            context: Context,
            requestTimeout: Long = Constants.NETWORK_DEFAULT_REQUEST_TIMEOUT,
            connectTimeout: Long = Constants.NETWORK_DEFAULT_CONNECT_TIMEOUT,
            socketTimeout: Long = Constants.NETWORK_DEFAULT_SOCKET_TIMEOUT,
        ): JellyfinApi {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance =
                        JellyfinApi(
                            androidContext = context.applicationContext,
                            requestTimeout = requestTimeout,
                            connectTimeout = connectTimeout,
                            socketTimeout = socketTimeout,
                        )
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}

/**
 * Jellyfin 10.11 can omit `IsOriginal` on media streams even though the generated SDK model marks
 * it as required. Normalize only that optional server field at its documented response paths;
 * preserve explicit values and leave malformed or unrelated responses untouched.
 */
internal object MediaStreamOriginalCompatibilityInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!mayContainMediaStreams(chain.request().url.encodedPath)) {
            return chain.proceed(chain.request())
        }
        val response = chain.proceed(chain.request())
        val body = response.body
        val mediaType = body.contentType() ?: return response
        if (mediaType.subtype != "json" && !mediaType.subtype.endsWith("+json")) return response

        val source = body.string()
        val normalized = runCatching { normalizeMediaStreamOriginal(source) }.getOrNull()
        if (normalized == null || normalized == source) {
            return response
                .newBuilder()
                .removeHeader("Content-Length")
                .removeHeader("Content-Encoding")
                .body(source.toResponseBody(mediaType))
                .build()
        }

        return response
            .newBuilder()
            .removeHeader("Content-Length")
            .removeHeader("Content-Encoding")
            .body(normalized.toResponseBody(mediaType))
            .build()
    }

    internal fun normalizeMediaStreamOriginal(source: String): String {
        val root =
            kotlinx.serialization.json.Json.parseToJsonElement(source) as? JsonObject
                ?: return source
        val normalized = normalizeMediaSources(root["MediaSources"] as? JsonArray)
        val rootStreams = normalizeMediaStreams(root["MediaStreams"] as? JsonArray)
        val items = root["Items"] as? JsonArray
        var itemsChanged = false
        val updatedItems = items?.map { item ->
            val itemObject = item as? JsonObject ?: return@map item
            val mediaSources = normalizeMediaSources(itemObject["MediaSources"] as? JsonArray)
            val itemStreams = normalizeMediaStreams(itemObject["MediaStreams"] as? JsonArray)
            if (mediaSources == null && itemStreams == null) item
            else {
                itemsChanged = true
                var updatedItem = itemObject
                if (mediaSources != null)
                    updatedItem = JsonObject(updatedItem + ("MediaSources" to mediaSources))
                if (itemStreams != null)
                    updatedItem = JsonObject(updatedItem + ("MediaStreams" to itemStreams))
                updatedItem
            }
        }

        if (normalized == null && rootStreams == null && !itemsChanged) return source
        val updatedRoot = root.toMutableMap()
        if (normalized != null) updatedRoot["MediaSources"] = normalized
        if (rootStreams != null) updatedRoot["MediaStreams"] = rootStreams
        if (itemsChanged) updatedRoot["Items"] = JsonArray(updatedItems!!)
        return JsonObject(updatedRoot).toString()
    }

    private fun normalizeMediaStreams(streams: JsonArray?): JsonArray? {
        if (streams == null) return null
        var changed = false
        val updated = streams.map { stream ->
            val streamObject = stream as? JsonObject ?: return@map stream
            if ("IsOriginal" in streamObject) return@map stream
            changed = true
            JsonObject(streamObject + ("IsOriginal" to JsonPrimitive(false)))
        }
        return if (changed) JsonArray(updated) else null
    }

    private fun mayContainMediaStreams(path: String): Boolean {
        return "/Items" in path ||
            path.endsWith("/Episodes") ||
            path.endsWith("/NextUp") ||
            path.endsWith("/PlaybackInfo")
    }

    private fun normalizeMediaSources(sources: JsonArray?): JsonArray? {
        if (sources == null) return null
        var changed = false
        val updated = sources.map { source ->
            val sourceObject = source as? JsonObject ?: return@map source
            val streams = sourceObject["MediaStreams"] as? JsonArray ?: return@map source
            val updatedStreams = normalizeMediaStreams(streams) ?: return@map source
            changed = true
            JsonObject(sourceObject + ("MediaStreams" to updatedStreams))
        }
        return if (changed) JsonArray(updated) else null
    }
}
