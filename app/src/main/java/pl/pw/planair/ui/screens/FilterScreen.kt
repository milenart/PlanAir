package pl.pw.planair.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.material.datepicker.MaterialDatePicker
import pl.pw.planair.data.* // Import wszystkich klas danych związanych z filtrami (EventCategory, PriceRange, FilterState)
import pl.pw.planair.ui.filter.FilterViewModel
import pl.pw.planair.utils.findFragmentActivity // Upewnij się, że ta ścieżka jest poprawna
import java.text.SimpleDateFormat
import java.util.*
import android.app.AlertDialog // Import dla Android View AlertDialog
import androidx.activity.result.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen(
    filterViewModel: FilterViewModel,
    onApplyFilters: (FilterState) -> Unit,
    onBackClick: () -> Unit
) {
    val filterState by filterViewModel.filterState.collectAsState()
    val context = LocalContext.current

    var showLocationDialog by remember { mutableStateOf(false) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            filterViewModel.setLocationPermissionGranted(true)
            filterViewModel.requestUserLocation()
        } else {
            filterViewModel.setLocationPermissionGranted(false)
            Toast.makeText(context, "Uprawnienia lokalizacyjne odrzucone.", Toast.LENGTH_SHORT)
                .show()
            filterViewModel.useDefaultLocation()
        }
    }

    // Funkcja do wyświetlania DatePicker.
    val showDatePicker = { isStartDateParam: Boolean ->
        val currentActivity = context.findFragmentActivity()

        currentActivity?.let { fragmentActivity ->
            val builder = MaterialDatePicker.Builder.datePicker()
            val currentSelection =
                if (isStartDateParam) filterState.startDate else filterState.endDate

            if (currentSelection != null) {
                builder.setSelection(currentSelection)
            } else {
                builder.setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            }

            val picker = builder
                .setTitleText(if (isStartDateParam) "Wybierz datę początkową" else "Wybierz datę końcową")
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                if (isStartDateParam) {
                    filterViewModel.updateStartDate(selection)
                } else {
                    filterViewModel.updateEndDate(selection)
                }
            }
            picker.show(fragmentActivity.supportFragmentManager, picker.toString())
        } ?: run {
            Toast.makeText(
                context,
                "Błąd: Brak dostępu do FragmentActivity dla wyboru daty.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Filtry") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                }
                // Możesz dodać kolory do TopAppBar, jeśli chcesz
                // colors = TopAppBarDefaults.topAppBarColors(
                // containerColor = MaterialTheme.colorScheme.primaryContainer,
                // titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                // )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            val sectionSpacing = 24.dp // Zwiększony odstęp między sekcjami

            // --- Kategoria ---
            Text(
                text = "Kategoria: ${filterState.getCategoryDisplayName()}",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(sectionSpacing))

            // --- Promień ---
            Text(
                text = "Promień: ${filterState.radiusKm.toInt()} km",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, // Wyśrodkowanie
                modifier = Modifier.fillMaxWidth()
            )
            Slider(
                value = filterState.radiusKm,
                onValueChange = { filterViewModel.updateRadius(it) },
                valueRange = 0f..100f,
                steps = 19,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(sectionSpacing))

            // --- Zakres cen ---
            Text(
                text = "Cena:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, // Wyśrodkowanie
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center, // Wyśrodkowuje grupę FilterChip
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Aby FilterChip były bliżej siebie, można usunąć dodatkowe Spacery
                // lub użyć mniejszego `spacedBy` jeśli jest taka potrzeba.
                // W tym przypadku Arrangement.Center powinno je ułożyć obok siebie.
                PriceRange.entries.forEach { priceOption ->
                    FilterChip(
                        selected = filterState.priceRange == priceOption,
                        onClick = { filterViewModel.updatePriceRange(priceOption) },
                        label = { Text(priceOption.displayName()) },
                        modifier = Modifier.padding(horizontal = 4.dp) // Mały padding, aby nie stykały się całkowicie
                    )
                }
            }
            Spacer(Modifier.height(sectionSpacing))

            // --- Zakres dat ---
            Text(
                text = "Data od:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, // Wyśrodkowanie
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { showDatePicker(true) },
                    modifier = Modifier
                        .height(48.dp)
                        .wrapContentWidth(),
                    contentPadding = PaddingValues(horizontal = 24.dp)
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = "Wybierz datę początkową")
                    Spacer(Modifier.width(8.dp))
                    val startDateText = filterState.startDate?.let {
                        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(it))
                    } ?: "Wybierz datę"
                    Text(text = startDateText)
                }
            }
            Spacer(Modifier.height(12.dp)) // Mniejszy odstęp między przyciskami dat

            Text(
                text = "Data do:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, // Wyśrodkowanie
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { showDatePicker(false) },
                    modifier = Modifier
                        .height(48.dp)
                        .wrapContentWidth(),
                    contentPadding = PaddingValues(horizontal = 24.dp)
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = "Wybierz datę końcową")
                    Spacer(Modifier.width(8.dp))
                    val endDateText = filterState.endDate?.let {
                        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(it))
                    } ?: "Wybierz datę"
                    Text(text = endDateText)
                }
            }
            Spacer(Modifier.height(sectionSpacing))

            // --- Lokalizacja ---
            Text(
                text = "Lokalizacja: ${filterState.filterLocation.name ?: "Nie wybrano"}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, // Wyśrodkowanie
                modifier = Modifier.fillMaxWidth()
            )
            Row( // Dodajemy Row, aby wyśrodkować przycisk lokalizacji
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { showLocationDialog = true },
                    modifier = Modifier // Usunięto fillMaxWidth, aby przycisk był mniejszy
                        .height(48.dp)
                        .wrapContentWidth(),
                    contentPadding = PaddingValues(horizontal = 24.dp)
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = "Wybierz lokalizację")
                    Spacer(Modifier.width(8.dp))
                    Text("Wybierz lokalizację")
                }
            }

            Spacer(Modifier.height(sectionSpacing + 8.dp)) // Trochę większy odstęp przed Divider

            // --- Kreska oddzielająca ---
            Divider(
                modifier = Modifier.fillMaxWidth(), // Usunięto .padding(horizontal = 0.dp) bo fillMaxWidth to załatwia
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(Modifier.height(sectionSpacing)) // Zwiększony odstęp

            // --- Przyciski akcji ---
            // Możemy je też trochę "upiększyć" jeśli chcesz, np. zaokrąglone rogi
            Button(
                onClick = { onApplyFilters(filterState) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp), // Lekko wyższy
                shape = RoundedCornerShape(12.dp) // Zaokrąglone rogi
            ) {
                Text("Zatwierdź filtry", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(Modifier.height(12.dp)) // Odstęp między przyciskami akcji

            OutlinedButton(
                onClick = {
                    filterViewModel.resetFilters() // Pamiętaj o problemie z kategorią przy resetowaniu
                    Toast.makeText(context, "Filtry zresetowane", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp), // Lekko wyższy
                shape = RoundedCornerShape(12.dp) // Zaokrąglone rogi
            ) {
                Text("Resetuj filtry", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(16.dp)) // Końcowy odstęp na dole
        }
    }


// Ta funkcja pomocnicza jest poza @Composable, ponieważ tworzy i pokazuje AlertDialog z Android Views.
    if (showLocationDialog) {
        LocationSelectionDialog(
            onDismissRequest = { showLocationDialog = false },
            onUseMyLocationClick = {
                showLocationDialog = false // Zamknij dialog
                if (checkLocationPermission(context)) {
                    filterViewModel.requestUserLocation()
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            },
            onUseDefaultLocationClick = {
                showLocationDialog = false // Zamknij dialog
                filterViewModel.useDefaultLocation()
            }
        )
    }
}

@Composable
fun LocationSelectionDialog(
    onDismissRequest: () -> Unit,
    onUseMyLocationClick: () -> Unit,
    onUseDefaultLocationClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Wybierz Lokalizację",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Skąd chcesz filtrować wydarzenia?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly // Rozkłada przyciski równomiernie
                ) {
                    Button(
                        onClick = onUseMyLocationClick,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "Moja lokalizacja",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth() // Dodaj to
                        )
                    }
                    Button(
                        onClick = onUseDefaultLocationClick,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "Użyj domyślnej",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth() // Dodaj to
                        )
                    }
                }
                // Możesz dodać przycisk "Anuluj", jeśli chcesz
                // TextButton(onClick = onDismissRequest) {
                //     Text("Anuluj")
                // }
            }
        }
    }
}

// Funkcja pomocnicza do sprawdzania uprawnień lokalizacyjnych
fun checkLocationPermission(context: Context): Boolean {
    return (ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED)
}