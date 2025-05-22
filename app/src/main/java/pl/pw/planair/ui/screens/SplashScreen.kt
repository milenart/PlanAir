package pl.pw.planair.ui.screens // lub odpowiedni pakiet

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
// import androidx.compose.ui.graphics.Color // Jeśli nie używasz bezpośrednio, można usunąć
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import pl.pw.planair.R // Upewnij się, że R jest poprawnie zaimportowane
import pl.pw.planair.navigation.AppScreens // Zaimportuj swój zaktualizowany AppScreens

@Composable
fun SplashScreen(navController: NavController) {
    // Czas wyświetlania ekranu powitalnego w milisekundach
    val splashScreenDurationMillis = 2000L // 2 sekundy, możesz dostosować

    LaunchedEffect(key1 = true) {
        delay(splashScreenDurationMillis)
        // Po odczekaniu, nawiguj do ekranu IntroScreen
        // i usuń SplashScreen ze stosu nawigacji
        navController.navigate(AppScreens.IntroScreen.route) { // <--- ZMIANA: Nawiguj do IntroScreen
            popUpTo(AppScreens.SplashScreen.route) {         // <--- ZMIANA: Usuń ten SplashScreen
                inclusive = true // Usuń również sam SplashScreen ze stosu
            }
            launchSingleTop = true // Opcjonalnie: aby uniknąć wielokrotnego tworzenia IntroScreen, jeśli już jest na górze stosu
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo), // UPEWNIJ SIĘ, ŻE NAZWA PLIKU LOGO JEST POPRAWNA (np. logo.png)
            contentDescription = "Logo aplikacji",
            modifier = Modifier.size(300.dp) // Dostosuj rozmiar logo
        )
        // Możesz tu dodać tekst, np. nazwę aplikacji, jeśli chcesz
        // Text(text = "PlanAir", style = MaterialTheme.typography.headlineMedium)
    }
}