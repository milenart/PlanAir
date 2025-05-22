// pl.pw.planair.ui.components/EventDetailsView.kt

package pl.pw.planair.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
// import androidx.compose.material.icons.filled.ArrowBack // Nie jest tu używany, ale zostawiam, jeśli planujesz
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Navigation // DODAJ TEN IMPORT
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
// import androidx.compose.ui.graphics.Color // Nie jest bezpośrednio używany, ale może być przez MaterialTheme

import pl.pw.planair.data.Event
// import pl.pw.planair.data.EventCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EventDetailsView(
    event: Event,
    modifier: Modifier = Modifier,
    isFavorite: Boolean,
    onFavoriteClick: (Event) -> Unit,
    onNavigateClick: (Event) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Nagłówek z tytułem wydarzenia oraz przyciskami nawigacji i ulubionych
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically, // Wyrównaj elementy w Row do środka wertykalnie
            // horizontalArrangement = Arrangement.SpaceBetween // Usunięte, bo Column z ikonami będzie na końcu
        ) {
            // Usunięty Spacer(Modifier.width(8.dp)) - niepotrzebny, jeśli tytuł zajmuje dostępną przestrzeń

            Text(
                text = event.title ?: "Brak tytułu",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f) // Tytuł zajmuje dostępną przestrzeń
            )

            // Kolumna dla przycisków akcji (Nawigacja i Ulubione)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally // Wyrównaj ikony w tej kolumnie do środka
            ) {
                // Przycisk Nawigacji (tylko jeśli są współrzędne)
                if (event.location?.coordinates?.coordinates != null && event.location.coordinates.coordinates.size >= 2) {
                    IconButton(onClick = { onNavigateClick(event) }) {
                        Icon(
                            imageVector = Icons.Filled.TurnRight,
                                    contentDescription = "Nawiguj do wydarzenia"
                            // Możesz dodać tint, jeśli chcesz inny kolor
                            // tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Przycisk ulubionych (gwiazdka)
                IconButton(onClick = { onFavoriteClick(event) }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite) "Usuń z ulubionych" else "Dodaj do ulubionych",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Opis wydarzenia
        Text(
            text = event.description ?: "Brak opisu.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(8.dp))

        // Kategoria wydarzenia
        Text(
            text = "Kategoria: ${event.category.name}", // Upewnij się, że event.category nie jest null
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        // Lokalizacja
        event.location?.let { loc ->
            loc.address?.let { address ->
                Text(
                    text = "Adres: $address",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            loc.city?.let { city ->
                Text(
                    text = "Miasto: $city",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            loc.district?.let { district ->
                Text(
                    text = "Dzielnica: $district",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (loc.address != null || loc.city != null || loc.district != null) {
                Spacer(Modifier.height(8.dp))
            }
        }

        // Ceny i data/czas
        if (event.price != null || event.date != null || event.start_time != null) {
            Column {
                if (event.price != null) {
                    val displayPrice = event.price.toDoubleOrNull()
                    if (displayPrice != null) {
                        Text(
                            text = "Cena: ${String.format("%.2f zł", displayPrice)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Cena: ${event.price} zł",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (event.date != null) {
                    val parsedDateMillis: Long? = try {
                        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                        dateFormat.parse(event.date)?.time
                    } catch (e: Exception) {
                        null
                    }

                    if (parsedDateMillis != null) {
                        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                        val formattedDate = dateFormat.format(Date(parsedDateMillis))
                        Text(
                            text = "Data: $formattedDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Data: ${event.date}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (event.start_time != null) {
                    Text(
                        text = "Godzina: ${event.start_time}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Link do źródła
        if (event.source_link != null) {
            Text(
                text = "Źródło: ${event.source_link}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}