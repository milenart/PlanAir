package pl.pw.planair.ui.screen
//Physical size: 1080x2400
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import pl.pw.planair.data.Event
import pl.pw.planair.ui.components.EventListItem
import pl.pw.planair.ui.components.OsmMapView
import pl.pw.planair.ui.components.EventDetailsView
import pl.pw.planair.ui.map.viewmodel.MapViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import org.osmdroid.views.MapView
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.BoundingBox
import kotlinx.coroutines.delay
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenWithMapAndList(
    mapViewModel: MapViewModel = viewModel()
) {
    val filteredEvents by mapViewModel.filteredEvents.collectAsState()
    val selectedEvent by mapViewModel.selectedEvent.collectAsState()
    var mapView by remember { mutableStateOf<MapView?>(null) }

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true
    )

    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)

    // Efekt dla widoku domyślnego (wszystkie markery)
    LaunchedEffect(filteredEvents, selectedEvent, mapView) {
        val currentMapView = mapView
        if (selectedEvent == null && filteredEvents.isNotEmpty() && currentMapView != null) {
            try {
                val boundingBox = BoundingBox.fromGeoPoints(filteredEvents.map { GeoPoint(it.lat, it.lon) })
                val percentage = 0.1
                val latHeight = boundingBox.latNorth - boundingBox.latSouth
                val lonWidth = boundingBox.lonEast - boundingBox.lonWest
                val latMargin = latHeight * percentage
                val lonMargin = lonWidth * percentage

                val paddedBox = BoundingBox(
                    boundingBox.latNorth + latMargin,
                    boundingBox.lonEast + lonMargin,
                    boundingBox.latSouth - latMargin,
                    boundingBox.lonWest - lonMargin
                )
                currentMapView.zoomToBoundingBox(paddedBox, true)
            } catch (e: Exception) {
                if (filteredEvents.size == 1) {
                    val singleMarkerLocation = GeoPoint(filteredEvents.first().lat, filteredEvents.first().lon)
                    currentMapView.controller.setCenter(singleMarkerLocation)
                    currentMapView.controller.setZoom(15.0)
                } else {
                    currentMapView.controller.setCenter(GeoPoint(52.2370, 21.0170))
                    currentMapView.controller.setZoom(10.0)
                }
            }
        } else if (selectedEvent == null && filteredEvents.isEmpty() && currentMapView != null) {
            currentMapView.controller.setCenter(GeoPoint(52.2370, 21.0170))
            currentMapView.controller.setZoom(6.0)
        }
    }

    // Efekt dla wybranego markera
    LaunchedEffect(selectedEvent, mapView, density) {
        val currentMapView = mapView ?: return@LaunchedEffect
        val event = selectedEvent ?: return@LaunchedEffect

        delay(300) // daj mapie czas

        if (currentMapView.width == 0 || currentMapView.height == 0) return@LaunchedEffect

        val yOffsetDp = 50.dp
        val yOffsetPx = with(density) { yOffsetDp.toPx() }

        val geoPoint = GeoPoint(event.lat, event.lon)
        val projection = currentMapView.projection
        val screenPoint = projection.toPixels(geoPoint, null)

        val newCenter = try {
            projection.fromPixels(screenPoint.x, (screenPoint.y + yOffsetPx).toInt()) as? GeoPoint
        } catch (e: Exception) {
            null
        } ?: geoPoint

        currentMapView.controller.setZoom(17.0)
        delay(200)
        currentMapView.controller.animateTo(newCenter)
    }


    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 75.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val currentSelectedEvent = selectedEvent
                    if (currentSelectedEvent != null) {
                        IconButton(
                            onClick = { mapViewModel.clearSelectedEvent() },
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Wstecz do listy")
                        }
                    }
                }

                if (selectedEvent != null) {
                    EventDetailsView(
                        event = selectedEvent!!,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((configuration.screenHeightDp * 0.4).dp)
                            .padding(horizontal = 16.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 600.dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "Lista Wydarzeń",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 0.dp,
                                end = 0.dp,
                                top = 4.dp,
                                bottom = 4.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
                            )
                        ) {
                            items(filteredEvents) { event ->
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
        },
        sheetPeekHeight = 75.dp
    ) { paddingValues ->
        OsmMapView(
            markers = filteredEvents,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()),
            onMapViewReady = { map ->
                mapView = map
            }
        )
    }
}