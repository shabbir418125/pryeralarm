package com.prayeralarm.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.prayeralarm.app.DISTRICTS
import com.prayeralarm.app.R
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prayeralarm.app.AppSettings
import com.prayeralarm.app.Prayer
import com.prayeralarm.app.Times
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

// Colors taken from the purple studio background of the profile photo
private val Deep = Color(0xFF0E0140)      // darkest edge of the photo background
private val Bg = Color(0xFF16055A)        // app background
private val CardBg = Color(0xFF2A0F82)    // normal card
private val CardNext = Color(0xFF4B02B0)  // highlighted (next prayer) card
private val Violet = Color(0xFF7103D1)    // bright glow of the photo background
private val Lavender = Color(0xFFD2B8FF)  // accent text / icons on dark purple
private val Muted = Color(0xFFB9A8E8)     // secondary text
private val Outline = Color(0xFF4A2D9E)

class Actions(
    val pickMp3: (Prayer) -> Unit,
    val pickDefaultAdhan: () -> Unit,
    val pickReminder: () -> Unit,
    val useGps: () -> Unit,
    val batterySettings: () -> Unit,
    val notificationPermission: () -> Unit,
    val testPlay: (Prayer) -> Unit,
    val stopPlay: () -> Unit,
)

private sealed interface Screen {
    data object Home : Screen
    data object Config : Screen
    data class Detail(val prayer: Prayer) : Screen
}

private fun String.bn(): String =
    map { if (it in '0'..'9') '০' + (it - '0') else it }.joinToString("")

private fun fmtTime(ms: Long): String =
    Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)).bn()

private fun fmtCountdown(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d:%02d".format(total / 3600, (total % 3600) / 60, total % 60).bn()
}

private fun fmtDate(d: LocalDate): String =
    d.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("bn", "BD")))

@Composable
fun PrayerApp(rev: Int, onChanged: () -> Unit, actions: Actions) {
    val ctx = LocalContext.current
    val settings = remember { AppSettings(ctx) }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Lavender, onPrimary = Deep,
            secondary = Violet, onSecondary = Color.White,
            secondaryContainer = Violet, onSecondaryContainer = Color.White,
            surface = Bg, onSurface = Color.White,
            background = Bg, onBackground = Color.White,
            surfaceVariant = CardBg, onSurfaceVariant = Muted,
            surfaceContainer = CardBg, surfaceContainerHigh = CardBg, surfaceContainerHighest = CardBg,
            outline = Muted, outlineVariant = Outline,
        )
    ) {
      Surface(Modifier.fillMaxSize(), color = Bg, contentColor = Color.White) {
        val s = screen
        if (s is Screen.Detail) {
            BackHandler { screen = Screen.Home }
            PrayerDetailScreen(s.prayer, settings, rev, onChanged, actions) { screen = Screen.Home }
        } else {
            Scaffold(
                containerColor = Bg,
                bottomBar = {
                    NavigationBar(containerColor = Deep) {
                        NavigationBarItem(
                            selected = s is Screen.Home,
                            onClick = { screen = Screen.Home },
                            icon = { Icon(Icons.Filled.Home, null) },
                            label = { Text("হোম") },
                        )
                        NavigationBarItem(
                            selected = s is Screen.Config,
                            onClick = { screen = Screen.Config },
                            icon = { Icon(Icons.Filled.Settings, null) },
                            label = { Text("সেটিংস") },
                        )
                    }
                },
            ) { pad ->
                Box(Modifier.padding(pad).fillMaxSize()) {
                    if (s is Screen.Home) {
                        HomeScreen(settings, rev, now, onChanged) { screen = Screen.Detail(it) }
                    } else {
                        ConfigScreen(settings, rev, onChanged, actions)
                    }
                }
            }
        }
      }
    }
}

// ───────────────────────── Home ─────────────────────────

