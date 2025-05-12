package pl.pw.planair.ui.screen

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pl.pw.planair.data.IntroButtonData
import pl.pw.planair.ui.components.EntryButtons
import pl.pw.planair.ui.components.EntryButtonsIndicator
import kotlin.math.abs

@Composable
fun IntroScreen(
    onNavigateToMap: (filter: String?) -> Unit
) {
    // Dane i stan animacji
    val buttonItems = remember {
        listOf(
            IntroButtonData("Sport", "SPORT"),
            IntroButtonData("Kultura", "KULTURA"),
            IntroButtonData("Edukacja", "EDUKACJA"),
            IntroButtonData("Aktywność Społeczna", "AKTYWNOSC_SPOLECZNA"),
        )
    }

    // Rozmiary ekranu i elementów
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val peekWidth = 32.dp // Miejsce na podgląd następnego elementu
    val gapWidth = 16.dp  // Odstęp między elementami
    val itemWidth = screenWidth - (peekWidth * 2) - gapWidth
    val itemHeight = (configuration.screenHeightDp * 0.6f).dp
    Log.d("IntroScreen", "itemWidth: $itemWidth, itemHeight: $itemHeight")

    // Snapowanie - kluczowa część!
    val lazyListState = rememberLazyListState()
    val snapBehavior = rememberSingleStepFlingBehavior(lazyListState)


    // Aktualnie wycentrowany element (dla wskaźnika)
    val currentIndex by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) 0
            else {
                val viewportCenter = layoutInfo.viewportStartOffset + layoutInfo.viewportSize.width / 2
                layoutInfo.visibleItemsInfo
                    .minByOrNull { item -> abs(item.offset + item.size / 2 - viewportCenter) }
                    ?.index ?: 0
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // LazyRow z wymuszonym snapowaniem
        LazyRow(
            state = lazyListState,
            flingBehavior = snapBehavior, // Gwarantuje snapowanie do 1 elementu
            contentPadding = PaddingValues(horizontal = peekWidth),
            horizontalArrangement = Arrangement.spacedBy(gapWidth),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(buttonItems) { index, itemData ->
                // Animacja skalowania dla aktualnego elementu
                val scale by animateFloatAsState(
                    targetValue = if (currentIndex == index) 1f else 0.9f,
                    animationSpec = tween(300)
                )

                EntryButtons(
                    buttonData = itemData,
                    onInteraction = { clickedData ->
                        onNavigateToMap(clickedData.filterCategory)
                    },
                    index = index,
                    animationTrigger = true,
                    itemWidth = itemWidth,
                    itemHeight = itemHeight,
                    isCentered = (currentIndex == index)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Wskaźnik
        EntryButtonsIndicator(
            itemCount = buttonItems.size,
            currentIndex = currentIndex
        )
    }
}

@Composable
fun rememberSingleStepFlingBehavior(
    lazyListState: LazyListState
): FlingBehavior {
    val scope = rememberCoroutineScope()

    return object : FlingBehavior {
        override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
            val currentIndex = lazyListState.firstVisibleItemIndex
            val currentOffset = lazyListState.firstVisibleItemScrollOffset
            val threshold = 50 // Próg czułości: jeśli już przesunięto powyżej tego, to idź dalej

            val targetIndex = when {
                initialVelocity > 0 -> (currentIndex + 1).coerceAtMost(lazyListState.layoutInfo.totalItemsCount - 1)
                initialVelocity < 0 -> {
                    if (currentOffset > threshold) currentIndex // wróć na bieżący
                    else (currentIndex - 1).coerceAtLeast(0)
                }
                else -> {
                    if (currentOffset > threshold) (currentIndex + 1).coerceAtMost(lazyListState.layoutInfo.totalItemsCount - 1)
                    else currentIndex
                }
            }

            scope.launch {
                lazyListState.animateScrollToItem(targetIndex)
            }

            return 0f
        }
    }
}
