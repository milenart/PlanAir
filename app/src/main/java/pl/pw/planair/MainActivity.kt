package pl.pw.planair

import android.os.Bundle
import android.util.Log
// import androidx.activity.ComponentActivity // Zmieniasz na AppCompatActivity, więc to może nie być potrzebne
import androidx.activity.compose.setContent
// import androidx.activity.enableEdgeToEdge // Zakomentowane, więc zostawiam
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
// import androidx.compose.runtime.LaunchedEffect // Nie jest tu bezpośrednio używany
// import androidx.lifecycle.viewmodel.compose.viewModel // Nie tworzysz tu ViewModelu bezpośrednio
// import androidx.navigation.NavType // Importy związane z nawigacją są ok, jeśli AppNavHost ich używa
// import androidx.navigation.compose.NavHost
// import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
// import androidx.navigation.navArgument
import pl.pw.planair.ui.theme.PlanAirTheme
// import org.osmdroid.config.Configuration // Jeśli konfiguracja OSM jest gdzie indziej, to ok
// import pl.pw.planair.data.loadMarkersFromJson // Jeśli ładowanie danych jest gdzie indziej, to ok
// import pl.pw.planair.ui.map.viewmodel.MapViewModel // Nie tworzysz tu ViewModelu bezpośrednio
// import pl.pw.planair.ui.screens.IntroScreen // Jeśli AppNavHost zarządza IntroScreen, to ok
// import pl.pw.planair.ui.screens.MainScreenWithMapAndList // Jeśli AppNavHost zarządza tym ekranem, to ok
// import androidx.navigation.NavHostController
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.appcompat.app.AppCompatActivity // Używasz AppCompatActivity
import pl.pw.planair.navigation.AppNavHost


class MainActivity : AppCompatActivity() { // Dziedziczysz z AppCompatActivity, to jest OK


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(
                "UncaughtException",
                "Wyjątek na wątku ${thread.name}: ${throwable.message}",
                throwable
            )
        }

        setContent {
            PlanAirTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavHost(navController = navController)
                }
            }
        }
    }
}