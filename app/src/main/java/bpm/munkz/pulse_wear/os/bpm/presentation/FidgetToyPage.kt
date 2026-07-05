package bpm.munkz.pulse_wear.os.bpm.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import bpm.munkz.pulse_wear.os.bpm.BuildConfig
import bpm.munkz.pulse_wear.os.bpm.R
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FidgetToyPage() {
    val context = LocalContext.current
    val savedSettings = remember(context) { context.loadFidgetSettings() }
    var toyIndex by remember { mutableIntStateOf(0) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    var spinVelocityDegreesPerSecond by remember { mutableFloatStateOf(0f) }
    var touchPulse by remember { mutableFloatStateOf(0f) }
    var rewardPulse by remember { mutableFloatStateOf(0f) }
    var fidgetCount by rememberSaveable { mutableIntStateOf(0) }
    var mainColorArgb by rememberSaveable { mutableIntStateOf(savedSettings.mainColorArgb) }
    var backgroundColorArgb by rememberSaveable { mutableIntStateOf(savedSettings.backgroundColorArgb) }
    var ringColorArgb by rememberSaveable { mutableIntStateOf(savedSettings.ringColorArgb) }
    var switchMask by remember { mutableIntStateOf(0) }
    var mazePosition by remember { mutableIntStateOf(0) }
    var freeButtonPositions by remember {
        mutableStateOf(
            listOf(
                Offset(-32f, -32f),
                Offset(32f, -32f),
                Offset(-32f, 32f),
                Offset(32f, 32f),
            ),
        )
    }
    var whackButtonPositions by remember {
        mutableStateOf(listOf(0, 5, 10, 15))
    }
    var squishyPull by remember { mutableStateOf(Offset.Zero) }
    var squishyPressure by remember { mutableFloatStateOf(0f) }
    var magSnapPosition by remember { mutableIntStateOf(1) }
    var popGridMask by remember { mutableIntStateOf(0) }
    var infinityFold by remember { mutableIntStateOf(0) }
    var ratchetStep by remember { mutableIntStateOf(0) }
    var liquidBlobPosition by remember { mutableStateOf(Offset.Zero) }
    var gearRotation by remember { mutableFloatStateOf(0f) }
    var worryStoneRub by remember { mutableFloatStateOf(0f) }
    var keyClickMask by remember { mutableIntStateOf(0) }
    var zenTracePoints by remember { mutableStateOf(emptyList<Offset>()) }
    var slingshotPosition by remember { mutableStateOf(Offset.Zero) }
    var slingshotVelocity by remember { mutableStateOf(Offset.Zero) }
    var slingshotPulling by remember { mutableStateOf(false) }
    var mazePuzzle by remember { mutableStateOf(generateFidgetMazePuzzle()) }
    var mazePlayerCell by remember { mutableIntStateOf(mazePuzzle.startCell) }
    var hapticFeedbackEnabled by rememberSaveable { mutableStateOf(savedSettings.hapticFeedbackEnabled) }
    var soundFeedbackEnabled by rememberSaveable { mutableStateOf(savedSettings.soundFeedbackEnabled) }
    var feedbackSoundMode by rememberSaveable { mutableStateOf(savedSettings.feedbackSoundMode) }
    var accentIntensityMode by rememberSaveable { mutableStateOf(savedSettings.accentIntensityMode) }
    var appLanguage by rememberSaveable { mutableStateOf(savedSettings.appLanguage) }
    var keepScreenOn by rememberSaveable { mutableStateOf(savedSettings.keepScreenOn) }
    var cpuPercentVisible by rememberSaveable { mutableStateOf(savedSettings.cpuPercentVisible) }
    var pinnedToyIdsCsv by rememberSaveable { mutableStateOf(savedSettings.pinnedToyIdsCsv) }
    var reviewPopupOpen by remember { mutableStateOf(false) }
    var donationPopupOpen by remember { mutableStateOf(false) }
    var colorPopupOpen by remember { mutableStateOf(false) }
    var intensityPopupOpen by remember { mutableStateOf(false) }
    var reviewStatusText by remember { mutableStateOf("") }
    var donationThanksText by remember { mutableStateOf("") }
    var donationCounts by remember { mutableStateOf(context.loadFidgetDonationCounts()) }
    val isInstalledFromPlay = remember(context) { context.isInstalledFromPlay() }
    val mainColor = colorFromChoice(mainColorArgb)
    val backgroundColor = colorFromChoice(backgroundColorArgb)
    val ringColor = colorFromChoice(ringColorArgb)
    val fidgetText = fidgetTextFor(appLanguage)
    val donationBadge = fidgetDonationBadgeFor(donationCounts)
    val pinnedToyIds = remember(pinnedToyIdsCsv) { pinnedToyIdsCsv.toPinnedToyIds() }
    val fidgetPageOrder = remember(pinnedToyIds) { fidgetPageOrderFor(pinnedToyIds) }
    val nextRewardCount = nextFibonacciTarget(fidgetCount)
    val cpuUsagePercent = rememberFidgetCpuUsagePercent(enabled = cpuPercentVisible)
    val feedbackController = remember(context) {
        FidgetFeedbackController(context.applicationContext)
    }
    val hostActivity = remember(context) { context.findActivity() }
    val donationCoordinator = remember(context, isInstalledFromPlay) {
        if (!isInstalledFromPlay) {
            null
        } else {
            BillingUnlockCoordinator(
                context = context,
                productIds = FIDGET_DONATION_PRODUCTS.map { it.productId }.toSet(),
                consumableProductIds = FIDGET_DONATION_PRODUCTS.map { it.productId }.toSet(),
                onProductOwned = { productId ->
                    donationThanksText = donationThanksTextFor(productId, appLanguage)
                    donationCounts = context.recordFidgetDonation(productId)
                },
                onProductConsumed = { productId ->
                    donationThanksText = donationThanksTextFor(productId, appLanguage)
                    donationCounts = context.recordFidgetDonation(productId)
                },
            )
        }
    }

    DisposableEffect(feedbackController) {
        onDispose {
            feedbackController.release()
        }
    }

    DisposableEffect(donationCoordinator) {
        donationCoordinator?.start()
        onDispose {
            donationCoordinator?.stop()
        }
    }

    DisposableEffect(hostActivity, keepScreenOn) {
        val window = hostActivity?.window
        if (keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    fun triggerFeedback(countFidget: Boolean = true) {
        if (countFidget) {
            val nextCount = fidgetCount + 1
            fidgetCount = nextCount
            if (isFibonacciReward(nextCount)) {
                rewardPulse = 1f
                touchPulse = 1f
            }
        }
        feedbackController.play(
            hapticEnabled = hapticFeedbackEnabled,
            soundEnabled = soundFeedbackEnabled,
            beatSoundMode = feedbackSoundMode,
            accentIntensityMode = accentIntensityMode,
        )
    }

    fun saveSettingsAndCloseMenu() {
        context.saveFidgetSettings(
            FidgetSettingsState(
                mainColorArgb = mainColorArgb,
                backgroundColorArgb = backgroundColorArgb,
                ringColorArgb = ringColorArgb,
                hapticFeedbackEnabled = hapticFeedbackEnabled,
                soundFeedbackEnabled = soundFeedbackEnabled,
                feedbackSoundMode = feedbackSoundMode,
                accentIntensityMode = accentIntensityMode,
                appLanguage = appLanguage,
                keepScreenOn = keepScreenOn,
                cpuPercentVisible = cpuPercentVisible,
                pinnedToyIdsCsv = pinnedToyIdsCsv,
            ),
        )
        triggerFeedback(countFidget = false)
        toyIndex = FIDGET_SPINNER_INDEX
    }

    fun togglePinnedToy(toyId: Int) {
        val nextPinnedToyIds = if (toyId in pinnedToyIds) {
            pinnedToyIds - toyId
        } else {
            (pinnedToyIds + toyId).sortedWith(fidgetToyOrderComparator())
        }
        pinnedToyIdsCsv = nextPinnedToyIds.joinToString(",")
        context.saveFidgetPinnedToyIds(pinnedToyIdsCsv)
        triggerFeedback(countFidget = false)
    }

    fun moveInFidgetOrder(delta: Int) {
        val currentIndex = fidgetPageOrder.indexOf(toyIndex).takeIf { it >= 0 } ?: 0
        toyIndex = fidgetPageOrder[(currentIndex + delta).wrapFidgetIndex(fidgetPageOrder.size)]
    }

    LaunchedEffect(Unit) {
        var previousFrameNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (previousFrameNanos != 0L) {
                    val elapsedSeconds = (frameNanos - previousFrameNanos) / 1_000_000_000f
                    rotationDegrees += spinVelocityDegreesPerSecond * elapsedSeconds
                    spinVelocityDegreesPerSecond *= 0.992f
                    if (spinVelocityDegreesPerSecond in -3f..3f) {
                        spinVelocityDegreesPerSecond = 0f
                    }
                    if (!slingshotPulling && slingshotVelocity.vectorLength() > 0f) {
                        val nextPosition = slingshotPosition + slingshotVelocity * elapsedSeconds
                        var nextX = nextPosition.x
                        var nextY = nextPosition.y
                        var nextVelocityX = slingshotVelocity.x
                        var nextVelocityY = slingshotVelocity.y

                        if (nextX > SLINGSHOT_BOUNCE_LIMIT_DP) {
                            nextX = SLINGSHOT_BOUNCE_LIMIT_DP
                            nextVelocityX = -abs(nextVelocityX) * SLINGSHOT_BOUNCE_DAMPING
                        } else if (nextX < -SLINGSHOT_BOUNCE_LIMIT_DP) {
                            nextX = -SLINGSHOT_BOUNCE_LIMIT_DP
                            nextVelocityX = abs(nextVelocityX) * SLINGSHOT_BOUNCE_DAMPING
                        }

                        if (nextY > SLINGSHOT_BOUNCE_LIMIT_DP) {
                            nextY = SLINGSHOT_BOUNCE_LIMIT_DP
                            nextVelocityY = -abs(nextVelocityY) * SLINGSHOT_BOUNCE_DAMPING
                        } else if (nextY < -SLINGSHOT_BOUNCE_LIMIT_DP) {
                            nextY = -SLINGSHOT_BOUNCE_LIMIT_DP
                            nextVelocityY = abs(nextVelocityY) * SLINGSHOT_BOUNCE_DAMPING
                        }

                        slingshotPosition = Offset(nextX, nextY)
                        slingshotVelocity = Offset(nextVelocityX, nextVelocityY) * SLINGSHOT_ROLLING_DAMPING
                        if (slingshotVelocity.vectorLength() < SLINGSHOT_STOP_SPEED) {
                            slingshotVelocity = Offset.Zero
                        }
                    }
                    if (squishyPressure > 0f || squishyPull.vectorLength() > 0.1f) {
                        squishyPressure *= 0.9f
                        squishyPull *= 0.84f
                        if (squishyPressure < 0.02f) {
                            squishyPressure = 0f
                        }
                        if (squishyPull.vectorLength() < 0.4f) {
                            squishyPull = Offset.Zero
                        }
                    }
                    if (worryStoneRub > 0f) {
                        worryStoneRub *= 0.965f
                        if (worryStoneRub < 0.01f) {
                            worryStoneRub = 0f
                        }
                    }
                    if (zenTracePoints.isNotEmpty()) {
                        zenTracePoints = zenTracePoints.takeLast(28)
                    }
                    touchPulse *= 0.9f
                    rewardPulse *= 0.94f
                }
                previousFrameNanos = frameNanos
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = 0.98f),
                        backgroundColor.copy(alpha = 0.78f),
                        Color.Black,
                    ),
                ),
            )
    ) {
        val compactWatch = minOf(maxWidth, maxHeight) <= 205.dp
        val navButtonWidth = if (compactWatch) 30.dp else 38.dp
        val navButtonHeight = if (compactWatch) 54.dp else 64.dp
        val navButtonFontSize = if (compactWatch) 42.sp else 50.sp
        val navButtonPadding = if (compactWatch) 4.dp else 8.dp

        Canvas(modifier = Modifier.fillMaxSize()) {
            val rewardAlpha = (0.16f + rewardPulse * 0.58f).coerceIn(0f, 0.74f)
            drawCircle(
                color = ringColor.copy(alpha = rewardAlpha),
                radius = size.minDimension * (0.42f + rewardPulse * 0.04f),
                center = center,
                style = Stroke(width = (3.dp + (rewardPulse * 6f).dp).toPx()),
            )
            drawCircle(
                color = mainColor.copy(alpha = 0.11f + rewardPulse * 0.18f),
                radius = size.minDimension * (0.29f + touchPulse * 0.035f + rewardPulse * 0.025f),
                center = center,
                style = Stroke(width = 10.dp.toPx()),
            )
        }

        if (toyIndex != FIDGET_MENU_INDEX && toyIndex != FIDGET_WALL_INDEX) {
            FidgetPlaylistNavButton(
                isNext = false,
                accentColor = mainColor,
                accentColorArgb = mainColorArgb,
                width = navButtonWidth,
                height = navButtonHeight,
                fontSize = navButtonFontSize,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = navButtonPadding),
                onClick = {
                    triggerFeedback()
                    moveInFidgetOrder(-1)
                },
            )

            FidgetPlaylistNavButton(
                isNext = true,
                accentColor = mainColor,
                accentColorArgb = mainColorArgb,
                width = navButtonWidth,
                height = navButtonHeight,
                fontSize = navButtonFontSize,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = navButtonPadding),
                onClick = {
                    triggerFeedback()
                    moveInFidgetOrder(1)
                },
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, top = 30.dp, end = 14.dp, bottom = 7.dp),
        ) {
            if (toyIndex != FIDGET_MENU_INDEX) {
                FidgetTitleWithDonationBadge(
                    title = fidgetText.title,
                    badge = donationBadge,
                )
            } else {
                Spacer(modifier = Modifier.height(2.dp))
            }

            Box(
                contentAlignment = if (toyIndex == FIDGET_MENU_INDEX) {
                    Alignment.TopCenter
                } else {
                    Alignment.Center
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when (toyIndex) {
                    FIDGET_WALL_INDEX -> {
                        FidgetSelectionWallPage(
                            pinnedToyIds = pinnedToyIds,
                            accentColor = mainColor,
                            accentColorArgb = mainColorArgb,
                            onToySelected = { toyId ->
                                triggerFeedback(countFidget = false)
                                toyIndex = toyId
                            },
                            onPinToggle = ::togglePinnedToy,
                        )
                    }

                    FIDGET_SPINNER_INDEX -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(118.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.07f))
                                .border(1.dp, Color(0xFF56F1C8).copy(alpha = 0.7f), CircleShape)
                                .combinedClickable(
                                    onClick = {},
                                onLongClick = {
                                    triggerFeedback()
                                    spinVelocityDegreesPerSecond = 0f
                                    touchPulse = 1f
                                },
                                )
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = {
                                        spinVelocityDegreesPerSecond = 0f
                                        touchPulse = 1f
                                        triggerFeedback()
                                    },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            val center = androidx.compose.ui.geometry.Offset(
                                                x = size.width / 2f,
                                                y = size.height / 2f,
                                            )
                                            val previousAngle = angleDegrees(change.previousPosition, center)
                                            val currentAngle = angleDegrees(change.position, center)
                                            val deltaDegrees = shortestAngleDelta(previousAngle, currentAngle)
                                            val elapsedMillis = (change.uptimeMillis - change.previousUptimeMillis)
                                                .coerceAtLeast(1L)

                                            rotationDegrees += deltaDegrees
                                            spinVelocityDegreesPerSecond = (deltaDegrees / elapsedMillis * 1_000f)
                                                .coerceIn(-2_700f, 2_700f)
                                            touchPulse = 1f
                                        },
                                        onDragEnd = {
                                            spinVelocityDegreesPerSecond *= 1.35f
                                        },
                                        onDragCancel = {
                                            spinVelocityDegreesPerSecond = 0f
                                        },
                                    )
                                },
                        ) {
                            FidgetSpinner(rotation = rotationDegrees)
                        }
                    }

                    FIDGET_SWITCH_INDEX -> {
                        SwitchFidgetToy(
                            switchMask = switchMask,
                            onSwitchToggle = { index ->
                                triggerFeedback()
                                switchMask = switchMask xor (1 shl index)
                            },
                        )
                    }

                    else -> {
                        if (toyIndex == FIDGET_SWITCH_MAZE_INDEX) {
                            SwitchMazeToy(
                                mazePosition = mazePosition,
                                onMove = { deltaColumn, deltaRow ->
                                    triggerFeedback()
                                    val column = (mazePosition % SWITCH_MAZE_COLUMNS + deltaColumn)
                                        .coerceIn(0, SWITCH_MAZE_COLUMNS - 1)
                                    val row = (mazePosition / SWITCH_MAZE_COLUMNS + deltaRow)
                                        .coerceIn(0, SWITCH_MAZE_ROWS - 1)
                                    mazePosition = row * SWITCH_MAZE_COLUMNS + column
                                },
                            )
                        } else if (toyIndex == FIDGET_FREE_BUTTON_INDEX) {
                            FreeMoveButtonToy(
                                buttonPositions = freeButtonPositions,
                                onButtonDragStart = {
                                    triggerFeedback()
                                },
                                onButtonMove = { index, delta ->
                                    freeButtonPositions = freeButtonPositions.mapIndexed { positionIndex, position ->
                                        if (positionIndex == index) {
                                            Offset(
                                                x = (position.x + delta.x).coerceIn(-45f, 45f),
                                                y = (position.y + delta.y).coerceIn(-45f, 45f),
                                            )
                                        } else {
                                            position
                                        }
                                    }
                                },
                            )
                        } else if (toyIndex == FIDGET_WHACK_BUTTON_INDEX) {
                            WhackColorButtonToy(
                                buttonPositions = whackButtonPositions,
                                onButtonTap = { index ->
                                    triggerFeedback()
                                    whackButtonPositions = whackButtonPositions.mapIndexed { positionIndex, position ->
                                        if (positionIndex == index) {
                                            randomOpenWhackPosition(
                                                currentPosition = position,
                                                occupiedPositions = whackButtonPositions.filterIndexed { otherIndex, _ ->
                                                    otherIndex != index
                                                }.toSet(),
                                            )
                                        } else {
                                            position
                                        }
                                    }
                                },
                            )
                        } else if (toyIndex == FIDGET_SQUISHY_INDEX) {
                            SquishyFidgetToy(
                                pullOffset = squishyPull,
                                pressure = squishyPressure,
                                accentColor = mainColor,
                                onPress = {
                                    triggerFeedback()
                                    squishyPressure = 1f
                                },
                                onPull = { pullOffset, pressure ->
                                    squishyPull = pullOffset
                                    squishyPressure = pressure
                                    touchPulse = 1f
                                },
                                onRelease = {
                                    triggerFeedback()
                                    squishyPressure = 0.7f
                                },
                            )
                        } else if (toyIndex == FIDGET_MAG_SNAP_INDEX) {
                            MagSnapFidgetToy(
                                position = magSnapPosition,
                                onMove = { delta ->
                                    val nextPosition = (magSnapPosition + delta).coerceIn(0, 2)
                                    if (nextPosition != magSnapPosition) {
                                        triggerFeedback()
                                        magSnapPosition = nextPosition
                                    }
                                },
                            )
                        } else if (toyIndex == FIDGET_POP_GRID_INDEX) {
                            PopGridFidgetToy(
                                popMask = popGridMask,
                                onPop = { index ->
                                    triggerFeedback()
                                    popGridMask = popGridMask xor (1 shl index)
                                    if (popGridMask == (1 shl POP_GRID_COUNT) - 1) {
                                        rewardPulse = 1f
                                    }
                                },
                                onReset = {
                                    triggerFeedback()
                                    popGridMask = 0
                                },
                            )
                        } else if (toyIndex == FIDGET_INFINITY_CUBE_INDEX) {
                            InfinityFlipFidgetToy(
                                fold = infinityFold,
                                onFlip = {
                                    triggerFeedback()
                                    infinityFold = (infinityFold + 1).wrapFidgetIndex(4)
                                },
                            )
                        } else if (toyIndex == FIDGET_RATCHET_RING_INDEX) {
                            RatchetRingFidgetToy(
                                step = ratchetStep,
                                onStep = { delta ->
                                    triggerFeedback()
                                    ratchetStep = (ratchetStep + delta).wrapFidgetIndex(RATCHET_STEP_COUNT)
                                    touchPulse = 1f
                                },
                            )
                        } else if (toyIndex == FIDGET_LIQUID_MAZE_INDEX) {
                            LiquidMazeFidgetToy(
                                blobPosition = liquidBlobPosition,
                                onMove = { delta ->
                                    liquidBlobPosition = (liquidBlobPosition + delta).limitedToBox(45f, 45f)
                                    touchPulse = 1f
                                },
                                onRelease = {
                                    triggerFeedback()
                                },
                            )
                        } else if (toyIndex == FIDGET_GEAR_JAM_INDEX) {
                            GearJamFidgetToy(
                                rotation = gearRotation,
                                onTurn = { delta ->
                                    triggerFeedback()
                                    gearRotation += delta
                                    touchPulse = 1f
                                },
                            )
                        } else if (toyIndex == FIDGET_WORRY_STONE_INDEX) {
                            WorryStoneFidgetToy(
                                rub = worryStoneRub,
                                accentColor = mainColor,
                                onRub = { delta ->
                                    worryStoneRub = (worryStoneRub + delta.vectorLength() / 80f).coerceIn(0f, 1f)
                                    triggerFeedback()
                                },
                            )
                        } else if (toyIndex == FIDGET_KEY_CLICKS_INDEX) {
                            KeyClicksFidgetToy(
                                keyMask = keyClickMask,
                                onKeyPress = { index ->
                                    triggerFeedback()
                                    keyClickMask = keyClickMask xor (1 shl index)
                                },
                            )
                        } else if (toyIndex == FIDGET_ZEN_TRACE_INDEX) {
                            ZenTraceFidgetToy(
                                tracePoints = zenTracePoints,
                                accentColor = mainColor,
                                onTrace = { point ->
                                    zenTracePoints = (zenTracePoints + point).takeLast(28)
                                    touchPulse = 1f
                                },
                                onTraceStart = {
                                    triggerFeedback()
                                },
                                onClear = {
                                    triggerFeedback()
                                    zenTracePoints = emptyList()
                                },
                            )
                        } else if (toyIndex == FIDGET_SLINGSHOT_INDEX) {
                            SlingshotFidgetToy(
                                ballPosition = slingshotPosition,
                                pullLimit = SLINGSHOT_PULL_LIMIT_DP,
                                onPullStart = {
                                    triggerFeedback()
                                    slingshotPulling = true
                                    slingshotVelocity = Offset.Zero
                                },
                                onPullMove = { pullOffset ->
                                    slingshotPosition = pullOffset
                                },
                                onRelease = { pullOffset ->
                                    triggerFeedback()
                                    slingshotPulling = false
                                    slingshotPosition = pullOffset
                                    slingshotVelocity = if (pullOffset.vectorLength() > 2f) {
                                        -pullOffset * SLINGSHOT_LAUNCH_MULTIPLIER
                                    } else {
                                        Offset.Zero
                                    }
                                },
                            )
                        } else if (toyIndex == FIDGET_MAZE_INDEX) {
                            MazeFidgetToy(
                                puzzle = mazePuzzle,
                                playerCell = mazePlayerCell,
                                onMove = { direction ->
                                    val nextCell = mazePuzzle.nextCell(mazePlayerCell, direction)
                                    if (nextCell != mazePlayerCell) {
                                        triggerFeedback()
                                        mazePlayerCell = nextCell
                                        if (nextCell == mazePuzzle.endCell) {
                                            rewardPulse = 1f
                                        }
                                    }
                                },
                                onRefresh = {
                                    triggerFeedback()
                                    mazePuzzle = generateFidgetMazePuzzle()
                                    mazePlayerCell = mazePuzzle.startCell
                                },
                            )
                        } else {
                            FidgetMenuPage(
                                appLanguage = appLanguage,
                                text = fidgetText,
                                hapticFeedbackEnabled = hapticFeedbackEnabled,
                                soundFeedbackEnabled = soundFeedbackEnabled,
                                feedbackSoundMode = feedbackSoundMode,
                                accentIntensityMode = accentIntensityMode,
                                keepScreenOn = keepScreenOn,
                                cpuPercentVisible = cpuPercentVisible,
                                cpuUsagePercent = cpuUsagePercent,
                            mainColorArgb = mainColorArgb,
                            backgroundColorArgb = backgroundColorArgb,
                            ringColorArgb = ringColorArgb,
                                onReviewClick = {
                                    triggerFeedback(countFidget = false)
                                    reviewStatusText = ""
                                    reviewPopupOpen = true
                                },
                                onDonateClick = {
                                    triggerFeedback(countFidget = false)
                                    donationPopupOpen = true
                                },
                                onDoneClick = ::saveSettingsAndCloseMenu,
                                onKeepScreenToggle = {
                                    keepScreenOn = !keepScreenOn
                                    triggerFeedback(countFidget = false)
                                },
                                onCpuToggle = {
                                    cpuPercentVisible = !cpuPercentVisible
                                    triggerFeedback(countFidget = false)
                                },
                                onLanguageChoice = { language ->
                                    appLanguage = language
                                    donationThanksText = ""
                                    triggerFeedback(countFidget = false)
                                },
                                onAccentIntensityModeChoice = { mode ->
                                    accentIntensityMode = mode
                                    triggerFeedback(countFidget = false)
                                },
                                onMainColorChoice = { colorArgb ->
                                    mainColorArgb = colorArgb
                                    triggerFeedback(countFidget = false)
                                },
                                onBackgroundColorChoice = { colorArgb ->
                                    backgroundColorArgb = colorArgb
                                    triggerFeedback(countFidget = false)
                                },
                                onRingColorChoice = { colorArgb ->
                                    ringColorArgb = colorArgb
                                    rewardPulse = 1f
                                    triggerFeedback(countFidget = false)
                                },
                                onHapticToggle = {
                                    val nextEnabled = !hapticFeedbackEnabled
                                    hapticFeedbackEnabled = nextEnabled
                                    feedbackController.play(
                                        hapticEnabled = nextEnabled,
                                        soundEnabled = false,
                                        beatSoundMode = feedbackSoundMode,
                                        accentIntensityMode = accentIntensityMode,
                                    )
                                },
                                onSoundToggle = {
                                    val nextEnabled = !soundFeedbackEnabled
                                    soundFeedbackEnabled = nextEnabled
                                    feedbackController.play(
                                        hapticEnabled = false,
                                        soundEnabled = nextEnabled,
                                        beatSoundMode = feedbackSoundMode,
                                        accentIntensityMode = accentIntensityMode,
                                    )
                                },
                                onSoundModeChoice = { mode ->
                                    feedbackSoundMode = mode
                                    feedbackController.play(
                                        hapticEnabled = hapticFeedbackEnabled,
                                        soundEnabled = true,
                                        beatSoundMode = mode,
                                        accentIntensityMode = accentIntensityMode,
                                    )
                                },
                            )
                        }
                    }
                }
            }

            if (toyIndex != FIDGET_MENU_INDEX) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(43.dp),
                ) {
                    FidgetNavButton(
                        text = fidgetText.menu,
                        wide = true,
                        accentColor = mainColor,
                        accentColorArgb = mainColorArgb,
                        onClick = {
                            triggerFeedback(countFidget = false)
                            toyIndex = FIDGET_MENU_INDEX
                        },
                    )
                    Text(
                        text = fidgetText.rewardLine(fidgetCount, nextRewardCount),
                        color = ringColor.copy(alpha = 0.88f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }

        if (reviewPopupOpen) {
            PlayStoreReviewPopup(
                text = fidgetText,
                statusText = reviewStatusText,
                onOpenReview = {
                    triggerFeedback()
                    if (!isInstalledFromPlay) {
                        reviewStatusText = fidgetText.installFromPlay
                        return@PlayStoreReviewPopup
                    }
                    context.openFidgetPlayStoreListing()
                    reviewPopupOpen = false
                },
                onOpenPrivacyPolicy = {
                    triggerFeedback()
                    context.openFidgetPrivacyPolicy()
                },
                onDismiss = {
                    triggerFeedback()
                    reviewStatusText = ""
                    reviewPopupOpen = false
                },
            )
        }

        if (donationPopupOpen) {
            FidgetDonationPopup(
                text = fidgetText,
                statusText = donationThanksText.ifBlank {
                    donationCoordinator?.statusText ?: fidgetText.installFromPlay
                },
                onDonate = { productId ->
                    triggerFeedback()
                    if (!isInstalledFromPlay) {
                        donationThanksText = fidgetText.installFromPlay
                        return@FidgetDonationPopup
                    }
                    donationThanksText = ""
                    donationCoordinator?.buy(hostActivity, productId)
                },
                onDismiss = {
                    triggerFeedback()
                    donationPopupOpen = false
                },
            )
        }

        if (colorPopupOpen) {
            FidgetColorPopup(
                text = fidgetText,
                mainColorArgb = mainColorArgb,
                backgroundColorArgb = backgroundColorArgb,
                ringColorArgb = ringColorArgb,
                onMainColorChoice = { colorArgb ->
                    mainColorArgb = colorArgb
                    triggerFeedback(countFidget = false)
                },
                onBackgroundColorChoice = { colorArgb ->
                    backgroundColorArgb = colorArgb
                    triggerFeedback(countFidget = false)
                },
                onRingColorChoice = { colorArgb ->
                    ringColorArgb = colorArgb
                    rewardPulse = 1f
                    triggerFeedback(countFidget = false)
                },
                onDismiss = {
                    triggerFeedback(countFidget = false)
                    colorPopupOpen = false
                },
            )
        }

        if (intensityPopupOpen) {
            FidgetIntensityPopup(
                appLanguage = appLanguage,
                text = fidgetText,
                selectedMode = accentIntensityMode,
                onModeChoice = { mode ->
                    accentIntensityMode = mode
                    triggerFeedback(countFidget = false)
                },
                onDismiss = {
                    triggerFeedback(countFidget = false)
                    intensityPopupOpen = false
                },
            )
        }
    }
}