@Composable
private fun HomeScreen(
    settings: AppSettings,
    rev: Int,
    now: Long,
    onChanged: () -> Unit,
    openDetail: (Prayer) -> Unit,
) {
    @Suppress("UNUSED_VARIABLE") val r = rev
    val day = Times.forDate(LocalDate.now(), settings)
    val next = Times.next(now, settings, onlyEnabled = false)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)) {
        item {
            Column(
                Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Deep, Violet)))
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("স্বয়ংক্রিয় নামাজের এলার্ম", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text("অ্যাপ নির্মাতা: সাব্বির", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    }
                    Image(
                        painter = painterResource(R.drawable.profile),
                        contentDescription = "সাব্বির",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(46.dp).clip(CircleShape).border(2.dp, Color.White, CircleShape),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(settings.placeName, color = Color.White, fontSize = 14.sp)
                }
                Text(fmtDate(LocalDate.now()), color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                Spacer(Modifier.height(20.dp))
                if (next != null) {
                    Text("পরবর্তী ওয়াক্ত", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(next.first.bn, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(12.dp))
                        Text(fmtTime(next.second), color = Color.White, fontSize = 22.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        fmtCountdown(next.second - now),
                        color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Light,
                    )
                    Text("বাকি", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                }
            }
        }
        val rows: List<Pair<Prayer?, Long>> = listOf(
            Prayer.FAJR to day.fajr, null to day.sunrise, Prayer.DHUHR to day.dhuhr,
            Prayer.ASR to day.asr, Prayer.MAGHRIB to day.maghrib, Prayer.ISHA to day.isha,
        )
        items(rows) { (p, t) ->
            val isNext = p != null && next?.first == p
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)
                    .clickable(enabled = p != null) { p?.let(openDetail) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = if (isNext) CardNext else CardBg),
                border = BorderStroke(if (isNext) 1.5.dp else 1.dp, if (isNext) Lavender else Outline),
            ) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p?.bn ?: "সূর্যোদয়", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = if (isNext) Color.White else Lavender)
                        Text(fmtTime(t), fontSize = 15.sp, color = Muted)
                    }
                    if (p != null) {
                        val on = settings.enabled(p)
                        IconButton(onClick = { settings.setEnabled(p, !on); onChanged() }) {
                            Icon(
                                if (on) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                                contentDescription = null,
                                tint = if (on) Lavender else Muted,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────── Per-prayer settings ─────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrayerDetailScreen(
    prayer: Prayer,
    settings: AppSettings,
    rev: Int,
    onChanged: () -> Unit,
    actions: Actions,
    onBack: () -> Unit,
) {
    @Suppress("UNUSED_VARIABLE") val r = rev
    val offset = settings.offset(prayer)
    val day = Times.forDate(LocalDate.now(), settings)
    val customName = settings.customName(prayer)
    val hasCustom = settings.customPath(prayer) != null

    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth().background(Deep).statusBarsPadding().padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
            Text(prayer.bn, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Color.White)) { Text("সম্পন্ন") }
            Spacer(Modifier.width(8.dp))
        }

        Text("সময় সমন্বয়", fontSize = 18.sp, modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp))
        Card(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("${prayer.bn} ${fmtTime(day.of(prayer))}", fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SquareButton(Icons.Filled.Remove) { settings.setOffset(prayer, offset - 1); onChanged() }
                    Text(
                        "${offset.toString().bn()} মিনিট",
                        modifier = Modifier.weight(1f), fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    SquareButton(Icons.Filled.Add) { settings.setOffset(prayer, offset + 1); onChanged() }
                }
                Slider(
                    value = offset.toFloat(),
                    onValueChange = { settings.setOffset(prayer, it.roundToInt()); onChanged() },
                    valueRange = -120f..120f,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("-১২০ মিনিট", fontSize = 12.sp, color = Muted)
                    Text("১২০ মিনিট", fontSize = 12.sp, color = Muted)
                }
                TextButton(
                    onClick = { settings.setOffset(prayer, 0); onChanged() },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text("রিসেট", color = Color.White) }
            }
        }

        val pickCtx = LocalContext.current
        val customMin = settings.customTime(prayer)
        Text("আজানের সময় নির্ধারণ", fontSize = 18.sp, modifier = Modifier.padding(16.dp, 20.dp, 16.dp, 8.dp))
        Card(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                Row(
                    Modifier.fillMaxWidth()
                        .clickable { settings.setCustomTime(prayer, -1); onChanged() }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("স্বয়ংক্রিয় (হিসাব + উপরের মিনিট সমন্বয়)", color = if (customMin < 0) Lavender else Color.White)
                    }
                    RadioButton(selected = customMin < 0, onClick = { settings.setCustomTime(prayer, -1); onChanged() })
                }
                val openPicker = {
                    val cur = if (customMin >= 0) customMin else {
                        val z = Instant.ofEpochMilli(day.of(prayer)).atZone(ZoneId.systemDefault())
                        z.hour * 60 + z.minute
                    }
                    android.app.TimePickerDialog(
                        pickCtx,
                        { _, h, m -> settings.setCustomTime(prayer, h * 60 + m); onChanged() },
                        cur / 60, cur % 60, false,
                    ).show()
                }
                Row(
                    Modifier.fillMaxWidth()
                        .clickable { openPicker() }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("নিজের নির্ধারিত সময়", color = if (customMin >= 0) Lavender else Color.White)
                        if (customMin >= 0) {
                            Text(fmtTime(day.of(prayer)) + " (প্রতিদিন একই সময়)", fontSize = 12.sp, color = Muted)
                        } else {
                            Text("ট্যাপ করে ঘড়ির সময় বেছে নিন", fontSize = 12.sp, color = Muted)
                        }
                    }
                    RadioButton(selected = customMin >= 0, onClick = { openPicker() })
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(16.dp, 20.dp, 16.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (settings.enabled(prayer)) "আজান সময় - চালু" else "আজান সময় - বন্ধ",
                fontSize = 18.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f),
            )
            Switch(
                checked = settings.enabled(prayer),
                onCheckedChange = { settings.setEnabled(prayer, it); onChanged() },
                colors = SwitchDefaults.colors(checkedTrackColor = Violet, checkedThumbColor = Color.White),
            )
        }

        Card(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
        ) {
            Column(Modifier.padding(vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth().clickable { settings.setCustom(prayer, null, null); onChanged() }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("ডিফল্ট আজান (সেটিংসে বাছাই করা)",modifier = Modifier.weight(1f), color = if (!hasCustom) Lavender else Color.White)
                    RadioButton(selected = !hasCustom, onClick = { settings.setCustom(prayer, null, null); onChanged() })
                }
                Row(Modifier.fillMaxWidth().clickable { actions.pickMp3(prayer) }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("কাস্টম MP3", color = if (hasCustom) Lavender else Color.White)
                        if (customName != null) Text(customName, fontSize = 12.sp, color = Muted)
                    }
                    RadioButton(selected = hasCustom, onClick = { actions.pickMp3(prayer) })
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { actions.testPlay(prayer) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Color.White),
            ) {
                Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("পরীক্ষা করুন")
            }
            OutlinedButton(onClick = actions.stopPlay, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Stop, null); Spacer(Modifier.width(6.dp)); Text("বন্ধ")
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SquareButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Violet).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
}

