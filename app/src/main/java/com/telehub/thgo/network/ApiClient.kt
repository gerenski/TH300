package com.telehub.thgo.network

import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import org.json.JSONArray

class ApiClient(
    private val mac: String,
    private val url: String,
    private val referer: String
) {
    data class Channel(
        val id: String,
        val name: String,
        val cmd: String,
        val genre: String
    )

    private val client = OkHttpClient()

    fun login(
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val body = FormBody.Builder()
            .add("type", "stb")
            .add("action", "handshake")
            .add("token", "")
            .add("JsHttpRequest", "1-xml")
            .build()

        val request = buildRequest(body, "Bearer null")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFailure("Handshake failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyStr = response.body?.string() ?: return
                val token = try {
                    JSONObject(bodyStr).getJSONObject("js").getString("token")
                } catch (e: Exception) {
                    return onFailure("Token parse error: ${e.message}")
                }
                onSuccess(token)
            }
        })
    }

    fun getProfile(token: String, onComplete: () -> Unit) {
        val body = FormBody.Builder()
            .add("type", "stb")
            .add("action", "get_profile")
            .add("hd", "1")
            .add("JsHttpRequest", "1-xml")
            .add("hw_version", "1.7-BD-00")
            .add("ver", "ImageDescription: 21842")
            .add("serial_number", "STB0000001")
            .build()

        val request = buildRequest(body, "Bearer $token")
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("❌ get_profile failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                println("👤 PROFILE: ${response.body?.string()}")
                onComplete()
            }
        })
    }

    fun getGenres(token: String, onResult: (Map<String, String>) -> Unit) {
        val body = FormBody.Builder()
            .add("type", "itv")
            .add("action", "get_genres")
            .add("JsHttpRequest", "1-xml")
            .build()

        val request = buildRequest(body, "Bearer $token")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("❌ get_genres failed: ${e.message}")
                onResult(emptyMap())
            }

            override fun onResponse(call: Call, response: Response) {
                val result = response.body?.string() ?: return onResult(emptyMap())
                val json = JSONObject(result)
                val data = json.getJSONArray("js")
                val genreMap = mutableMapOf<String, String>()

                for (i in 0 until data.length()) {
                    val genreObj = data.getJSONObject(i)
                    val id = genreObj.getString("id")
                    val title = genreObj.getString("title")
                    genreMap[id] = title
                }

                println("🎭 Genres loaded: $genreMap")
                onResult(genreMap)
            }
        })
    }

    //fun getAllChannels(token: String, genreMap: Map<String, String>, onResult: (List<Triple<String, String, String>>) -> Unit) {
    fun getAllChannels(token: String, genreMap: Map<String, String>, onResult: (List<Channel>) -> Unit){
    val body = FormBody.Builder()
            .add("type", "itv")
            .add("action", "get_all_channels")
            .add("JsHttpRequest", "1-xml")
            .build()

        val request = buildRequest(body, "Bearer $token")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("❌ get_all_channels failed: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                val result = response.body?.string() ?: return
                val data = JSONObject(result).getJSONObject("js").getJSONArray("data")
                val list = mutableListOf<Channel>()
                for (i in 0 until data.length()) {
                    val ch = data.getJSONObject(i)
                    //println("📺 Channel JSON: $ch") // This will print the full info of each channel

                    val id = ch.getString("id")
                    val name = ch.getString("name")
                    val cmd = ch.getString("cmd").trim()
                    val genreId = ch.optString("tv_genre_id", "")
                    val genre = genreMap[genreId] ?: "Other"
                    list.add(Channel(id, name, cmd, genre))
                }
                println("✅ Channels parsed: ${list.size}")

                onResult(list)
            }
        })
    }

    fun getShortEpg(
        token: String,
        channelId: String,
        date: String,
        onResult: (List<Triple<String, String, String>>) -> Unit
    ) {
        val body = FormBody.Builder()
            .add("type", "itv")
            .add("action", "get_short_epg")
            .add("id", channelId)
            .add("date", date)
            .add("JsHttpRequest", "1-xml")
            .build()

        val request = buildRequest(body, "Bearer $token")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("❌ get_short_epg failed: ${e.message}")
                onResult(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                val result = response.body?.string() ?: return onResult(emptyList())

                val json = JSONObject(result)
                val js = json.get("js")

                val epgItems = mutableListOf<Triple<String, String, String>>()

                if (js is JSONArray) {
                    for (i in 0 until js.length()) {
                        val epg = js.getJSONObject(i)
                        val title = epg.getString("title")
                        val start = epg.getString("start")
                        val end = epg.getString("end")
                        epgItems.add(Triple(start, end, title))
                    }
                }

                onResult(epgItems)
                println("📡 Raw EPG response: $result")
                println("📨 Requesting EPG for id=$channelId, date=$date")
            }

        })
    }


    fun createLink(token: String, cmd: String, onResult: (String) -> Unit) {
        val body = FormBody.Builder()
            .add("type", "itv")
            .add("action", "create_link")
            .add("cmd", cmd)
            .add("series", "0")
            .add("forced_storage", "undefined")
            .add("disable_ad", "0")
            .add("JsHttpRequest", "1-xml")
            .build()

        val request = buildRequest(body, "Bearer $token")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("❌ create_link failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val result = response.body?.string() ?: return
                val streamUrl = JSONObject(result).getJSONObject("js").getString("cmd")
                onResult(streamUrl)
            }
        })
    }

    private fun buildRequest(body: RequestBody, auth: String): Request {
        return Request.Builder()
            .url(url)
            .post(body)
            .addHeader("User-Agent", "Mozilla/5.0 (QtEmbedded; U; Linux; C)")
            .addHeader("Authorization", auth)
            .addHeader("X-User-Agent", "Model: MAG254; Link: Ethernet")
            .addHeader("Referer", referer)
            .addHeader("Cookie", "mac=$mac; stb_lang=en; timezone=Europe/Sofia")
            .addHeader("Accept", "*/*")
            .build()
    }
}
