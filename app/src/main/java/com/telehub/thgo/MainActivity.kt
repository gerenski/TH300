package com.telehub.thgo.ui

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import com.telehub.thgo.network.ApiClient
import com.telehub.thgo.player.playerManager
import com.telehub.thgo.viewmodels.MainViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*

// ToDo
@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private lateinit var client: ApiClient
    private lateinit var playerManager: playerManager
    private lateinit var player: ExoPlayer
    private val mac = "00:1A:79:00:00:01"
    private val url = "http://test.eurolan.net/stalker_portal/server/load.php"
    private val referer = "http://test.eurolan.net/stalker_portal/c/"
    private val viewModel by viewModels<MainViewModel>()
    // Removed: showControls field is no longer needed

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()
        client = ApiClient(mac, url, referer)
        playerManager = playerManager(this, mac, referer)

        setContent {
            val genres = viewModel.genres
            val dates = viewModel.availableDates
            val selectedDate = viewModel.selectedDate
            val playerView = remember {
                playerManager.getPlayerView().apply {
                    useController = false
                }
            }
            var controlsVisible by remember { mutableStateOf(true) }
            val focusRequester = remember { FocusRequester() }

// Request focus when controls become visible
            LaunchedEffect(controlsVisible) {
                if (controlsVisible) {
                    focusRequester.requestFocus()
                }
            }
            // No sync needed between showControls and controlsVisible anymore

            MaterialTheme {
                Scaffold(
                    topBar = {
                        if (controlsVisible) {
                            CenterAlignedTopAppBar(
                                title = {
                                    Text("\uD83D\uDCFA THGo IPTV", style = MaterialTheme.typography.titleLarge)
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(focusRequester)
                            .focusable()
                            .onKeyEvent {
                                if (it.type == KeyEventType.KeyDown &&
                                    (it.key == Key.Enter || it.key == Key.DirectionCenter)
                                ) {
                                    controlsVisible = !controlsVisible
                                    true
                                } else {
                                    false // Don't consume arrows
                                }
                            }
                            .padding(innerPadding)
                    ) {
                        AndroidView(
                            factory = {
                                playerView.apply {
                                    setOnKeyListener { _, _, event ->
                                        // Let ENTER/DPAD_CENTER events bubble up to Compose
                                        if (event.keyCode == KeyEvent.KEYCODE_ENTER ||
                                            event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                                            false
                                        } else {
                                            true
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        if (controlsVisible) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
                            ) {
                                Row(modifier = Modifier.fillMaxSize()) {
                                    LazyColumn(
                                        modifier = Modifier
                                            .weight(2f)
                                            .heightIn(max = 200.dp)
                                            .padding(2.dp)
                                    ) {
                                        items(genres) { genre ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 1.dp)
                                                    .clickable {
                                                        viewModel.selectGenre(if (genre == "All") null else genre)
                                                    },
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                            ) {
                                                Box(modifier = Modifier.padding(4.dp)) {
                                                    Text(genre, style = MaterialTheme.typography.bodyMedium)
                                                }
                                            }
                                        }
                                    }

                                    LazyColumn(
                                        modifier = Modifier
                                            .focusable()
                                            .focusRequester(focusRequester)
                                            .weight(3f)
                                            .heightIn(max = 452.dp)
                                            .padding(2.dp)
                                    ) {
                                        items(viewModel.filteredChannels) { channel ->
                                            val name = channel.name
                                            val cmd = channel.cmd
                                            val isSelected = viewModel.selectedChannel?.id == channel.id

                                            LaunchedEffect(Unit) {
                                                if (viewModel.selectedChannel == null) {
                                                    viewModel.selectChannel(channel)
                                                }
                                            }
                                            LaunchedEffect(isSelected) {
                                                if (isSelected) {
                                                    viewModel.selectDate(LocalDate.now().toString())
                                                    viewModel.loadEpg(viewModel.token, channel.id, viewModel.selectedDate, client)
                                                }
                                            }

                                            ChannelCard(name = name) {
                                                client.createLink(viewModel.token, cmd) { streamUrl ->
                                                    runOnUiThread {
                                                        playerManager.play(streamUrl)
                                                        controlsVisible = false
                                                    }
                                                }
                                            }

                                            LaunchedEffect(
                                                key1 = viewModel.selectedChannel,
                                                key2 = viewModel.selectedDate
                                            ) {
                                                viewModel.selectedChannel?.let { channel ->
                                                    viewModel.loadEpg(viewModel.token, channel.id, viewModel.selectedDate, client)
                                                }
                                            }
                                        }
                                    }

                                    val today = LocalDate.now()
                                    val datesList = List(9) { today.minusDays(4 - it.toLong()).toString() }
                                    LazyColumn(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(max = 452.dp)
                                            .padding(1.dp)
                                    ) {
                                        items(datesList) { date ->
                                            val displayDate = LocalDate.parse(date)
                                                .format(DateTimeFormatter.ofPattern("dd.MM"))
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

                                    LazyColumn(
                                        modifier = Modifier
                                            .weight(7f)
                                            .heightIn(max = 450.dp)
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
                }
            }

            client.login(
                onSuccess = { token ->
                    viewModel.updateToken(token)
                    client.getProfile(token) {
                        client.getGenres(token) { genreMap ->
                            runOnUiThread {
                                viewModel.updateGenres(genreMap)
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
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            // toggling Compose state must be done inside setContent scope
            return false // let Compose handle this via modifier/onKeyEvent if needed
        }
        return super.onKeyDown(keyCode, event)
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
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playerManager.release()
    }
}
