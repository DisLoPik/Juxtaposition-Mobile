package com.dislopik.juxtaposition.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember

/** Every place the app can be. */
sealed interface Screen {
    data object Home : Screen
    data class CommunityDetail(val communityId: String, val communityName: String?) : Screen
    data class PostDetail(val postId: String) : Screen

    /** A user's page. A null [pid] means the signed-in user, served at /users/me. */
    data class Profile(val pid: Long?) : Screen

    /** Writing a new post in a community, or a reply when [parentPostId] is set. */
    data class Composer(
        val communityId: String,
        val communityName: String?,
        val parentPostId: String? = null
    ) : Screen {
        val isReply: Boolean get() = parentPostId != null
    }
}

/** A minimal back stack. The app has few screens, so this beats pulling in a nav library. */
@Stable
class Navigator(initial: Screen = Screen.Home) {
    private val stack = mutableStateListOf(initial)

    val current: Screen get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1

    fun push(screen: Screen) {
        stack.add(screen)
    }

    fun pop(): Boolean {
        if (!canGoBack) return false
        stack.removeAt(stack.lastIndex)
        return true
    }
}

@Composable
fun rememberNavigator(): Navigator = remember { Navigator() }
