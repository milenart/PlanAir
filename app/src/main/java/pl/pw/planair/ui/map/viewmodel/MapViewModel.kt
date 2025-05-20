package pl.pw.planair.ui.map.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
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
import pl.pw.planair.R // <-- Importuj klase R z Twojego glownego pakietu aplikacji
import pl.pw.planair.data.Event // <-- Upewnij sie ze importujesz swoja klase Event

// TODO: Zdecyduj, czy potrzebujesz EventCategory tutaj, czy tylko nazwy kategorii jako Stringi
import pl.pw.planair.data.EventCategory // Jeśli używasz tego enuma w applyFilter, zachowaj import

const val FAVORITES_FILTER_KEY = "FAVORITES_FILTER"

// Zmien z ViewModel() na AndroidViewModel(application)
class MapViewModel(application: Application) : AndroidViewModel(application) {

    // --- STAN (State) ---
    private val _allEvents = MutableStateFlow<List<Event>>(emptyList())
    // Zostawiamy _currentFilter jako String?, użyjemy specjalnej wartości dla ulubionych
    private val _currentFilter = MutableStateFlow<String?>(null)
    val currentFilter: StateFlow<String?> = _currentFilter.asStateFlow()

    private val _selectedEvent = MutableStateFlow<Event?>(null)
    val selectedEvent: StateFlow<Event?> = _selectedEvent.asStateFlow()

    // <-- DODAJ STAN PRZECHOWUJĄCY ULUBIONE WYDARZENIA
    private val _favoriteEvents = MutableStateFlow<List<Event>>(emptyList())
    val favoriteEvents: StateFlow<List<Event>> = _favoriteEvents.asStateFlow()


    // --- STAŁA/WARTOŚĆ SPECJALNA DLA FILTRA ULUBIONYCH ---
    // Uzyjemy specjalnego stringa do oznaczenia, ze filtr ulubionych jest aktywny
    private val FAVORITES_FILTER_KEY = "FAVORITES_FILTER" // <-- Zdefiniuj klucz dla filtra ulubionych


    // --- filteredEvents będzie teraz kombinacją _allEvents, _favoriteEvents i _currentFilter ---
    // Zmieniamy combine, aby uwzględniało _favoriteEvents i logike filtra ulubionych
    val filteredEvents: StateFlow<List<Event>> = combine(
        _allEvents,
        _favoriteEvents, // <-- DODAJ _favoriteEvents do obserwowanych flow
        _currentFilter
    ) { allEvents, favoriteEvents, currentFilter -> // <-- DODAJ favoriteEvents do parametrow lambdy
        when {
            // <-- DODAJ LOGIKĘ FILTROWANIA ULUBIONYCH
            currentFilter == FAVORITES_FILTER_KEY -> {
                favoriteEvents // Jeśli filtr to nasz klucz dla ulubionych, pokaż tylko ulubione
            }
            currentFilter != null -> { // Jeśli filtr nie jest nullem (czyli to nazwa kategorii)
                allEvents.filter { it.category.name == currentFilter } // Filtrujemy listę po nazwie kategorii
            }
            else -> { // Jeśli filtr to null (czyli "Wszystko")
                allEvents // Zwracamy całą listę wydarzeń
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- INICJALIZACJA ---
    init {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            // Ładowanie danych wydarzeń z JSON
            fetchEvents(context, R.raw.events) // <-- Uzyj swojego ID zasobu JSON

            // Ładowanie ulubionych na start (na razie pusta lista)
            loadFavorites() // <-- DODAJ WYWOŁANIE FUNKCJI ŁADOWANIA ULUBIONYCH
        }
    }

    // --- LOGIKA (Logic) ---

    // Funkcja do ładowania danych wydarzeń z JSON (Twoja istniejąca)
    private suspend fun fetchEvents(context: Context, resourceId: Int) {
        val eventList = loadMarkersFromJson(context, resourceId) // <-- Wywołaj swoją funkcję
        _allEvents.value = eventList // Zaktualizuj stan _allEvents swoimi danymi
    }

    // <-- DODAJ TĘ FUNKCJĘ DO ŁADOWANIA ULUBIONYCH (TERAZ PUSTA IMPLEMENTACJA)
    private fun loadFavorites() {
        viewModelScope.launch {
            // TODO: Zaimplementuj ładowanie ulubionych z trwałego magazynu danych (np. DataStore, Room)
            // Na razie inicjalizuj pustą listą lub wczytaj z prostego miejsca jeśli masz
            _favoriteEvents.value = emptyList() // Przykładowa pusta lista na start
        }
    }


    // Modyfikacja funkcji applyFilter - teraz ustawia filtr kategorii lub All
    fun applyFilter(filter: String?) {
        // Jeśli filter jest nullem LUB jest kluczem ulubionych, ustawiamy na null (wszystko)
        // Inaczej ustawiamy na podany String (zakładając, że to nazwa kategorii)
        _currentFilter.value = if (filter == null || filter == FAVORITES_FILTER_KEY) null else filter
        // TODO: Mozesz opcjonalnie dodać logike przewijania listy/mapy po zmianie filtra
        clearSelectedEvent() // Wyczyść zaznaczone wydarzenie po zmianie filtra
    }

    // <-- DODAJ TĘ FUNKCJĘ DO USTAWIANIA FILTRA NA ULUBIONE
    fun showFavorites() {
        _currentFilter.value = FAVORITES_FILTER_KEY // Ustaw _currentFilter na klucz filtra ulubionych
        clearSelectedEvent() // Wyczyść zaznaczone wydarzenie po zmianie filtra
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
        _favoriteEvents.value = currentFavorites // Zaktualizuj stan ulubionych
        // TODO: W przyszlosci zaimplementuj trwale zapisywanie ulubionych (np. DataStore, Room)
    }
}