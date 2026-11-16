package ru.ecobirch.fa.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ecobirch.fa.app.domain.math.LeafBioindicationEngine
import ru.ecobirch.fa.app.domain.model.LandmarkGroup
import ru.ecobirch.fa.app.domain.model.LandmarkKey
import ru.ecobirch.fa.app.domain.model.LeafPoint
import ru.ecobirch.fa.app.domain.model.LeafPoints
import ru.ecobirch.fa.app.ui.theme.EcoColors
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Interactive morphological calibration canvas addressing audit precision issues:
 * - Pinch-to-zoom (1x..5x) + two-finger pan + quick zoom buttons
 * - Generous touch target matching closest landmark without jumping
 * - Live 2.5x Circular Magnifying Loupe rendered in corner opposite the dragged landmark
 * - Precision chip bar & 4-way micro-nudge buttons (0.3% step)
 */
@Composable
fun LeafCalibrationCanvas(
    photoBitmap: Bitmap?,
    isTemplate: Boolean,
    aspectRatio: Float,
    points: LeafPoints,
    activeLandmark: LandmarkKey,
    onSelectLandmark: (LandmarkKey) -> Unit,
    onMoveLandmark: (LandmarkKey, LeafPoint) -> Unit,
    onNudgeActive: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var viewZoom by remember { mutableFloatStateOf(1f) }
    var viewPan by remember { mutableStateOf(Offset.Zero) }
    var draggingKey by remember { mutableStateOf<LandmarkKey?>(null) }

    val imageBitmap = remember(photoBitmap) { photoBitmap?.asImageBitmap() }
    val clampedAspect = aspectRatio.coerceIn(0.65f, 1.35f)
    val density = LocalDensity.current

    Column(modifier = modifier.fillMaxWidth()) {
        // Zoom & precision bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(EcoColors.Slate900)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Зум: ${"%.1f".format(viewZoom)}× · Активна: ${activeLandmark.shortTitle}",
                color = EcoColors.Emerald100,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        viewZoom = (viewZoom - 0.5f).coerceAtLeast(1f)
                        if (viewZoom == 1f) viewPan = Offset.Zero
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Уменьшить", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = { viewZoom = (viewZoom + 0.5f).coerceAtMost(5f) },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Приблизить", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = {
                        viewZoom = 1f
                        viewPan = Offset.Zero
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.CenterFocusStrong, contentDescription = "Сброс вида", tint = EcoColors.Emerald100, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Main Interactive Calibration Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(EcoColors.Slate950)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            val hitRadiusPx = with(density) { 34.dp.toPx() }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(clampedAspect)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF07110C))
                    .border(1.dp, EcoColors.Slate700, RoundedCornerShape(16.dp))
                    .clipToBounds()
                    .pointerInput(points, viewZoom, viewPan) {
                        awaitEachGesture {
                            val firstDown = awaitFirstDown(requireUnconsumed = false)
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            val center = Offset(w / 2f, h / 2f)

                            fun screenToPercent(screenPos: Offset): LeafPoint {
                                val unpanned = (screenPos - center - viewPan) / viewZoom + center
                                return LeafPoint(
                                    x = ((unpanned.x / w) * 100f).coerceIn(1f, 99f),
                                    y = ((unpanned.y / h) * 100f).coerceIn(1f, 99f)
                                )
                            }

                            fun percentToScreen(pt: LeafPoint): Offset {
                                val base = Offset(pt.x / 100f * w, pt.y / 100f * h)
                                return (base - center) * viewZoom + center + viewPan
                            }

                            // Find nearest landmark within touch hit radius
                            var bestKey: LandmarkKey? = null
                            var bestDist = Float.MAX_VALUE
                            for (key in LandmarkKey.entries) {
                                val pScreen = percentToScreen(points[key])
                                val dist = hypot(pScreen.x - firstDown.position.x, pScreen.y - firstDown.position.y)
                                if (dist <= hitRadiusPx && dist < bestDist) {
                                    bestDist = dist
                                    bestKey = key
                                }
                            }

                            if (bestKey != null) {
                                draggingKey = bestKey
                                onSelectLandmark(bestKey)
                                firstDown.consume()
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val activePointers = event.changes.filter { it.pressed }
                                    if (activePointers.isEmpty()) break
                                    if (activePointers.size >= 2) {
                                        // Allow pinch-zoom anytime even if a finger started on a point
                                        draggingKey = null
                                        val zoomDelta = event.calculateZoom()
                                        val panDelta = event.calculatePan()
                                        val newZoom = (viewZoom * zoomDelta).coerceIn(1f, 5f)
                                        viewZoom = newZoom
                                        val maxPanX = (w * (newZoom - 1f)) / 2f
                                        val maxPanY = (h * (newZoom - 1f)) / 2f
                                        viewPan = if (newZoom <= 1.01f) Offset.Zero else Offset(
                                            (viewPan.x + panDelta.x).coerceIn(-maxPanX, maxPanX),
                                            (viewPan.y + panDelta.y).coerceIn(-maxPanY, maxPanY)
                                        )
                                        event.changes.forEach { it.consume() }
                                    } else {
                                        val ch = activePointers.first()
                                        if (ch.positionChanged()) {
                                            onMoveLandmark(bestKey, screenToPercent(ch.position))
                                            ch.consume()
                                        }
                                    }
                                }
                                draggingKey = null
                            } else {
                                // Pan / Pinch zoom background
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val activePointers = event.changes.filter { it.pressed }
                                    if (activePointers.isEmpty()) break
                                    val zoomDelta = event.calculateZoom()
                                    val panDelta = event.calculatePan()
                                    val newZoom = (viewZoom * zoomDelta).coerceIn(1f, 5f)
                                    viewZoom = newZoom
                                    val maxPanX = (w * (newZoom - 1f)) / 2f
                                    val maxPanY = (h * (newZoom - 1f)) / 2f
                                    viewPan = if (newZoom <= 1.01f) Offset.Zero else Offset(
                                        (viewPan.x + panDelta.x).coerceIn(-maxPanX, maxPanX),
                                        (viewPan.y + panDelta.y).coerceIn(-maxPanY, maxPanY)
                                    )
                                    if (activePointers.size >= 2 || viewZoom > 1.01f) {
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                        }
                    }
            ) {
                // Transformed image + vector overlay
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = viewZoom
                            scaleY = viewZoom
                            translationX = viewPan.x
                            translationY = viewPan.y
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    if (imageBitmap != null && !isTemplate) {
                        drawImage(
                            image = imageBitmap,
                            dstOffset = IntOffset.Zero,
                            dstSize = IntSize(w.roundToInt(), h.roundToInt())
                        )
                    } else {
                        drawTemplateLeafSilhouette(w, h)
                    }

                    drawMorphometryVectors(
                        points = points,
                        aspectRatio = clampedAspect,
                        w = w,
                        h = h,
                        strokeScale = 1f / viewZoom
                    )
                    drawLandmarks(
                        points = points,
                        activeKey = activeLandmark,
                        w = w,
                        h = h,
                        invZoom = 1f / viewZoom
                    )
                }

                // Live 2.5x Precision Magnifying Loupe while dragging any landmark
                val activeDrag = draggingKey
                if (activeDrag != null) {
                    val targetPt = points[activeDrag]
                    val loupeAlignment = if (targetPt.x < 50f && targetPt.y < 50f) {
                        Alignment.TopEnd
                    } else {
                        Alignment.TopStart
                    }
                    MagnifyingLoupe(
                        targetPt = targetPt,
                        imageBitmap = if (!isTemplate) imageBitmap else null,
                        points = points,
                        aspectRatio = clampedAspect,
                        activeKey = activeDrag,
                        modifier = Modifier
                            .align(loupeAlignment)
                            .padding(10.dp)
                    )
                }
            }
        }

        // Quick Landmark Selector Chips + 4-way Micro-Nudge Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(vertical = 8.dp)
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(LandmarkKey.entries) { key ->
                    val selected = key == activeLandmark
                    val dotColor = landmarkColor(key)
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectLandmark(key) },
                        label = {
                            Text(
                                text = key.shortTitle,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EcoColors.Emerald50,
                            selectedLabelColor = EcoColors.Emerald900
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Micro-nudge D-pad bar for sub-pixel positioning
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val curPt = points[activeLandmark]
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeLandmark.fullTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoColors.Slate900
                    )
                    Text(
                        text = "X: ${"%.1f".format(curPt.x)}% · Y: ${"%.1f".format(curPt.y)}% (шаг 0.3%)",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = EcoColors.Slate500
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    NudgeButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Влево") { onNudgeActive(-0.3f, 0f) }
                    NudgeButton(Icons.Default.KeyboardArrowUp, "Вверх") { onNudgeActive(0f, -0.3f) }
                    NudgeButton(Icons.Default.KeyboardArrowDown, "Вниз") { onNudgeActive(0f, 0.3f) }
                    NudgeButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Вправо") { onNudgeActive(0.3f, 0f) }
                }
            }
        }
    }
}

