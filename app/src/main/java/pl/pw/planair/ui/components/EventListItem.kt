// pl/pw/planair/ui/components/EventListItem.kt (przykładowa ścieżka)

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.pw.planair.data.Event
import pl.pw.planair.data.getDisplayName
import kotlin.text.isNotBlank

@Composable
fun EventListItem(
    event: Event,
    isFavorite: Boolean,
    onEventClick: (Event) -> Unit,
    onFavoriteClick: (Event) -> Unit,
    onNavigateClick: (Event) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clickable { onEventClick(event) }, // Cały element klikalny, aby otworzyć szczegóły
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween // Aby przycisk był po prawej
        ) {
            Column(modifier = Modifier.weight(1f)) { // Kolumna na tekst, aby zajął dostępną przestrzeń
                Text(
                    text = event.title ?: "Brak tytułu",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))

                event.location?.address?.let { address ->
                    if (address.isNotBlank()) {
                        TextWithIcon(
                            icon = Icons.Filled.LocationOn,
                            text = address,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                TextWithIcon(
                    icon = Icons.Filled.CalendarToday, // Użycie ikony kalendarza
                    text = event.date ?: "Brak danych", // Usunięto prefiks "Data: " bo ikona już to sugeruje
                    style = MaterialTheme.typography.bodySmall
                )

                event.start_time?.let { time ->
                    if (time.isNotBlank()) {
                        TextWithIcon(
                            icon = Icons.Filled.Schedule,
                            text = "Godzina: $time",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Ikona Nawigacji (tylko jeśli są współrzędne)
                if (event.location?.coordinates?.coordinates != null && event.location.coordinates.coordinates.size >= 2) {
                    IconButton(onClick = { onNavigateClick(event) }) {
                        Icon(
                            imageVector = Icons.Filled.TurnRight,
                            contentDescription = "Nawiguj"
                        )
                    }
                }

                // Ikona Ulubionych
                IconButton(onClick = { onFavoriteClick(event) }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite) "Usuń z ulubionych" else "Dodaj do ulubionych",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                }
            }
        }
    }
}
@Composable
private fun TextWithIcon( // Możesz zmienić na `internal fun` jeśli chcesz używać w całym module
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = LocalTextStyle.current,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // Ikona jest dekoracyjna, tekst dostarcza kontekstu
            modifier = Modifier.size(16.dp), // Dostosuj rozmiar ikony
            tint = MaterialTheme.colorScheme.onSurfaceVariant // Kolor ikony, aby pasował do tekstu pomocniczego
        )
        Spacer(modifier = Modifier.width(4.dp)) // Odstęp między ikoną a tekstem
        Text(
            text = text,
            style = style,
            // Możesz chcieć, aby kolor tekstu był taki sam jak ikony dla spójności
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = maxLines,
            overflow = overflow
        )
    }
}