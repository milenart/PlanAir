// pl.pw.planair.ui.map.viewmodel/MapViewModel.kt

package pl.pw.planair.ui.map.viewmodel

import android.app.Application
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

import pl.pw.planair.R
import pl.pw.planair.data.Event
import pl.pw.planair.data.EventCategory
import pl.pw.planair.data.FilterLocation
import pl.pw.planair.data.FilterState
import pl.pw.planair.data.LocationType
import pl.pw.planair.data.PriceRange
import pl.pw.planair.data.loadMarkersFromJson // Upewnij się, że to masz

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

const val FAVORITES_FILTER_KEY = "FAVORITES_FILTER"

class MapViewModel(application: Application) : AndroidViewModel(application) {

    // --- STAN (State) ---
    private val _allEvents = MutableStateFlow<List<Event>>(emptyList())
    // Expose as read-only StateFlow
    val allEvents: StateFlow<List<Event>> = _allEvents.asStateFlow()

    // Stan wybranego wydarzenia do wyświetlenia szczegółów
    private val _selectedEvent = MutableStateFlow<Event?>(null)
    val selectedEvent: StateFlow<Event?> = _selectedEvent.asStateFlow()

    // Stan filtra - wszystkie parametry filtrujące
    private val _currentFilterState = MutableStateFlow(FilterState())
    val currentFilterState: StateFlow<FilterState> = _currentFilterState.asStateFlow()
    //Log.d("MapViewModel", "Current filter state: $_currentFilterState")

    val filterState: StateFlow<FilterState> = _currentFilterState.asStateFlow()
    // Zbiór ulubionych wydarzeń (przechowujemy całe obiekty Event)
    private val _favoriteEvents = MutableStateFlow<List<Event>>(emptyList())
    val favoriteEvents: StateFlow<List<Event>> = _favoriteEvents.asStateFlow()

