package pl.pw.planair.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import pl.pw.planair.data.Event

@Composable
fun OsmMapView(
    markers: List<Event>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(13.0)
            controller.setCenter(GeoPoint(52.235, 21.015))

            val markersOverlay = overlays

            markers.forEach { event ->
                val marker = Marker(this)
                marker.position = GeoPoint(event.lat, event.lon)
                marker.title = event.title
                marker.snippet = event.description

//                    val drawableResId = getMarkerDrawableRes(event.category)
//                    val markerIcon = ContextCompat.getDrawable(context, drawableResId)
//                    marker.setIcon(markerIcon)

                markersOverlay.add(marker)
            }
            //invalidate()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            mapView
        },
        update = { view ->
        }
    )
}