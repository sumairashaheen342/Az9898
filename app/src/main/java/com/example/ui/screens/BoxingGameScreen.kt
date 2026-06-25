package com.example.ui.screens

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Game Design Palette Tokens
val GameDarkCanvas = Color(0xFF0E0E12)
val GameCardSlate = Color(0xFF16161F)
val NeonPink = Color(0xFFFF1493)
val NeonCyan = Color(0xFF00FFFF)
val LaserGold = Color(0xFFFFD700)
val CyberGreen = Color(0xFF39FF14)
val ShieldBlue = Color(0xFF1E90FF)

enum class GameMode {
    LOBBY,
    FIGHT,
    SPEED_BAG,
    WIN,
    LOSE,
    SPEED_BAG_OVER
}

enum class PlayerState {
    IDLE,
    PUNCHING_LEFT,
    PUNCHING_RIGHT,
    BLOCKING,
    DODGING
}

enum class OpponentState {
    IDLE,
    WINDING_UP,
    PUNCHING,
    BLOCKING,
    STUNNED
}

enum class OpponentTier {
    GLASS_JOE,
    ROBO_BASHER,
    IRON_GIANT,
    COSMIC_KING
}

/**
 * Lightweight retroactive ToneGenerator Sound Player to prevent freezes/blocking.
 */
object GameAudio {
    private var toneGen: ToneGenerator? = null

    init {
        try {
            toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 65)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playTone(toneType: Int, duration: Int) {
        try {
            toneGen?.startTone(toneType, duration)
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    fun playLightHit() {
        playTone(ToneGenerator.TONE_PROP_ACK, 70)
    }

    fun playHeavyHit() {
        playTone(ToneGenerator.TONE_DTMF_D, 150)
    }

    fun playDodge() {
        playTone(ToneGenerator.TONE_CDMA_SIGNAL_OFF, 100)
    }

    fun playBlock() {
        playTone(ToneGenerator.TONE_CDMA_PIP, 50)
    }

    fun playWarning() {
        playTone(ToneGenerator.TONE_SUP_DIAL, 150)
    }

    fun playSpecialCharge() {
        playTone(ToneGenerator.TONE_PROP_PROMPT, 200)
    }

    fun playWinAsync(scope: kotlinx.coroutines.CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            try {
                playTone(ToneGenerator.TONE_PROP_BEEP, 80)
                delay(120)
                playTone(ToneGenerator.TONE_PROP_BEEP2, 100)
                delay(120)
                playTone(ToneGenerator.TONE_SUP_RADIO_ACK, 250)
            } catch (e: Exception) {}
        }
    }

    fun playLoseAsync(scope: kotlinx.coroutines.CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            try {
                playTone(ToneGenerator.TONE_CDMA_PIP, 200)
                delay(150)
                playTone(ToneGenerator.TONE_CDMA_PIP, 300)
            } catch (e: Exception) {}
        }
    }
}

@Composable
fun BoxingGameScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Persistent Statistics via SharedPreferences
    val prefs = remember { context.getSharedPreferences("boxing_game_stats", Context.MODE_PRIVATE) }
    var persistentWins by remember { mutableStateOf(prefs.getInt("wins", 0)) }
    var persistentLosses by remember { mutableStateOf(prefs.getInt("losses", 0)) }
    var speedBagHighScore by remember { mutableStateOf(prefs.getInt("speed_bag_highscore", 0)) }

    // Core Screen Game Modes
    var gameMode by remember { mutableStateOf(GameMode.LOBBY) }

    // Opponent Tier Choice
    var selectedOpponentTier by remember { mutableStateOf(OpponentTier.GLASS_JOE) }

    // Opponent attributes based on selection
    val opponentName = when (selectedOpponentTier) {
        OpponentTier.GLASS_JOE -> "Glass Joe 👤"
        OpponentTier.ROBO_BASHER -> "Robo-Basher 3000 🤖"
        OpponentTier.IRON_GIANT -> "Iron Titan 👹"
        OpponentTier.COSMIC_KING -> "Giga Champion 👑"
    }

    val opponentAvatar = when (selectedOpponentTier) {
        OpponentTier.GLASS_JOE -> "👤"
        OpponentTier.ROBO_BASHER -> "🤖"
        OpponentTier.IRON_GIANT -> "👹"
        OpponentTier.COSMIC_KING -> "👑"
    }

