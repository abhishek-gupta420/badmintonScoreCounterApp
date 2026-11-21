package com.abhi.badmintonscoreconunter

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.abhi.badmintonscoreconunter.R
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class Screen {
    Splash, Setup, Score, Winner
}

data class SetResult(val team1Score: Int, val team2Score: Int)

data class SavedMatchState(
    val screen: Screen,
    val team1Name: String,
    val team2Name: String,
    val matchPoints: Int,
    val team1Sets: Int,
    val team2Sets: Int,
    val score1: Int,
    val score2: Int,
    val history: List<SetResult>,
    val winnerName: String,
    val forceWatchMode: Boolean
)

object MatchStorage {
    private const val PREFS_NAME = "badminton_score_prefs"

    fun save(context: Context, state: SavedMatchState) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val historyStr = state.history.joinToString(";") { "${it.team1Score}-${it.team2Score}" }
        prefs.edit()
            .putString("screen", state.screen.name)
            .putString("team1", state.team1Name)
            .putString("team2", state.team2Name)
            .putInt("matchPoints", state.matchPoints)
            .putInt("team1Sets", state.team1Sets)
            .putInt("team2Sets", state.team2Sets)
            .putInt("score1", state.score1)
            .putInt("score2", state.score2)
            .putString("history", historyStr)
            .putString("winner", state.winnerName)
            .putBoolean("forceWatch", state.forceWatchMode)
            .apply()
    }

    fun load(context: Context): SavedMatchState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val screenName = prefs.getString("screen", Screen.Splash.name) ?: Screen.Splash.name
        val screen = try { Screen.valueOf(screenName) } catch (_: Exception) { Screen.Splash }
        val team1 = prefs.getString("team1", "") ?: ""
        val team2 = prefs.getString("team2", "") ?: ""
        val matchPoints = prefs.getInt("matchPoints", 21)
        val t1Sets = prefs.getInt("team1Sets", 0)
        val t2Sets = prefs.getInt("team2Sets", 0)
        val s1 = prefs.getInt("score1", 0)
        val s2 = prefs.getInt("score2", 0)
        val historyStr = prefs.getString("history", "") ?: ""
        val history = if (historyStr.isEmpty()) emptyList() else historyStr.split(";").mapNotNull {
            val parts = it.split("-")
            if (parts.size == 2) {
                val p1 = parts[0].toIntOrNull()
                val p2 = parts[1].toIntOrNull()
                if (p1 != null && p2 != null) SetResult(p1, p2) else null
            } else null
        }
        val winner = prefs.getString("winner", "") ?: ""
        val forceWatch = prefs.getBoolean("forceWatch", false)

        return SavedMatchState(
            screen = screen,
            team1Name = team1,
            team2Name = team2,
            matchPoints = matchPoints,
            team1Sets = t1Sets,
            team2Sets = t2Sets,
            score1 = s1,
            score2 = s2,
            history = history,
            winnerName = winner,
            forceWatchMode = forceWatch
        )
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}

fun triggerWristVibration(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            val vibrator = vibratorManager?.defaultVibrator
            vibrator?.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(70)
            }
        }
    } catch (_: Exception) {}
}

@Composable
fun CurrentTimeDisplay(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 12.sp,
    color: Color = Color.White
) {
    var timeText by remember { mutableStateOf(getCurrentTimeString()) }

    LaunchedEffect(Unit) {
        while (true) {
            timeText = getCurrentTimeString()
            delay(1000)
        }
    }

    Text(
        text = timeText,
        color = color,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

fun getCurrentTimeString(): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date())
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            BadmintonScoreCounterApp()
        }
    }
}

