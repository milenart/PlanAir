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

import pl.pw.planair.data.Event
import pl.pw.planair.data.EventCategory


@Composable
fun EventDetailsView(
    event: Event,
    modifier: Modifier = Modifier // Ten modyfikator przychodzi z nadrzednego komponentu (MapAndListScreen) i ustala np. wysokosc
    // TODO: Dodaj parametry dla lambd przycisków Ulubione i Nawiguj
) {
    // Glowny Box dla calej zawartosci widoku szczegółów.
    // Zastosuj modyfikator (np. ustawiający wysokość) do tego Boxa.
    Box(
        modifier = modifier.fillMaxWidth() // Panel wypelnia szerokosc, a wysokosc jest ustawiana przez 'modifier'
    ) {
        // Kolumna zawierająca TYTUŁ, OPIS, KATEGORIĘ itp.
        // Ta Kolumna ma padding, ktory nadaje tresci odstep od krawedzi panelu.
        Column(
            modifier = Modifier
                .fillMaxSize() // Kolumna wypelnia caly dostepny rozmiar Boxa
                .padding(16.dp)
        ) {
            // Tytuł wydarzenia
            Text(
                text = event.title ?: "Brak tytułu",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.height(16.dp)) // Odstep po tytule

            // Opis wydarzenia
            Text(
                text = event.description ?: "Brak opisu.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(8.dp))

            // Kategoria wydarzenia
            Text(
                text = "Kategoria: ${event.category.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // TODO: Inne szczegóły i przyciski
        }

        // TODO: Tutaj w przyszlosci mozesz dodac przyciski Ulubione i Nawiguj,
        // uzywajac rowniez align do umieszczenia ich w innych rogach lub w okreslonym miejscu
    }
}