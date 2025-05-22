package pl.pw.planair.ui.screens

import EventListItem
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch // Dla coroutineScope
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import pl.pw.planair.data.EventCategory // Upewnij się, że jest import
import pl.pw.planair.ui.components.EventDetailsView
import pl.pw.planair.ui.components.OsmMapView
import pl.pw.planair.ui.map.viewmodel.MapViewModel
import android.util.Log
import androidx.compose.runtime.saveable.rememberSaveable
import pl.pw.planair.data.LocationType
import pl.pw.planair.data.generateUniqueKey
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import pl.pw.planair.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenWithMapAndList(
    mapViewModel: MapViewModel = viewModel(),
    onNavigateToFilter: (initialCategory: String?) -> Unit
) {
    val filteredEvents by mapViewModel.filteredEvents.collectAsState()
    val selectedEvent by mapViewModel.selectedEvent.collectAsState()
    val currentFilterState by mapViewModel.currentFilterState.collectAsState()
    val favoriteEventIds by mapViewModel.favoriteEventIds.collectAsState()
    val eventForNavigationDialog by mapViewModel.showNavigationDialog.collectAsState()

    val isFavorite by remember(selectedEvent, favoriteEventIds) { // Zmienione z favoriteEvents na favoriteEventIds
        derivedStateOf {
            selectedEvent?.let { event ->
                val eventKey = event.generateUniqueKey() // Generujemy klucz dla wybranego wydarzenia
                favoriteEventIds.contains(eventKey)     // Sprawdzamy, czy klucz jest w zbiorze ID
            } ?: false
        }
    }
    var initialCenteringDone by rememberSaveable { mutableStateOf(false) }

    var mapView: MapView? by remember { mutableStateOf(null) }
    val userLocation by mapViewModel.userLocation.collectAsState()

    // Stan dla BottomSheetScaffold
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            // Rozpocznij częściowo rozwinięty, jeśli są wydarzenia, lub ukryty, jeśli nie ma
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = false // Pozwól na całkowite ukrycie, jeśli to pożądane
        )
    )
    val coroutineScope = rememberCoroutineScope()

    eventForNavigationDialog?.let { eventToShowDialogFor ->
        AlertDialog(
            onDismissRequest = { mapViewModel.dismissNavigationDialog() },
            title = { Text("Przejdź do Google Maps") },
            text = { Text("Czy na pewno chcesz otworzyć lokalizację tego wydarzenia w aplikacji Google Maps?") },
            confirmButton = {
                TextButton(onClick = {
                    mapViewModel.navigateToEventLocation(eventToShowDialogFor)
                }) {
                    Text("Tak")
                }
            },
            dismissButton = {
                TextButton(onClick = { mapViewModel.dismissNavigationDialog() }) {
                    Text("Anuluj")
                }
            }
        )
    }

    // Efekt do centrowania mapy
    LaunchedEffect(selectedEvent, userLocation, mapView, initialCenteringDone) {
        val currentMapView = mapView ?: return@LaunchedEffect

        // Sprawdź, czy mapa jest gotowa (ma wymiary)
        if (currentMapView.width == 0 || currentMapView.height == 0) {
            Log.w("MapAndListScreen", "Mapa niegotowa (brak wymiarów), odkładam centrowanie.")
            return@LaunchedEffect
        }

        if (selectedEvent != null) {
            // 1. Jeśli wybrano wydarzenie, centruj na nim z offsetem
            selectedEvent!!.location?.coordinates?.coordinates?.let { coords ->
                if (coords.size >= 2) {
                    val geoPoint = GeoPoint(coords[1], coords[0])
                    currentMapView.controller?.setZoom(15.0)
                    val projection = currentMapView.projection
                    val screenPoint = projection.toPixels(geoPoint, null)
                    val offsetYPx = 350f
                    val newCenter = try {
                        projection.fromPixels(screenPoint.x, (screenPoint.y + offsetYPx).toInt()) as? GeoPoint
                    } catch (e: Exception) {
                        Log.e("MapAndListScreen", "Błąd konwersji z pikseli na GeoPoint: ${e.message}", e)
                        geoPoint
                    }
                    currentMapView.controller?.animateTo(newCenter)
                    Log.d("MapAndListScreen", "Centrowanie na wybranym evencie z przesunięciem (GeoPoint=$newCenter)")
                    initialCenteringDone = true // Wybór eventu nadpisuje domyślne centrowanie
                }
            }
        } else if (!initialCenteringDone && currentFilterState.filterLocation.type != LocationType.USER_LOCATION) {
            // 2. Jeśli NIE wybrano wydarzenia, początkowe centrowanie NIE zostało nadpisane
            //    I filtr NIE jest jeszcze ustawiony na "Moja lokalizacja":
            //    Centruj na Warszawie.
            currentMapView.controller?.setZoom(12.0)
            currentMapView.controller?.animateTo(GeoPoint(52.2297, 21.0122)) // Warszawa
            Log.d("MapAndListScreen", "Wykonano początkowe/domyślne centrowanie na Warszawie.")
            // Nie ustawiamy tu initialCenteringDone = true, aby pozwolić na zmianę przez filtr.
            // Jeśli filtr zmieni się na USER_LOCATION, initialCenteringDone zostanie ustawione.
        } else {
            // 3. NIE wybrano wydarzenia, ALE (initialCenteringDone == true LUB filtr jest na USER_LOCATION)
            //    initialCenteringDone ustawiamy na true, jeśli filtr jest na USER_LOCATION,
            //    aby zaznaczyć, że użytkownik podjął świadomą decyzję.
            if (currentFilterState.filterLocation.type == LocationType.USER_LOCATION) {
                initialCenteringDone = true // Użytkownik wybrał "Moja lokalizacja"
                userLocation?.let { loc ->
                    currentMapView.controller?.setZoom(12.0)
                    currentMapView.controller?.animateTo(GeoPoint(loc.latitude, loc.longitude))
                    Log.d("MapAndListScreen", "Centrowanie na lokalizacji użytkownika (po filtrze).")
                } ?: run {
                    Log.d("MapAndListScreen", "Filtr na USER_LOCATION, ale userLocation jest null. Czekam...")
                    // Można tu ewentualnie wrócić do Warszawy, jeśli userLocation długo nie przychodzi
                    // currentMapView.controller?.animateTo(GeoPoint(52.2297, 21.0122))
                }
            } else if (currentFilterState.filterLocation.latitude != null && currentFilterState.filterLocation.longitude != null) {
                // Filtr jest ustawiony na konkretne współrzędne (inne niż USER_LOCATION)
                initialCenteringDone = true // Inny filtr też jest świadomą decyzją
                currentMapView.controller?.setZoom(12.0)
                currentMapView.controller?.animateTo(
                    GeoPoint(currentFilterState.filterLocation.latitude!!, currentFilterState.filterLocation.longitude!!)
                )
                Log.d("MapAndListScreen", "Centrowanie na lokalizacji z filtra: ${currentFilterState.filterLocation.name}.")
            } else if (!initialCenteringDone) {
                // Ostateczny fallback, jeśli nic innego nie pasuje i nie było jeszcze centrowania na Warszawie
                currentMapView.controller?.setZoom(12.0)
                currentMapView.controller?.animateTo(GeoPoint(52.2297, 21.0122)) // Warszawa
                Log.d("MapAndListScreen", "Fallback centrowanie na Warszawie (brak konkretnego celu).")
            }
        }
    }

    // Efekt do zarządzania stanem bottom sheet w zależności od selectedEvent
    LaunchedEffect(selectedEvent, filteredEvents, scaffoldState.bottomSheetState) { // Dodaj scaffoldState.bottomSheetState jako klucz, jeśli chcesz reagować na jego zmiany
        if (selectedEvent != null) {
            // Gdy wybrano wydarzenie, panel (50%) powinien się rozwinąć
            if (scaffoldState.bottomSheetState.currentValue != SheetValue.Expanded) { // Rozwiń tylko jeśli nie jest już rozwinięty
                coroutineScope.launch { scaffoldState.bottomSheetState.expand() }
            }
        } else {
            // Gdy widok listy (panel 75%)
            // Jeśli są wydarzenia i panel nie jest już rozwinięty (do 75%), rozwiń go.
            // Jeśli użytkownik go zwinął do peekHeight, może tam pozostać, chyba że chcesz go na siłę rozwijać.
            // Ta logika może wymagać dopasowania do preferowanego UX.
            // Poniżej przykład: jeśli są eventy i nie jest schowany, próbuj rozwinąć.
            if (filteredEvents.isNotEmpty() && !scaffoldState.bottomSheetState.hasPartiallyExpandedState) {
                // Jeśli chcesz, aby zawsze wracał do pełnego rozwinięcia (75%) przy liście:
                if (scaffoldState.bottomSheetState.currentValue != SheetValue.Expanded) {
                    coroutineScope.launch { scaffoldState.bottomSheetState.expand() }
                }
            } else if (filteredEvents.isEmpty() && selectedEvent == null) {
                // Opcjonalnie: jeśli nie ma eventów i nic nie jest wybrane, schowaj panel
                // if (scaffoldState.bottomSheetState.currentValue != SheetValue.Hidden && scaffoldState.bottomSheetState.currentValue != SheetValue.PartiallyExpanded) {
                //    coroutineScope.launch { scaffoldState.bottomSheetState.hide() } // Upewnij się, że skipHiddenState = false
                // }
                // Lub pozostaw w stanie częściowo rozwiniętym, jeśli peekHeight > 0
                if (scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) {
                    coroutineScope.launch { scaffoldState.bottomSheetState.partialExpand() }
                }
            }
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White) // Tło dla całego obszaru nagłówka
                // .height(TopAppBarDefaults. wysokości np. SmallTopAppBarHeight) // Opcjonalnie, jeśli chcesz ustalić wysokość
            ) {
                // Logo wyśrodkowane w Boxie (czyli na całej szerokości TopAppBar)
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo PlanAir",
                    modifier = Modifier
                        .align(Alignment.Center) // Centruje logo w Box
                        .height(40.dp)
                )

                // TopAppBar tylko dla ikon, z przezroczystym tłem
                TopAppBar(
                    title = { /* Puste, logo jest obsługiwane przez Box */ },
                    navigationIcon = {
                        IconButton(onClick = { mapViewModel.toggleShowOnlyFavorites() }) {
                            Icon(
                                imageVector = if (currentFilterState.showOnlyFavorites) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (currentFilterState.showOnlyFavorites) "Pokaż wszystkie wydarzenia" else "Pokaż tylko ulubione",
                                // Upewnij się, że kolor ikon jest widoczny na tle (Color.White)
                                tint = if (currentFilterState.showOnlyFavorites) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            mapViewModel.prepareForFilterScreenNavigation()
                            val currentFilters = mapViewModel.currentFilterState.value
                            val initialFilterValue = if (currentFilters.showOnlyFavorites) {
                                pl.pw.planair.ui.map.viewmodel.FAVORITES_FILTER_KEY
                            } else {
                                currentFilters.category?.name
                            }
                            onNavigateToFilter(initialFilterValue)
                        }) {
                            Icon(Icons.Filled.FilterList, contentDescription = "Filtruj" /*, tint = TwójKolorIkon */)
                        }
                        IconButton(onClick = { mapViewModel.fetchLastLocation() }) {
                            Icon(Icons.Filled.LocationOn, contentDescription = "Moja lokalizacja" /*, tint = TwójKolorIkon */)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent, // Kluczowe: TopAppBar jest przezroczysty
                        scrolledContainerColor = Color.Transparent // Dla stanu przewiniętego
                    )
                    // Możesz usunąć domyślny cień, jeśli nie jest potrzebny
                    // modifier = Modifier.shadow(0.dp)
                )
            }
        },
        sheetPeekHeight = if (selectedEvent == null && filteredEvents.isEmpty()) 0.dp else 120.dp, // Mniejsza wysokość, gdy lista jest pusta lub tylko tytuł
        sheetContent = {
            val targetMaxHeightFraction = if (selectedEvent == null) 0.75f else 0.5f
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(targetMaxHeightFraction)
                    .defaultMinSize(minHeight = 120.dp) // Minimalna wysokość, aby uniknąć zapadania się
                    //.padding(bottom = 16.dp) // Padding na dole treści sheetu
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedEvent != null) {
                        IconButton(
                            onClick = { mapViewModel.clearSelectedEvent() },
                            modifier = Modifier.size(60.dp) // Dopasuj rozmiar ikony, jeśli trzeba
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Powrót do listy"
                            )
                        }
                    }
                }

                if (selectedEvent != null) {
                    EventDetailsView(
                        event = selectedEvent!!,
                        isFavorite = isFavorite,
                        onFavoriteClick = { mapViewModel.toggleFavorite(it) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onNavigateClick = { eventToRequestNav ->
                            mapViewModel.requestNavigation(eventToRequestNav)}
                    )

                } else {
                    if (filteredEvents.isEmpty()) {
                        Text(
                            text = "Brak wydarzeń spełniających kryteria.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(), // Wypełnij dostępną szerokość w Column
                            contentPadding = PaddingValues(horizontal = 8.dp) // Padding dla elementów listy
                        ) {
                            // Używamy event.generateUniqueKey() dla stabilnych i unikalnych kluczy
                            items(filteredEvents, key = { event -> event.generateUniqueKey() }) { event ->
                                // Dla każdego elementu listy sprawdzamy, czy jego klucz jest w zbiorze ulubionych
                                val isItemFavorite = favoriteEventIds.contains(event.generateUniqueKey())

                                EventListItem(
                                    event = event,
                                    isFavorite = isItemFavorite, // Przekazujemy stan ulubionego dla tego elementu
                                    onEventClick = { clickedEvent ->
                                        mapViewModel.selectEventForDetails(clickedEvent)
                                    },
                                    onNavigateClick = { eventToRequestNav ->
                                        mapViewModel.requestNavigation(eventToRequestNav)},
                                    onFavoriteClick = { eventToToggle -> // Przekazujemy funkcję do przełączania ulubionego
                                        mapViewModel.toggleFavorite(eventToToggle)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { contentPadding -> // Główna zawartość ekranu (mapa)
        Box(modifier = Modifier.padding(contentPadding)) { // Box, aby OsmMapView mógł prawidłowo zająć przestrzeń
            OsmMapView(
                markers = filteredEvents,
                modifier = Modifier.fillMaxSize(), // Mapa wypełnia całą dostępną przestrzeň
                onMapViewReady = { map -> mapView = map },
                onMarkerClick = { clickedEvent ->
                    mapViewModel.selectEventForDetails(clickedEvent)
                    true
                },
                selectedEvent = selectedEvent,
            )
        }
    }
}
