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
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.size
import androidx.core.content.ContextCompat

import pl.pw.planair.R
import pl.pw.planair.data.Event
import pl.pw.planair.data.EventCategory
import pl.pw.planair.data.FilterLocation
import pl.pw.planair.data.FilterState
import pl.pw.planair.data.LocationType
import pl.pw.planair.data.PriceRange
import pl.pw.planair.data.loadMarkersFromJson
import pl.pw.planair.data.generateUniqueKey
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

const val FAVORITES_FILTER_KEY = "FAVORITES_FILTER"

class MapViewModel(application: Application) : AndroidViewModel(application) {


    private val FAVORITES_FILE_NAME = "user_favorites.json"

    // --- STAN (State) ---
    private val _allEvents = MutableStateFlow<List<Event>>(emptyList())
    // Expose as read-only StateFlow
    val allEvents: StateFlow<List<Event>> = _allEvents.asStateFlow()

    // Stan wybranego wydarzenia do wyświetlenia szczegółów
    private val _selectedEvent = MutableStateFlow<Event?>(null)
    val selectedEvent: StateFlow<Event?> = _selectedEvent.asStateFlow()

    private val _currentlySelectedEventKey = MutableStateFlow<String?>(null)
    val currentlySelectedEventKey: StateFlow<String?> = _currentlySelectedEventKey.asStateFlow()

    // Stan filtra - wszystkie parametry filtrujące
    private val _currentFilterState = MutableStateFlow(FilterState())
    val currentFilterState: StateFlow<FilterState> = _currentFilterState.asStateFlow()

    val filterState: StateFlow<FilterState> = _currentFilterState.asStateFlow()
    // Zbiór ulubionych wydarzeń (przechowujemy całe obiekty Event)
    private val _favoriteEventIds = MutableStateFlow<Set<String>>(emptySet()) // Przechowuje ID ulubionych
    val favoriteEventIds: StateFlow<Set<String>> = _favoriteEventIds.asStateFlow()

    private var initialCategoryFromFirstScreen: EventCategory? = null

    // Stan przed zmianą na tryb "tylko ulubione"
    private var _previousFilterStateBeforeFavoritesMode: FilterState? = null

    // Aktualna lokalizacja użytkownika
    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation: StateFlow<Location?> = _userLocation.asStateFlow()