private const val FIDGET_WALL_INDEX = 0
private const val FIDGET_SPINNER_INDEX = 1
private const val FIDGET_SWITCH_INDEX = 2
private const val FIDGET_SWITCH_MAZE_INDEX = 3
private const val FIDGET_FREE_BUTTON_INDEX = 4
private const val FIDGET_WHACK_BUTTON_INDEX = 5
private const val FIDGET_SLINGSHOT_INDEX = 6
private const val FIDGET_MAZE_INDEX = 7
private const val FIDGET_SQUISHY_INDEX = 8
private const val FIDGET_MAG_SNAP_INDEX = 9
private const val FIDGET_POP_GRID_INDEX = 10
private const val FIDGET_INFINITY_CUBE_INDEX = 11
private const val FIDGET_RATCHET_RING_INDEX = 12
private const val FIDGET_LIQUID_MAZE_INDEX = 13
private const val FIDGET_GEAR_JAM_INDEX = 14
private const val FIDGET_WORRY_STONE_INDEX = 15
private const val FIDGET_KEY_CLICKS_INDEX = 16
private const val FIDGET_ZEN_TRACE_INDEX = 17
private const val FIDGET_MENU_INDEX = 18
private const val SWITCH_MAZE_COLUMNS = 4
private const val SWITCH_MAZE_ROWS = 4
private const val SWITCH_MAZE_CELL_COUNT = SWITCH_MAZE_COLUMNS * SWITCH_MAZE_ROWS
private const val FIDGET_MAZE_COLUMNS = 5
private const val FIDGET_MAZE_ROWS = 5
private const val FIDGET_MAZE_CELL_COUNT = FIDGET_MAZE_COLUMNS * FIDGET_MAZE_ROWS
private const val MAZE_OPEN_UP = 1
private const val MAZE_OPEN_RIGHT = 2
private const val MAZE_OPEN_DOWN = 4
private const val MAZE_OPEN_LEFT = 8
private const val SLINGSHOT_PULL_LIMIT_DP = 42f
private const val SLINGSHOT_BOUNCE_LIMIT_DP = 47f
private const val SLINGSHOT_LAUNCH_MULTIPLIER = 16f
private const val SLINGSHOT_BOUNCE_DAMPING = 0.92f
private const val SLINGSHOT_ROLLING_DAMPING = 0.992f
private const val SLINGSHOT_STOP_SPEED = 8f
private const val SQUISHY_PULL_LIMIT_DP = 38f
private const val POP_GRID_COUNT = 12
private const val RATCHET_STEP_COUNT = 16
private const val FIDGET_PRIVACY_POLICY_URL = "https://labmunkz.com/privacy-policy/"
private const val FIDGET_DONATION_1_PRODUCT_ID = "fidget_donation_1"
private const val FIDGET_DONATION_3_PRODUCT_ID = "fidget_donation_3"
private const val FIDGET_DONATION_5_PRODUCT_ID = "fidget_donation_5"
private const val FIDGET_DONATION_10_PRODUCT_ID = "fidget_donation_10"
private const val FIDGET_SETTINGS_PREFS = "munkz_fidget_toy_settings"
private const val FIDGET_MAIN_COLOR_KEY = "main_color"
private const val FIDGET_BACKGROUND_COLOR_KEY = "background_color"
private const val FIDGET_RING_COLOR_KEY = "ring_color"
private const val FIDGET_HAPTIC_ENABLED_KEY = "haptic_enabled"
private const val FIDGET_SOUND_ENABLED_KEY = "sound_enabled"
private const val FIDGET_SOUND_MODE_KEY = "sound_mode"
private const val FIDGET_ACCENT_INTENSITY_KEY = "accent_intensity"
private const val FIDGET_LANGUAGE_KEY = "language"
private const val FIDGET_KEEP_SCREEN_ON_KEY = "keep_screen_on"
private const val FIDGET_CPU_VISIBLE_KEY = "cpu_visible"
private const val FIDGET_PINNED_TOYS_KEY = "pinned_toys"
private const val FIDGET_DONATION_COUNT_PREFIX = "donation_count_"