@Composable
private fun NudgeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    cd: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = EcoColors.Slate100,
        border = androidx.compose.foundation.BorderStroke(1.dp, EcoColors.Slate200),
        modifier = Modifier.size(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = cd, tint = EcoColors.Slate900, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun MagnifyingLoupe(
    targetPt: LeafPoint,
    imageBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    points: LeafPoints,
    aspectRatio: Float,
    activeKey: LandmarkKey,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(Color(0xFF06150E))
                .border(2.5.dp, EcoColors.Emerald100, CircleShape)
                .clipToBounds()
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val loupeZoom = 2.6f
                val cx = size.width / 2f
                val cy = size.height / 2f
                // Render virtual canvas centered on targetPt
                val virtW = size.width * 2.5f
                val virtH = size.height * 2.5f
                val targetX = (targetPt.x / 100f) * virtW
                val targetY = (targetPt.y / 100f) * virtH

                drawContext.canvas.save()
                drawContext.canvas.translate(cx - targetX * loupeZoom, cy - targetY * loupeZoom)
                drawContext.canvas.scale(loupeZoom, loupeZoom)

                if (imageBitmap != null) {
                    drawImage(
                        image = imageBitmap,
                        dstOffset = IntOffset.Zero,
                        dstSize = IntSize(virtW.roundToInt(), virtH.roundToInt())
                    )
                } else {
                    drawTemplateLeafSilhouette(virtW, virtH)
                }
                drawMorphometryVectors(points, aspectRatio, virtW, virtH, strokeScale = 0.7f)
                drawLandmarks(points, activeKey, virtW, virtH, invZoom = 0.55f)
                drawContext.canvas.restore()

                // Precision crosshair in the center of the loupe
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(cx - 18f, cy),
                    end = Offset(cx + 18f, cy),
                    strokeWidth = 1.6f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(cx, cy - 18f),
                    end = Offset(cx, cy + 18f),
                    strokeWidth = 1.6f
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            color = EcoColors.Slate950.copy(alpha = 0.88f),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = "${activeKey.shortTitle} (${"%.1f".format(targetPt.x)}, ${"%.1f".format(targetPt.y)})",
                color = Color.White,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

private fun DrawScope.drawTemplateLeafSilhouette(w: Float, h: Float) {
    val path = Path().apply {
        moveTo(w * 0.50f, h * 0.88f)
        cubicTo(w * 0.35f, h * 0.88f, w * 0.10f, h * 0.72f, w * 0.08f, h * 0.52f)
        cubicTo(w * 0.06f, h * 0.32f, w * 0.28f, h * 0.20f, w * 0.50f, h * 0.08f)
        cubicTo(w * 0.72f, h * 0.20f, w * 0.94f, h * 0.32f, w * 0.92f, h * 0.52f)
        cubicTo(w * 0.90f, h * 0.72f, w * 0.65f, h * 0.88f, w * 0.50f, h * 0.88f)
        close()
    }
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF15803D), Color(0xFF166534), Color(0xFF052E16))
        )
    )
    drawPath(path = path, color = Color(0xFF22C55E), style = Stroke(width = 2.5f))
}