    // Stan uprawnień do lokalizacji
    private val _hasLocationPermission = MutableStateFlow(false)
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application.applicationContext)

    private val _showNavigationDialog = MutableStateFlow<Event?>(null) // Przechowuje Event lub null
    val showNavigationDialog: StateFlow<Event?> = _showNavigationDialog.asStateFlow()

    fun requestNavigation(event: Event) {
        _showNavigationDialog.value = event
    }

    fun dismissNavigationDialog() {
        _showNavigationDialog.value = null
    }

    val filteredEvents: StateFlow<List<Event>> =
        combine(
            _allEvents,
            _currentFilterState,
            _favoriteEventIds,
            _userLocation // Dodajemy userLocation do combine, jeśli filtr lokalizacji od niego zależy
        ) { events, filterState, favoriteIds, currentUserLocation ->
            Log.d("MapViewModel", "Applying filters. Current filterState: $filterState, Favorites count: ${favoriteIds.size}")

            if (events.isEmpty()) {
                Log.d("MapViewModel", "No events in _allEvents, returning empty list.")
                return@combine emptyList()
            }

            val filtered = events.filter { event ->
                val eventKey = event.generateUniqueKey()

                // 1. GŁÓWNY WARUNEK: "TYLKO ULUBIONE"
                if (filterState.showOnlyFavorites) {
                    val isFavorite = favoriteIds.contains(eventKey)
                    if (!isFavorite) Log.d("MapViewModel", "Event '${event.title}' NOT a favorite (showOnlyFavorites mode).")
                    return@filter isFavorite
                }

                // --- PONIŻEJ LOGIKA DLA NORMALNEGO TRYBU (gdy filterState.showOnlyFavorites == false) ---

                // 2. Filtr kategorii
                val categoryMatch = filterState.category == null || event.category == filterState.category
                if (!categoryMatch) Log.d("MapViewModel", "Event '${event.title}' FAILED category filter (expected: ${filterState.category}, actual: ${event.category}).")

                // 3. Filtr lokalizacji i promienia
                val locationMatch = when (filterState.filterLocation.type) {
                    LocationType.USER_LOCATION -> {
                        currentUserLocation?.let { userLoc ->
                            event.location?.coordinates?.coordinates?.let { coords ->
                                if (coords.size >= 2) {
                                    val eventLat = coords[1]
                                    val eventLon = coords[0]
                                    val distance = calculateDistance(
                                        userLoc.latitude,
                                        userLoc.longitude,
                                        eventLat,
                                        eventLon
                                    )
                                    val radiusKm = filterState.radiusKm
                                    distance <= radiusKm
                                } else false
                            } ?: false
                        } ?: true // Jeśli lokalizacja użytkownika nie jest dostępna, przepuść (lub false)
                    }
                    LocationType.DEFAULT_LOCATION -> {
                        if (filterState.filterLocation.latitude != null && filterState.filterLocation.longitude != null) {
                            event.location?.coordinates?.coordinates?.let { coords ->
                                if (coords.size >= 2) {
                                    val eventLat = coords[1]
                                    val eventLon = coords[0]
                                    val distance = calculateDistance(
                                        filterState.filterLocation.latitude!!, // !! jest bezpieczne dzięki wcześniejszemu sprawdzeniu
                                        filterState.filterLocation.longitude!!, // !! jest bezpieczne
                                        eventLat,
                                        eventLon
                                    )
                                    val radiusKm = filterState.radiusKm
                                    distance <= radiusKm
                                } else false
                            } ?: false
                        } else {
                            true // Jeśli DEFAULT_LOCATION nie ma zdefiniowanych współrzędnych w filterState, przepuść
                        }
                    }
                    LocationType.MAP_POINT -> {
                        if (filterState.filterLocation.latitude != null && filterState.filterLocation.longitude != null) {
                            event.location?.coordinates?.coordinates?.let { coords ->
                                if (coords.size >= 2) {
                                    val eventLat = coords[1]
                                    val eventLon = coords[0]
                                    val distance = calculateDistance(
                                        filterState.filterLocation.latitude!!, // !! jest bezpieczne
                                        filterState.filterLocation.longitude!!, // !! jest bezpieczne
                                        eventLat,
                                        eventLon
                                    )
                                    val radiusKm = filterState.radiusKm
                                    distance <= radiusKm
                                } else false
                            } ?: false
                        } else {
                            true // Jeśli MAP_POINT nie ma zdefiniowanych współrzędnych w filterState, przepuść
                        }
                    }
                    else -> true // Domyślnie przepuść, jeśli typ lokalizacji nie jest obsługiwany lub nie ma filtra
                }
                if (!locationMatch) Log.d("MapViewModel", "Event '${event.title}' FAILED location filter.")

                // 4. Filtr zakresu cen
                val priceMatch = when (filterState.priceRange) {
                    PriceRange.ALL -> true
                    PriceRange.FREE -> event.price.isNullOrEmpty() || event.price.equals("0", ignoreCase = true) || event.price.equals("bezpłatne", ignoreCase = true) || event.price.equals("free", ignoreCase = true) || event.price?.toDoubleOrNull() == 0.0
                    PriceRange.PAID -> !(event.price.isNullOrEmpty() || event.price.equals("0", ignoreCase = true) || event.price.equals("bezpłatne", ignoreCase = true) || event.price.equals("free", ignoreCase = true) || event.price?.toDoubleOrNull() == 0.0)
                }
                if (!priceMatch) Log.d("MapViewModel", "Event '${event.title}' FAILED price filter (expected: ${filterState.priceRange}, actual price: ${event.price}).")

                // 5. Filtr daty
                val dateMatch = if (filterState.startDate != null || filterState.endDate != null) {
                    event.date?.let { eventDateString ->
                        try {
                            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                            dateFormat.isLenient = false
                            val eventDate = dateFormat.parse(eventDateString)
                            val eventTimeMillis = eventDate?.time ?: 0L

                            val startMillis = filterState.startDate?.let { getStartOfDayMillis(it) } ?: Long.MIN_VALUE
                            val endMillis = filterState.endDate?.let { getEndOfDayMillis(it) } ?: Long.MAX_VALUE

                            eventTimeMillis in startMillis..endMillis
                        } catch (e: Exception) {
                            Log.e("MapViewModel", "Błąd parsowania daty dla wydarzenia ${event.title}: $eventDateString, ${e.message}")
                            false
                        }
                    } ?: false
                } else {
                    true
                }
                if (!dateMatch) Log.d("MapViewModel", "Event '${event.title}' FAILED date filter.")

                // Łączenie wszystkich warunków dla normalnego trybu
                categoryMatch && locationMatch && priceMatch && dateMatch
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
        loadFavoriteIds()
    }

    private fun saveFavoriteIds() {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>().applicationContext
                val idsToSave = _favoriteEventIds.value
                val jsonString = Json.encodeToString(idsToSave)
                val file = File(context.filesDir, FAVORITES_FILE_NAME)
                file.writeText(jsonString)
                Log.d("MapViewModel", "Zapisano ${idsToSave.size} ulubionych ID do pliku.")
            } catch (e: Exception) {  // Złap bardziej szczegółowe wyjątki
                Log.e("MapViewModel", "Błąd podczas zapisywania ulubionych ID do JSON: ${e.message}", e)
            }
        }
    }

    private fun loadFavoriteIds() {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>().applicationContext
                val file = File(context.filesDir, FAVORITES_FILE_NAME)
                if (file.exists()) {
                    val jsonString = file.readText()
                    if (jsonString.isNotBlank()) {
                        val ids = Json.decodeFromString<Set<String>>(jsonString)
                        _favoriteEventIds.value = ids
                        Log.d("MapViewModel", "Załadowano ${ids.size} ulubionych ID z pliku.")
                    } else {
                        _favoriteEventIds.value = emptySet()
                        Log.d("MapViewModel", "Plik ulubionych jest pusty.")
                    }
                } else {
                    _favoriteEventIds.value = emptySet()
                    Log.d("MapViewModel", "Plik ulubionych nie istnieje. Inicjalizuję pusty zbiór.")
                }
            } catch (e: Exception) { // Złap bardziej szczegółowe wyjątki, jeśli to konieczne
                Log.e("MapViewModel", "Błąd podczas ładowania ulubionych ID z JSON: ${e.message}", e)
                _favoriteEventIds.value = emptySet() // W razie błędu, zacznij z pustym zbiorem
            }
        }
    }

    private fun loadEvents(context: Context) {
        viewModelScope.launch {
            try {
                val events = loadMarkersFromJson(context, R.raw.wydarzenia)
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
                initialCategoryFromFirstScreen = category
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
        _previousFilterStateBeforeFavoritesMode = null
        Log.d("MapViewModel", "Applied full filter state: $filterState")
        clearSelectedEvent()
    }

    fun prepareForFilterScreenNavigation() {
        // Jeśli jesteśmy w trybie ulubionych, wyłącz go i przywróć poprzedni stan.
        // Jeśli nie, po prostu upewnij się, że _previousFilterStateBeforeFavoritesMode jest czysty.
        if (_currentFilterState.value.showOnlyFavorites) {
            _currentFilterState.update {
                _previousFilterStateBeforeFavoritesMode?.copy(showOnlyFavorites = false)
                    ?: it.copy(showOnlyFavorites = false)
            }
        }
        _previousFilterStateBeforeFavoritesMode = null
        Log.d("MapViewModel", "Prepared for filter screen navigation. Current filters: ${_currentFilterState.value}")
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
        _currentlySelectedEventKey.value = event.generateUniqueKey()
    }

    // Funkcja do czyszczenia wybranego wydarzenia (wywoływana przyciskiem wstecz)
    fun clearSelectedEvent() {
        _selectedEvent.value = null
        _currentlySelectedEventKey.value = null}

    fun toggleFavorite(event: Event) {
        val eventKey = event.generateUniqueKey() // Używamy funkcji rozszerzającej

        _favoriteEventIds.update { currentFavoriteIds ->
            val newFavoriteIds = currentFavoriteIds.toMutableSet()
            if (newFavoriteIds.contains(eventKey)) {
                newFavoriteIds.remove(eventKey)
                Log.d("MapViewModel", "Usunięto z ulubionych klucz: $eventKey. Nowa liczba ulubionych: ${newFavoriteIds.size}")
            } else {
                newFavoriteIds.add(eventKey)
                Log.d("MapViewModel", "Dodano do ulubionych klucz: $eventKey. Nowa liczba ulubionych: ${newFavoriteIds.size}")
            }
            newFavoriteIds
        }
        saveFavoriteIds()
    }

    /**
     * Inicjuje nawigację do lokalizacji podanego wydarzenia za pomocą Google Maps.
     *
     * @param event Wydarzenie, do którego lokalizacji ma zostać uruchomiona nawigacja.
     */
    fun navigateToEventLocation(event: Event) {
        val context = getApplication<Application>().applicationContext
        val coordinates = event.location?.coordinates?.coordinates
        // Po potwierdzeniu dialogu, czyścimy stan dialogu
        _showNavigationDialog.value = null // lub dismissNavigationDialog()

        if (coordinates != null && coordinates.size >= 2) {
            val latitude = coordinates[1]
            val longitude = coordinates[0]
            // Opcjonalnie: użyj nazwy wydarzenia lub lokalizacji jako etykiety
            val locationLabel = event.location?.address ?: event.title ?: "Lokalizacja wydarzenia"
            val encodedLabel = Uri.encode(locationLabel) // Ważne, aby zakodować specjalne znaki

            // ZMIANA URI: "geo:latitude,longitude?q=latitude,longitude(Label)"
            // To URI pokazuje pinezkę na mapie z opcjonalną etykietą.
            // Użytkownik sam musi kliknąć "Wyznacz trasę".
            val gmmIntentUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encodedLabel)")
            // Alternatywnie, jeśli nie chcesz etykiety, a tylko pinezkę:
            // val gmmIntentUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
            // Lub jeszcze prościej, samo "geo:latitude,longitude" powinno wycentrować mapę:
            // val gmmIntentUri = Uri.parse("geo:$latitude,$longitude")


            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
                Log.d("MapViewModel", "Pokazywanie lokalizacji w Google Maps: $latitude,$longitude dla: ${event.title}")
            } else {
                Toast.makeText(context, "Aplikacja Google Maps nie jest zainstalowana.", Toast.LENGTH_LONG).show()
                Log.w("MapViewModel", "Aplikacja Google Maps nie jest zainstalowana dla: ${event.title}")
                // Fallback do przeglądarki (jak poprzednio)
                try {
                    val webIntentUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
                    // ... reszta fallbacku ...
                } catch (e: Exception) { /* ... */ }
            }
        } else {
            Toast.makeText(context, "Brak dokładnych współrzędnych dla wydarzenia: ${event.title}", Toast.LENGTH_SHORT).show()
            Log.w("MapViewModel", "Próba pokazania lokalizacji dla wydarzenia bez współrzędnych: ${event.title}")
        }
    }

    // W klasie MapViewModel
    fun toggleShowOnlyFavorites() {
        _currentFilterState.update { currentState ->
            val newShowOnlyFavoritesState = !currentState.showOnlyFavorites

            if (newShowOnlyFavoritesState) {
                _previousFilterStateBeforeFavoritesMode = currentState.copy()
                // Włączamy tryb "tylko ulubione" - resetujemy inne filtry
                currentState.copy(
                    showOnlyFavorites = true,
                    category = null,
                    startDate = null,
                    endDate = null,
                    priceRange = PriceRange.ALL, // Załóżmy, że masz PriceRange.ALL
                    //filterLocation = FilterLocation(type = LocationType.EVERYWHERE) // Resetuj lokalizację
                    // Dodaj resetowanie innych pól FilterState, jeśli je masz
                )
            } else {
                // Wyłączamy tryb "tylko ulubione" - przywracamy możliwość działania innych filtrów
                // (ale nie przywracamy ich poprzednich wartości, użytkownik ustawi je od nowa, jeśli chce)
                _previousFilterStateBeforeFavoritesMode?.copy(
                    showOnlyFavorites = false // Upewnij się, że ten tryb jest wyłączony
                ) ?: currentState.copy(showOnlyFavorites = false)
            }
        }
        Log.d("MapViewModel", "Toggled showOnlyFavorites. New state: ${_currentFilterState.value}")
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