package pl.pw.planair.ui.screens

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
import pl.pw.planair.ui.components.EventListItem
import pl.pw.planair.ui.components.OsmMapView
import pl.pw.planair.ui.map.viewmodel.MapViewModel
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenWithMapAndList(
    mapViewModel: MapViewModel = viewModel(),
    onNavigateToFilter: (initialCategory: String?) -> Unit
) {
    val filteredEvents by mapViewModel.filteredEvents.collectAsState()
    val selectedEvent by mapViewModel.selectedEvent.collectAsState()
    val currentFilterState by mapViewModel.currentFilterState.collectAsState()
    val favoriteEvents by mapViewModel.favoriteEvents.collectAsState()

    val isFavorite by remember(selectedEvent, favoriteEvents) {
        derivedStateOf { selectedEvent?.let { favoriteEvents.contains(it) } ?: false }
    }

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

    // Efekt do centrowania mapy
    LaunchedEffect(selectedEvent, userLocation, mapView) {
        val currentMapView = mapView ?: return@LaunchedEffect

        selectedEvent?.let { event ->
            event.location?.coordinates?.coordinates?.let { coords ->
                if (coords.size >= 2) {
                    val geoPoint = GeoPoint(coords[1], coords[0])

                    // Sprawdź, czy mapa jest gotowa (ma wymiary)
                    if (currentMapView.width == 0 || currentMapView.height == 0) {
                        Log.w("MapAndListScreen", "Mapa niegotowa (brak wymiarów).")
                        return@LaunchedEffect
                    }

                    val projection = currentMapView.projection
                    val screenPoint = projection.toPixels(geoPoint, null)

                    // Stałe przesunięcie w pikselach (np. 200px w górę)
                    val offsetYPx = 75f
                    val newCenter = try {
                        projection.fromPixels(screenPoint.x, (screenPoint.y + offsetYPx).toInt()) as? GeoPoint
                    } catch (e: Exception) {
                        Log.e("MapAndListScreen", "Błąd konwersji z pikseli na GeoPoint: ${e.message}", e)
                        geoPoint
                    }

                    currentMapView.controller?.setZoom(15.0)
                    currentMapView.controller?.animateTo(newCenter)
                    Log.d("MapAndListScreen", "Centrowanie z przesunięciem (GeoPoint=$newCenter)")
                }
            }
        } ?: userLocation?.let { loc ->
            currentMapView.controller?.animateTo(GeoPoint(loc.latitude, loc.longitude), 13.0, 500L)
            Log.d("MapAndListScreen", "Centrowanie na lokalizacji użytkownika.")
        } ?: run {
            if (filteredEvents.isNotEmpty() && currentFilterState.filterLocation.latitude != null) {
                currentMapView.controller?.animateTo(
                    GeoPoint(currentFilterState.filterLocation.latitude!!, currentFilterState.filterLocation.longitude!!),
                    10.0, 500L
                )
            } else {
                currentMapView.controller?.animateTo(GeoPoint(52.2297, 21.0122), 10.0, 500L) // Warszawa
            }
            Log.d("MapAndListScreen", "Centrowanie na domyślnej lokalizacji lub lokalizacji filtra.")
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
            TopAppBar(
                title = { Text("PlanAir") },
                actions = {
                    IconButton(onClick = {
                        val initialFilter = if (currentFilterState.showOnlyFavorites) {
                            pl.pw.planair.ui.map.viewmodel.FAVORITES_FILTER_KEY
                        } else {
                            currentFilterState.category?.name
                        }
                        onNavigateToFilter(initialFilter)
                    }) {
                        Icon(Icons.Filled.FilterList, contentDescription = "Filtruj")
                    }
                    IconButton(onClick = { mapViewModel.fetchLastLocation() }) {
                        Icon(Icons.Filled.LocationOn, contentDescription = "Moja lokalizacja")
                    }
                }
            )
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
                    modifier = Modifier.fillMaxWidth()
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
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    // Usuń dodatkowy przycisk "Powrót do listy", jeśli go tam miałeś
                } else {

                    if (filteredEvents.isEmpty()) {
                        Text(
                            text = "Brak wydarzeń spełniających kryteria.",
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(), // Wypełnij dostępną szerokość w Column
                            contentPadding = PaddingValues(horizontal = 8.dp) // Padding dla elementów listy
                        ) {
                            items(filteredEvents, key = { "${it.date}_${it.title}" }) { event ->
                                EventListItem(
                                    event = event,
                                    onEventClick = { clickedEvent ->
                                        mapViewModel.selectEventForDetails(clickedEvent)
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
                    // Nie ma potrzeby ręcznego rozwijania, LaunchedEffect to obsłuży
                    true
                }
            )
        }
    }
}