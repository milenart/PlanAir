package pl.pw.planair

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pl.pw.planair.ui.theme.PlanAirTheme
import org.osmdroid.config.Configuration
import pl.pw.planair.data.loadMarkersFromJson
import pl.pw.planair.ui.map.viewmodel.MapViewModel
import pl.pw.planair.ui.screen.IntroScreen // Dodaj ten import
import pl.pw.planair.ui.screen.MainScreenWithMapAndList
import androidx.navigation.NavHostController // <-- Importuj kontroler nawigacji
import androidx.navigation.compose.rememberNavController // <-- Importuj funkcje do zapamietywania kontrolera
import pl.pw.planair.navigation.AppNavHost // <-- Importuj swoj NavHost Composable
import pl.pw.planair.navigation.AppScreens
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlanAirTheme { // Twoj motyw aplikacji
                // Glowny kontener UI, wypelnia caly ekran
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // ZMIANA TUTAJ: Uzyj swojego AppNavHost
                    val navController = rememberNavController() // Utworz i zapamietaj kontroler nawigacji
                    AppNavHost(navController = navController) // <-- Wywolaj swoj komponent NavHost
                }
            }
        }
    }
}











