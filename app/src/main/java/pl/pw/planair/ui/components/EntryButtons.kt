package pl.pw.planair.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.pw.planair.data.IntroButtonData

@Composable
fun EntryButtons(
    buttonData: IntroButtonData,
    onInteraction: (IntroButtonData) -> Unit,
    itemWidth: Dp,
    itemHeight: Dp,
    isCentered: Boolean,
    scale: Float,
    swipeUpVelocityThreshold: Float = 1000f,
    modifier: Modifier = Modifier,
    category: String? = null,
    color: Color
) {
    val graphicAlpha by animateFloatAsState(
        targetValue = if (isCentered) 1f else 0.7f,
        animationSpec = tween(durationMillis = 300),
        label = "graphicAlpha"
    )

    val draggableState = remember { DraggableState { } }

    Box(
        modifier = modifier // <-- ZASTOSOWANIE MODYFIKATORA
            .width(itemWidth)
            .height(itemHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Gray)
            .clickable { onInteraction(buttonData) }
            .draggable(
                state = draggableState,
                orientation = Orientation.Vertical,
                onDragStopped = { velocity ->
                    if (velocity < swipeUpVelocityThreshold) {
                        onInteraction(buttonData)
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        buttonData.imageResId?.let { imageResId ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.TopCenter
            ) {
                Image(
                    painter = painterResource(id = imageResId),
                    contentDescription = buttonData.text,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(graphicAlpha)
                )

                Text(
                    text = buttonData.text ?: "",
                    color = color,
                    fontSize = 25.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 12.dp, end = 12.dp)
                        .align(Alignment.TopCenter),
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                )
            }

        }
    }
}