private data class FidgetToyInfo(
    val id: Int,
    val name: String,
    val style: String,
)

private data class FidgetDonationProduct(
    val label: String,
    val productId: String,
)

private data class FidgetDonationBadge(
    val label: String,
    val count: Int,
    val colorArgb: Int,
)

private enum class MazeDirection {
    Up,
    Right,
    Down,
    Left,
}

private data class FidgetMazePuzzle(
    val openings: List<Int>,
    val startCell: Int,
    val endCell: Int,
) {
    fun nextCell(currentCell: Int, direction: MazeDirection): Int {
        val column = currentCell % FIDGET_MAZE_COLUMNS
        val row = currentCell / FIDGET_MAZE_COLUMNS
        val openMask = openings.getOrElse(currentCell) { 0 }
        return when (direction) {
            MazeDirection.Up -> if (row > 0 && openMask and MAZE_OPEN_UP != 0) {
                currentCell - FIDGET_MAZE_COLUMNS
            } else {
                currentCell
            }
            MazeDirection.Right -> if (column < FIDGET_MAZE_COLUMNS - 1 && openMask and MAZE_OPEN_RIGHT != 0) {
                currentCell + 1
            } else {
                currentCell
            }
            MazeDirection.Down -> if (row < FIDGET_MAZE_ROWS - 1 && openMask and MAZE_OPEN_DOWN != 0) {
                currentCell + FIDGET_MAZE_COLUMNS
            } else {
                currentCell
            }
            MazeDirection.Left -> if (column > 0 && openMask and MAZE_OPEN_LEFT != 0) {
                currentCell - 1
            } else {
                currentCell
            }
        }
    }
}

private data class FidgetSettingsState(
    val mainColorArgb: Int,
    val backgroundColorArgb: Int,
    val ringColorArgb: Int,
    val hapticFeedbackEnabled: Boolean,
    val soundFeedbackEnabled: Boolean,
    val feedbackSoundMode: BeatSoundMode,
    val accentIntensityMode: AccentIntensityMode,
    val appLanguage: AppLanguage,
    val keepScreenOn: Boolean,
    val cpuPercentVisible: Boolean,
    val pinnedToyIdsCsv: String,
)

private val FIDGET_TOY_INFOS = listOf(
    FidgetToyInfo(FIDGET_SPINNER_INDEX, "Spin Storm", "Motion"),
    FidgetToyInfo(FIDGET_SWITCH_INDEX, "Flip Stack", "Switches"),
    FidgetToyInfo(FIDGET_SWITCH_MAZE_INDEX, "Grid Stepper", "Switches"),
    FidgetToyInfo(FIDGET_FREE_BUTTON_INDEX, "Button Drift", "Touch"),
    FidgetToyInfo(FIDGET_WHACK_BUTTON_INDEX, "Color Pop Hunt", "Touch"),
    FidgetToyInfo(FIDGET_SLINGSHOT_INDEX, "Bounce Shot", "Motion"),
    FidgetToyInfo(FIDGET_MAZE_INDEX, "Maze Shuffle", "Puzzle"),
    FidgetToyInfo(FIDGET_SQUISHY_INDEX, "Squish Pop", "Soft"),
    FidgetToyInfo(FIDGET_MAG_SNAP_INDEX, "Mag Snap", "Click"),
    FidgetToyInfo(FIDGET_POP_GRID_INDEX, "Pop Grid", "Touch"),
    FidgetToyInfo(FIDGET_INFINITY_CUBE_INDEX, "Infinity Flip", "Motion"),
    FidgetToyInfo(FIDGET_RATCHET_RING_INDEX, "Ratchet Ring", "Click"),
    FidgetToyInfo(FIDGET_LIQUID_MAZE_INDEX, "Liquid Maze", "Flow"),
    FidgetToyInfo(FIDGET_GEAR_JAM_INDEX, "Gear Jam", "Motion"),
    FidgetToyInfo(FIDGET_WORRY_STONE_INDEX, "Worry Stone", "Soft"),
    FidgetToyInfo(FIDGET_KEY_CLICKS_INDEX, "Key Clicks", "Click"),
    FidgetToyInfo(FIDGET_ZEN_TRACE_INDEX, "Zen Trace", "Flow"),
)

private data class FidgetText(
    val title: String,
    val menu: String,
    val toys: String,
    val rewardLine: (count: Int, nextReward: Int) -> String,
    val links: String,
    val review: String,
    val donate: String,
    val feedback: String,
    val vibe: String,
    val sound: String,
    val click: String,
    val wood: String,
    val bell: String,
    val bigBeep: String,
    val watch: String,
    val keepOn: String,
    val keepOff: String,
    val theme: String,
    val mainColor: String,
    val backgroundColor: String,
    val ringColor: String,
    val language: String,
    val cpu: String,
    val on: String,
    val off: String,
    val done: String,
    val addReviewTitle: String,
    val openPlayStore: String,
    val no: String,
    val privacyPolicy: String,
    val colors: String,
    val intensityHelp: String,
    val installFromPlay: String,
    val thanksFor: (label: String) -> String,
)

private fun fidgetTextFor(language: AppLanguage): FidgetText {
    return when (language) {
        AppLanguage.English -> FidgetText(
            title = "Fidget Toy",
            menu = "Menu",
            toys = "Toys",
            rewardLine = { count, nextReward -> "$count taps  |  $nextReward reward" },
            links = "Links",
            review = "Review",
            donate = "Donate",
            feedback = "Feedback",
            vibe = "Vibe",
            sound = "Sound",
            click = "Click",
            wood = "Wood",
            bell = "Bell",
            bigBeep = "Big Beep",
            watch = "Watch",
            keepOn = "Keep On",
            keepOff = "Keep Off",
            theme = "Theme",
            mainColor = "Main",
            backgroundColor = "BG",
            ringColor = "Ring",
            language = "Language",
            cpu = "CPU",
            on = "On",
            off = "Off",
            done = "Done",
            addReviewTitle = "Add a review?",
            openPlayStore = "Open Play Store",
            no = "No",
            privacyPolicy = "Privacy Policy",
            colors = "Colors",
            intensityHelp = "Beep + vibe strength",
            installFromPlay = "Install from Play to use",
            thanksFor = { label -> "Thanks for $label" },
        )
        AppLanguage.Spanish -> FidgetText(
            title = "Juguete Fidget",
            menu = "Menu",
            toys = "Juguet",
            rewardLine = { count, nextReward -> "$count toques  |  $nextReward premio" },
            links = "Links",
            review = "Resena",
            donate = "Donar",
            feedback = "Toque",
            vibe = "Vibra",
            sound = "Sonido",
            click = "Click",
            wood = "Madera",
            bell = "Camp",
            bigBeep = "Beep Gran",
            watch = "Reloj",
            keepOn = "Keep Si",
            keepOff = "Keep No",
            theme = "Tema",
            mainColor = "Color",
            backgroundColor = "Fondo",
            ringColor = "Aro",
            language = "Idioma",
            cpu = "CPU",
            on = "Si",
            off = "No",
            done = "Listo",
            addReviewTitle = "Agregar resena?",
            openPlayStore = "Abrir Play Store",
            no = "No",
            privacyPolicy = "Privacidad",
            colors = "Colores",
            intensityHelp = "Fuerza beep + vibra",
            installFromPlay = "Instala desde Play",
            thanksFor = { label -> "Gracias por $label" },
        )
    }
}

private val FIDGET_DONATION_PRODUCTS = listOf(
    FidgetDonationProduct("$1", FIDGET_DONATION_1_PRODUCT_ID),
    FidgetDonationProduct("$3", FIDGET_DONATION_3_PRODUCT_ID),
    FidgetDonationProduct("$5", FIDGET_DONATION_5_PRODUCT_ID),
    FidgetDonationProduct("$10", FIDGET_DONATION_10_PRODUCT_ID),
)

