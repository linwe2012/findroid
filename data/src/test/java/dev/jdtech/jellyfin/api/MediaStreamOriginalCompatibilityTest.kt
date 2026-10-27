package dev.jdtech.jellyfin.api

import android.content.ContextWrapper
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jellyfin.sdk.api.client.extensions.showApi
import org.jellyfin.sdk.api.client.util.ApiSerializer
import org.jellyfin.sdk.api.okhttp.OkHttpFactory
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo
import org.jellyfin.sdk.model.DeviceInfo
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.MediaStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaStreamOriginalCompatibilityTest {
    private val json = ApiSerializer.json

    @Test
    fun missingIsOriginalIsDefaultedOnlyInMediaStreamResponseShapes() {
        val fixture =
            """
            {"Items":[{"MediaSources":[{"MediaStreams":[{"Type":"Video","Index":0,"IsInterlaced":false,"IsDefault":false,"IsForced":false,"IsHearingImpaired":false,"IsExternal":false,"IsTextSubtitleStream":false,"SupportsExternalStream":false}]}]}]}
            """
                .trimIndent()

        assertThrows(SerializationException::class.java) {
            json.decodeFromString<MediaStream>(
                """{"Type":"Video","Index":0,"IsInterlaced":false,"IsDefault":false,"IsForced":false,"IsHearingImpaired":false,"IsExternal":false,"IsTextSubtitleStream":false,"SupportsExternalStream":false}"""
            )
        }

        val normalized =
            MediaStreamOriginalCompatibilityInterceptor.normalizeMediaStreamOriginal(fixture)
        val mediaStream =
            json.decodeFromString<MediaStream>(
                json
                    .parseToJsonElement(normalized)
                    .jsonObject["Items"]!!
                    .jsonArray[0]
                    .jsonObject["MediaSources"]!!
                    .jsonArray[0]
                    .jsonObject["MediaStreams"]!!
                    .jsonArray[0]
                    .toString()
            )
        assertFalse(mediaStream.isOriginal)
    }

    @Test
    fun explicitOriginalValuesAndUnrelatedJsonArePreserved() {
        val explicitTrue = """{"MediaSources":[{"MediaStreams":[{"IsOriginal":true}]}]}"""
        val explicitFalse = """{"MediaSources":[{"MediaStreams":[{"IsOriginal":false}]}]}"""
        val explicitNull = """{"MediaSources":[{"MediaStreams":[{"IsOriginal":null}]}]}"""
        val unrelated = """{"Other":[{"MediaStreams":[{}]}]}"""

        assertTrue(
            MediaStreamOriginalCompatibilityInterceptor.normalizeMediaStreamOriginal(explicitTrue)
                .contains("\"IsOriginal\":true")
        )
        assertTrue(
            MediaStreamOriginalCompatibilityInterceptor.normalizeMediaStreamOriginal(explicitFalse)
                .contains("\"IsOriginal\":false")
        )
        assertTrue(
            MediaStreamOriginalCompatibilityInterceptor.normalizeMediaStreamOriginal(explicitNull)
                .contains("\"IsOriginal\":null")
        )
        assertEquals(
            unrelated,
            MediaStreamOriginalCompatibilityInterceptor.normalizeMediaStreamOriginal(unrelated),
        )
    }

    @Test
    fun okhttpFactoryInterceptorMakesMissingFieldDecodeOnActualItemResponse() {
        val fixture =
            """
            {"Items":[{"MediaSources":[{"MediaStreams":[{"IsInterlaced":false,"IsDefault":false,"IsForced":false,"IsHearingImpaired":false,"Type":"Video","Index":0,"IsExternal":false,"IsTextSubtitleStream":false,"SupportsExternalStream":false}]}]}]}
            """
                .trimIndent()
        val server = MockWebServer()
        server.enqueue(
            MockResponse.Builder()
                .addHeader("Content-Type", "application/json")
                .body(fixture)
                .build()
        )
        server.enqueue(
            MockResponse.Builder()
                .addHeader("Content-Type", "application/json")
                .body(fixture)
                .build()
        )
        server.enqueue(
            MockResponse.Builder().addHeader("Content-Type", "text/plain").body(fixture).build()
        )
        server.start()
        try {
            val client =
                OkHttpClient.Builder()
                    .addInterceptor(MediaStreamOriginalCompatibilityInterceptor)
                    .build()
            val response =
                client.newCall(Request.Builder().url(server.url("/Shows/NextUp")).build()).execute()
            assertEquals(null, response.header("Content-Length"))
            val streamJson =
                json
                    .parseToJsonElement(response.body.string())
                    .jsonObject["Items"]!!
                    .jsonArray[0]
                    .jsonObject["MediaSources"]!!
                    .jsonArray[0]
                    .jsonObject["MediaStreams"]!!
                    .jsonArray[0]
                    .toString()
            assertFalse(json.decodeFromString<MediaStream>(streamJson).isOriginal)

            val unrelatedResponse =
                client.newCall(Request.Builder().url(server.url("/Sessions")).build()).execute()
            assertEquals(fixture, unrelatedResponse.body.string())

            val nonJsonResponse =
                client.newCall(Request.Builder().url(server.url("/Shows/NextUp")).build()).execute()
            assertEquals(fixture, nonJsonResponse.body.string())
        } finally {
            server.close()
        }
    }

    @Test
    fun sdkTypedEpisodesClientDecodesMissingOriginalFromItemsMediaSources() = runBlocking {
        val fixture =
            checkNotNull(javaClass.getResource("/real-server-mediastream-omits-is-original.json"))
                .readText()
        val streamFixtures =
            json
                .parseToJsonElement(fixture)
                .jsonObject["Items"]!!
                .jsonArray[0]
                .jsonObject["MediaSources"]!!
                .jsonArray[0]
                .jsonObject["MediaStreams"]!!
                .jsonArray
        assertEquals(5, streamFixtures.size)
        streamFixtures.forEach { stream ->
            assertThrows(SerializationException::class.java) {
                json.decodeFromString<MediaStream>(stream.toString())
            }
        }
        val server = MockWebServer()
        server.enqueue(
            MockResponse.Builder()
                .addHeader("Content-Type", "application/json")
                .body(fixture)
                .build()
        )
        server.start()
        try {
            val client =
                OkHttpClient.Builder()
                    .addInterceptor(MediaStreamOriginalCompatibilityInterceptor)
                    .build()
            val api = createJellyfin {
                context = ContextWrapper(null)
                clientInfo = ClientInfo(name = "test", version = "1")
                deviceInfo = DeviceInfo(id = "test-id", name = "test-device")
                apiClientFactory = OkHttpFactory(client)
            }
                .createApi(baseUrl = server.url("/").toString())
            val result =
                api.showApi.getEpisodes(
                    UUID.fromString("00000000-0000-0000-0000-000000000002"),
                    UUID.fromString("00000000-0000-0000-0000-000000000003"),
                    seasonId = UUID.fromString("00000000-0000-0000-0000-000000000004"),
                    fields = listOf(ItemFields.OVERVIEW),
                )
            val streams = result.content.items.single().mediaSources!!.single().mediaStreams!!
            assertEquals(5, streams.size)
            assertTrue(streams.all { !it.isOriginal })
        } finally {
            server.close()
        }
    }
}
