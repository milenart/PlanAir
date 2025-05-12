package pl.pw.planair.ui.components

import androidx.compose.foundation.background // Do tła kropek
import androidx.compose.foundation.layout.* // Do Row, size, padding, fillMaxWidth
import androidx.compose.foundation.shape.CircleShape // Do ksztaltu kropki
import androidx.compose.runtime.Composable // Adnotacja Composable
import androidx.compose.ui.Alignment // Do wyrownania w Row
import androidx.compose.ui.Modifier // Do modyfikatorow
import androidx.compose.ui.draw.clip // Do przyciecia do ksztaltu
import androidx.compose.ui.graphics.Color // Do kolorow
import androidx.compose.ui.unit.dp // Jednostki miary

// Funkcja Composable do wyswietlania wskaznika stron
@Composable
fun EntryButtonsIndicator(
    itemCount: Int, // Calkowita liczba elementow
    currentIndex: Int // Indeks aktualnie widocznego/wycentrowanego elementu (od 0)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth() // Zajmij pelna szerokosc
            .padding(16.dp), // Padding wokol wskaznika
        horizontalArrangement = Arrangement.Center, // Wyśrodkuj kropki poziomo
        verticalAlignment = Alignment.CenterVertically // Wyśrodkuj kropki pionowo w Row
    ) {
        // Tworzymy kropke dla kazdego elementu
        repeat(itemCount) { index -> // Petla od 0 do itemCount - 1
            // Rozmiar i kolor kropki zalezny od tego, czy jej index odpowiada currentIndex
            val indicatorSize = if (index == currentIndex) 10.dp else 8.dp // Zwieksz rozmiar aktualnej kropki
            val indicatorColor = if (index == currentIndex) Color.Gray else Color.LightGray // Zmien kolor aktualnej kropki

            Box(
                modifier = Modifier
                    // ** WAZNA KOLEJNOSC MODYFIKATOROW **
                    .size(indicatorSize) // 1. Najpierw ustaw rozmiar Boxa
                    .clip(CircleShape)   // 2. Potem przytnij go do ksztaltu kola
                    .background(indicatorColor) // 3. Ustaw tlo (kolor kropki)
                    .padding(horizontal = 4.dp) // <--- 4. NA KONCU DODAJ PADDING WOKOL GOTOWej KROPKI
            )
        }
    }
}