private val FIDGET_DONATION_BADGE_COLORS = mapOf(
    FIDGET_DONATION_1_PRODUCT_ID to 0xFFB8FF00.toInt(),
    FIDGET_DONATION_3_PRODUCT_ID to 0xFF56F1C8.toInt(),
    FIDGET_DONATION_5_PRODUCT_ID to 0xFFFFC857.toInt(),
    FIDGET_DONATION_10_PRODUCT_ID to 0xFFFF2AD4.toInt(),
)

private fun fidgetDonationBadgeFor(donationCounts: Map<String, Int>): FidgetDonationBadge? {
    val highestDonation = FIDGET_DONATION_PRODUCTS
        .lastOrNull { donation -> donationCounts.getOrDefault(donation.productId, 0) > 0 }
        ?: return null
    val totalDonationCount = FIDGET_DONATION_PRODUCTS
        .sumOf { donation -> donationCounts.getOrDefault(donation.productId, 0) }
        .coerceIn(1, 5)
    return FidgetDonationBadge(
        label = highestDonation.label,
        count = totalDonationCount,
        colorArgb = FIDGET_DONATION_BADGE_COLORS.getValue(highestDonation.productId),
    )
}

@Composable
private fun FidgetTitleWithDonationBadge(
    title: String,
    badge: FidgetDonationBadge?,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp),
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
        )

        if (badge != null) {
            FidgetDonationBadgePill(
                badge = badge,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = 72.dp),
            )
        }
    }
}

@Composable
private fun FidgetDonationBadgePill(
    badge: FidgetDonationBadge,
    modifier: Modifier = Modifier,
) {
    val badgeColor = colorFromChoice(badge.colorArgb)
    val badgeText = if (badge.count > 1) {
        "${badge.label} x${badge.count}"
    } else {
        badge.label
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .width(if (badge.count > 1) 45.dp else 30.dp)
            .height(17.dp)
            .clip(RoundedCornerShape(50))
            .background(badgeColor.copy(alpha = 0.9f))
            .border(1.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(50)),
    ) {
        Text(
            text = badgeText,
            color = readableTextColorFor(badge.colorArgb),
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun FidgetMenuPage(
    appLanguage: AppLanguage,
    text: FidgetText,
    hapticFeedbackEnabled: Boolean,
    soundFeedbackEnabled: Boolean,
    feedbackSoundMode: BeatSoundMode,
    accentIntensityMode: AccentIntensityMode,
    keepScreenOn: Boolean,
    cpuPercentVisible: Boolean,
    cpuUsagePercent: Float?,
    mainColorArgb: Int,
    backgroundColorArgb: Int,
    ringColorArgb: Int,
    onReviewClick: () -> Unit,
    onDonateClick: () -> Unit,
    onDoneClick: () -> Unit,
    onKeepScreenToggle: () -> Unit,
    onCpuToggle: () -> Unit,
    onLanguageChoice: (AppLanguage) -> Unit,
    onAccentIntensityModeChoice: (AccentIntensityMode) -> Unit,
    onMainColorChoice: (Int) -> Unit,
    onBackgroundColorChoice: (Int) -> Unit,
    onRingColorChoice: (Int) -> Unit,
    onHapticToggle: () -> Unit,
    onSoundToggle: () -> Unit,
    onSoundModeChoice: (BeatSoundMode) -> Unit,
) {
    val settingsScrollState = rememberScrollState()
    val accentColor = colorFromChoice(mainColorArgb)

    LaunchedEffect(Unit) {
        settingsScrollState.scrollTo(0)
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val watchSClass = minOf(maxWidth, maxHeight) <= 200.dp
        val horizontalPadding = if (watchSClass) 14.dp else 18.dp
        val topPadding = if (watchSClass) 8.dp else 10.dp
        val sectionSpacing = if (watchSClass) 7.dp else 9.dp
        val tightSpacing = if (watchSClass) 4.dp else 5.dp
        val labelFontSize = if (watchSClass) 9.sp else 10.sp
        val scrollBarHeight = if (watchSClass) 104.dp else 132.dp

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(settingsScrollState)
                .padding(start = horizontalPadding, top = topPadding, end = horizontalPadding, bottom = 10.dp),
        ) {
            FidgetMenuSectionTitle(text.language, labelFontSize, accentColor)
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = tightSpacing),
            ) {
                AppLanguages.forEach { language ->
                    FidgetSettingsButton(
                        text = when (language) {
                            AppLanguage.English -> "EN"
                            AppLanguage.Spanish -> "ES"
                        },
                        selected = appLanguage == language,
                        accentColor = accentColor,
                        accentColorArgb = mainColorArgb,
                        onClick = { onLanguageChoice(language) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(sectionSpacing))

            Image(
                painter = painterResource(id = R.drawable.munkz_fidget_toy_logo),
                contentDescription = null,
                modifier = Modifier.size(44.dp),
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "Munkz",
                color = accentColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(sectionSpacing))

            FidgetMenuSectionTitle(text.links, labelFontSize, accentColor)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = tightSpacing),
            ) {
                FidgetSettingsButton(
                    text = text.review,
                    selected = false,
                    accentColor = accentColor,
                    accentColorArgb = mainColorArgb,
                    onClick = onReviewClick,
                )
                FidgetSettingsButton(
                    text = text.donate,
                    selected = false,
                    accentColor = accentColor,
                    accentColorArgb = mainColorArgb,
                    onClick = onDonateClick,
                )
            }

            Spacer(modifier = Modifier.height(sectionSpacing))

            FidgetMenuSectionTitle(text.feedback, labelFontSize, accentColor)
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = tightSpacing),
            ) {
                FidgetSettingsButton(
                    text = text.vibe,
                    selected = hapticFeedbackEnabled,
                    accentColor = accentColor,
                    accentColorArgb = mainColorArgb,
                    onClick = onHapticToggle,
                )
                FidgetSettingsButton(
                    text = text.sound,
                    selected = soundFeedbackEnabled,
                    accentColor = accentColor,
                    accentColorArgb = mainColorArgb,
                    onClick = onSoundToggle,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = tightSpacing),
            ) {
                FidgetSettingsButton(
                    text = text.click,
                    selected = feedbackSoundMode == BeatSoundMode.Clicks,
                    accentColor = accentColor,
                    accentColorArgb = mainColorArgb,
                    onClick = { onSoundModeChoice(BeatSoundMode.Clicks) },
                )
                FidgetSettingsButton(
                    text = text.wood,
                    selected = feedbackSoundMode == BeatSoundMode.Wood,
                    accentColor = accentColor,
                    accentColorArgb = mainColorArgb,
                    onClick = { onSoundModeChoice(BeatSoundMode.Wood) },
                )
                FidgetSettingsButton(
                    text = text.bell,
                    selected = feedbackSoundMode == BeatSoundMode.Bell,
                    accentColor = accentColor,
                    accentColorArgb = mainColorArgb,
                    onClick = { onSoundModeChoice(BeatSoundMode.Bell) },
                )
            }

            Spacer(modifier = Modifier.height(sectionSpacing))

            FidgetMenuSectionTitle(text.bigBeep, labelFontSize, accentColor)
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = tightSpacing),
            ) {
                AccentIntensityChoices.forEach { choice ->
                    FidgetMiniChoiceButton(
                        text = choice.labelFor(appLanguage),
                        selected = accentIntensityMode == choice.mode,
                        accentColor = accentColor,
                        accentColorArgb = mainColorArgb,
                        onClick = { onAccentIntensityModeChoice(choice.mode) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(sectionSpacing))

            FidgetMenuSectionTitle(text.watch, labelFontSize, accentColor)
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = tightSpacing),
            ) {
                FidgetSettingsButton(
                    text = if (keepScreenOn) text.keepOn else text.keepOff,
                    selected = keepScreenOn,
                    accentColor = accentColor,
                    accentColorArgb = mainColorArgb,
                    onClick = onKeepScreenToggle,
                )
            }

            Spacer(modifier = Modifier.height(sectionSpacing))

            FidgetMenuSectionTitle(text.theme, labelFontSize, accentColor)
            FidgetColorRow(
                label = text.mainColor,
                selectedColorArgb = mainColorArgb,
                choices = ThemeMainColorOptions,
                onColorChoice = onMainColorChoice,
            )
            FidgetColorRow(
                label = text.backgroundColor,
                selectedColorArgb = backgroundColorArgb,
                choices = ThemeBackgroundColorOptions,
                onColorChoice = onBackgroundColorChoice,
            )
            FidgetColorRow(
                label = text.ringColor,
                selectedColorArgb = ringColorArgb,
                choices = PulseColorOptions,
                onColorChoice = onRingColorChoice,
            )

            Spacer(modifier = Modifier.height(sectionSpacing))

            FidgetMenuSectionTitle(text.cpu, labelFontSize, accentColor)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = tightSpacing),
            ) {
                FidgetSettingsButton(
                    text = if (cpuPercentVisible) text.on else text.off,
                    selected = cpuPercentVisible,
                    accentColor = accentColor,
                    accentColorArgb = mainColorArgb,
                    onClick = onCpuToggle,
                )
                Text(
                    text = cpuUsagePercent.formatFidgetCpuPercent(),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(48.dp),
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }

        if (settingsScrollState.maxValue > 0) {
            FidgetMenuScrollBar(
                scrollState = settingsScrollState,
                accentColor = accentColor,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(top = 42.dp, end = 5.dp, bottom = 22.dp)
                    .width(4.dp)
                    .height(scrollBarHeight),
            )
        }

        FidgetThemeButton(
            text = text.done,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 2.dp, end = 7.dp)
                .rotate(38f)
                .width(58.dp)
                .height(24.dp),
            fontSize = 9.sp,
            selected = true,
            prominent = true,
            accentColor = accentColor,
            accentColorArgb = mainColorArgb,
            onClick = onDoneClick,
        )
    }
}

