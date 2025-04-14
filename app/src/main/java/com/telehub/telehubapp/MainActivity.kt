package com.telehub.telehubapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
            val genres: List<String> by remember { derivedStateOf { viewModel.extractGenresFromChannels() } }

            MaterialTheme {
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("📺 TeleHub IPTV", style = MaterialTheme.typography.titleLarge) }
                        )
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {
                        // 📁 Genres
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(8.dp)
                        ) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { viewModel.selectGenre(null) },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                ) {
                                    Box(modifier = Modifier.padding(12.dp)) {
                                        Text("❌ Clear Filter", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }

                            items(genres) { genre ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            viewModel.selectGenre(genre)
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(modifier = Modifier.padding(12.dp)) {
                                        Text(genre, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }

                        // 📺 Channels
                        LazyColumn(
                            modifier = Modifier
                                .weight(2f)
                                .fillMaxHeight()
                                .padding(8.dp)
                        ) {
                            items(viewModel.filteredChannels) { channel ->
                                val name = channel.first        // or channel.component1()
                                val cmd = channel.second        // or channel.component2()
                                ChannelCard(name = name) {
                                    client.createLink(viewModel.token, cmd) { streamUrl ->
                                        playerManager.play(streamUrl)
                                        setContentView(playerManager.getPlayerView())
                                    }

                                }
                            }
                        }

                        // 🕒 EPG
                        LazyColumn(
                            modifier = Modifier
                                .weight(2f)
                                .fillMaxHeight()
                                .padding(8.dp)
                        ) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { /* TODO: Show more info, maybe future recording? */ },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Now Playing", style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("12:00 - News Hour")
                                        Text("12:30 - Tech Show")
                                        Text("13:00 - Movie Time")
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
                    client.getAllChannels(token) { list ->
                        runOnUiThread {
                            viewModel.updateChannels(list)
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
                .padding(vertical = 4.dp)
                .clickable { onClick() },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.padding(12.dp)) {
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

            setContentView(playerView)

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