    // Aktualna lokalizacja użytkownika
    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    // Stan uprawnień do lokalizacji
    private val _hasLocationPermission = MutableStateFlow(false)
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application.applicationContext)


    // Przefiltrowana lista wydarzeń - to jest to, co będzie obserwować UI (mapa i lista)
    // Używamy combine, aby reagować na zmiany we wszystkich strumieniach (allEvents, currentFilterState, favoriteEvents)
    val filteredEvents: StateFlow<List<Event>> =
        combine(_allEvents, _currentFilterState, _favoriteEvents) { events, filterState, favoriteEventsList -> // Zmieniona nazwa parametru na favoriteEventsList dla jasności
            Log.d("MapViewModel", "Applying filters. Current filterState: $filterState, Favorites count: ${favoriteEventsList.size}")
            val filtered = events.filter { event ->
                // 1. Filtr "Tylko Ulubione" (jeśli aktywny, ma pierwszeństwo)
                val favoriteMatch = if (filterState.showOnlyFavorites) {
                    favoriteEventsList.contains(event)
                } else {
                    true // Jeśli nie filtrujemy tylko po ulubionych, wszystkie przechodzą ten etap
                }

                // 2. Filtr kategorii (stosowany tylko jeśli nie filtrujemy wyłącznie po ulubionych LUB jeśli chcemy ulubione z danej kategorii)
                // Dla uproszczenia: jeśli showOnlyFavorites jest true, kategoria jest ignorowana.
                // Jeśli chcesz bardziej złożonej logiki (np. ulubione Z kategorii Sport), trzeba by to dostosować.
                val categoryMatch = if (filterState.showOnlyFavorites) {
                    true // Jeśli pokazujemy tylko ulubione, kategoria nie ma już znaczenia na tym etapie
                } else {
                    filterState.category == null || event.category == filterState.category
                }
                Log.d("MapViewModel", "Event ${event.title} - ${filterState.category} - ${event.category}")

                // 3. Filtr promienia i lokalizacji
                val locationMatch = if (filterState.filterLocation.latitude != null && filterState.filterLocation.longitude != null) {
                    event.location?.coordinates?.coordinates?.let { coords ->
                        if (coords.size >= 2) { // Dodatkowe zabezpieczenie przed IndexOutOfBounds
                            val eventLat = coords[1]
                            val eventLon = coords[0]
                            val distance = calculateDistance(
                                filterState.filterLocation.latitude!!, // Pewność, że nie null dzięki warunkowi wyżej
                                filterState.filterLocation.longitude!!, // Pewność, że nie null
                                eventLat,
                                eventLon
                            )
                            distance <= filterState.radiusKm
                        } else false
                    } ?: false
                } else {
                    true
                }

                // 4. Filtr zakresu cen
                val priceMatch = when (filterState.priceRange) {
                    PriceRange.ALL -> true // Wszystkie wydarzenia, bez względu na cenę
                    PriceRange.FREE -> {
                        // Darmowe: cena to "0", "bezpłatne", "free" lub puste/null
                        event.price?.toDoubleOrNull() == 0.0 ||
                                event.price.equals("0", ignoreCase = true) ||
                                event.price.equals("bezpłatne", ignoreCase = true) ||
                                event.price.equals("free", ignoreCase = true) ||
                                event.price.isNullOrEmpty()
                    }
                    PriceRange.PAID -> {
                        // Płatne: cena nie jest darmowa i nie jest pusta/null
                        !(event.price?.toDoubleOrNull() == 0.0 ||
                                event.price.equals("0", ignoreCase = true) ||
                                event.price.equals("bezpłatne", ignoreCase = true) ||
                                event.price.equals("free", ignoreCase = true) ||
                                event.price.isNullOrEmpty())
                    }
                }

                // 5. Filtr daty
                val dateMatch = if (filterState.startDate != null || filterState.endDate != null) {
                    val eventDateString = event.date
                    if (eventDateString != null) {
                        try {
                            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                            dateFormat.isLenient = false // Ścisłe parsowanie daty
                            val eventDate = dateFormat.parse(eventDateString)
                            val eventTimeMillis = eventDate?.time ?: 0L

                            // Normalizuj daty do początku dnia dla startDate i końca dnia dla endDate
                            val startMillis = filterState.startDate?.let { getStartOfDayMillis(it) } ?: Long.MIN_VALUE
                            val endMillis = filterState.endDate?.let { getEndOfDayMillis(it) } ?: Long.MAX_VALUE

                            eventTimeMillis in startMillis..endMillis
                        } catch (e: Exception) {
                            Log.e("MapViewModel", "Błąd parsowania daty dla wydarzenia ${event.title}: $eventDateString, ${e.message}")
                            false
                        }
                    } else {
                        false
                    }
                } else {
                    true
                }
                // Łączenie wszystkich warunków
                favoriteMatch && categoryMatch && locationMatch && priceMatch && dateMatch
            }
            Log.d("MapViewModel", "Filtered events count: ${filtered.size}")
            filtered
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadEvents(application.applicationContext)
        checkAndFetchInitialLocation()
    }

    private fun loadEvents(context: Context) {
        viewModelScope.launch {
            try {
                val events = loadMarkersFromJson(context, R.raw.events_2)
                _allEvents.value = events
                Log.d("MapViewModel", "Loaded ${events.size} events.")
            } catch (e: Exception) {
                Log.e("MapViewModel", "Error loading events: ${e.message}", e)
            }
        }
    }

    fun applyFilter(filterString: String?) {
        _currentFilterState.update { current ->
            if (filterString == FAVORITES_FILTER_KEY) {
                current.copy(
                    showOnlyFavorites = true,
                    category = null // Opcjonalnie: resetuj kategorię, gdy wybierasz "Ulubione" jako główny filtr
                ).also { Log.d("MapViewModel", "Applied favorites filter via string. New state: $it") }
            } else {
                val category = filterString?.let {
                    try { EventCategory.valueOf(it.uppercase(Locale.ROOT)) } catch (e: IllegalArgumentException) { null }
                }
                // Jeśli przełączamy na zwykły filtr kategorii, wyłącz filtr ulubionych
                current.copy(
                    category = category,
                    showOnlyFavorites = false
                ).also { Log.d("MapViewModel", "Applied category filter: $category. New state: $it") }
            }
        }
        clearSelectedEvent()
    }

    fun applyFilter(filterState: FilterState) {
        // Tutaj po prostu przyjmujemy stan z FilterScreen, który powinien już mieć poprawnie ustawione showOnlyFavorites
        _currentFilterState.value = filterState
        Log.d("MapViewModel", "Applied full filter state: $filterState")
        clearSelectedEvent()
    }
    private fun getStartOfDayMillis(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun getEndOfDayMillis(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }
    // Funkcja do ustawiania wybranego wydarzenia (wywoływana z listy lub markera)
    fun selectEventForDetails(event: Event) {
        _selectedEvent.value = event
    }

    // Funkcja do czyszczenia wybranego wydarzenia (wywoływana przyciskiem wstecz)
    fun clearSelectedEvent() {
        _selectedEvent.value = null
        // Logika ruchu kamery mapy (np. powrót do widoku listy) jest w komponencie Composable
    }

    // <-- DODAJ TĘ FUNKCJĘ DO PRZEŁĄCZANIA STATUSU ULUBIONEGO
    fun toggleFavorite(event: Event) {
        val currentFavorites = _favoriteEvents.value.toMutableList()
        if (currentFavorites.contains(event)) {
            // Jeśli wydarzenie jest na liście ulubionych, usuń je
            currentFavorites.remove(event)
            Log.d("MapViewModel", "Usunięto z ulubionych: ${event.title}. Nowa liczba ulubionych: ${currentFavorites.size}")
        } else {
            // Jeśli wydarzenia nie ma na liście ulubionych, dodaj je
            currentFavorites.add(event)
            Log.d("MapViewModel", "Dodano do ulubionych: ${event.title}. Nowa liczba ulubionych: ${currentFavorites.size}")
        }
        _favoriteEvents.value = currentFavorites.toList() // Ustaw nową listę (immutable)
    }

    fun updateLocationPermission(granted: Boolean) {
        _hasLocationPermission.value = granted
        if (granted) {
            fetchLastLocation() // Spróbuj pobrać lokalizację, jeśli uprawnienie zostało przyznane
        }
    }

    private fun checkAndFetchInitialLocation() {
        viewModelScope.launch { // Uruchom w viewModelScope dla bezpieczeństwa
            if (ContextCompat.checkSelfPermission(
                    getApplication<Application>().applicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                    getApplication<Application>().applicationContext,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                _hasLocationPermission.value = true
                fetchLastLocationInternal() // Wewnętrzna funkcja do pobierania
            } else {
                _hasLocationPermission.value = false
                Log.w("MapViewModel", "Brak uprawnień do lokalizacji przy starcie ViewModelu.")
                // Można rozważyć ustawienie filterLocation na default, jeśli była próba ustawienia USER_LOCATION bez uprawnień
            }
        }
    }
    // Dodajemy wewnętrzną funkcję, aby uniknąć duplikacji i zachować publiczną fetchLastLocation dla UI
    @SuppressLint("MissingPermission")
    private fun fetchLastLocationInternal() {
        if (!hasLocationPermission.value) {
            Log.w("MapViewModel", "Próba pobrania lokalizacji bez uprawnień (fetchLastLocationInternal).")
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                Log.d("MapViewModel", "Pobrano lokalizację użytkownika (MapViewModel): ${it.latitude}, ${it.longitude}")
                _userLocation.value = it
                // Jeśli filtr jest ustawiony na lokalizację użytkownika, ale nie ma jeszcze współrzędnych, zaktualizuj go
                if (_currentFilterState.value.filterLocation.type == LocationType.USER_LOCATION &&
                    _currentFilterState.value.filterLocation.latitude == null) { // Sprawdzamy czy latitude jest null jako wskaźnik
                    _currentFilterState.update { current ->
                        current.copy(filterLocation = FilterLocation(
                            type = LocationType.USER_LOCATION,
                            latitude = it.latitude,
                            longitude = it.longitude,
                            name = "Moja lokalizacja (automatycznie)" // Można dodać dopisek
                        ))
                    }
                    Log.d("MapViewModel", "Automatycznie zaktualizowano filterLocation na podstawie UserLocation.")
                }
            } ?: run {
                Log.w("MapViewModel", "Nie udało się pobrać ostatniej lokalizacji użytkownika (MapViewModel). Być może jest wyłączona.")
            }
        }.addOnFailureListener { e ->
            Log.e("MapViewModel", "Błąd podczas pobierania lokalizacji (MapViewModel): ${e.message}")
        }
    }

    // Publiczna funkcja, którą może wywołać UI (np. przycisk "Moja Lokalizacja")
    fun fetchLastLocation() {
        viewModelScope.launch {
            if (ContextCompat.checkSelfPermission(
                    getApplication<Application>().applicationContext, Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                    getApplication<Application>().applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w("MapViewModel", "Brak uprawnień do lokalizacji. Nie można pobrać lokalizacji użytkownika (fetchLastLocation).")
                _hasLocationPermission.value = false
                // Można tu wysłać event do UI, aby poprosić o uprawnienia
                return@launch
            }
            _hasLocationPermission.value = true
            fetchLastLocationInternal()
        }
    }

    // Funkcja pomocnicza do obliczania odległości między dwoma punktami GeoPoint w kilometrach
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0] / 1000 // Konwertuj metry na kilometry
    }
}