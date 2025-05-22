package pl.pw.planair.data

import java.time.LocalDate

// Enum dla zakresów cen
enum class PriceRange {
    ALL,      // Wszystkie wydarzenia (domyślne)
    PAID,     // Płatne
    FREE;     // Bezpłatne

    fun displayName(): String {
        return when (this) {
            ALL -> "Wszystkie"
            PAID -> "Płatne"
            FREE -> "Bezpłatne"
        }
    }
}
// Enum dla typu lokalizacji używanej do filtrowania
enum class LocationType {
    USER_LOCATION,      // Aktualna lokalizacja użytkownika
    MAP_POINT,          // Punkt wybrany na mapie (na przyszłość)
    DEFAULT_LOCATION    // Domyślna lokalizacja (np. centrum Warszawy)
}

// Klasa danych dla wybranego punktu filtrowania lokalizacji
data class FilterLocation(
    val type: LocationType,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val name: String? = null // Nazwa do wyświetlania (np. "Moja lokalizacja", "Plac Defilad")
)

// Glowna klasa danych przechowujaca wszystkie filtry
data class FilterState(
    val category: EventCategory? = null, // Kategoria z IntroScreen (null = wszystkie)
    val radiusKm: Float = 20.0f,         // Promień w kilometrach (domyślnie np. 20 km)
    val priceRange: PriceRange = PriceRange.FREE, // Zakres cen (domyślnie dowolna)
    val startDate: Long? = null,         // Data początkowa zakresu (timestamp, null = brak filtra)
    val endDate: Long? = null,           // Data końcowa zakresu (timestamp, null = brak filtra)
    val filterLocation: FilterLocation = FilterLocation(LocationType.DEFAULT_LOCATION,
        latitude = 52.2297, // Przykładowe centrum Warszawy
        longitude = 21.0122,
        name = "Domyślna lokalizacja (Warszawa)"),
    val showOnlyFavorites: Boolean = false
) {
    // Funkcja pomocnicza do pobierania nazwy kategorii dla nagłówka
    fun getCategoryDisplayName(): String {
        if (showOnlyFavorites) return "Ulubione"
        return category?.name?.replace("_", " ")?.lowercase()?.capitalizeWords() ?: "Wszystkie"
    }
}