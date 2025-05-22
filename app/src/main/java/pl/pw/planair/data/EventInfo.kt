// pl.pw.planair.data/EventInfo.kt

package pl.pw.planair.data

import java.util.Locale

enum class EventCategory {
    SPORT,
    KULTURA,
    EDUKACJA,
    AKTYWNOSC_SPOLECZNA,
    OTHER
}

// Klasa do reprezentacji zagnieżdżonych współrzędnych w JSON-ie
data class CoordinatesData(
    val type: String?,
    val coordinates: List<Double>? // Zazwyczaj [longitude, latitude]
)

// Klasa do reprezentacji zagnieżdżonej lokalizacji w JSON-ie
data class LocationData(
    val city: String?,
    val district: String?,
    val address: String?,
    val coordinates: CoordinatesData?
)


data class Event(
    val title: String? = null,
    val description: String? = null,
    val date: String? = null, // Zmieniono na String, bo w JSON-ie jest "21-05-2025"
    val start_time: String? = null,
    val source_link: String? = null,
    val image_url: String? = null,
    val category: EventCategory = EventCategory.OTHER,
    val price: String? = null, // Zmieniono na String, bo w JSON-ie jest "0"
    val location: LocationData? = null, // Używamy teraz LocationData
    // Dodałem to pole: Jeśli chcesz filtrować po ulubionych (showOnlyFavorites w FilterState),
    // Twoja klasa Event MUSI mieć pole isFavorite.
    // Musisz również upewnić się, że to pole jest ustawiane (np. z bazy danych ulubionych).
    val isFavorite: Boolean = false
)

fun String.capitalizeWords(): String = split(" ").joinToString(" ") { word ->
    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

fun EventCategory.getDisplayName(): String {
    return when (this) {
        EventCategory.SPORT -> "Sport"
        EventCategory.KULTURA -> "Kultura i Rozrywka" // Zgodnie z JSON
        EventCategory.EDUKACJA -> "Edukacja"
        EventCategory.AKTYWNOSC_SPOLECZNA -> "Aktywność Społeczna"
        EventCategory.OTHER -> "Inne" // Lub "Wszystkie", jeśli OTHER ma takie znaczenie w filtrach
    }
}

// Funkcja rozszerzająca do generowania unikalnego klucza dla Event
fun Event.generateUniqueKey(): String {
    // Używamy pól, które najprawdopodobniej zapewnią unikalność.
    // Upewnij się, że wartości null są obsługiwane, np. przez zastąpienie ich pustym stringiem lub stałą.
    val titlePart = title ?: "no_title"
    val datePart = date ?: "no_date"
    val startTimePart = start_time ?: "no_start_time" // Dodajemy czas rozpoczęcia dla większej unikalności
    return "${titlePart}_${datePart}_${startTimePart}"
}