// ───────────────────────── Settings ─────────────────────────

private val PLACES = listOf(
    Triple("হবিগঞ্জ", 24.3745, 91.4156),
    Triple("ঢাকা", 23.8103, 90.4125),
    Triple("চট্টগ্রাম", 22.3569, 91.7832),
    Triple("সিলেট", 24.8949, 91.8687),
    Triple("মৌলভীবাজার", 24.4829, 91.7774),
    Triple("রাজশাহী", 24.3636, 88.6241),
    Triple("খুলনা", 22.8456, 89.5403),
    Triple("বরিশাল", 22.7010, 90.3535),
    Triple("রংপুর", 25.7439, 89.2752),
    Triple("ময়মনসিংহ", 24.7471, 90.4203),
    Triple("কক্সবাজার", 21.4272, 92.0058),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigScreen(settings: AppSettings, rev: Int, onChanged: () -> Unit, actions: Actions) {
    @Suppress("UNUSED_VARIABLE") val r = rev
    var latText by remember(rev) { mutableStateOf(settings.lat.toString()) }
    var lonText by remember(rev) { mutableStateOf(settings.lon.toString()) }
    var showDistricts by remember { mutableStateOf(false) }
    val cfgCtx = LocalContext.current
    val hasBundledMakkah = remember {
        cfgCtx.resources.getIdentifier("adhan_makkah", "raw", cfgCtx.packageName) != 0
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().background(Deep).statusBarsPadding().padding(20.dp)) {
            Text("সেটিংস", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }

        SectionTitle("অবস্থান")
        OutlinedButton(onClick = { showDistricts = true }, modifier = Modifier.padding(horizontal = 16.dp)) {
            Icon(Icons.Filled.LocationOn, null)
            Spacer(Modifier.width(6.dp))
            Text("জেলা: ${settings.placeName} (ম্যাপ/তালিকা থেকে ৬৪ জেলা)")
        }
        if (showDistricts) {
            DistrictPickerDialog(
                current = settings.placeName,
                onPick = { d ->
                    settings.placeName = d.first
                    settings.lat = d.second
                    settings.lon = d.third
                    onChanged()
                    showDistricts = false
                },
                onDismiss = { showDistricts = false },
            )
        }
        Row(Modifier.fillMaxWidth().padding(16.dp, 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = latText, onValueChange = { latText = it },
                label = { Text("অক্ষাংশ") }, singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = lonText, onValueChange = { lonText = it },
                label = { Text("দ্রাঘিমাংশ") }, singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val la = latText.toDoubleOrNull()
                    val lo = lonText.toDoubleOrNull()
                    if (la != null && lo != null && la in -90.0..90.0 && lo in -180.0..180.0) {
                        settings.lat = la; settings.lon = lo; settings.placeName = "কাস্টম অবস্থান"; onChanged()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Color.White),
            ) { Text("সংরক্ষণ") }
            OutlinedButton(onClick = actions.useGps) {
                Icon(Icons.Filled.LocationOn, null); Spacer(Modifier.width(4.dp)); Text("GPS ব্যবহার করুন")
            }
        }

        SectionTitle("হিসাব পদ্ধতি")
        SwitchRow("আসর: হানাফি (ছায়া দ্বিগুণ)", settings.asrHanafi) { settings.asrHanafi = it; onChanged() }
        Text(
            "ফজর ও এশা: কারাচি পদ্ধতি (১৮°)। প্রতিটি ওয়াক্তে ±১২০ মিনিট সমন্বয় করা যায়।",
            fontSize = 12.sp, color = Muted, modifier = Modifier.padding(horizontal = 16.dp),
        )

        SectionTitle("আজান ও রিমাইন্ডার")
        AudioRow(
            title = "মক্কার আজান (সব ওয়াক্তের জন্য)",
            subtitle = settings.defaultAdhanName
                ?: if (hasBundledMakkah) "অ্যাপের ভেতরের মক্কার আজান ব্যবহার হচ্ছে। চাইলে অন্য MP3 বাছুন"
                else "মক্কার আজানের MP3 ফাইল বাছুন (অথবা adhan_makkah.mp3 অ্যাপে যোগ করে বিল্ড করুন)",
            hasValue = settings.defaultAdhanPath != null,
            onPick = actions.pickDefaultAdhan,
            onClear = { settings.defaultAdhanPath = null; settings.defaultAdhanName = null; onChanged() },
        )
        SwitchRow("আজানের পর রিমাইন্ডার: “ওয়াক্ত হয়েছে, নামাজের জন্য প্রস্তুত হোন”", settings.reminderEnabled) {
            settings.reminderEnabled = it; onChanged()
        }
        AudioRow(
            title = "নিজের রেকর্ড করা রিমাইন্ডার (ঐচ্ছিক)",
            subtitle = settings.reminderName ?: "বাছাই করা হয়নি। ফোনের বাংলা ভয়েস বলবে",
            hasValue = settings.reminderPath != null,
            onPick = actions.pickReminder,
            onClear = { settings.reminderPath = null; settings.reminderName = null; onChanged() },
        )

        SectionTitle("রিমাইন্ডারের সময়সূচি")
        SwitchRow("আজানের আগে রিমাইন্ডার", settings.preEnabled) { settings.preEnabled = it; onChanged() }
        if (settings.preEnabled) {
            Text(
                "আজানের কত মিনিট আগে (এক বা একাধিক বাছুন, যেমন ১৫ ও ১০ মিনিট)",
                fontSize = 13.sp, color = Muted, modifier = Modifier.padding(horizontal = 16.dp),
            )
            val chosen = settings.preMinutesList
            MultiChipRow(listOf(5, 10, 15, 20, 30, 45, 60), chosen, { "${it.toString().bn()} মিনিট" }) { m ->
                settings.preMinutesList = if (m in chosen) chosen - m else chosen + m
                onChanged()
            }
            if (chosen.isEmpty()) {
                Text("কোনো সময় বাছা হয়নি, তাই আজানের আগে রিমাইন্ডার বাজবে না।", fontSize = 12.sp, color = Muted, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
        Text("আজানের পরে রিমাইন্ডার কতবার", fontSize = 13.sp, color = Muted, modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 0.dp))
        ChipRow(listOf(0, 1, 2, 3, 4, 5), settings.postCount, { if (it == 0) "বন্ধ" else "${it.toString().bn()} বার" }) {
            settings.postCount = it; onChanged()
        }
        if (settings.postCount > 0) {
            Text("প্রতি কত মিনিট পর পর", fontSize = 13.sp, color = Muted, modifier = Modifier.padding(horizontal = 16.dp))
            ChipRow(listOf(5, 10, 15, 20, 30), settings.postInterval, { "${it.toString().bn()} মিনিট" }) {
                settings.postInterval = it; onChanged()
            }
        }

        SectionTitle("ভলিউম ও এলার্ম")
        Text("ভলিউম: ${settings.volume.toString().bn()}%", modifier = Modifier.padding(horizontal = 16.dp))
        Slider(
            value = settings.volume.toFloat(),
            onValueChange = { settings.volume = it.roundToInt(); onChanged() },
            valueRange = 0f..100f,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        SwitchRow("আজানের সময় এলার্ম ভলিউম বাড়িয়ে দিন", settings.raiseAlarmVolume) { settings.raiseAlarmVolume = it; onChanged() }
        SwitchRow("আজানের সময় ভাইব্রেশন", settings.vibrate) { settings.vibrate = it; onChanged() }

        SectionTitle("ব্যাকগ্রাউন্ডে নির্ভুলভাবে চালাতে")
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = actions.batterySettings) { Text("ব্যাটারি সীমা বন্ধ করুন") }
            OutlinedButton(onClick = actions.notificationPermission) { Text("নোটিফিকেশন অনুমতি") }
        }
        Text(
            "ফোনের ব্যাটারি সেভার বা অটো-স্টার্ট সীমা এই অ্যাপের জন্য বন্ধ রাখুন, তাহলে স্ক্রিন অফ থাকলেও আজান ঠিক সময়ে বাজবে। স্পিকার/ব্লুটুথ কানেক্ট থাকলে আজান সেখানেই বাজবে।",
            fontSize = 12.sp, color = Muted, modifier = Modifier.padding(16.dp),
        )
        Spacer(Modifier.height(16.dp))
    }
}

private typealias District = Triple<String, Double, Double>

/** Rough outline of Bangladesh as (longitude to latitude) points, used only to draw the map. */
private val BD_OUTLINE: List<Pair<Double, Double>> = listOf(
    88.35 to 26.46,
    88.80 to 26.42,
    89.05 to 26.35,
    89.40 to 26.10,
    89.75 to 26.12,
    89.85 to 25.65,
    89.85 to 25.30,
    90.25 to 25.18,
    90.70 to 25.15,
    91.20 to 25.18,
    91.65 to 25.15,
    92.05 to 25.12,
    92.30 to 24.95,
    92.20 to 24.60,
    92.05 to 24.30,
    91.80 to 24.15,
    91.70 to 23.90,
    91.60 to 23.65,
    91.45 to 23.25,
    91.60 to 23.05,
    91.75 to 23.10,
    92.00 to 23.65,
    92.30 to 23.25,
    92.40 to 22.80,
    92.35 to 22.30,
    92.30 to 21.70,
    92.25 to 21.20,
    92.30 to 20.80,
    92.05 to 21.12,
    91.92 to 21.45,
    91.88 to 21.85,
    91.85 to 22.15,
    91.70 to 22.40,
    91.40 to 22.55,
    91.05 to 22.50,
    90.70 to 22.35,
    90.40 to 22.00,
    90.10 to 21.85,
    89.60 to 21.80,
    89.15 to 21.80,
    89.05 to 22.15,
    89.10 to 22.55,
    88.90 to 22.85,
    88.75 to 23.15,
    88.70 to 23.40,
    88.50 to 23.75,
    88.60 to 24.00,
    88.30 to 24.25,
    88.10 to 24.50,
    88.20 to 25.00,
    88.45 to 25.30,
    88.10 to 25.60,
    88.25 to 25.90,
    88.10 to 26.15
)

private const val MAP_MIN_LON = 87.9
private const val MAP_MAX_LON = 92.8
private const val MAP_MIN_LAT = 20.5
private const val MAP_MAX_LAT = 26.7

private fun mapPoint(lon: Double, lat: Double, w: Float, h: Float) = Offset(
    ((lon - MAP_MIN_LON) / (MAP_MAX_LON - MAP_MIN_LON) * w).toFloat(),
    ((MAP_MAX_LAT - lat) / (MAP_MAX_LAT - MAP_MIN_LAT) * h).toFloat(),
)

@Composable
private fun DistrictMap(current: String, picked: District?, onTap: (District) -> Unit) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .aspectRatio(0.725f)
            .pointerInput(Unit) {
                detectTapGestures { off ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    var best: District? = null
                    var bestDist = Float.MAX_VALUE
                    for (d in DISTRICTS) {
                        val dist = (mapPoint(d.third, d.second, w, h) - off).getDistance()
                        if (dist < bestDist) { bestDist = dist; best = d }
                    }
                    best?.let(onTap)
                }
            }
    ) {
        val w = size.width
        val h = size.height
        val outline = Path()
        BD_OUTLINE.forEachIndexed { i, (lon, lat) ->
            val p = mapPoint(lon, lat, w, h)
            if (i == 0) outline.moveTo(p.x, p.y) else outline.lineTo(p.x, p.y)
        }
        outline.close()
        drawPath(outline, CardBg)
        drawPath(outline, Violet, style = Stroke(width = 3.dp.toPx()))
        for (d in DISTRICTS) {
            val c = mapPoint(d.third, d.second, w, h)
            when (d.first) {
                picked?.first -> {
                    drawCircle(Color.White, radius = 10.dp.toPx(), center = c)
                    drawCircle(Violet, radius = 6.dp.toPx(), center = c)
                }
                current -> drawCircle(Color.White, radius = 6.dp.toPx(), center = c)
                else -> drawCircle(Lavender.copy(alpha = 0.9f), radius = 3.5.dp.toPx(), center = c)
            }
        }
    }
}

