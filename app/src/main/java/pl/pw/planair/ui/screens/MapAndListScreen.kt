package pl.pw.planair.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.* // Import * dla łatwiejszego dostępu do remember, getValue, setValue, etc.
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity // Potrzebne do konwersji dp na piksele
import androidx.compose.ui.unit.Density // Potrzebne do konwersji dp na piksele
import androidx.compose.ui.platform.LocalConfiguration // Potrzebne do rozmiarów ekranu
import pl.pw.planair.data.Event // Upewnij sie ze masz import Event
// Usunięto import FilterCriterion - nie jest już używany bezpośrednio w UI do sprawdzania typu filtra
// Usunięto import EventCategory - nie jest już używany bezpośrednio w UI w logice filtra

import pl.pw.planair.ui.components.EventListItem
import pl.pw.planair.ui.components.OsmMapView
import pl.pw.planair.ui.components.EventDetailsView

import pl.pw.planair.ui.map.viewmodel.MapViewModel
// Import klucza dla filtra ulubionych z ViewModelu
import pl.pw.planair.ui.map.viewmodel.FAVORITES_FILTER_KEY // <-- DODAJ TEN IMPORT

import androidx.lifecycle.viewmodel.compose.viewModel

import org.osmdroid.views.MapView // Potrzebne do typu MapView
import org.osmdroid.util.GeoPoint // Potrzebne do okreslenia punktu na mapie
import org.osmdroid.util.BoundingBox // Potrzebne do obliczenia BoundingBox z markerów
import android.util.Log // Do logowania debugowego
import android.graphics.Point // Potrzebne dla klasy Point (wspolrzedne pikselowe)

// Import ikon
import androidx.compose.material.icons.filled.FilterList // Ikona filtra
import androidx.compose.material.icons.filled.Favorite // Ikona ulubionych (wypełniona)
import androidx.compose.material.icons.filled.FavoriteBorder // Ikona ulubionych (obramowanie)

// Importy dla WindowInsets i związanych z nimi funkcji
import androidx.compose.foundation.layout.WindowInsets // <-- DODAJ TEN IMPORT
import androidx.compose.foundation.layout.systemBars // <-- DODAJ TEN IMPORT
import androidx.compose.foundation.layout.asPaddingValues // <-- DODAJ TEN IMPORT


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenWithMapAndList(
    mapViewModel: MapViewModel = viewModel()
) {
    // Stany z ViewModelu
    val filteredEvents by mapViewModel.filteredEvents.collectAsState()
    val selectedEvent by mapViewModel.selectedEvent.collectAsState()
    val favoriteEvents by mapViewModel.favoriteEvents.collectAsState()
    //currentFilter jest teraz String? z ViewModelu
    val currentFilter by mapViewModel.currentFilter.collectAsState()


    // Stan przechowujący instancję MapView osmdroid
    var mapView by remember { mutableStateOf<MapView?>(null) }

    // Zasoby do obliczeń jednostek i rozmiarów ekranu
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    // Stan i scaffold dla BottomSheet'a
    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded, // Startujemy z panelem częściowo rozwiniętym
        skipHiddenState = true // Nie pozwalamy na ukrycie panelu przeciągnięciem w dół
    )

    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)


    // --- LaunchedEffect do Ustawienia Widoku Początkowego/Domyślnego ---
