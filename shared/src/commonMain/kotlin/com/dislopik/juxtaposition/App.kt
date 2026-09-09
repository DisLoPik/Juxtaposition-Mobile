package com.dislopik.juxtaposition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.dislopik.juxtaposition.data.Juxt
import com.dislopik.juxtaposition.ui.JuxtTheme
import com.dislopik.juxtaposition.ui.Screen
import com.dislopik.juxtaposition.ui.components.LocalSelfPid
import com.dislopik.juxtaposition.ui.rememberNavigator
import com.dislopik.juxtaposition.ui.screens.ComposerScreen
import com.dislopik.juxtaposition.ui.screens.CommunityScreen
import com.dislopik.juxtaposition.ui.screens.HomeScreen
import com.dislopik.juxtaposition.ui.screens.LoginScreen
import com.dislopik.juxtaposition.ui.screens.PostScreen
import com.dislopik.juxtaposition.ui.screens.UserProfileScreen

@Composable
fun App() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .crossfade(true)
            .build()
    }

    JuxtTheme {
        var signedIn by remember { mutableStateOf(Juxt.api.isSignedIn) }

        if (signedIn) {
            SignedInApp(
                onSignOut = {
                    Juxt.api.logout()
                    signedIn = false
                }
            )
        } else {
            LoginScreen(onSignedIn = { signedIn = true })
        }
    }
}

// BackHandler is deprecated in favour of NavigationEventHandler, which this Compose
// version does not ship yet, so it stays until the replacement is available.
@Suppress("DEPRECATION")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SignedInApp(onSignOut: () -> Unit) {
    val navigator = rememberNavigator()

    // Bumped after a successful post so the screen underneath re-reads from the server.
    var refreshKey by remember { mutableStateOf(0) }

    // The signed-in PID is what allows a post to offer Delete instead of Report.
    var selfPid by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(Unit) {
        selfPid = runCatching { Juxt.api.loadSelf()?.pid }.getOrNull()
    }

    BackHandler(enabled = navigator.canGoBack) { navigator.pop() }

    CompositionLocalProvider(LocalSelfPid provides selfPid) {
        when (val screen = navigator.current) {
            Screen.Home -> HomeScreen(
                onOpenPost = { navigator.push(Screen.PostDetail(it)) },
                onOpenCommunity = { id, name -> navigator.push(Screen.CommunityDetail(id, name)) },
                onOpenProfile = { navigator.push(Screen.Profile(it)) },
                onSignOut = onSignOut,
                onAuthLost = onSignOut
            )

            is Screen.CommunityDetail -> CommunityScreen(
                communityId = screen.communityId,
                communityName = screen.communityName,
                refreshKey = refreshKey,
                onBack = { navigator.pop() },
                onOpenPost = { navigator.push(Screen.PostDetail(it)) },
                onOpenProfile = { navigator.push(Screen.Profile(it)) },
                onCompose = { id, name -> navigator.push(Screen.Composer(id, name)) },
                onAuthLost = onSignOut
            )

            is Screen.PostDetail -> PostScreen(
                postId = screen.postId,
                refreshKey = refreshKey,
                onBack = { navigator.pop() },
                onOpenCommunity = { id, name -> navigator.push(Screen.CommunityDetail(id, name)) },
                onOpenProfile = { navigator.push(Screen.Profile(it)) },
                onReply = { communityId, communityName, parentId ->
                    navigator.push(Screen.Composer(communityId, communityName, parentId))
                },
                onAuthLost = onSignOut
            )

            is Screen.Profile -> UserProfileScreen(
                // The signed-in PID resolves to /users/me, the page that reports itself as self.
                pid = screen.pid?.takeIf { it != selfPid },
                refreshKey = refreshKey,
                onBack = { navigator.pop() },
                onOpenPost = { navigator.push(Screen.PostDetail(it)) },
                onOpenCommunity = { id, name -> navigator.push(Screen.CommunityDetail(id, name)) },
                onOpenProfile = { navigator.push(Screen.Profile(it)) },
                onAuthLost = onSignOut
            )

            is Screen.Composer -> ComposerScreen(
                communityId = screen.communityId,
                communityName = screen.communityName,
                parentPostId = screen.parentPostId,
                onBack = { navigator.pop() },
                onPosted = {
                    refreshKey++
                    navigator.pop()
                },
                onAuthLost = onSignOut
            )
        }
    }
}