@Composable
private fun DistrictPickerDialog(
    current: String,
    onPick: (District) -> Unit,
    onDismiss: () -> Unit,
) {
    var mapMode by remember { mutableStateOf(true) }
    var picked by remember { mutableStateOf<District?>(DISTRICTS.firstOrNull { it.first == current }) }
    var q by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Bg, contentColor = Color.White) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().background(Deep).padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("জেলা বাছুন (৬৪টি জেলা)", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("বন্ধ") }
                }
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = mapMode, onClick = { mapMode = true }, label = { Text("ম্যাপ") })
                    FilterChip(selected = !mapMode, onClick = { mapMode = false }, label = { Text("তালিকা") })
                }
                if (mapMode) {
                    Column(
                        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("ম্যাপে আপনার জেলার বিন্দুতে ট্যাপ করুন", fontSize = 13.sp, color = Muted)
                        Spacer(Modifier.height(8.dp))
                        DistrictMap(current, picked) { picked = it }
                        Text("ম্যাপের সীমানা আনুমানিক। নামাজের সময় জেলার অবস্থান থেকে হিসাব হয়।", fontSize = 11.sp, color = Muted)
                    }
                    Row(
                        Modifier.fillMaxWidth().background(Deep).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("নির্বাচিত জেলা", fontSize = 12.sp, color = Muted)
                            Text(picked?.first ?: "—", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { picked?.let(onPick) },
                            enabled = picked != null,
                            colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Color.White),
                        ) { Text("বেছে নিন") }
                    }
                } else {
                    OutlinedTextField(
                        value = q, onValueChange = { q = it },
                        singleLine = true, label = { Text("জেলার নাম খুঁজুন") },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                    val list = DISTRICTS.filter { it.first.contains(q.trim()) }
                    LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                        items(list) { d ->
                            Row(
                                Modifier.fillMaxWidth().clickable { onPick(d) }.padding(vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    d.first, modifier = Modifier.weight(1f), fontSize = 16.sp,
                                    color = if (d.first == current) Lavender else Color.White,
                                    fontWeight = if (d.first == current) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiChipRow(options: List<Int>, selected: List<Int>, label: (Int) -> String, onToggle: (Int) -> Unit) {
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options) { v ->
            FilterChip(selected = v in selected, onClick = { onToggle(v) }, label = { Text(label(v)) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChipRow(options: List<Int>, selected: Int, label: (Int) -> String, onSelect: (Int) -> Unit) {
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options) { v ->
            FilterChip(selected = selected == v, onClick = { onSelect(v) }, label = { Text(label(v)) })
        }
    }
}

@Composable
private fun AudioRow(title: String, subtitle: String, hasValue: Boolean, onPick: () -> Unit, onClear: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(title, fontWeight = FontWeight.Medium)
        Text(subtitle, fontSize = 12.sp, color = Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            OutlinedButton(onClick = onPick) { Text("ফাইল বাছুন") }
            if (hasValue) TextButton(onClick = onClear) { Text("মুছুন") }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Lavender, modifier = Modifier.padding(16.dp, 20.dp, 16.dp, 8.dp))
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = Violet, checkedThumbColor = Color.White))
    }
}
