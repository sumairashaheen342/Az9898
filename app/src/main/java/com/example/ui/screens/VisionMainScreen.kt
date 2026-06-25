package com.example.ui.screens

import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.LinearGradient
import android.graphics.Shader
import androidx.compose.ui.graphics.toArgb
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.VisionItem
import com.example.ui.viewmodel.VisionViewModel
import kotlinx.coroutines.delay

// Premium Visual Theme Color Palette Tokens
val DarkCanvas = Color(0xFF0E0E12)
val CardSlate = Color(0xFF16161F)
val NeonPurple = Color(0xFF8A2BE2)
val NeonCoral = Color(0xFFFF4500)
val ElectricBlue = Color(0xFF00BFFF)
val AmbientGlow = Brush.linearGradient(listOf(Color(0xFF8A2BE2), Color(0xFFFF4500)))

private fun simulateCameraCapture(viewModel: VisionViewModel) {
    val colors = listOf(Color(0xFF8A2BE2), Color(0xFFFF4500), Color(0xFF00BFFF), Color(0xFF34A853))
    val randomColor = colors.random()
    val width = 512
    val height = 512
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint()
    
    val lg = LinearGradient(
        0f, 0f, width.toFloat(), height.toFloat(),
        randomColor.toArgb(), Color.Black.toArgb(),
        Shader.TileMode.CLAMP
    )
    paint.shader = lg
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    
    paint.shader = null
    paint.color = Color.White.toArgb()
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 4f
    
    canvas.drawCircle(width / 2f, height / 2f, 80f, paint)
    canvas.drawCircle(width / 2f, height / 2f, 5f, paint)
    
    val clip = 40f
    paint.strokeWidth = 6f
    // Top-left
    canvas.drawLine(clip, clip, clip + 50f, clip, paint)
    canvas.drawLine(clip, clip, clip, clip + 50f, paint)
    // Top-right
    canvas.drawLine(width - clip, clip, width - clip - 50f, clip, paint)
    canvas.drawLine(width - clip, clip, width - clip, clip + 50f, paint)
    // Bottom-left
    canvas.drawLine(clip, height - clip, clip + 50f, height - clip, paint)
    canvas.drawLine(clip, height - clip, clip, height - clip - 50f, paint)
    // Bottom-right
    canvas.drawLine(width - clip, height - clip, width - clip - 50f, height - clip, paint)
    canvas.drawLine(width - clip, height - clip, width - clip, height - clip - 50f, paint)
    
    paint.style = Paint.Style.FILL
    paint.textSize = 24f
    paint.color = Color.White.toArgb()
    canvas.drawText("REC 4K UHD", clip + 20f, height - clip - 20f, paint)
    canvas.drawText("ISO 400", width - clip - 130f, height - clip - 20f, paint)
    
    viewModel.setCapturedImage(bitmap)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisionMainScreen(
    viewModel: VisionViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.setCapturedImage(bitmap)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                simulateCameraCapture(viewModel)
            }
        } else {
            simulateCameraCapture(viewModel)
        }
    }

    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    
    val activeResultType by viewModel.activeResultType.collectAsStateWithLifecycle()
    val activeImageBase64 by viewModel.activeImageBase64.collectAsStateWithLifecycle()
    val activeVideoUri by viewModel.activeVideoUri.collectAsStateWithLifecycle()
    val activeAnalysisText by viewModel.activeAnalysisText.collectAsStateWithLifecycle()
    
    val promptInput by viewModel.promptInput.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val selectedQuality by viewModel.selectedQuality.collectAsStateWithLifecycle()
    val selectedAspectRatio by viewModel.selectedAspectRatio.collectAsStateWithLifecycle()
    
    val uploadedImageBase64 by viewModel.uploadedImageBase64.collectAsStateWithLifecycle()
    val selectedSampleName by viewModel.selectedSampleName.collectAsStateWithLifecycle()
    
    val historyItems by viewModel.allHistory.collectAsStateWithLifecycle()

    val playStoreQuery by viewModel.playStoreQuery.collectAsStateWithLifecycle()
    val isScanningPlayStore by viewModel.isScanningPlayStore.collectAsStateWithLifecycle()
    val scannedPlayStoreInfo by viewModel.scannedPlayStoreInfo.collectAsStateWithLifecycle()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Image, 1: Video, 2: Analyze, 3: Play Store Studio
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF101016),
                drawerTonalElevation = 8.dp,
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
                    .testTag("history_sidebar")
            ) {
                SidebarHistoryContent(
                    historyItems = historyItems,
                    onRestoreItem = { item ->
                        restoreHistoryToActive(item, viewModel)
                        scope.launch { drawerState.close() }
                    },
                    onDeleteItem = { id ->
                        viewModel.deleteHistoryItem(id)
                    },
                    onClearAll = {
                        viewModel.clearAllHistory()
                    },
                    onClose = {
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .background(DarkCanvas),
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            },
                            modifier = Modifier.testTag("sidebar_menu_trigger")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (historyItems.isNotEmpty()) {
                                        Badge(
                                            containerColor = NeonPurple,
                                            contentColor = Color.White
                                        ) {
                                            Text(text = historyItems.size.toString(), fontSize = 8.sp)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = "Open History Vault",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "Vision Logo",
                            tint = NeonCoral,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "VisionCraft AI",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DarkCanvas,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(
                        onClick = { viewModel.clearAllHistory() },
                        modifier = Modifier.testTag("clear_history_header_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = "Clear History",
                            tint = Color.Gray
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkCanvas)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            
            // SYSTEM API KEY STATUS HEADER
            if (activeTab != 4) {
                item {
                    ApiKeyStatusBanner()
                }
            }

            // STUDIO MODE SELECTION TABS
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardSlate)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val tabs = listOf(
                        Triple(0, "Image", Icons.Outlined.PhotoLibrary),
                        Triple(1, "Video", Icons.Outlined.MovieFilter),
                        Triple(2, "Analyze", Icons.Outlined.Psychology),
                        Triple(3, "Play Store", Icons.Outlined.ShoppingBag),
                        Triple(4, "Games", Icons.Outlined.Gamepad)
                    )
                    tabs.forEach { (index, title, icon) ->
                        val isSelected = activeTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NeonPurple else Color.Transparent)
                                .clickable {
                                    activeTab = index
                                    viewModel.clearActiveResult()
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp)
                                .testTag("studio_tab_$index"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = title,
                                    tint = if (isSelected) Color.White else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = title,
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // CORE VISUAL CANVAS PREVIEW SPACE
            if (activeTab != 4) {
                item {
                    Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardSlate)
                        .border(
                            BorderStroke(
                                1.dp,
                                if (isGenerating) AmbientGlow else Brush.linearGradient(
                                    listOf(Color.DarkGray, Color.DarkGray)
                                )
                            ),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isGenerating -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = NeonCoral,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Synthesizing pixels...",
                                    color = Color.LightGray,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Rendering visual structures",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        activeResultType != null -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                when (activeResultType) {
                                    "IMAGE" -> {
                                        val bitmap = activeImageBase64?.let { base64ToImageBitmap(it) }
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap,
                                                contentDescription = "AI Generated Output",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            SampleGradientBox(
                                                sampleName = selectedSampleName ?: "VSC_Mountains",
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                    "VIDEO" -> {
                                        // Standard simulation media player
                                        SimulatedVideoPlayer(
                                            prompt = promptInput,
                                            baseSampleName = selectedSampleName
                                        )
                                    }
                                    "ANALYSIS" -> {
                                        LazyColumn(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(16.dp)
                                        ) {
                                            item {
                                                Text(
                                                    text = activeAnalysisText ?: "No description outline",
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    lineHeight = 18.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                // OVERLAY BADGE
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = activeResultType ?: "",
                                        color = NeonCoral,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // ACTIONS ROW BACK/CLEAR
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Vision AI Output",
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                    IconButton(
                                        onClick = { viewModel.clearActiveResult() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Close Preview",
                                            tint = Color.LightGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                        uploadedImageBase64 != null -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                val bitmap = uploadedImageBase64?.let { base64ToImageBitmap(it) }
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = "Uploaded Base Canvas",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    SampleGradientBox(
                                        sampleName = selectedSampleName ?: "VSC_Mountains",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                // Overlay badge representing source state
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (selectedSampleName == "CAMERA_CAPTURE") "CAMERA CAPTURE" else "UPLOADED BASE",
                                        color = NeonPurple,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Actions row back/clear
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (selectedSampleName == "CAMERA_CAPTURE") "Active Live Snap" else "Selected Sandbox Template",
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                    IconButton(
                                        onClick = { viewModel.clearUploadedImage() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Clear Selected Image",
                                            tint = Color.LightGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                        else -> {
                            // EMPTY STATE/ CANVAS SET UP
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (activeTab == 0) Icons.Outlined.PhotoCamera 
                                                  else if (activeTab == 1) Icons.Outlined.SlowMotionVideo 
                                                  else if (activeTab == 2) Icons.Outlined.BatchPrediction 
                                                  else Icons.Outlined.ShoppingBag,
                                    contentDescription = "Empty active state",
                                    tint = NeonPurple.copy(alpha = 0.7f),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = if (activeTab == 0) "Generate or Edit Images" 
                                           else if (activeTab == 1) "Animate Video Timeline" 
                                           else if (activeTab == 2) "Visual Brain Analytics" 
                                           else "Play Store Promo Builder",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (activeTab == 0) "Select an asset block below as high-quality seed, type coordinates and spark prompt!"
                                           else if (activeTab == 1) "Animate a sample photo to standard 16:9 MP4 vector motion format cleanly!"
                                           else if (activeTab == 2) "Feed one of our visual vectors to compute advanced lighting and composite matrices."
                                           else "Search or scan any app above, select your desired asset category below, and spawn graphics!",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
            }

            // LIVE CAMERA & SANDBOX CANVAS CORNER (MANDATORY TO ACCESS HARDWARE CAMERA WORKFLOWS IN CONTEXT)
            if (activeTab != 4) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Live Camera & Sandbox Canvas",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Snap Live",
                                color = Color(0xFF3DDC84),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .clickable {
                                        try {
                                            permissionLauncher.launch(android.Manifest.permission.CAMERA)
                                        } catch (e: Exception) {
                                            simulateCameraCapture(viewModel)
                                        }
                                    }
                                    .testTag("camera_header_snap_btn")
                            )
                            if (uploadedImageBase64 != null) {
                                Text(
                                    text = "Clear Selected",
                                    color = NeonCoral,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .clickable { viewModel.clearUploadedImage() }
                                        .testTag("clear_uploaded_image_btn")
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // PREPENDED LIVE CAMERA QUICK INITIATOR
                        item {
                            val isCameraActive = selectedSampleName == "CAMERA_CAPTURE"
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCameraActive) NeonPurple.copy(alpha = 0.3f) else CardSlate
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isCameraActive) NeonPurple else Color.DarkGray
                                ),
                                modifier = Modifier
                                    .width(130.dp)
                                    .clickable {
                                        try {
                                            permissionLauncher.launch(android.Manifest.permission.CAMERA)
                                        } catch (e: Exception) {
                                            simulateCameraCapture(viewModel)
                                        }
                                    }
                                    .testTag("camera_capture_card")
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(70.dp)
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(Color(0xFF3DDC84), Color(0xFF0F3B1A), Color(0xFF041408))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PhotoCamera,
                                            contentDescription = "Camera Access Trigger",
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = "Device Camera",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = if (isCameraActive) "Snap Loaded" else "Snap Live Shot",
                                            color = if (isCameraActive) NeonPurple else Color(0xFF3DDC84),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        val samples = listOf(
                            Pair("Cyberpunk Tokyo", "VSC_CITY"),
                            Pair("Magic Forest", "VSC_FOREST"),
                            Pair("Futuristic Android", "VSC_ANDROID"),
                            Pair("Arctic Summit", "VSC_MOUNTAINS")
                        )
                        items(samples) { (name, typeCode) ->
                            val isSelected = selectedSampleName == typeCode
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) NeonPurple.copy(alpha = 0.3f) else CardSlate
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) NeonPurple else Color.DarkGray
                                ),
                                modifier = Modifier
                                    .width(130.dp)
                                    .clickable {
                                        viewModel.selectSampleImage(typeCode, typeCode)
                                    }
                                    .testTag("sample_card_$typeCode")
                            ) {
                                Column {
                                    SampleGradientBox(
                                        sampleName = typeCode,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(70.dp)
                                    )
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "Loaded Base Photo",
                                            color = if (isSelected) NeonPurple else Color.Gray,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }

            // DUAL ERROR DIALOGUE DISPLAY
            if (activeTab != 4) {
                item {
                    AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut()
                ) {
                    errorMessage?.let { error ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1010)),
                            border = BorderStroke(1.dp, Color(0xFF8B0000)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = "Warning",
                                    tint = NeonCoral,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Operational Warning",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = error,
                                        color = Color.LightGray,
                                        fontSize = 10.sp,
                                        lineHeight = 13.sp
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.dismissError() },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Dismiss",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }

            // CREATION INPUT SET & GENERATION CONTROLLERS OR PLAY STORE BRANDING AGENT
            if (activeTab == 3) {
                item {
                    PlayStoreStudioControls(
                        viewModel = viewModel,
                        playStoreQuery = playStoreQuery,
                        isScanningPlayStore = isScanningPlayStore,
                        scannedPlayStoreInfo = scannedPlayStoreInfo,
                        onSwitchTab = { targetTab -> activeTab = targetTab }
                    )
                }
            } else if (activeTab == 4) {
                item {
                    BoxingGameScreen()
                }
            } else {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardSlate),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Visual Synthesis Controls",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // Prompt Text Field
                            TextField(
                                value = promptInput,
                                onValueChange = { viewModel.promptInput.value = it },
                                placeholder = {
                                    Text(
                                        text = if (activeTab == 0) "describe the scene you want to generate (e.g., retro neon synthwave city)..."
                                               else if (activeTab == 1) "describe the movement sequence (e.g., zoom in camera slowly, dust flying)..."
                                               else "ask questions or input scanning prompt constraints...",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = DarkCanvas,
                                    unfocusedContainerColor = DarkCanvas,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedIndicatorColor = NeonPurple,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 70.dp)
                                    .testTag("prompt_input_field")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // OPTION GRIDS
                            if (activeTab == 0) {
                                // IMAGE ASPECT RATIO & QUALITY SELECTORS
                                Text(
                                    text = "Aspect Ratio Override",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                val aspectRatios = listOf("1:1", "3:2", "4:3", "16:9", "9:16", "21:9")
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(aspectRatios) { ratio ->
                                        val isSelected = selectedAspectRatio == ratio
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSelected) NeonPurple else DarkCanvas)
                                                .clickable { viewModel.selectedAspectRatio.value = ratio }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                                .testTag("ratio_box_$ratio"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = ratio,
                                                color = if (isSelected) Color.White else Color.LightGray,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Studio Image Model",
                                            color = Color.Gray,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(
                                                selected = selectedModel == "gemini-3.1-flash-image-preview",
                                                onClick = { viewModel.selectedModel.value = "gemini-3.1-flash-image-preview" },
                                                colors = RadioButtonDefaults.colors(selectedColor = NeonPurple)
                                            )
                                            Text("Flash", color = Color.White, fontSize = 11.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            RadioButton(
                                                selected = selectedModel == "gemini-3-pro-image-preview",
                                                onClick = { viewModel.selectedModel.value = "gemini-3-pro-image-preview" },
                                                colors = RadioButtonDefaults.colors(selectedColor = NeonPurple)
                                            )
                                            Text("Studio Pro", color = Color.White, fontSize = 11.sp)
                                        }
                                    }

                                    if (selectedModel == "gemini-3-pro-image-preview") {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Resolution Scale",
                                                color = Color.Gray,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(DarkCanvas)
                                                    .padding(2.dp)
                                            ) {
                                                val sizes = listOf("1K", "2K", "4K")
                                                sizes.forEach { size ->
                                                    val isSizeSel = selectedQuality == size
                                                    Text(
                                                        text = size,
                                                        color = if (isSizeSel) Color.White else Color.Gray,
                                                        fontWeight = if (isSizeSel) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 10.sp,
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(if (isSizeSel) NeonPurple else Color.Transparent)
                                                            .clickable { viewModel.selectedQuality.value = size }
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                            .testTag("size_select_$size")
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (activeTab == 1) {
                                // VIDEO SETTINGS
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Veo 3 Engine Active",
                                            color = Color.LightGray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Model: veo-3.1-fast-generate-preview",
                                            color = Color.Gray,
                                            fontSize = 9.sp
                                        )
                                    }
                                    
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Aspect Ratio",
                                            color = Color.Gray,
                                            fontSize = 10.sp
                                        )
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(DarkCanvas)
                                                .padding(2.dp)
                                        ) {
                                            listOf("16:9", "9:16").forEach { ratio ->
                                                val isSel = selectedAspectRatio == ratio
                                                Text(
                                                    text = if (ratio == "16:9") "16:9 Horizontal" else "9:16 Portrait",
                                                    color = if (isSel) Color.White else Color.Gray,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(if (isSel) NeonPurple else Color.Transparent)
                                                        .clickable { viewModel.selectedAspectRatio.value = ratio }
                                                        .padding(6.dp)
                                                        .testTag("video_ratio_$ratio")
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // ANALYSIS INFO
                                Column {
                                    Text(
                                        text = "Cognitive Intelligence Panel",
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Model: gemini-3.1-pro-preview",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // CORE ACTION BUTTONS
                            Button(
                                onClick = {
                                    when (activeTab) {
                                        0 -> viewModel.runImageGeneration()
                                        1 -> viewModel.runVideoGeneration()
                                        2 -> viewModel.runContentAnalysis(isVideo = false)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("run_action_btn"),
                                enabled = !isGenerating
                            ) {
                                Text(
                                    text = when (activeTab) {
                                        0 -> if (uploadedImageBase64 != null) "Edit Loaded Base Canvas ->" else "Spark AI Image Generator"
                                        1 -> if (uploadedImageBase64 != null) "Animate Sample into Video" else "Generate Veo Cinematic Video"
                                        2 -> "Analyze Active Asset"
                                        else -> "Execute Pipeline"
                                    },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // TIMELINE / INTEGRATED ROOM HISTORY SCREEN
            if (activeTab != 4) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "History & Project Timelines (${historyItems.size})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        if (historyItems.isNotEmpty()) {
                            Text(
                                text = "Clear All",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                modifier = Modifier.clickable { viewModel.clearAllHistory() }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (historyItems.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardSlate),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No saved creations in local Room DB.",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(historyItems) { item ->
                                HistoryCard(
                                    item = item,
                                    onRestore = {
                                        // Tap to restore historical item into active preview
                                        restoreHistoryToActive(item, viewModel)
                                    },
                                    onDelete = {
                                        viewModel.deleteHistoryItem(item.id)
                                    }
                                )
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
fun ApiKeyStatusBanner() {
    val apiKey = com.example.BuildConfig.GEMINI_API_KEY
    val isDemo = apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY"

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isDemo) Color(0xFF1E2012) else Color(0xFF122018)
        ),
        border = BorderStroke(
            1.dp,
            if (isDemo) Color(0xFFB8860B).copy(alpha = 0.5f) else Color(0xFF006400).copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isDemo) Color.Yellow else Color.Green)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isDemo) "Prototype Mode: Configured with Sandbox Simulator" else "Cloud Core Connected: Gemini Live Keys Active",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Text(
                    text = if (isDemo) "Use AI Studio Secrets panel to securely bind your GEMINI_API_KEY for true cloud API rendering."
                           else "Streaming queries automatically directed through standard Google developer endpoints.",
                    color = Color.LightGray,
                    fontSize = 9.sp,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

@Composable
fun SampleGradientBox(sampleName: String, modifier: Modifier = Modifier) {
    val brush = when (sampleName) {
        "VSC_CITY" -> Brush.radialGradient(listOf(Color(0xFF8A2BE2), Color(0xFF3B0066), Color(0xFF070014)))
        "VSC_FOREST" -> Brush.radialGradient(listOf(Color(0xFF2E8B57), Color(0xFF0F3B1A), Color(0xFF041408)))
        "VSC_ANDROID" -> Brush.radialGradient(listOf(Color(0xFFFF4500), Color(0xFF661100), Color(0xFF1F0500)))
        else -> Brush.radialGradient(listOf(Color(0xFF00BFFF), Color(0xFF002244), Color(0xFF000511)))
    }
    Box(
        modifier = modifier
            .background(brush)
            .drawBehind {
                // Draw decorative ambient neon grid lines
                val cols = 8
                val rows = 8
                val widthGap = size.width / cols
                val heightGap = size.height / rows
                for (i in 1..cols) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.05f),
                        start = androidx.compose.ui.geometry.Offset(i * widthGap, 0f),
                        end = androidx.compose.ui.geometry.Offset(i * widthGap, size.height),
                        strokeWidth = 1f
                    )
                }
                for (j in 1..rows) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.05f),
                        start = androidx.compose.ui.geometry.Offset(0f, j * heightGap),
                        end = androidx.compose.ui.geometry.Offset(size.width, j * heightGap),
                        strokeWidth = 1f
                    )
                }
            }
    )
}

@Composable
fun SimulatedVideoPlayer(
    prompt: String,
    baseSampleName: String?
) {
    var isPlaying by remember { mutableStateOf(true) }
    var streamProgress by remember { mutableFloatStateOf(0.4f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                delay(120)
                streamProgress += 0.04f
                if (streamProgress > 1.0f) streamProgress = 0.0f
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SampleGradientBox(
            sampleName = baseSampleName ?: "VSC_MOUNTAINS",
            modifier = Modifier.fillMaxSize()
        )

        // DRAW SLIDING PLAYBACK LINE REPRESENTING RUNWAY VEO MOTION RENDERING
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val lineX = size.width * streamProgress
                    drawLine(
                        color = NeonCoral.copy(alpha = 0.8f),
                        start = androidx.compose.ui.geometry.Offset(lineX, 0f),
                        end = androidx.compose.ui.geometry.Offset(lineX, size.height),
                        strokeWidth = 4f
                    )
                }
        )

        // CONTROLLER INDICATORS GIVING HOLLYWOOD CINEMATOGRAPHIC OUTLINES
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .clickable { isPlaying = !isPlaying },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(BorderStroke(1.dp, NeonCoral), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Playback",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Veo 3.1 Cinema Render: ${if (isPlaying) "Streaming Playback" else "Playback Paused"}",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // FOOTER WITH DURATION HUD
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Length: 5.0 seconds",
                color = Color.LightGray,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Format: MP4 (16:9)",
                color = ElectricBlue,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun HistoryCard(
    item: VisionItem,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        modifier = Modifier
            .width(150.dp)
            .clickable { onRestore() }
            .testTag("history_card_${item.id}"),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                if (item.type == "IMAGE" && item.mediaData != null) {
                    val bitmap = base64ToImageBitmap(item.mediaData)
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "History preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        SampleGradientBox(sampleName = "VSC_MOUNTAINS", modifier = Modifier.fillMaxSize())
                    }
                } else {
                    SampleGradientBox(
                        sampleName = if (item.prompt.contains("city")) "VSC_CITY" else "VSC_FOREST",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.type,
                        color = if (item.type == "VIDEO") ElectricBlue else NeonCoral,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete Item",
                        tint = Color.Red,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = item.prompt,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = getModelNickname(item.modelName),
                    color = Color.Gray,
                    fontSize = 8.sp
                )
                Text(
                    text = if (item.type == "IMAGE") "Size: ${item.imageSize ?: "1K"}" else "Format: MP4",
                    color = Color.LightGray,
                    fontSize = 7.sp
                )
            }
        }
    }
}

fun restoreHistoryToActive(item: VisionItem, viewModel: VisionViewModel) {
    viewModel.clearActiveResult()
    viewModel.promptInput.value = item.prompt
    
    // Set standard ViewModel values to restore state instantly!
    when (item.type) {
        "IMAGE" -> {
            viewModel.selectedModel.value = item.modelName
            viewModel.selectedQuality.value = item.imageSize ?: "1K"
            viewModel.selectedAspectRatio.value = item.aspectRatio ?: "1:1"
            
            // Re-inflate preview data
            com.example.ui.screens.activeResultRef(viewModel, "IMAGE", item.mediaData, null, null)
        }
        "VIDEO" -> {
            viewModel.selectedAspectRatio.value = item.aspectRatio ?: "16:9"
            com.example.ui.screens.activeResultRef(viewModel, "VIDEO", null, item.mediaData, null)
        }
        "ANALYSIS" -> {
            com.example.ui.screens.activeResultRef(viewModel, "ANALYSIS", null, null, item.responseText)
        }
    }
}

/**
 * Accessor reflection/workaround helper to avoid modifying view private state setters.
 */
fun activeResultRef(
    vm: VisionViewModel,
    type: String,
    image: String?,
    video: String?,
    analysis: String?
) {
    try {
        val activeResultTypeField = vm::class.java.getDeclaredField("_activeResultType")
        activeResultTypeField.isAccessible = true
        val artFlow = activeResultTypeField.get(vm) as MutableStateFlow<String?>
        artFlow.value = type

        val activeImageBase64Field = vm::class.java.getDeclaredField("_activeImageBase64")
        activeImageBase64Field.isAccessible = true
        val aibFlow = activeImageBase64Field.get(vm) as MutableStateFlow<String?>
        aibFlow.value = image

        val activeVideoUriField = vm::class.java.getDeclaredField("_activeVideoUri")
        activeVideoUriField.isAccessible = true
        val avuFlow = activeVideoUriField.get(vm) as MutableStateFlow<String?>
        avuFlow.value = video

        val activeAnalysisTextField = vm::class.java.getDeclaredField("_activeAnalysisText")
        activeAnalysisTextField.isAccessible = true
        val aatFlow = activeAnalysisTextField.get(vm) as MutableStateFlow<String?>
        aatFlow.value = analysis
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun base64ToImageBitmap(base64Str: String): ImageBitmap? {
    return try {
        // Handle code placeholders instead of crashes
        if (base64Str.startsWith("VSC_")) return null
        val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

fun getModelNickname(fullName: String): String {
    return when {
        fullName.contains("pro-image") -> "Studio Pro"
        fullName.contains("flash-image") -> "Flash Image"
        fullName.contains("veo") -> "Veo Motion"
        fullName.contains("pro-preview") -> "Gemini Pro"
        else -> "AI Engine"
    }
}

@Composable
fun PlayStoreStudioControls(
    viewModel: VisionViewModel,
    playStoreQuery: String,
    isScanningPlayStore: Boolean,
    scannedPlayStoreInfo: com.example.ui.viewmodel.PlayStoreAppInfo?,
    onSwitchTab: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Store,
                    contentDescription = "Play Store Icon",
                    tint = NeonCoral,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Google Play Store Promo Asset Synth",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Type any application name or package ID below to index metadata using Gemini Search Grounding. Then immediately generate store assets.",
                color = Color.LightGray,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // App identifier text field
            TextField(
                value = playStoreQuery,
                onValueChange = { viewModel.playStoreQuery.value = it },
                placeholder = {
                    Text(
                        text = "app name or package ID (e.g. Spotify, Duolingo, com.Slack)...",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkCanvas,
                    unfocusedContainerColor = DarkCanvas,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = NeonCoral,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("play_store_query_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // **Google Apps Store Showcase**
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Android,
                    contentDescription = "Google Android Logo",
                    tint = Color(0xFF3DDC84), // Android light green
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Google Apps Store Showcase",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
            ) {
                val googleApps = listOf(
                    Triple("Google Maps", "maps", "Travel & Local"),
                    Triple("YouTube", "youtube", "Entertainment"),
                    Triple("Gmail", "gmail", "Communication"),
                    Triple("Google Drive", "drive", "Productivity")
                )
                items(googleApps) { (appName, searchTerm, category) ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkCanvas),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .width(140.dp)
                            .clickable {
                                viewModel.playStoreQuery.value = appName
                                viewModel.scanPlayStoreApp()
                            }
                            .testTag("google_app_card_$searchTerm")
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = when(searchTerm) {
                                                "maps" -> listOf(Color(0xFF34A853), Color(0xFF4285F4))
                                                "youtube" -> listOf(Color(0xFFEA4335), Color(0xFFC5221F))
                                                "gmail" -> listOf(Color(0xFFEA4335), Color(0xFF4285F4))
                                                else -> listOf(Color(0xFFF9BC05), Color(0xFF4285F4))
                                            }
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when(searchTerm) {
                                        "maps" -> Icons.Filled.Place
                                        "youtube" -> Icons.Filled.PlayArrow
                                        "gmail" -> Icons.Filled.Email
                                        else -> Icons.Filled.Cloud
                                    },
                                    contentDescription = appName,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = appName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = category,
                                color = Color.Gray,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = "Rating Star",
                                    tint = NeonCoral,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = when(searchTerm) {
                                        "maps" -> "4.3 • Google"
                                        "youtube" -> "4.1 • Google"
                                        "gmail" -> "4.4 • Google"
                                        else -> "4.5 • Google"
                                    },
                                    color = Color.LightGray,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Analyze button
            Button(
                onClick = { viewModel.scanPlayStoreApp() },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCoral),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("scan_playstore_btn"),
                enabled = !isScanningPlayStore
            ) {
                if (isScanningPlayStore) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Grounded scanning...", color = Color.White, fontSize = 12.sp)
                } else {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = "Scan icon",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan & Architect Store Assets", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Expanded Metadata Details & Generators Cards list!
            if (scannedPlayStoreInfo != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color.DarkGray, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // App Info Header badge
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCanvas),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = scannedPlayStoreInfo.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NeonPurple.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = "Rating",
                                        tint = NeonCoral,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = scannedPlayStoreInfo.rating,
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = scannedPlayStoreInfo.category,
                                color = NeonCoral,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "•", color = Color.Gray, fontSize = 8.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = scannedPlayStoreInfo.developer,
                                color = Color.Gray,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = scannedPlayStoreInfo.description,
                            color = Color.LightGray,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Select & Auto-Forge Store Deliverables:",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // The 4 Assets generators matrices
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val assets = listOf(
                        Quadruple(
                            "Feature Graphic Banner (16:9)",
                            scannedPlayStoreInfo.bannerPrompt,
                            "16:9",
                            0,
                            Icons.Filled.Photo,
                            "ratio_banner"
                        ),
                        Quadruple(
                            "High-Contrast App Icon (1:1)",
                            scannedPlayStoreInfo.iconPrompt,
                            "1:1",
                            0,
                            Icons.Filled.Palette,
                            "ratio_icon"
                        ),
                        Quadruple(
                            "Mobile Screenshot (9:16 portrait)",
                            scannedPlayStoreInfo.screenshotPrompt,
                            "9:16",
                            0,
                            Icons.Filled.Smartphone,
                            "ratio_screenshot"
                        ),
                        Quadruple(
                            "Cinematic Teaser Trailer (16:9 Video)",
                            scannedPlayStoreInfo.videoPrompt,
                            "16:9",
                            1,
                            Icons.Filled.Videocam,
                            "ratio_video"
                        )
                    )

                    assets.forEach { (name, prompt, ratio, targetScreen, icon, tag) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkCanvas),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.promptInput.value = prompt
                                    viewModel.selectedAspectRatio.value = ratio
                                    onSwitchTab(targetScreen)
                                    viewModel.clearActiveResult()
                                }
                                .testTag("asset_template_$tag")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = name,
                                    tint = NeonPurple,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = prompt,
                                        color = Color.Gray,
                                        fontSize = 9.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Brush.linearGradient(listOf(Color(0xFF8A2BE2), Color(0xFFFF4500))))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Forge",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class Quadruple<A, B, C, D, E, F>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F
)

@Composable
fun SidebarHistoryContent(
    historyItems: List<VisionItem>,
    onRestoreItem: (VisionItem) -> Unit,
    onDeleteItem: (Int) -> Unit,
    onClearAll: () -> Unit,
    onClose: () -> Unit
) {
    var filterType by remember { mutableStateOf<String?>("ALL") } // "ALL", "IMAGE", "VIDEO", "ANALYSIS"

    val filteredItems = remember(historyItems, filterType) {
        if (filterType == "ALL") {
            historyItems
        } else {
            historyItems.filter { it.type == filterType }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14))
            .padding(16.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Drawer Header with Close Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = "Vault",
                    tint = NeonPurple,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Creations Vault",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp).testTag("sidebar_close_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close Sidebar",
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        
        Text(
            text = "Your recently generated assets of images & videos.",
            color = Color.Gray,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 4.dp).padding(bottom = 12.dp)
        )

        // Filter chips row
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val filters = listOf(
                Pair("All", "ALL"),
                Pair("Images", "IMAGE"),
                Pair("Videos", "VIDEO"),
                Pair("Specs", "ANALYSIS")
            )
            filters.forEach { (label, type) ->
                val isSelected = filterType == type
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) NeonPurple else CardSlate)
                        .border(1.dp, if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable { filterType = type }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("sidebar_filter_${type.lowercase()}")
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }

        HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(12.dp))

        // History list
        Box(modifier = Modifier.weight(1f)) {
            if (filteredItems.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.HourglassEmpty,
                        contentDescription = "No items",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Vault is empty",
                        color = Color.Gray,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Generate items in the workspace to store them here.",
                        color = Color.DarkGray,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize().testTag("sidebar_items_list")
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        SidebarHistoryRow(
                            item = item,
                            onRestore = { onRestoreItem(item) },
                            onDelete = { onDeleteItem(item.id) }
                        )
                    }
                }
            }
        }

        // Clean-out Section
        if (historyItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onClearAll,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E1216)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFC5221F).copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sidebar_clear_all_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteSweep,
                    contentDescription = "Truncate Vault",
                    tint = Color(0xFFEA4335),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Clear All Vault Assets",
                    color = Color(0xFFEA4335),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SidebarHistoryRow(
    item: VisionItem,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRestore() }
            .testTag("sidebar_item_row_${item.id}"),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail preview
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black)
            ) {
                if (item.type == "IMAGE" && item.mediaData != null) {
                    val bitmap = base64ToImageBitmap(item.mediaData)
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Sidebar thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        SampleGradientBox(sampleName = "VSC_MOUNTAINS", modifier = Modifier.fillMaxSize())
                    }
                } else {
                    SampleGradientBox(
                        sampleName = if (item.prompt.contains("city")) "VSC_CITY" else "VSC_FOREST",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Type corner badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = if (item.type == "ANALYSIS") "SPEC" else item.type,
                        color = when (item.type) {
                            "IMAGE" -> NeonPurple
                            "VIDEO" -> ElectricBlue
                            else -> NeonCoral
                        },
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Body info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.prompt,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = getModelNickname(item.modelName),
                        color = Color.Gray,
                        fontSize = 8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (item.type == "IMAGE") "Size: ${item.imageSize ?: "1K"}" else if (item.type == "VIDEO") "MP4" else "TXT",
                        color = Color.LightGray,
                        fontSize = 7.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action: delete from sidebar directly
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("sidebar_delete_btn_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = Color.Gray.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

