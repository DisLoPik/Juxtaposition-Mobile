package com.dislopik.juxtaposition.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dislopik.juxtaposition.data.Juxt
import com.dislopik.juxtaposition.data.JuxtAuthException
import com.dislopik.juxtaposition.model.Post
import com.dislopik.juxtaposition.model.PostThread
import com.dislopik.juxtaposition.model.ReportReason
import com.dislopik.juxtaposition.ui.components.ErrorState
import com.dislopik.juxtaposition.ui.components.FullScreenLoading
import com.dislopik.juxtaposition.ui.components.ConfirmDialog
import com.dislopik.juxtaposition.ui.components.PostActions
import com.dislopik.juxtaposition.ui.components.PostCard
import com.dislopik.juxtaposition.ui.components.ReportDialog
import com.dislopik.juxtaposition.ui.describeError
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostScreen(
    postId: String,
    refreshKey: Int,
    onBack: () -> Unit,
    onOpenCommunity: (String, String?) -> Unit,
    onOpenProfile: (Long) -> Unit,
    onReply: (communityId: String, communityName: String?, parentPostId: String) -> Unit,
    onAuthLost: () -> Unit
) {
    var thread by remember { mutableStateOf<PostThread?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var busyYeahs by remember { mutableStateOf<Set<String>>(emptySet()) }
    var reportTarget by remember { mutableStateOf<Post?>(null) }
    var deleteTarget by remember { mutableStateOf<Post?>(null) }
    var actionBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun load() {
        loading = true
        error = null
        scope.launch {
            try {
                thread = Juxt.api.thread(postId)
            } catch (e: JuxtAuthException) {
                onAuthLost()
            } catch (e: Throwable) {
                error = describeError(e)
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(postId, refreshKey) { load() }

    fun toggleYeah(post: Post) {
        if (post.id in busyYeahs) return
        busyYeahs = busyYeahs + post.id
        scope.launch {
            try {
                val result = Juxt.api.toggleYeah(post.id, post.yeahed)
                thread = thread?.let { current ->
                    fun apply(p: Post) =
                        if (p.id == post.id) p.copy(yeahed = result.yeahed, empathyCount = result.count) else p
                    current.copy(
                        post = apply(current.post),
                        replies = current.replies.map(::apply)
                    )
                }
            } catch (e: JuxtAuthException) {
                onAuthLost()
            } catch (e: Throwable) {
                snackbarHostState.showSnackbar(describeError(e))
            } finally {
                busyYeahs = busyYeahs - post.id
            }
        }
    }

    fun sendReport(reason: ReportReason, text: String) {
        val post = reportTarget ?: return
        actionBusy = true
        scope.launch {
            try {
                Juxt.api.reportPost(post.id, reason, text)
                reportTarget = null
                snackbarHostState.showSnackbar("Report sent. Thanks.")
            } catch (e: JuxtAuthException) {
                onAuthLost()
            } catch (e: Throwable) {
                snackbarHostState.showSnackbar(describeError(e))
            } finally {
                actionBusy = false
            }
        }
    }

    fun deletePost() {
        val post = deleteTarget ?: return
        val isMainPost = post.id == thread?.post?.id
        actionBusy = true
        scope.launch {
            try {
                Juxt.api.deletePost(post.id)
                deleteTarget = null
                if (isMainPost) {
                    // The whole thread is gone, so there is nothing left to show.
                    onBack()
                } else {
                    thread = thread?.let { it.copy(replies = it.replies.filterNot { r -> r.id == post.id }) }
                    snackbarHostState.showSnackbar("Reply deleted.")
                }
            } catch (e: JuxtAuthException) {
                onAuthLost()
            } catch (e: Throwable) {
                snackbarHostState.showSnackbar(describeError(e))
            } finally {
                actionBusy = false
            }
        }
    }

    val actions = PostActions(
        onToggleYeah = { toggleYeah(it) },
        onOpenCommunity = onOpenCommunity,
        onOpenProfile = onOpenProfile,
        onReport = { reportTarget = it },
        onDelete = { deleteTarget = it }
    )

    if (reportTarget != null) {
        ReportDialog(
            busy = actionBusy,
            onDismiss = { if (!actionBusy) reportTarget = null },
            onSubmit = { reason, text -> sendReport(reason, text) }
        )
    }
    if (deleteTarget != null) {
        ConfirmDialog(
            title = if (deleteTarget?.id == thread?.post?.id) "Delete post?" else "Delete reply?",
            message = "This removes it from Juxtaposition for everyone. It cannot be undone.",
            confirmLabel = "Delete",
            busy = actionBusy,
            destructive = true,
            onDismiss = { if (!actionBusy) deleteTarget = null },
            onConfirm = { deletePost() }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Post", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←", style = MaterialTheme.typography.titleLarge) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            val current = thread
            if (current != null && current.communityId != null && !current.post.removed) {
                ExtendedFloatingActionButton(
                    onClick = { onReply(current.communityId, current.communityName, current.post.id) },
                    text = { Text("Reply") },
                    icon = { Text("↩") }
                )
            }
        }
    ) { innerPadding ->
        when {
            loading && thread == null -> FullScreenLoading(Modifier.padding(innerPadding))

            error != null -> ErrorState(
                message = error!!,
                onRetry = { load() },
                modifier = Modifier.padding(innerPadding)
            )

            thread != null -> {
                val current = thread!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        PostCard(
                            post = current.post,
                            actions = actions,
                            yeahBusy = current.post.id in busyYeahs
                        )
                    }

                    item {
                        Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                            Text(
                                text = if (current.replies.isEmpty()) {
                                    "No replies yet"
                                } else {
                                    "${current.replies.size} " +
                                        if (current.replies.size == 1) "reply" else "replies"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider(Modifier.padding(top = 6.dp))
                        }
                    }

                    items(current.replies, key = { it.id }) { reply ->
                        PostCard(
                            post = reply,
                            actions = actions,
                            yeahBusy = reply.id in busyYeahs,
                            showCommunity = false
                        )
                    }
                }
            }
        }
    }
}