// --- LaunchedEffect do Ustawienia Widoku Początkowego/Domyślnego ---
    LaunchedEffect(filteredEvents, selectedEvent, mapView) {
        val currentMapView = mapView
        // Ustaw widok poczatkowy tylko gdy lista wydarzen jest niepusta, zadne nie jest wybrane i mapa jest gotowa
        if (selectedEvent == null && filteredEvents.isNotEmpty() && currentMapView != null) {
            Log.d("MapZoomDebug", "--- Uruchamiam ustawienie widoku domyślnego ---")
            Log.d("MapZoomDebug", "Stan: selectedEvent=null, filteredEvents.isNotEmpty()=${filteredEvents.isNotEmpty()}, currentMapView!=null=${currentMapView != null}")
            Log.d("MapZoomDebug", "Liczba przefiltrowanych wydarzeń do BoundingBox: ${filteredEvents.size}") // <-- Dodaj logowanie liczby wydarzeń

            try {
                // Upewnij sie, ze lista filteredEvents nie jest pusta przed tworzeniem BoundingBox
                if (filteredEvents.isEmpty()) {
                    Log.w("MapZoomDebug", "filteredEvents jest puste po zmianie filtra. Nie mozna utworzyc BoundingBox.")
                    // Mozesz tutaj dodac fallback np. centrowanie na domyslnej pozycji
                    currentMapView.controller.setCenter(GeoPoint(52.2370, 21.0170)) // Domyślne centrum (np. Warszawa)
                    currentMapView.controller.setZoom(10.0) // Domyślny zoom
                    return@LaunchedEffect // Wyjdz z efektu
                }

                val geoPoints = filteredEvents.map { GeoPoint(it.lat, it.lon) }
                Log.d("MapZoomDebug", "Utworzono listę GeoPoints (${geoPoints.size}) do BoundingBox.")

                val boundingBox = BoundingBox.fromGeoPoints(geoPoints) // <-- Uzyj listy GeoPoints

                Log.d("MapZoomDebug", "Obliczone bazowe BoundingBox: $boundingBox") // <-- Dodaj logowanie BoundingBox bazowego

                // RĘCZNE OBLICZENIE NOWEGO BOUNDING BOX Z MARGINESEM
                val percentage = 0.1 // 10% marginesu (mozesz dostosowac)
                val latHeight = boundingBox.latNorth - boundingBox.latSouth
                val lonWidth = boundingBox.lonEast - boundingBox.lonWest
                // Sprawdz czy wysokosc/szerokosc nie sa zerowe, zeby uniknac NaN/Infinity
                val latMargin = if (latHeight > 0) latHeight * percentage else 0.0
                val lonMargin = if (lonWidth > 0) lonWidth * percentage else 0.0

                val paddedBox = BoundingBox(
                    boundingBox.latNorth + latMargin,
                    boundingBox.lonEast + lonMargin,
                    boundingBox.latSouth - latMargin,
                    boundingBox.lonWest - lonMargin
                )
                Log.d("MapZoomDebug", "Obliczone padded BoundingBox z marginesem: $paddedBox") // <-- Dodaj logowanie BoundingBox z marginesem


                currentMapView.zoomToBoundingBox(paddedBox, true) // Wyśrodkuj i ustaw zoom
                Log.i("MapZoomDebug", "Ustawiono domyslny widok mapy na BoundingBox: $paddedBox")

            }  catch (e: Exception) {
                // Logika fallbacku na wypadek błędu (np. tylko 1 marker w bounding box)
                if (filteredEvents.size == 1 && currentMapView != null) { // Dodaj sprawdzenie currentMapView != null
                    val singleMarkerLocation = GeoPoint(filteredEvents.first().lat, filteredEvents.first().lon)
                    currentMapView.controller.setCenter(singleMarkerLocation)
                    currentMapView.controller.setZoom(15.0) // Domyślny zoom dla 1 markera
                    Log.i("MapZoomDebug", "Ustawiono widok na pojedynczy marker: $singleMarkerLocation z zoomem 15.0 (fallback)")
                } else if (currentMapView != null) { // Dodaj sprawdzenie currentMapView != null
                    // Inny błąd lub lista pusta (choć to sprawdzone wcześniej)
                    currentMapView.controller.setCenter(GeoPoint(52.2370, 21.0170)) // Domyślne centrum (np. Warszawa)
                    currentMapView.controller.setZoom(10.0) // Domyślny zoom
                    Log.i("MapZoomDebug", "Ustawiono domyslny widok na Warszawę (fallback).")
                } else {
                    Log.w("MapZoomDebug", "Cannot apply fallback view, MapView is null.")
                }
                Log.e("MapZoomDebug", "Błąd w LaunchedEffect domyślnego widoku podczas ustawiania BoundingBox: ${e.message}", e)
            }
            Log.d("MapZoomDebug", "--- Koniec ustawienia widoku domyślnego ---")
        }
        // Ustaw domyślny widok globalny, gdy lista wydarzeń jest pusta i żadne nie jest wybrane
        else if (selectedEvent == null && filteredEvents.isEmpty() && currentMapView != null) {
            Log.d("MapZoomDebug", "Lista wydarzeń pusta i brak wybranego, ustawiam widok globalny.")
            currentMapView.controller.setCenter(GeoPoint(52.2370, 21.0170))
            currentMapView.controller.setZoom(6.0)
        } else if (currentMapView == null) {
            Log.d("MapZoomDebug", "LaunchedEffect domyślny widok: MapView jest null.")
        } else if (selectedEvent != null) {
            Log.d("MapZoomDebug", "LaunchedEffect domyślny widok: Wybrane wydarzenie != null, nie ustawiam widoku domyślnego.")
        } else {
            Log.d("MapZoomDebug", "LaunchedEffect domyślny widok: Warunki niespełnione.")
        }
    }


    // --- LaunchedEffect do Reakcji na Zmiany selectedEvent (Zoom na Pojedynczy Marker z Przesunięciem) ---
    // Ten efekt uruchamia się, gdy selectedEvent, mapView lub density się zmieni.
    // Odpowiada za przesunięcie kamery mapy, aby wybrany marker był widoczny nad panelem szczegółów.
    LaunchedEffect(selectedEvent, mapView, density) {
        // Używamy '?: return@LaunchedEffect' do bezpiecznego wyjścia, jeśli potrzebne obiekty są null
        val currentMapView = mapView ?: return@LaunchedEffect
        val event = selectedEvent ?: return@LaunchedEffect // Używamy 'event' zamiast 'currentSelectedEvent' dla zwięzłości

        Log.d("MapZoomDebug", "--- Uruchamiam zoom na wybrane wydarzenie ---")
        Log.d("MapZoomDebug", "Wybrane wydarzenie: ${event.title}, Punkt: ${event.lat},${event.lon}")

        // Ustaw docelowy zoom (przed obliczeniem przesunięcia)
        currentMapView.controller.setZoom(17.0) // Możesz dostosować ten zoom


        // Sprawdzenie gotowości mapy (wymiary)
        if (currentMapView.width == 0 || currentMapView.height == 0) {
            Log.w("MapZoomDebug", "Mapa nie jest jeszcze gotowa (brak wymiarów). Przerywam.")
            return@LaunchedEffect
        }
        val mapViewWidthPx = currentMapView.width.toFloat()
        val mapViewHeightPx = currentMapView.height.toFloat()
        Log.d("MapZoomDebug", "Wymiary MapView: $mapViewWidthPx x $mapViewHeightPx px")

        val projection = currentMapView.projection


        // --- Logika Obliczania Nowego Centrum Mapy z Przesunięciem (Twoja działająca implementacja) ---
        // Obliczamy GeoPoint, na który trzeba się wyśrodkować, aby marker był przesunięty o yOffsetPx w górę od środka ekranu.

        // Określ stałe przesunięcie w pionie (w górę od środka ekranu), gdzie ma pojawić się marker.
        // To jest przesunięcie w pikselach OD środka EKRANU MAPVIEW, DO miejsca, gdzie chcemy marker.
        // Ujemne yOffsetPx oznacza przesunięcie w górę.
        val yOffsetDp = 200.dp
        val yOffsetPx = with(density) { yOffsetDp.toPx() } // Konwersja na piksele
        Log.d("MapZoomDebug", "Docelowe przesunięcie markera od środka ekranu w górę: $yOffsetPx px ($yOffsetDp DP)")


        // Pobierz GeoPoint wydarzenia
        val geoPoint = GeoPoint(event.lat, event.lon)

        // Przelicz GeoPoint wydarzenia na współrzędne pikselowe NA EKRANIE (względem lewego górnego rogu Mapview)
        val screenPoint = projection.toPixels(geoPoint, null) // Współrzędne pikselowe markera na ekranie
        Log.d("MapZoomDebug", "Event Screen Point (current): $screenPoint") // Loguj obecną pozycję markera w pikselach

        // Oblicz docelowe współrzędne pikselowe na ekranie, na które ma trafić marker:
        // X: środek ekranu MapView
        // Y: środek ekranu MapView MINUS yOffsetPx (bo yOffsetPx jest w górę, a Y na ekranie rośnie w dół)
        val targetMarkerScreenX = mapViewWidthPx / 2f
        // TWOJA działająca logika wzięła screenPoint.y + yOffsetPx. To oznacza, że przelicza GeoPoint,
        // który jest na ekranie o yOffsetPx PONIŻEJ OBECNEJ POZYCJI MARKERA.
        // W ten sposób wyśrodkowanie na tym punkcie przesuwa mapę w górę, umieszczając markera wyżej.
        // Zachowujemy Twoją działającą logikę:
        val targetPointForCenterConversionY = screenPoint.y + yOffsetPx
        Log.d("MapZoomDebug", "Docelowy Y pikselowy dla punktu do konwersji na nowe centrum: $targetPointForCenterConversionY px")


        val newCenter = try {
            // Oblicz GeoPoint, który znajduje się na ekranie w pozycji (screenPoint.x, screenPoint.y + yOffsetPx)
            // TO jest GeoPoint, który MUSI STAĆ SIĘ NOWYM CENTRUM MAPY.
            projection.fromPixels(screenPoint.x, targetPointForCenterConversionY.toInt()) as? GeoPoint
        } catch (e: Exception) {
            Log.e("MapZoomDebug", "Błąd podczas konwersji z pikseli na GeoPoint dla nowego centrum: ${e.message}", e)
            null // Ustaw na null w przypadku błędu konwersji
        } ?: geoPoint // Fallback na GeoPoint wydarzenia jeśli konwersja się nie powiodła

        Log.d("MapZoomDebug", "Obliczone nowe centrum GeoPoint (z przesunięcia pikselowego): $newCenter")

        // --- Animacja ---
        if (newCenter != null) {
            Log.i("MapZoomDebug", "Animowanie mapy do obliczonego środka: $newCenter z zoomem 17.0")
            currentMapView.controller.animateTo(newCenter) // AnimateTo z jednym argumentem używa bieżącego zoomu i domyślnego czasu
            // Jeśli chcesz zdefiniować czas animacji, użyj:
            // currentMapView.controller.animateTo(newCenter, currentMapView.zoomLevelDouble, 1000L)
        } else {
            // Fallback: Jeśli obliczenie nowego centrum się nie powiodło, po prostu wycentruj na samym markerze
            Log.e("MapZoomDebug", "newCenter jest null! Centruję na markerze bez przesunięcia.")
            currentMapView.controller.animateTo(geoPoint, 17.0, 1000L) // Użyj 17.0 jako fallback zoomu
        }
        Log.d("MapZoomDebug", "--- Koniec zoom na wybrane wydarzenie ---")
    }


    // --- BottomSheetScaffold - Główna Struktura Ekranu ---
    BottomSheetScaffold(
        scaffoldState = scaffoldState, // Stan scaffoldu (kontroluje stan panelu)

        sheetContent = { // Zawartość panelu dolnego
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 75.dp) // Minimalna wysokość, gdy panel jest zwinięty
            ) {
                // --- Górny pasek przycisków w panelu (dynamiczny - zalezy od widoku listy/szczegółów) ---

                // Zdefiniuj currentSelectedEvent TYLKO RAZ na początku bloku sheetContent
                val currentSelectedEvent = selectedEvent // Pobierz aktualnie wybrane wydarzenie z ViewModelu
                val currentFilterValue = currentFilter // Pobierz aktualne kryterium filtra z ViewModelu (String?)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 0.dp), // Minimalny padding horyzontalny
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween // Rozkłada elementy równomiernie na całej szerokości
                ) {
                    if (currentSelectedEvent != null) {
                        // --- WIDOK SZCZEGÓŁÓW: Przycisk Wstecz (lewo) ---
                        // Przycisk Ulubione jest w EventDetailsView
                        // Lewy przycisk: Wstecz do listy
                        IconButton(
                            onClick = { mapViewModel.clearSelectedEvent() }, // Wywołaj funkcję ViewModelu do czyszczenia wybranego wydarzenia
                            modifier = Modifier.padding(4.dp) // Mały padding wokół ikony
                        ) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Wstecz do listy")
                        }

                        // Spacer zajmujący dostępną przestrzeń, spycha przycisk Wstecz w lewo
                        Spacer(Modifier.weight(1f))

                        // TODO: Tutaj MOŻE być przycisk Nawiguj, jeśli zdecydujesz, że będzie w tym górnym pasku
                        /*
                        IconButton(onClick = { /* TODO: Zaimplementuj nawigację */ }) {
                            Icon(Icons.Default.Navigation, contentDescription = "Nawiguj do wydarzenia") // Wymaga importu ikony nawigacji
                        }
                         */

                    } else {
                        // --- WIDOK LISTY: Przycisk Filtr Kategorii (lewo) i Przycisk Pokaż Ulubione/Wszystkie (prawo) ---
                        // Lewy przycisk: Filtr Kategori / Inne kryteria (Placeholder)
                        // Ten przycisk będzie otwierał panel wyboru filtrów (do zaimplementowania później)
                        IconButton( // <-- Prawidłowa struktura wywołania IconButton
                            onClick = {
                                // TODO: Zaimplementuj logikę otwierania panelu/dialogu wyboru filtrów
                                Log.d("MapZoomDebug", "Kliknięto przycisk 'Otwórz filtry'.")
                            },
                            modifier = Modifier.padding(4.dp) // Mały padding
                        ) { // <-- Treść przycisku (Ikona) znajduje się W TEJ LAMBDZIE
                            Icon(Icons.Filled.FilterList, contentDescription = "Otwórz filtry") // <-- Ikona filtra
                        }
                        // Usunięto samodzielny blok { Icon(...) } który powodował błąd składniowy.


                        // Prawy przycisk: Pokaż Ulubione / Pokaż Wszystkie (przełącznik stanu filtra)
                        IconButton(
                            onClick = {
                                // Przełącz kryterium filtra w ViewModelu
                                // currentFilterValue to String?
                                if (currentFilterValue == FAVORITES_FILTER_KEY) {
                                    // Jeśli obecnie jest filtr ulubionych, wróć do wszystkich
                                    mapViewModel.applyFilter(null) // Ustaw _currentFilter na null w ViewModelu (pokaż wszystkie)
                                } else {
                                    // Jeśli nie jest filtr ulubionych, ustaw filtr na ulubione
                                    mapViewModel.showFavorites() // Ustaw _currentFilter na FAVORITES_FILTER_KEY w ViewModelu
                                }
                            },
                            modifier = Modifier.padding(4.dp) // Mały padding wokół ikony
                        ) {
                            // Ikona zmienia się w zależności od tego, czy AKTYWNY jest filtr ulubionych
                            // Sprawdzamy, czy currentFilterValue jest równy kluczowi filtra ulubionych
                            val isFavoritesFilterActive = currentFilterValue == FAVORITES_FILTER_KEY // <-- Ta linia jest poprawna
                            val icon = if (isFavoritesFilterActive) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder
                            val tint = if (isFavoritesFilterActive) MaterialTheme.colorScheme.primary else LocalContentColor.current // Kolor ikony
                            val contentDescription = if (isFavoritesFilterActive) "Pokaż wszystkie wydarzenia" else "Pokaż ulubione wydarzenia" // Opis dostępności dla TalkBack
                            Icon(
                                imageVector = icon,
                                contentDescription = contentDescription,
                                tint = tint // Ustaw kolor ikony
                            )
                        }
                    }
                } // Koniec Row dla przycisków w górze sheeta


                // --- Główna treść panelu (poniżej górnego paska przycisków) ---
                // Wyświetla widok szczegółów lub listę, w zależności od selectedEvent
                // NIE DEFINIUJ currentSelectedEvent PONOWNIE TUTAJ
                if (currentSelectedEvent != null) { // Używamy zmiennej zdefiniowanej na początku sheetContent
                    // Widok SZCZEGÓŁÓW Wydarzenia
                    // Sprawdz, czy wybrane wydarzenie jest na liscie ulubionych (potrzebne dla EventDetailsView)
                    val isEventFavorite = favoriteEvents.contains(currentSelectedEvent)

                    EventDetailsView(
                        event = currentSelectedEvent, // Przekaż obiekt wydarzenia do widoku szczegółów
                        modifier = Modifier
                            .fillMaxWidth() // Wypełnia szerokość Column
                            .height(400.dp) // <-- UPEWNIJ SIE ZE TA WARTOSC JEST ZGODNA z obliczeniami zoomu (yOffsetDp) i wygladem
                            .padding(horizontal = 0.dp), // Padding horyzontalny jest w EventDetailsView Column, nie tutaj
                        // <-- PRZEKAŻ STATUS ULUBIONEGO I LAMBDĘ KLIKNIĘCIA PRZYCISKU ULUBIONYCH (dla przycisku w samym EventDetailsView)
                        isFavorite = isEventFavorite, // Przekazanie statusu ulubionego do EventDetailsView
                        onFavoriteClick = { eventToToggle -> // Lambda wywoływana po kliknięciu przycisku ulubionych WIDOKU SZCZEGÓŁÓW
                            mapViewModel.toggleFavorite(eventToToggle) // Wywołaj funkcję ViewModelu
                        }
                        // TODO: Przekaż lambda dla przycisku Nawiguj w EventDetailsView w przyszlosci
                        // onNavigateClick = { eventToNavigate -> /* TODO: Zaimplementuj nawigację */ }
                    )
                } else {
                    // Widok LISTY Wydarzeń
                    // Lista wydarzeń będzie automatycznie filtrowana przez StateFlow filteredEvents
                    Column(modifier = Modifier
                        .fillMaxWidth() // Wypełnia szerokość Column
                        .heightIn(max = 600.dp) // Maksymalna wysokość dla listy, aby nie zajmowała całego ekranu
                        .padding(horizontal = 16.dp) // Padding horyzontalny dla całej kolumny listy
                    ) {
                        // Opcjonalny nagłówek listy (możesz go usunąć, jeśli górny pasek przycisków pełni rolę nagłówka)
                        /*
                         Text(
                             text = "Lista Wydarzeń",
                             modifier = Modifier
                                 .fillMaxWidth(),
                             textAlign = androidx.compose.ui.text.style.TextAlign.Center
                         )
                         */
                        // LazyColumn do wyświetlania listy wydarzeń
                        // contentPadding uwzględnia SystemBars (np. pasek nawigacyjny na dole)
                        val systemBarPadding = WindowInsets.systemBars.asPaddingValues()
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 0.dp, // Padding po lewej stronie elementów listy
                                end = 0.dp, // Padding po prawej stronie elementów listy
                                top = 4.dp, // Padding na górze listy
                                bottom = 4.dp.plus(systemBarPadding.calculateBottomPadding()) // Padding na dole listy + padding od paska systemowego
                            )
                        ) {
                            // Wyświetla elementy listy dla każdego wydarzenia w filteredEvents
                            items(filteredEvents) { event -> // filteredEvents jest już filtrowane przez ViewModel
                                EventListItem(
                                    event = event, // Przekaż obiekt wydarzenia do elementu listy
                                    onEventClick = { clickedEvent ->
                                        mapViewModel.selectEventForDetails(clickedEvent) // Kliknięcie elementu listy ustawia selectedEvent
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },

        sheetPeekHeight = 100.dp, // Wysokość panelu, gdy jest zwinięty

    ) { paddingValues -> // Padding nałożony przez Scaffold na zawartość poniżej panelu
        // --- Zawartość ekranu pod panelem (mapa) ---
        OsmMapView(
            markers = filteredEvents, // Mapa wyświetla markery dla przefiltrowanych wydarzeń
            modifier = Modifier
                .fillMaxSize() // Mapa wypełnia całą dostępną przestrzeń
                // Dodaj padding na dole, aby mapa nie była zasłonięta przez pasek nawigacyjny na dole ekranu
                .padding(bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()),
            onMapViewReady = { map ->
                mapView = map // Zapisz instancję MapView w stanie
            },
            onMarkerClick = { clickedEvent ->
                mapViewModel.selectEventForDetails(clickedEvent) // Kliknięcie markera ustawia selectedEvent (wyświetla szczegóły i zoomuje)
                true // Zwróć true, aby skonsumować zdarzenie i nie pokazywać domyślnego dymka markera osmdroid
            }
        )
    }
}