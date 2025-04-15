package com.telehub.telehubapp.viewmodels

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import java.time.LocalDate
import com.telehub.telehubapp.network.ApiClient
import com.telehub.telehubapp.network.ApiClient.Channel

class MainViewModel : ViewModel() {

    private val allChannels = mutableListOf<Channel>()

    var token by mutableStateOf("")
        private set

    fun updateToken(newToken: String) {
        token = newToken
    }

    var channels = mutableStateListOf<Channel>()
    //var allChannels = mutableListOf<Channel>()
    val filteredChannels: List<Channel> get() = channels
    var selectedChannel by mutableStateOf<Channel?>(null)

    fun updateChannels(list: List<ApiClient.Channel>) {
        allChannels.clear()
        allChannels.addAll(list)
        applyGenreFilter()
    }

    fun selectChannel(channel: Channel) {
        selectedChannel = channel
        selectedDate = LocalDate.now().toString() // Auto-select today
        applyGenreFilter()
    }

    fun selectGenre(genre: String?) {
        selectedGenre = genre
        applyGenreFilter()
    }

    private fun applyGenreFilter() {
        val genre = selectedGenre
        val filtered = if (genre == null) allChannels else allChannels.filter {
            it.genre.equals(genre, ignoreCase = true)
        }
        channels.clear()
        channels.addAll(filtered)
    }

    var selectedGenre by mutableStateOf<String?>(null)
        private set

    fun extractGenresFromChannels(): List<String> {
        println("📺 Genre extraction from ${allChannels.size} channels")
        return allChannels.map { it.genre }.distinct().sorted()
    }

    var genres by mutableStateOf<List<String>>(emptyList())
        private set

    fun updateGenres(map: Map<String, String>) {
        genres = map.values.distinct().sorted()
    }

    val availableDates: List<String> = List(7) { offset ->
        LocalDate.now().minusDays(3).plusDays(offset.toLong()).toString()
    }

    var selectedDate by mutableStateOf(LocalDate.now().toString())
        private set

    fun selectDate(date: String) {
        selectedDate = date
    }

    var epgItems by mutableStateOf<List<Triple<String, String, String>>>(emptyList())
        private set

    fun updateEpg(list: List<Triple<String, String, String>>) {
        println("📺 EPG items loaded: ${list.size}")
        list.forEach {
            println("🕒 ${it.first} - ${it.second}: ${it.third}")
        }
        epgItems = list
    }

    fun loadEpg(token: String, id: String, date: String, apiClient: ApiClient) {
        apiClient.getShortEpg(token, id, date) { epg ->
            updateEpg(epg)
        }
    }


}