@Composable
private fun FidgetMenuScrollBar(
    scrollState: ScrollState,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(accentColor.copy(alpha = 0.16f)),
    ) {
        val maxScroll = scrollState.maxValue
        val progress = if (maxScroll > 0) {
            (scrollState.value.toFloat() / maxScroll.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        val thumbHeight = (maxHeight * 0.34f).coerceAtLeast(24.dp)
        val thumbOffset = (maxHeight - thumbHeight) * progress

        Box(
            modifier = Modifier
                .offset(y = thumbOffset)
                .fillMaxWidth()
                .height(thumbHeight)
                .clip(RoundedCornerShape(50))
                .background(accentColor.copy(alpha = 0.82f)),
        )
    }
}

@Composable
private fun FidgetMenuSectionTitle(
    text: String,
    fontSize: TextUnit,
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    Text(
        text = text,
        color = accentColor,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

private fun Boolean.thenCpuLabel(cpuUsagePercent: Float?): String {
    return if (this) {
        cpuUsagePercent.formatFidgetCpuPercent()
    } else {
        "--%"
    }
}

@Composable
private fun FidgetIntensityPopup(
    appLanguage: AppLanguage,
    text: FidgetText,
    selectedMode: AccentIntensityMode,
    onModeChoice: (AccentIntensityMode) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .width(150.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF061112))
                .border(1.dp, Color(0xFF56F1C8).copy(alpha = 0.72f), RoundedCornerShape(16.dp))
                .padding(horizontal = 9.dp, vertical = 10.dp),
        ) {
            Text(
                text = text.bigBeep,
                color = Color(0xFFFFC857),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                AccentIntensityChoices.forEach { choice ->
                    FidgetMiniChoiceButton(
                        text = choice.labelFor(appLanguage),
                        selected = selectedMode == choice.mode,
                        onClick = { onModeChoice(choice.mode) },
                    )
                }
            }
            Text(
                text = text.intensityHelp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
                fontSize = 8.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 7.dp),
            )
            FidgetMenuChoiceButton(
                text = text.done,
                selected = false,
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun FidgetColorPopup(
    text: FidgetText,
    mainColorArgb: Int,
    backgroundColorArgb: Int,
    ringColorArgb: Int,
    onMainColorChoice: (Int) -> Unit,
    onBackgroundColorChoice: (Int) -> Unit,
    onRingColorChoice: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .width(162.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF061112))
                .border(1.dp, Color(0xFF56F1C8).copy(alpha = 0.72f), RoundedCornerShape(16.dp))
                .padding(horizontal = 9.dp, vertical = 8.dp),
        ) {
            Text(
                text = text.colors,
                color = Color(0xFFFFC857),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            FidgetColorRow(
                label = text.mainColor,
                selectedColorArgb = mainColorArgb,
                choices = ThemeMainColorOptions,
                onColorChoice = onMainColorChoice,
            )
            FidgetColorRow(
                label = text.backgroundColor,
                selectedColorArgb = backgroundColorArgb,
                choices = ThemeBackgroundColorOptions,
                onColorChoice = onBackgroundColorChoice,
            )
            FidgetColorRow(
                label = text.ringColor,
                selectedColorArgb = ringColorArgb,
                choices = PulseColorOptions,
                onColorChoice = onRingColorChoice,
            )
            FidgetMenuChoiceButton(
                text = text.done,
                selected = false,
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun FidgetColorRow(
    label: String,
    selectedColorArgb: Int,
    choices: List<Int>,
    onColorChoice: (Int) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 6.dp),
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.86f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        choices.chunked(4).forEach { colorRow ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                colorRow.forEach { colorArgb ->
                    FidgetColorSwatch(
                        colorArgb = colorArgb,
                        selected = selectedColorArgb == colorArgb,
                        onClick = { onColorChoice(colorArgb) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FidgetColorSwatch(
    colorArgb: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(9.dp)
    val swatchColor = colorFromChoice(colorArgb)
    Box(
        modifier = Modifier
            .width(36.dp)
            .height(22.dp)
            .clip(shape)
            .then(
                if (isRainbowColor(colorArgb)) {
                    Modifier.background(Brush.horizontalGradient(RainbowColors), shape)
                } else {
                    Modifier.background(swatchColor, shape)
                },
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.onBackground
                } else {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.28f)
                },
                shape = shape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Text(
                text = "*",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = selectedSwatchMarkColor(colorArgb),
            )
        }
    }
}

@Composable
private fun FidgetMiniChoiceButton(
    text: String,
    selected: Boolean,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    accentColorArgb: Int = NEON_GREEN_COLOR,
    onClick: () -> Unit,
) {
    FidgetThemeButton(
        text = text,
        modifier = Modifier
            .width(29.dp)
            .height(24.dp),
        fontSize = 8.sp,
        selected = selected,
        prominent = selected,
        accentColor = accentColor,
        accentColorArgb = accentColorArgb,
        onClick = onClick,
    )
}

@Composable
private fun FidgetDonationPopup(
    text: FidgetText,
    statusText: String,
    onDonate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .width(154.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF061112))
                .border(1.dp, Color(0xFF56F1C8).copy(alpha = 0.72f), RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp, vertical = 11.dp),
        ) {
            Text(
                text = text.donate,
                color = Color(0xFFFFC857),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = statusText,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f),
                fontSize = 8.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.padding(top = 3.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                FidgetDonationChoiceButton(
                    text = FIDGET_DONATION_PRODUCTS[0].label,
                    selected = true,
                    onClick = { onDonate(FIDGET_DONATION_PRODUCTS[0].productId) },
                )
                FidgetDonationChoiceButton(
                    text = FIDGET_DONATION_PRODUCTS[1].label,
                    selected = false,
                    onClick = { onDonate(FIDGET_DONATION_PRODUCTS[1].productId) },
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 6.dp),
            ) {
                FidgetDonationChoiceButton(
                    text = FIDGET_DONATION_PRODUCTS[2].label,
                    selected = false,
                    onClick = { onDonate(FIDGET_DONATION_PRODUCTS[2].productId) },
                )
                FidgetDonationChoiceButton(
                    text = FIDGET_DONATION_PRODUCTS[3].label,
                    selected = false,
                    onClick = { onDonate(FIDGET_DONATION_PRODUCTS[3].productId) },
                )
            }
            FidgetMenuChoiceButton(
                text = text.done,
                selected = false,
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun FidgetDonationChoiceButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    GlassCommandButton(
        text = text,
        modifier = Modifier
            .width(50.dp)
            .height(24.dp),
        fontSize = 9.sp,
        selected = selected,
        prominent = selected,
        onClick = onClick,
    )
}

@Composable
private fun PlayStoreReviewPopup(
    text: FidgetText,
    statusText: String,
    onOpenReview: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.width(146.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF061112))
                    .border(1.dp, Color(0xFF56F1C8).copy(alpha = 0.72f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 12.dp),
            ) {
                Text(
                    text = text.addReviewTitle,
                    color = Color(0xFFFFC857),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = statusText.ifBlank { text.openPlayStore },
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.86f),
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 10.dp),
                ) {
                    FidgetMenuChoiceButton(
                        text = text.no,
                        selected = false,
                        onClick = onDismiss,
                    )
                    FidgetMenuChoiceButton(
                        text = text.review,
                        selected = true,
                        onClick = onOpenReview,
                    )
                }
            }
            Text(
                text = text.privacyPolicy,
                color = Color(0xFF56F1C8),
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenPrivacyPolicy)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun FidgetMenuChoiceButton(
    text: String,
    selected: Boolean,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    accentColorArgb: Int = NEON_GREEN_COLOR,
    onClick: () -> Unit,
) {
    FidgetThemeButton(
        text = text,
        modifier = Modifier
            .width(if (text.length > 5) 50.dp else 38.dp)
            .height(24.dp),
        fontSize = 8.sp,
        selected = selected,
        prominent = selected,
        accentColor = accentColor,
        accentColorArgb = accentColorArgb,
        onClick = onClick,
    )
}

@Composable
private fun FidgetSettingsButton(
    text: String,
    selected: Boolean,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    accentColorArgb: Int = NEON_GREEN_COLOR,
    onClick: () -> Unit,
) {
    FidgetThemeButton(
        text = text,
        modifier = Modifier
            .width(
                when {
                    text.length > 7 -> 64.dp
                    text.length > 5 -> 54.dp
                    else -> 44.dp
                },
            )
            .height(25.dp),
        fontSize = 8.sp,
        selected = selected,
        prominent = selected,
        accentColor = accentColor,
        accentColorArgb = accentColorArgb,
        onClick = onClick,
    )
}

@Composable
private fun FidgetThemeButton(
    text: String,
    modifier: Modifier,
    fontSize: TextUnit,
    selected: Boolean,
    prominent: Boolean,
    accentColor: Color,
    accentColorArgb: Int,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    val buttonColor = if (prominent) {
        accentColor.copy(alpha = if (selected) 0.92f else 0.78f)
    } else if (selected) {
        accentColor.copy(alpha = 0.34f)
    } else {
        accentColor.copy(alpha = 0.14f)
    }
    val borderColor = if (prominent || selected) {
        accentColor.copy(alpha = 0.95f)
    } else {
        accentColor.copy(alpha = 0.58f)
    }
    val textColor = if (prominent) {
        readableTextColorFor(accentColorArgb)
    } else {
        MaterialTheme.colorScheme.onBackground
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(buttonColor, shape)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

private fun Context.openFidgetPlayStoreListing() {
    val packageName = BuildConfig.APPLICATION_ID
    val marketIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=$packageName"),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val webIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://play.google.com/store/apps/details?id=$packageName&reviewId=0"),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    runCatching {
        startActivity(marketIntent)
    }.recoverCatching {
        startActivity(webIntent)
    }
}

private fun Context.isInstalledFromPlay(): Boolean {
    val installerPackageName = runCatching {
        packageManager.getInstallSourceInfo(packageName).installingPackageName
    }.getOrNull()
    return installerPackageName == "com.android.vending"
}

private fun Context.openFidgetPrivacyPolicy() {
    val policyIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse(FIDGET_PRIVACY_POLICY_URL),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    runCatching {
        startActivity(policyIntent)
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private fun Context.loadFidgetSettings(): FidgetSettingsState {
    val preferences = getSharedPreferences(FIDGET_SETTINGS_PREFS, Context.MODE_PRIVATE)
    val languageIndex = preferences.getInt(
        FIDGET_LANGUAGE_KEY,
        AppLanguages.indexOf(AppLanguage.English),
    )
    return FidgetSettingsState(
        mainColorArgb = preferences.getInt(FIDGET_MAIN_COLOR_KEY, NEON_GREEN_COLOR),
        backgroundColorArgb = preferences.getInt(FIDGET_BACKGROUND_COLOR_KEY, 0xFF061112.toInt()),
        ringColorArgb = preferences.getInt(FIDGET_RING_COLOR_KEY, NEON_GREEN_COLOR),
        hapticFeedbackEnabled = preferences.getBoolean(FIDGET_HAPTIC_ENABLED_KEY, true),
        soundFeedbackEnabled = preferences.getBoolean(FIDGET_SOUND_ENABLED_KEY, false),
        feedbackSoundMode = BeatSoundMode.fromPersistedValue(
            preferences.getInt(FIDGET_SOUND_MODE_KEY, BeatSoundMode.Clicks.persistedValue),
        ),
        accentIntensityMode = AccentIntensityMode.fromPersistedValue(
            preferences.getInt(FIDGET_ACCENT_INTENSITY_KEY, AccentIntensityMode.Big.persistedValue),
        ),
        appLanguage = AppLanguages.getOrElse(languageIndex) { AppLanguage.English },
        keepScreenOn = preferences.getBoolean(FIDGET_KEEP_SCREEN_ON_KEY, false),
        cpuPercentVisible = preferences.getBoolean(FIDGET_CPU_VISIBLE_KEY, false),
        pinnedToyIdsCsv = preferences.getString(FIDGET_PINNED_TOYS_KEY, "") ?: "",
    )
}

private fun Context.saveFidgetSettings(settings: FidgetSettingsState) {
    getSharedPreferences(FIDGET_SETTINGS_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putInt(FIDGET_MAIN_COLOR_KEY, settings.mainColorArgb)
        .putInt(FIDGET_BACKGROUND_COLOR_KEY, settings.backgroundColorArgb)
        .putInt(FIDGET_RING_COLOR_KEY, settings.ringColorArgb)
        .putBoolean(FIDGET_HAPTIC_ENABLED_KEY, settings.hapticFeedbackEnabled)
        .putBoolean(FIDGET_SOUND_ENABLED_KEY, settings.soundFeedbackEnabled)
        .putInt(FIDGET_SOUND_MODE_KEY, settings.feedbackSoundMode.persistedValue)
        .putInt(FIDGET_ACCENT_INTENSITY_KEY, settings.accentIntensityMode.persistedValue)
        .putInt(FIDGET_LANGUAGE_KEY, AppLanguages.indexOf(settings.appLanguage).coerceAtLeast(0))
        .putBoolean(FIDGET_KEEP_SCREEN_ON_KEY, settings.keepScreenOn)
        .putBoolean(FIDGET_CPU_VISIBLE_KEY, settings.cpuPercentVisible)
        .putString(FIDGET_PINNED_TOYS_KEY, settings.pinnedToyIdsCsv)
        .apply()
}

private fun Context.saveFidgetPinnedToyIds(pinnedToyIdsCsv: String) {
    getSharedPreferences(FIDGET_SETTINGS_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(FIDGET_PINNED_TOYS_KEY, pinnedToyIdsCsv)
        .apply()
}

private fun Context.loadFidgetDonationCounts(): Map<String, Int> {
    val preferences = getSharedPreferences(FIDGET_SETTINGS_PREFS, Context.MODE_PRIVATE)
    return FIDGET_DONATION_PRODUCTS.associate { donation ->
        donation.productId to preferences.getInt(donation.donationCountKey(), 0).coerceAtLeast(0)
    }
}

private fun Context.recordFidgetDonation(productId: String): Map<String, Int> {
    if (FIDGET_DONATION_PRODUCTS.none { donation -> donation.productId == productId }) {
        return loadFidgetDonationCounts()
    }
    val preferences = getSharedPreferences(FIDGET_SETTINGS_PREFS, Context.MODE_PRIVATE)
    val donation = FIDGET_DONATION_PRODUCTS.first { it.productId == productId }
    val updatedCount = (preferences.getInt(donation.donationCountKey(), 0) + 1)
        .coerceAtMost(5)
    preferences.edit()
        .putInt(donation.donationCountKey(), updatedCount)
        .apply()
    return loadFidgetDonationCounts()
}

private fun FidgetDonationProduct.donationCountKey(): String {
    return "$FIDGET_DONATION_COUNT_PREFIX$productId"
}

private fun String.toPinnedToyIds(): List<Int> {
    val builtToyIds = FIDGET_TOY_INFOS.map { it.id }.toSet()
    return split(",")
        .mapNotNull { value -> value.trim().toIntOrNull() }
        .filter { toyId -> toyId in builtToyIds }
        .distinct()
        .sortedWith(fidgetToyOrderComparator())
}

private fun fidgetToyOrderComparator(): Comparator<Int> {
    val order = FIDGET_TOY_INFOS.mapIndexed { index, info -> info.id to index }.toMap()
    return compareBy { toyId: Int -> order[toyId] ?: Int.MAX_VALUE }
}

private fun fidgetPageOrderFor(pinnedToyIds: List<Int>): List<Int> {
    val pinnedSet = pinnedToyIds.toSet()
    val unpinnedToyIds = FIDGET_TOY_INFOS
        .map { it.id }
        .filterNot { toyId -> toyId in pinnedSet }
    return listOf(FIDGET_WALL_INDEX) + pinnedToyIds + unpinnedToyIds
}

private fun Int.wrapFidgetIndex(size: Int): Int {
    if (size <= 0) return 0
    return ((this % size) + size) % size
}

private fun donationThanksTextFor(productId: String, language: AppLanguage): String {
    val label = FIDGET_DONATION_PRODUCTS.firstOrNull { it.productId == productId }?.label
        ?: "that"
    return fidgetTextFor(language).thanksFor(label)
}

private fun randomOpenWhackPosition(
    currentPosition: Int,
    occupiedPositions: Set<Int>,
): Int {
    val choices = (0 until SWITCH_MAZE_CELL_COUNT)
        .filter { position -> position != currentPosition && position !in occupiedPositions }
    if (choices.isEmpty()) return currentPosition
    return choices[Random.nextInt(choices.size)]
}

private fun generateFidgetMazePuzzle(): FidgetMazePuzzle {
    val openings = MutableList(FIDGET_MAZE_CELL_COUNT) { 0 }
    val visited = BooleanArray(FIDGET_MAZE_CELL_COUNT)
    val stack = mutableListOf(Random.nextInt(FIDGET_MAZE_CELL_COUNT))
    visited[stack.last()] = true

    while (stack.isNotEmpty()) {
        val currentCell = stack.last()
        val column = currentCell % FIDGET_MAZE_COLUMNS
        val row = currentCell / FIDGET_MAZE_COLUMNS
        val neighbors = buildList {
            if (row > 0) add(MazeDirection.Up to currentCell - FIDGET_MAZE_COLUMNS)
            if (column < FIDGET_MAZE_COLUMNS - 1) add(MazeDirection.Right to currentCell + 1)
            if (row < FIDGET_MAZE_ROWS - 1) add(MazeDirection.Down to currentCell + FIDGET_MAZE_COLUMNS)
            if (column > 0) add(MazeDirection.Left to currentCell - 1)
        }.filter { (_, nextCell) -> !visited[nextCell] }

        if (neighbors.isEmpty()) {
            stack.removeAt(stack.lastIndex)
        } else {
            val (direction, nextCell) = neighbors[Random.nextInt(neighbors.size)]
            openings[currentCell] = openings[currentCell] or direction.openMask()
            openings[nextCell] = openings[nextCell] or direction.opposite().openMask()
            visited[nextCell] = true
            stack += nextCell
        }
    }

    val startCell = Random.nextInt(FIDGET_MAZE_CELL_COUNT)
    var endCell = Random.nextInt(FIDGET_MAZE_CELL_COUNT)
    while (endCell == startCell) {
        endCell = Random.nextInt(FIDGET_MAZE_CELL_COUNT)
    }
    return FidgetMazePuzzle(
        openings = openings,
        startCell = startCell,
        endCell = endCell,
    )
}

private fun MazeDirection.openMask(): Int {
    return when (this) {
        MazeDirection.Up -> MAZE_OPEN_UP
        MazeDirection.Right -> MAZE_OPEN_RIGHT
        MazeDirection.Down -> MAZE_OPEN_DOWN
        MazeDirection.Left -> MAZE_OPEN_LEFT
    }
}

private fun MazeDirection.opposite(): MazeDirection {
    return when (this) {
        MazeDirection.Up -> MazeDirection.Down
        MazeDirection.Right -> MazeDirection.Left
        MazeDirection.Down -> MazeDirection.Up
        MazeDirection.Left -> MazeDirection.Right
    }
}

private fun Offset.toMazeDirection(): MazeDirection? {
    if (vectorLength() < 12f) return null
    return if (abs(x) > abs(y)) {
        if (x > 0f) MazeDirection.Right else MazeDirection.Left
    } else {
        if (y > 0f) MazeDirection.Down else MazeDirection.Up
    }
}

private fun isFibonacciReward(count: Int): Boolean {
    if (count <= 0) return false
    var previous = 1
    var current = 1
    while (current < count) {
        val next = previous + current
        previous = current
        current = next
    }
    return current == count
}

private fun nextFibonacciTarget(count: Int): Int {
    if (count < 1) return 1
    var previous = 1
    var current = 1
    while (current <= count) {
        val next = previous + current
        previous = current
        current = next
    }
    return current
}

@Composable
private fun rememberFidgetCpuUsagePercent(enabled: Boolean): Float? {
    val sampler = remember { FidgetCpuSampler() }
    var cpuUsagePercent by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(enabled) {
        if (!enabled) {
            cpuUsagePercent = null
            return@LaunchedEffect
        }

        sampler.reset()
        while (true) {
            delay(1_000L)
            cpuUsagePercent = sampler.sample()
        }
    }

    return cpuUsagePercent
}

private class FidgetCpuSampler {
    private var previousWallNanos = 0L
    private var previousCpuMillis = 0L

    fun reset() {
        previousWallNanos = 0L
        previousCpuMillis = 0L
    }

    fun sample(): Float? {
        val wallNanos = System.nanoTime()
        val cpuMillis = android.os.Process.getElapsedCpuTime()
        if (previousWallNanos == 0L) {
            previousWallNanos = wallNanos
            previousCpuMillis = cpuMillis
            return null
        }

        val wallMillis = (wallNanos - previousWallNanos) / 1_000_000f
        val cpuDeltaMillis = (cpuMillis - previousCpuMillis).toFloat()
        previousWallNanos = wallNanos
        previousCpuMillis = cpuMillis
        if (wallMillis <= 0f) return null
        return (cpuDeltaMillis / wallMillis * 100f).coerceIn(0f, 100f)
    }
}

private fun Float?.formatFidgetCpuPercent(): String {
    if (this == null) return "--%"
    return "${roundToInt().coerceIn(0, 100)}%"
}

private fun AccentIntensityMode.feedbackVolume(): Float {
    return when (this) {
        AccentIntensityMode.Big -> 0.95f
        AccentIntensityMode.Medium -> 0.72f
        AccentIntensityMode.Little -> 0.52f
        AccentIntensityMode.Silent -> 0.2f
    }
}

private fun AccentIntensityMode.feedbackDurationMs(): Int {
    return when (this) {
        AccentIntensityMode.Big -> 44
        AccentIntensityMode.Medium -> 34
        AccentIntensityMode.Little -> 24
        AccentIntensityMode.Silent -> 14
    }
}

private fun AccentIntensityMode.feedbackVibrationMs(): Long {
    return when (this) {
        AccentIntensityMode.Big -> 28L
        AccentIntensityMode.Medium -> 18L
        AccentIntensityMode.Little -> 11L
        AccentIntensityMode.Silent -> 6L
    }
}

private fun AccentIntensityMode.feedbackVibrationAmplitude(): Int {
    return when (this) {
        AccentIntensityMode.Big -> VibrationEffect.DEFAULT_AMPLITUDE
        AccentIntensityMode.Medium -> 156
        AccentIntensityMode.Little -> 84
        AccentIntensityMode.Silent -> 32
    }
}

@Composable
private fun FidgetSelectionWallPage(
    pinnedToyIds: List<Int>,
    accentColor: Color,
    accentColorArgb: Int,
    onToySelected: (Int) -> Unit,
    onPinToggle: (Int) -> Unit,
) {
    val scrollState = rememberScrollState()
    val pinnedSet = pinnedToyIds.toSet()
    val builtByStyle = FIDGET_TOY_INFOS.groupBy { it.style }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(end = 5.dp, bottom = 4.dp),
        ) {
            Text(
                text = "Toy Wall",
                color = accentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (pinnedToyIds.isEmpty()) "Pin favorites to lead the scroll" else "Pinned lead the scroll",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            if (pinnedToyIds.isNotEmpty()) {
                FidgetWallSectionTitle("Pinned", accentColor)
                pinnedToyIds.mapNotNull { toyId ->
                    FIDGET_TOY_INFOS.firstOrNull { it.id == toyId }
                }.forEach { toy ->
                    FidgetWallToyRow(
                        toy = toy,
                        pinned = true,
                        enabled = true,
                        accentColor = accentColor,
                        accentColorArgb = accentColorArgb,
                        onToySelected = onToySelected,
                        onPinToggle = onPinToggle,
                    )
                }
            }

            builtByStyle.forEach { (style, toys) ->
                FidgetWallSectionTitle(style, accentColor)
                toys.forEach { toy ->
                    FidgetWallToyRow(
                        toy = toy,
                        pinned = toy.id in pinnedSet,
                        enabled = true,
                        accentColor = accentColor,
                        accentColorArgb = accentColorArgb,
                        onToySelected = onToySelected,
                        onPinToggle = onPinToggle,
                    )
                }
            }
        }

        FidgetMenuScrollBar(
            scrollState = scrollState,
            accentColor = accentColor,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(4.dp)
                .height(88.dp),
        )
    }
}

@Composable
private fun FidgetWallSectionTitle(
    text: String,
    accentColor: Color,
) {
    Text(
        text = text,
        color = accentColor.copy(alpha = 0.9f),
        fontSize = 9.sp,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
    )
}

@Composable
private fun FidgetWallToyRow(
    toy: FidgetToyInfo,
    pinned: Boolean,
    enabled: Boolean,
    accentColor: Color,
    accentColorArgb: Int,
    onToySelected: (Int) -> Unit,
    onPinToggle: (Int) -> Unit,
) {
    val rowAlpha = if (enabled) 1f else 0.5f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        FidgetThemeButton(
            text = toy.name,
            modifier = Modifier
                .weight(1f)
                .height(25.dp),
            fontSize = 9.sp,
            selected = pinned,
            prominent = enabled && pinned,
            accentColor = accentColor.copy(alpha = rowAlpha),
            accentColorArgb = accentColorArgb,
            onClick = {
                if (enabled) {
                    onToySelected(toy.id)
                }
            },
        )
        FidgetThemeButton(
            text = if (enabled) {
                if (pinned) "Pinned" else "Pin"
            } else {
                "Soon"
            },
            modifier = Modifier
                .width(if (enabled && pinned) 44.dp else 36.dp)
                .height(25.dp),
            fontSize = 8.sp,
            selected = pinned,
            prominent = enabled && pinned,
            accentColor = accentColor.copy(alpha = rowAlpha),
            accentColorArgb = accentColorArgb,
            onClick = {
                if (enabled) {
                    onPinToggle(toy.id)
                }
            },
        )
    }
}

@Composable
private fun FidgetSpinner(rotation: Float) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .rotate(rotation),
    ) {
        repeat(3) { index ->
            val angle = Math.toRadians((index * 120).toDouble())
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (cos(angle) * 27.dp.toPx()).roundToInt(),
                            y = (sin(angle) * 27.dp.toPx()).roundToInt(),
                        )
                    }
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFC857),
                                Color(0xFFEF476F),
                                Color(0xFF3A0B18),
                            ),
                        ),
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.44f), CircleShape),
            )
        }
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFF56F1C8))
                .border(3.dp, Color.Black.copy(alpha = 0.42f), CircleShape),
        )
    }
}

