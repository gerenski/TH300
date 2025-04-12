package com.telehub.telehubapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.hls.HlsMediaSource

class MainActivity : ComponentActivity() {
    private lateinit var player: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        player = ExoPlayer.Builder(this).build()

        setContent {
            MaterialTheme {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text("TeleHub", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = { loginAndPlay() }) {
                        Text("Login & Play")
                    }
                }
            }
        }
    }

    private fun loginAndPlay() {
        val mac = "00:1A:79:00:00:01"
        val client = OkHttpClient()

        val url = "http://test.eurolan.net/stalker_portal/server/load.php"

        val body = FormBody.Builder()
            .add("type", "stb")
            .add("action", "handshake")
            .add("token", "")
            .add("JsHttpRequest", "1-xml")
            .build()

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("User-Agent", "Mozilla/5.0 (QtEmbedded; U; Linux; C)")
            .addHeader("Authorization", "Bearer null")
            .addHeader("X-User-Agent", "Model: MAG254; Link: Ethernet")
            .addHeader("Referer", "http://test.eurolan.net/stalker_portal/c/")
            .addHeader("Cookie", "mac=$mac; stb_lang=en; timezone=Europe/Sofia")
            .addHeader("Accept", "*/*")
            .build()

        println("➡️ Sending handshake request...")
        lifecycleScope.launch {
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                override fun onResponse(call: Call, response: Response) {
                    val bodyStr = response.body?.string() ?: ""
                    println("RAW RESPONSE: $bodyStr")

                    try {
                        val json = JSONObject(bodyStr)
                        val token = json.getJSONObject("js").getString("token")
                        println("TOKEN: $token")

                        // ➕ Now call get_profile
                        getProfile(token)

                    } catch (e: Exception) {
                        println("JSON ERROR: ${e.message}")
                    }
                }


            })
        }
    }

    private fun getProfile(token: String) {
        val mac = "00:1A:79:00:00:01"
        val client = OkHttpClient()

        val url = "http://test.eurolan.net/stalker_portal/server/load.php"

        val body = FormBody.Builder()
            .add("type", "stb")
            .add("action", "get_profile")
            .add("hd", "1")
            .add("JsHttpRequest", "1-xml")
            .add("hw_version", "1.7-BD-00")
            .add("ver", "ImageDescription: 21842")
            .add("serial_number", "STB0000001")
            .build()

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("User-Agent", "Mozilla/5.0 (QtEmbedded; U; Linux; C)")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("X-User-Agent", "Model: MAG254; Link: Ethernet")
            .addHeader("Referer", "http://test.eurolan.net/stalker_portal/c/")
            .addHeader("Cookie", "mac=$mac; stb_lang=en; timezone=Europe/Sofia")
            .addHeader("Accept", "*/*")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("❌ get_profile failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val profileStr = response.body?.string() ?: ""
                println("👤 PROFILE: $profileStr")
                getAllChannels(token)
            }
        })
    }

    private fun getAllChannels(token: String) {
        val mac = "00:1A:79:00:00:01"
        val client = OkHttpClient()

        val url = "http://test.eurolan.net/stalker_portal/server/load.php"

        val body = FormBody.Builder()
            .add("type", "itv")
            .add("action", "get_all_channels")
            .add("JsHttpRequest", "1-xml")
            .build()

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("User-Agent", "Mozilla/5.0 (QtEmbedded; U; Linux; C)")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("X-User-Agent", "Model: MAG254; Link: Ethernet")
            .addHeader("Referer", "http://test.eurolan.net/stalker_portal/c/")
            .addHeader("Cookie", "mac=$mac; stb_lang=en; timezone=Europe/Sofia")
            .addHeader("Accept", "*/*")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("❌ get_all_channels failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val channelsStr = response.body?.string() ?: ""
                println("📺 CHANNELS: $channelsStr")
                createLink(token, "ffrt http:///ch/97")

            }
        })

    }

    private fun createLink(token: String, cmd: String) {
        val mac = "00:1A:79:00:00:01"
        val url = "http://test.eurolan.net/stalker_portal/server/load.php"

        val body = FormBody.Builder()
            .add("type", "itv")
            .add("action", "create_link")
            .add("cmd", cmd)
            .add("series", "0")
            .add("forced_storage", "undefined")
            .add("disable_ad", "0")
            .add("JsHttpRequest", "1-xml")
            .build()

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("User-Agent", "Mozilla/5.0 (QtEmbedded; U; Linux; C)")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("X-User-Agent", "Model: MAG254; Link: Ethernet")
            .addHeader("Referer", "http://test.eurolan.net/stalker_portal/c/")
            .addHeader("Cookie", "mac=$mac; stb_lang=en; timezone=Europe/Sofia")
            .addHeader("Accept", "*/*")
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("❌ create_link failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val result = response.body?.string()
                println("🎥 STREAM LINK RESULT: $result")
                val streamUrl = JSONObject(result).getJSONObject("js").getString("cmd")
                println("🎬 FINAL STREAM: $streamUrl")
                playStream(streamUrl)
            }
        })
    }

    private fun playStream(url: String) {
        runOnUiThread {
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setUserAgent("Mozilla/5.0 (QtEmbedded; U; Linux; C)")
                .setDefaultRequestProperties(
                    mapOf(
                        "User-Agent" to "Mozilla/5.0 (QtEmbedded; U; Linux; C)",
                        "Referer" to "http://test.eurolan.net/stalker_portal/c/",
                        "Cookie" to "mac=00:1A:79:00:00:01; stb_lang=en; timezone=Europe/Sofia",
                        "Accept-Encoding" to "identity"  // 👈 add this!
                    )
                )

            val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(url))

            player.setMediaSource(mediaSource)
            player.prepare()
            player.play()
            val playerView = PlayerView(this).apply {
                useController = true
                this.player = player
            }

            setContentView(playerView)

            player.setMediaSource(mediaSource)
            player.prepare()
            player.play()
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}
