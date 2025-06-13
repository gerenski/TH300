package com.telehub.telehubapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.compose.foundation.clickable
import com.telehub.telehubapp.network.ApiClient
import com.telehub.telehubapp.player.playerManager
import androidx.activity.viewModels
import com.telehub.telehubapp.viewmodels.MainViewModel
import androidx.media3.common.util.UnstableApi
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private lateinit var client: ApiClient
    private lateinit var playerManager: playerManager
    private lateinit var player: ExoPlayer
    private val mac = "00:1A:79:00:00:01"
    private val url = "http://test.eurolan.net/stalker_portal/server/load.php"
    private val referer = "http://test.eurolan.net/stalker_portal/c/"
    private val viewModel by viewModels<MainViewModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()
        client = ApiClient(mac, url, referer)
        playerManager = playerManager(this, mac, referer)

        setContent {
            val genres = viewModel.genres
            val dates = viewModel.availableDates
            val selectedDate = viewModel.selectedDate
            val playerView = remember { playerManager.getPlayerView() }

            MaterialTheme {
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("📺 TeleHub IPTV", style = MaterialTheme.typography.titleLarge) }
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {
                        AndroidView(
                            factory = { playerView },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                        // 📁 Genres
                        LazyColumn(
                            modifier = Modifier
                                .weight(2f)
                                //.fillMaxHeight()
                                .heightIn(max = 200.dp) // or whatever height fits ~10 items
                                .padding(2.dp)//Space from left side of screen
                        ) {
                            items(genres) { genre ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 1.dp)//Space between cards, vertical
                                        .clickable {
                                            viewModel.selectGenre(if (genre == "All") null else genre)
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(modifier = Modifier.padding(4.dp)) { //Vertical size of cards
                                        Text(genre, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }

                        // 📺 Channels
                        LazyColumn(
                            modifier = Modifier
                                .weight(3f)
                                //.fillMaxHeight()
                                .heightIn(max = 452.dp) // or whatever height fits ~10 items
                                .padding(2.dp)
                        ) {
                            items(viewModel.filteredChannels) { channel ->
                                val name = channel.name        // or channel.component1()
                                val cmd = channel.cmd        // or channel.component2()
                                val isSelected = viewModel.selectedChannel?.id == channel.id

                                // Auto-select first channel or preserve selection
                                LaunchedEffect(Unit) {
                                    if (viewModel.selectedChannel == null) {
                                        viewModel.selectChannel(channel)
                                    }
                                }
                                LaunchedEffect(isSelected) {
                                    if (isSelected) {
                                        viewModel.selectDate(LocalDate.now().toString())  // Auto-select today's date
                                        viewModel.loadEpg(viewModel.token, channel.id, viewModel.selectedDate, client)
                                    }
                                }

                                ChannelCard(name = name) {
                                    client.createLink(viewModel.token, cmd) { streamUrl ->
                                        runOnUiThread {
                                            playerManager.play(streamUrl)
                                        }
                                    }

                                }

                                // Update selection and EPG outside of click
                                LaunchedEffect(key1 = viewModel.selectedChannel, key2 = viewModel.selectedDate) {
                                    viewModel.selectedChannel?.let { channel ->
                                        viewModel.loadEpg(viewModel.token, channel.id, viewModel.selectedDate, client)
                                    }
                                }

                            }
                        }
                        val today = LocalDate.now()
                        val dates = List(9) { today.minusDays(4 - it.toLong()).toString() }
                        // 📅 Date
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(max = 452.dp)
                                .padding(1.dp)
                        ) {
                            items(dates) { date ->
                                val displayDate = LocalDate.parse(date).format(DateTimeFormatter.ofPattern("dd.MM"))
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .clickable { viewModel.selectDate(date) },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(modifier = Modifier.padding(13.dp)) {
                                        Text(displayDate, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }


                        // 🕒 EPG
                        LazyColumn(
                            modifier = Modifier
                                .weight(7f)
                                //.fillMaxHeight()
                                .heightIn(max = 450.dp) // or whatever height fits ~10 items
                                .padding(2.dp)
                        ) {
                            items(viewModel.epgItems) { epg ->
                                val (start, end, title) = epg
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("$start - $end", style = MaterialTheme.typography.labelSmall)
                                        Text(title, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }

                        }
                    }
                }
            }

        }

        client.login(
            onSuccess = { token ->
                viewModel.updateToken(token)
                client.getProfile(token) {
                    client.getGenres(token) { genreMap ->
                        runOnUiThread {
                            viewModel.updateGenres(genreMap) // ← this line populates the genres for UI
                        }

                        client.getAllChannels(token, genreMap) { list ->
                            runOnUiThread {
                                viewModel.updateChannels(list)
                            }
                        }
                    }
                }
            },
            onFailure = { error -> println("Login error: $error") }
        )


    }

    @Composable
    fun ChannelCard(name: String, onClick: () -> Unit) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp) //Vertical distance between cards
                .clickable { onClick() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.padding(9.dp)) {
                Text(name, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }

    @UnstableApi
    private fun playStream(url: String) {
        runOnUiThread {
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setUserAgent("Mozilla/5.0 (QtEmbedded; U; Linux; C)")
                .setDefaultRequestProperties(
                    mapOf(
                        "User-Agent" to "Mozilla/5.0 (QtEmbedded; U; Linux; C)",
                        "Referer" to referer,
                        "Cookie" to "mac=$mac; stb_lang=en; timezone=Europe/Sofia",
                        "Accept-Encoding" to "identity"
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

            player.setMediaSource(mediaSource)
            player.prepare()
            player.play()
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        playerManager.release()
    }
}
