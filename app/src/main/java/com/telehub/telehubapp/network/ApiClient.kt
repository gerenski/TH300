package com.telehub.telehubapp.network

import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class ApiClient(
    private val mac: String,
    private val url: String,
    private val referer: String
) {
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

    fun getAllChannels(token: String, onResult: (List<Triple<String, String, String>>) -> Unit) {
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
                val list = mutableListOf<Triple<String, String, String>>()
                for (i in 0 until data.length()) {
                    val ch = data.getJSONObject(i)
                    val name = ch.getString("name")
                    val cmd = ch.getString("cmd")
                    val genre = ch.optString("tv_genre_name", "Other") // fallback
                    list.add(Triple(name, cmd, genre))
                }
                println("✅ Channels parsed: ${list.size}")

                onResult(list)
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
