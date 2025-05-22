// pl.pw.planair.data/LocationData.kt

package pl.pw.planair.data

// Klasa do parsowania obiektu "coordinates" w JSON
data class CoordinatesJson(
    val type: String, // "Point"
    val coordinates: List<Double> // [longitude, latitude]
)

// Klasa do parsowania obiektu "location" w JSON
data class LocationJson(
    val city: String? = null,
    val district: String? = null,
    val address: String? = null,
    val coordinates: CoordinatesJson? = null
)