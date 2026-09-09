package com.dislopik.juxtaposition.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dislopik.juxtaposition.data.Juxt
import com.dislopik.juxtaposition.data.JuxtAuthException
import com.dislopik.juxtaposition.model.ProfileTab
import com.dislopik.juxtaposition.model.UserProfile
import com.dislopik.juxtaposition.ui.JuxtColors
import com.dislopik.juxtaposition.ui.PostListDialogs
import com.dislopik.juxtaposition.ui.PostListState
import com.dislopik.juxtaposition.ui.components.Avatar
import com.dislopik.juxtaposition.ui.components.EmptyState
import com.dislopik.juxtaposition.ui.components.ErrorState
import com.dislopik.juxtaposition.ui.components.FullScreenLoading
import com.dislopik.juxtaposition.ui.components.LoadMoreButton
import com.dislopik.juxtaposition.ui.components.PostActions
import com.dislopik.juxtaposition.ui.components.PostCard
import com.dislopik.juxtaposition.ui.describeError
import kotlinx.coroutines.launch

/**
 * A user's page. [pid] is null for the signed-in user's profile, served at /users/me.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    pid: Long?,
    refreshKey: Int,
    onBack: () -> Unit,
    onOpenPost: (String) -> Unit,
    onOpenCommunity: (String, String?) -> Unit,
    onOpenProfile: (Long) -> Unit,
    onAuthLost: () -> Unit
) {
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var profileError by remember { mutableStateOf<String?>(null) }
    var following by remember { mutableStateOf(false) }
    var followBusy by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(ProfileTab.POSTS) }
    // The profile request already returns the Posts tab, so skip the tab effect's first run.
    var tabEffectPrimed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val state = remember { PostListState(scope, onAuthLost) }
    val snackbarHostState = remember { SnackbarHostState() }

    // The profile page already carries the first page of posts, so load both together.
    LaunchedEffect(pid, refreshKey) {
        profileError = null
        try {
            val (loaded, firstPage) = Juxt.api.userProfile(pid)
            profile = loaded
            following = loaded.isFollowing
            state.setPage(firstPage)
        } catch (e: JuxtAuthException) {
            onAuthLost()
        } catch (e: Throwable) {
            profileError = describeError(e)
        }
    }

    // Switching tabs fetches that list on its own.
    LaunchedEffect(tab) {
        if (!tabEffectPrimed) {
            tabEffectPrimed = true
            return@LaunchedEffect
        }
        state.load { Juxt.api.userPosts(pid, tab) }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            state.clearMessage()
        }
    }

    val actions = remember(state) {
        PostActions(
            onToggleYeah = { state.toggleYeah(it) },
            onOpen = { onOpenPost(it.id) },
            onOpenCommunity = onOpenCommunity,
            onOpenProfile = onOpenProfile,
            onReport = { state.requestReport(it) },
            onDelete = { state.requestDelete(it) }
        )
    }

    PostListDialogs(state)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = profile?.miiName ?: "Profile",
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        when {
            profileError != null -> ErrorState(
                message = profileError!!,
                modifier = Modifier.padding(innerPadding)
            )

            profile == null -> FullScreenLoading(Modifier.padding(innerPadding))

            else -> Column(Modifier.fillMaxSize().padding(innerPadding)) {
                ProfileHeader(
                    profile = profile!!,
                    following = following,
                    followBusy = followBusy,
                    onToggleFollow = {
                        val target = profile!!.pid
                        if (!followBusy) {
                            followBusy = true
                            scope.launch {
                                try {
                                    following = Juxt.api.toggleFollowUser(target)
                                } catch (e: JuxtAuthException) {
                                    onAuthLost()
                                } catch (e: Throwable) {
                                    snackbarHostState.showSnackbar(describeError(e))
                                } finally {
                                    followBusy = false
                                }
                            }
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileTab.entries.forEach { entry ->
                        FilterChip(
                            selected = tab == entry,
                            onClick = { tab = entry },
                            label = { Text(entry.title) }
                        )
                    }
                }

                when {
                    state.loading && state.posts.isEmpty() -> FullScreenLoading()

                    state.error != null && state.posts.isEmpty() -> ErrorState(
                        message = state.error!!,
                        onRetry = { state.load { Juxt.api.userPosts(pid, tab) } }
                    )

                    state.posts.isEmpty() -> EmptyState(
                        if (tab == ProfileTab.POSTS) "No posts yet." else "No Yeahed posts yet."
                    )

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.posts, key = { it.id }) { post ->
                            PostCard(
                                post = post,
                                actions = actions,
                                yeahBusy = state.isYeahBusy(post.id)
                            )
                        }
                        if (state.hasMore) {
                            item {
                                LoadMoreButton(
                                    loading = state.loadingMore,
                                    onClick = { state.loadMore() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    profile: UserProfile,
    following: Boolean,
    followBusy: Boolean,
    onToggleFollow: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(profile.avatarUrl, size = 64)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = profile.miiName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                profile.username?.let {
                    Text(
                        text = "@$it",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!profile.isSelf) {
                if (following) {
                    Button(
                        onClick = onToggleFollow,
                        enabled = !followBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JuxtColors.Purple,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Following")
                    }
                } else {
                    OutlinedButton(onClick = onToggleFollow, enabled = !followBusy) {
                        Text("Follow")
                    }
                }
            }
        }

        profile.comment?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        if (profile.stats.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                profile.stats.forEach { stat ->
                    Column {
                        Text(
                            text = stat.value,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stat.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
