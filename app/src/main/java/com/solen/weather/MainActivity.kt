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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
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
    val precipitation: Int
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

@Composable
private fun SolenScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var place by remember { mutableStateOf(Place("Уфа", 54.7388, 55.9721, "Россия")) }
    var forecast by remember { mutableStateOf<WeatherForecast?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var searchOpen by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }

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
                Spacer(Modifier.width(9.dp))
                RoundIconButton(
                    icon = "location",
                    description = "Моё местоположение",
                    onClick = {
                        if (
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            requestDeviceLocation(context) { lat, lon ->
                                place = Place("Моё местоположение", lat, lon)
                            }
                        } else {
                            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }
                )
                Spacer(Modifier.width(9.dp))
                RoundIconButton(
                    icon = "refresh",
                    description = "Обновить погоду",
                    onClick = {
                        scope.launch {
                            loading = true
                            error = null
                            try {
                                forecast = loadForecast(place)
                            } catch (e: Exception) {
                                error = "Не удалось обновить погоду. Проверь интернет."
                            }
                            loading = false
                        }
                    }
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
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
                            fontWeight = FontWeight.Light,
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
                    Text(
                        text = current?.let { weatherEmoji(it.code, it.isDay) } ?: "◌",
                        fontSize = 78.sp,
                        modifier = Modifier.padding(start = 2.dp, end = 2.dp)
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
                                HourChip(hour, index == 0)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(23.dp))
                SectionTitle("ОСАДКИ", "Планируй день спокойно")
                Spacer(Modifier.height(11.dp))
                GlassPanel {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = current?.let {
                                        if (it.precipitation < 0.1) "Дождя почти нет" else "Возможны осадки"
                                    } ?: "Ждём прогноз",
                                    color = pale,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = current?.let { "Сейчас: ${it.precipitation} мм" } ?: "Обновим после загрузки",
                                    color = muted,
                                    fontSize = 12.sp
                                )
                            }
                            Text("↗", color = Color(0xFFB9D8FF), fontSize = 26.sp)
                        }
                        Spacer(Modifier.height(17.dp))
                        PrecipitationLine(current?.hours.orEmpty())
                    }
                }

                Spacer(Modifier.height(23.dp))
                SectionTitle("ДЕТАЛИ ПОГОДЫ", "Всё, что влияет на ощущения")
                Spacer(Modifier.height(11.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WeatherDetail(
                        modifier = Modifier.weight(1f),
                        icon = "💧",
                        title = "Влажность",
                        value = current?.let { "${it.humidity}%" } ?: "—"
                    )
                    WeatherDetail(
                        modifier = Modifier.weight(1f),
                        icon = "〰",
                        title = "Ветер",
                        value = current?.let { "${it.wind} км/ч" } ?: "—"
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WeatherDetail(
                        modifier = Modifier.weight(1f),
                        icon = "🌅",
                        title = "Рассвет",
                        value = current?.sunrise ?: "—"
                    )
                    WeatherDetail(
                        modifier = Modifier.weight(1f),
                        icon = "🌇",
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
                            days.forEachIndexed { index, day ->
                                DayRow(day, index)
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

            BottomBar()
        }
    }

    if (searchOpen) {
        AlertDialog(
            onDismissRequest = { searchOpen = false },
            containerColor = Color(0xFF152338),
            title = {
                Text("Найти город", color = pale, fontWeight = FontWeight.SemiBold)
            },
            text = {
                Column {
                    Text(
                        "Погода в любой точке мира",
                        color = muted,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    TextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        singleLine = true,
                        placeholder = { Text("Например, Москва") },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = pale,
                            unfocusedTextColor = pale,
                            focusedContainerColor = Color.White.copy(alpha = 0.07f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.07f),
                            focusedIndicatorColor = Color(0xFFB9D8FF),
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color(0xFFB9D8FF)
                        )
                    )
                    if (searching) {
                        Spacer(Modifier.height(10.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFFB9D8FF),
                            strokeWidth = 2.dp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = searchText.isNotBlank() && !searching,
                    onClick = {
                        scope.launch {
                            searching = true
                            try {
                                val found = searchCity(searchText.trim())
                                if (found != null) {
                                    place = found
                                    searchOpen = false
                                } else {
                                    error = "Город не найден. Попробуй другое написание."
                                }
                            } catch (e: Exception) {
                                error = "Не удалось найти город. Проверь интернет."
                            }
                            searching = false
                        }
                    }
                ) {
                    Text("Найти", color = Color(0xFFB9D8FF))
                }
            },
            dismissButton = {
                TextButton(onClick = { searchOpen = false }) {
                    Text("Отмена", color = muted)
                }
            }
        )
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
            val image = when (icon) {
                "search" -> Icons.Outlined.Search
                "location" -> Icons.Outlined.LocationOn
                else -> Icons.Outlined.Refresh
            }
            androidx.compose.material3.Icon(
                imageVector = image,
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
                color = muted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(text = subtitle, color = pale, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
        Text("SOLEN", color = Color.White.copy(alpha = 0.28f), fontSize = 9.sp, letterSpacing = 1.2.sp)
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
        Text(weatherEmoji(hour.code, true), fontSize = 22.sp)
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

@Composable
private fun WeatherDetail(
    modifier: Modifier,
    icon: String,
    title: String,
    value: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(21.dp))
            .background(glass)
            .border(1.dp, glassBorder, RoundedCornerShape(21.dp))
            .padding(15.dp)
    ) {
        Text(icon, fontSize = 19.sp)
        Spacer(Modifier.height(12.dp))
        Text(title, color = muted, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = pale, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DayRow(day: DayForecast, index: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (index == 0) "Сегодня" else dayName(day.date),
            color = pale,
            fontSize = 13.sp,
            modifier = Modifier.weight(1.2f)
        )
        Text(weatherEmoji(day.code, true), fontSize = 20.sp, modifier = Modifier.width(43.dp))
        Text(
            text = "${day.min}°",
            color = muted,
            fontSize = 13.sp,
            modifier = Modifier.width(34.dp)
        )
        TemperatureRange(day.min, day.max, Modifier.weight(1f))
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
private fun TemperatureRange(min: Int, max: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.height(5.dp)) {
        val width = size.width
        drawLine(
            color = Color.White.copy(alpha = 0.10f),
            start = Offset(0f, size.height / 2),
            end = Offset(width, size.height / 2),
            strokeWidth = 5.dp.toPx(),
            cap = StrokeCap.Round
        )
        val spread = (max - min).coerceAtLeast(1)
        val offset = (min + 15).coerceIn(0, 40) / 40f
        val barWidth = (spread / 40f).coerceIn(0.18f, 0.92f) * width
        drawLine(
            brush = Brush.horizontalGradient(listOf(Color(0xFF74B5F2), Color(0xFFE8C58D))),
            start = Offset(width * offset, size.height / 2),
            end = Offset((width * offset + barWidth).coerceAtMost(width), size.height / 2),
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
private fun BottomBar() {
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
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomItem("☼", "Сегодня", true)
        BottomItem("⌁", "Карта", false)
        BottomItem("♡", "Места", false)
    }
}

@Composable
private fun BottomItem(symbol: String, label: String, selected: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            symbol,
            color = if (selected) Color(0xFFB9D8FF) else muted.copy(alpha = 0.72f),
            fontSize = 20.sp
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
            "&hourly=temperature_2m,precipitation_probability,weather_code" +
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
                precipitation = probabilities.getInt(i)
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

private suspend fun searchCity(query: String): Place? = withContext(Dispatchers.IO) {
    val safeQuery = URLEncoder.encode(query, "UTF-8")
    val url = URL("https://geocoding-api.open-meteo.com/v1/search?name=$safeQuery&count=1&language=ru&format=json")
    val connection = url.openConnection() as HttpURLConnection
    connection.connectTimeout = 12000
    connection.readTimeout = 12000
    try {
        if (connection.responseCode !in 200..299) return@withContext null
        val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        val results = root.optJSONArray("results") ?: return@withContext null
        if (results.length() == 0) return@withContext null
        val found = results.getJSONObject(0)
        Place(
            name = found.optString("name", query),
            latitude = found.getDouble("latitude"),
            longitude = found.getDouble("longitude"),
            country = found.optString("country", "")
        )
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

private fun weatherEmoji(code: Int, isDay: Boolean): String = when (code) {
    0 -> if (isDay) "☀️" else "🌙"
    1 -> if (isDay) "🌤️" else "☁️"
    2 -> "⛅"
    3 -> "☁️"
    45, 48 -> "🌫️"
    51, 53, 55, 56, 57 -> "🌦️"
    61, 63, 65, 66, 67 -> "🌧️"
    71, 73, 75, 77, 85, 86 -> "❄️"
    80, 81, 82 -> "🌧️"
    95, 96, 99 -> "⛈️"
    else -> "🌤️"
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
