package com.example.skbt_up_gibdd_eyewitness.feature.location

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.skbt_up_gibdd_eyewitness.ui.components.AppTopBar
import kotlinx.coroutines.launch
import com.yandex.mapkit.Animation
import com.yandex.runtime.image.ImageProvider
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map as YandexMap
import com.yandex.mapkit.mapview.MapView
import java.lang.ref.WeakReference

@Composable
fun LocationPickerScreen(
    onBackClick: () -> Unit,
    onLocationSelected: (StaticLocation) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val geocodingService = remember { GeocodingService.create() }
    val markerImage = remember(context) { ImageProvider.fromBitmap(createLocationMarkerBitmap(context)) }
    var selectedLocation by remember { mutableStateOf<StaticLocation?>(null) }
    var mapController by remember { mutableStateOf<YandexMap?>(null) }
    var inputListener by remember { mutableStateOf<InputListener?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<AddressSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    val selectPoint: (StaticLocation) -> Unit = { location ->
        val point = Point(location.latitude, location.longitude)
        mapController?.let { map ->
            map.mapObjects.clear()
            map.mapObjects.addPlacemark().apply {
                geometry = point
                setIcon(markerImage)
            }
            map.move(
                CameraPosition(point, 16f, 0f, 0f),
                Animation(Animation.Type.SMOOTH, 0.5f),
                null,
            )
        }
        selectedLocation = location
        searchResults = emptyList()
    }
    val searchAddress: () -> Unit = {
        val query = searchQuery.trim()
        if (query.isNotEmpty() && !isSearching) {
            scope.launch {
                isSearching = true
                searchError = null
                geocodingService.search(query)
                    .onSuccess { results ->
                        searchResults = results
                        if (results.isEmpty()) searchError = "Адрес не найден"
                    }
                    .onFailure { searchError = "Не удалось выполнить поиск" }
                isSearching = false
            }
        }
    }
    val mapView = remember {
        MapView(context)
    }

    DisposableEffect(mapView) {
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
        onDispose {
            mapView.onStop()
            MapKitFactory.getInstance().onStop()
        }
    }

    LaunchedEffect(mapView) {
        val map = mapView.mapWindow.map
        mapController = map
        map.move(CameraPosition(KOSTROMA_CENTER, 13f, 0f, 0f))
        val listener = object : InputListener {
            override fun onMapTap(map: YandexMap, point: Point) {
                selectPoint(StaticLocation(point.latitude, point.longitude))
            }

            override fun onMapLongTap(map: YandexMap, point: Point) = Unit
        }
        inputListener = listener
        map.addInputListener(WeakReference(listener))
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AppTopBar(onBackClick)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
            )
            Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(12.dp)) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Улица или адрес в Костромской области") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { searchAddress() }),
                    trailingIcon = {
                        IconButton(onClick = searchAddress, enabled = searchQuery.isNotBlank() && !isSearching) {
                            if (isSearching) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Rounded.Search, "Найти адрес")
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
                if (searchResults.isNotEmpty()) {
                    Surface(color = Color.White, shape = RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp), shadowElevation = 4.dp) {
                        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 240.dp)) {
                            items(searchResults) { result ->
                                Text(
                                    result.displayName,
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        result.locationOrNull()?.let(selectPoint)
                                        searchQuery = result.displayName.substringBefore(',')
                                    }.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
                searchError?.let { error ->
                    Text(
                        error,
                        modifier = Modifier.padding(top = 6.dp).background(Color.White, RoundedCornerShape(8.dp)).padding(8.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            Text(
                "Яндекс Карты",
                modifier = Modifier.align(Alignment.BottomEnd).background(Color(0xCCFFFFFF)).padding(4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.DarkGray,
            )
        }
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
            Text(
                selectedLocation?.let { "${it.latitude.formatCoordinate()}, ${it.longitude.formatCoordinate()}" }
                    ?: "Точка не выбрана",
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = { selectedLocation?.let(onLocationSelected) },
                enabled = selectedLocation != null,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) { Text("Отправить точку") }
        }
    }
}

private fun Double.formatCoordinate() = String.format(java.util.Locale.US, "%.6f", this)

private fun createLocationMarkerBitmap(context: Context): Bitmap {
    val density = context.resources.displayMetrics.density
    val width = (44 * density).toInt()
    val height = (56 * density).toInt()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val centerX = width / 2f
    val radius = width * 0.36f
    val centerY = radius + width * 0.08f

    paint.color = android.graphics.Color.rgb(220, 38, 38)
    val pin = Path().apply {
        addCircle(centerX, centerY, radius, Path.Direction.CW)
        moveTo(centerX - radius * 0.62f, centerY + radius * 0.72f)
        lineTo(centerX, height.toFloat())
        lineTo(centerX + radius * 0.62f, centerY + radius * 0.72f)
        close()
    }
    canvas.drawPath(pin, paint)

    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(centerX, centerY, radius * 0.42f, paint)
    return bitmap
}

private val KOSTROMA_CENTER = Point(57.7679, 40.9269)
