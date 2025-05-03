package pl.pw.planair.data

enum class EventCategory {
    SPORT,
    KULTURA,
    EDUKACJA,
    AKTYWNOSC_SPOLECZNA,
    OTHER
}

data class Event(
    val id: Int,
    val lat: Double,
    val lon: Double,
    val title: String? = null,
    val category: EventCategory = EventCategory.OTHER,
    val description: String? = null

)
