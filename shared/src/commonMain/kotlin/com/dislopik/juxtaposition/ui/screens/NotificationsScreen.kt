package com.dislopik.juxtaposition.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dislopik.juxtaposition.data.Juxt
import com.dislopik.juxtaposition.data.JuxtAuthException
import com.dislopik.juxtaposition.model.JuxtNotification
import com.dislopik.juxtaposition.ui.components.Avatar
import com.dislopik.juxtaposition.ui.components.EmptyState
import com.dislopik.juxtaposition.ui.components.ErrorState
import com.dislopik.juxtaposition.ui.components.FullScreenLoading
import com.dislopik.juxtaposition.ui.describeError
import kotlinx.coroutines.launch

@Composable
fun NotificationsContent(
    refreshKey: Int,
    onOpenPost: (String) -> Unit,
    onOpenProfile: (Long) -> Unit,
    onAuthLost: () -> Unit
) {
    var items by remember { mutableStateOf<List<JuxtNotification>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        loading = true
        error = null
        scope.launch {
            try {
                // Opening this page is also what marks notifications read, same as the site.
                items = Juxt.api.notifications()
            } catch (e: JuxtAuthException) {
                onAuthLost()
            } catch (e: Throwable) {
                error = describeError(e)
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(refreshKey) { load() }

    when {
        loading && items.isEmpty() -> FullScreenLoading()
        error != null && items.isEmpty() -> ErrorState(error!!, onRetry = { load() })
        items.isEmpty() -> EmptyState("No notifications yet.")
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(items) { index, notification ->
                NotificationRow(
                    notification = notification,
                    onClick = {
                        val postId = notification.postId
                        val pid = notification.userPid
                        when {
                            postId != null -> onOpenPost(postId)
                            pid != null -> onOpenProfile(pid)
                            else -> Unit
                        }
                    }
                )
                if (index == items.lastIndex) Spacer(Modifier.padding(bottom = 4.dp))
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: JuxtNotification, onClick: () -> Unit) {
    val actionable = notification.postId != null || notification.userPid != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (actionable) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(notification.iconUrl, size = 40)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = notification.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (notification.timeText.isNotBlank()) {
                    Text(
                        text = notification.timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