@Composable
private fun SwitchFidgetToy(
    switchMask: Int,
    onSwitchToggle: (Int) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .width(128.dp)
            .height(118.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(4) { index ->
                FidgetToggleSwitch(
                    switchedOn = switchMask and (1 shl index) != 0,
                    onClick = { onSwitchToggle(index) },
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 10.dp),
        ) {
            repeat(4) { index ->
                val switchIndex = index + 4
                FidgetToggleSwitch(
                    switchedOn = switchMask and (1 shl switchIndex) != 0,
                    onClick = { onSwitchToggle(switchIndex) },
                )
            }
        }
    }
}

@Composable
private fun SwitchMazeToy(
    mazePosition: Int,
    onMove: (deltaColumn: Int, deltaRow: Int) -> Unit,
) {
    val column = mazePosition % SWITCH_MAZE_COLUMNS
    val row = mazePosition / SWITCH_MAZE_COLUMNS
    val buttonColor = if ((row + column) % 2 == 0) Color(0xFFFFC857) else Color(0xFF56F1C8)

    Box(
        modifier = Modifier
            .size(118.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color(0xFF56F1C8).copy(alpha = 0.72f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val paddingPx = 12.dp.toPx()
            val cellWidth = (size.width - paddingPx * 2f) / SWITCH_MAZE_COLUMNS
            val cellHeight = (size.height - paddingPx * 2f) / SWITCH_MAZE_ROWS

            repeat(SWITCH_MAZE_COLUMNS + 1) { lineIndex ->
                val x = paddingPx + lineIndex * cellWidth
                drawLine(
                    color = Color(0xFF56F1C8).copy(alpha = 0.22f),
                    start = androidx.compose.ui.geometry.Offset(x, paddingPx),
                    end = androidx.compose.ui.geometry.Offset(x, size.height - paddingPx),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            repeat(SWITCH_MAZE_ROWS + 1) { lineIndex ->
                val y = paddingPx + lineIndex * cellHeight
                drawLine(
                    color = Color(0xFFFFC857).copy(alpha = 0.18f),
                    start = androidx.compose.ui.geometry.Offset(paddingPx, y),
                    end = androidx.compose.ui.geometry.Offset(size.width - paddingPx, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }
        }

        MazeStepButton(
            color = buttonColor,
            modifier = Modifier.offset {
                val cellStepPx = 23.dp.toPx()
                IntOffset(
                    x = ((column - 1.5f) * cellStepPx).roundToInt(),
                    y = ((row - 1.5f) * cellStepPx).roundToInt(),
                )
            },
            onMove = onMove,
        )
    }
}

@Composable
private fun MazeStepButton(
    color: Color,
    modifier: Modifier = Modifier,
    onMove: (deltaColumn: Int, deltaRow: Int) -> Unit,
) {
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.42f),
                        color,
                        color.copy(alpha = 0.46f),
                        Color.Black.copy(alpha = 0.24f),
                    ),
                ),
            )
            .border(2.dp, Color.White.copy(alpha = 0.44f), CircleShape)
            .pointerInput(Unit) {
                var dragX = 0f
                var dragY = 0f
                detectDragGestures(
                    onDragStart = {
                        dragX = 0f
                        dragY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragX += dragAmount.x
                        dragY += dragAmount.y
                    },
                    onDragEnd = {
                        if (abs(dragX) > abs(dragY)) {
                            when {
                                dragX > 6f -> onMove(1, 0)
                                dragX < -6f -> onMove(-1, 0)
                            }
                        } else {
                            when {
                                dragY > 6f -> onMove(0, 1)
                                dragY < -6f -> onMove(0, -1)
                            }
                        }
                    },
                    onDragCancel = {
                        dragX = 0f
                        dragY = 0f
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.5f)),
        )
    }
}

@Composable
private fun FidgetToggleSwitch(
    switchedOn: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val trackColor = if (switchedOn) Color(0xFF56F1C8) else Color(0xFF1B2428)
    val knobColor = if (switchedOn) Color(0xFFFFC857) else Color(0xFF8D98A0)
    val knobAlignment = if (switchedOn) Alignment.TopCenter else Alignment.BottomCenter

    Box(
        modifier = modifier
            .width(24.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        trackColor.copy(alpha = if (switchedOn) 0.78f else 0.46f),
                        Color.Black.copy(alpha = 0.34f),
                    ),
                ),
                shape = RoundedCornerShape(12.dp),
            )
            .border(
                width = 1.dp,
                color = if (switchedOn) Color(0xFFFFC857) else Color(0xFF56F1C8).copy(alpha = 0.42f),
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(3.dp),
        contentAlignment = knobAlignment,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(knobColor)
                .border(1.dp, Color.White.copy(alpha = 0.42f), CircleShape),
        )
    }
}

