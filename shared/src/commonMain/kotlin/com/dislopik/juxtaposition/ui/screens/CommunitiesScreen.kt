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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dislopik.juxtaposition.data.Juxt
import com.dislopik.juxtaposition.data.JuxtAuthException
import com.dislopik.juxtaposition.model.Community
import com.dislopik.juxtaposition.ui.components.Avatar
import com.dislopik.juxtaposition.ui.components.EmptyState
import com.dislopik.juxtaposition.ui.components.ErrorState
import com.dislopik.juxtaposition.ui.components.FullScreenLoading
import com.dislopik.juxtaposition.ui.components.juxtTextFieldColors
import com.dislopik.juxtaposition.ui.describeError
import kotlinx.coroutines.launch

@Composable
fun CommunitiesContent(
    refreshKey: Int,
    onOpenCommunity: (String, String?) -> Unit,
    onAuthLost: () -> Unit
) {
    var communities by remember { mutableStateOf<List<Community>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun load() {
        loading = true
        error = null
        scope.launch {
            try {
                communities = Juxt.api.communities()
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

    val visible = remember(communities, query) {
        if (query.isBlank()) communities
        else communities.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }

    when {
        loading && communities.isEmpty() -> FullScreenLoading()
        error != null && communities.isEmpty() -> ErrorState(error!!, onRetry = { load() })
        else -> Column(Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search communities") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = juxtTextFieldColors(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            )

            if (visible.isEmpty()) {
                EmptyState("No communities match \"$query\".")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visible, key = { it.id }) { community ->
                        CommunityRow(community) { onOpenCommunity(community.id, community.name) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityRow(community: Community, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(community.iconUrl, size = 48)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = community.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                community.followersText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
