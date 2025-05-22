package pl.pw.planair.ui.screens

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
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import pl.pw.planair.R

@Composable
fun IntroScreen(
    onNavigateToMap: (filter: String?) -> Unit
) {
    val buttonItems = remember {
        listOf(
            IntroButtonData("Sport", "SPORT", R.drawable.sport_ekran),
            IntroButtonData("Kultura i rozrywka", "KULTURA", R.drawable.rozrywka_ekran),
            IntroButtonData("Edukacja", "EDUKACJA", R.drawable.edukacja_ekran),
            IntroButtonData("Spotkania i integracje", "AKTYWNOSC_SPOLECZNA", R.drawable.spoleczne_ekran)
        )
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    // --- KLUCZOWE ZMIANY TUTAJ: Uproszczone obliczenia i usunięcie horizontalArrangement z LazyRow ---

    // Szerokość centralnego boxa. Pozostawiamy tu "centralItemFraction" jako główny kontroler widoczności.
    val centralItemFraction = 0.7f // 70% szerokości ekranu
    val itemWidth = screenWidth * centralItemFraction

    // Odstęp (peek) jaki ma być widoczny z każdej strony.
    // Obliczamy go tak, aby suma: (itemWidth + 2 * horizontalPadding) = screenWidth
    // Czyli horizontalPadding = (screenWidth - itemWidth) / 2
    // ALE - jeśli chcemy też odstęp między elementami, to musimy go dodać gdzie indziej (np. w EntryButtons padding)
    val horizontalPadding = (screenWidth - itemWidth) / 2

    val itemHeight = (configuration.screenHeightDp * 0.7f).dp
    Log.d("IntroScreen", "itemWidth: $itemWidth, itemHeight: $itemHeight, horizontalPadding: $horizontalPadding")

    val lazyListState = rememberLazyListState()
    val snapBehavior = rememberSingleStepFlingBehavior(lazyListState)

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
    fun getCategoryColor(category: String?): Color {
        return when (category) {
            "KULTURA" -> Color(0xFF732BC0) // Kultura i rozrywka – fiolet
            "EDUKACJA" -> Color(0xFF2CB7C8) // Edukacja – turkus
            "AKTYWNOSC_SPOLECZNA" -> Color(0xFFC76600) // Społeczne – pomarańczowy
            "SPORT" -> Color(0xFF00720A) // Sport – zielony
            else -> Color.Gray // domyślny
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LazyRow(
            state = lazyListState,
            flingBehavior = snapBehavior,
            contentPadding = PaddingValues(horizontal = horizontalPadding), // To teraz kontroluje widoczność "peek"
            // --- USUNIĘTO horizontalArrangement = Arrangement.spacedBy(gapWidth) ---
            // Odstępy dodamy bezpośrednio w EntryButtons lub poprzez inny modyfikator
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(buttonItems) { index, itemData ->
                val scale by animateFloatAsState(
                    targetValue = if (currentIndex == index) 1f else 0.9f,
                    animationSpec = tween(300),
                    label = "scaleAnimation"
                )

                EntryButtons(
                    buttonData = itemData,
                    onInteraction = { clickedData ->
                        onNavigateToMap(clickedData.filterCategory)
                    },
                    itemWidth = itemWidth,
                    itemHeight = itemHeight,
                    isCentered = (currentIndex == index),
                    scale = scale,
                    modifier = if (index < buttonItems.size - 1) Modifier.padding(end = 16.dp) else Modifier,
                    category = itemData.filterCategory,
                    color = getCategoryColor(itemData.filterCategory),

                )
            }
        }

        Spacer(Modifier.height(16.dp))

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
            val threshold = 50 // Próg czułości

            val targetIndex = when {
                initialVelocity > 0 -> (currentIndex + 1).coerceAtMost(lazyListState.layoutInfo.totalItemsCount - 1)
                initialVelocity < 0 -> {
                    if (currentOffset > threshold) currentIndex
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