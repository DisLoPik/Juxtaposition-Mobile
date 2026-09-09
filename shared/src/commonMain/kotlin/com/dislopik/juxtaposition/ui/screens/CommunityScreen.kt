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
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dislopik.juxtaposition.data.Juxt
import com.dislopik.juxtaposition.data.JuxtAuthException
import com.dislopik.juxtaposition.model.CommunityDetail
import com.dislopik.juxtaposition.model.CommunitySort
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    communityId: String,
    communityName: String?,
    refreshKey: Int,
    onBack: () -> Unit,
    onOpenPost: (String) -> Unit,
    onOpenProfile: (Long) -> Unit,
    onCompose: (String, String?) -> Unit,
    onAuthLost: () -> Unit
) {
    var sort by remember { mutableStateOf(CommunitySort.RECENT) }
    var detail by remember { mutableStateOf<CommunityDetail?>(null) }
    var following by remember { mutableStateOf(false) }
    var followBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val state = remember { PostListState(scope, onAuthLost) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(communityId, refreshKey) {
        runCatching { Juxt.api.communityDetail(communityId) }
            .onSuccess {
                detail = it
                following = it.isFollowing
            }
    }

    LaunchedEffect(communityId, sort, refreshKey) {
        state.load { Juxt.api.communityPosts(communityId, sort) }
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
            onOpenProfile = onOpenProfile,
            onReport = { state.requestReport(it) },
            onDelete = { state.requestDelete(it) }
        )
    }

    PostListDialogs(state)

    val title = detail?.community?.name ?: communityName ?: "Community"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←", style = MaterialTheme.typography.titleLarge) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (detail?.canPost != false) {
                ExtendedFloatingActionButton(
                    onClick = { onCompose(communityId, title) },
                    text = { Text("Post") },
                    icon = { Text("✎") }
                )
            }
        }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            detail?.let { info ->
                CommunityHeader(
                    detail = info,
                    following = following,
                    followBusy = followBusy,
                    onToggleFollow = {
                        if (!followBusy) {
                            followBusy = true
                            scope.launch {
                                try {
                                    following = Juxt.api.toggleFollowCommunity(communityId)
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
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CommunitySort.entries.forEach { entry ->
                    FilterChip(
                        selected = sort == entry,
                        onClick = { sort = entry },
                        label = { Text(entry.title) }
                    )
                }
            }

            when {
                state.loading && state.posts.isEmpty() -> FullScreenLoading()

                state.error != null && state.posts.isEmpty() -> ErrorState(
                    message = state.error!!,
                    onRetry = { state.load { Juxt.api.communityPosts(communityId, sort) } }
                )

                state.posts.isEmpty() -> EmptyState("No posts in this community yet.")

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.posts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            actions = actions,
                            yeahBusy = state.isYeahBusy(post.id),
                            showCommunity = false
                        )
                    }
                    if (state.hasMore) {
                        item {
                            LoadMoreButton(loading = state.loadingMore, onClick = { state.loadMore() })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityHeader(
    detail: CommunityDetail,
    following: Boolean,
    followBusy: Boolean,
    onToggleFollow: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(detail.community.iconUrl, size = 56)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = detail.community.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                val stats = listOfNotNull(
                    detail.followerCount?.let { "$it followers" },
                    detail.postCount?.let { "$it posts" }
                ).joinToString("  ·  ")
                if (stats.isNotBlank()) {
                    Text(
                        text = stats,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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

        detail.description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (!detail.isOpen) {
            Text(
                text = "This community is closed to new posts.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
