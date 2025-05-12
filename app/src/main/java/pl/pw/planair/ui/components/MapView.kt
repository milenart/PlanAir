package pl.pw.planair.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView // Upewnij sie ze masz ten import

// Potrzebne importy dla osmdroid i markerow
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

import pl.pw.planair.data.Event // Upewnij sie ze importujesz Twoja klase Event
// Importy do obslugi ikon markerow (jesli odkomentujesz kod)
// import androidx.core.content.ContextCompat
// import pl.pw.planair.R // Zalezy gdzie masz zasoby ikon, moze byc inny pakiet R

@Composable
fun OsmMapView(
    markers: List<Event>, // Lista wydarzeń/markerów przekazywana do komponentu
    modifier: Modifier = Modifier,
    onMapViewReady: (MapView) -> Unit = {}
) {
    val context = LocalContext.current

    // Stworzenie i konfiguracja MapView odbywa sie tylko RAZ w bloku remember
    val mapView = remember {
        MapView(context).apply {
            // Podstawowa konfiguracja mapy
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
//            controller.setZoom(13.0)
//            controller.setCenter(GeoPoint(52.235, 21.015))

        }
    }

    // AndroidView hostuje widok Androida (MapView osmdroid) w Compose
    // Blok update {} jest wywoływany ZAWSZE, gdy zmienia sie ktorykolwiek z parametrow Composable (czyli lista 'markers')
    AndroidView(
        modifier = modifier,
        factory = {
            onMapViewReady(mapView)
            mapView // Zwracamy instancje MapView stworzona w remember
        },
        update = { view -> // <-- TEN BLOK BEDZIE WYKONANY ZA KAZDYM RAZEM GDY markers SIE ZMIENI
            // Tutaj umieszczamy logike, ktora ma dzialac na instancji MapView (view)
            // w odpowiedzi na zmiany parametrow Composable (markers)

            // 1. Wyczyść istniejące markery z mapy
            // WAZNE: Nie usuwaj wszystkich nakladek, bo usuniesz tez warstwe z kafelkami mapy!
            // Usun tylko te nakladki, ktore sa markerami.
            val overlaysToRemove = view.overlays.filterIsInstance<Marker>()
            view.overlays.removeAll(overlaysToRemove)

            // 2. Dodaj nowe markery na podstawie aktualnej listy 'markers'
            markers.forEach { event ->
                val marker = Marker(view) // Stworz nowy marker
                marker.position = GeoPoint(event.lat, event.lon) // Ustaw pozycje z obiektu Event
                marker.title = event.title // Ustaw tytul z obiektu Event
                marker.snippet = event.description // Ustaw opis z obiektu Event
                view.overlays.add(marker) // Dodaj nowy marker do nakladek mapy
            }

            // 3. Odśwież widok mapy, aby wyświetlić nowe markery
            view.invalidate() // Wymus przerysowanie mapy
        }
    )
}

// TODO: Funkcja pomocnicza do wyboru ikony markera (przykladowa, jesli chcesz uzywac ikon)
/*
fun getMarkerDrawableRes(categoryName: String): Int {
     return when(categoryName) {
         "SPORT" -> R.drawable.ic_marker_sport // Zmien na swoje ID zasobu ikony
         "KULTURA" -> R.drawable.ic_marker_kultura
         "EDUKACJA" -> R.drawable.ic_marker_edu
         "AKTYWNOSC_SPOLECZNA" -> R.drawable.ic_marker_spoleczna
         else -> R.drawable.ic_marker_default
     }
}
*/