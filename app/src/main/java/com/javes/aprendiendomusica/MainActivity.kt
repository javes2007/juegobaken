package com.javes.aprendiendomusica

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.key.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow

private enum class Page { HOME, PRACTICE, PROGRESS, PROFILE }
private val notes = listOf("Do","Do♯","Re","Re♯","Mi","Fa","Fa♯","Sol","Sol♯","La","La♯","Si")
private fun hz(midi: Int) = 440.0 * 2.0.pow((midi - 69) / 12.0)
private fun note(midi: Int) = "${notes[Math.floorMod(midi, 12)]} ${midi / 12 - 1}"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { MusicApp() } }
}

@Composable
private fun MusicApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("aprendiendo_musica", 0) }
    var name by remember { mutableStateOf(prefs.getString("name", "Músico") ?: "Músico") }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark", true)) }
    var points by remember { mutableIntStateOf(prefs.getInt("points", 0)) }
    var best by remember { mutableIntStateOf(prefs.getInt("best", 0)) }
    var played by remember { mutableIntStateOf(prefs.getInt("played", 0)) }
    var streak by remember { mutableIntStateOf(0) }
    var instrument by remember { mutableStateOf(Instrument.PIANO) }
    var page by remember { mutableStateOf(Page.HOME) }
    var target by remember { mutableIntStateOf(60) }
    var message by remember { mutableStateOf("Toca la nota indicada para ganar puntos.") }
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var audioName by remember { mutableStateOf("No has seleccionado una canción") }
    var media by remember { mutableStateOf<MediaPlayer?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            audioUri = uri
            audioName = uri.lastPathSegment?.substringAfterLast('/') ?: "Audio seleccionado"
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
    }
    val bg = if (dark) Color(0xFF10121C) else Color(0xFFF3F4FA)
    val card = if (dark) Color(0xFF1C2030) else Color.White
    val fg = if (dark) Color(0xFFF7F7FF) else Color(0xFF181A29)
    val muted = if (dark) Color(0xFFA5ABC0) else Color(0xFF697087)
    val accent = if (dark) Color(0xFFAA8BFF) else Color(0xFF7051D8)
    fun save() { prefs.edit().putString("name", name).putBoolean("dark", dark).putInt("points", points).putInt("best", best).putInt("played", played).apply() }
    fun hit(midi: Int) {
        MusicEngine.play(hz(midi), instrument, if (instrument == Instrument.DRUMS || instrument == Instrument.CONGAS) 230 else 420)
        played++
        if (page == Page.PRACTICE) {
            if (midi % 12 == target % 12) {
                points += 10; streak++; message = "¡Muy bien! Nota correcta: +10 puntos."
                if (points > best) best = points
                target = listOf(60, 62, 64, 65, 67, 69, 71, 72).random()
            } else { streak = 0; message = "Sigue intentando. Busca ${note(target)}." }
            save()
        }
    }
    MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = accent, background = bg, surface = card) else lightColorScheme(primary = accent, background = bg, surface = card)) {
        Column(Modifier.fillMaxSize().background(bg).padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("RITMO LAB", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("Aprendiendo Música", color = fg, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                }
                IconButton(onClick = { dark = !dark; save() }) { Icon(if (dark) Icons.Default.LightMode else Icons.Default.DarkMode, "Tema", tint = fg) }
                IconButton(onClick = { page = Page.PROFILE }) { Icon(Icons.Default.AccountCircle, "Perfil", tint = fg) }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Inicio" to Page.HOME, "Practicar" to Page.PRACTICE, "Progreso" to Page.PROGRESS, "Perfil" to Page.PROFILE).forEach { (label, p) ->
                    FilterChip(selected = page == p, onClick = { page = p }, label = { Text(label) })
                }
            }
            Spacer(Modifier.height(12.dp))
            when (page) {
                Page.HOME -> HomePage(name, points, played, instrument, fg, muted, card, accent, { page = Page.PRACTICE }) { instrument = it; page = Page.PRACTICE }
                Page.PRACTICE -> PracticePage(instrument, target, streak, message, fg, muted, card, accent, audioName,
                    onInstrument = { instrument = it }, onHit = { hit(it) }, onTarget = { target = it },
                    onPick = { picker.launch(arrayOf("audio/*")) },
                    onPlay = { runCatching { media?.release(); media = MediaPlayer().apply { setDataSource(context, audioUri!!); prepare(); start() } }.onFailure { audioName = "No se pudo abrir el audio seleccionado" } },
                    onStop = { runCatching { media?.stop(); media?.release() }; media = null })
                Page.PROGRESS -> ProgressPage(points, best, played, fg, muted, card, accent)
                Page.PROFILE -> ProfilePage(name, dark, fg, muted, card,
                    onName = { name = it.ifBlank { "Músico" }; save() },
                    onTheme = { dark = it; save() },
                    onReset = { points = 0; best = 0; played = 0; streak = 0; save() })
            }
            Spacer(Modifier.height(8.dp))
            Text("Aprende, practica y disfruta la música.", color = muted, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp))
        }
    }
    DisposableEffect(Unit) { onDispose { media?.release() } }
}

