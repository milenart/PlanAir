package pl.pw.planair.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.graphics.drawable.Drawable // Import klasy Drawable
import androidx.core.content.ContextCompat // Potrzebne do ladowania Drawables
import android.util.Log // Do logowania
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase

import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker // Import klasy Marker
import org.osmdroid.views.overlay.Overlay

import pl.pw.planair.data.Event // Upewnij sie, ze to Twoja klasa Event
import pl.pw.planair.data.EventCategory // Upewnij sie, ze to Twoj enum EventCategory
import pl.pw.planair.R // <-- Upewnij sie, ze masz import do zasobów R


@Composable
fun OsmMapView(
    markers: List<Event>, // Lista wydarzeń do wyświetlenia jako markery
    modifier: Modifier = Modifier, // Modifier dla komponowalnego
    onMapViewReady: (MapView) -> Unit = {}, // Lambda wywoływana, gdy MapView jest gotowe
    // <-- DODANY PARAMETR LAMBDA DLA KLIKNIĘCIA MARKERA (Z KROKU 4.1)
    onMarkerClick: (Event) -> Boolean = { false } // Lambda wywoływana po kliknięciu markera. Zwraca Boolean (czy zdarzenie klikniecia zostalo skonsumowane).
) {
    val context = LocalContext.current // Pobierz kontekst Androida


    val jawgTileSource = object : OnlineTileSourceBase(
        "Jawg Streets",
        0, 20, 256, "",
        arrayOf("https://tile.jawg.io/jawg-streets/")
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val token = "iNKMQnoSvQTfrUSABYgonO6Or3YgYb5eFJr2ulx1BCn9dcjygRwtYNcsPpliOwrl"

            return "https://tile.jawg.io/jawg-streets/$z/$x/$y.png?access-token=$token"
        }
    }



    // Utworzenie i zapamiętanie instancji MapView
    val mapView = remember {
        MapView(context).apply {
            // Konfiguracja osmdroid
            Configuration.getInstance().load(
                context,
                PreferenceManager.getDefaultSharedPreferences(context)
            )
            //setTileSource(jawgTileSource)
            setTileSource(TileSourceFactory.MAPNIK)

            setMultiTouchControls(true) // umożliwia pinch to zoom
        }
    }



    // Kompozycja AndroidView, która hostuje MapView
    AndroidView(
        modifier = modifier,
        factory = {
            onMapViewReady(mapView) // Przekaż instancję MapView do rodzica (MapAndListScreen)
            mapView // Zwróć MapView do hostowania
        },
        update = { view ->
            // Logika aktualizacji markerów za kazdym razem, gdy zmieni sie lista 'markers'
            Log.d("OsmMapView", "Updating map markers. Count: ${markers.size}")

            // Wyczyść TYLKO stare markery, ale ZACHOWAJ inne nakładki (np. domyślne warstwy osmdroid, jeśli jakieś są)
            val overlaysToRemove = view.overlays.filterIsInstance<Marker>()
            if (overlaysToRemove.isNotEmpty()) {
                Log.d("OsmMapView", "Removing ${overlaysToRemove.size} old markers.")
                view.overlays.removeAll(overlaysToRemove)
            } else {
                Log.d("OsmMapView", "No old markers to remove.")
            }


            // Dodaj nowe markery na mapę
            markers.forEach { event ->
                val marker = Marker(view) // Utworz nowy obiekt Marker
                marker.position = GeoPoint(event.lat, event.lon) // Ustaw pozycję markera
                marker.title = event.title // Ustaw tytuł (moze byc widoczny w dymku)
                marker.snippet = event.description // Ustaw snippet/opis (moze byc widoczny w dymku)

                // <-- Ładowanie i ustawianie ikony zależnie od kategorii (Z KROKU 4.1, CZĘŚĆ 2)
                val iconDrawable: Drawable? = when (event.category) {
                    EventCategory.EDUKACJA -> ContextCompat.getDrawable(context, R.drawable.edukacja) // Zmień ID na Twoje
                    EventCategory.KULTURA -> ContextCompat.getDrawable(context, R.drawable.kultura) // Zmień ID na Twoje
                    EventCategory.SPORT -> ContextCompat.getDrawable(context, R.drawable.sport_2) // Zmień ID na Twoje
                    EventCategory.AKTYWNOSC_SPOLECZNA -> ContextCompat.getDrawable(context, R.drawable.spoleczne) // Zmień ID na Twoje
                    EventCategory.OTHER -> ContextCompat.getDrawable(context, R.drawable.spoleczne)
                }

                iconDrawable?.let { // Jeśli Drawable zostało poprawnie załadowane
                    marker.setIcon(it) // Ustaw ikonę markera
                    // Opcjonalnie mozesz dostosowac punkt zakotwiczenia ikony (gdzie ikona "trzyma się" punktu GeoPoint)
                    // marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM) // Przyklad: srodek X, dół Y ikony bedzie na punkcie GeoPoint
                }


                // <-- DODAJ NASŁUCHIWACZ KLIKNIĘĆ DO MARKERA (Z KROKU 4.2)
                marker.setOnMarkerClickListener { clickedMarker, mapView ->
                    // Po kliknieciu markera, wywołaj lambda onMarkerClick przekazana z nadrzednego komponentu
                    // Obiekt 'event' z pętli forEach jest dostępny w zasięgu tej lambdy i odnosi się do tego markera
                    onMarkerClick(event) // <-- Wywołaj lambda z obiektem Event, który został kliknięty

                    true // Zwróć true, aby poinformować, że kliknięcie zostało obsłużone i NIE wyświetlać domyślnego dymka osmdroid
                }

                view.overlays.add(marker) // Dodaj marker do nakładek mapy
            }
            view.invalidate() // Odśwież widok mapy, aby markery się pojawiły
            Log.d("OsmMapView", "Finished updating map markers.")
        }
    )
}