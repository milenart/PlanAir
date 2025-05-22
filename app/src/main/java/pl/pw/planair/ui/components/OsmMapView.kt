// pl.pw.planair.ui.components/OsmMapView.kt

package pl.pw.planair.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import android.util.Log
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import pl.pw.planair.data.Event
import pl.pw.planair.data.EventCategory
import pl.pw.planair.R

@Composable
fun OsmMapView(
    markers: List<Event>,
    modifier: Modifier = Modifier,
    onMapViewReady: (MapView) -> Unit = {},
    onMarkerClick: (Event) -> Boolean = { false }
) {
    val context = LocalContext.current
    val osmConfig = remember { Configuration.getInstance() }
    osmConfig.load(context, PreferenceManager.getDefaultSharedPreferences(context))

    AndroidView(
        factory = {
            MapView(it).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(12.0)
                controller.setCenter(GeoPoint(52.2297, 21.0122)) // Centrum Warszawy

                onMapViewReady(this)
            }
        },
        modifier = modifier,
        update = { view ->
            view.overlays.clear() // Wyczyść stare markery

            markers.forEach { event ->
                // Pamiętaj, że koordynaty w JSON to [longitude, latitude]
                val longitude = event.location?.coordinates?.coordinates?.getOrNull(0)
                val latitude = event.location?.coordinates?.coordinates?.getOrNull(1)

                if (latitude != null && longitude != null) {
                    val geoPoint = GeoPoint(latitude, longitude)
                    val marker = Marker(view)
                    marker.position = geoPoint
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    marker.title = event.title
                    marker.snippet = event.description

                    // Ustawianie ikon na podstawie kategorii
                    val iconDrawable: Drawable? = when (event.category) {
                        EventCategory.SPORT -> ContextCompat.getDrawable(context, R.drawable.sport_2)
                        EventCategory.KULTURA -> ContextCompat.getDrawable(context, R.drawable.kultura)
                        EventCategory.EDUKACJA -> ContextCompat.getDrawable(context, R.drawable.edukacja_1_1)
                        EventCategory.AKTYWNOSC_SPOLECZNA -> ContextCompat.getDrawable(context, R.drawable.spoleczne)
                        EventCategory.OTHER -> ContextCompat.getDrawable(context, R.drawable.other)
                    }

                    iconDrawable?.let {
                        marker.setIcon(it)
                    }

                    marker.setOnMarkerClickListener { clickedMarker, mapView ->
                        onMarkerClick(event)
                    }
                    view.overlays.add(marker)
                } else {
                    Log.w("OsmMapView", "Brak poprawnych koordynatów dla wydarzenia: ${event.title}")
                }
            }
            view.invalidate()
        }
    )
}