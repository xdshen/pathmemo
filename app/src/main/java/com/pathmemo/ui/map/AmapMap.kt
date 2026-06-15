package com.pathmemo.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.amap.api.maps.MapView
import com.amap.api.maps.model.MyLocationStyle
import com.pathmemo.data.model.MapType

@Composable
fun AMapView(
    modifier: Modifier = Modifier,
    mapType: MapType = MapType.NORMAL,
    onMapReady: (MapView) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        // When this composable enters an already-resumed screen (e.g. navigation),
        // LifecycleEventObserver will not receive the prior ON_RESUME event, so
        // we must call it explicitly to make the map render correctly.
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            mapView.onResume()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                onCreate(null)
                map.apply {
                    myLocationStyle = MyLocationStyle().apply {
                        myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER)
                        showMyLocation(true)
                    }
                    isMyLocationEnabled = true
                    uiSettings.isMyLocationButtonEnabled = true
                    uiSettings.isZoomControlsEnabled = true
                    this.mapType = mapType.value
                    onMapReady(mapView)
                }
            }
        },
        modifier = modifier,
        update = { mv ->
            if (mv.map.mapType != mapType.value) {
                mv.map.mapType = mapType.value
            }
        }
    )
}
