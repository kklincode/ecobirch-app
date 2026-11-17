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
 * Interactive morphological calibration canvas:
 * - Pinch-to-zoom (1x..5x) + two-finger pan + spacious zoom buttons
 * - Generous touch target matching closest landmark without jumping
 * - Live 2.6x Circular Magnifying Loupe rendered opposite the dragged landmark
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
        // Dark Studio Zoom Bar + Calibration Viewport
        Surface(
            color = EcoColors.Slate950,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = EcoColors.Emerald900,
                            shape = RoundedCornerShape(99.dp)
                        ) {
                            Text(
                                text = "${"%.1f".format(viewZoom)}×",
                                color = EcoColors.Emerald100,
                                fontSize = 12.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = activeLandmark.shortTitle,
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                viewZoom = (viewZoom - 0.5f).coerceAtLeast(1f)
                                if (viewZoom == 1f) viewPan = Offset.Zero
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Уменьшить", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        IconButton(
                            onClick = { viewZoom = (viewZoom + 0.5f).coerceAtMost(5f) },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Приблизить", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        IconButton(
                            onClick = {
                                viewZoom = 1f
                                viewPan = Offset.Zero
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(Icons.Default.CenterFocusStrong, contentDescription = "Сброс вида", tint = EcoColors.Emerald100, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val hitRadiusPx = with(density) { 36.dp.toPx() }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(clampedAspect)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF050E0A))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
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
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Landmark Selector Chips + 4-way Micro-Nudge Card
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(22.dp),
            shadowElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(LandmarkKey.entries) { key ->
                        val selected = key == activeLandmark
                        val dotColor = landmarkColor(key)
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectLandmark(key) },
                            shape = RoundedCornerShape(99.dp),
                            label = {
                                Text(
                                    text = key.shortTitle,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EcoColors.Emerald100,
                                selectedLabelColor = EcoColors.Emerald900
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val curPt = points[activeLandmark]
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = activeLandmark.fullTitle,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoColors.Slate900
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "X ${"%.1f".format(curPt.x)}% · Y ${"%.1f".format(curPt.y)}%  (шаг 0.3%)",
                            fontSize = 12.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = EcoColors.Slate500
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        NudgeButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Влево") { onNudgeActive(-0.3f, 0f) }
                        NudgeButton(Icons.Default.KeyboardArrowUp, "Вверх") { onNudgeActive(0f, -0.3f) }
                        NudgeButton(Icons.Default.KeyboardArrowDown, "Вниз") { onNudgeActive(0f, 0.3f) }
                        NudgeButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Вправо") { onNudgeActive(0.3f, 0f) }
                    }
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
        shape = RoundedCornerShape(12.dp),
        color = EcoColors.SurfaceTonal,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = cd, tint = EcoColors.Slate900, modifier = Modifier.size(22.dp))
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
                .size(124.dp)
                .clip(CircleShape)
                .background(Color(0xFF06150E))
                .border(3.dp, EcoColors.Emerald100, CircleShape)
                .clipToBounds()
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val loupeZoom = 2.6f
                val cx = size.width / 2f
                val cy = size.height / 2f
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

                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(cx - 20f, cy),
                    end = Offset(cx + 20f, cy),
                    strokeWidth = 1.8f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(cx, cy - 20f),
                    end = Offset(cx, cy + 20f),
                    strokeWidth = 1.8f
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            color = EcoColors.Slate950.copy(alpha = 0.90f),
            shape = RoundedCornerShape(99.dp)
        ) {
            Text(
                text = "${activeKey.shortTitle} · ${"%.1f".format(targetPt.x)}, ${"%.1f".format(targetPt.y)}",
                color = Color.White,
                fontSize = 11.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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

    drawLine(
        color = Color(0xFFEF4444),
        start = px(points.base),
        end = px(points.tip),
        strokeWidth = 3f * strokeScale,
        pathEffect = dash
    )

    val veinColor = Color(0xFF4ADE80)
    drawLine(veinColor, px(points.leftVein1Base), px(points.leftVein1End), strokeWidth = 2.6f * strokeScale)
    drawLine(veinColor, px(points.leftVein2Base), px(points.leftVein2End), strokeWidth = 2.6f * strokeScale)
    drawLine(veinColor, px(points.rightVein1Base), px(points.rightVein1End), strokeWidth = 2.6f * strokeScale)
    drawLine(veinColor, px(points.rightVein2Base), px(points.rightVein2End), strokeWidth = 2.6f * strokeScale)

    val leftFoot = LeafBioindicationEngine.projectPointOntoLine(points.leftWidthMax, points.base, points.tip, aspectRatio)
    val rightFoot = LeafBioindicationEngine.projectPointOntoLine(points.rightWidthMax, points.base, points.tip, aspectRatio)
    val widthColor = Color(0xFFFBBF24)
    drawLine(widthColor, px(leftFoot), px(points.leftWidthMax), strokeWidth = 2.2f * strokeScale, pathEffect = dash)
    drawLine(widthColor, px(rightFoot), px(points.rightWidthMax), strokeWidth = 2.2f * strokeScale, pathEffect = dash)

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

        val haloRadius = (if (isActive) 20f else 13f) * invZoom
        val dotRadius = (if (isActive) 10f else 7f) * invZoom

        drawCircle(
            color = color.copy(alpha = if (isActive) 0.42f else 0.24f),
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
            style = Stroke(width = (if (isActive) 2.6f else 1.6f) * invZoom)
        )
    }
}

private fun landmarkColor(key: LandmarkKey): Color = when (key.group) {
    LandmarkGroup.AXIS -> Color(0xFFF43F5E)
    LandmarkGroup.WIDTH -> Color(0xFFFBBF24)
    LandmarkGroup.VEIN_BASE -> Color(0xFFE2E8F0)
    LandmarkGroup.VEIN_END -> Color(0xFF10B981)
}
