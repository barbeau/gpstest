package com.android.gpstest.ui.status

import android.location.Location
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import com.android.gpstest.model.DilutionOfPrecision
import com.android.gpstest.model.SatelliteMetadata
import com.android.gpstest.ui.SignalInfoViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@Composable
fun StatusScreen(viewModel: SignalInfoViewModel) {
    //
    // Observe LiveData from ViewModel
    //
    val location: Location by viewModel.location.observeAsState(Location("default"))
    val ttff: String by viewModel.ttff.observeAsState("")
    val altitudeMsl: Double by viewModel.altitudeMsl.observeAsState(Double.NaN)
    val dop: DilutionOfPrecision by viewModel.dop.observeAsState(DilutionOfPrecision(Double.NaN,Double.NaN,Double.NaN))
    val satelliteMetadata: SatelliteMetadata by viewModel.satelliteMetadata.observeAsState(SatelliteMetadata(0,0,0,0,0,0))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column {
            LocationCard(
                location,
                ttff,
                altitudeMsl,
                dop,
                satelliteMetadata)
//            Filter() // TODO - annotated text - https://foso.github.io/Jetpack-Compose-Playground/material/card/
//            GnssStatusCard()
//            SbasStatusCard()
        }
    }
}