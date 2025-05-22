package pl.pw.planair.ui.filter

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.pw.planair.data.EventCategory
import pl.pw.planair.data.FilterLocation
import pl.pw.planair.data.FilterState
import pl.pw.planair.data.LocationType // Poprawny import z Twojego FilterData.kt
import pl.pw.planair.data.PriceRange
import android.Manifest // Dodaj ten import
import android.content.Context
import android.content.pm.PackageManager // Dodaj ten import
import androidx.core.content.ContextCompat
import pl.pw.planair.data.Event // Dodaj import Event z EventInfo.kt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FilterViewModel(
    application: Application,
    initialFilterStateFromNav: FilterState
) : AndroidViewModel(application) {

    private val _filterState = MutableStateFlow(initialFilterStateFromNav)
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    // Wartości potrzebne do poprawnego działania funkcji resetFilters
    private val categoryForReset: EventCategory?
    private val showOnlyFavoritesForReset: Boolean
    private val filterLocationForReset: FilterLocation

    private val _locationPermissionGranted = MutableStateFlow(false)
    val locationPermissionGranted: StateFlow<Boolean> = _locationPermissionGranted.asStateFlow()

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application.applicationContext)

    init {
        // Zapisujemy wartości z initialFilterStateFromNav, które będą użyte przy resecie
        categoryForReset = initialFilterStateFromNav.category
        showOnlyFavoritesForReset = initialFilterStateFromNav.showOnlyFavorites
        filterLocationForReset = initialFilterStateFromNav.filterLocation
        // radiusForReset = initialFilterStateFromNav.radiusKm // Jeśli chcesz też to zapamiętać

        Log.d("FilterViewModel", "Initialized with filter from NavHost: $initialFilterStateFromNav")
    }

    fun updateCategory(category: EventCategory?) {
        _filterState.update { it.copy(category = category) }
    }

    fun updateRadius(radius: Float) {
        _filterState.update { it.copy(radiusKm = radius) }
    }

    fun updatePriceRange(priceRange: PriceRange) {
        _filterState.update { it.copy(priceRange = priceRange) }
    }

    fun updateStartDate(date: Long?) {
        _filterState.update { it.copy(startDate = date) }
    }

    fun updateEndDate(date: Long?) {
        _filterState.update { it.copy(endDate = date) }
    }

    fun updateFilterLocation(location: FilterLocation) {
        _filterState.update { it.copy(filterLocation = location) }
    }

    fun setLocationPermissionGranted(granted: Boolean) {
        _locationPermissionGranted.value = granted
        // Jeśli uprawnienia zostały przyznane, od razu spróbuj pobrać lokalizację użytkownika
        if (granted) {
            requestUserLocation()
        }
    }

    fun requestUserLocation() {
        if (!checkLocationPermission(getApplication<Application>().applicationContext)) {
            Log.w("FilterViewModel", "Brak uprawnień do lokalizacji. Nie można pobrać lokalizacji użytkownika.")
            // Zapewnij, że stan filtra wróci do domyślnej lokalizacji, jeśli uprawnienia są niewystarczające
            useDefaultLocation()
            return
        }

        viewModelScope.launch {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    Log.d("FilterViewModel", "Pobrano lokalizację: ${it.latitude}, ${it.longitude}")
                    updateFilterLocation(
                        FilterLocation(
                            type = LocationType.USER_LOCATION,
                            latitude = it.latitude,
                            longitude = it.longitude,
                            name = "Moja lokalizacja"
                        )
                    )
                } ?: run {
                    Log.w("FilterViewModel", "Nie udało się pobrać ostatniej lokalizacji użytkownika, używam domyślnej.")
                    useDefaultLocation()
                }
            }.addOnFailureListener { e ->
                Log.e("FilterViewModel", "Błąd podczas pobierania lokalizacji: ${e.message}")
                useDefaultLocation() // Wróć do domyślnej w przypadku błędu
            }
        }
    }

    fun useDefaultLocation() {
        _filterState.update { current ->
            current.copy(filterLocation = FilterLocation(
                type = LocationType.DEFAULT_LOCATION,
                latitude = 52.2297, // Przykładowe centrum Warszawy
                longitude = 21.0122,
                name = "Domyślna lokalizacja (Warszawa)"
            ))
        }
    }

    fun toggleShowOnlyFavorites() {
        _filterState.update { currentState ->
            val newShowFavorites = !currentState.showOnlyFavorites
            currentState.copy(
                showOnlyFavorites = newShowFavorites,
                // Jeśli włączamy "tylko ulubione", resetujemy kategorię
                category = if (newShowFavorites) null else currentState.category
            )
        }
    }

    fun resetFilters() {
        _filterState.update {
            // Resetuj do wartości domyślnych, ALE zachowaj początkową kategorię/ulubione
            FilterState(
                category = categoryForReset,
                showOnlyFavorites = showOnlyFavoritesForReset,
                filterLocation = filterLocationForReset,
                radiusKm = FilterState().radiusKm, // Domyślny promień
                priceRange = FilterState().priceRange, // Domyślny zakres cen
                startDate = null, // Domyślnie brak daty
                endDate = null
            )
        }
        // _locationPermissionGranted.value = false // Czy na pewno chcemy resetować stan uprawnień?
        // Raczej nie, bo uprawnienia są stanem aplikacji, a nie filtra.
        // Chyba że reset filtrów ma też oznaczać "zapomnij o mojej lokalizacji".
        // Na razie zostawmy to bez zmian.
        Log.d("FilterViewModel", "Filtry zresetowane. Nowy stan: ${_filterState.value}")
        // Jeśli po resecie chcemy od razu spróbować pobrać lokalizację użytkownika,
        // jeśli typ lokalizacji to USER_LOCATION i mamy uprawnienia:
        if (_filterState.value.filterLocation.type == LocationType.USER_LOCATION && locationPermissionGranted.value) {
            requestUserLocation()
        } else if (_filterState.value.filterLocation.type == LocationType.DEFAULT_LOCATION && _filterState.value.filterLocation.latitude == null) {
            useDefaultLocation()
        }
    }

    // Funkcja do filtrowania wydarzeń, dostosowana do Twojego EventInfo.kt
    fun filterEvents(allEvents: List<Event>): List<Event> {
        val currentFilter = _filterState.value
        return allEvents.filter { event ->
            // --- Filtrowanie po kategorii ---
            val categoryMatch = if (currentFilter.category == null) true
            else event.category == currentFilter.category

            // --- Filtrowanie po cenie ---
            val priceMatch = when (currentFilter.priceRange) {
                PriceRange.ALL -> true
                PriceRange.FREE -> {
                    event.price.equals("0", ignoreCase = true) ||
                            event.price.equals("bezpłatne", ignoreCase = true) ||
                            event.price.equals("free", ignoreCase = true)
                }
                PriceRange.PAID -> {
                    !(event.price.equals("0", ignoreCase = true) ||
                            event.price.equals("bezpłatne", ignoreCase = true) ||
                            event.price.equals("free", ignoreCase = true))
                }
            }

            // --- Filtrowanie po promieniu (jeśli lokalizacja jest ustawiona i nie jest to domyślna) ---
            val locationMatch = if (currentFilter.filterLocation.type != LocationType.DEFAULT_LOCATION &&
                currentFilter.filterLocation.latitude != null &&
                currentFilter.filterLocation.longitude != null) {

                val eventLat = event.location?.coordinates?.coordinates?.getOrNull(1) // Latitude
                val eventLon = event.location?.coordinates?.coordinates?.getOrNull(0) // Longitude

                if (eventLat != null && eventLon != null) {
                    val filterLat = currentFilter.filterLocation.latitude!!
                    val filterLon = currentFilter.filterLocation.longitude!!
                    val distance = calculateDistance(filterLat, filterLon, eventLat, eventLon)
                    distance <= currentFilter.radiusKm
                } else {
                    false // Brak koordynatów wydarzenia
                }
            } else {
                true // Brak aktywnego filtra lokalizacji lub używana jest domyślna lokalizacja, która nie filtruje
            }

            // --- Filtrowanie po dacie ---
            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val eventDateMillis = try {
                event.date?.let { dateFormat.parse(it)?.time }
            } catch (e: Exception) {
                Log.e("FilterViewModel", "Błąd parsowania daty '${event.date}': ${e.message}")
                null
            }

            val startDateMatch = if (currentFilter.startDate != null) {
                eventDateMillis != null && eventDateMillis >= currentFilter.startDate
            } else {
                true
            }

            val endDateMatch = if (currentFilter.endDate != null) {
                eventDateMillis != null && eventDateMillis <= currentFilter.endDate
            } else {
                true
            }

            // --- Filtrowanie po ulubionych ---
            // Usunięto, ponieważ pole 'isFavorite' nie występuje w Twojej klasie Event.
            // Jeśli chcesz tę funkcjonalność, musisz dodać `val isFavorite: Boolean = false` do Event.kt
            // w EventInfo.kt i dodać logikę ustawiania tego pola (np. z bazy danych ulubionych).
            val favoritesMatch = if (currentFilter.showOnlyFavorites) {
                // Jeśli showOnlyFavorites jest true, ale Event nie ma isFavorite,
                // żadne wydarzenia nie zostaną zwrócone. To wymaga decyzji:
                // a) Dodać `isFavorite` do Event.kt
                // b) Pobrać stan ulubionych z innej bazy danych i sprawdzić tutaj
                // Na razie zakładam, że ta funkcjonalność jest WYŁĄCZONA, jeśli nie ma isFavorite w Event.
                false // Zwróć false, jeśli nie ma sposobu na określenie, czy wydarzenie jest ulubione
            } else {
                true
            }

            categoryMatch && priceMatch && locationMatch && startDateMatch && endDateMatch && favoritesMatch
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0] / 1000 // Konwertuj metry na kilometry
    }

    // Dodana funkcja pomocnicza do sprawdzania uprawnień
    private fun checkLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }
}