package pl.pw.planair.navigation // Upewnij sie, ze nazwa pakietu jest poprawna

/**
 * Definiuje trasy (routes) dla wszystkich ekranów w aplikacji.
 * Używa sealed class dla bezpieczeństwa typów i unikalności tras.
 */
sealed class AppScreens(val route: String) {

    // Obiekt reprezentujący trasę do Ekranu Powitalnego
    object IntroScreen : AppScreens("intro_screen")

    // Obiekt reprezentujący trasę do Ekranu Mapy i Listy.
    // Trasa zawiera argument "filter", który bedzie przekazywany z Ekranu Powitalnego.
    // Argument w trasie jest oznaczony {nazwa_argumentu}.
    object MapScreen : AppScreens("map_screen") {
        // Definiujemy tez nazwe argumentu jako stala
        const val FILTER_ARG = "filter"

        // Funkcja pomocnicza do tworzenia pełnej trasy z argumentem
        // Np. AppScreens.MapScreen.createRoute("SPORT") da "map_screen/SPORT"
        fun createRoute(filter: String?): String {
            // Jesli filtr jest null, uzyj specjalnej wartosci "null" lub pomysl o innej strategii
            // Na potrzeby trasy, null string nie jest idealny. Mozemy np. przejsc na trase bazowa
            // lub uzyc placeholderu. Na razie zalożmy, że zawsze bedziemy przekazywać stringa
            // nawet "null" jako string, a ViewModel sobie z tym poradzi.
            // LUB zdefiniuj trasę bez argumentu i trasę z argumentem jeśli nawigacja jest opcjonalna
            return if (filter == null) {
                // Jesli filtr = null (dla "Wszystko"), nawiguj na trase bazowa bez argumentu
                // Wymaga to osobnego composable(route = AppScreens.MapScreen.route) {} w NavHost
                MapScreen.route // "map_screen"
            } else {
                // Nawiguj na trase z konkretnym filtrem
                "${MapScreen.route}/$filter" // "map_screen/SPORT"
            }
            // Powyzsze wymaga dostosowania konfiguracji NavHost!
            // Na razie trzymajmy sie prostszego podejscia z argumentem w trasie,
            // a w Main Activity, jesli filter == null, przekazemy do ViewModelu null
        }
    }

    // TODO: Dodaj tutaj obiekty dla innych ekranów, jeśli będziesz je tworzyć
    // object EventDetailsScreen : AppScreens("event_details/{${EventDetailsScreen.EVENT_ID_ARG}}") {
    //     const val EVENT_ID_ARG = "eventId"
    // }
}