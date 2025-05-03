package pl.pw.planair.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.pw.planair.data.Event
import pl.pw.planair.ui.components.EventListItem
import pl.pw.planair.ui.components.OsmMapView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenWithMapAndList(events: List<Event>) {

    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded, //Collapsed lub PartiallyExpanded
        skipHiddenState = true
    )

    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = {
            Column(modifier = Modifier.heightIn(max = 600.dp)) {
                // nagłówek
                Text(
                    text = "Lista Wydarzeń",
                    modifier = Modifier
                        .fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                val systemBarPadding = WindowInsets.systemBars.asPaddingValues()
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        end = 8.dp,
                        top = 4.dp,
                        bottom = 4.dp + systemBarPadding.calculateBottomPadding()
                    )
                ) {
                    items(events) { event ->
                        EventListItem(event = event)
                    }
                }
            }
        },
        sheetPeekHeight = 75.dp,

        ) { paddingValues ->
        OsmMapView(
            markers = events,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding())
        )
    }
}
