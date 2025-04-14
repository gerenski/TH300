package com.telehub.telehubapp.viewmodels

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    private val allChannels = mutableListOf<Triple<String, String, String>>()

    var token by mutableStateOf("")
        private set

    var channels = mutableStateListOf<Triple<String, String, String>>()
        private set

    var selectedChannel by mutableStateOf<Pair<String, String>?>(null)

    fun updateToken(newToken: String) {
        token = newToken
    }

    fun updateChannels(list: List<Triple<String, String, String>>) {
        allChannels.clear()
        allChannels.addAll(list)
        applyGenreFilter()
    }

    fun selectChannel(channel: Pair<String, String>) {
        selectedChannel = channel
    }

    fun selectGenre(genre: String?) {
        selectedGenre = genre
        applyGenreFilter()
    }

    private fun applyGenreFilter() {
        val genre = selectedGenre
        val filtered = if (genre == null) allChannels else allChannels.filter {
            it.third.equals(genre, ignoreCase = true)
        }
        channels.clear()
        channels.addAll(filtered)
    }

    var selectedGenre by mutableStateOf<String?>(null)
        private set

    fun extractGenresFromChannels(): List<String> {
        println("📺 Genre extraction from ${allChannels.size} channels")
        return allChannels.map { it.third }.distinct().sorted()
    }

    val filteredChannels: List<Triple<String, String, String>>
        get() = channels

}


