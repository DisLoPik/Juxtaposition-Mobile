package com.dislopik.juxtaposition.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class HomeTab(val title: String, val glyph: String) {
    FEED("Feed", "🏠"),
    COMMUNITIES("Communities", "👥"),
    NOTIFICATIONS("Alerts", "🔔")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenPost: (String) -> Unit,
    onOpenCommunity: (String, String?) -> Unit,
    onOpenProfile: (Long?) -> Unit,
    onSignOut: () -> Unit,
    onAuthLost: () -> Unit
) {
    var tab by remember { mutableStateOf(HomeTab.FEED) }
    var refreshTick by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (tab) {
                            HomeTab.FEED -> "Juxtaposition"
                            HomeTab.COMMUNITIES -> "Communities"
                            HomeTab.NOTIFICATIONS -> "Notifications"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { refreshTick++ }) {
                        Text("⟳", style = MaterialTheme.typography.titleLarge)
                    }
                    TextButton(onClick = { onOpenProfile(null) }) { Text("Me") }
                    TextButton(onClick = onSignOut) { Text("Sign out") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                HomeTab.entries.forEach { entry ->
                    NavigationBarItem(
                        selected = tab == entry,
                        onClick = { tab = entry },
                        icon = { Text(entry.glyph) },
                        label = { Text(entry.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (tab) {
                HomeTab.FEED -> FeedContent(
                    refreshKey = refreshTick,
                    snackbarHostState = snackbarHostState,
                    onOpenPost = onOpenPost,
                    onOpenCommunity = onOpenCommunity,
                    onOpenProfile = { onOpenProfile(it) },
                    onAuthLost = onAuthLost
                )

                HomeTab.COMMUNITIES -> CommunitiesContent(
                    refreshKey = refreshTick,
                    onOpenCommunity = onOpenCommunity,
                    onAuthLost = onAuthLost
                )

                HomeTab.NOTIFICATIONS -> NotificationsContent(
                    refreshKey = refreshTick,
                    onOpenPost = onOpenPost,
                    onOpenProfile = { onOpenProfile(it) },
                    onAuthLost = onAuthLost
                )
            }
        }
    }
}
