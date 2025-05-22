// pl.pw.planair.ui.components/EventListItem.kt

package pl.pw.planair.ui.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.pw.planair.data.Event
import pl.pw.planair.data.EventCategory
import pl.pw.planair.data.capitalizeWords
import pl.pw.planair.data.FilterState
import pl.pw.planair.data.getDisplayName

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.text.ParseException

@Composable
fun EventListItem(event: Event,
                  onEventClick: (Event) -> Unit) {

    val formattedDate = event.date?.let { dateString ->
        try {
            val inputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            inputFormat.isLenient = false // Ścisłe parsowanie
            val dateObject = inputFormat.parse(dateString)
            dateObject?.let {
                val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                outputFormat.format(it)
            } ?: dateString // Jeśli parsowanie zwróci null, pokaż oryginalny string
        } catch (e: ParseException) {
            Log.e("EventListItem", "Błąd parsowania daty: '$dateString'", e)
            dateString // W razie błędu pokaż oryginalny string
        }
    } ?: "Brak daty"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onEventClick(event) },

        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = event.title ?: "Brak tytułu", style = MaterialTheme.typography.titleMedium) // Dodany styl dla tytułu
            Text(text = "Data: $formattedDate", style = MaterialTheme.typography.bodySmall)
            if (event.start_time != null) {
                Text(text = "Godzina: ${event.start_time}", style = MaterialTheme.typography.bodySmall)
            }
            event.location?.address?.let { address ->
                Text(text = "Adres: $address", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString() } }
    fun EventCategory.getDisplayNameCapitalized(): String {
        return when (this) {
            EventCategory.SPORT -> "Sport"
            EventCategory.KULTURA -> "Kultura i Rozrywka" // Zgodnie z JSON
            EventCategory.EDUKACJA -> "Edukacja"
            EventCategory.AKTYWNOSC_SPOLECZNA -> "Aktywność Społeczna"
            EventCategory.OTHER -> "Inne"
        }
    }
}