@Composable
fun BadmintonScoreCounterApp() {
    val context = LocalContext.current
    val isSystemWatch = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)
    }

    val savedState = remember { MatchStorage.load(context) }

    var forceWatchMode by remember { mutableStateOf(savedState.forceWatchMode) }
    val useWatchUi = isSystemWatch || forceWatchMode

    var currentScreen by remember {
        mutableStateOf(if (savedState.screen == Screen.Splash) Screen.Splash else savedState.screen)
    }

    var team1Name by remember { mutableStateOf(if (savedState.team1Name.isEmpty() && useWatchUi) "T1" else savedState.team1Name) }
    var team2Name by remember { mutableStateOf(if (savedState.team2Name.isEmpty() && useWatchUi) "T2" else savedState.team2Name) }
    var matchPoints by remember { mutableIntStateOf(savedState.matchPoints) }

    var team1SetsWon by remember { mutableIntStateOf(savedState.team1Sets) }
    var team2SetsWon by remember { mutableIntStateOf(savedState.team2Sets) }
    var currentSetScore1 by remember { mutableIntStateOf(savedState.score1) }
    var currentSetScore2 by remember { mutableIntStateOf(savedState.score2) }
    val setHistory = remember { mutableStateListOf<SetResult>().apply { addAll(savedState.history) } }
    var winnerName by remember { mutableStateOf(savedState.winnerName) }
    var setWinnerMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentScreen, team1Name, team2Name, matchPoints, team1SetsWon, team2SetsWon, currentSetScore1, currentSetScore2, setHistory.size, winnerName, forceWatchMode) {
        if (currentScreen != Screen.Splash) {
            MatchStorage.save(
                context,
                SavedMatchState(
                    screen = currentScreen,
                    team1Name = team1Name,
                    team2Name = team2Name,
                    matchPoints = matchPoints,
                    team1Sets = team1SetsWon,
                    team2Sets = team2SetsWon,
                    score1 = currentSetScore1,
                    score2 = currentSetScore2,
                    history = setHistory.toList(),
                    winnerName = winnerName,
                    forceWatchMode = forceWatchMode
                )
            )
        }
    }

    val darkBackground = Color(0x0A, 0x0A, 0x1A)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = darkBackground
    ) {
        when (currentScreen) {
            Screen.Splash -> {
                SplashScreen(onTimeout = {
                    currentScreen = if (savedState.screen != Screen.Splash) savedState.screen else Screen.Setup
                })
            }
            Screen.Setup -> {
                if (useWatchUi) {
                    WatchSetupScreen(
                        team1Name = if (team1Name.length == 0) "T1" else team1Name,
                        onTeam1NameChange = { team1Name = it },
                        team2Name = if (team2Name.length == 0) "T2" else team2Name,
                        onTeam2NameChange = { team2Name = it },
                        matchPoints = matchPoints,
                        onMatchPointsChange = { matchPoints = it },
                        onStartMatch = {
                            team1SetsWon = 0
                            team2SetsWon = 0
                            currentSetScore1 = 0
                            currentSetScore2 = 0
                            setHistory.clear()
                            currentScreen = Screen.Score
                        }
                    )
                } else {
                    SetupScreen(
                        team1Name = team1Name,
                        onTeam1NameChange = { team1Name = it },
                        team2Name = team2Name,
                        onTeam2NameChange = { team2Name = it },
                        matchPoints = matchPoints,
                        onMatchPointsChange = { matchPoints = it },
                        onStartMatch = {
                            team1SetsWon = 0
                            team2SetsWon = 0
                            currentSetScore1 = 0
                            currentSetScore2 = 0
                            setHistory.clear()
                            currentScreen = Screen.Score
                        },
                        onToggleWatchMode = { forceWatchMode = true }
                    )
                }
            }
            Screen.Score -> {
                val t1 = if (team1Name.length == 0) "T1" else team1Name
                val t2 = if (team2Name.length == 0) "T2" else team2Name

                if (useWatchUi) {
                    WatchScoreScreen(
                        team1Name = t1,
                        team2Name = t2,
                        matchPoints = matchPoints,
                        team1Score = currentSetScore1,
                        team2Score = currentSetScore2,
                        team1Sets = team1SetsWon,
                        team2Sets = team2SetsWon,
                        setHistory = setHistory,
                        setWinnerMessage = setWinnerMessage,
                        onDismissSetWinner = {
                            setWinnerMessage = null
                            if (team1SetsWon == 2 || team2SetsWon == 2) {
                                winnerName = if (team1SetsWon == 2) t1 else t2
                                currentScreen = Screen.Winner
                            } else {
                                currentSetScore1 = 0
                                currentSetScore2 = 0
                            }
                        },
                        onScoreChange = { s1, s2 ->
                            if (setWinnerMessage != null) return@WatchScoreScreen

                            currentSetScore1 = s1
                            currentSetScore2 = s2

                            val maxPoints = matchPoints + 9
                            val isSetOver = when {
                                (s1 >= matchPoints || s2 >= matchPoints) && abs(s1 - s2) >= 2 -> true
                                s1 == maxPoints || s2 == maxPoints -> true
                                else -> false
                            }

                            if (isSetOver) {
                                setHistory.add(SetResult(s1, s2))
                                val setWinner = if (s1 > s2) t1 else t2
                                if (s1 > s2) team1SetsWon++ else team2SetsWon++
                                setWinnerMessage = "Set Winner: $setWinner"
                            }
                        },
                        onReset = {
                            MatchStorage.clear(context)
                            currentScreen = Screen.Setup
                        }
                    )
                } else {
                    ScoreScreen(
                        team1Name = t1,
                        team2Name = t2,
                        matchPoints = matchPoints,
                        team1Score = currentSetScore1,
                        team2Score = currentSetScore2,
                        team1Sets = team1SetsWon,
                        team2Sets = team2SetsWon,
                        setHistory = setHistory,
                        setWinnerMessage = setWinnerMessage,
                        onDismissSetWinner = {
                            setWinnerMessage = null
                            if (team1SetsWon == 2 || team2SetsWon == 2) {
                                winnerName = if (team1SetsWon == 2) t1 else t2
                                currentScreen = Screen.Winner
                            } else {
                                currentSetScore1 = 0
                                currentSetScore2 = 0
                            }
                        },
                        onScoreChange = { s1, s2 ->
                            if (setWinnerMessage != null) return@ScoreScreen

                            currentSetScore1 = s1
                            currentSetScore2 = s2

                            val maxPoints = matchPoints + 9
                            val isSetOver = when {
                                (s1 >= matchPoints || s2 >= matchPoints) && abs(s1 - s2) >= 2 -> true
                                s1 == maxPoints || s2 == maxPoints -> true
                                else -> false
                            }

                            if (isSetOver) {
                                setHistory.add(SetResult(s1, s2))
                                val setWinner = if (s1 > s2) t1 else t2
                                if (s1 > s2) team1SetsWon++ else team2SetsWon++
                                setWinnerMessage = "Set winner is -> $setWinner"
                            }
                        },
                        onReset = {
                            MatchStorage.clear(context)
                            currentScreen = Screen.Setup
                        }
                    )
                }
            }
            Screen.Winner -> {
                val t1 = if (team1Name.length == 0) "T1" else team1Name
                val t2 = if (team2Name.length == 0) "T2" else team2Name

                if (useWatchUi) {
                    WatchWinnerScreen(
                        winnerName = winnerName,
                        setHistory = setHistory,
                        onNewMatch = {
                            MatchStorage.clear(context)
                            currentScreen = Screen.Setup
                        }
                    )
                } else {
                    WinnerScreen(
                        winnerName = winnerName,
                        setHistory = setHistory,
                        team1Name = t1,
                        team2Name = t2,
                        onNewMatch = {
                            MatchStorage.clear(context)
                            currentScreen = Screen.Setup
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1200)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.app_logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SMASH",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )
            Text(
                text = "SCORE",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp
            )
        }

        Text(
            text = "Developed By Abhishek Gupta",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    team1Name: String,
    onTeam1NameChange: (String) -> Unit,
    team2Name: String,
    onTeam2NameChange: (String) -> Unit,
    matchPoints: Int,
    onMatchPointsChange: (Int) -> Unit,
    onStartMatch: () -> Unit,
    onToggleWatchMode: () -> Unit
) {
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            containerColor = Color(0xFF1A1A2E),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.8f),
            icon = { Text("🏸", fontSize = 40.sp) },
            title = {
                Text(
                    "About Smash Score",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .fillMaxWidth()
                ) {
                    Text(
                        "Welcome to Smash Score! Designed for Phone & Galaxy Watch.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Scoring Rules:", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    Text("• Points: Tap your side to score. Long-press to undo.")
                    Text("• Set Winner: Reach target points (11, 15, or 21) to win set.")
                    Text("• Deuce: Tied games require a 2 point lead.")
                    Text("• Sudden Death: Max score is Target + 9.")
                }
            },
            confirmButton = {
                Button(
                    onClick = { showInfoDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Got it!")
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text(
                text = "🏸 Badminton Score Counter",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        CurrentTimeDisplay(fontSize = 14.sp, color = Color(0xFF4CAF50), modifier = Modifier.padding(bottom = 16.dp))

        OutlinedTextField(
            value = team1Name,
            onValueChange = onTeam1NameChange,
            label = { Text("Team 1 Name") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF4CAF50),
                unfocusedBorderColor = Color.White,
                focusedLabelColor = Color(0xFF4CAF50),
                unfocusedLabelColor = Color.White
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = team2Name,
            onValueChange = onTeam2NameChange,
            label = { Text("Team 2 Name") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF4CAF50),
                unfocusedBorderColor = Color.White,
                focusedLabelColor = Color(0xFF4CAF50),
                unfocusedLabelColor = Color.White
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Match Points",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val options = listOf(11 to "Quick", 15 to "Medium", 21 to "Official ⭐")
            options.forEach { (points, label) ->
                MatchPointChip(
                    points = points,
                    label = label,
                    isSelected = matchPoints == points,
                    onClick = { onMatchPointsChange(points) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onStartMatch,
            enabled = team1Name.isNotBlank() && team2Name.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50),
                disabledContainerColor = Color.Gray
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Start Match", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Learn about app",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF4CAF50),
                modifier = Modifier
                    .clickable { showInfoDialog = true }
                    .padding(4.dp)
            )

            TextButton(
                onClick = onToggleWatchMode
            ) {
                Text("⌚ Watch Mode", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MatchPointChip(points: Int, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) Color(0xFF4CAF50) else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) null else BorderStroke(1.dp, Color.White),
        modifier = Modifier
            .width(100.dp)
            .height(56.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Text(
                text = points.toString(),
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 18.sp
            )
            Text(
                text = label,
                color = Color.White,
                fontSize = 10.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreScreen(
    team1Name: String,
    team2Name: String,
    matchPoints: Int,
    team1Score: Int,
    team2Score: Int,
    team1Sets: Int,
    team2Sets: Int,
    setHistory: List<SetResult>,
    setWinnerMessage: String?,
    onDismissSetWinner: () -> Unit,
    onScoreChange: (Int, Int) -> Unit,
    onReset: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    if (setWinnerMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissSetWinner,
            icon = { Text("🏸", fontSize = 40.sp) },
            title = {
                Text(
                    text = "Set Complete!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = setWinnerMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onDismissSetWinner,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Continue", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF1A1A2E),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Match?") },
            text = { Text("This will erase all current scores and go back to setup.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetDialog = false
                    onReset()
                }) {
                    Text("Reset", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Playing to $matchPoints", color = Color.White, fontSize = 15.sp)
                        CurrentTimeDisplay(fontSize = 12.sp, color = Color(0xFF4CAF50))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White)
                    }
                }
            )
        },
        containerColor = Color(0x0A, 0x0A, 0x1A)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SetDots(count = team1Sets, color = Color(0xFF1B5E20))
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = setHistory.joinToString(" | ") { "${it.team1Score}-${it.team2Score}" },
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(16.dp))
                SetDots(count = team2Sets, color = Color(0xFF0D47A1))
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                ScorePanel(
                    name = team1Name,
                    score = team1Score,
                    backgroundColor = Color(0xFF1B5E20),
                    modifier = Modifier.weight(1f),
                    onTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        triggerWristVibration(context)
                        onScoreChange(team1Score + 1, team2Score)
                    },
                    onLongPress = {
                        if (team1Score > 0) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            triggerWristVibration(context)
                            onScoreChange(team1Score - 1, team2Score)
                        }
                    }
                )
                ScorePanel(
                    name = team2Name,
                    score = team2Score,
                    backgroundColor = Color(0xFF0D47A1),
                    modifier = Modifier.weight(1f),
                    onTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        triggerWristVibration(context)
                        onScoreChange(team1Score, team2Score + 1)
                    },
                    onLongPress = {
                        if (team2Score > 0) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            triggerWristVibration(context)
                            onScoreChange(team1Score, team2Score - 1)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ScorePanel(
    name: String,
    score: Int,
    backgroundColor: Color,
    modifier: Modifier,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    var flashTrigger by remember { mutableIntStateOf(0) }
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)

    LaunchedEffect(score) {
        if (score > 0) {
            flashTrigger++
        }
    }

    val flashAlpha by animateFloatAsState(
        targetValue = if (flashTrigger % 2 == 1) 1f else 0.8f,
        animationSpec = tween(150),
        label = "flash"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(8.dp)
            .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = backgroundColor, spotColor = backgroundColor)
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor.copy(alpha = flashAlpha))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { currentOnTap() },
                    onLongPress = { currentOnLongPress() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = name,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = score.toString(),
                color = Color.White,
                fontSize = 90.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "TAP TO SCORE\nHOLD TO UNDO",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun SetDots(count: Int, color: Color) {
    Row {
        repeat(2) { index ->
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (index < count) color else Color.DarkGray)
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            )
            if (index == 0) Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

@Composable
fun WinnerScreen(
    winnerName: String,
    setHistory: List<SetResult>,
    team1Name: String,
    team2Name: String,
    onNewMatch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🏆", fontSize = 80.sp)
        Text(
            text = "WINNER!",
            style = MaterialTheme.typography.headlineLarge,
            color = Color(0xFF4CAF50),
            fontWeight = FontWeight.Black
        )
        Text(
            text = winnerName,
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Match History", color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                setHistory.forEachIndexed { index, result ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Set ${index + 1}", color = Color.White)
                        Text(
                            "${result.team1Score} - ${result.team2Score}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Button(
            onClick = onNewMatch,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("New Match", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/* =========================================================================
   WEAR OS OPTIMIZED SCREENS (SAMSUNG GALAXY WATCH / ROUND SCREEN FRIENDLY)
   ========================================================================= */

@Composable
fun WatchSetupScreen(
    team1Name: String,
    onTeam1NameChange: (String) -> Unit,
    team2Name: String,
    onTeam2NameChange: (String) -> Unit,
    matchPoints: Int,
    onMatchPointsChange: (Int) -> Unit,
    onStartMatch: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            CurrentTimeDisplay(fontSize = 12.sp, color = Color(0xFF4CAF50), modifier = Modifier.padding(bottom = 2.dp))

            Text(
                text = "🏸 SMASH SCORE",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Team Toggle / Labels
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = Color(0xFF1B5E20),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clickable {
                            onTeam1NameChange(if (team1Name == "T1") "Player 1" else "T1")
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(team1Name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Text("VS", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                Surface(
                    color = Color(0xFF0D47A1),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clickable {
                            onTeam2NameChange(if (team2Name == "T2") "Player 2" else "T2")
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(team2Name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Points Chips for Watch
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(11, 15, 21).forEach { pts ->
                    val selected = matchPoints == pts
                    Surface(
                        onClick = { onMatchPointsChange(pts) },
                        color = if (selected) Color(0xFF4CAF50) else Color(0xFF222233),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(width = 46.dp, height = 32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$pts",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onStartMatch,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(38.dp)
            ) {
                Text("START MATCH", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun WatchScoreScreen(
    team1Name: String,
    team2Name: String,
    matchPoints: Int,
    team1Score: Int,
    team2Score: Int,
    team1Sets: Int,
    team2Sets: Int,
    setHistory: List<SetResult>,
    setWinnerMessage: String?,
    onDismissSetWinner: () -> Unit,
    onScoreChange: (Int, Int) -> Unit,
    onReset: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    if (setWinnerMessage != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .clickable { onDismissSetWinner() }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎉", fontSize = 36.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = setWinnerMessage,
                    color = Color(0xFF4CAF50),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDismissSetWinner,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("CONTINUE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Split Tap Area for Watch: Left Half and Right Half
        Row(modifier = Modifier.fillMaxSize()) {
            // Team 1 (Left Half - Green)
            WatchScorePanel(
                name = team1Name,
                score = team1Score,
                sets = team1Sets,
                backgroundColor = Color(0xFF1B5E20),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onTap = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    triggerWristVibration(context)
                    onScoreChange(team1Score + 1, team2Score)
                },
                onLongPress = {
                    if (team1Score > 0) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        triggerWristVibration(context)
                        onScoreChange(team1Score - 1, team2Score)
                    }
                }
            )

            // Team 2 (Right Half - Blue)
            WatchScorePanel(
                name = team2Name,
                score = team2Score,
                sets = team2Sets,
                backgroundColor = Color(0xFF0D47A1),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onTap = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    triggerWristVibration(context)
                    onScoreChange(team1Score, team2Score + 1)
                },
                onLongPress = {
                    if (team2Score > 0) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        triggerWristVibration(context)
                        onScoreChange(team1Score, team2Score - 1)
                    }
                }
            )
        }

        // Top Overlay: Centered Live Clock & Target Pill for Watch
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 10.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CurrentTimeDisplay(fontSize = 12.sp, color = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "• T:$matchPoints",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Reset button at top right with safe round-screen margin
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 18.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
                    .clickable { onReset() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Bottom Overlay: Set History summary
        if (setHistory.size > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = setHistory.joinToString(" | ") { "${it.team1Score}-${it.team2Score}" },
                    color = Color.Yellow,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun WatchScorePanel(
    name: String,
    score: Int,
    sets: Int,
    backgroundColor: Color,
    modifier: Modifier,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    var flashTrigger by remember { mutableIntStateOf(0) }
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)

    LaunchedEffect(score) {
        if (score > 0) flashTrigger++
    }

    val flashAlpha by animateFloatAsState(
        targetValue = if (flashTrigger % 2 == 1) 1f else 0.85f,
        animationSpec = tween(120),
        label = "watchFlash"
    )

    Box(
        modifier = modifier
            .background(backgroundColor.copy(alpha = flashAlpha))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { currentOnTap() },
                    onLongPress = { currentOnLongPress() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                if (sets > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "($sets)",
                        color = Color.Yellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Text(
                text = score.toString(),
                color = Color.White,
                fontSize = 52.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = "TAP +1",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun WatchWinnerScreen(
    winnerName: String,
    setHistory: List<SetResult>,
    onNewMatch: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CurrentTimeDisplay(fontSize = 12.sp, color = Color(0xFF4CAF50), modifier = Modifier.padding(bottom = 6.dp))

            Text("🏆 WINNER!", color = Color(0xFF4CAF50), fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text(winnerName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(6.dp))

            setHistory.forEachIndexed { idx, res ->
                Text(
                    "Set ${idx + 1}: ${res.team1Score} - ${res.team2Score}",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onNewMatch,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("NEW MATCH", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