private fun DrawScope.drawMorphometryVectors(
    points: LeafPoints,
    aspectRatio: Float,
    w: Float,
    h: Float,
    strokeScale: Float
) {
    fun px(pt: LeafPoint) = Offset(pt.x / 100f * w, pt.y / 100f * h)

    val dash = PathEffect.dashPathEffect(floatArrayOf(8f * strokeScale, 5f * strokeScale), 0f)
    val dot = PathEffect.dashPathEffect(floatArrayOf(3f * strokeScale, 4f * strokeScale), 0f)

    // Central midrib axis
    drawLine(
        color = Color(0xFFEF4444),
        start = px(points.base),
        end = px(points.tip),
        strokeWidth = 3f * strokeScale,
        pathEffect = dash
    )

    // Lateral veins 1 & 2 (left and right)
    val veinColor = Color(0xFF4ADE80)
    drawLine(veinColor, px(points.leftVein1Base), px(points.leftVein1End), strokeWidth = 2.6f * strokeScale)
    drawLine(veinColor, px(points.leftVein2Base), px(points.leftVein2End), strokeWidth = 2.6f * strokeScale)
    drawLine(veinColor, px(points.rightVein1Base), px(points.rightVein1End), strokeWidth = 2.6f * strokeScale)
    drawLine(veinColor, px(points.rightVein2Base), px(points.rightVein2End), strokeWidth = 2.6f * strokeScale)

    // Orthogonal width perpendiculars onto central axis (Trait 1)
    val leftFoot = LeafBioindicationEngine.projectPointOntoLine(points.leftWidthMax, points.base, points.tip, aspectRatio)
    val rightFoot = LeafBioindicationEngine.projectPointOntoLine(points.rightWidthMax, points.base, points.tip, aspectRatio)
    val widthColor = Color(0xFFFBBF24)
    drawLine(widthColor, px(leftFoot), px(points.leftWidthMax), strokeWidth = 2.2f * strokeScale, pathEffect = dash)
    drawLine(widthColor, px(rightFoot), px(points.rightWidthMax), strokeWidth = 2.2f * strokeScale, pathEffect = dash)

    // Trait 4 marginal distances
    val endColor = Color(0xFF38BDF8)
    drawLine(endColor, px(points.leftVein1End), px(points.leftVein2End), strokeWidth = 2f * strokeScale, pathEffect = dot)
    drawLine(endColor, px(points.rightVein1End), px(points.rightVein2End), strokeWidth = 2f * strokeScale, pathEffect = dot)
}

