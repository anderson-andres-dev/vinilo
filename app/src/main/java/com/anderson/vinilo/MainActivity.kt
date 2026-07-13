/*
 * Copyright (c) 2026 Vinilo Project
 * MainActivity.kt is part of Vinilo.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.anderson.vinilo

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.anderson.vinilo.library.AlbumDetailScreen
import com.anderson.vinilo.library.ArtistDetailScreen
import com.anderson.vinilo.library.GenreDetailScreen
import com.anderson.vinilo.library.LibraryScreen
import com.anderson.vinilo.library.PlaylistDetailScreen
import com.anderson.vinilo.playback.CompactPlayerBar
import com.anderson.vinilo.playback.NowPlayingScreen
import com.anderson.vinilo.playback.PlaybackViewModel
import com.anderson.vinilo.playback.QueueScreen
import com.anderson.vinilo.search.SearchScreen
import com.anderson.vinilo.settings.SettingsScreen
import com.anderson.vinilo.ui.theme.ViniloTheme
import dagger.hilt.android.AndroidEntryPoint
import org.oxycblt.musikr.Music

@UnstableApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val playbackViewModel: PlaybackViewModel = hiltViewModel()
            val coverAccentColors by playbackViewModel.coverAccentColors.collectAsStateWithLifecycle()
            ViniloTheme(coverAccentColors = coverAccentColors) {
                val navController = rememberNavController()
                val currentRoute =
                    navController.currentBackStackEntryAsState().value?.destination?.route
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (currentRoute != "nowPlaying") {
                            CompactPlayerBar(
                                viewModel = playbackViewModel,
                                onOpenNowPlaying = { navController.navigate("nowPlaying") },
                            )
                        }
                    },
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "library",
                        modifier = Modifier.padding(innerPadding),
                    ) {
                        composable("library") {
                            LibraryScreen(
                                playbackViewModel = playbackViewModel,
                                onOpenSettings = { navController.navigate("settings") },
                                onOpenSearch = { navController.navigate("search") },
                                onOpenAlbum = { uid -> navController.navigate("album/${uid.encoded()}") },
                                onOpenArtist = { uid -> navController.navigate("artist/${uid.encoded()}") },
                                onOpenGenre = { uid -> navController.navigate("genre/${uid.encoded()}") },
                                onOpenPlaylist = { uid -> navController.navigate("playlist/${uid.encoded()}") },
                            )
                        }
                        composable("settings") {
                            SettingsScreen(onBack = { navController.popBackStack() })
                        }
                        composable("search") {
                            SearchScreen(
                                playbackViewModel = playbackViewModel,
                                onBack = { navController.popBackStack() },
                                onOpenAlbum = { uid -> navController.navigate("album/${uid.encoded()}") },
                                onOpenArtist = { uid -> navController.navigate("artist/${uid.encoded()}") },
                                onOpenGenre = { uid -> navController.navigate("genre/${uid.encoded()}") },
                                onOpenPlaylist = { uid -> navController.navigate("playlist/${uid.encoded()}") },
                            )
                        }
                        composable("nowPlaying") {
                            NowPlayingScreen(
                                viewModel = playbackViewModel,
                                onBack = { navController.popBackStack() },
                                onOpenQueue = { navController.navigate("queue") },
                            )
                        }
                        composable("queue") {
                            QueueScreen(
                                viewModel = playbackViewModel,
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable(
                            "album/{uid}",
                            arguments = listOf(navArgument("uid") { type = NavType.StringType }),
                        ) { backStackEntry ->
                            val uid = backStackEntry.decodedUid() ?: return@composable
                            AlbumDetailScreen(
                                uid = uid,
                                playbackViewModel = playbackViewModel,
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable(
                            "artist/{uid}",
                            arguments = listOf(navArgument("uid") { type = NavType.StringType }),
                        ) { backStackEntry ->
                            val uid = backStackEntry.decodedUid() ?: return@composable
                            ArtistDetailScreen(
                                uid = uid,
                                playbackViewModel = playbackViewModel,
                                onBack = { navController.popBackStack() },
                                onOpenAlbum = { albumUid -> navController.navigate("album/${albumUid.encoded()}") },
                            )
                        }
                        composable(
                            "genre/{uid}",
                            arguments = listOf(navArgument("uid") { type = NavType.StringType }),
                        ) { backStackEntry ->
                            val uid = backStackEntry.decodedUid() ?: return@composable
                            GenreDetailScreen(
                                uid = uid,
                                playbackViewModel = playbackViewModel,
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable(
                            "playlist/{uid}",
                            arguments = listOf(navArgument("uid") { type = NavType.StringType }),
                        ) { backStackEntry ->
                            val uid = backStackEntry.decodedUid() ?: return@composable
                            PlaylistDetailScreen(
                                uid = uid,
                                playbackViewModel = playbackViewModel,
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Music.UID.encoded(): String = Uri.encode(toString())

private fun NavBackStackEntry.decodedUid(): Music.UID? =
    arguments?.getString("uid")?.let { Music.UID.fromString(Uri.decode(it)) }
