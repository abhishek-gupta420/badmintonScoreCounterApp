package com.abhi.badmintonscoreconunter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs

enum class Screen {
    Splash, Setup, Score, Winner
}

data class SetResult(val team1Score: Int, val team2Score: Int)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BadmintonScoreCounterApp()
        }
    }
}

@Composable
fun BadmintonScoreCounterApp() {
    var currentScreen by remember { mutableStateOf(Screen.Splash) }
    var team1Name by remember { mutableStateOf("") }
    var team2Name by remember { mutableStateOf("") }
    var matchPoints by remember { mutableIntStateOf(21) }
    
    var team1SetsWon by remember { mutableIntStateOf(0) }
    var team2SetsWon by remember { mutableIntStateOf(0) }
    var currentSetScore1 by remember { mutableIntStateOf(0) }
    var currentSetScore2 by remember { mutableIntStateOf(0) }
    val setHistory = remember { mutableStateListOf<SetResult>() }
    var winnerName by remember { mutableStateOf("") }
    var setWinnerMessage by remember { mutableStateOf<String?>(null) }

    val darkBackground = Color(0x0A, 0x0A, 0x1A)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = darkBackground
    ) {
        when (currentScreen) {
            Screen.Splash -> SplashScreen(onTimeout = { currentScreen = Screen.Setup })
            Screen.Setup -> SetupScreen(
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
                }
            )
            Screen.Score -> ScoreScreen(
                team1Name = team1Name,
                team2Name = team2Name,
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
                        winnerName = if (team1SetsWon == 2) team1Name else team2Name
                        currentScreen = Screen.Winner
                    } else {
                        currentSetScore1 = 0
                        currentSetScore2 = 0
                    }
                },
                onScoreChange = { t1, t2 ->
                    if (setWinnerMessage != null) return@ScoreScreen
                    
                    currentSetScore1 = t1
                    currentSetScore2 = t2
                    
                    val maxPoints = matchPoints + 9
                    val isSetOver = when {
                        (t1 >= matchPoints || t2 >= matchPoints) && abs(t1 - t2) >= 2 -> true
                        t1 == maxPoints || t2 == maxPoints -> true
                        else -> false
                    }
                    
                    if (isSetOver) {
                        setHistory.add(SetResult(t1, t2))
                        val setWinner = if (t1 > t2) team1Name else team2Name
                        if (t1 > t2) team1SetsWon++ else team2SetsWon++
                        setWinnerMessage = "Set winner is -> $setWinner"
                    }
                },
                onReset = {
                    currentScreen = Screen.Setup
                }
            )
            Screen.Winner -> WinnerScreen(
                winnerName = winnerName,
                setHistory = setHistory,
                team1Name = team1Name,
                team2Name = team2Name,
                onNewMatch = {
                    currentScreen = Screen.Setup
                }
            )
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1000)
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
                    .size(280.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(24.dp))
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
                .padding(bottom = 48.dp),
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
    onStartMatch: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🏸 Badminton Score Counter",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = team1Name,
            onValueChange = onTeam1NameChange,
            label = { Text("Team 1 Name") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
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

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Match Points",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.Start)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val options = listOf(11 to "Quick Match", 15 to "Medium Match", 21 to "Official ⭐")
            options.forEach { (points, label) ->
                MatchPointChip(
                    points = points,
                    label = label,
                    isSelected = matchPoints == points,
                    onClick = { onMatchPointsChange(points) }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

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
            .height(60.dp)
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
                text = label.split(" ")[0],
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
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
                title = { Text("Playing to $matchPoints", color = Color.White, fontSize = 16.sp) },
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
            // Sets indicator
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
                        onScoreChange(team1Score + 1, team2Score)
                    },
                    onLongPress = {
                        if (team1Score > 0) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                        onScoreChange(team1Score, team2Score + 1)
                    },
                    onLongPress = {
                        if (team2Score > 0) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
