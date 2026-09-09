package com.dislopik.juxtaposition.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dislopik.juxtaposition.data.Juxt
import com.dislopik.juxtaposition.model.FeedType
import com.dislopik.juxtaposition.ui.PostListDialogs
import com.dislopik.juxtaposition.ui.PostListState
import com.dislopik.juxtaposition.ui.components.EmptyState
import com.dislopik.juxtaposition.ui.components.ErrorState
import com.dislopik.juxtaposition.ui.components.FullScreenLoading
import com.dislopik.juxtaposition.ui.components.LoadMoreButton
import com.dislopik.juxtaposition.ui.components.PostActions
import com.dislopik.juxtaposition.ui.components.PostCard

@Composable
fun FeedContent(
    refreshKey: Int,
    snackbarHostState: SnackbarHostState,
    onOpenPost: (String) -> Unit,
    onOpenCommunity: (String, String?) -> Unit,
    onOpenProfile: (Long) -> Unit,
    onAuthLost: () -> Unit
) {
    var feedType by remember { mutableStateOf(FeedType.FOLLOWING) }
    val scope = rememberCoroutineScope()
    val state = remember { PostListState(scope, onAuthLost) }

    LaunchedEffect(feedType, refreshKey) {
        state.load { Juxt.api.feed(feedType) }
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

    Column(Modifier.fillMaxSize()) {
        PrimaryTabRow(
            selectedTabIndex = FeedType.entries.indexOf(feedType),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            FeedType.entries.forEach { entry ->
                Tab(
                    selected = feedType == entry,
                    onClick = { feedType = entry },
                    text = { Text(entry.title) }
                )
            }
        }

        when {
            state.loading && state.posts.isEmpty() -> FullScreenLoading()

            state.error != null && state.posts.isEmpty() -> ErrorState(
                message = state.error!!,
                onRetry = { state.load { Juxt.api.feed(feedType) } }
            )

            state.posts.isEmpty() -> EmptyState(
                when (feedType) {
                    FeedType.FOLLOWING -> "Nothing here yet. Follow some people and communities to fill your feed."
                    FeedType.PEOPLE -> "No posts from people you follow yet."
                    FeedType.ALL -> "No posts to show right now."
                }
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
                        LoadMoreButton(loading = state.loadingMore, onClick = { state.loadMore() })
                    }
                }
            }
        }
    }
}
