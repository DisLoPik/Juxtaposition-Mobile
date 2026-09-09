package com.dislopik.juxtaposition.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dislopik.juxtaposition.data.Juxt
import com.dislopik.juxtaposition.data.JuxtAuthException
import com.dislopik.juxtaposition.data.JuxtException
import androidx.compose.runtime.Composable
import com.dislopik.juxtaposition.model.Post
import com.dislopik.juxtaposition.model.PostPage
import com.dislopik.juxtaposition.model.ReportReason
import com.dislopik.juxtaposition.ui.components.ConfirmDialog
import com.dislopik.juxtaposition.ui.components.ReportDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Turns an exception into something worth showing the user. */
fun describeError(error: Throwable): String = when (error) {
    is JuxtException -> error.message ?: "Something went wrong."
    is JuxtAuthException -> error.message ?: "Signed out."
    else -> "Could not reach Juxtaposition. Check your connection."
}

/**
 * Shared paging/yeah behaviour for every screen that shows a list of posts
 * (the activity feeds, a community and a post's replies).
 */
@Stable
class PostListState(
    private val scope: CoroutineScope,
    private val onAuthLost: () -> Unit
) {
    var posts by mutableStateOf<List<Post>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var loadingMore by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var message by mutableStateOf<String?>(null)

    /** The post a report or delete dialog is currently open for. */
    var reportTarget by mutableStateOf<Post?>(null)
        private set
    var deleteTarget by mutableStateOf<Post?>(null)
        private set
    var actionBusy by mutableStateOf(false)
        private set

    private var nextLink: String? = null
    private var busyYeahs by mutableStateOf<Set<String>>(emptySet())

    val hasMore: Boolean get() = nextLink != null && posts.isNotEmpty()

    fun isYeahBusy(postId: String): Boolean = postId in busyYeahs

    fun load(loader: suspend () -> PostPage) {
        loading = true
        error = null
        scope.launch {
            try {
                val page = loader()
                posts = page.posts
                nextLink = page.nextLink
            } catch (e: JuxtAuthException) {
                onAuthLost()
            } catch (e: Throwable) {
                error = describeError(e)
            } finally {
                loading = false
            }
        }
    }

    /** Adopts a page that came bundled with another request, e.g. a profile page. */
    fun setPage(page: PostPage) {
        posts = page.posts
        nextLink = page.nextLink
        loading = false
        error = null
    }

    fun loadMore() {
        val link = nextLink ?: return
        if (loadingMore) return
        loadingMore = true
        scope.launch {
            try {
                val page = Juxt.api.morePosts(link)
                // The server keeps rendering a "load more" button even on the last page,
                // so an empty result is what actually marks the end of a feed.
                if (page.posts.isEmpty()) {
                    nextLink = null
                } else {
                    val known = posts.mapTo(mutableSetOf()) { it.id }
                    posts = posts + page.posts.filter { it.id !in known }
                    nextLink = page.nextLink
                }
            } catch (e: JuxtAuthException) {
                onAuthLost()
            } catch (e: Throwable) {
                message = describeError(e)
            } finally {
                loadingMore = false
            }
        }
    }

    fun toggleYeah(post: Post) {
        if (post.id in busyYeahs) return
        busyYeahs = busyYeahs + post.id
        scope.launch {
            try {
                val result = Juxt.api.toggleYeah(post.id, post.yeahed)
                update(post.id) { it.copy(yeahed = result.yeahed, empathyCount = result.count) }
            } catch (e: JuxtAuthException) {
                onAuthLost()
            } catch (e: Throwable) {
                message = describeError(e)
            } finally {
                busyYeahs = busyYeahs - post.id
            }
        }
    }

    fun requestReport(post: Post) {
        reportTarget = post
    }

    fun requestDelete(post: Post) {
        deleteTarget = post
    }

    fun dismissDialogs() {
        if (actionBusy) return
        reportTarget = null
        deleteTarget = null
    }

    fun submitReport(reason: ReportReason, text: String) {
        val post = reportTarget ?: return
        actionBusy = true
        scope.launch {
            try {
                Juxt.api.reportPost(post.id, reason, text)
                message = "Report sent. Thanks."
                reportTarget = null
            } catch (e: JuxtAuthException) {
                onAuthLost()
            } catch (e: Throwable) {
                message = describeError(e)
            } finally {
                actionBusy = false
            }
        }
    }

    /** Deletes the post and drops it from the list, so the change is visible immediately. */
    fun confirmDelete(onDeleted: (Post) -> Unit = {}) {
        val post = deleteTarget ?: return
        actionBusy = true
        scope.launch {
            try {
                Juxt.api.deletePost(post.id)
                posts = posts.filterNot { it.id == post.id }
                message = "Post deleted."
                deleteTarget = null
                onDeleted(post)
            } catch (e: JuxtAuthException) {
                onAuthLost()
            } catch (e: Throwable) {
                message = describeError(e)
            } finally {
                actionBusy = false
            }
        }
    }

    fun update(postId: String, transform: (Post) -> Post) {
        posts = posts.map { if (it.id == postId) transform(it) else it }
    }

    fun clearMessage() {
        message = null
    }
}

/** Renders whichever of the report / delete dialogs [state] currently has open. */
@Composable
fun PostListDialogs(state: PostListState, onDeleted: (Post) -> Unit = {}) {
    if (state.reportTarget != null) {
        ReportDialog(
            busy = state.actionBusy,
            onDismiss = { state.dismissDialogs() },
            onSubmit = { reason, text -> state.submitReport(reason, text) }
        )
    }

    if (state.deleteTarget != null) {
        ConfirmDialog(
            title = "Delete post?",
            message = "This removes your post from Juxtaposition for everyone. It cannot be undone.",
            confirmLabel = "Delete",
            busy = state.actionBusy,
            destructive = true,
            onDismiss = { state.dismissDialogs() },
            onConfirm = { state.confirmDelete(onDeleted) }
        )
    }
}
