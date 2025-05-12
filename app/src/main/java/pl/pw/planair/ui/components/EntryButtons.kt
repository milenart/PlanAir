package pl.pw.planair.ui.components

import androidx.compose.animation.core.animateDpAsState // Do animowania wartości Dp
import androidx.compose.animation.core.animateFloatAsState // Do animowania wartości Float (przezroczystości)
import androidx.compose.animation.core.tween // Do definiowania animacji
import androidx.compose.foundation.background // Do tła elementu
import androidx.compose.foundation.clickable // Do uczynienia elementu klikalnym
import androidx.compose.foundation.layout.* // Do Box, Column, Spacer, padding, width/height
import androidx.compose.foundation.shape.RoundedCornerShape // Opcjonalnie: zaokrąglone rogi
import androidx.compose.material3.Text // Tekst w przycisku
import androidx.compose.runtime.* // Do remember, itp.
import androidx.compose.ui.Alignment // Do wyrównania zawartości
import androidx.compose.ui.Modifier // Do modyfikatorów
import androidx.compose.ui.draw.alpha // Do przezroczystości
import androidx.compose.ui.draw.clip // Do przycięcia do kształtu
import androidx.compose.ui.graphics.Color // Do kolorów
import androidx.compose.ui.unit.dp // Jednostki miary
import androidx.compose.ui.unit.Dp // Typ Dp

// Importy dla gestu draggable
import androidx.compose.foundation.gestures.Orientation // <-- Dodaj import
import androidx.compose.foundation.gestures.draggable // <-- Dodaj import
import androidx.compose.foundation.gestures.rememberDraggableState // <-- Dodaj import

// Import dla placeholderu grafiki (np. ikony)
import androidx.compose.foundation.Image // <-- Dodaj import
import androidx.compose.material.icons.Icons // <-- Dodaj import
import androidx.compose.material.icons.filled.Star // <-- Przykład ikony (zmienisz na swoja grafikę)
import androidx.compose.ui.graphics.vector.rememberVectorPainter // <-- Dodaj import

// ** IMPORT KLASY DANYCH DLA BOXA **
import pl.pw.planair.data.IntroButtonData


@Composable
fun EntryButtons(
    // ** ZMIANA: Przyjmujemy caly obiekt danych przycisku **
    buttonData: IntroButtonData,
    // ** ZMIANA: Jeden wspolny callback na interakcje (klikniecie LUB przesuniecie) **
    onInteraction: (IntroButtonData) -> Unit, // Callback przyjmuje obiekt danych przycisku
    index: Int,
    animationTrigger: Boolean, // Animacja pojawienia sie boxa (przy starcie ekranu)
    itemWidth: Dp, // Szerokosc elementu (obliczona w IntroScreen)
    itemHeight: Dp,
    // ** ZMIANA: Nowy parametr wskazujacy czy ten box jest wycentrowany (dla animacji grafiki) **
    isCentered: Boolean
) {
    // --- Konfiguracja Animacji Pojawienia Sie Boxa (przy starcie ekranu) ---
    val duration = 600 // ms
    val initialDelay = 100 // ms (opóźnienie startu pierwszego elementu)
    val delayPerItem = 100 // ms (dodatkowe opóźnienie dla każdego kolejnego elementu)

    val alpha by animateFloatAsState(
        targetValue = if (animationTrigger) 1f else 0f,
        animationSpec = tween(
            durationMillis = duration,
            delayMillis = initialDelay + index * delayPerItem
        ), label = "boxAlpha_${index}"
    )

    // Animacja przesunięcia poziomego (wsuwanie z boku) przy starcie ekranu
    val slideOffset = 200.dp // Przykladowa wartosc przesuniecia - dostosuj jesli trzeba
    val offsetX by animateDpAsState(
        targetValue = if (animationTrigger) 0.dp else slideOffset,
        animationSpec = tween(
            durationMillis = duration,
            delayMillis = initialDelay + index * delayPerItem
        ), label = "boxOffsetX_${index}"
    )

    // ** ZMIANA: Stan dla modyfikatora draggable **
    // rememberDraggableState przyjmuje onDelta, nie onDrag
    val draggableState = rememberDraggableState(onDelta = { y ->
        // Ten kod jest wykonywany podczas przesuniecia o y pikseli
        // Nie uzywamy y do fizycznego przesuwania Boxa w tym przypadku,
        // ale lambda musi byc poprawnie zdefiniowana
    })
    // Próg prędkości swipe w górę
    val swipeUpVelocityThreshold = -500f // ** PRÓG PRĘDKOŚCI ** - dostosuj jesli trzeba

    // ** ZMIANA: Animacja grafiki wewnatrz Boxa triggered by isCentered **
    val graphicAlpha by animateFloatAsState(
        targetValue = if (isCentered) 1f else 0.4f, // Grafika widoczna gdy wycentrowany, lekko przezroczysta gdy nie
        animationSpec = tween(durationMillis = 300), // Czas animacji grafiki
        label = "graphicAlpha_${index}"
    )


    Box(
        modifier = Modifier
            // ** ZMIANA: Ustawiamy szerokosc i wysokosc przekazane jako parametry **
            .width(itemWidth)
            .height(itemHeight)

            // ** ZMIANA: USUN ZEWNETRZNY PADDING **
            // Usuwamy padding, ktory wczesniej dzialal jak margines.
            // Odstepy miedzy elementami sa teraz obslugiwane przez horizontalArrangement w LazyRow.
            // .padding(8.dp) // <--- USUN LUB ZAKOMENTUJ TE LINIE

            .alpha(alpha) // Animacja pojawienia sie boxa
            .offset(x = offsetX) // Animacja wsuwania boxa

            .clip(RoundedCornerShape(8.dp)) // Zaokraglone rogi
            .background(Color.Gray.copy(alpha = 0.5f)) // Tlo boxa

            // ** ZMIANA: Zmieniamy modyfikatory klikalnosci i draggable, aby wywolywaly nowy callback **
            // Zazwyczaj clickable przed draggable, jesli oba gesty moga wystapic
            .clickable { onInteraction(buttonData) } // Klikniecie wywoluje onInteraction z danymi
            .draggable(
                state = draggableState,
                orientation = Orientation.Vertical, // Tylko przesuniecia pionowe
                onDragStopped = { velocity -> // Reaguj po zakonczeniu przesuniecia
                    if (velocity < swipeUpVelocityThreshold) {
                        onInteraction(buttonData) // Przesuniecie w gore wywoluje onInteraction z danymi
                    }
                }
            )
            .padding(16.dp), // Wewnetrzne wypelnienie w Boxie (zachowaj)


        contentAlignment = Alignment.Center // Wyśrodkuj zawartość
    ) {
        // ** ZMIANA: Zawartosc Boxa (tekst i grafika placeholder) **
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = buttonData.text) // Tekst z IntroButtonData
            Spacer(Modifier.height(8.dp))
            // ** ZMIANA: Placeholder grafiki wektorowej z animacja **
            Image(
                painter = rememberVectorPainter(image = Icons.Filled.Star), // <-- Zmien na swoja grafike VectorDrawable/SVG/Lottie
                contentDescription = buttonData.text, // Opis dla dostepnosci
                modifier = Modifier
                    .size(64.dp) // <-- Dostosuj rozmiar
                    .alpha(graphicAlpha) // <-- Zastosuj animowana przezroczystosc (triggered by isCentered)
            )
        }
    }
}