package com.android.gpstest.wear

import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.android.gpstest.library.LocationLabelAndData
import com.android.gpstest.wear.theme.GpstestTheme
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp(LocationLabelAndData.locationLabelAndDataSample)
        }
    }
}

@Composable
fun WearApp(satStatues: List<String>) {
    GpstestTheme {
        val listState = rememberScalingLazyListState()
        Scaffold(
            timeText = {
                if (!listState.isScrollInProgress) {
                    TimeText(
                        timeSource = TimeTextDefaults.timeSource(
                            DateFormat.getBestDateTimePattern(Locale.getDefault(), "hh:mm:ss")
                        )
                    )
                }
            },
            positionIndicator = {
                PositionIndicator(
                    scalingLazyListState = listState
                )
            }
        ) {
            val contentModifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
            ScalingLazyColumn(
                modifier = contentModifier,
                autoCentering = AutoCenteringParams(itemIndex = 0),
                state = listState
            ) {

                for (satStatue in satStatues) {
                    item {
                        Text(text = satStatue)
                    }
                }
            }
        }
    }
}