private fun DrawScope.drawLandmarks(
    points: LeafPoints,
    activeKey: LandmarkKey,
    w: Float,
    h: Float,
    invZoom: Float
) {
    for (key in LandmarkKey.entries) {
        val pt = points[key]
        val center = Offset(pt.x / 100f * w, pt.y / 100f * h)
        val isActive = key == activeKey
        val color = landmarkColor(key)

        val haloRadius = (if (isActive) 18f else 12f) * invZoom
        val dotRadius = (if (isActive) 9f else 6.5f) * invZoom

        drawCircle(
            color = color.copy(alpha = if (isActive) 0.40f else 0.24f),
            radius = haloRadius,
            center = center
        )
        drawCircle(
            color = color,
            radius = dotRadius,
            center = center
        )
        drawCircle(
            color = Color.White,
            radius = dotRadius,
            center = center,
            style = Stroke(width = (if (isActive) 2.5f else 1.5f) * invZoom)
        )
    }
}

private fun landmarkColor(key: LandmarkKey): Color = when (key.group) {
    LandmarkGroup.AXIS -> Color(0xFFF43F5E)
    LandmarkGroup.WIDTH -> Color(0xFFFBBF24)
    LandmarkGroup.VEIN_BASE -> Color(0xFFE2E8F0)
    LandmarkGroup.VEIN_END -> Color(0xFF10B981)
}
