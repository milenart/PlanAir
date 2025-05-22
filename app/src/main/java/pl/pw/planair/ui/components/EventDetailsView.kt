// pl.pw.planair.ui.components/EventDetailsView.kt

package pl.pw.planair.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

import pl.pw.planair.data.Event
import pl.pw.planair.data.EventCategory
import java.text.SimpleDateFormat // Import dla formatowania daty
import java.util.Date // Import dla klasy Date
import java.util.Locale // Import dla Locale

@Composable
fun EventDetailsView(
    event: Event,
    modifier: Modifier = Modifier,
    isFavorite: Boolean,
    onFavoriteClick: (Event) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Nagłówek z tytułem wydarzenia i przyciskiem ulubionych
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Spacer(Modifier.width(8.dp))
            Text(
                text = event.title ?: "Brak tytułu",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            // Przycisk ulubionych (gwiazdka)
            IconButton(onClick = { onFavoriteClick(event) }) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (isFavorite) "Usuń z ulubionych" else "Dodaj do ulubionych",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
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
            text = "Kategoria: ${event.category.name}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        // Lokalizacja (rozwiązanie Unresolved reference: name i @Composable error)
        event.location?.let { loc ->
            // Wyświetlamy adres, miasto i dzielnicę, jeśli dostępne
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
                    // Próbujemy parsować cenę na Double, aby formatować walutę
                    val displayPrice = event.price.toDoubleOrNull()
                    if (displayPrice != null) {
                        Text(
                            text = "Cena: ${String.format("%.2f zł", displayPrice)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // Jeśli nie udało się sparsować na Double, wyświetl surowy string
                        Text(
                            text = "Cena: ${event.price} zł",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (event.date != null) {
                    // Konwersja daty ze Stringa na Long (timestamp) do formatowania
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
                            text = "Data: ${event.date}", // Wyświetl surowy String, jeśli nie udało się sparsować
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
                color = MaterialTheme.colorScheme.primary // Podświetl link
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}