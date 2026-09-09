package com.dislopik.juxtaposition.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.dislopik.juxtaposition.model.Post
import com.dislopik.juxtaposition.ui.JuxtColors

/**
 * A single post, used for feed items, community listings, the opened post and its replies.
 */
@Composable
fun PostCard(
    post: Post,
    actions: PostActions,
    yeahBusy: Boolean = false,
    showCommunity: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (post.removed) JuxtColors.SurfaceDark else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            PostHeader(post, actions, showCommunity)

            if (post.removed) {
                Text(
                    text = "This post was removed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 10.dp)
                )
            } else {
                // Only make the body tappable where there is somewhere to go.
                PostBody(post, actions.onOpen?.let { open -> { open(post) } })
            }

            PostButtons(post, actions, yeahBusy)
        }
    }
}

@Composable
private fun PostHeader(post: Post, actions: PostActions, showCommunity: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Avatar(
            url = post.author.avatarUrl,
            size = 42,
            modifier = Modifier.clickable(enabled = actions.onOpenProfile != null && post.author.pid > 0) {
                actions.onOpenProfile?.invoke(post.author.pid)
            }
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = post.author.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.clickable(enabled = actions.onOpenProfile != null && post.author.pid > 0) {
                    actions.onOpenProfile?.invoke(post.author.pid)
                }
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = post.timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                if (showCommunity && post.communityName != null) {
                    Text(
                        text = "  ·  ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = post.communityName,
                        style = MaterialTheme.typography.labelSmall,
                        // The site renders the whole .extra-info line, community link
                        // included, in --text-secondary.
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.clickable(enabled = post.communityId != null) {
                            post.communityId?.let { actions.onOpenCommunity?.invoke(it, post.communityName) }
                        }
                    )
                }
            }
        }

        PostOverflowMenu(post, actions)
    }
}

@Composable
private fun PostOverflowMenu(post: Post, actions: PostActions) {
    val selfPid = LocalSelfPid.current
    val isMine = selfPid != null && selfPid == post.author.pid
    val canDelete = isMine && actions.onDelete != null && !post.removed
    val canReport = !isMine && actions.onReport != null && !post.removed

    if (!canDelete && !canReport) return

    var open by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { open = true }) {
            Text(
                text = "⋯",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (canReport) {
                DropdownMenuItem(
                    text = { Text("Report post") },
                    onClick = {
                        open = false
                        actions.onReport.invoke(post)
                    }
                )
            }
            if (canDelete) {
                DropdownMenuItem(
                    text = { Text("Delete post", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        open = false
                        actions.onDelete.invoke(post)
                    }
                )
            }
        }
    }
}

@Composable
private fun PostBody(post: Post, onOpen: (() -> Unit)?) {
    var spoilerRevealed by remember(post.id) { mutableStateOf(!post.isSpoiler) }

    Column(
        Modifier
            .fillMaxWidth()
            .then(if (onOpen != null) Modifier.clickable { onOpen() } else Modifier)
            .padding(top = 10.dp)
    ) {
        if (!spoilerRevealed) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = { spoilerRevealed = true }) { Text("Show spoiler") }
            }
            return@Column
        }

        post.body?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        post.imageUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .padding(top = if (post.hasText) 10.dp else 0.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}

@Composable
private fun PostButtons(post: Post, actions: PostActions, yeahBusy: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val yeahColor =
            if (post.yeahed) JuxtColors.Yeah
            else MaterialTheme.colorScheme.onSurfaceVariant

        TextButton(
            onClick = { actions.onToggleYeah(post) },
            enabled = !yeahBusy && !post.removed
        ) {
            Text(
                text = if (post.yeahed) "♥" else "♡",
                style = MaterialTheme.typography.titleMedium,
                color = yeahColor
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = post.empathyCount.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = yeahColor
            )
        }

        Text(
            text = "↩ ${post.replyCount}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
