package com.dislopik.juxtaposition.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dislopik.juxtaposition.data.Juxt
import com.dislopik.juxtaposition.data.JuxtApi
import com.dislopik.juxtaposition.data.JuxtAuthException
import com.dislopik.juxtaposition.data.Painting
import com.dislopik.juxtaposition.model.Feeling
import com.dislopik.juxtaposition.ui.JuxtColors
import com.dislopik.juxtaposition.ui.components.DrawingPreview
import com.dislopik.juxtaposition.ui.components.PaintingEditorDialog
import com.dislopik.juxtaposition.ui.components.Stroke
import com.dislopik.juxtaposition.ui.components.juxtTextFieldColors
import com.dislopik.juxtaposition.ui.components.rasterize
import com.dislopik.juxtaposition.ui.describeError
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposerScreen(
    communityId: String,
    communityName: String?,
    parentPostId: String?,
    onBack: () -> Unit,
    onPosted: () -> Unit,
    onAuthLost: () -> Unit
) {
    val isReply = parentPostId != null
    var text by remember { mutableStateOf("") }
    var feeling by remember { mutableStateOf(Feeling.NORMAL) }
    var spoiler by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var drawing by remember { mutableStateOf<List<Stroke>>(emptyList()) }
    var editorOpen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val hasDrawing = drawing.any { !it.isEraser }

    fun submit() {
        if (sending || (text.isBlank() && !hasDrawing)) return
        sending = true
        error = null
        scope.launch {
            try {
                // Rasterise on the way out so the drawing is only encoded once, on send.
                val painting = if (hasDrawing) Painting.encode(rasterize(drawing)) else null
                if (parentPostId != null) {
                    Juxt.api.reply(parentPostId, communityId, text.trim(), feeling, spoiler, painting)
                } else {
                    Juxt.api.createPost(communityId, text.trim(), feeling, spoiler, painting)
                }
                onPosted()
            } catch (e: JuxtAuthException) {
                onAuthLost()
            } catch (e: Throwable) {
                error = describeError(e)
            } finally {
                sending = false
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isReply) "Write a reply" else "New post",
                            fontWeight = FontWeight.SemiBold
                        )
                        communityName?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= JuxtApi.MAX_POST_LENGTH) text = it },
                placeholder = { Text(if (isReply) "Write your reply..." else "What's on your mind?") },
                enabled = !sending,
                shape = RoundedCornerShape(10.dp),
                colors = juxtTextFieldColors(),
                modifier = Modifier.fillMaxWidth().height(180.dp)
            )

            Text(
                text = "${text.length} / ${JuxtApi.MAX_POST_LENGTH}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Drawing",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            if (hasDrawing) {
                DrawingPreview(drawing)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { editorOpen = true },
                        enabled = !sending,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Edit drawing")
                    }
                    OutlinedButton(
                        onClick = { drawing = emptyList() },
                        enabled = !sending,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { editorOpen = true },
                    enabled = !sending,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add a drawing")
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Feeling",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Feeling.entries.take(3).forEach { entry ->
                    FilterChip(
                        selected = feeling == entry,
                        onClick = { feeling = entry },
                        label = { Text(entry.label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Feeling.entries.drop(3).forEach { entry ->
                    FilterChip(
                        selected = feeling == entry,
                        onClick = { feeling = entry },
                        label = { Text(entry.label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mark as spoiler",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = spoiler, onCheckedChange = { spoiler = it }, enabled = !sending)
            }

            error?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { submit() },
                enabled = !sending && (text.isNotBlank() || hasDrawing),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (sending) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (isReply) "Send reply" else "Post")
            }
        }
    }

    if (editorOpen) {
        PaintingEditorDialog(
            initial = drawing,
            onDismiss = { editorOpen = false },
            onSave = {
                drawing = it
                editorOpen = false
            }
        )
    }
}
