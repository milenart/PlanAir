package pl.pw.planair.ui.map.viewmodel

import android.app.Application // Import klasy Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel // <-- ZMIEN NA AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

// Importuj funkcje loadMarkersFromJson i klase Event jesli sa w innym pakiecie
import pl.pw.planair.data.loadMarkersFromJson // <-- Upewnij sie ze importujesz swoja funkcje
// import pl.pw.planair.data.Event // <-- Upewnij sie ze importujesz swoja klase Event jesli jest w innym pakiecie

// Import R dla dostepu do zasobów JSON (zazwyczaj R z Twojego pakietu glownego, np. pl.pw.planair.R)
import pl.pw.planair.R // <-- Importuj klase R z Twojego glownego pakietu aplikacji
import pl.pw.planair.data.Event


// Zmien z ViewModel() na AndroidViewModel(application)
class MapViewModel(application: Application) : AndroidViewModel(application) { // <-- ZMIEN TA LINIE

    // --- STAN (State) ---
    private val _allEvents = MutableStateFlow<List<Event>>(emptyList())
    private val _currentFilter = MutableStateFlow<String?>(null)
    val currentFilter: StateFlow<String?> = _currentFilter.asStateFlow()

    val filteredEvents: StateFlow<List<Event>> = combine(_allEvents, _currentFilter) { events, filter ->
        if (filter == null) { // Jeśli filtr to null (czyli "Wszystko")
            events // Zwracamy całą listę wydarzeń
        } else { // Jeśli jest wybrany konkretny filtr
            events.filter { it.category.name == filter } // Filtrujemy listę po kategorii
        }
    }.stateIn( // <-- ZMIANA TUTAJ
        scope = viewModelScope, // Użyj viewModelScope
        started = SharingStarted.WhileSubscribed(5000), // Strategia startu/stopu
        initialValue = emptyList() // Wartość początkowa, zanim combine coś wyemituje
    )

    // --- INICJALIZACJA ---
    init {
        viewModelScope.launch {
            // Pobierz Application context z AndroidViewModel
            val context = getApplication<Application>().applicationContext // <-- Uzyj getApplication()
            // Wywołaj swoją funkcję do ładowania danych z JSON-a
            fetchEvents(context, R.raw.events) // <-- ZMIEN TĘ LINIĘ: Uzyj swojego ID zasobu JSON
        }
    }

    // --- LOGIKA (Logic) ---

    // Zmien sygnature funkcji fetchEvents, zeby przyjmowala Context i resourceId
    private suspend fun fetchEvents(context: Context, resourceId: Int) {
        // Tutaj wywolujemy TWOJA funkcje do ladowania danych z JSON-a
        val eventList = loadMarkersFromJson(context, resourceId) // <-- Wywołaj swoją funkcję
        _allEvents.value = eventList // Zaktualizuj stan _allEvents swoimi danymi
    }

    fun applyFilter(filter: String?) {
        _currentFilter.value = filter
    }

    // --- NOWY STAN: Wybrane wydarzenie ---
    private val _selectedEvent = MutableStateFlow<Event?>(null)

    val selectedEvent: StateFlow<Event?> = _selectedEvent.asStateFlow()

    fun selectEventForDetails(event: Event) {
        _selectedEvent.value = event
        // TODO: W przyszlosci tutaj bedziemy wyzwalać ruch kamery mapy
    }

    // Funkcja wywoływana np. po wcisnieciu przycisku "Wstecz" w widoku szczegółów.
    // Czyści stan wybranego wydarzenia, co powinno spowodowac powrot do widoku listy.
    fun clearSelectedEvent() {
        _selectedEvent.value = null
        // TODO: W przyszlosci tutaj bedziemy wyzwalać oddalenie kamery mapy
    }

}