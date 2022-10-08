/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package com.android.gpswearos.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StatusCard(SampleData.statusSample)
        }
    }
}

data class SatelliteStatus(val index: String, val data: String)

@Composable
fun StatusCard(satStatues: List<SatelliteStatus>) {
    Card({} , modifier = Modifier
        .fillMaxWidth()
        .padding(5.dp)) {
        Box ( modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center){
            Row {
                LabelColumn(satStatues)
                ValueColumn(satStatues)
            }
        }
    }
}

@Composable
fun ValueColumn(satStatues: List<SatelliteStatus>) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .wrapContentWidth()
            .padding(top = 5.dp, bottom = 5.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        // Insert break line to leave space
        Text("\n")
        for (s in satStatues) {
            Text(
                text = s.data,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
        }
        Text("\n")
    }
}

@Composable
fun LabelColumn(satStatues: List<SatelliteStatus>) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .wrapContentWidth()
            .padding(top = 5.dp, bottom = 5.dp, start = 5.dp, end = 5.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.End
    ) {
        Text("\n")
        for (s in satStatues) {
            Text(
                text = s.index,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
        }
        Text("\n")
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    StatusCard(SampleData.statusSample)
}