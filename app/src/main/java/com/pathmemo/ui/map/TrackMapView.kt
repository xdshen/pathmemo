package com.pathmemo.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.TextureMapView
import com.amap.api.maps.model.BitmapDescriptor
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.pathmemo.data.model.LocationPoint
import com.pathmemo.data.model.MapType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * AMap wrapper dedicated to rendering tracks as clickable point markers.
 * Uses TextureMapView for better compatibility inside Compose layouts.
 * Redrawing happens in AndroidView's update block so the MapView is guaranteed
 * to be attached and laid out when overlays are added.
 */
@Composable
fun TrackMapView(
    tracks: Map<Long, List<LocationPoint>>,
    selectedPoint: LocationPoint? = null,
    onPointClick: (LocationPoint) -> Unit = {},
    modifier: Modifier = Modifier,
    mapType: MapType = MapType.NORMAL
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { TextureMapView(context) }
    val dotCache = remember { mutableMapOf<Int, BitmapDescriptor>() }
    val highlightDotCache = remember { mutableMapOf<Int, BitmapDescriptor>() }
    val onPointClickState = rememberUpdatedState(onPointClick)

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
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            mapView.onResume()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(mapView) {
        val clickListener = com.amap.api.maps.AMap.OnMarkerClickListener { marker ->
            val point = marker.getObject() as? LocationPoint
            point?.let { onPointClickState.value(it) }
            true
        }
        val infoWindowAdapter = object : com.amap.api.maps.AMap.InfoWindowAdapter {
            override fun getInfoWindow(marker: Marker): View? {
                val point = marker.getObject() as? LocationPoint ?: return null
                return createInfoWindowView(context, point)
            }

            override fun getInfoContents(marker: Marker): View? = null
        }
        mapView.map.setOnMarkerClickListener(clickListener)
        mapView.map.setInfoWindowAdapter(infoWindowAdapter)
        onDispose {
            mapView.map.setOnMarkerClickListener(null)
            mapView.map.setInfoWindowAdapter(null)
        }
    }

    val allPoints = tracks.values.flatten()

    LaunchedEffect(allPoints) {
        if (allPoints.isEmpty()) return@LaunchedEffect
        mapView.post {
            val bounds = buildBounds(allPoints)
            val southwest = bounds.southwest
            val northeast = bounds.northeast
            val isDegenerate = southwest.latitude == northeast.latitude &&
                    southwest.longitude == northeast.longitude
            when {
                allPoints.size == 1 -> {
                    val p = allPoints.first()
                    mapView.map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(LatLng(p.latitude, p.longitude), 18f)
                    )
                }
                isDegenerate -> {
                    mapView.map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(southwest, 18f)
                    )
                }
                else -> {
                    mapView.map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
                }
            }
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                onCreate(null)
                map.apply {
                    uiSettings.isZoomControlsEnabled = true
                    uiSettings.isMyLocationButtonEnabled = false
                    isMyLocationEnabled = false
                    this.mapType = mapType.value
                }
            }
        },
        update = { mv ->
            if (mv.map.mapType != mapType.value) {
                mv.map.mapType = mapType.value
            }
            Log.i("TrackMapView", "update called, tracks=${tracks.size}, totalPoints=${allPoints.size}")
            mv.map.clear()
            if (allPoints.isEmpty()) {
                Log.i("TrackMapView", "no points to draw")
                return@AndroidView
            }

            var selectedMarker: Marker? = null
            tracks.values.forEach { points ->
                points.forEach { p ->
                    val isSelected = selectedPoint == p
                    val color = getPointColor(p.timestamp)
                    val icon = if (isSelected) {
                        highlightDotCache.getOrPut(color) {
                            createDotBitmap(context, color, 18f)
                        }
                    } else {
                        dotCache.getOrPut(color) {
                            createDotBitmap(context, color, 10f)
                        }
                    }
                    val marker = mv.map.addMarker(
                        MarkerOptions()
                            .position(LatLng(p.latitude, p.longitude))
                            .icon(icon)
                            .anchor(0.5f, 0.5f)
                    )
                    marker?.setObject(p)
                    if (isSelected) {
                        selectedMarker = marker
                    }
                }
            }
            selectedMarker?.showInfoWindow()
            Log.i("TrackMapView", "drawn ${allPoints.size} point markers")
        },
        modifier = modifier
    )
}

private fun createDotBitmap(context: Context, color: Int, sizeDp: Float): BitmapDescriptor {
    val size = (context.resources.displayMetrics.density * sizeDp).toInt().coerceAtLeast(16)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        this.color = color
        isAntiAlias = true
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

private fun createInfoWindowView(context: Context, point: LocationPoint): View {
    val container = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(0xFFFFFFFF.toInt())
        setPadding(24, 16, 24, 16)
    }
    val timeText = TextView(context).apply {
        text = formatDateTime(point.timestamp)
        textSize = 14f
        setTextColor(0xFF000000.toInt())
    }
    val coordText = TextView(context).apply {
        text = "${"%.6f".format(point.latitude)}, ${"%.6f".format(point.longitude)}"
        textSize = 12f
        setTextColor(0xFF666666.toInt())
    }
    container.addView(timeText)
    container.addView(coordText)
    return container
}

private fun buildBounds(points: List<LocationPoint>): LatLngBounds {
    val builder = LatLngBounds.builder()
    points.forEach { builder.include(LatLng(it.latitude, it.longitude)) }
    return builder.build()
}

private fun formatDateTime(timestamp: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        .format(Date(timestamp))
}
