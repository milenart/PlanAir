package pl.pw.planair

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import pl.pw.planair.ui.theme.PlanAirTheme
import org.osmdroid.config.Configuration
import pl.pw.planair.data.loadMarkersFromJson
import pl.pw.planair.ui.screens.MainScreenWithMapAndList


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // trzeba sprawdzic jak wyglada na innych telefonach
        //enableEdgeToEdge()
        Configuration.getInstance().userAgentValue = applicationContext.packageName

        val eventList = loadMarkersFromJson(applicationContext, R.raw.events)

        setContent {
            PlanAirTheme {
                MainScreenWithMapAndList(events = eventList)
            }
        }
    }

    }