@Composable
private fun FreeMoveButtonToy(
    buttonPositions: List<Offset>,
    onButtonDragStart: () -> Unit,
    onButtonMove: (index: Int, delta: Offset) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(118.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color(0xFF56F1C8).copy(alpha = 0.72f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val paddingPx = 6.dp.toPx()
            repeat(9) { lineIndex ->
                val progress = lineIndex / 8f
                val x = paddingPx + (size.width - paddingPx * 2f) * progress
                val y = paddingPx + (size.height - paddingPx * 2f) * progress
                drawLine(
                    color = Color(0xFF56F1C8).copy(alpha = 0.16f),
                    start = Offset(x, paddingPx),
                    end = Offset(x, size.height - paddingPx),
                    strokeWidth = 1.dp.toPx(),
                )
                drawLine(
                    color = Color(0xFFFFC857).copy(alpha = 0.13f),
                    start = Offset(paddingPx, y),
                    end = Offset(size.width - paddingPx, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }
        }

        buttonPositions.forEachIndexed { index, position ->
            FreeMoveButton(
                index = index,
                position = position,
                onDragStart = onButtonDragStart,
                onDrag = { delta -> onButtonMove(index, delta) },
            )
        }
    }
}

@Composable
private fun FreeMoveButton(
    index: Int,
    position: Offset,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
) {
    val density = LocalDensity.current
    val colors = listOf(
        Color(0xFFFFC857),
        Color(0xFF56F1C8),
        Color(0xFFEF476F),
        Color(0xFF8D6BFF),
    )
    val color = colors[index % colors.size]

    Box(
        modifier = Modifier
            .offset(x = position.x.dp, y = position.y.dp)
            .size(28.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color,
                        color.copy(alpha = 0.52f),
                        Color.Black.copy(alpha = 0.24f),
                    ),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.48f), CircleShape)
            .pointerInput(index) {
                detectDragGestures(
                    onDragStart = {
                        onDragStart()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(
                            with(density) {
                                Offset(
                                    x = dragAmount.x.toDp().value,
                                    y = dragAmount.y.toDp().value,
                                )
                            }
                        )
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.45f)),
        )
    }
}

@Composable
private fun SquishyFidgetToy(
    pullOffset: Offset,
    pressure: Float,
    accentColor: Color,
    onPress: () -> Unit,
    onPull: (Offset, Float) -> Unit,
    onRelease: () -> Unit,
) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .size(118.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, accentColor.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                var dragOffset = Offset.Zero
                detectDragGestures(
                    onDragStart = {
                        dragOffset = Offset.Zero
                        onPress()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset = with(density) {
                            Offset(
                                x = dragOffset.x + dragAmount.x.toDp().value,
                                y = dragOffset.y + dragAmount.y.toDp().value,
                            )
                        }.limitedToLength(SQUISHY_PULL_LIMIT_DP)
                        onPull(
                            dragOffset,
                            (0.35f + dragOffset.vectorLength() / SQUISHY_PULL_LIMIT_DP).coerceIn(0f, 1f),
                        )
                    },
                    onDragEnd = {
                        onRelease()
                        dragOffset = Offset.Zero
                    },
                    onDragCancel = {
                        onRelease()
                        dragOffset = Offset.Zero
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val pullPx = Offset(pullOffset.x.dp.toPx(), pullOffset.y.dp.toPx())
            val pressureClamped = pressure.coerceIn(0f, 1f)
            val stretch = (pullOffset.vectorLength() / SQUISHY_PULL_LIMIT_DP).coerceIn(0f, 1f)
            val blobCenter = center + pullPx * 0.28f
            val blobWidth = 62.dp.toPx() + stretch * 18.dp.toPx() - pressureClamped * 7.dp.toPx()
            val blobHeight = 62.dp.toPx() - stretch * 8.dp.toPx() + pressureClamped * 12.dp.toPx()

            drawCircle(
                color = accentColor.copy(alpha = 0.16f),
                radius = 47.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )
            drawLine(
                color = Color(0xFFFFC857).copy(alpha = 0.18f + stretch * 0.34f),
                start = center,
                end = blobCenter,
                strokeWidth = (1.dp + (stretch * 3f).dp).toPx(),
            )
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.58f),
                        Color(0xFFFFC857).copy(alpha = 0.92f),
                        accentColor.copy(alpha = 0.8f),
                        Color(0xFFEF476F).copy(alpha = 0.82f),
                        Color.Black.copy(alpha = 0.25f),
                    ),
                    center = blobCenter + Offset(-11.dp.toPx(), -13.dp.toPx()),
                    radius = 58.dp.toPx(),
                ),
                topLeft = Offset(
                    x = blobCenter.x - blobWidth / 2f,
                    y = blobCenter.y - blobHeight / 2f,
                ),
                size = Size(blobWidth, blobHeight),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.34f + pressureClamped * 0.18f),
                radius = 8.dp.toPx() + pressureClamped * 4.dp.toPx(),
                center = blobCenter + Offset(-13.dp.toPx(), -15.dp.toPx()),
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.13f),
                radius = 23.dp.toPx() + pressureClamped * 4.dp.toPx(),
                center = blobCenter + Offset(8.dp.toPx(), 11.dp.toPx()),
                style = Stroke(width = 2.dp.toPx() + pressureClamped * 1.dp.toPx()),
            )
        }
    }
}

@Composable
private fun MagSnapFidgetToy(
    position: Int,
    onMove: (Int) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(118.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color(0xFF56F1C8).copy(alpha = 0.72f), RoundedCornerShape(16.dp))
            .pointerInput(position) {
                var dragX = 0f
                detectDragGestures(
                    onDragStart = { dragX = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragX += dragAmount.x
                    },
                    onDragEnd = {
                        when {
                            dragX > 9f -> onMove(1)
                            dragX < -9f -> onMove(-1)
                        }
                    },
                    onDragCancel = { dragX = 0f },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawLine(
                color = Color(0xFF56F1C8).copy(alpha = 0.42f),
                start = center + Offset(-38.dp.toPx(), 0f),
                end = center + Offset(38.dp.toPx(), 0f),
                strokeWidth = 5.dp.toPx(),
            )
            repeat(3) { index ->
                val x = center.x + (index - 1) * 34.dp.toPx()
                drawCircle(
                    color = Color(0xFFFFC857).copy(alpha = if (index == position) 0.7f else 0.28f),
                    radius = 10.dp.toPx(),
                    center = Offset(x, center.y),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
        Box(
            modifier = Modifier
                .offset(x = ((position - 1) * 34).dp)
                .size(34.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.62f), Color(0xFF56F1C8), Color(0xFF122A2A)),
                    ),
                )
                .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                .clickable { onMove(if (position < 2) 1 else -2) },
        )
    }
}

@Composable
private fun PopGridFidgetToy(
    popMask: Int,
    onPop: (Int) -> Unit,
    onReset: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(118.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color(0xFF56F1C8).copy(alpha = 0.72f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            repeat(3) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(4) { column ->
                        val index = row * 4 + column
                        val popped = popMask and (1 shl index) != 0
                        Box(
                            modifier = Modifier
                                .size(23.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            if (popped) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.5f),
                                            if (popped) Color(0xFF243033) else Color(0xFFFFC857),
                                            if (popped) Color(0xFF56F1C8).copy(alpha = 0.4f) else Color(0xFFEF476F),
                                        ),
                                    ),
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.38f), CircleShape)
                                .clickable { onPop(index) },
                        )
                    }
                }
            }
        }
        FidgetThemeButton(
            text = "R",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 4.dp)
                .size(width = 22.dp, height = 20.dp),
            fontSize = 8.sp,
            selected = true,
            prominent = true,
            accentColor = Color(0xFF56F1C8),
            accentColorArgb = 0xFF56F1C8.toInt(),
            onClick = onReset,
        )
    }
}

@Composable
private fun InfinityFlipFidgetToy(
    fold: Int,
    onFlip: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(118.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color(0xFF56F1C8).copy(alpha = 0.72f), RoundedCornerShape(16.dp))
            .clickable(onClick = onFlip),
        contentAlignment = Alignment.Center,
    ) {
        repeat(4) { index ->
            val column = index % 2
            val row = index / 2
            val open = (fold + index) % 4
            Box(
                modifier = Modifier
                    .offset(
                        x = ((column - 0.5f) * (34 + open * 3)).dp,
                        y = ((row - 0.5f) * (34 + (3 - open) * 3)).dp,
                    )
                    .rotate((fold * 18f + index * 7f) % 45f)
                    .size(34.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                        listOf(
                            Color(0xFFFFC857),
                            Color(0xFF56F1C8),
                            Color(0xFFEF476F),
                            Color(0xFF8D6BFF),
                        )[index],
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.44f), RoundedCornerShape(7.dp)),
            )
        }
    }
}

@Composable
private fun RatchetRingFidgetToy(
    step: Int,
    onStep: (Int) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(118.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color(0xFF56F1C8).copy(alpha = 0.72f), CircleShape)
            .pointerInput(step) {
                var drag = Offset.Zero
                detectDragGestures(
                    onDragStart = { drag = Offset.Zero },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        drag += dragAmount
                    },
                    onDragEnd = {
                        if (drag.vectorLength() > 7f) {
                            onStep(if (drag.x + drag.y >= 0f) 1 else -1)
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            repeat(RATCHET_STEP_COUNT) { index ->
                val angle = (index * 360f / RATCHET_STEP_COUNT - 90f) * PI.toFloat() / 180f
                val inner = center + Offset(cos(angle), sin(angle)) * 38.dp.toPx()
                val outer = center + Offset(cos(angle), sin(angle)) * 48.dp.toPx()
                drawLine(
                    color = if (index == step) Color(0xFFFFC857) else Color(0xFF56F1C8).copy(alpha = 0.38f),
                    start = inner,
                    end = outer,
                    strokeWidth = if (index == step) 3.dp.toPx() else 1.dp.toPx(),
                )
            }
            val pointerAngle = (step * 360f / RATCHET_STEP_COUNT - 90f) * PI.toFloat() / 180f
            drawLine(
                color = Color(0xFFFFC857),
                start = center,
                end = center + Offset(cos(pointerAngle), sin(pointerAngle)) * 32.dp.toPx(),
                strokeWidth = 3.dp.toPx(),
            )
            drawCircle(Color(0xFF56F1C8), 12.dp.toPx(), center)
        }
    }
}

@Composable
private fun LiquidMazeFidgetToy(
    blobPosition: Offset,
    onMove: (Offset) -> Unit,
    onRelease: () -> Unit,
) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .size(118.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color(0xFF56F1C8).copy(alpha = 0.72f), RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onMove(with(density) { Offset(dragAmount.x.toDp().value, dragAmount.y.toDp().value) })
                    },
                    onDragEnd = onRelease,
                    onDragCancel = onRelease,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val wallColor = Color(0xFF56F1C8).copy(alpha = 0.42f)
            drawLine(wallColor, Offset(25.dp.toPx(), 28.dp.toPx()), Offset(91.dp.toPx(), 28.dp.toPx()), 2.dp.toPx())
            drawLine(wallColor, Offset(25.dp.toPx(), 58.dp.toPx()), Offset(76.dp.toPx(), 58.dp.toPx()), 2.dp.toPx())
            drawLine(wallColor, Offset(42.dp.toPx(), 86.dp.toPx()), Offset(95.dp.toPx(), 86.dp.toPx()), 2.dp.toPx())
            drawLine(wallColor, Offset(42.dp.toPx(), 28.dp.toPx()), Offset(42.dp.toPx(), 58.dp.toPx()), 2.dp.toPx())
            drawLine(wallColor, Offset(76.dp.toPx(), 58.dp.toPx()), Offset(76.dp.toPx(), 86.dp.toPx()), 2.dp.toPx())
            val blobCenter = center + Offset(blobPosition.x.dp.toPx(), blobPosition.y.dp.toPx())
            drawCircle(Color(0xFF56F1C8).copy(alpha = 0.34f), 20.dp.toPx(), blobCenter)
            drawCircle(Color(0xFFFFC857).copy(alpha = 0.85f), 13.dp.toPx(), blobCenter)
            drawCircle(Color.White.copy(alpha = 0.52f), 5.dp.toPx(), blobCenter + Offset(-4.dp.toPx(), -5.dp.toPx()))
        }
    }
}

@Composable
private fun GearJamFidgetToy(
    rotation: Float,
    onTurn: (Float) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(118.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color(0xFF56F1C8).copy(alpha = 0.72f), RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onTurn((dragAmount.x + dragAmount.y) * 0.8f)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        GearShape(Offset(-27f, 0f), 28.dp, rotation, Color(0xFFFFC857))
        GearShape(Offset(17f, -5f), 23.dp, -rotation * 1.3f, Color(0xFF56F1C8))
        GearShape(Offset(10f, 34f), 18.dp, rotation * 1.7f, Color(0xFFEF476F))
    }
}

@Composable
private fun GearShape(
    offset: Offset,
    size: Dp,
    rotation: Float,
    color: Color,
) {
    Box(
        modifier = Modifier
            .offset(x = offset.x.dp, y = offset.y.dp)
            .size(size)
            .rotate(rotation),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            repeat(8) { index ->
                val angle = index * PI.toFloat() / 4f
                drawLine(
                    color = color.copy(alpha = 0.85f),
                    start = center,
                    end = center + Offset(cos(angle), sin(angle)) * this.size.minDimension * 0.48f,
                    strokeWidth = 4.dp.toPx(),
                )
            }
            drawCircle(color, this.size.minDimension * 0.34f, center)
            drawCircle(Color.Black.copy(alpha = 0.42f), this.size.minDimension * 0.13f, center)
        }
    }
}

@Composable
private fun WorryStoneFidgetToy(
    rub: Float,
    accentColor: Color,
    onRub: (Offset) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(118.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, accentColor.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onRub(dragAmount)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawOval(
                brush = Brush.radialGradient(
                    listOf(
                        Color.White.copy(alpha = 0.35f + rub * 0.24f),
                        accentColor.copy(alpha = 0.78f),
                        Color(0xFF101418),
                    ),
                    center = center + Offset(-14.dp.toPx(), -18.dp.toPx()),
                    radius = 70.dp.toPx(),
                ),
                topLeft = center + Offset(-43.dp.toPx(), -34.dp.toPx()),
                size = Size(86.dp.toPx(), 68.dp.toPx()),
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.16f),
                radius = 16.dp.toPx() + rub * 6.dp.toPx(),
                center = center + Offset(8.dp.toPx(), 4.dp.toPx()),
                style = Stroke(width = 3.dp.toPx()),
            )
        }
    }
}

