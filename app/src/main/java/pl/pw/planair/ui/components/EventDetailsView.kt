package pl.pw.planair.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color // Dodaj import dla Color

import pl.pw.planair.data.Event
import pl.pw.planair.data.EventCategory // Upewnij sie, ze masz ten import jesli uzywasz EventCategory

// Import ikon dla ulubionych
import androidx.compose.material.icons.filled.Favorite // Ikonka serca (wypełniona)
import androidx.compose.material.icons.filled.FavoriteBorder // Ikonka serca (obramowanie)


@Composable
fun EventDetailsView(
    event: Event,
    modifier: Modifier = Modifier,
    // Dodaj parametry dla lambd przycisków Ulubione i Nawiguj
    // onNavigateClick: (Event) -> Unit = {}, // Placeholder dla przycisku Nawiguj

    // <-- DODANE PARAMETRY DLA ULUBIONYCH (Z KROKU 5.2)
    isFavorite: Boolean, // Informacja, czy wydarzenie jest ulubione
    onFavoriteClick: (Event) -> Unit // Lambda wywoływana po kliknięciu przycisku ulubionych

) {
    Box(
        modifier = modifier
            .fillMaxWidth() // Szerokosc ustawia rodzic
            .padding(horizontal = 16.dp) // Padding horyzontalny dla treści wewnątrz Boxa
    ) {
        // Przycisk Wstecz - umieszczony w lewym górnym rogu Boxa
        // Jego lambda onClick jest pusta, bo obsługa jest w nadrzednym komponencie
        IconButton(
            onClick = { /* Przyciskiem wstecz zarzadza rodzic (MapAndListScreen) */ },
            modifier = Modifier
                .align(Alignment.TopStart) // Umieść w lewym górnym rogu
                .padding(4.dp) // Mały padding
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Wstecz do listy")
        }

        // <-- IKONA/PRZYCISK ULUBIONYCH W PRAWYM GÓRNYM ROGU Boxa (Z KROKU 5.2)
        IconButton(
            onClick = { onFavoriteClick(event) }, // Wywołaj lambda przekazując wydarzenie
            modifier = Modifier
                .align(Alignment.TopEnd) // Umieść w prawym górnym rogu
                .padding(4.dp) // Mały padding
        ) {
            // Wybierz ikonę zależnie od statusu ulubionego (isFavorite)
            val icon = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder
            // Ustaw kolor ikony - np. primary dla ulubionych, domyślny dla nie-ulubionych
            val tint = if (isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current // Kolor ikony
            val contentDescription = if (isFavorite) "Usuń z ulubionych" else "Dodaj do ulubionych" // Opis dostępności

            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint // Ustaw kolor ikony
            )
        }


        // Główny kontener dla reszty szczegółów (tytuł, opis itp.)
        Column(
            modifier = Modifier
                .fillMaxSize() // Wypełnia Boxa
                // Dodaj padding z góry, żeby treść nie nachodziła na przyciski w górnych rogach (Wstecz i Ulubione)
                .padding(top = 48.dp) // Około 48dp lub więcej, żeby zrobić miejsce na przyciski u góry
        ) {
            // Tytuł wydarzenia
            Text(
                text = event.title ?: "Brak tytułu", // Wyświetl tytuł lub placeholder
                style = MaterialTheme.typography.headlineSmall, // Styl tekstu
                fontWeight = FontWeight.Bold, // Czcionka pogrubiona
            )

            Spacer(Modifier.height(16.dp)) // Odstęp po tytule

            // Opis wydarzenia
            Text(
                text = event.description ?: "Brak opisu.", // Wyświetl opis lub placeholder
                style = MaterialTheme.typography.bodyMedium // Styl tekstu
            )

            Spacer(Modifier.height(8.dp)) // Odstęp po opisie

            // Kategoria wydarzenia (opcjonalnie, zależy od Twojej klasy EventCategory)
            // Upewnij sie, ze 'event.category.name' jest poprawne
            Text(
                text = "Kategoria: ${event.category.name}", // Wyświetl nazwę kategorii
                style = MaterialTheme.typography.bodySmall, // Styl tekstu
                color = MaterialTheme.colorScheme.onSurfaceVariant // Kolor tekstu
            )

            // TODO: Dodaj tutaj inne szczegóły wydarzenia (data, czas, miejsce itp.)

            Spacer(Modifier.height(16.dp)) // Odstęp przed przyciskami akcji na dole (TODO)

            // TODO: Tutaj w przyszłości dodasz przycisk Nawiguj
            /*
            Button(onClick = {
                // onNavigateClick(event) // Wywołaj lambda nawigacji
            }) {
                Text("Nawiguj")
            }
             */
        }
    }
}