    // --- GAME ENGINE STATE variables ---
    var playerHealth by remember { mutableIntStateOf(100) }
    var playerStamina by remember { mutableIntStateOf(100) }
    var playerSpecialMeter by remember { mutableIntStateOf(0) } // Charge from 0 to 100%

    var opponentHealth by remember { mutableIntStateOf(100) }
    var opponentStamina by remember { mutableIntStateOf(100) }

    var playerState by remember { mutableStateOf(PlayerState.IDLE) }
    var opponentState by remember { mutableStateOf(OpponentState.IDLE) }

    // Visual indicators
    var showImpactSplash by remember { mutableStateOf(false) }
    var impactText by remember { mutableStateOf("") }
    var lastPunchByPlayer by remember { mutableStateOf(true) } // To align glove anims

    var combatLogs by remember { mutableStateOf(listOf("🥊 Ready to Rumble! Step into the ring...")) }
    var currentFightId by remember { mutableIntStateOf(0) }

    // Speed Bag Game attributes
    var speedBagScore by remember { mutableIntStateOf(0) }
    var speedBagTimeLeft by remember { mutableIntStateOf(10) }
    var speedBagStreak by remember { mutableIntStateOf(0) }
    var speedBagActiveId by remember { mutableIntStateOf(0) }

    // For rhythmic Speed Bag "Sweet Spot" needle
    val needleTransition = rememberInfiniteTransition(label = "needle_loop")
    val needlePosition by needleTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "needle"
    )

    fun addLog(msg: String) {
        val currentLogs = combatLogs.toMutableList()
        currentLogs.add(0, msg)
        combatLogs = currentLogs.take(25) // Keep last 25 logs
    }

    // --- COROUTINE GAME LOOP FOR ACTIVE FIGHTS ---
    LaunchedEffect(gameMode, selectedOpponentTier, currentFightId) {
        if (gameMode == GameMode.FIGHT) {
            playerHealth = 100
            playerStamina = 100
            playerSpecialMeter = 0
            opponentHealth = 100
            opponentStamina = 100
            playerState = PlayerState.IDLE
            opponentState = OpponentState.IDLE
            combatLogs = listOf("🔔 ROUND 1! Opponent ${opponentName} steps up!")

            while (playerHealth > 0 && opponentHealth > 0 && gameMode == GameMode.FIGHT) {
                // Determine AI punch frequency based on tier
                val actionInterval = when (selectedOpponentTier) {
                    OpponentTier.GLASS_JOE -> (2200..3200).random()
                    OpponentTier.ROBO_BASHER -> (1500..2300).random()
                    OpponentTier.IRON_GIANT -> (1100..1800).random()
                    OpponentTier.COSMIC_KING -> (750..1200).random()
                }

                delay(actionInterval.toLong())
                if (gameMode != GameMode.FIGHT) break

                // Opponent rolls dynamic combat decision
                val dice = (1..100).random()
                when {
                    // 1. DANGEROUS SUPER ATTACK (Telegraphed Wind-up!)
                    dice <= 40 -> {
                        opponentState = OpponentState.WINDING_UP
                        addLog("⚠️ Opponent ${opponentName} winds up a HEAVY punches! BLOCK or DODGE!")
                        GameAudio.playWarning()

                        // Window given to react
                        val reactWindow = when (selectedOpponentTier) {
                            OpponentTier.GLASS_JOE -> 1100L
                            OpponentTier.ROBO_BASHER -> 800L
                            OpponentTier.IRON_GIANT -> 520L
                            OpponentTier.COSMIC_KING -> 390L
                        }

                        delay(reactWindow)
                        if (gameMode != GameMode.FIGHT) break

                        // Assess hit outcome
                        if (playerState == PlayerState.DODGING) {
                            opponentState = OpponentState.STUNNED
                            addLog("✨ EXQUISITE DODGE! ${opponentName} is STUNNED and off-balance!")
                            GameAudio.playDodge()
                            impactText = "DODGED! STUNNED! 💫"
                            showImpactSplash = true
                            delay(400)
                            showImpactSplash = false
                            
                            // Let the stun linger a little bit
                            delay(2000)
                            if (opponentState == OpponentState.STUNNED) {
                                opponentState = OpponentState.IDLE
                            }
                        } else if (playerState == PlayerState.BLOCKING) {
                            val blockReduction = (4..8).random()
                            playerHealth = maxOf(0, playerHealth - blockReduction)
                            opponentState = OpponentState.PUNCHING
                            addLog("🛡️ Shield Blocked! Reduced heavy hit damage to $blockReduction HP.")
                            GameAudio.playBlock()
                            impactText = "BLOCKED 🛡️"
                            showImpactSplash = true
                            delay(300)
                            showImpactSplash = false
                            opponentState = OpponentState.IDLE
                        } else {
                            // Unprotected devastating impact!
                            val baseDmg = when (selectedOpponentTier) {
                                OpponentTier.GLASS_JOE -> (18..24).random()
                                OpponentTier.ROBO_BASHER -> (25..32).random()
                                OpponentTier.IRON_GIANT -> (33..44).random()
                                OpponentTier.COSMIC_KING -> (45..58).random()
                            }
                            playerHealth = maxOf(0, playerHealth - baseDmg)
                            opponentState = OpponentState.PUNCHING
                            addLog("💥 BAM! ${opponentName} landed a CRITICAL punch! -$baseDmg HP!")
                            GameAudio.playHeavyHit()
                            impactText = "CRITICAL BLAST! 💥"
                            showImpactSplash = true
                            delay(500)
                            showImpactSplash = false
                            opponentState = OpponentState.IDLE

                            if (playerHealth <= 0) {
                                gameMode = GameMode.LOSE
                                GameAudio.playLoseAsync(scope)
                                persistentLosses++
                                prefs.edit().putInt("losses", persistentLosses).apply()
                            }
                        }
                    }

                    // 2. SHIELD BLOCK
                    dice <= 65 -> {
                        opponentState = OpponentState.BLOCKING
                        addLog("🛡️ ${opponentName} holds up defensive gloves.")
                        delay(1300)
                        if (opponentState == OpponentState.BLOCKING) {
                            opponentState = OpponentState.IDLE
                        }
                    }

                    // 3. RAPID STANDARD JAB
                    else -> {
                        opponentState = OpponentState.PUNCHING
                        if (playerState == PlayerState.DODGING) {
                            addLog("💨 You dodged a rapid jab gracefully!")
                            impactText = "DODGED! 💨"
                        } else if (playerState == PlayerState.BLOCKING) {
                            val blockedDmg = (1..3).random()
                            playerHealth = maxOf(0, playerHealth - blockedDmg)
                            addLog("🛡️ standard jab blocked (-$blockedDmg HP).")
                            GameAudio.playBlock()
                            impactText = "BLOCKED"
                        } else {
                            val normalDmg = when (selectedOpponentTier) {
                                OpponentTier.GLASS_JOE -> (6..11).random()
                                OpponentTier.ROBO_BASHER -> (11..16).random()
                                OpponentTier.IRON_GIANT -> (15..22).random()
                                OpponentTier.COSMIC_KING -> (20..28).random()
                            }
                            playerHealth = maxOf(0, playerHealth - normalDmg)
                            addLog("🥊 ${opponentName} lands a crisp jab! Took $normalDmg damage.")
                            GameAudio.playLightHit()
                            impactText = "OUCH! 🥊"

                            if (playerHealth <= 0) {
                                gameMode = GameMode.LOSE
                                GameAudio.playLoseAsync(scope)
                                persistentLosses++
                                prefs.edit().putInt("losses", persistentLosses).apply()
                            }
                        }
                        showImpactSplash = true
                        delay(250)
                        showImpactSplash = false
                        opponentState = OpponentState.IDLE
                    }
                }
            }
        }
    }

    // --- STAMINA AUTO-RECOVERY TIMER LOOP ---
    LaunchedEffect(gameMode) {
        if (gameMode == GameMode.FIGHT) {
            while (gameMode == GameMode.FIGHT) {
                delay(400)
                if (playerState == PlayerState.IDLE) {
                    playerStamina = minOf(100, playerStamina + 8)
                }
                if (opponentState == OpponentState.IDLE) {
                    opponentStamina = minOf(100, opponentStamina + 6)
                }
            }
        }
    }

    // --- SPEED BAG TIMELINE CLOCK LOOP ---
    LaunchedEffect(gameMode, speedBagActiveId) {
        if (gameMode == GameMode.SPEED_BAG) {
            speedBagScore = 0
            speedBagTimeLeft = 10
            speedBagStreak = 0
            while (speedBagTimeLeft > 0 && gameMode == GameMode.SPEED_BAG) {
                delay(1000)
                speedBagTimeLeft--
                if (speedBagTimeLeft == 0) {
                    gameMode = GameMode.SPEED_BAG_OVER
                    if (speedBagScore > speedBagHighScore) {
                        speedBagHighScore = speedBagScore
                        prefs.edit().putInt("speed_bag_highscore", speedBagHighScore).apply()
                    }
                }
            }
        }
    }

    // Player action wrappers
    fun executePlayerPunch(isLeft: Boolean) {
        if (gameMode != GameMode.FIGHT || playerHealth <= 0 || opponentHealth <= 0) return

        val cost = if (isLeft) 12 else 24
        if (playerStamina < cost) {
            addLog("⚠️ Out of Stamina! Rest to recover your breath.")
            return
        }

        playerStamina -= cost
        playerState = if (isLeft) PlayerState.PUNCHING_LEFT else PlayerState.PUNCHING_RIGHT
        lastPunchByPlayer = isLeft

        scope.launch {
            delay(150) // Punch duration animation window

            // Assess damage outcome
            if (opponentState == OpponentState.BLOCKING) {
                val baseDmg = if (isLeft) 1 else 3
                opponentHealth = maxOf(0, opponentHealth - baseDmg)
                addLog("🛡️ Opponent blocked! Dealt minor scratch ($baseDmg damage).")
                GameAudio.playBlock()
                impactText = "BLOCKED 🛡️"
            } else {
                val isStunned = opponentState == OpponentState.STUNNED
                var baseDmg = if (isLeft) (6..10).random() else (14..22).random()
                if (isStunned) {
                    baseDmg = (baseDmg * 2.0).toInt()
                    addLog("⚡ STUN DAMAGE OVERLOAD! Landed heavy blows!")
                }

                opponentHealth = maxOf(0, opponentHealth - baseDmg)
                addLog("🥊 Landed a crisp ${if (isLeft) "Left Jab" else "Right Hook"}! Dealt $baseDmg damage!")
                GameAudio.playLightHit()
                impactText = if (isStunned) "CRIT $baseDmg! 💥" else "-$baseDmg 🥊"

                // Charge Special Meter
                val chargeGained = if (isLeft) 10 else 18
                playerSpecialMeter = minOf(100, playerSpecialMeter + chargeGained)
                if (playerSpecialMeter == 100) {
                    GameAudio.playSpecialCharge()
                }
            }

            showImpactSplash = true
            delay(200)
            showImpactSplash = false
            playerState = PlayerState.IDLE

            // Check win criteria
            if (opponentHealth <= 0) {
                gameMode = GameMode.WIN
                GameAudio.playWinAsync(scope)
                persistentWins++
                prefs.edit().putInt("wins", persistentWins).apply()
            }
        }
    }

    fun executePlayerDodge() {
        if (gameMode != GameMode.FIGHT || playerStamina < 15) return
        playerStamina -= 15
        playerState = PlayerState.DODGING
        addLog("🛡️ Dodging left-right! Ready to counter...")
        GameAudio.playDodge()
        scope.launch {
            delay(400) // Dodge invincibility window
            if (playerState == PlayerState.DODGING) {
                playerState = PlayerState.IDLE
            }
        }
    }

    fun executePlayerBlock() {
        if (gameMode != GameMode.FIGHT) return
        playerState = PlayerState.BLOCKING
        addLog("🛡️ Raised defensive high guard!")
        GameAudio.playBlock()
        scope.launch {
            delay(500)
            if (playerState == PlayerState.BLOCKING) {
                playerState = PlayerState.IDLE
            }
        }
    }

    fun executePlayerMegaPunch() {
        if (gameMode != GameMode.FIGHT || playerSpecialMeter < 100) return
        playerSpecialMeter = 0
        playerState = PlayerState.PUNCHING_RIGHT

        scope.launch {
            addLog("🔥 EXECUTING TITAN MEGA PUNCH! Bypasses all guards!")
            delay(250)
            val specialDmg = (35..48).random()
            opponentHealth = maxOf(0, opponentHealth - specialDmg)
            addLog("💥 KABOOM! Landed the Special Mega Punch! Dealt $specialDmg MASSIVE damage!")
            GameAudio.playHeavyHit()
            
            impactText = "MEGA BLAST $specialDmg! 💥🔥"
            showImpactSplash = true
            delay(500)
            showImpactSplash = false
            playerState = PlayerState.IDLE

            if (opponentHealth <= 0) {
                gameMode = GameMode.WIN
                GameAudio.playWinAsync(scope)
                persistentWins++
                prefs.edit().putInt("wins", persistentWins).apply()
            }
        }
    }

    // Speed Bag interaction tap
    fun hitSpeedBag() {
        if (gameMode != GameMode.SPEED_BAG) return

        // Sweet Spot needle is between 0 and 100. Let's make sweet spot 40..60
        val isPerfect = needlePosition in 40f..60f
        if (isPerfect) {
            speedBagStreak++
            val scoreAdded = 3 + (speedBagStreak / 3) // Streak multiplier
            speedBagScore += scoreAdded
            GameAudio.playHeavyHit()
            impactText = "PERFECT SHOT! +$scoreAdded 🔥"
        } else {
            speedBagStreak = 0
            speedBagScore += 1
            GameAudio.playLightHit()
            impactText = "HIT! +1 🥊"
        }

        scope.launch {
            showImpactSplash = true
            delay(150)
            showImpactSplash = false
        }
    }

    // Screen views
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(GameDarkCanvas)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (gameMode) {
            // ================== LOBBY SCREEN ==================
            GameMode.LOBBY -> {
                // Header Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GameCardSlate),
                    border = BorderStroke(1.dp, Brush.linearGradient(listOf(NeonPink, NeonCyan)))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "RETRO NEON BOXING 🥊",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Glow Arena reflex championships & training grounds",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Persistence Stats Dashboard Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = GameCardSlate),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("PRO RECORD 🏆", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$persistentWins",
                                    color = CyberGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                                Text("W", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 2.dp))
                                Text(
                                    text = " - $persistentLosses",
                                    color = NeonPink,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                                Text("L", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(start = 2.dp))
                            }
                            Text(
                                text = "Win Rate: ${if (persistentWins + persistentLosses == 0) "0%" else "${(persistentWins * 100) / (persistentWins + persistentLosses)}%"}",
                                color = Color.LightGray,
                                fontSize = 9.sp
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = GameCardSlate),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("SPEED BAG RECS ⏱️", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$speedBagHighScore",
                                color = LaserGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Text(
                                text = "Top Active Streak Score",
                                color = Color.LightGray,
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Arena Tier Chooser Title
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1. Active Championship Matches",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Select Tier",
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tier Selection Column
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tiers = listOf(
                        Triple(OpponentTier.GLASS_JOE, "Glass Joe", "Slow punches. Perfect for amateur training."),
                        Triple(OpponentTier.ROBO_BASHER, "Robo-Basher 3000", "Standard reaction speeds, shields punches."),
                        Triple(OpponentTier.IRON_GIANT, "Iron Titan", "Rapid hard attack cycles. High armor ratings."),
                        Triple(OpponentTier.COSMIC_KING, "Giga Champion", "Ultimate challenge! Hyper fast reflex required.")
                    )

                    tiers.forEach { (tier, name, desc) ->
                        val isSelected = selectedOpponentTier == tier
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedOpponentTier = tier }
                                .testTag("difficulty_${name.lowercase().replace(" ", "_")}_btn"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) NeonPink.copy(alpha = 0.15f) else GameCardSlate
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) NeonPink else Color.White.copy(alpha = 0.05f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) NeonPink else Color.DarkGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when(tier) {
                                            OpponentTier.GLASS_JOE -> "👤"
                                            OpponentTier.ROBO_BASHER -> "🤖"
                                            OpponentTier.IRON_GIANT -> "👹"
                                            OpponentTier.COSMIC_KING -> "👑"
                                        },
                                        fontSize = 18.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        color = if (isSelected) NeonPink else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = desc,
                                        color = Color.Gray,
                                        fontSize = 9.sp,
                                        lineHeight = 12.sp
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = NeonPink,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Fight Button
                Button(
                    onClick = {
                        currentFightId++
                        gameMode = GameMode.FIGHT
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("championship_fight_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = "Combat",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ENTER RING: FIGHT $opponentName",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = Color.DarkGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                // 2. SPEED BAG SECTOR
                Text(
                    text = "2. Quick Reaction Speed Bag Training",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GameCardSlate),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⚡ SPEED BAG BLITZ", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(LaserGold.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("10s Time Trial", color = LaserGold, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text("Rec: $speedBagHighScore pts", color = Color.Gray, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap when the needle aligns in the green Sweet Spot center zone! Hit multiple perfect streaks for massive bonus score gains.",
                            color = Color.LightGray,
                            fontSize = 9.sp,
                            lineHeight = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                speedBagActiveId++
                                gameMode = GameMode.SPEED_BAG
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .testTag("speed_bag_start_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = "Blitz",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "START 10S SPEED BAG TRIAL",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // ================== ACTIVE CHAMPIONSHIP FIGHT ==================
            GameMode.FIGHT -> {
                // Fight HUD Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GameCardSlate, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Player health stats column
                    Column(modifier = Modifier.weight(1f)) {
                        Text("YOU (CHAMP) 🥊", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { playerHealth / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = CyberGreen,
                            trackColor = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("$playerHealth/100 HP", color = CyberGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text("Stamina: $playerStamina%", color = LaserGold, fontSize = 8.sp)
                        }
                    }

                    // Separation badge
                    Text(
                        text = "VS",
                        color = NeonPink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )

                    // Opponent stats column
                    Column(modifier = Modifier.weight(1f)) {
                        Text(opponentName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { opponentHealth / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = NeonPink,
                            trackColor = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("$opponentHealth/100 HP", color = NeonPink, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = when (opponentState) {
                                    OpponentState.BLOCKING -> "GUARDING"
                                    OpponentState.WINDING_UP -> "⚠️ ATTACKING!"
                                    OpponentState.STUNNED -> "💫 STUNNED!"
                                    else -> "READY"
                                },
                                color = if (opponentState == OpponentState.WINDING_UP) LaserGold else Color.LightGray,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ACTIVE BOXING RING VISUALS WITH HIGH-POLISH RENDER
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), RoundedCornerShape(16.dp))
                        .drawBehind {
                            // Drawing dynamic boxing ropes & grid floor using standard canvas colors
                            val width = size.width
                            val height = size.height

                            // Draw a beautiful perspective grid representing ring floor canvas
                            drawRect(
                                color = Color(0xFF0F0F14),
                                size = size
                            )

                            // Perspective lines
                            val startY = height * 0.4f
                            for (i in 0..10) {
                                val startX = width * (0.1f + i * 0.08f)
                                drawLine(
                                    color = Color(0xFF1B1B2A),
                                    start = Offset(startX, startY),
                                    end = Offset(width * (i * 0.1f), height),
                                    strokeWidth = 1.5f
                                )
                            }

                            // Glowing Rope boundaries
                            // Back line
                            drawLine(
                                color = NeonCyan.copy(alpha = 0.5f),
                                start = Offset(width * 0.15f, startY),
                                end = Offset(width * 0.85f, startY),
                                strokeWidth = 3f
                            )
                            // Left Rope
                            drawLine(
                                color = NeonCyan.copy(alpha = 0.5f),
                                start = Offset(width * 0.15f, startY),
                                end = Offset(0f, height * 0.9f),
                                strokeWidth = 4f
                            )
                            // Right Rope
                            drawLine(
                                color = NeonCyan.copy(alpha = 0.5f),
                                start = Offset(width * 0.85f, startY),
                                end = Offset(width, height * 0.9f),
                                strokeWidth = 4f
                            )

                            // Ring Corners
                            // Left Post
                            drawLine(
                                color = Color.White.copy(alpha = 0.3f),
                                start = Offset(width * 0.15f, startY),
                                end = Offset(width * 0.15f, startY - 20f),
                                strokeWidth = 6f
                            )
                            // Right Post
                            drawLine(
                                color = Color.White.copy(alpha = 0.3f),
                                start = Offset(width * 0.85f, startY),
                                end = Offset(width * 0.85f, startY - 20f),
                                strokeWidth = 6f
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Screen shake/Impact flash animations
                    if (showImpactSplash) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (opponentState == OpponentState.PUNCHING) NeonPink.copy(alpha = 0.15f)
                                    else CyberGreen.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = impactText,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Opponent character canvas layer
                    val opponentScale by animateFloatAsState(
                        targetValue = when (opponentState) {
                            OpponentState.PUNCHING -> 1.4f
                            OpponentState.STUNNED -> 0.95f
                            else -> 1.15f
                        },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "opponent_scale"
                    )

                    val opponentOffsetX by animateDpAsState(
                        targetValue = when (opponentState) {
                            OpponentState.STUNNED -> (-12).dp
                            else -> 0.dp
                        },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
                        label = "opponent_shake"
                    )

                    val opponentColorGlow = when (opponentState) {
                        OpponentState.WINDING_UP -> NeonPink
                        OpponentState.BLOCKING -> ShieldBlue
                        OpponentState.STUNNED -> LaserGold
                        else -> Color.Transparent
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .offset(x = opponentOffsetX)
                            .padding(bottom = 20.dp)
                    ) {
                        // Health overlay bubble
                        if (opponentState == OpponentState.STUNNED) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(LaserGold)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("STUNNED 💫", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Opponent avatar drawing
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .scale(opponentScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            opponentColorGlow.copy(alpha = 0.4f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .border(
                                    BorderStroke(
                                        if (opponentState != OpponentState.IDLE) 2.dp else 0.dp,
                                        opponentColorGlow
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = opponentAvatar,
                                fontSize = 56.sp,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = opponentName,
                            color = Color.LightGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    // Player punching gloves overlay layers (Left Glove / Right Glove)
                    val leftGloveOffset by animateDpAsState(
                        targetValue = if (playerState == PlayerState.PUNCHING_LEFT) (-130).dp else (-50).dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                        label = "left_glove"
                    )

                    val rightGloveOffset by animateDpAsState(
                        targetValue = if (playerState == PlayerState.PUNCHING_RIGHT) (-130).dp else (-50).dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                        label = "right_glove"
                    )

                    // Draw Gloves on Screen
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Left Glove
                        Text(
                            text = "🥊",
                            fontSize = 36.sp,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 50.dp)
                                .offset(y = leftGloveOffset)
                        )

                        // Right Glove
                        Text(
                            text = "🥊",
                            fontSize = 36.sp,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 50.dp)
                                .offset(y = rightGloveOffset)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Rage/Special attack charge bar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GameCardSlate)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔥 MEGA CHARGE:", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        LinearProgressIndicator(
                            progress = { playerSpecialMeter / 100f },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (playerSpecialMeter == 100) LaserGold else NeonCyan,
                            trackColor = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (playerSpecialMeter == 100) "READY!" else "$playerSpecialMeter%",
                            color = if (playerSpecialMeter == 100) LaserGold else NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // CONTROLLERS MATRIX ACTION DUAL ROW
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // LEFT JAB
                        Button(
                            onClick = { executePlayerPunch(isLeft = true) },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("left_jab_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("LEFT JAB 🥊", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // RIGHT HOOK
                        Button(
                            onClick = { executePlayerPunch(isLeft = false) },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("right_hook_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("RIGHT HOOK 🥊", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // DODGE LEFT-RIGHT
                        Button(
                            onClick = { executePlayerDodge() },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("dodge_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Filled.DirectionsRun, contentDescription = "Dodge", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("QUICK DODGE 🛡️", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // BLOCK GUARD
                        Button(
                            onClick = { executePlayerBlock() },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("block_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ShieldBlue.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Filled.Shield, contentDescription = "Block", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("RAISE GUARD 🛡️", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // MEGA ULTIMATE ATTACK BUTTON
                    AnimatedVisibility(visible = playerSpecialMeter == 100) {
                        Button(
                            onClick = { executePlayerMegaPunch() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("mega_punch_btn")
                                .border(BorderStroke(2.dp, LaserGold), RoundedCornerShape(10.dp)),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.FlashOn, contentDescription = "Mega", tint = LaserGold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("UNLEASH TITAN MEGA PUNCH 💥🔥", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Flee rings Button
                Button(
                    onClick = { gameMode = GameMode.LOBBY },
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("RETREAT TO LOBBY 🏳️", color = Color.Gray, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SCROLLING FIGHT COMBAT logs
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    colors = CardDefaults.cardColors(containerColor = GameCardSlate),
                    border = BorderStroke(0.5.dp, Color.DarkGray)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("ACTIVE FIGHT TELEMETRY FEED", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(combatLogs) { log ->
                                Text(
                                    text = log,
                                    color = if (log.contains("⚠️")) LaserGold 
                                            else if (log.contains("💥")) NeonPink 
                                            else if (log.contains("✨") || log.contains("🛡️")) NeonCyan
                                            else Color.LightGray,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ================== ACTIVE SPEED BAG TRIAL ==================
            GameMode.SPEED_BAG -> {
                // Header Blitz Indicator
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GameCardSlate),
                    border = BorderStroke(1.dp, NeonCyan)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("REFLEX SPEED BAG TIMER ⏱️", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text("Streak Factor multiplier active", color = Color.Gray, fontSize = 8.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonCyan)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("${speedBagTimeLeft}s LEFT", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SWEET ZONE SLIDER INDICATOR
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GameDarkCanvas),
                    border = BorderStroke(1.dp, Color.DarkGray)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ZONE MATCH NEEDLE (TAP AT CENTER!)",
                            color = Color.LightGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Custom drawn slider bar with Green sweet spot at center
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .drawBehind {
                                    val w = size.width
                                    val h = size.height

                                    // Outer background Slate
                                    drawRect(Color(0xFF22222E), size = size)

                                    // Draw Green Sweet Spot in Center (40% to 60%)
                                    drawRect(
                                        color = CyberGreen,
                                        topLeft = Offset(w * 0.4f, 0f),
                                        size = androidx.compose.ui.geometry.Size(w * 0.2f, h)
                                    )

                                    // Draw current moving needle
                                    val needleX = w * (needlePosition / 100f)
                                    drawLine(
                                        color = Color.White,
                                        start = Offset(needleX, -2f),
                                        end = Offset(needleX, h + 2f),
                                        strokeWidth = 4f
                                    )
                                }
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Miss Zone", color = Color.Gray, fontSize = 8.sp)
                            Text("SWEET SPOT (3X PTS) 🎯", color = CyberGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text("Miss Zone", color = Color.Gray, fontSize = 8.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Score stats card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = GameCardSlate)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("SCORE", color = Color.Gray, fontSize = 9.sp)
                            Text(
                                "$speedBagScore",
                                color = NeonCyan,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = GameCardSlate)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("ACTIVE STREAK", color = Color.Gray, fontSize = 9.sp)
                            Text(
                                "$speedBagStreak",
                                color = LaserGold,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // THE SPEED BAG GRAPHIC ZONE
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .clickable { hitSpeedBag() }
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFF8A2BE2).copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(BorderStroke(2.dp, NeonCyan), CircleShape)
                        .testTag("speed_bag_tap_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    val scaleFactor by animateFloatAsState(
                        targetValue = if (showImpactSplash) 1.25f else 1.0f,
                        animationSpec = spring(stiffness = Spring.StiffnessHigh),
                        label = "speedbag_bounce"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.scale(scaleFactor)
                    ) {
                        // Hanging connector
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(16.dp)
                                .background(Color.Gray)
                        )
                        Text(
                            text = "🎈",
                            fontSize = 72.sp
                        )
                        Text(
                            text = "TAP TO HIT! 🥊",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }

                    if (showImpactSplash) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(CyberGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = impactText,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                    .padding(6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { gameMode = GameMode.LOBBY },
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("END SESSION & RETURN TO LOBBY ↩️", color = Color.Gray, fontSize = 11.sp)
                }
            }

            // ================== MATCH SUCCESS (WIN) ==================
            GameMode.WIN -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GameCardSlate),
                    border = BorderStroke(1.dp, CyberGreen),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🏆 KNOCK OUT! VICTORY! 🏆", fontSize = 20.sp, fontWeight = FontWeight.Black, color = CyberGreen)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "👑",
                            fontSize = 72.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "You successfully defeated opponent ${opponentName} in standard championship combat rules! Your pro victory count has increased.",
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { gameMode = GameMode.LOBBY },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("win_return_lobby_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("CLAIM REWARDS & RETURN TO LOBBY 🎁", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // ================== MATCH FAILED (LOSE) ==================
            GameMode.LOSE -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GameCardSlate),
                    border = BorderStroke(1.dp, NeonPink),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("❌ KNOCKED OUT! DEFEAT ❌", fontSize = 20.sp, fontWeight = FontWeight.Black, color = NeonPink)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "💫🤕",
                            fontSize = 64.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Opponent ${opponentName} knocked you flat on the canvas! Keep an eye on telegraphed winding-up moves next time and coordinate quick dodges.",
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { gameMode = GameMode.LOBBY },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("lose_return_lobby_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("RETRAIN & TRY AGAIN 🥊", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // ================== SPEED BAG SESSION OVER ==================
            GameMode.SPEED_BAG_OVER -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GameCardSlate),
                    border = BorderStroke(1.dp, LaserGold),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("⏱️ TRAINING COMPLETE ⏱️", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LaserGold)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "⚡🎈",
                            fontSize = 64.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Session Complete! You scored:",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "$speedBagScore Points",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = LaserGold
                        )

                        if (speedBagScore >= speedBagHighScore) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CyberGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("🔥 NEW PERSONAL HIGH SCORE! 🔥", color = CyberGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text("Personal Record: $speedBagHighScore pts", color = Color.Gray, fontSize = 10.sp)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { gameMode = GameMode.LOBBY },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("speed_bag_return_lobby_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = LaserGold),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("SAVE RECORDS & RETURN TO LOBBY ↩️", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}