@Composable
private fun KeyClicksFidgetToy(
    keyMask: Int,
    onKeyPress: (Int) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(118.dp)
            .height(104.dp),
    ) {
        repeat(3) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { column ->
                    val index = row * 3 + column
                    val down = keyMask and (1 shl index) != 0
                    Box(
                        modifier = Modifier
                            .size(width = 31.dp, height = 25.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (down) Color(0xFF56F1C8) else Color(0xFF20272B))
                            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                            .clickable { onKeyPress(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = listOf("C", "K", "T", "M", "Z", "B", "P", "D", "R")[index],
                            color = if (down) Color.Black else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZenTraceFidgetToy(
    tracePoints: List<Offset>,
    accentColor: Color,
    onTrace: (Offset) -> Unit,
    onTraceStart: () -> Unit,
    onClear: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(118.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, accentColor.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        onTraceStart()
                        onTrace(it)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        onTrace(change.position)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            repeat(5) { index ->
                val y = 22.dp.toPx() + index * 17.dp.toPx()
                drawLine(
                    color = Color(0xFFFFC857).copy(alpha = 0.16f),
                    start = Offset(18.dp.toPx(), y),
                    end = Offset(size.width - 18.dp.toPx(), y),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            tracePoints.zipWithNext().forEachIndexed { index, (start, end) ->
                drawLine(
                    color = accentColor.copy(alpha = (0.2f + index / tracePoints.size.toFloat()).coerceIn(0.2f, 0.92f)),
                    start = start,
                    end = end,
                    strokeWidth = 3.dp.toPx(),
                )
            }
            tracePoints.lastOrNull()?.let { point ->
                drawCircle(Color.White.copy(alpha = 0.6f), 4.dp.toPx(), point)
            }
        }
        FidgetThemeButton(
            text = "C",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 4.dp)
                .size(width = 22.dp, height = 20.dp),
            fontSize = 8.sp,
            selected = true,
            prominent = true,
            accentColor = accentColor,
            accentColorArgb = NEON_GREEN_COLOR,
            onClick = onClear,
        )
    }
}

@Composable
private fun SlingshotFidgetToy(
    ballPosition: Offset,
    pullLimit: Float,
    onPullStart: () -> Unit,
    onPullMove: (Offset) -> Unit,
    onRelease: (Offset) -> Unit,
) {
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .size(118.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color(0xFF56F1C8).copy(alpha = 0.72f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val ballCenter = center + Offset(ballPosition.x.dp.toPx(), ballPosition.y.dp.toPx())
            val pullRatio = (ballPosition.vectorLength() / pullLimit).coerceIn(0f, 1f)

            drawCircle(
                color = Color(0xFF56F1C8).copy(alpha = 0.18f),
                radius = 45.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )
            drawLine(
                color = Color(0xFFFFC857).copy(alpha = 0.58f + pullRatio * 0.32f),
                start = center,
                end = ballCenter,
                strokeWidth = (2.dp + (pullRatio * 2f).dp).toPx(),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.18f),
                radius = 9.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        Box(
            modifier = Modifier
                .offset(x = ballPosition.x.dp, y = ballPosition.y.dp)
                .size(27.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFC857),
                            Color(0xFFEF476F),
                            Color.Black.copy(alpha = 0.32f),
                        ),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = 0.58f), CircleShape)
                .pointerInput(pullLimit) {
                    var dragOffset = Offset.Zero
                    detectDragGestures(
                        onDragStart = {
                            dragOffset = ballPosition
                            onPullStart()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffset = with(density) {
                                Offset(
                                    x = dragOffset.x + dragAmount.x.toDp().value,
                                    y = dragOffset.y + dragAmount.y.toDp().value,
                                )
                            }.limitedToLength(pullLimit)
                            onPullMove(dragOffset)
                        },
                        onDragEnd = {
                            onRelease(dragOffset)
                            dragOffset = Offset.Zero
                        },
                        onDragCancel = {
                            onRelease(dragOffset)
                            dragOffset = Offset.Zero
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.46f)),
            )
        }
    }
}

@Composable
private fun MazeFidgetToy(
    puzzle: FidgetMazePuzzle,
    playerCell: Int,
    onMove: (MazeDirection) -> Unit,
    onRefresh: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(118.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color(0xFF56F1C8).copy(alpha = 0.72f), RoundedCornerShape(16.dp))
            .pointerInput(puzzle) {
                var dragDelta = Offset.Zero
                detectDragGestures(
                    onDragStart = {
                        dragDelta = Offset.Zero
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragDelta += dragAmount
                    },
                    onDragEnd = {
                        dragDelta.toMazeDirection()?.let(onMove)
                        dragDelta = Offset.Zero
                    },
                    onDragCancel = {
                        dragDelta = Offset.Zero
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val boardPadding = 11.dp.toPx()
            val cellSize = (size.minDimension - boardPadding * 2f) / FIDGET_MAZE_COLUMNS
            val startColumn = puzzle.startCell % FIDGET_MAZE_COLUMNS
            val startRow = puzzle.startCell / FIDGET_MAZE_COLUMNS
            val endColumn = puzzle.endCell % FIDGET_MAZE_COLUMNS
            val endRow = puzzle.endCell / FIDGET_MAZE_COLUMNS
            val playerColumn = playerCell % FIDGET_MAZE_COLUMNS
            val playerRow = playerCell / FIDGET_MAZE_COLUMNS

            fun cellCenter(column: Int, row: Int): Offset {
                return Offset(
                    x = boardPadding + cellSize * (column + 0.5f),
                    y = boardPadding + cellSize * (row + 0.5f),
                )
            }

            drawCircle(
                color = Color(0xFF56F1C8).copy(alpha = 0.58f),
                radius = cellSize * 0.24f,
                center = cellCenter(startColumn, startRow),
            )
            drawCircle(
                color = Color(0xFFFFC857).copy(alpha = 0.88f),
                radius = cellSize * 0.25f,
                center = cellCenter(endColumn, endRow),
                style = Stroke(width = 2.dp.toPx()),
            )

            puzzle.openings.forEachIndexed { cellIndex, openMask ->
                val column = cellIndex % FIDGET_MAZE_COLUMNS
                val row = cellIndex / FIDGET_MAZE_COLUMNS
                val left = boardPadding + column * cellSize
                val top = boardPadding + row * cellSize
                val right = left + cellSize
                val bottom = top + cellSize
                val wallColor = Color(0xFF56F1C8).copy(alpha = 0.76f)
                val wallWidth = 1.4.dp.toPx()

                if (openMask and MAZE_OPEN_UP == 0) {
                    drawLine(wallColor, Offset(left, top), Offset(right, top), wallWidth)
                }
                if (openMask and MAZE_OPEN_RIGHT == 0) {
                    drawLine(wallColor, Offset(right, top), Offset(right, bottom), wallWidth)
                }
                if (openMask and MAZE_OPEN_DOWN == 0) {
                    drawLine(wallColor, Offset(left, bottom), Offset(right, bottom), wallWidth)
                }
                if (openMask and MAZE_OPEN_LEFT == 0) {
                    drawLine(wallColor, Offset(left, top), Offset(left, bottom), wallWidth)
                }
            }

            drawCircle(
                color = Color(0xFFEF476F),
                radius = cellSize * 0.28f,
                center = cellCenter(playerColumn, playerRow),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.48f),
                radius = cellSize * 0.1f,
                center = cellCenter(playerColumn, playerRow),
            )
        }

        FidgetThemeButton(
            text = "R",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 4.dp)
                .width(23.dp)
                .height(21.dp),
            fontSize = 9.sp,
            selected = true,
            prominent = true,
            accentColor = Color(0xFFFFC857),
            accentColorArgb = 0xFFFFC857.toInt(),
            onClick = onRefresh,
        )
    }
}

@Composable
private fun WhackColorButtonToy(
    buttonPositions: List<Int>,
    onButtonTap: (Int) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(118.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color(0xFF56F1C8).copy(alpha = 0.72f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val paddingPx = 8.dp.toPx()
            val cellWidth = (size.width - paddingPx * 2f) / SWITCH_MAZE_COLUMNS
            val cellHeight = (size.height - paddingPx * 2f) / SWITCH_MAZE_ROWS

            repeat(SWITCH_MAZE_COLUMNS + 1) { lineIndex ->
                val x = paddingPx + lineIndex * cellWidth
                drawLine(
                    color = Color(0xFF56F1C8).copy(alpha = 0.18f),
                    start = Offset(x, paddingPx),
                    end = Offset(x, size.height - paddingPx),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            repeat(SWITCH_MAZE_ROWS + 1) { lineIndex ->
                val y = paddingPx + lineIndex * cellHeight
                drawLine(
                    color = Color(0xFFFFC857).copy(alpha = 0.14f),
                    start = Offset(paddingPx, y),
                    end = Offset(size.width - paddingPx, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }
        }

        buttonPositions.forEachIndexed { index, position ->
            val column = position % SWITCH_MAZE_COLUMNS
            val row = position / SWITCH_MAZE_COLUMNS
            WhackColorButton(
                index = index,
                modifier = Modifier.offset {
                    val cellStepPx = 25.dp.toPx()
                    IntOffset(
                        x = ((column - 1.5f) * cellStepPx).roundToInt(),
                        y = ((row - 1.5f) * cellStepPx).roundToInt(),
                    )
                },
                onClick = { onButtonTap(index) },
            )
        }
    }
}

@Composable
private fun WhackColorButton(
    index: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = listOf(
        Color(0xFFFFC857),
        Color(0xFF56F1C8),
        Color(0xFFEF476F),
        Color(0xFF8D6BFF),
    )
    val color = colors[index % colors.size]

    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.48f),
                        color,
                        color.copy(alpha = 0.55f),
                        Color.Black.copy(alpha = 0.3f),
                    ),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.48f)),
        )
    }
}

@Composable
private fun FidgetPlaylistNavButton(
    isNext: Boolean,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    accentColorArgb: Int = NEON_GREEN_COLOR,
    width: Dp = 38.dp,
    height: Dp = 64.dp,
    fontSize: TextUnit = 50.sp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val borderColor = accentColor.copy(alpha = 0.86f)
    val chevronColor = readableTextColorFor(accentColorArgb)

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.58f),
                        accentColor.copy(alpha = 0.24f),
                        Color.Black.copy(alpha = 0.34f),
                    ),
                ),
                shape = shape,
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = shape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isNext) ">" else "<",
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = chevronColor,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FidgetNavButton(
    text: String,
    wide: Boolean = false,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    accentColorArgb: Int = NEON_GREEN_COLOR,
    onClick: () -> Unit,
) {
    if (!wide) {
        FidgetThemeButton(
            text = text,
            modifier = Modifier
                .size(width = 44.dp, height = 30.dp)
                .height(30.dp),
            fontSize = 16.sp,
            selected = false,
            prominent = false,
            accentColor = accentColor,
            accentColorArgb = accentColorArgb,
            onClick = onClick,
        )
        return
    }

    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .width(58.dp)
            .height(25.dp)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.96f),
                        accentColor.copy(alpha = 0.78f),
                    ),
                ),
                shape = shape,
            )
            .border(2.dp, accentColor, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = readableTextColorFor(accentColorArgb),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

private fun angleDegrees(
    point: androidx.compose.ui.geometry.Offset,
    center: androidx.compose.ui.geometry.Offset,
): Float {
    return (atan2(point.y - center.y, point.x - center.x) * 180f / PI).toFloat()
}

private fun Offset.vectorLength(): Float {
    return sqrt(x * x + y * y)
}

private fun Offset.limitedToLength(maxLength: Float): Offset {
    val length = vectorLength()
    if (length <= maxLength || length == 0f) return this
    val scale = maxLength / length
    return Offset(x * scale, y * scale)
}

private fun Offset.limitedToBox(maxX: Float, maxY: Float): Offset {
    return Offset(
        x = x.coerceIn(-maxX, maxX),
        y = y.coerceIn(-maxY, maxY),
    )
}

private fun shortestAngleDelta(
    previousDegrees: Float,
    currentDegrees: Float,
): Float {
    var delta = currentDegrees - previousDegrees
    while (delta > 180f) delta -= 360f
    while (delta < -180f) delta += 360f
    return delta
}

private class FidgetFeedbackController(context: Context) {
    private val vibrator: Vibrator? = runCatching {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    }.getOrNull()
    private val clickTone = ToneGenerator(AudioManager.STREAM_MUSIC, 54)
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val loadedSamples = mutableSetOf<Int>()
    private val woodSoundId: Int
    private val bellSoundId: Int

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSamples += sampleId
            }
        }
        woodSoundId = soundPool.load(context, R.raw.wood_mid, 1)
        bellSoundId = soundPool.load(context, R.raw.bell_mid, 1)
    }

    fun play(
        hapticEnabled: Boolean,
        soundEnabled: Boolean,
        beatSoundMode: BeatSoundMode,
        accentIntensityMode: AccentIntensityMode,
    ) {
        if (hapticEnabled) {
            vibrator?.vibrate(
                VibrationEffect.createOneShot(
                    accentIntensityMode.feedbackVibrationMs(),
                    accentIntensityMode.feedbackVibrationAmplitude(),
                ),
            )
        }

        if (!soundEnabled) return

        when (beatSoundMode) {
            BeatSoundMode.Clicks -> clickTone.startTone(
                ToneGenerator.TONE_PROP_BEEP,
                accentIntensityMode.feedbackDurationMs(),
            )
            BeatSoundMode.Wood -> playSample(woodSoundId, accentIntensityMode)
            BeatSoundMode.Bell -> playSample(bellSoundId, accentIntensityMode)
        }
    }

    fun release() {
        clickTone.release()
        soundPool.release()
    }

    private fun playSample(
        soundId: Int,
        accentIntensityMode: AccentIntensityMode,
    ) {
        if (soundId !in loadedSamples) {
            clickTone.startTone(ToneGenerator.TONE_PROP_BEEP, accentIntensityMode.feedbackDurationMs())
            return
        }
        val volume = accentIntensityMode.feedbackVolume()
        val streamId = soundPool.play(soundId, volume, volume, 1, 0, 1f)
        if (streamId == 0) {
            clickTone.startTone(ToneGenerator.TONE_PROP_BEEP, accentIntensityMode.feedbackDurationMs())
        }
    }
}
