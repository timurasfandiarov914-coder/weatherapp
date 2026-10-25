package com.solen.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(10, 18, 31)
        window.navigationBarColor = android.graphics.Color.rgb(10, 18, 31)
        window.decorView.systemUiVisibility = 0
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFFB9D8FF),
                secondary = Color(0xFFA5C8F0),
                background = Color(0xFF0A121F),
                surface = Color(0xFF142136)
            )) {
                SolenScreen()
            }
        }
    }
}

private data class Place(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String = ""
)

private data class HourForecast(
    val time: String,
    val temperature: Int,
    val code: Int,
    val precipitation: Int,
    val precipitationMm: Double,
    val isDay: Boolean
)

private data class DayForecast(
    val date: String,
    val max: Int,
    val min: Int,
    val code: Int
)

private data class WeatherForecast(
    val temperature: Int,
    val feelsLike: Int,
    val humidity: Int,
    val wind: Int,
    val precipitation: Double,
    val code: Int,
    val isDay: Boolean,
    val sunrise: String,
    val sunset: String,
    val hours: List<HourForecast>,
    val days: List<DayForecast>
)

private val ink = Color(0xFF0A121F)
private val muted = Color(0xFF9CAEC5)
private val pale = Color(0xFFF3F7FC)
private val glass = Color.White.copy(alpha = 0.075f)
private val glassBorder = Color.White.copy(alpha = 0.10f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SolenScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var place by remember { mutableStateOf(Place("Уфа", 54.7388, 55.9721, "Россия")) }
    var forecast by remember { mutableStateOf<WeatherForecast?>(null) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchOpen by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var cityResults by remember { mutableStateOf<List<Place>>(emptyList()) }

    LaunchedEffect(place) {
        loading = true
        error = null
        try {
            forecast = loadForecast(place)
        } catch (e: Exception) {
            error = "Не удалось обновить погоду. Проверь интернет и попробуй ещё раз."
        }
        loading = false
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            requestDeviceLocation(context) { lat, lon ->
                place = Place("Моё местоположение", lat, lon)
                searchOpen = false
            }
        }
    }

    val current = forecast
    val backgroundTop = when {
        current == null -> Color(0xFF0A121F)
        current.code in 51..99 -> Color(0xFF101D30)
        !current.isDay -> Color(0xFF080F1D)
        current.code in 0..2 -> Color(0xFF142D4B)
        else -> Color(0xFF17263B)
    }
    val backgroundBottom = when {
        current != null && current.code in 51..99 -> Color(0xFF253A51)
        current != null && current.isDay -> Color(0xFF315579)
        else -> Color(0xFF1B2E46)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(backgroundTop, backgroundBottom)))
    ) {
        AnimatedSky()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(top = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SOLEN",
                        color = pale,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = muted,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = if (place.country.isBlank()) place.name else "${place.name}, ${place.country}",
                            color = muted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                RoundIconButton(
                    icon = "search",
                    description = "Найти город",
                    onClick = {
                        searchText = ""
                        searchOpen = true
                    }
                )
            }

            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = {
                    scope.launch {
                        refreshing = true
                        error = null
                        try {
                            forecast = loadForecast(place)
                        } catch (_: Exception) {
                            error = "Не удалось обновить погоду. Проверь интернет."
                        }
                        refreshing = false
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 16.dp)
            ) {
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "ПОГОДА СЕЙЧАС",
                    color = muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.5.sp
                )
                Spacer(Modifier.height(5.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = current?.let { "${it.temperature}°" } ?: "—°",
                            color = pale,
                            fontSize = 92.sp,
                            lineHeight = 98.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-5).sp
                        )
                        Text(
                            text = current?.let { weatherDescription(it.code) } ?: if (loading) "Загружаем небо…" else "Нет данных",
                            color = pale,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = current?.let { "Ощущается как ${it.feelsLike}°" } ?: "Точный прогноз по вашему городу",
                            color = muted,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = current?.let { "Ветер ${it.wind} км/ч  ·  Влажность ${it.humidity}%" } ?: "Интернет нужен для актуальных данных",
                            color = muted,
                            fontSize = 12.sp
                        )
                    }
                    WeatherGlyph(
                        code = current?.code ?: 0,
                        isDay = current?.isDay ?: true,
                        modifier = Modifier.padding(start = 2.dp, end = 2.dp).size(52.dp),
                        size = 44
                    )
                }

                if (loading) {
                    Row(
                        modifier = Modifier.padding(top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(15.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFB9D8FF)
                        )
                        Spacer(Modifier.width(9.dp))
                        Text("Обновляем данные", color = muted, fontSize = 12.sp)
                    }
                }
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(text = error.orEmpty(), color = Color(0xFFFFC2B8), fontSize = 12.sp)
                }

                Spacer(Modifier.height(24.dp))
                SectionTitle("ПО БЛИЖАЙШИМ ЧАСАМ", "Следующие 24 часа")
                Spacer(Modifier.height(12.dp))
                GlassPanel {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 6.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        val hours = current?.hours.orEmpty()
                        if (hours.isEmpty()) {
                            repeat(6) { PlaceholderHour() }
                        } else {
                            hours.take(12).forEachIndexed { index, hour ->
                                val displayedHour = if (index == 0 && current != null) {
                                    hour.copy(
                                        temperature = current.temperature,
                                        code = current.code,
                                        isDay = current.isDay
                                    )
                                } else hour
                                HourChip(displayedHour, index == 0)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(23.dp))
                SectionTitle("ОСАДКИ", "Планируй день спокойно")
                Spacer(Modifier.height(11.dp))
                val forecastHours = current?.hours.orEmpty()
                val hasExpectedPrecipitation = forecastHours.any { it.precipitationMm >= 0.1 || it.precipitation >= 20 }
                GlassPanel {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (current == null) "Ждём прогноз" else if (hasExpectedPrecipitation) "Возможны осадки" else "Осадков не ожидается",
                                    color = pale,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (current == null) "Обновим после загрузки" else if (hasExpectedPrecipitation) "Вероятность и интенсивность на ближайшие часы" else "В ближайшие 24 часа дождь маловероятен",
                                    color = muted,
                                    fontSize = 12.sp
                                )
                            }
                            androidx.compose.material3.Icon(
                                imageVector = if (hasExpectedPrecipitation) Icons.Outlined.WaterDrop else Icons.Outlined.CheckCircleOutline,
                                contentDescription = null,
                                tint = Color(0xFFB9D8FF),
                                modifier = Modifier.size(25.dp)
                            )
                        }
                        if (current != null && hasExpectedPrecipitation) {
                            Spacer(Modifier.height(17.dp))
                            PrecipitationLine(forecastHours)
                        }
                    }
                }

                Spacer(Modifier.height(23.dp))
                SectionTitle("ДЕТАЛИ ПОГОДЫ", "Всё, что влияет на ощущения")
                Spacer(Modifier.height(11.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WeatherDetail(
                        modifier = Modifier.weight(1f),
                        icon = WeatherDetailIcon.HUMIDITY,
                        title = "Влажность",
                        value = current?.let { "${it.humidity}%" } ?: "—"
                    )
                    WeatherDetail(
                        modifier = Modifier.weight(1f),
                        icon = WeatherDetailIcon.WIND,
                        title = "Ветер",
                        value = current?.let { "${it.wind} км/ч" } ?: "—"
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WeatherDetail(
                        modifier = Modifier.weight(1f),
                        icon = WeatherDetailIcon.SUNRISE,
                        title = "Рассвет",
                        value = current?.sunrise ?: "—"
                    )
                    WeatherDetail(
                        modifier = Modifier.weight(1f),
                        icon = WeatherDetailIcon.SUNSET,
                        title = "Закат",
                        value = current?.sunset ?: "—"
                    )
                }

                Spacer(Modifier.height(23.dp))
                SectionTitle("НА 7 ДНЕЙ", "Неделя впереди")
                Spacer(Modifier.height(11.dp))
                GlassPanel {
                    Column(modifier = Modifier.padding(horizontal = 15.dp, vertical = 6.dp)) {
                        val days = current?.days.orEmpty()
                        if (days.isEmpty()) {
                            repeat(4) { PlaceholderDay() }
                        } else {
                            val weekMin = days.minOf { it.min }
                            val weekMax = days.maxOf { it.max }
                            days.forEachIndexed { index, day ->
                                DayRow(day, index, weekMin, weekMax)
                                if (index < days.lastIndex) {
                                    Spacer(Modifier.height(1.dp))
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(Color.White.copy(alpha = 0.06f))
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "ПРОГНОЗ • OPEN-METEO",
                    color = Color.White.copy(alpha = 0.42f),
                    fontSize = 9.sp,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(8.dp))
            }
            }

            BottomBar(onCitiesClick = { searchText = ""; searchOpen = true })
        }
    }

    LaunchedEffect(searchText, searchOpen) {
        if (!searchOpen) return@LaunchedEffect
        if (searchText.trim().length < 2) {
            cityResults = listOf(
                Place("Москва", 55.7558, 37.6173, "Россия"),
                Place("Санкт-Петербург", 59.9343, 30.3351, "Россия"),
                Place("Уфа", 54.7388, 55.9721, "Россия"),
                Place("Казань", 55.7961, 49.1064, "Россия")
            )
            searching = false
        } else {
            delay(350)
            searching = true
            cityResults = try { searchCities(searchText.trim()) } catch (_: Exception) { emptyList() }
            searching = false
        }
    }

    if (searchOpen) {
        val focusRequester = remember { FocusRequester() }
        val keyboard = LocalSoftwareKeyboardController.current
        LaunchedEffect(Unit) {
            delay(120)
            focusRequester.requestFocus()
            keyboard?.show()
        }
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { searchOpen = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
                    .background(Color(0xFF0A121F))
                    .padding(WindowInsets.statusBars.asPaddingValues())
                    .padding(horizontal = 22.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Найти город",
                        color = pale,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    androidx.compose.material3.IconButton(onClick = { searchOpen = false }) {
                        androidx.compose.material3.Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Закрыть поиск",
                            tint = muted
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("ПОГОДА В ЛЮБОЙ ТОЧКЕ МИРА", color = Color(0xFFC3D2E5), fontSize = 11.sp, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(18.dp))
                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    shape = RoundedCornerShape(17.dp),
                    singleLine = true,
                    leadingIcon = { androidx.compose.material3.Icon(Icons.Outlined.Search, contentDescription = null, tint = Color(0xFFD7E6F7)) },
                    placeholder = { Text("Например, Москва", color = Color(0xFFC3D2E5)) },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = pale,
                        unfocusedTextColor = pale,
                        focusedContainerColor = Color(0xFF1B2C43),
                        unfocusedContainerColor = Color(0xFF1B2C43),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        cursorColor = Color(0xFFB9D8FF)
                    )
                )
                Spacer(Modifier.height(18.dp))
                androidx.compose.material3.TextButton(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            requestDeviceLocation(context) { lat, lon -> place = Place("Моё местоположение", lat, lon); searchOpen = false }
                        } else {
                            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Color(0xFFB9D8FF))
                    Spacer(Modifier.width(8.dp))
                    Text("Определить моё местоположение", color = Color(0xFFD7E6F7))
                }
                Spacer(Modifier.height(10.dp))
                Text(if (searchText.trim().length < 2) "ПОПУЛЯРНЫЕ ГОРОДА" else "РЕЗУЛЬТАТЫ ПОИСКА", color = Color(0xFFC3D2E5), fontSize = 11.sp, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(8.dp))
                if (searching) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 16.dp)) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = Color(0xFFB9D8FF), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Ищем город…", color = muted, fontSize = 13.sp)
                    }
                } else if (cityResults.isEmpty()) {
                    Text(if (searchText.length >= 2) "Ничего не найдено. Проверь написание." else "Начни вводить название города.", color = Color(0xFFC3D2E5), fontSize = 13.sp, modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        cityResults.forEach { city ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                    .clickable { place = city; searchOpen = false }
                                    .padding(horizontal = 16.dp, vertical = 15.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(city.name, color = pale, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                    if (searchText.trim().length >= 2 && city.country.isNotBlank()) {
                                        Spacer(Modifier.height(3.dp))
                                        Text(city.country, color = Color(0xFFC3D2E5), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedSky() {
    val transition = rememberInfiniteTransition(label = "sky-drift")
    val drift by transition.animateFloat(
        initialValue = -45f,
        targetValue = 55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cloud-offset"
    )
    Canvas(modifier = Modifier.fillMaxWidth().height(390.dp)) {
        val unit = size.width / 390f
        val alpha = 0.10f
        drawCircle(
            color = Color(0xFF90BDEB).copy(alpha = alpha),
            radius = 120f * unit,
            center = Offset(size.width * 0.76f + drift * unit, size.height * 0.13f)
        )
        drawCircle(
            color = Color(0xFFB8D2F1).copy(alpha = alpha * 0.52f),
            radius = 80f * unit,
            center = Offset(size.width * 0.91f + drift * unit, size.height * 0.20f)
        )
        drawCircle(
            color = Color(0xFF678CB9).copy(alpha = alpha * 0.50f),
            radius = 100f * unit,
            center = Offset(size.width * 0.18f - drift * unit * 0.5f, size.height * 0.38f)
        )
    }
}

@Composable
private fun RoundIconButton(
    icon: String,
    description: String,
    onClick: () -> Unit
) {
    androidx.compose.material3.IconButton(onClick = onClick, modifier = Modifier.size(42.dp)) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.07f))
                .border(1.dp, glassBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = description,
                tint = pale,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color(0xFFC3D2E5),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(text = subtitle, color = pale, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun GlassPanel(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(23.dp))
            .background(glass)
            .border(1.dp, glassBorder, RoundedCornerShape(23.dp))
    ) {
        content()
    }
}

@Composable
private fun HourChip(hour: HourForecast, highlight: Boolean) {
    Column(
        modifier = Modifier
            .width(57.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(if (highlight) Color.White.copy(alpha = 0.11f) else Color.Transparent)
            .padding(horizontal = 3.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            if (highlight) "СЕЙЧАС" else hour.time,
            color = if (highlight) pale else muted,
            fontSize = if (highlight) 8.sp else 10.sp,
            fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal
        )
        WeatherGlyph(hour.code, hour.isDay, Modifier.size(25.dp), size = 22)
        Text("${hour.temperature}°", color = pale, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(
            if (hour.precipitation > 0) "${hour.precipitation}%" else " ",
            color = Color(0xFFB9D8FF),
            fontSize = 9.sp
        )
    }
}

@Composable
private fun PlaceholderHour() {
    Column(
        modifier = Modifier.width(57.dp).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("—", color = muted, fontSize = 10.sp)
        Text("◌", color = muted, fontSize = 22.sp)
        Text("—°", color = pale, fontSize = 15.sp)
    }
}

@Composable
private fun PrecipitationLine(hours: List<HourForecast>) {
    Canvas(modifier = Modifier.fillMaxWidth().height(44.dp)) {
        val points = hours.take(12)
        if (points.size >= 2) {
            val maxValue = (points.maxOf { it.precipitation }).coerceAtLeast(10).toFloat()
            val step = size.width / (points.size - 1)
            val coords = points.mapIndexed { index, point ->
                Offset(step * index, size.height - (point.precipitation / maxValue) * (size.height - 6f) - 3f)
            }
            for (i in 0 until coords.lastIndex) {
                drawLine(
                    color = Color(0xFF8BCBFF).copy(alpha = 0.85f),
                    start = coords[i],
                    end = coords[i + 1],
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            coords.forEach {
                drawCircle(Color(0xFFB9D8FF), radius = 3.dp.toPx(), center = it)
            }
        } else {
            drawLine(
                color = Color.White.copy(alpha = 0.22f),
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(hours.getOrNull(0)?.time ?: "СЕЙЧАС", color = muted, fontSize = 10.sp)
        Text(hours.getOrNull(5)?.time ?: "ПОЗЖЕ", color = muted, fontSize = 10.sp)
        Text(hours.getOrNull(10)?.time ?: "ДАЛЬШЕ", color = muted, fontSize = 10.sp)
    }
}

private enum class WeatherDetailIcon { HUMIDITY, WIND, SUNRISE, SUNSET }

@Composable
private fun WeatherDetail(
    modifier: Modifier,
    icon: WeatherDetailIcon,
    title: String,
    value: String
) {
    val image = when (icon) {
        WeatherDetailIcon.HUMIDITY -> Icons.Outlined.WaterDrop
        WeatherDetailIcon.WIND -> Icons.Outlined.Air
        WeatherDetailIcon.SUNRISE -> Icons.Outlined.WbTwilight
        WeatherDetailIcon.SUNSET -> Icons.Outlined.WbTwilight
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(21.dp))
            .background(glass)
            .border(1.dp, glassBorder, RoundedCornerShape(21.dp))
            .padding(15.dp)
    ) {
        Box(modifier = Modifier.size(22.dp)) {
            androidx.compose.material3.Icon(
                imageVector = image,
                contentDescription = title,
                tint = Color(0xFFD7E6F7),
                modifier = Modifier.align(Alignment.Center).size(20.dp)
            )
            if (icon == WeatherDetailIcon.SUNRISE || icon == WeatherDetailIcon.SUNSET) {
                androidx.compose.material3.Icon(
                    imageVector = if (icon == WeatherDetailIcon.SUNRISE) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                    contentDescription = null,
                    tint = Color(0xFFB9D8FF),
                    modifier = Modifier.align(Alignment.TopEnd).size(11.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(title, color = muted, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = pale, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DayRow(day: DayForecast, index: Int, weekMin: Int, weekMax: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (index == 0) "Сегодня" else dayName(day.date),
            color = pale,
            fontSize = 13.sp,
            modifier = Modifier.weight(1.2f)
        )
        WeatherGlyph(day.code, true, Modifier.width(43.dp).size(22.dp), size = 19)
        Text(
            text = "${day.min}°",
            color = muted,
            fontSize = 13.sp,
            modifier = Modifier.width(34.dp)
        )
        TemperatureRange(day.min, day.max, weekMin, weekMax, Modifier.weight(1f))
        Text(
            text = "${day.max}°",
            color = pale,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(35.dp)
        )
    }
}

@Composable
private fun TemperatureRange(min: Int, max: Int, weekMin: Int, weekMax: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.height(6.dp)) {
        val width = size.width
        drawLine(
            color = Color.White.copy(alpha = 0.16f),
            start = Offset(0f, size.height / 2),
            end = Offset(width, size.height / 2),
            strokeWidth = 5.dp.toPx(),
            cap = StrokeCap.Round
        )
        val spread = (weekMax - weekMin).coerceAtLeast(1)
        val start = ((min - weekMin).toFloat() / spread).coerceIn(0f, 1f)
        val end = ((max - weekMin).toFloat() / spread).coerceIn(start, 1f)
        drawLine(
            brush = Brush.horizontalGradient(listOf(Color(0xFF79BDF7), Color(0xFFE9D5AD))),
            start = Offset(width * start, size.height / 2),
            end = Offset(width * end, size.height / 2),
            strokeWidth = 5.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun PlaceholderDay() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Загружаем", color = muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text("◌", color = muted, fontSize = 18.sp)
        Spacer(Modifier.width(30.dp))
        Text("—°   —°", color = pale, fontSize = 12.sp)
    }
}

@Composable
private fun BottomBar(onCitiesClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A121F).copy(alpha = 0.78f))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.07f))
            .padding(
                start = 28.dp,
                end = 28.dp,
                top = 12.dp,
                bottom = 10.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomItem(Icons.Outlined.WbSunny, "Сегодня", true, {})
        Spacer(Modifier.width(68.dp))
        BottomItem(Icons.Outlined.Public, "Города", false, onCitiesClick)
    }
}

@Composable
private fun BottomItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 2.dp)
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Color(0xFFB9D8FF) else Color(0xFFC3D2E5),
            modifier = Modifier.size(21.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            color = if (selected) pale else muted.copy(alpha = 0.72f),
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
        if (selected) {
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .width(14.dp)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFB9D8FF))
            )
        }
    }
}

private suspend fun loadForecast(place: Place): WeatherForecast = withContext(Dispatchers.IO) {
    val url = URL(
        "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${place.latitude}&longitude=${place.longitude}" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,wind_speed_10m" +
            "&hourly=temperature_2m,precipitation_probability,precipitation,weather_code,is_day" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset" +
            "&timezone=auto&forecast_days=7"
    )
    val connection = url.openConnection() as HttpURLConnection
    connection.connectTimeout = 12000
    connection.readTimeout = 12000
    connection.setRequestProperty("User-Agent", "SOLEN-Android/0.1")
    try {
        if (connection.responseCode !in 200..299) error("Weather server returned ${connection.responseCode}")
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        val root = JSONObject(body)
        val now = root.getJSONObject("current")
        val hourly = root.getJSONObject("hourly")
        val daily = root.getJSONObject("daily")
        val times = hourly.getJSONArray("time")
        val temps = hourly.getJSONArray("temperature_2m")
        val probabilities = hourly.getJSONArray("precipitation_probability")
        val hourlyPrecipitation = hourly.getJSONArray("precipitation")
        val hourlyIsDay = hourly.getJSONArray("is_day")
        val codes = hourly.getJSONArray("weather_code")
        val currentTime = now.getString("time")
        var start = 0
        for (i in 0 until times.length()) {
            if (times.getString(i) >= currentTime.substring(0, 13) + ":00") {
                start = i
                break
            }
        }
        val hours = (start until minOf(start + 24, times.length())).map { i ->
            HourForecast(
                time = times.getString(i).substringAfter("T").take(5),
                temperature = temps.getDouble(i).roundToInt(),
                code = codes.getInt(i),
                precipitation = probabilities.getInt(i),
                precipitationMm = hourlyPrecipitation.getDouble(i),
                isDay = hourlyIsDay.getInt(i) == 1
            )
        }
        val dailyTimes = daily.getJSONArray("time")
        val maxes = daily.getJSONArray("temperature_2m_max")
        val mins = daily.getJSONArray("temperature_2m_min")
        val dailyCodes = daily.getJSONArray("weather_code")
        val days = (0 until dailyTimes.length()).map { i ->
            DayForecast(
                date = dailyTimes.getString(i),
                max = maxes.getDouble(i).roundToInt(),
                min = mins.getDouble(i).roundToInt(),
                code = dailyCodes.getInt(i)
            )
        }
        val sunrise = daily.getJSONArray("sunrise").optString(0, "")
        val sunset = daily.getJSONArray("sunset").optString(0, "")
        WeatherForecast(
            temperature = now.getDouble("temperature_2m").roundToInt(),
            feelsLike = now.getDouble("apparent_temperature").roundToInt(),
            humidity = now.getInt("relative_humidity_2m"),
            wind = now.getDouble("wind_speed_10m").roundToInt(),
            precipitation = now.getDouble("precipitation"),
            code = now.getInt("weather_code"),
            isDay = now.getInt("is_day") == 1,
            sunrise = sunrise.substringAfter("T", "—").take(5),
            sunset = sunset.substringAfter("T", "—").take(5),
            hours = hours,
            days = days
        )
    } finally {
        connection.disconnect()
    }
}

private suspend fun searchCities(query: String): List<Place> = withContext(Dispatchers.IO) {
    val safeQuery = URLEncoder.encode(query, "UTF-8")
    val url = URL("https://geocoding-api.open-meteo.com/v1/search?name=$safeQuery&count=6&language=ru&format=json")
    val connection = url.openConnection() as HttpURLConnection
    connection.connectTimeout = 12000
    connection.readTimeout = 12000
    try {
        if (connection.responseCode !in 200..299) return@withContext emptyList()
        val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        val results = root.optJSONArray("results") ?: return@withContext emptyList()
        (0 until results.length()).map { index ->
            val found = results.getJSONObject(index)
            Place(
                name = found.optString("name", query),
                latitude = found.getDouble("latitude"),
                longitude = found.getDouble("longitude"),
                country = listOf(found.optString("admin1", ""), found.optString("country", ""))
                    .filter { it.isNotBlank() }.distinct().joinToString(", ")
            )
        }
    } finally {
        connection.disconnect()
    }
}

@SuppressLint("MissingPermission")
private fun requestDeviceLocation(context: Context, onResult: (Double, Double) -> Unit) {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val lastKnown = try {
        manager.getProviders(true).mapNotNull { provider ->
            try {
                manager.getLastKnownLocation(provider)
            } catch (_: Exception) {
                null
            }
        }.maxByOrNull { it.time }
    } catch (_: Exception) {
        null
    }
    if (lastKnown != null) {
        onResult(lastKnown.latitude, lastKnown.longitude)
        return
    }
    val provider = try {
        when {
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> null
        }
    } catch (_: Exception) {
        null
    } ?: return
    try {
        manager.requestSingleUpdate(
            provider,
            object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    onResult(location.latitude, location.longitude)
                }
                @Deprecated("Deprecated in Android API")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) = Unit
            },
            Looper.getMainLooper()
        )
    } catch (_: Exception) {
        // Location can be unavailable while the device is offline or location services are disabled.
    }
}

private fun weatherDescription(code: Int): String = when (code) {
    0 -> "Ясное небо"
    1 -> "Преимущественно ясно"
    2 -> "Переменная облачность"
    3 -> "Пасмурно"
    45, 48 -> "Туман"
    51, 53, 55 -> "Морось"
    56, 57 -> "Ледяная морось"
    61, 63, 65 -> "Дождь"
    66, 67 -> "Ледяной дождь"
    71, 73, 75, 77 -> "Снег"
    80, 81, 82 -> "Ливень"
    85, 86 -> "Снегопад"
    95 -> "Гроза"
    96, 99 -> "Гроза с градом"
    else -> "Переменная погода"
}

@Composable
private fun WeatherGlyph(code: Int, isDay: Boolean, modifier: Modifier = Modifier, size: Int = 22) {
    val image = when (code) {
        0 -> if (isDay) Icons.Outlined.WbSunny else Icons.Outlined.NightsStay
        1 -> if (isDay) Icons.Outlined.WbSunny else Icons.Outlined.NightsStay
        2, 3, 45, 48 -> Icons.Outlined.Cloud
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> Icons.Outlined.WaterDrop
        71, 73, 75, 77, 85, 86 -> Icons.Outlined.AcUnit
        95, 96, 99 -> Icons.Outlined.Thunderstorm
        else -> if (isDay) Icons.Outlined.WbSunny else Icons.Outlined.NightsStay
    }
    androidx.compose.material3.Icon(
        imageVector = image,
        contentDescription = weatherDescription(code),
        tint = Color(0xFFD7E6F7),
        modifier = modifier.size(size.dp)
    )
}

private fun dayName(date: String): String {
    return try {
        val localDate = LocalDate.parse(date)
        localDate.format(DateTimeFormatter.ofPattern("EEE", Locale("ru")))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("ru")) else it.toString() }
    } catch (_: Exception) {
        date
    }
}
