package pl.pw.planair.data
import androidx.annotation.DrawableRes

// Klasa danych dla pojedynczego boxa na ekranie powitalnym
data class IntroButtonData(
    val text: String, // Tekst wyswietlany na przycisku
    val filterCategory: String?,
    @DrawableRes val imageResId: Int? = null
)