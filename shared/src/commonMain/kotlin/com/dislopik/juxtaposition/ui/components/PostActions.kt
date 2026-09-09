package com.dislopik.juxtaposition.ui.components

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import com.dislopik.juxtaposition.model.Post

/**
 * The signed-in user's PID, so posts can tell which ones belong to that account and offer
 * Delete. Null until the session has been resolved.
 */
val LocalSelfPid = compositionLocalOf<Long?> { null }

/**
 * What a post card can do. Passing one object keeps the call sites readable as the number
 * of actions grows, so a screen can leave out whatever it does not support.
 */
@Immutable
class PostActions(
    val onToggleYeah: (Post) -> Unit,
    val onOpen: ((Post) -> Unit)? = null,
    val onOpenCommunity: ((String, String?) -> Unit)? = null,
    val onOpenProfile: ((Long) -> Unit)? = null,
    val onReport: ((Post) -> Unit)? = null,
    val onDelete: ((Post) -> Unit)? = null
)