@Composable
private fun CardBox(color: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(color, RoundedCornerShape(18.dp)).padding(16.dp), content = content)
}
@Composable
private fun HomePage(name: String, points: Int, played: Int, selected: Instrument, fg: Color, muted: Color, card: Color, accent: Color, start: () -> Unit, choose: (Instrument) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
        item {
            CardBox(card) {
                Text("¡Hola, $name! 👋", color = fg, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("Hoy puedes aprender algo nuevo.", color = muted, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Metric("⭐", points.toString(), "Puntos", fg, accent, Modifier.weight(1f))
                    Metric("🎵", played.toString(), "Notas tocadas", fg, accent, Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = start, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("COMENZAR A PRACTICAR") }
            }
        }
        item { Text("Elige tu instrumento", color = fg, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        items(Instrument.values().toList().size) { index ->
            val item = Instrument.values()[index]
            Surface(onClick = { choose(item) }, color = card, shape = RoundedCornerShape(14.dp), border = androidx.compose.foundation.BorderStroke(1.dp, if (selected == item) accent else Color.Transparent)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(item.emoji, fontSize = 29.sp)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(item.label, color = fg, fontWeight = FontWeight.Bold)
                        Text(when (item) { Instrument.PIANO -> "Melodías, escalas y acordes"; Instrument.BASS -> "Líneas graves y ritmo"; Instrument.ACCORDION -> "Melodías de acordeón"; Instrument.DRUMS -> "Pulso y coordinación"; Instrument.CONGAS -> "Patrones de percusión" }, color = muted, fontSize = 11.sp)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = muted)
                }
            }
        }
    }
}
@Composable
private fun Metric(emoji: String, value: String, label: String, fg: Color, accent: Color, modifier: Modifier) {
    Column(modifier.background(accent.copy(alpha = .12f), RoundedCornerShape(12.dp)).padding(12.dp)) {
        Text(emoji, fontSize = 19.sp); Text(value, color = fg, fontSize = 22.sp, fontWeight = FontWeight.Bold); Text(label, color = accent, fontSize = 11.sp)
    }
}
@Composable
private fun PracticePage(instrument: Instrument, target: Int, streak: Int, message: String, fg: Color, muted: Color, card: Color, accent: Color, audioName: String,
    onInstrument: (Instrument) -> Unit, onHit: (Int) -> Unit, onTarget: (Int) -> Unit, onPick: () -> Unit, onPlay: () -> Unit, onStop: () -> Unit) {
    val focus = remember { FocusRequester() }
    var octave by remember { mutableIntStateOf(4) }
    var guide by remember { mutableStateOf(true) }
    Column(Modifier.fillMaxSize().focusRequester(focus).onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        val keys = mapOf(Key.A to 0, Key.S to 2, Key.D to 4, Key.F to 5, Key.G to 7, Key.H to 9, Key.J to 11, Key.K to 12, Key.L to 14,
            Key.W to 1, Key.E to 3, Key.T to 6, Key.Y to 8, Key.U to 10, Key.I to 13, Key.O to 15)
        val offset = keys[event.key] ?: return@onPreviewKeyEvent false
        onHit(60 + offset); true
    }.focusable()) {
        Text("Sala de práctica", color = fg, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Text("Toca las teclas en pantalla o usa un teclado físico.", color = muted, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Instrument.values().forEach { item -> FilterChip(selected = instrument == item, onClick = { onInstrument(item) }, label = { Text("${item.emoji} ${item.label}") }) }
        }
        Spacer(Modifier.height(8.dp))
        CardBox(card) {
            Text("RETO DE NOTAS", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(note(target), color = fg, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold); Text("Racha: $streak", color = muted, fontSize = 11.sp) }
                Button(onClick = { onTarget(listOf(60,62,64,65,67,69,71,72).random()) }) { Text("Nuevo reto") }
            }
            Text(message, color = if (message.startsWith("¡")) Color(0xFF42D6A0) else muted, fontSize = 12.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Teclado", color = fg, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = { guide = !guide }) { Text(if (guide) "Ocultar guía" else "Ver guía") }
            IconButton(onClick = { octave = (octave - 1).coerceAtLeast(2) }) { Icon(Icons.Default.Remove, "Bajar octava", tint = fg) }
            Text("C$octave", color = fg)
            IconButton(onClick = { octave = (octave + 1).coerceAtMost(6) }) { Icon(Icons.Default.Add, "Subir octava", tint = fg) }
        }
        if (guide) Text("PC: A S D F G H J K L · Negras: W E T Y U I O", color = muted, fontSize = 10.sp)
        PianoKeys(octave, onHit)
        Spacer(Modifier.height(8.dp))
        CardBox(card) {
            Text("Practicar con una canción", color = fg, fontWeight = FontWeight.Bold)
            Text(audioName, color = muted, fontSize = 10.sp, maxLines = 2)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onPick, modifier = Modifier.weight(1f)) { Icon(Icons.Default.UploadFile, null); Text("Cargar") }
                Button(onClick = onPlay, modifier = Modifier.weight(1f), enabled = audioName != "No has seleccionado una canción") { Icon(Icons.Default.PlayArrow, null); Text("Escuchar") }
                IconButton(onClick = onStop) { Icon(Icons.Default.Stop, "Detener", tint = fg) }
            }
            Text("Importa un audio para escucharlo mientras practicas.", color = muted, fontSize = 10.sp)
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}
@Composable
private fun PianoKeys(octave: Int, onHit: (Int) -> Unit) {
    val whites = listOf(0,2,4,5,7,9,11)
    val blacks = listOf(1,3,6,8,10)
    BoxWithConstraints(Modifier.fillMaxWidth().height(158.dp).background(Color(0xFF11131B), RoundedCornerShape(10.dp)).padding(4.dp)) {
        val kw = maxWidth / 14
        Row(Modifier.fillMaxSize()) {
            repeat(14) { i ->
                val midi = (octave + 1) * 12 + whites[i % 7] + (i / 7) * 12
                Box(Modifier.weight(1f).fillMaxHeight().padding(horizontal = 1.dp).background(Color(0xFFF7F5F0), RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)).clickable { onHit(midi) }, contentAlignment = Alignment.BottomCenter) {
                    Text(notes[whites[i % 7]], color = Color.DarkGray, fontSize = 8.sp, modifier = Modifier.padding(bottom = 7.dp))
                }
            }
        }
        listOf(0,1,3,4,5,7,8,10,11,12).forEachIndexed { i, pos ->
            val midi = (octave + 1) * 12 + blacks[i % 5] + (i / 5) * 12
            Box(Modifier.offset(x = kw * (pos + 1) - kw * .32f).width(kw * .62f).height(96.dp)
                .background(Color(0xFF272A38), RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                .border(1.dp, Color.Black, RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)).clickable { onHit(midi) })
        }
    }
}
@Composable
private fun ProgressPage(points: Int, best: Int, played: Int, fg: Color, muted: Color, card: Color, accent: Color) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Tu progreso", color = fg, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        CardBox(card) {
            Text("Nivel actual", color = muted, fontSize = 12.sp)
            Text(if (points < 100) "Principiante" else if (points < 300) "Intermedio" else "Avanzado", color = fg, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(progress = { (points % 100) / 100f }, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
            Text("${points % 100}/100 puntos para el siguiente nivel", color = muted, fontSize = 11.sp)
        }
        Metric("⭐", points.toString(), "Puntos acumulados", fg, accent, Modifier.fillMaxWidth())
        Metric("🏆", best.toString(), "Mejor puntuación", fg, accent, Modifier.fillMaxWidth())
        Metric("🎼", played.toString(), "Notas tocadas", fg, accent, Modifier.fillMaxWidth())
        Text("Consejo: practica primero las notas naturales y luego incorpora sostenidos y bemoles.", color = muted, fontSize = 12.sp)
    }
}
@Composable
private fun ProfilePage(name: String, dark: Boolean, fg: Color, muted: Color, card: Color, onName: (String) -> Unit, onTheme: (Boolean) -> Unit, onReset: () -> Unit) {
    var draft by remember(name) { mutableStateOf(name) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Mi perfil", color = fg, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        CardBox(card) {
            Text("Nombre del músico", color = muted, fontSize = 12.sp)
            OutlinedTextField(value = draft, onValueChange = { draft = it }, label = { Text("Tu nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(onClick = { onName(draft) }, modifier = Modifier.fillMaxWidth()) { Text("Guardar nombre") }
        }
        CardBox(card) { Row(verticalAlignment = Alignment.CenterVertically) { Text("Modo oscuro", color = fg, modifier = Modifier.weight(1f)); Switch(checked = dark, onCheckedChange = onTheme) } }
        OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.RestartAlt, null); Text(" Reiniciar progreso") }
        Text("Tu perfil y puntuaciones se guardan en este dispositivo.", color = muted, fontSize = 11.sp)
    }
}
