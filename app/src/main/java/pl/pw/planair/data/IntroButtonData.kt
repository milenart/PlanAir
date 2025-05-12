package pl.pw.planair.data

// Klasa danych dla pojedynczego boxa na ekranie powitalnym
data class IntroButtonData(
    val text: String, // Tekst wyswietlany na przycisku
    val filterCategory: String? // Kategoria wydarzen do przefiltrowania (null dla "Wszystko")
)