package com.dislopik.juxtaposition.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas as GraphicsCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke as StrokeStyle
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dislopik.juxtaposition.data.Painting
import com.dislopik.juxtaposition.ui.JuxtColors

/** One continuous mark, in painting coordinates (320 x 120). */
data class Stroke(
    val points: List<Offset>,
    val width: Float,
    val isEraser: Boolean
)

enum class DrawTool(val label: String, val width: Float, val isEraser: Boolean) {
    THIN("Thin", 2f, false),
    MEDIUM("Medium", 5f, false),
    THICK("Thick", 10f, false),
    ERASER("Eraser", 14f, true)
}

@Stable
class DrawingState {
    val strokes: SnapshotStateList<Stroke> = mutableStateListOf()
    var tool by mutableStateOf(DrawTool.MEDIUM)

    val isEmpty: Boolean get() = strokes.none { !it.isEraser }

    fun undo() {
        if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex)
    }

    fun clear() = strokes.clear()

    fun replaceWith(other: List<Stroke>) {
        strokes.clear()
        strokes.addAll(other)
    }
}

@Composable
fun rememberDrawingState(): DrawingState = remember { DrawingState() }

/**
 * Renders strokes into the 320x120 bilevel bitmap Juxtaposition expects, and returns its
 * pixels for [Painting.encode].
 */
fun rasterize(strokes: List<Stroke>): IntArray {
    val bitmap = ImageBitmap(Painting.WIDTH, Painting.HEIGHT)
    val canvas = GraphicsCanvas(bitmap)

    val paper = Paint().apply {
        color = Color.White
        style = PaintingStyle.Fill
    }
    canvas.drawRect(0f, 0f, Painting.WIDTH.toFloat(), Painting.HEIGHT.toFloat(), paper)

    val ink = Paint().apply {
        style = PaintingStyle.Stroke
        strokeCap = StrokeCap.Round
        strokeJoin = StrokeJoin.Round
        isAntiAlias = false // the format is bilevel, so smoothing only muddies the edges
    }

    strokes.forEach { stroke ->
        ink.color = if (stroke.isEraser) Color.White else Color.Black
        ink.strokeWidth = stroke.width

        if (stroke.points.size == 1) {
            // A tap still leaves a dot.
            val dot = Paint().apply {
                color = ink.color
                style = PaintingStyle.Fill
                isAntiAlias = false
            }
            canvas.drawCircle(stroke.points.first(), stroke.width / 2f, dot)
        } else if (stroke.points.size > 1) {
            canvas.drawPath(stroke.toPath(), ink)
        }
    }

    val pixels = IntArray(Painting.WIDTH * Painting.HEIGHT)
    bitmap.readPixels(pixels)
    return pixels
}

private fun Stroke.toPath(): Path = Path().apply {
    moveTo(points.first().x, points.first().y)
    for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
}

/** Draws the strokes at [scale], used for both the editor and the composer's preview. */
private fun DrawScope.drawStrokes(strokes: List<Stroke>, scale: Float) {
    strokes.forEach { stroke ->
        val color = if (stroke.isEraser) Color.White else Color.Black
        val width = stroke.width * scale
        if (stroke.points.size == 1) {
            drawCircle(color, radius = width / 2f, center = stroke.points.first() * scale)
        } else if (stroke.points.size > 1) {
            val path = Path().apply {
                moveTo(stroke.points.first().x * scale, stroke.points.first().y * scale)
                for (i in 1 until stroke.points.size) {
                    lineTo(stroke.points[i].x * scale, stroke.points[i].y * scale)
                }
            }
            drawPath(
                path = path,
                color = color,
                style = StrokeStyle(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

/** Read-only thumbnail of a finished drawing. */
@Composable
fun DrawingPreview(strokes: List<Stroke>, modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(Painting.WIDTH.toFloat() / Painting.HEIGHT)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
    ) {
        drawStrokes(strokes, size.width / Painting.WIDTH)
    }
}

/**
 * Full-screen drawing pad. Lives in a dialog so finger drags never fight the composer's
 * scrolling.
 */
@Composable
fun PaintingEditorDialog(
    initial: List<Stroke>,
    onDismiss: () -> Unit,
    onSave: (List<Stroke>) -> Unit
) {
    val state = rememberDrawingState()
    LaunchedEffect(Unit) { state.replaceWith(initial) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(JuxtColors.Background)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Draw",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Drawings are black and white, ${Painting.WIDTH} x ${Painting.HEIGHT}.",
                    style = MaterialTheme.typography.labelSmall,
                    color = JuxtColors.TextSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                DrawingSurface(state)

                Spacer(Modifier.height(16.dp))
                ToolBar(state)
                Spacer(Modifier.height(16.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onSave(state.strokes.toList()) },
                        enabled = !state.isEmpty,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JuxtColors.Purple,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Use drawing")
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawingSurface(state: DrawingState) {
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(Painting.WIDTH.toFloat() / Painting.HEIGHT)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    val scale = size.width / Painting.WIDTH.toFloat()
                    detectTapGestures { offset ->
                        state.strokes.add(
                            Stroke(
                                points = listOf(offset / scale),
                                width = state.tool.width,
                                isEraser = state.tool.isEraser
                            )
                        )
                    }
                }
                .pointerInput(Unit) {
                    val scale = size.width / Painting.WIDTH.toFloat()
                    detectDragGestures(
                        onDragStart = { current = listOf(it / scale) },
                        onDrag = { change, _ -> current = current + change.position / scale },
                        onDragEnd = {
                            if (current.isNotEmpty()) {
                                state.strokes.add(
                                    Stroke(
                                        points = current,
                                        width = state.tool.width,
                                        isEraser = state.tool.isEraser
                                    )
                                )
                            }
                            current = emptyList()
                        },
                        onDragCancel = { current = emptyList() }
                    )
                }
        ) {
            val scale = size.width / Painting.WIDTH
            drawStrokes(state.strokes, scale)
            if (current.isNotEmpty()) {
                drawStrokes(
                    listOf(Stroke(current, state.tool.width, state.tool.isEraser)),
                    scale
                )
            }
        }
    }
}

@Composable
private fun ToolBar(state: DrawingState) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DrawTool.entries.forEach { tool ->
            val selected = state.tool == tool
            Button(
                onClick = { state.tool = tool },
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected) JuxtColors.Purple else JuxtColors.InputField,
                    contentColor = if (selected) Color.White else JuxtColors.TextSecondary
                ),
                modifier = Modifier.weight(1f)
            ) {
                if (tool.isEraser) {
                    Text("Erase", style = MaterialTheme.typography.labelSmall)
                } else {
                    // Show each pen as a dot at its real size.
                    Box(
                        Modifier
                            .size((tool.width * 1.6f).dp.coerceAtLeast(4.dp))
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) Color.White else JuxtColors.TextSecondary)
                    )
                }
            }
        }

        TextButton(onClick = { state.undo() }, enabled = state.strokes.isNotEmpty()) {
            Text("Undo", style = MaterialTheme.typography.labelSmall)
        }
        TextButton(onClick = { state.clear() }, enabled = state.strokes.isNotEmpty()) {
            Text("Clear", style = MaterialTheme.typography.labelSmall)
        }
    }
}
